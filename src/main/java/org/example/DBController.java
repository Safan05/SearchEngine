package org.example;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;

import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;

import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;

import opennlp.tools.stemmer.PorterStemmer;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

import java.util.*;
import java.lang.Object;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.stereotype.Component;

@Component
public class DBController {
    public MongoClient mongoClient;
    public MongoDatabase database;
    public MongoCollection<Document> pageCollection;
    public MongoCollection<Document> termsCollection;
    public MongoCollection<Document> pagesCollection;
    public MongoCollection<Document> toVisitCollection;
    public PorterStemmer stemmer = new PorterStemmer();

    public void initializeDatabaseConnection() {
        //String url = "mongodb+srv://abdallahsafan05:a123456789@cluster0.qyomt.mongodb.net/";
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        database = mongoClient.getDatabase("SearchEngine");
        pagesCollection = database.getCollection("IndexedPages");
        pageCollection = database.getCollection("VisitedPages");
        termsCollection = database.getCollection("Terms");

        System.out.println("Connected to Database successfully");
    }
    public ObjectId storePageMetaInfo(String title, String url, String content,
                                      boolean[] headers, ArrayList<String> links) {
        // Create update document
        Document updateDoc = new Document()
                .append("title", title)
                .append("content", content)
                .append("headers", Arrays.asList(headers[0], headers[1], headers[2]))
                .append("lastIndexed", new Date())
                .append("links", links)
                .append("isIndexed", true); // Set isIndexed to true

        // Update existing document only (no upsert)
        UpdateResult result = pageCollection.updateOne(
                eq("URL", url), // Filter by URL
                new Document("$set", updateDoc) // Update fields
        );

        // Check if a document was updated
        if (result.getMatchedCount() > 0) {
            // Document was updated, retrieve its ID
            Document existing = pageCollection.find(eq("url", url)).first();
            return existing != null ? existing.getObjectId("_id") : null;
        } else {
            // No document found, return null
            System.out.println("No document found to update for URL: " + url);
            return null;
        }
    }

    public void storeTermInfo(String term, ObjectId pageId, String url,
                              String title, int frequency, double tf,
                              List<Integer> positions, boolean[] headers,List<String> snippets) {
        Document termDoc = new Document("term", term)
                .append("df", 1)
                .append("pages", Arrays.asList(
                        new Document()
                                .append("pageId", pageId)
                                .append("url", url)
                                .append("title", title)
                                .append("frequency", frequency)
                                .append("tf", tf)
                                .append("positions", positions)
                                .append("headers", Arrays.asList(headers[0], headers[1], headers[2]))
                                .append("snippets", snippets)
                ));

        termsCollection.updateOne(
                eq("term", term),
                new Document("$inc", new Document("df", 1))
                        .append("$push", new Document("pages", termDoc.get("pages", List.class).get(0))),
                new UpdateOptions().upsert(true)
        );
    }
    public List<String> getLinksFromPage(String url) {
        Document pageDoc = pageCollection.find(eq("URL", url)).first();

        if (pageDoc == null) return Collections.emptyList();

        List<String> links = new ArrayList<>();
        Object linksObj = pageDoc.get("links");

        if (linksObj instanceof List) {
            for (Object item : (List<?>) linksObj) {
                if (item instanceof String) {
                    links.add((String) item);
                }
            }
        }

        return links;
    }
    public void updatePageRank(String url, double pageRank) {
        pageCollection.updateOne(
                eq("URL", url),
                set("pageRank", pageRank)
        );
    }
    public void updateTermIDF(String term, double idf) {
        termsCollection.updateOne(
                eq("term", term),
                set("idf", idf)
        );

        // Update TF-IDF scores for all pages containing this term
        termsCollection.updateMany(
                eq("term", term),
                new Document("$set", new Document("pages.$[].tfidf",
                        new Document("$multiply", Arrays.asList("$pages.tf", idf))))
        );
    }

    public long getTotalDocumentCount() {
        return pageCollection.countDocuments();
    }

    public int getDocumentCountForTerm(String term) {
        Document termDoc = termsCollection.find(eq("term", term)).first();
        return termDoc != null ? termDoc.getInteger("df", 0) : 0;
    }

    public Set<String> getVisitedPages() {
        Set<String> visited = new HashSet<String>();
        pageCollection.find().projection(Projections.include("URL")).map(document -> document.getString("URL"))
                .into(visited);
        return visited.isEmpty() ? null : visited;
    }

    public Set<String> getCompactStrings() {
        Set<String> compactStrings = new HashSet<String>();
        pageCollection.find().projection(Projections.include("CompactString")).map(document -> document.getString("CompactString"))
                .into(compactStrings);
        return compactStrings;
    }


    public void addVisitedPage(String url, String title, String content, String fingerprint) {
        Document doc = new Document()
                .append("URL", url)
                .append("title", title)
                .append("content", content)
                .append("CompactString", fingerprint)
                .append("timestamp", new Date())
                .append("isIndexed", false);
        pageCollection.insertOne(doc);
    }
    public void resetVisitedPages(){
        try {
            DeleteResult result = pageCollection.deleteMany(new Document());
            System.out.println("Deleted " + result.getDeletedCount() + " documents");
        } catch (Exception e) {
            System.err.println("Error deleting documents: " + e.getMessage());
        }
    }
    public void closeConnection() {
        mongoClient.close();
        System.out.println("Connection with MongoDB is closed");
    }


}