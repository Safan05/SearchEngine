package org.example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.Jsoup;

public class Crawler implements Runnable {
    private static final DBController mongoDB = new DBController();
    private static final ConcurrentHashMap<String, Boolean> visitedUrls = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> contentFingerprints = new ConcurrentHashMap<>();
    private static final int THREAD_POOL_SIZE = 20;
    private static final int MAX_PAGES = 300;
    private static final AtomicInteger pageCounter = new AtomicInteger(0);
    private static final BlockingQueue<String> urlQueue = new LinkedBlockingQueue<>();

    private static ExecutorService executorService;
    private static volatile boolean running = true;


    static {
        mongoDB.initializeDatabaseConnection();
        mongoDB.resetVisitedPages();
        mongoDB.getCompactStrings().forEach(fp -> contentFingerprints.put(fp, true));
    }

    @Override
    public void run() {
        while (running && pageCounter.get() < MAX_PAGES) {
            try {
                String crawledUrl = urlQueue.take(); // Blocking take instead of poll
                if (crawledUrl == null) continue;

                processUrl(crawledUrl);
            } catch (InterruptedException e) {
                System.out.println("[INFO] Thread interrupted: " + Thread.currentThread().getName());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.out.println("[ERROR] in thread " + Thread.currentThread().getName() + ": " + e.getMessage());
            }
        }
    }

    private void processUrl(String crawledUrl) throws IOException {
        String normalizedUrl = Normalize.normalizeUrl(crawledUrl);

        // Skip if visited using atomic operation
        if (visitedUrls.putIfAbsent(normalizedUrl, true) != null) {
            return;
        }

        // Check robots.txt
        if (!RobotsTxtHandler.canCrawl(crawledUrl)) {
            System.out.println("[ROBOTS] Blocked: " + crawledUrl + " (" + Thread.currentThread().getName() + ")");
            return;
        }

        Document doc = Jsoup.connect(crawledUrl)
                .userAgent("MyCrawler/1.0")
                .timeout(10000)
                .get();

        String fingerprint = Normalize.generateSiteFingerprint(doc);

        // Check for duplicate content
        if (contentFingerprints.putIfAbsent(fingerprint, true) != null) {
            System.out.println("[DUPLICATE] Found duplicate content at: " + crawledUrl +
                    " (" + Thread.currentThread().getName() + ")");
            return;
        }

        System.out.println("[CRAWLING] Processing: " + crawledUrl +
                " (" + Thread.currentThread().getName() + ")");

        processPage(doc);

        // Add to database
        mongoDB.addVisitedPage(normalizedUrl, doc.title(), doc.text(), fingerprint);

        // Extract and queue new links
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String nextUrl = link.attr("abs:href");
            String normalizedNextUrl = Normalize.normalizeUrl(nextUrl);

            if (!visitedUrls.containsKey(normalizedNextUrl)) {
                urlQueue.offer(nextUrl);
            }
        }

        // Update counter and check limit
        if (pageCounter.incrementAndGet() >= MAX_PAGES) {
            System.out.println("[LIMIT] Reached maximum pages limit (" + MAX_PAGES + ")");
            running = false;
            executorService.shutdownNow();
        }
    }

    private static void processPage(Document doc) {
        System.out.println("  Title: " + doc.title());
        System.out.println("  Content length: " + doc.text().length());
    }

    public static void main(String[] args) {
        System.out.println("[CRAWLING] Starting Crawler");

        // Initialize executor service
        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        // Load initial URLs
        Path path = Path.of("URLS.txt");
        try {
            Files.lines(path, StandardCharsets.UTF_8).forEach(urlQueue::offer);

            // Submit tasks to executor service
            for (int i = 0; i < THREAD_POOL_SIZE; i++) {
                executorService.submit(new Crawler());
                System.out.println("Thread " + (i+1) + " started");
            }

            // Add shutdown hook for proper cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running = false;
                executorService.shutdownNow();
                mongoDB.closeConnection();
            }));

            // Wait for completion
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

        } catch (IOException e) {
            System.err.println("Error reading URLS.txt: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Crawl interrupted: " + e.getMessage());
        } finally {
            running = false;
            executorService.shutdownNow();

            mongoDB.closeConnection();
        }

        System.out.println("\nCrawl finished.");
        System.out.println("Unique pages visited: " + visitedUrls.size());
    }
}