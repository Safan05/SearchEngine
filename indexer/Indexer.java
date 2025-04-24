import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class Indexer {
    static Connection connection = null;

    Indexer(Document document, String url) {
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

        try {
            connection = DatabaseConnection.getConnection();

            // Store page meta info
            PreparedStatement pageStmt = connection.prepareStatement("INSERT INTO pages VALUES (?, ?, ?);");
            pageStmt.setString(1, title);
            pageStmt.setString(2, url);
            pageStmt.setString(3, normalized);
            pageStmt.executeUpdate();

            // Store term frequencies
            for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
                String term = entry.getKey();
                double tf = entry.getValue(); // Already normalized later

                PreparedStatement termStmt = connection.prepareStatement("INSERT INTO terms VALUES (?, ?, ?);");
                termStmt.setString(1, term);
                termStmt.setString(2, url);
                termStmt.setDouble(3, tf); // optionally normalize
                termStmt.executeUpdate();
            }

        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
    }

    // Compute raw term frequencies
    private Map<String, Integer> computeTF(String text) {
        Map<String, Integer> tfMap = new HashMap<>();
        String[] words = text.split("\\s+");
        for (String word : words) {
            tfMap.put(word, tfMap.getOrDefault(word, 0) + 1);
        }

        // Normalize to relative frequency
        int totalWords = words.length;
        Map<String, Integer> normalized = new HashMap<>();
        for (Map.Entry<String, Integer> entry : tfMap.entrySet()) {
            int count = entry.getValue();
            normalized.put(entry.getKey(), count); // can change to relative: (double) count / totalWords
        }
        return normalized;
    }
}
