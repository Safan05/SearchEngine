package org.example.controller;

import org.example.model.SearchResponse;
import org.example.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private final SearchService searchService = new SearchService();

    @GetMapping
    public SearchResponse search(@RequestParam("query") String query) throws IOException {
        System.out.println("Search query: " + query);
        return searchService.processSearchQuery(query);
    }


}