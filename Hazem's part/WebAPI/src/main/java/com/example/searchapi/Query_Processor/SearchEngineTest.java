package com.example.searchapi.Query_Processor;
import java.util.*;

public class SearchEngineTest {
    public static void main(String[] args) {
        // Create sample documents
        List<Document> documents = new ArrayList<>();
        documents.add(new Document("1", "Java Programming", "http://example.com/java", "Learn Java programming basics"));
        documents.add(new Document("2", "Python Basics", "http://example.com/python", "Introduction to Python programming"));
        documents.add(new Document("3", "Java Advanced", "http://example.com/java-advanced", "Advanced Java programming concepts"));
        documents.add(new Document("4", "Coding Tips", "http://example.com/coding", "Java and Python coding tips"));

        // Initialize the indexer
        IndexerImpl indexer = new IndexerImpl(documents);

        // Initialize the query processor
        QueryProcessor queryProcessor = new QueryProcessor(indexer);

        // Test 1: Standard query
        System.out.println("Test 1: Standard Query - 'java programming'");
        SearchResults results1 = queryProcessor.processQuery("java programming");
        printResults(results1);

        // Test 2: Phrase query
        System.out.println("\nTest 2: Phrase Query - '\"Java programming\"'");
        SearchResults results2 = queryProcessor.processQuery("\"Java programming\"");
        printResults(results2);

        // Test 3: Query with stop words
        System.out.println("\nTest 3: Query with Stop Words - 'java prog and python'");
        SearchResults results3 = queryProcessor.processQuery("java and python");
        printResults(results3);

        // Test 4: Empty query
        System.out.println("\nTest 4: Empty Query - ''");
        SearchResults results4 = queryProcessor.processQuery("");
        printResults(results4);


    }

    private static void printResults(SearchResults results) {
        System.out.println("Query: " + results.getOriginalQuery());
        System.out.println("Is Phrase Search: " + results.isPhraseSearch());
        System.out.println("Processing Time: " + results.getProcessingTime() + "ms");
        System.out.println("Number of Results: " + results.getResults().size());
        System.out.println("Results:");
        for (Document doc : results.getResults()) {
            System.out.println(" - ID: " + doc.getId() + ", Title: " + doc.getTitle() + ", URL: " + doc.getUrl() + " , snippet: "+doc.getSnippet());
        }
    }
}