package com.example.searchapi.controller;

import com.example.searchapi.model.SearchResponse;
import com.example.searchapi.service.SearchService;
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