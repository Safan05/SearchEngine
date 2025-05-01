package com.example.searchapi.Query_Processor;
import java.util.*;

public class IndexerImpl implements Indexer {
    // Inverted index: term -> list of documents
    private Map<String, List<Document>> invertedIndex;
    // Term positions: docId -> term -> list of positions
    private Map<String, Map<String, List<Integer>>> termPositions;
    // Document frequency: term -> number of documents containing the term
    private Map<String, Integer> documentFrequency;
    // Total number of documents
    private int totalDocuments;

    public IndexerImpl(List<Document> documents) {
        this.invertedIndex = new HashMap<>();
        this.termPositions = new HashMap<>();
        this.documentFrequency = new HashMap<>();
        this.totalDocuments = documents.size();

        // Build the index from the provided documents
        buildIndex(documents);
    }

    private void buildIndex(List<Document> documents) {
        PorterStemmer stemmer = new PorterStemmer();

        for (Document doc : documents) {
            String content = (doc.getTitle() + " " + doc.getSnippet()).toLowerCase();
            String[] tokens = content.split("\\s+");
            Map<String, List<Integer>> docPositions = new HashMap<>();

            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                if (!token.isEmpty()) {
                    String stemmedToken = stemmer.stem(token);
                    
                    // Update inverted index
                    invertedIndex.computeIfAbsent(stemmedToken, k -> new ArrayList<>()).add(doc);
                    
                    // Update term positions
                    docPositions.computeIfAbsent(stemmedToken, k -> new ArrayList<>()).add(i);
                    
                    // Update document frequency
                    documentFrequency.merge(stemmedToken, 1, (oldVal, newVal) -> oldVal);
                }
            }
            
            termPositions.put(doc.getId(), docPositions);
        }
    }

    @Override
    public List<Document> getDocumentsForTerm(String term) {
        return invertedIndex.getOrDefault(term, Collections.emptyList());
    }

    @Override
    public List<Integer> getTermPositions(String docId, String term) {
        Map<String, List<Integer>> docPositions = termPositions.getOrDefault(docId, Collections.emptyMap());
        return docPositions.getOrDefault(term, Collections.emptyList());
    }

    // Helper method for relevance scoring: Term Frequency
    public int getTermFrequency(String docId, String term) {
        List<Integer> positions = getTermPositions(docId, term);
        return positions.size();
    }

    // Helper method for relevance scoring: Inverse Document Frequency
    public double getInverseDocumentFrequency(String term) {
        int docFreq = documentFrequency.getOrDefault(term, 0);
        if (docFreq == 0) return 0.0;
        return Math.log((double) totalDocuments / docFreq);
    }
}