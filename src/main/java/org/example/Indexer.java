package org.example.indexer;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import java.util.*;

public class Indexer {
    private MongoDatabase database;

    public Indexer(Document document, String url) {
        database = MongoDatabase.getConnection();

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
        MongoCollection<org.bson.Document> pagesCollection = database.getCollection("pages");
        org.bson.Document pageDoc = new org.bson.Document()
                .append("title", title)
                .append("url", url)
                .append("content", normalized);
        pagesCollection.insertOne(pageDoc);

        // Store term frequencies
        MongoCollection<org.bson.Document> termsCollection = database.getCollection("terms");
        for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
            org.bson.Document termDoc = new org.bson.Document()
                    .append("term", entry.getKey())
                    .append("url", url)
                    .append("tf", entry.getValue());
            termsCollection.insertOne(termDoc);
        }
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