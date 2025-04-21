package org.example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.Jsoup;

public class Main {
    private static final ConcurrentHashMap<String, Boolean> visitedUrls = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> contentFingerprints = new ConcurrentHashMap<>();
    private static final AtomicBoolean foundDuplicate = new AtomicBoolean(false);
    private static final AtomicInteger duplicateCount = new AtomicInteger(0);
    private static final int THREAD_POOL_SIZE = 5; // Adjust based on your needs

    private static final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static final BlockingQueue<String> urlQueue = new LinkedBlockingQueue<>();

    public static void crawl() {
        while (!foundDuplicate.get() && !Thread.currentThread().isInterrupted()) {
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
                if (visitedUrls.putIfAbsent(normalizedUrl, true) != null) {
                    continue;
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
                    if (contentFingerprints.putIfAbsent(fingerprint, true) != null) {
                        System.out.println("[DUPLICATE] Found duplicate content at: " + crawledUrl +
                                " (" + Thread.currentThread().getName() + ")");
                        System.out.println("[DUPLICATE] Fingerprint: " + fingerprint);

                        continue;
                    }

                    System.out.println("[CRAWLING] Processing: " + crawledUrl +
                            " (" + Thread.currentThread().getName() + ")");

                    // Process page content
                    processPage(doc);

                    // Extract and queue new links
                    Elements links = doc.select("a[href]");
                    for (Element link : links) {
                        String nextUrl = link.attr("abs:href");
                        String normalizedNextUrl = Normalize.normalizeUrl(nextUrl);
                        if (!visitedUrls.containsKey(normalizedNextUrl)) {
                            urlQueue.offer(nextUrl);
                        }
                    }

                } catch (Exception e) {
                    System.out.println("[ERROR] Failed to crawl " + crawledUrl +
                            " (" + Thread.currentThread().getName() + "): " + e.getMessage());
                }
            } catch (InterruptedException e) {
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
        Path path = Path.of("URLS.txt");
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                urlQueue.add(line);
            }

            // Start multiple crawler threads
            for (int i = 0; i < THREAD_POOL_SIZE; i++) {
                executorService.submit(Main::crawl);
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
        }

        System.out.println("\nCrawl finished.");
        System.out.println("Unique pages visited: " + visitedUrls.size());
        System.out.println("Duplicate pages found: " + duplicateCount.get());
    }
}