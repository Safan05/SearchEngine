package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.*;

public class Indexer {
    DBController mongoDB = new DBController();
    private Set<String> visitedUrls;

    public Indexer() {
        System.out.println("Indexer started.");
        mongoDB.initializeDatabaseConnection();
        visitedUrls = Collections.synchronizedSet(mongoDB.getVisitedPages());

        List<org.bson.Document> nonIndexedPages = mongoDB.getnonIndexedPages();

        for (org.bson.Document page : nonIndexedPages) {
            String url = page.getString("URL");
            if (url == null || visitedUrls.contains(url)) continue;

            try {
                Document document = Jsoup.connect(url).get();

                String title = document.title();

                // Extract content
                Elements elements = document.select("h1, h2, h3, p, li");
                StringBuilder contentBuilder = new StringBuilder();
                for (Element element : elements) {
                    contentBuilder.append(element.text()).append(" ");
                }

                String rawText = contentBuilder.toString();
                String normalized = TextProcessor.normalize(rawText);
                Map<String, Integer> termFrequency = computeTF(normalized);

                // Store in database
                mongoDB.storePageMetaInfo(title, url, normalized);
                mongoDB.storeTermFrequencies(termFrequency, url);

                // Mark page as indexed
                mongoDB.setIndexedAsTrue(page.getObjectId("_id"));

            } catch (IOException e) {
                System.err.println("Failed to fetch or index URL: " + url);
            }
        }
    }

    // Compute raw term frequencies
    private Map<String, Integer> computeTF(String text) {
        Map<String, Integer> tfMap = new HashMap<>();
        String[] words = text.split("\\s+");
        for (String word : words) {
            tfMap.put(word, tfMap.getOrDefault(word, 0) + 1);
            System.out.println("Words"+word);
        }
        return tfMap;
    }
}
