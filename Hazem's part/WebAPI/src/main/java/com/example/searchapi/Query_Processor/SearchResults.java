package com.example.searchapi.Query_Processor;

import java.util.List;

public class SearchResults {
    private List<Document> results;
    private long processingTime;
    private String originalQuery;
    private boolean isPhraseSearch;
    
    public SearchResults(List<Document> results, long processingTime, 
                        String originalQuery, boolean isPhraseSearch) {
        this.results = results;
        this.processingTime = processingTime;
        this.originalQuery = originalQuery;
        this.isPhraseSearch = isPhraseSearch;
    }
    
    // Getters
    public List<Document> getResults() { return results; }
    public long getProcessingTime() { return processingTime; }
    public String getOriginalQuery() { return originalQuery; }
    public boolean isPhraseSearch() { return isPhraseSearch; }
    public int getResultCount() { return results.size(); }
}
