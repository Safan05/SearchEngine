package com.example.searchapi.Query_Processor;
public class Document {
    private String id;
    private String title;
    private String url;
    private String snippet;
    
    // Constructor, getters, and setters
    public Document(String id, String title, String url, String snippet) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.snippet = snippet;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }
}