package org.example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.Jsoup;

public class Crawler extends Thread {
    private static final DBController mongoDB = new DBController();
    private static Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<String>());
    private static Set<String> contentFingerprints = Collections.synchronizedSet(new HashSet<String>());
    private static final int THREAD_POOL_SIZE = 5; // Adjust based on your needs
    private static final int MAX_PAGES = 6000;
    private static final AtomicInteger pageCounter = new AtomicInteger(0);

    private static final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static BlockingQueue<String> urlQueue = new LinkedBlockingQueue<>();

    static {
        mongoDB.initializeDatabaseConnection();
        visitedUrls = Collections.synchronizedSet(mongoDB.getVisitedPages());
        contentFingerprints = Collections.synchronizedSet(mongoDB.getCompactStrings());
        urlQueue = new LinkedBlockingQueue<>(mongoDB.getPendingPages());
    }

    @Override
    public void run() {
        while (!urlQueue.isEmpty() && !Thread.currentThread().isInterrupted() && pageCounter.get() < MAX_PAGES) {
            try {
                String crawledUrl = urlQueue.poll(1, TimeUnit.SECONDS);
                if (crawledUrl == null) {
                    if (executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                        break; // No more URLs and all tasks completed
                    }
                    continue;
                }

                String normalizedUrl = Normalize.normalizeUrl(crawledUrl);

                // Skip if visited (thread-safe check)
                synchronized (visitedUrls) {
                    if (visitedUrls.contains(normalizedUrl)) {
                        continue;
                    }
                }

                // Check robots.txt
                if (!RobotsTxtHandler.canCrawl(crawledUrl)) {
                    System.out.println("[ROBOTS] Blocked: " + crawledUrl + " (" + Thread.currentThread().getName() + ")");
                    continue;
                }

                try {
                    Document doc = Jsoup.connect(crawledUrl)
                            .userAgent("MyCrawler/1.0")
                            .timeout(10000)
                            .get();

                    String fingerprint = Normalize.generateSiteFingerprint(doc);

                    // Check for duplicate content (thread-safe)
                    synchronized (contentFingerprints) {
                        if (contentFingerprints.contains(fingerprint)) {
                            System.out.println("[DUPLICATE] Found duplicate content at: " + crawledUrl +
                                    " (" + Thread.currentThread().getName() + ")");
                            System.out.println("[DUPLICATE] Fingerprint: " + fingerprint);
                            continue;
                        }
                    }

                    System.out.println("[CRAWLING] Processing: " + crawledUrl +
                            " (" + Thread.currentThread().getName() + ")");

                    // Process page content
                    processPage(doc);

                    // Add to visited pages in database
                    mongoDB.addVisitedPage(normalizedUrl, doc.title(), doc.text(), fingerprint);

                    // Add fingerprint to content fingerprints
                    synchronized (contentFingerprints) {
                        contentFingerprints.add(fingerprint);
                    }

                    // Add URL to visited URLs
                    synchronized (visitedUrls) {
                        visitedUrls.add(normalizedUrl);
                    }

                    // Remove from pending pages in database
                    mongoDB.removePendingPage(crawledUrl);

                    // Extract and queue new links
                    Elements links = doc.select("a[href]");
                    for (Element link : links) {
                        String nextUrl = link.attr("abs:href");
                        String normalizedNextUrl = Normalize.normalizeUrl(nextUrl);

                        synchronized (visitedUrls) {
                            if (!visitedUrls.contains(normalizedNextUrl)) {
                                if (urlQueue.offer(nextUrl)) {
                                    // Add to pending pages in database
                                    mongoDB.addPendingPage(nextUrl);
                                }
                            }
                        }
                    }
                    // After successful page processing:
                    int count = pageCounter.incrementAndGet();
                    if (count >= MAX_PAGES) {
                        System.out.println("[LIMIT] Reached maximum pages limit (" + MAX_PAGES + ")");
                        executorService.shutdownNow(); // Stop all threads
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("[ERROR] Failed to crawl " + crawledUrl +
                            " (" + Thread.currentThread().getName() + "): " + e.getMessage());
                    // Remove from pending pages if there was an error
                    mongoDB.removePendingPage(crawledUrl);
                }
            } catch (InterruptedException e) {
                System.out.println("[ERROR] Interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void processPage(Document doc) {
        // Your page processing logic here
        System.out.println("  Title: " + doc.title());
        System.out.println("  Content length: " + doc.text().length());
    }
    public static void main(String[] args) {
        System.out.println("[CRAWLING] Starting Crawler");
        Path path = Path.of("URLS.txt");
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (urlQueue.offer(line)) {
                    // Add to pending pages in database
                    mongoDB.addPendingPage(line);
                }
            }

            // Start multiple crawler threads
            for (int i = 0; i < THREAD_POOL_SIZE; i++) {
                new Thread(new Crawler()).start();
            }

            // Wait for completion or duplicate limit
            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        } catch (IOException e) {
            System.err.println("Error reading URLS.txt: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Crawl interrupted: " + e.getMessage());
        } finally {
            if (!executorService.isTerminated()) {
                executorService.shutdownNow();
            }

            // Clean up any remaining pending pages
            for (String url : urlQueue) {
                mongoDB.removePendingPage(url);
            }

            mongoDB.closeConnection();
        }

        System.out.println("\nCrawl finished.");
        System.out.println("Unique pages visited: " + visitedUrls.size());
    }
}