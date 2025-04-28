package org.example;
import java.util.*;

public class Ranker {
    private static final DBController mongoDB = new DBController();

    private static final double TITLE_WEIGHT = 1.0;
    private static final double H1_WEIGHT = 0.8;
    private static final double H2_WEIGHT = 0.6;
    private static final double H3_WEIGHT = 0.4;
    static {
        mongoDB.initializeDatabaseConnection();
    }

    private final Map<String, Double> linkScores = new HashMap<>();
    private final Map<String, Double> sortedLinkScores = new LinkedHashMap<>();
    private final List<String> sortedLinks = new ArrayList<>();
    private List<WordResult> wordResults = new ArrayList<>();

    public void score(List<WordResult> results, List<String> queryWords) {
        this.wordResults = results;

        // 1. Score based on headers (h1, h2, h3)
        for (WordResult result : results) {
            for (int i = 0; i < result.getLinks().size(); i++) {
                String link = normalizeUrl(result.getLinks().get(i));
                List<Boolean> headers = result.getHeaders().get(i);

                double headerScore = 0.0;
                if (headers.get(0)) headerScore += H1_WEIGHT;
                if (headers.get(1)) headerScore += H2_WEIGHT;
                if (headers.get(2)) headerScore += H3_WEIGHT;

                linkScores.put(link, linkScores.getOrDefault(link, 0.0) + headerScore);
            }
        }

        // 2. Score based on TF-IDF
        for (WordResult result : results) {
            for (int i = 0; i < result.getLinks().size(); i++) {
                String link = normalizeUrl(result.getLinks().get(i));
                double tfIdfScore = 5.0 * result.getTF().get(i) * result.getIdf();
                linkScores.put(link, linkScores.getOrDefault(link, 0.0) + tfIdfScore);
            }
        }

        // 3. Score based on word occurrence in Title
        for (WordResult result : results) {
            for (int i = 0; i < result.getTitles().size(); i++) {
                String link = normalizeUrl(result.getLinks().get(i));
                String title = result.getTitles().get(i).toLowerCase();
                for (String word : queryWords) {
                    if (title.contains(word.toLowerCase())) {
                        linkScores.put(link, linkScores.getOrDefault(link, 0.0) + 7 * 0.5);
                        break; // Don't count multiple times for the same title
                    }
                }
            }
        }

        // 4. Score based on PageRank
        for (WordResult result : results) {
            for (int i = 0; i < result.getLinks().size(); i++) {
                String link = normalizeUrl(result.getLinks().get(i));
                double pagerankScore = 10.0 * result.getRanks().get(i);
                linkScores.put(link, linkScores.getOrDefault(link, 0.0) + pagerankScore);
            }
        }
    }

    public void sortByValue() {
        List<Map.Entry<String, Double>> list = new LinkedList<>(linkScores.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (Map.Entry<String, Double> entry : list) {
            sortedLinkScores.put(entry.getKey(), entry.getValue());
            sortedLinks.add(entry.getKey());
        }
    }

    public List<RankerResult> setResults() {
        List<RankerResult> results = new ArrayList<>();
        List<String> allLinks = new ArrayList<>();
        List<String> allTitles = new ArrayList<>();
        List<String> allDescriptions = new ArrayList<>();

        for (WordResult result : wordResults) {
            allLinks.addAll(result.getLinks());
            allTitles.addAll(result.getTitles());
            allDescriptions.addAll(result.getDescriptions());
        }

        for (String link : sortedLinks) {
            int index = allLinks.indexOf(link);
            if (index != -1) {
                RankerResult res = new RankerResult();
                res.setLink(link);
                res.setTitle(allTitles.get(index));
                res.setDescription(allDescriptions.get(index));
                results.add(res);
            }
        }

        return results;
    }

    private String normalizeUrl(String url) {
        if (url == null) return "";
        return url.trim().toLowerCase(); // You can improve this later for real normalization
    }
}