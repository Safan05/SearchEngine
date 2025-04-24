package Query_Processor;
import java.util.*;
import java.util.stream.Collectors;

public class QueryProcessor {
    private final Indexer indexer;
    private final Set<String> stopWords;
    private final PorterStemmer stemmer;

    public QueryProcessor(Indexer indexer) {
        this.indexer = indexer;
        this.stemmer = new PorterStemmer();
        this.stopWords = initializeStopWords();
    }

    private Set<String> initializeStopWords() {
        return new HashSet<>(Arrays.asList(
            "a", "an", "the", "and", "or", "but", "in", "on", "at", "to",
            "of", "for", "with", "as", "by", "is", "are", "was", "were",
            "be", "been", "being", "have", "has", "had", "do", "does", "did"
        ));
    }

    public SearchResults processQuery(String query) {
        long startTime = System.currentTimeMillis();
        
        boolean isPhraseSearch = query.startsWith("\"") && query.endsWith("\"");
        List<String> processedTerms = preprocessQuery(query, isPhraseSearch);
        
        List<Document> results;
        if (isPhraseSearch) {
            results = processPhraseQuery(processedTerms);
        } else {
            results = processStandardQuery(processedTerms);
        }
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        return new SearchResults(results, processingTime, query, isPhraseSearch);
    }

    private List<String> preprocessQuery(String query, boolean isPhraseSearch) {
        // Remove quotes if phrase search
        String processed = isPhraseSearch ? 
            query.substring(1, query.length() - 1) : 
            query;
        
        // Convert to lowercase and split into terms
        String[] tokens = processed.toLowerCase().split("\\s+");
        
        // Process each term
        List<String> terms = new ArrayList<>();
        for (String token : tokens) {
            if (!token.isEmpty()) {
                if (isPhraseSearch) {
                    terms.add(token); // Don't stem for phrase searches
                } else if (!stopWords.contains(token)) {
                    terms.add(stemmer.stem(token)); // Stem non-stopwords
                }
            }
        }
        
        return terms;
    }

    private List<Document> processStandardQuery(List<String> terms) {
        if (terms.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Get documents containing any of the terms (OR logic)
        Set<Document> allDocs = new HashSet<>();
        for (String term : terms) {
            List<Document> docs = indexer.getDocumentsForTerm(term);
            if (docs != null) {
                allDocs.addAll(docs);
            }
        }
        
        return new ArrayList<>(allDocs);
    }

    private List<Document> processPhraseQuery(List<String> terms) {
        if (terms.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Get documents containing all terms (AND logic)
        List<Document> commonDocs = new ArrayList<>(
            indexer.getDocumentsForTerm(terms.get(0)));
        
        for (int i = 1; i < terms.size() && !commonDocs.isEmpty(); i++) {
            List<Document> docs = indexer.getDocumentsForTerm(terms.get(i));
            commonDocs.retainAll(docs);
        }
        
        // Verify the phrase sequence exists in these documents
        List<Document> phraseDocs = new ArrayList<>();
        for (Document doc : commonDocs) {
            if (hasPhraseSequence(doc, terms)) {
                phraseDocs.add(doc);
            }
        }
        
        return phraseDocs;
    }

    private boolean hasPhraseSequence(Document doc, List<String> terms) {
        List<List<Integer>> positionsList = new ArrayList<>();
        for (String term : terms) {
            positionsList.add(indexer.getTermPositions(doc.getId(), term));
        }
        
        // Check if terms appear in sequence in the document
        for (int pos : positionsList.get(0)) {
            boolean sequenceFound = true;
            
            for (int i = 1; i < positionsList.size(); i++) {
                int expectedPos = pos + i;
                if (!positionsList.get(i).contains(expectedPos)) {
                    sequenceFound = false;
                    break;
                }
            }
            
            if (sequenceFound) {
                return true;
            }
        }
        
        return false;
    }

    // Helper method for relevance scoring (optional)
    protected double calculateRelevance(Document doc, List<String> queryTerms) {
        double score = 0.0;
        
        // Basic TF-IDF like scoring
        for (String term : queryTerms) {
            int tf = indexer.getTermFrequency(doc.getId(), term);
            double idf = indexer.getInverseDocumentFrequency(term);
            score += tf * idf;
        }
        
        // Boost score if term is in title
        String title = doc.getTitle().toLowerCase();
        for (String term : queryTerms) {
            if (title.contains(term)) {
                score += 2.0; // Title boost
            }
        }
        
        return score;
    }
}