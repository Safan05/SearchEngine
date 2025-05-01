package com.example.searchapi.model;

import com.example.searchapi.Query_Processor.Document;
import java.util.List;

public class SearchResponse {
//    private List<SearchResult> results;
    private int total;
    private List<Document> results;

    // Constructor
    public SearchResponse(List<Document> results, int total) {
        this.results = results;
        this.total = total;
    }

    // Getters and setters
    public List<Document> getResults() {
        return results;
    }

    public void setResults(List<Document> results) {
        this.results = results;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}