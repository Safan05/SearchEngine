package com.example.searchapi.Query_Processor;
import java.util.List;

public interface Indexer {
    List<Document> getDocumentsForTerm(String term);
    List<Integer> getTermPositions(String docId, String term);
}

