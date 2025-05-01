package com.example.searchapi.controller;

import com.example.searchapi.model.SearchResponse;
import com.example.searchapi.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping
    public SearchResponse search(@RequestParam("query") String query) {
        System.out.println("Search query: " + query);
        return searchService.processSearchQuery(query);
    }
}