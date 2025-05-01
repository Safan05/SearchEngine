package com.example.searchapi.Query_Processor;

import java.util.ArrayList;
import java.util.List;

public class DemoDocuments {
    List<Document> documents = new ArrayList<>();
    public DemoDocuments() {
        documents.add(new Document("1", "Java Programming", "http://example.com/java", "Learn Java programming basics"));
        documents.add(new Document("2", "Python Basics", "http://example.com/python", "Introduction to Python programming"));
        documents.add(new Document("3", "Java Advanced", "http://example.com/java-advanced", "Advanced Java programming concepts"));
        documents.add(new Document("4", "Coding Tips", "http://example.com/coding", "Java and Python coding tips"));
    }

    public List<Document> getDocuments() { return documents; }

}
