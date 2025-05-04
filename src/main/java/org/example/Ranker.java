package org.example;

import java.util.*;
import java.util.stream.Collectors;

public class Ranker {
    // Weights configuration
    private static final double TITLE_WEIGHT = 0.3;
    private static final double H1_WEIGHT = 0.25;
    private static final double H2_WEIGHT = 0.15;
    private static final double H3_WEIGHT = 0.1;
    private static final double TFIDF_WEIGHT = 0.4;
    private static final double PAGERANK_WEIGHT = 0.3;

    public List<RankerResults> score(List<wordResult> results, List<String> queryWords) {
        System.out.println(queryWords);
        System.out.println(results);
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Aggregate all pages with their components
        Map<String, PageComponents> pageData = aggregatePageData(results, queryWords);

        // 2. Calculate scores for each page
        Map<String, Double> finalScores = calculateScores(pageData, queryWords);

        // 3. Sort by score and prepare results
        return prepareRankedResults(finalScores, pageData);
    }

    private Map<String, PageComponents> aggregatePageData(List<wordResult> results, List<String> queryWords) {
        Map<String, PageComponents> pageData = new HashMap<>();

        for (wordResult result : results) {
            for (int i = 0; i < result.getLinks().size(); i++) {
                String link = normalizeUrl(result.getLinks().get(i));
                PageComponents components = pageData.computeIfAbsent(link, k -> new PageComponents());

                // Aggregate TF-IDF scores (sum for multiple query terms)
                components.tfIdfSum += result.getTF().get(i) * result.getIdf();

                // Track best header match
                List<Boolean> headers = result.getHeaders().get(i);
                components.headerScore += headers.get(0) ? H1_WEIGHT : 0;
                components.headerScore += headers.get(1) ? H2_WEIGHT : 0;
                components.headerScore += headers.get(2) ? H3_WEIGHT : 0;

                // Track title matches
                String title = result.getTitles().get(i).toLowerCase();
                components.titleMatches += queryWords.stream().filter(word -> title.contains(word.toLowerCase())).count();

                System.out.println(pageData);
                System.out.println(result.getRanks());
                // Track best PageRank
                components.pageRank = Math.max(components.pageRank, result.getRanks().get(i));
                components.snippets = result.getSnippets().get(i);

                // Store metadata (use first occurrence)
                if (components.title == null) {
                    components.title = result.getTitles().get(i);
                    components.description = result.getDescriptions().get(i);
                }
            }
        }
        return pageData;
    }

    private Map<String, Double> calculateScores(Map<String, PageComponents> pageData, List<String> queryWords) {
        // Find max values for normalization
        double maxTfIdf = pageData.values().stream()
                .mapToDouble(c -> c.tfIdfSum)
                .max().orElse(1.0);
        double maxPageRank = pageData.values().stream()
                .mapToDouble(c -> c.pageRank)
                .max().orElse(1.0);

        Map<String, Double> finalScores = new HashMap<>();
        for (Map.Entry<String, PageComponents> entry : pageData.entrySet()) {
            PageComponents comp = entry.getValue();

            // Normalized scores
            double tfIdfScore = TFIDF_WEIGHT * (comp.tfIdfSum / maxTfIdf);
            double pageRankScore = PAGERANK_WEIGHT * (comp.pageRank / maxPageRank);
            double titleScore = TITLE_WEIGHT * (comp.titleMatches / (double) queryWords.size());
            double headerScore = Math.min(comp.headerScore, H1_WEIGHT); // Cap at best header weight

            finalScores.put(entry.getKey(),
                    tfIdfScore + pageRankScore + titleScore + headerScore);
        }

        return finalScores;
    }

    private List<RankerResults> prepareRankedResults(Map<String, Double> scores, Map<String, PageComponents> pageData) {
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> {
                    RankerResults result = new RankerResults();
                    result.setLink(entry.getKey());
                    result.setScore(entry.getValue());
                    PageComponents comp = pageData.get(entry.getKey());
                    result.setTitle(comp.title);
                    result.setDescription(comp.description);
                    result.setSnippet(comp.snippets);
                    return result;
                })
                .collect(Collectors.toList());
    }

    private String normalizeUrl(String url) {
        if (url == null) return "";
        // Improved normalization - remove fragments, trailing slashes, etc.
        return url.trim().toLowerCase()
                .replaceAll("#.*$", "")
                .replaceAll("/+$", "");
    }

    // Helper class to store page components
    private static class PageComponents {
        double tfIdfSum = 0;
        double headerScore = 0;
        int titleMatches = 0;
        double pageRank = 0;
        String title;
        String description;
        List<String> snippets;
    }
}