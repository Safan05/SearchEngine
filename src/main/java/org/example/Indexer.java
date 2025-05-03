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
    private final PorterStemmer stemmer;
    private final Map<String, Map<String, List<Integer>>> termPositions;
    private final Map<String, Set<String>> linkGraph = new HashMap<>(); // URL -> Outgoing links
    private final Map<String, Set<String>> reverseLinkGraph = new HashMap<>(); // URL -> Incoming links

    public Indexer() {
        System.out.println("Indexer started.");
        this.stemmer = new PorterStemmer();
        this.termPositions = new ConcurrentHashMap<>();

        // Initialize database connection
        mongoDB = new DBController();
        mongoDB.initializeDatabaseConnection();

        // Retrieve visited URLs
        visitedUrls = Collections.synchronizedSet(mongoDB.getVisitedPages());
        //     System.out.println("Visited urls: " + visitedUrls);
        // Create thread pool
        executor = Executors.newFixedThreadPool(20);



        // Second pass: Process content and calculate PageRank
        List<Future<?>> futures = new ArrayList<>();
        for (String url : visitedUrls) {
            futures.add(executor.submit(() -> processPage(url)));
        }
        waitForCompletion(futures);
        System.out.println("All Pages processed.");

        // First pass: Build link graph
        buildLinkGraph();
        // Calculate PageRank scores
          Map<String, Double> pageRanks = calculatePageRank();

        // Store PageRank scores
          storePageRanks(pageRanks);
        System.out.println("All Pages ranked.");
        // Calculate and store IDF values
        calculateAndStoreIDF();

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt(); // Preserve interrupt status
        }


        System.out.println("Indexer finished.");
    }

    private void buildLinkGraph() {
        for (String url : visitedUrls) {
            String normalizedUrl = validateAndNormalizeUrl(url);
            if (normalizedUrl == null) continue;
            List<String> links = mongoDB.getLinksFromPage(normalizedUrl);
            linkGraph.putIfAbsent(normalizedUrl, new HashSet<>());

            for (String link : links) {
                String normalizedLink = validateAndNormalizeUrl(link);
                if (normalizedLink != null && visitedUrls.contains(normalizedLink)) {
                    linkGraph.get(normalizedUrl).add(normalizedLink);
                    reverseLinkGraph.computeIfAbsent(normalizedLink, k -> new HashSet<>()).add(normalizedUrl);
                }
            }

            reverseLinkGraph.putIfAbsent(normalizedUrl, reverseLinkGraph.getOrDefault(normalizedUrl, new HashSet<>()));
        }
    }


    private Map<String, Double> calculatePageRank() {
        final double DAMPING_FACTOR = 0.85;
        final int MAX_ITERATIONS = 50;
        final double CONVERGENCE_THRESHOLD = 0.0001;

        Map<String, Double> pageRanks = new HashMap<>();
        Map<String, Double> newPageRanks = new HashMap<>();
        int N = visitedUrls.size();
        double initialRank = 1.0 / N;

        // Initialize ranks
        for (String url : visitedUrls) {
            pageRanks.put(url, initialRank);
        }

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            double danglingSum = 0.0;

            // Calculate sum of dangling node ranks
            for (String url : visitedUrls) {
                if (linkGraph.getOrDefault(url, Collections.emptySet()).isEmpty()) {
                    danglingSum += pageRanks.get(url);
                }
            }

            // Distribute dangling sum to all nodes
            double danglingFactor = DAMPING_FACTOR * danglingSum / N;

            for (String url : visitedUrls) {
                double sum = 0.0;

                // Sum contributions from pages linking to this URL
                for (String incoming : reverseLinkGraph.getOrDefault(url, Collections.emptySet())) {
                    int outDegree = linkGraph.getOrDefault(incoming, Collections.emptySet()).size();
                    if (outDegree > 0) {
                        sum += pageRanks.get(incoming) / outDegree;
                    }
                }

                // Calculate new rank
                double newRank = (1.0 - DAMPING_FACTOR) / N + DAMPING_FACTOR * sum + danglingFactor;
                newPageRanks.put(url, newRank);
            }

            // Check for convergence
            boolean converged = true;
            for (String url : visitedUrls) {
                if (Math.abs(newPageRanks.get(url) - pageRanks.get(url)) > CONVERGENCE_THRESHOLD) {
                    converged = false;
                    break;
                }
            }

            // Update ranks for next iteration
            pageRanks = new HashMap<>(newPageRanks);

            if (converged) {
                System.out.println("PageRank converged after " + (iter + 1) + " iterations");
                break;
            }
        }

        return pageRanks;
    }

    private void storePageRanks(Map<String, Double> pageRanks) {
        for (Map.Entry<String, Double> entry : pageRanks.entrySet()) {
            mongoDB.updatePageRank(entry.getKey(), entry.getValue());
        }
    }

    private void calculateAndStoreIDF() {
        long totalDocuments = mongoDB.getTotalDocumentCount();
        termPositions.keySet().forEach(term -> {
            int documentFrequency = mongoDB.getDocumentCountForTerm(term);
            double idf = Math.log((double) totalDocuments / (1 + documentFrequency));
            mongoDB.updateTermIDF(term, idf);
        });
    }

    private void processPage(String url) {
        String threadInfo = "Thread-" + Thread.currentThread().getId();
        try {
            String normalizedUrl = validateAndNormalizeUrl(url);
            if (normalizedUrl == null) return;
            if (mongoDB.isPageIndexed(normalizedUrl)) {
                System.out.println("Skipping already indexed URL: " + normalizedUrl);
                return;
            }
            Document document = Jsoup.connect(normalizedUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            String title = document.title();

            // Extract headers information
            boolean[] headers = new boolean[3];
            headers[0] = !document.select("h1").isEmpty();
            headers[1] = !document.select("h2").isEmpty();
            headers[2] = !document.select("h3").isEmpty();

            // Extract links and update graph
            Elements linkElements = document.select("a[href]");
            Set<String> links = new HashSet<>();
            for (Element link : linkElements) {
                String linkUrl = link.attr("abs:href");
                String normalizedLink = validateAndNormalizeUrl(linkUrl);
                if (normalizedLink != null && visitedUrls.contains(normalizedLink)) {
                    links.add(normalizedLink);
                }
            }

            Elements contentElements = document.select("h1, h2, h3, p, li");
            StringBuilder contentBuilder = new StringBuilder();
            for (Element element : contentElements) {
                contentBuilder.append(element.text()).append(" ");
            }

            String rawText = contentBuilder.toString();
            String normalized = TextProcessor.normalize(rawText);
            TermData termData = computeTFWithPositions(normalized);

            ObjectId pageId = mongoDB.storePageMetaInfo(
                    title,
                    normalizedUrl,
                    normalized,
                    headers,
                    new ArrayList<>(links) // Store outgoing links
            );

            for (Map.Entry<String, Integer> entry : termData.termFrequency.entrySet()) {
                String term = entry.getKey();
                int frequency = entry.getValue();
                //int idf = calculateAndStoreIDF();
                List<Integer> positions = termData.termPositions.get(term);
                double tf = (double) frequency / termData.totalTerms;
                List<String> snippets = getCenteredTermSnippets(term, rawText, 250);
                mongoDB.storeTermInfo(
                        term,
                        pageId,
                        normalizedUrl,
                        title,
                        frequency,
                        tf,
                        positions,
                        headers,
                        snippets
                );

                termPositions.computeIfAbsent(term, k -> new ConcurrentHashMap<>())
                        .put(normalizedUrl, positions);
            }
            System.out.println(threadInfo + " - Indexed: " + normalizedUrl);
            //System.out.println("Indexed: " + normalizedUrl);
        } catch (IOException e) {
            System.err.println(threadInfo +" - Failed to index URL: " + url + " - " + e.getMessage());
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
                    String stemmed = stemmer.stem(word);
                    tfMap.put(stemmed, tfMap.getOrDefault(stemmed, 0) + 1);
                    positionsMap.computeIfAbsent(stemmed, k -> new ArrayList<>()).add(i);
                    totalTerms++;
                }
            }
        }

        return new TermData(tfMap, positionsMap, totalTerms);
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
    private List<String> getCenteredTermSnippets(String term, String text, int snippetLength) {
        List<String> snippets = new ArrayList<>();

        if (text == null || term == null || text.isEmpty() || term.isEmpty()) {
            return snippets;
        }

        String[] words = text.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            String word = words[i].toLowerCase();
            String stemmed = stemWord(word);
            if (stemmed.equals(term)) {
                // Calculate positions to center the term
                int halfContext = snippetLength / 2;
                int start = Math.max(0, i - halfContext);
                int end = Math.min(words.length, start + snippetLength);

                // Adjust if we're near the end of text
                if (end - start < snippetLength) {
                    start = Math.max(0, end - snippetLength);
                }

                StringBuilder snippet = new StringBuilder();
                for (int j = start; j < end; j++) {
                    String currentWord = words[j];
                    if (stemWord(currentWord.toLowerCase()).equals(term)) {
                        snippet.append("<b>").append(currentWord).append("</b> ");
                    } else {
                        snippet.append(currentWord).append(" ");
                    }
                }

                // Add ellipsis if not at beginning/end
                if (start > 0) snippet.insert(0, "... ");
                if (end < words.length) snippet.append("...");

                snippets.add(snippet.toString().trim());
            }
        }
        return snippets;
    }
    public static String stemWord(String word) {
        Stemmer stemmer = new Stemmer();
        stemmer.add(word.toCharArray(), word.length());
        stemmer.stem();
        String stemmed = stemmer.toString();
        return stemmed;
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
            //System.err.println("Invalid URL format: " + url);
            return null;
        }
    }
}
