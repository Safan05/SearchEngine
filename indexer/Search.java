import java.sql.*;
import java.util.*;

public class Search {
    private static final Connection connection = DatabaseConnection.getConnection();

    public static void search(String query) {
        String normalizedQuery = TextProcessor.normalize(query);
        String[] terms = normalizedQuery.split("\\s+");

        Map<String, Double> idfMap = computeIDF(terms);
        Map<String, Double> docScores = new HashMap<>();

        for (String term : terms) {
            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT url, tf FROM terms WHERE term = ?");
                stmt.setString(1, term);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    String url = rs.getString("url");
                    double tf = rs.getDouble("tf");
                    double idf = idfMap.getOrDefault(term, 0.0);
                    double tfidf = tf * idf;

                    docScores.put(url, docScores.getOrDefault(url, 0.0) + tfidf);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Sort results by score descending
        List<Map.Entry<String, Double>> sortedResults = new ArrayList<>(docScores.entrySet());
        sortedResults.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Fetch titles and show results
        for (Map.Entry<String, Double> entry : sortedResults) {
            String url = entry.getKey();
            double score = entry.getValue();

            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT title FROM pages WHERE url = ?");
                stmt.setString(1, url);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    System.out.println("Title: " + rs.getString("title"));
                    System.out.println("URL: " + url);
                    System.out.printf("Score: %.4f\n", score);
                    System.out.println("----------------------------------");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (sortedResults.isEmpty()) {
            System.out.println("No results found.");
        }
    }

    private static Map<String, Double> computeIDF(String[] terms) {
        Map<String, Double> idfMap = new HashMap<>();
        int totalDocs = getTotalDocumentCount();

        for (String term : terms) {
            int docCount = getDocumentCountContainingTerm(term);
            double idf = Math.log((double) totalDocs / (1 + docCount));
            idfMap.put(term, idf);
        }

        return idfMap;
    }

    private static int getTotalDocumentCount() {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM pages");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    private static int getDocumentCountContainingTerm(String term) {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(DISTINCT url) FROM terms WHERE term = ?");
            stmt.setString(1, term);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
