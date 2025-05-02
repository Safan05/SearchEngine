package com.example.searchapi.service;


import com.example.searchapi.SearchApiApplication;
import com.github.xjavathehutt.porterstemmer.PorterStemmer;
import com.example.searchapi.model.SearchResponse;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.hibernate.cache.cfg.internal.DomainDataRegionConfigImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.stereotype.Service;


import javax.naming.directory.SearchResult;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;


import java.io.IOException;



@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> collection;
    private List<Document> documents;
    private List<Document> ranked_documents;

    public SearchService() {
        try {
            this.mongoClient = MongoClients.create("mongodb://localhost:27017");
            this.database = mongoClient.getDatabase("SearchEngine");
            this.collection = database.getCollection("Terms");

            // Validate connection
            if (database.listCollectionNames().first() == null) {
                throw new RuntimeException("Failed to list collections - database connection issue");
            }
            logger.info("Successfully connected to MongoDB database: SearchEngine");
        } catch (Exception e) {
            logger.error("Failed to connect to MongoDB: {}", e.getMessage(), e);
            throw new RuntimeException("Could not connect to MongoDB", e);
        }
    }

    public SearchResponse processSearchQuery(String query) throws IOException {
        SearchResponse response = new SearchResponse();
        List<Document> results = new ArrayList<>();

        try {
            Pattern phrasePattern = Pattern.compile("\"([^\"]+)\"");
            Matcher matcher = phrasePattern.matcher(query);

            if (matcher.find()) {
                String phrase = matcher.group(1).toLowerCase().trim();
                results = handlePhraseSearch(phrase);

                String remainingQuery = matcher.replaceAll("").trim();
                if (!remainingQuery.isEmpty()) {
                    List<Document> termResults = handleTermSearch(remainingQuery);
                    results = intersectResults(results, termResults);
                }
            } else {
                results = handleTermSearch(query);
            }

            if (results.isEmpty()) {
                logger.warn("No results found for query: {}", query);
                response.setResults(results);
                response.setTotal(results.size());
                return response;
            }

            response.setResults(results);
            response.setTotal(results.size());
            logger.info("Search query processed successfully: {} with {} results", query, results.size());
        } catch (Exception e) {
            logger.error("Error processing search query: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing search query", e);
        }

        this.ranked_documents = rank_documents(results);
        List<Document> allResults = new ArrayList<>(convertDocumentsToResults(ranked_documents));
        response.setResults(allResults);
        response.setTotal(allResults.size());

        return response;
    }

    public List<Document> rank_documents(List<Document> documents) {
        // TODO: call ranker
        return documents;
    }


    public static String getSnippet(String stemmedWord, String url, int snippetLength) throws IOException {
        // Parse the webpage using Jsoup

        String text;
        try {
            // Parse the webpage using Jsoup
            org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
            text = doc.body().text();
        } catch (Exception e) {
            // If URL fetch fails, return empty string
            return "";
        }

        // Tokenize the text into words while preserving their positions
        List<String> words = new ArrayList<>();
        List<Integer> wordStartPositions = new ArrayList<>();
        List<Integer> wordEndPositions = new ArrayList<>();

        Pattern wordPattern = Pattern.compile("\\b\\w+\\b");
        Matcher wordMatcher = wordPattern.matcher(text);
        while (wordMatcher.find()) {
            words.add(wordMatcher.group());
            wordStartPositions.add(wordMatcher.start());
            wordEndPositions.add(wordMatcher.end());
        }

        // Find words that stem to the given stemmedWord
        List<Integer> matchingIndices = new ArrayList<>();
        List<String> matchingWords = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            String stemmed = stemWord(word);
            if (stemmed.equals(stemmedWord)) {
                matchingIndices.add(i);
                matchingWords.add(word);
            }
        }

        // If no matches, return empty string
        if (matchingIndices.isEmpty()) {
            return " ";
        }

        // Take the first match for the snippet
        int firstMatchIndex = matchingIndices.get(0);
        int wordStart = wordStartPositions.get(firstMatchIndex);
        int wordEnd = wordEndPositions.get(firstMatchIndex);

        // Calculate snippet boundaries
        int snippetStart = Math.max(0, wordStart - snippetLength / 2);
        int snippetEnd = Math.min(text.length(), wordEnd + snippetLength / 2);

        // Adjust to word boundaries
        if (snippetStart > 0) {
            int spaceIndex = text.lastIndexOf(" ", snippetStart);
            snippetStart = (spaceIndex == -1) ? 0 : spaceIndex + 1;
        }
        if (snippetEnd < text.length()) {
            int spaceIndex = text.indexOf(" ", snippetEnd);
            snippetEnd = (spaceIndex == -1) ? text.length() : spaceIndex;
        }

        // Extract the snippet
        String snippet = text.substring(snippetStart, snippetEnd);

        // Bold all matching words in the snippet
        String boldedSnippet = snippet;
        for (String matchingWord : matchingWords) {
            boldedSnippet = boldedSnippet.replaceAll("(?i)\\b" + Pattern.quote(matchingWord) + "\\b", "<b>$0</b>");
        }

        // Add "..." at the start/end if the snippet is cut off
        if (snippetStart > 0) boldedSnippet = "..." + boldedSnippet;
        if (snippetEnd < text.length()) boldedSnippet = boldedSnippet + "...";

        return boldedSnippet;
    }

    public List<Document> convertDocumentsToResults(List<Document> documents) throws IOException {
        List<Document> results = new ArrayList<>();

        if (documents == null || documents.isEmpty()) {
            return results;
        }

        for (Document doc : documents) {
            String term = doc.getString("term");
            List<Document> pages = doc.getList("pages", Document.class);

            for (Document page : pages) {
                String id = page.getString("id");
                String url = page.getString("url");
                String title = page.getString("title");
                String snippet = page.getString("snippet"); // Fetch text from URL
                if(snippet == null || snippet.isEmpty()) continue;


                Map<String, Object> dictionary = new HashMap<>();
                dictionary.put("term", term);
                dictionary.put("url", url);
                dictionary.put("title", title);
                dictionary.put("snippet", snippet);
                dictionary.put("id",id);

                Document result = new Document(dictionary);


                results.add(result);
            }
        }

        return results;
    }

    private List<Document> handleTermSearch(String query) {
        String[] terms = query.toLowerCase().split("\\s+");
        Set<Document> results = new HashSet<>(); // Use Set to avoid duplicates
        for (String term : terms) {
            String stemmed = stemWord(term);
            if (stemmed.isEmpty()) continue;
            System.out.println("Stemmed term: " + stemmed);
            FindIterable<Document> documents = collection.find(new Document("term", stemmed));
            for (Document doc : documents) {
                results.add(doc);
            }
        }
        return new ArrayList<>(results);
    }

    private List<Document> handlePhraseSearch(String phrase) {
        List<Document> phraseResults = new ArrayList<>();
        String[] terms = phrase.split("\\s+");

        if (terms.length < 1) return phraseResults;

        List<List<Document>> termDocsList = new ArrayList<>();
        for (String term : terms) {
            if (term.isEmpty()) continue;
            List<Document> termDocs = new ArrayList<>();
            FindIterable<Document> documents = collection.find(new Document("term", term));
            for (Document doc : documents) {
                termDocs.add(doc);
            }
            if (termDocs.isEmpty()) {
                logger.warn("No documents found for term: {}", term);
                return phraseResults; // Early return if any term has no matches
            }
            termDocsList.add(termDocs);
        }

        List<String> commonPageIds = findCommonPageIds(termDocsList);

        for (String pageId : commonPageIds) {
            if (checkPhraseInPage(pageId, termDocsList, terms)) {
                for (Document doc : termDocsList.get(0)) {
                    if (docContainsPageId(doc, pageId)) {
                        if (!phraseResults.contains(doc)) {
                            phraseResults.add(doc);
                        }
                        break;
                    }
                }
            }
        }

        return phraseResults;
    }

    private List<String> findCommonPageIds(List<List<Document>> termDocsList) {
        List<String> commonPageIds = new ArrayList<>();
        if (termDocsList.isEmpty()) return commonPageIds;

        Set<String> pageIds = new HashSet<>();
        for (Document doc : termDocsList.get(0)) {
            List<Document> pages = doc.getList("pages", Document.class);
            for (Document page : pages) {
                pageIds.add(page.getString("id"));
            }
        }

        for (int i = 1; i < termDocsList.size(); i++) {
            Set<String> currentPageIds = new HashSet<>();
            for (Document doc : termDocsList.get(i)) {
                List<Document> pages = doc.getList("pages", Document.class);
                for (Document page : pages) {
                    currentPageIds.add(page.getString("id"));
                }
            }
            pageIds.retainAll(currentPageIds);
        }

        return new ArrayList<>(pageIds);
    }

    private boolean checkPhraseInPage(String pageId, List<List<Document>> termDocsList, String[] terms) {
        List<List<Integer>> positionsList = new ArrayList<>();
        for (int i = 0; i < termDocsList.size(); i++) {
            List<Integer> positions = new ArrayList<>();
            for (Document doc : termDocsList.get(i)) {
                List<Document> pages = doc.getList("pages", Document.class);
                for (Document page : pages) {
                    if (page.getString("id").equals(pageId)) {
                        positions.addAll(page.getList("positions", Integer.class));
                        break;
                    }
                }
                if (!positions.isEmpty()) break;
            }
            if (positions.isEmpty()) {
                logger.warn("No positions found for term: {} in page: {}", terms[i], pageId);
                return false;
            }
            positionsList.add(positions);
        }

        // Sort positions for each term to check sequence
        for (List<Integer> positions : positionsList) {
            Collections.sort(positions);
        }

        // Check for consecutive positions
        for (int i = 0; i < positionsList.get(0).size(); i++) {
            int startPos = positionsList.get(0).get(i);
            boolean isSequence = true;
            for (int j = 1; j < positionsList.size(); j++) {
                int expectedPos = startPos + j;
                if (positionsList.get(j).isEmpty() || !positionsList.get(j).contains(expectedPos)) {
                    isSequence = false;
                    break;
                }
            }
            if (isSequence) return true;
        }
        return false;
    }

    private boolean docContainsPageId(Document doc, String pageId) {
        List<Document> pages = doc.getList("pages", Document.class);
        for (Document page : pages) {
            if (page.getString("id").equals(pageId)) {
                return true;
            }
        }
        return false;
    }

    private List<Document> intersectResults(List<Document> phraseResults, List<Document> termResults) {
        Set<Document> termResultSet = new HashSet<>(termResults);
        List<Document> intersected = new ArrayList<>();
        for (Document phraseDoc : phraseResults) {
            if (termResultSet.contains(phraseDoc)) {
                intersected.add(phraseDoc);
            }
        }
        return intersected;
    }


     public static String stemWord(String word) {
        Stemmer stemmer = new Stemmer();
         stemmer.add(word.toCharArray(), word.length());
         stemmer.stem();
         String stemmed = stemmer.toString();
        return stemmed;
    }


    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            logger.info("MongoDB connection closed");
        }
    }
}