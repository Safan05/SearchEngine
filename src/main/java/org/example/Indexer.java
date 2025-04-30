package org.example;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import java.util.*;

public class Indexer {
    DBController mongoDB = new DBController();
    private Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<String>());
    public Indexer(Document document, String url) {

        mongoDB.initializeDatabaseConnection();
        visitedUrls = Collections.synchronizedSet(mongoDB.getVisitedPages());

        String title = document.title();
        String link = url;

        // Extract relevant elements
        Elements elements = document.select("h1, h2, h3, p, li");
        StringBuilder contentBuilder = new StringBuilder();
        for (Element element : elements) {
            contentBuilder.append(element.text()).append(" ");
        }

        String rawText = contentBuilder.toString();
        String normalized = TextProcessor.normalize(rawText);
        Map<String, Integer> termFrequency = computeTF(normalized);

        // Store page meta info

        // Store page meta info using DBController method
        mongoDB.storePageMetaInfo(title, url, normalized);
        // Store term frequencies using DBController method
        mongoDB.storeTermFrequencies(termFrequency, url);
    }

    // Compute raw term frequencies
    private Map<String, Integer> computeTF(String text) {
        Map<String, Integer> tfMap = new HashMap<>();
        String[] words = text.split("\\s+");
        for (String word : words) {
            tfMap.put(word, tfMap.getOrDefault(word, 0) + 1);
        }
        return tfMap;
    }
}