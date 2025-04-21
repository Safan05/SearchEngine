package org.example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.Jsoup;

public class Main {
    private static final HashSet<String> visitedUrls = new HashSet<>();
    private static final HashSet<String> contentFingerprints = new HashSet<>();


    public static void crawl(String url) {


        Queue<String> queue = new LinkedList<>();
        queue.add(url);

        while (!queue.isEmpty()) {
            String crawledUrl = queue.poll();
            String normalizedUrl = Normalize.normalizeUrl(crawledUrl);

            // Skip if visited
            if (isVisited(normalizedUrl)) {
                continue;
            }

            // Check robots.txt
            if (!RobotsTxtHandler.canCrawl(crawledUrl)) {
                System.out.println("[ROBOTS] Blocked: " + crawledUrl);
                continue;
            }

            try {
                Document doc = Jsoup.connect(crawledUrl)
                        .userAgent("MyCrawler/1.0")
                        .timeout(10000)
                        .get();

                String fingerprint = Normalize.generateSiteFingerprint(doc);

                // Check for duplicate content
                if (contentFingerprints.contains(fingerprint)) {
                    System.out.println("[DUPLICATE] Found duplicate content at: " + crawledUrl);
                    System.out.println("[DUPLICATE] Fingerprint: " + fingerprint);
                    continue;
                }

                // Mark as visited
                visitedUrls.add(normalizedUrl);
                contentFingerprints.add(fingerprint);
                System.out.println("[CRAWLING] Processing: " + crawledUrl);

                // Process page content
                processPage(doc);

                // Extract and queue new links
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String nextUrl = link.attr("abs:href");
                    String normalizedNextUrl = Normalize.normalizeUrl(nextUrl);
                    if (!isVisited(normalizedNextUrl)) {
                        queue.add(nextUrl);
                    }
                }

            } catch (Exception e) {
                System.out.println("[ERROR] Failed to crawl " + crawledUrl + ": " + e.getMessage());
            }
        }
    }

    private static boolean isVisited(String url) {
        return visitedUrls.contains(url);
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
                System.out.println("\nStarting crawl for: " + line);
                crawl(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading URLS.txt: " + e.getMessage());
        }

        System.out.println("\nCrawl finished.");
        System.out.println("Unique pages visited: " + visitedUrls.size());
    }
}