package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import org.bson.types.ObjectId;
import opennlp.tools.stemmer.PorterStemmer;

public class Indexer {
    private final DBController mongoDB;
    private final Set<String> visitedUrls;
    private final ExecutorService executor;

    private final Map<String, Map<String, List<Integer>>> termPositions;
    private String getTermSnippet(String term, String text, int snippetLength) {
        if (text == null || term == null || text.isEmpty() || term.isEmpty()) {
            return "";
        }

        String[] words = text.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            String word = words[i].toLowerCase();
            String stemmed = stemWord(word);
            if (stemmed.equals(term)) {
                // Get the snippet starting at this word
                StringBuilder snippet = new StringBuilder();
                int end = Math.min(i + snippetLength, words.length);
                for (int j = i; j < end; j++) {
                    String currentWord = words[j];
                    // Highlight the matching term
                    if (stemWord(currentWord.toLowerCase()).equals(term)) {
                        snippet.append("<b>").append(currentWord).append("</b> ");
                    } else {
                        snippet.append(currentWord).append(" ");
                    }
                }
                return snippet.toString().trim();
            }
        }
        return "";
    }
    public Indexer() {
        System.out.println("Indexer started.");
        this.termPositions = new ConcurrentHashMap<>();

        // Initialize database connection
        mongoDB = new DBController();
        mongoDB.initializeDatabaseConnection();

        // Retrieve visited URLs
        visitedUrls = Collections.synchronizedSet(mongoDB.getVisitedPages());

        // Create thread pool
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // Process URLs in parallel
        List<Future<?>> futures = new ArrayList<>();
        for (String url : visitedUrls) {
            futures.add(executor.submit(() -> processPage(url)));
        }

        waitForCompletion(futures);
        calculateAndStoreIDF();
        executor.shutdown();
    }

    private void calculateAndStoreIDF() {
        long totalDocuments = mongoDB.getTotalDocumentCount();
        termPositions.keySet().forEach(term -> {
            int documentFrequency = mongoDB.getDocumentCountForTerm(term);
            double idf = Math.log((double) totalDocuments / (1 + documentFrequency));
            mongoDB.updateTermIDF(term, idf);
        });
    }

    private String validateAndNormalizeUrl(String url) {
        try {
            if (url == null || url.isEmpty() ||
                    url.startsWith("javascript:") ||
                    url.startsWith("mailto:") ||
                    url.startsWith("#")) {
                return null;
            }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            url = url.split("#")[0].replaceAll("/+$", "");
            new java.net.URL(url);
            return url;
        } catch (Exception e) {
            System.err.println("Invalid URL format: " + url);
            return null;
        }
    }

    private void processPage(String url) {
        try {
            String normalizedUrl = validateAndNormalizeUrl(url);
            if (normalizedUrl == null) return;

            Document document = Jsoup.connect(normalizedUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            String title = document.title();
            Elements elements = document.select("h1, h2, h3, p, li");
            StringBuilder contentBuilder = new StringBuilder();
            for (Element element : elements) {
                contentBuilder.append(element.text()).append(" ");
            }

            String rawText = contentBuilder.toString();
            String normalized = TextProcessor.normalize(rawText);
            TermData termData = computeTFWithPositions(normalized);

            ObjectId pageId = mongoDB.storePageMetaInfo(title, normalizedUrl, normalized);

            for (Map.Entry<String, Integer> entry : termData.termFrequency.entrySet()) {
                String term = entry.getKey();
                int frequency = entry.getValue();
                List<Integer> positions = termData.termPositions.get(term);
                double tf = (double) frequency / termData.totalTerms;
                String snippet = getTermSnippet(term, normalized, 50);
                mongoDB.storeTermInfo(
                        term,
                        pageId,
                        normalizedUrl,
                        title,
                        frequency,
                        tf,
                        positions,
                        snippet
                );

                termPositions.computeIfAbsent(term, k -> new ConcurrentHashMap<>())
                        .put(normalizedUrl, positions);
            }

            System.out.println("Indexed: " + normalizedUrl);
        } catch (IOException e) {
            System.err.println("Failed to index URL: " + url + " - " + e.getMessage());
        }
    }

    private TermData computeTFWithPositions(String text) {
        Map<String, Integer> tfMap = new HashMap<>();
        Map<String, List<Integer>> positionsMap = new HashMap<>();
        int totalTerms = 0;

        if (text != null && !text.isEmpty()) {
            String[] words = text.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                String word = words[i].toLowerCase();
                if (word.length() > 2) {
                    String stemmed = stemWord(word); // Stem the word
                    tfMap.put(stemmed, tfMap.getOrDefault(stemmed, 0) + 1);
                    positionsMap.computeIfAbsent(stemmed, k -> new ArrayList<>()).add(i);
                    totalTerms++;
                }
            }
        }

        return new TermData(tfMap, positionsMap, totalTerms);
    }

    public static String stemWord(String word) {
        Stemmer stemmer = new Stemmer();
        stemmer.add(word.toCharArray(), word.length());
        stemmer.stem();
        String stemmed = stemmer.toString();
        return stemmed;
    }
    private void waitForCompletion(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Indexing error: " + e.getMessage());
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) {
        new Indexer();
    }

    private static class TermData {
        final Map<String, Integer> termFrequency;
        final Map<String, List<Integer>> termPositions;
        final int totalTerms;

        public TermData(Map<String, Integer> termFrequency,
                        Map<String, List<Integer>> termPositions,
                        int totalTerms) {
            this.termFrequency = termFrequency;
            this.termPositions = termPositions;
            this.totalTerms = totalTerms;
        }
    }
}