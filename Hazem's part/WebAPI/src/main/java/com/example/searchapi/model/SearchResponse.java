package com.example.searchapi.model;


import java.util.List;
import org.bson.Document;


public class SearchResponse {

    private int total;
    private List<Document> results;


    public SearchResponse(List<Document> results, int total) {
        this.results = results;
        this.total = total;
    }
    public SearchResponse() {

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