package org.example.service;


import org.example.Ranker;
import org.example.RankerResults;
import org.example.model.SearchResponse;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.example.wordResult;
import org.jsoup.Jsoup;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static com.mongodb.client.model.Filters.*;


@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private final  Ranker ranker= new Ranker();
    private final MongoClient mongoClient;
    protected final MongoDatabase database;
    private final MongoCollection<Document> collection;
    private final MongoCollection<Document> Pages;
    protected List<RankerResults> ranker_results;

    public SearchService() {
        try {
            this.mongoClient = MongoClients.create("mongodb://localhost:27017");
            this.database = mongoClient.getDatabase("SearchEngine");
            this.collection = database.getCollection("Terms");
            this.Pages = database.getCollection("VisitedPages");
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

        // Check if the query is a phrase search (enclosed in quotes)
        if (isPhraseSearch(query)) {
            results = handlePhraseSearch(query);
        } else {
            results = handleTermSearch(query);
        }

        if (results.isEmpty()) {
            logger.warn("No results found for query: {}", query);
            response.setResults(results);
            response.setTotal(results.size());
            return response;
        }

        List<String> Query_Words = Arrays.asList(query.toLowerCase().split("[\\s\\-_.!@#]+\"\'"));
        this.ranker_results = rank_documents(results,Query_Words);
        List<Document> finalResults;

        if(isPhraseSearch(query)) {
            finalResults = handlePhraseResults(ranker_results,query.substring(1, query.length() - 1));
        }
        else {
            finalResults = new ArrayList<>(convertDocumentsToResults(ranker_results));
//            finalResults = new ArrayList<>(convertDocumentsToResultsWithoutRanking(results));
        }

        response.setResults(finalResults);
        response.setTotal(finalResults.size());

        return response;
    }

    private boolean isPhraseSearch(String query) {
        return query.startsWith("\"") && query.endsWith("\"") && query.length() > 2;
    }



    private List<Document> handlePhraseSearch(String query) {
        // Remove the quotes
        String phrase = query.substring(1, query.length() - 1);
        String[] terms = phrase.toLowerCase().split("[\\s\\-_.!@#\"']+");

        // Get documents containing all terms (like current handleTermSearch)
        List<Document> documentsWithAllTerms = handleTermSearch(phrase);

        // Now filter to only keep documents where terms appear in order
        List<Document> finalDocuments = new ArrayList<>();
        for (Document termDoc : documentsWithAllTerms) {
            List<Document> pages = termDoc.getList("pages", Document.class);
            List<Document> docPages = new ArrayList<>();
            for (Document page : pages) {
                int x = checkPhraseInPage(page.getList("snippets", String.class), phrase);
                if (x != -1) {
                    docPages.add(page);
                }
            }
            Document genDoc = new Document();
            genDoc.put("id",termDoc.get("_id"));
            genDoc.put("pages",docPages);
            genDoc.put("term",termDoc.getString("term"));
            if(termDoc.keySet().contains("idf") && termDoc.getDouble("idf") != null){
                genDoc.put("idf",termDoc.getDouble("idf"));
            }
            else{
                genDoc.put("idf",null);
            }
            genDoc.put("df",termDoc.get("df"));
            finalDocuments.add(genDoc);
        }

        return finalDocuments;
    }

    private List<Document> handlePhraseResults(List<RankerResults> pages, String origPhrase){
        List<Document> results = new ArrayList<>();
        for(RankerResults page: pages){
            int snippetIndex = checkPhraseInPage(page.getSnippets(),origPhrase);
            if(snippetIndex == -1) continue;
            String phrasesnippet = page.getSnippets().get(snippetIndex);
            phrasesnippet = phrasesnippet.replace("<b>","").replace("</b>","");
            int startOfPhrase = phrasesnippet.indexOf(origPhrase);
            int endOfPhrase = startOfPhrase + origPhrase.length();
            if(endOfPhrase >= phrasesnippet.length())
                endOfPhrase = phrasesnippet.length() - 1;

            phrasesnippet = phrasesnippet.substring(0,startOfPhrase) + "<b>" + origPhrase + "</b>" + phrasesnippet.substring(endOfPhrase);

            String url = page.getLink();
            String title = page.getTitle();
            String term = page.getTerm();
            String id = page.getId();
            Map<String, Object> dictionary = new HashMap<>();
            dictionary.put("term", term);
            dictionary.put("url", url);
            dictionary.put("title", title);
            dictionary.put("snippet", phrasesnippet);
            dictionary.put("id",id);
            Document result = new Document(dictionary);
            results.add(result);
        }
        return results;
    }

    private int checkPhraseInPage(List<String> snippets, String phrase) {
        String snippet;

        for (int i = 0;i < snippets.size();i++) {
            String s = snippets.get(i).replace("<b>","").replace("</b>","");
            if(findExactSentence(phrase,s) != -1) {
                return i;
            }
        }
        return -1;

    }




//    public List<Document> filterPages(List<Document> pages,List<String>queryWords) {
//        List<Document> uniquePages = new ArrayList<>();
//        List<String> urls = new ArrayList<>();
//        for (Document page : pages) {
//            String pageUrl = page.getString("url");
//            if (!urls.contains(pageUrl)) {
//                urls.add(pageUrl);
//                uniquePages.add(page);
//            }
//            else{
//                Document page1 = uniquePages.get(urls.indexOf(pageUrl));
//                Document page2 = page;
//
//                List<String> snippets = new ArrayList<>();
//                snippets.add(page1.getString("snippet"));
//                snippets.add(page2.getString("snippet"));
//
//                System.out.println("s1:" + page1.getString("snippet"));
//                System.out.println("s2:" + page2.getString("snippet"));
//
//                String bestSnippet = extractSnippetsFromUrl(pageUrl,queryWords);
//                uniquePages.get(urls.indexOf(pageUrl)).put("snippet",bestSnippet);
//
//                System.out.println("best snippet:" + bestSnippet);
//
//
//            }
//
//        }
//
//        return uniquePages;
//    }

    public static String extractSnippetsFromUrl(String url, List<String> queryWords) {
        String pageText = fetchPageText(url);
        if (pageText == null || pageText.isEmpty()) return "";

        Set<String> stemmedWords = new HashSet<>();
        for (String word : queryWords) {
            stemmedWords.add(stemWord(word.toLowerCase()));
        }

        String[] tokens = pageText.split("\\s+");
        List<String> lowerTokens = new ArrayList<>();
        for (String token : tokens) {
            lowerTokens.add(token.toLowerCase());
        }

        List<Integer> matchIndexes = new ArrayList<>();
        for (int i = 0; i < lowerTokens.size(); i++) {
            String clean = tokens[i].replaceAll("\\W", "").toLowerCase();
            if (stemmedWords.contains(stemWord(clean))) {
                matchIndexes.add(i);
            }
        }

        if (matchIndexes.isEmpty()) return "";

        List<String> snippets = new ArrayList<>();
        for (int index : matchIndexes) {
            int start = Math.max(0, index - 35);
            int end = Math.min(tokens.length, index + 36);

            StringBuilder snippet = new StringBuilder("- ");
            for (int i = start; i < end; i++) {
                String clean = tokens[i].replaceAll("\\W", "").toLowerCase();
                if (stemmedWords.contains(stemWord(clean))) {
                    snippet.append("<b>").append(tokens[i]).append("</b>");
                } else {
                    snippet.append(tokens[i]);
                }
                snippet.append(" ");
            }

            snippets.add(snippet.toString().trim());
        }

        return String.join("\n", snippets);
    }

    private static String fetchPageText(String urlString) {
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL(urlString);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(url.openStream())
            );
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line).append(" ");
            }
            in.close();
        } catch (IOException e) {
            return "";
        }
        return result.toString().replaceAll("(?i)<[^>]*>", ""); // Strip HTML tags
    }




    public List<RankerResults> rank_documents(List<Document> documents, List<String> Query_Words) {

        List<wordResult> wordResults = new ArrayList<>();
        for (Document document : documents) {
            wordResult wordresult = new wordResult();
            wordresult.setIDF(document.getDouble("idf"));
            List<Document> pages = document.getList("pages", Document.class);
            for (Document page : pages) {
                String url = page.getString("url");
                wordresult.addLinks(url);
                wordresult.addDesc(page.getString("snippet"));
                wordresult.addTitle(page.getString("title"));
                wordresult.addTF(page.getDouble("tf"));
                wordresult.addPosition(page.getList("positions",Integer.class));
                wordresult.addHeaders(page.getList("headers", Boolean.class));
                wordresult.addSnippets(page.getList("snippets",String.class));
                double rank=0;
               // System.out.println(url);
               // System.out.println("THE PAGE IS"+Pages.find(eq("URL",url)).first());
                if (Pages.find(eq("URL",url)).first()!= null)
                    rank = Objects.requireNonNull(Pages.find(eq("URL", url)).first()).getDouble("pageRank");
                wordresult.addranks(rank);
            }
            wordResults.add(wordresult);
        }
        return ranker.score(wordResults,Query_Words);
        //;;
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


    public List<Document> convertDocumentsToResults(List<RankerResults> documents) throws IOException {
        List<Document> results = new ArrayList<>();

        if (documents == null || documents.isEmpty()) {
            return results;
        }

        for (RankerResults doc : documents) {

            String url = doc.getLink();
            String title = doc.getTitle();

            String term = doc.getTerm();
            String id = doc.getId();
            List<String> snippets = doc.getSnippets();
            if(snippets == null || snippets.isEmpty()) continue;


            Map<String, Object> dictionary = new HashMap<>();
            dictionary.put("term", term);
            dictionary.put("url", url);
            dictionary.put("title", title);
            dictionary.put("snippet", snippets.getFirst());
            dictionary.put("id",id);

            Document result = new Document(dictionary);


            results.add(result);
            }

        return results;
    }

    public List<Document> convertDocumentsToResultsWithoutRanking(List<Document> documents) throws IOException {
        List<Document> results = new ArrayList<>();

        if (documents == null || documents.isEmpty()) {
            return results;
        }

        for (Document doc : documents) {
            List<Document> pages = doc.getList("pages", Document.class);
            for(Document page: pages){

                List<String> snippets = page.getList("snippets",String.class);
                if(snippets == null || snippets.isEmpty() ||
                        snippets.getFirst() == null || snippets.getFirst().isEmpty()) continue;

                String url = page.getString("url");
                String title = page.getString("title");

                String term = doc.getString("term");
                String id = page.getString("pageId");

                Map<String, Object> dictionary = new HashMap<>();
                dictionary.put("term", term);
                dictionary.put("url", url);
                dictionary.put("title", title);
                dictionary.put("snippet", snippets.getFirst());
                dictionary.put("id",id);

                Document result = new Document(dictionary);


                results.add(result);
            }


        }

        return results;
    }
    private List<Document> handleTermSearch(String query) {
        String[] terms = query.toLowerCase().split("[\\s\\-_.!@#'\"]+");
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


     public static String stemWord(String word) {
        Stemmer stemmer = new Stemmer();
         stemmer.add(word.toCharArray(), word.length());
         stemmer.stem();
        return stemmer.toString();
    }

    public int findExactSentence(String sentence, String text) {
        sentence = sentence.trim();
        int sentenceLength = sentence.length();
        int textLength = text.length();

        if (sentenceLength == 0) {
            return textLength == 0 ? 0 : -1;
        }

        // Search through the text for exact matches
        int index = 0;
        while (index < textLength) {
            // Find the next occurrence of the sentence
            index = text.indexOf(sentence, index);
            if (index == -1) {
                break;
            }

            // Check boundaries - must be either:
            // 1. At start of text and sentence ends at text boundary or is followed by whitespace/punctuation
            // 2. Preceded by whitespace/punctuation and ends at text boundary or is followed by whitespace/punctuation
            boolean validStart = (index == 0) ||
                    isBoundaryCharacter(text.charAt(index - 1));
            boolean validEnd = (index + sentenceLength == textLength) ||
                    isBoundaryCharacter(text.charAt(index + sentenceLength));

            if (validStart && validEnd) {
                return index;
            }

            index++;
        }

        return -1;
    }

    private static boolean isBoundaryCharacter(char c) {
        // Consider whitespace or punctuation as boundary characters
        return Character.isWhitespace(c) ||
                c == '.' || c == '!' || c == '?' ||
                c == ',' || c == ';' || c == ':' ||
                c == '(' || c == ')' || c == '[' ||
                c == ']' || c == '{' || c == '}';
    }



    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            logger.info("MongoDB connection closed");
        }
    }
}