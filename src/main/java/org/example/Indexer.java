package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class Indexer {
    private final DBController mongoDB;
    private final Set<String> visitedUrls;
    private final ExecutorService executor;

    public Indexer() {
        System.out.println("Indexer started.");

        // Initialize database connection
        mongoDB = new DBController();
        mongoDB.initializeDatabaseConnection();

        // Retrieve visited URLs
        visitedUrls = Collections.synchronizedSet(mongoDB.getVisitedPages());

        // Create a thread pool for concurrent execution
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // Process visited URLs in parallel
        List<Future<?>> futures = new ArrayList<>();
        for (String url : visitedUrls) {
            futures.add(executor.submit(() -> processPage(url)));
        }

        // Wait for all tasks to complete
        waitForCompletion(futures);

        // Shut down executor service
        executor.shutdown();
    }

    /**
     * Validates and normalizes a URL before processing
     */
    private String validateAndNormalizeUrl(String url) {
        try {
            // Skip invalid URLs
            if (url == null || url.isEmpty() ||
                    url.startsWith("javascript:") ||
                    url.startsWith("mailto:") ||
                    url.startsWith("#")) {
                return null;
            }

            // Ensure URL has proper protocol
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            // Additional normalization
            url = url.split("#")[0]  // Remove fragments
                    .replaceAll("/+$", "");  // Remove trailing slashes

            // Final validation
            new java.net.URL(url);
            return url;
        } catch (Exception e) {
            System.err.println("Invalid URL format: " + url);
            return null;
        }
    }

    /**
     * Fetches and processes a web page.
     */
    private void processPage(String url) {
        try {
         //   System.out.println("Processing page " + url);
            // Validate and normalize URL first
            String normalizedUrl = validateAndNormalizeUrl(url);
          //  System.out.println("Processing page " + normalizedUrl);
            if (normalizedUrl == null) {
                System.err.println("Skipping invalid URL: " + url);
                return;
            }

            // Fetch the page content with proper timeout and user agent
            Document document = Jsoup.connect(normalizedUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            String title = document.title();
            // Extract text from headers and paragraphs
            Elements elements = document.select("h1, h2, h3, p, li");
            StringBuilder contentBuilder = new StringBuilder();
            for (Element element : elements) {
                contentBuilder.append(element.text()).append(" ");
            }

            // Normalize and compute term frequencies
            String rawText = contentBuilder.toString();
            String normalized = TextProcessor.normalize(rawText);
            Map<String, Integer> termFrequency = computeTF(normalized);

            // Store processed data in the database
            mongoDB.storePageMetaInfo(title, normalizedUrl, normalized);
            mongoDB.storeTermFrequencies(termFrequency, normalizedUrl);

            System.out.println("Successfully indexed: " + normalizedUrl);
        } catch (IOException e) {
            System.err.println("Failed to fetch or index URL: " + url + " - " + e.getMessage());
        }
    }

    /**
     * Computes term frequency from the given text.
     */
    private Map<String, Integer> computeTF(String text) {
        Map<String, Integer> tfMap = new HashMap<>();
        if (text == null || text.isEmpty()) {
            return tfMap;
        }

        String[] words = text.split("\\s+");
        for (String word : words) {
            if (word.length() > 2) {  // Ignore short words
                tfMap.put(word.toLowerCase(), tfMap.getOrDefault(word.toLowerCase(), 0) + 1);
            }
        }
        return tfMap;
    }

    /**
     * Waits for all indexing tasks to complete.
     */
    private void waitForCompletion(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                System.err.println("Indexing interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                System.err.println("Error in indexing task: " + e.getCause().getMessage());
            }
        }
    }

    public static void main(String[] args) {
        new Indexer();
    }
}