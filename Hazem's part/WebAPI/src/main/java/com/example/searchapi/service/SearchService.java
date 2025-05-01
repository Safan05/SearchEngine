package com.example.searchapi.service;

import com.example.searchapi.model.SearchResponse;
import org.springframework.stereotype.Service;
import com.example.searchapi.Query_Processor.*;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    public SearchResponse processDemoSearchQuery(String query) {
        // Generate 15 demo results
        List<Document> results = new ArrayList<>();
        int total = 15;

        for (int i = 0; i < total; i++) {
            int id = i + 1;
            String title = "Search Result #" + id + " for " + query;
            String url = "https://example.com/result#" + id;
            String snippet = "This is a sample result for your search query <b>" + query + "</b>. Result number #" + id + " shows how the snippet might look with highlighted terms..";

            results.add(new Document(Integer.toString(id), title, url, snippet));
        }

        return new SearchResponse(results, total);
    }

    public SearchResponse processSearchQuery(String query) {

        DemoDocuments demoDocuments = new DemoDocuments();
        IndexerImpl indexer = new IndexerImpl(demoDocuments.getDocuments());
        QueryProcessor queryProcessor = new QueryProcessor(indexer);

        SearchResults searchResults = queryProcessor.processQuery(query);

        return new SearchResponse(searchResults.getResults(), searchResults.getResultCount());
    }
}