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
     * Fetches and processes a web page.
     */
    private void processPage(String url) {
        try {
            // Fetch the page content
            Document document = Jsoup.connect(url).get();
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
            mongoDB.storePageMetaInfo(title, url, normalized);
            mongoDB.storeTermFrequencies(termFrequency, url);

            System.out.println("Processed: " + url);
        } catch (IOException e) {
            System.err.println("Failed to fetch or index URL: " + url);
        }
    }

    /**
     * Computes term frequency from the given text.
     */
    private Map<String, Integer> computeTF(String text) {
        Map<String, Integer> tfMap = new HashMap<>();
        String[] words = text.split("\\s+");

        for (String word : words) {
            tfMap.put(word, tfMap.getOrDefault(word, 0) + 1);
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
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error in indexing task: " + e.getMessage());
            }
        }
    }
}