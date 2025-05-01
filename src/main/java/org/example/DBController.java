package org.example;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;

import com.mongodb.client.result.UpdateResult;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;

import opennlp.tools.stemmer.PorterStemmer;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Updates.set;

import java.io.IOException;
import java.util.*;
import java.lang.Object;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Math.log;

import org.springframework.stereotype.Component;

@Component
public class DBController {
    public MongoClient mongoClient;
    public MongoDatabase database;
    public MongoCollection<Document> pageCollection;
    public MongoCollection<Document> termsCollection;
    public MongoCollection<Document> wordCollection;
    public MongoCollection<Document> pagesCollection;
    public MongoCollection<Document> queryHistoryCollection;
    public MongoCollection<Document> toVisitCollection;
    public PorterStemmer stemmer = new PorterStemmer();

    public void initializeDatabaseConnection() {
        //String url = "mongodb+srv://abdallahsafan05:a123456789@cluster0.qyomt.mongodb.net/";
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        database = mongoClient.getDatabase("SearchEngine");
        pagesCollection = database.getCollection("IndexedPages");
        pageCollection = database.getCollection("VisitedPages");
        termsCollection = database.getCollection("Terms");
      //  wordCollection = database.getCollection("Word");
      //  queryHistoryCollection = database.getCollection("QueryHistory");
        toVisitCollection = database.getCollection("PendingPages");

        System.out.println("Connected to Database successfully");
    }

    public void PrintCollectionData(String colName) {
        MongoCollection<Document> collections = database.getCollection(colName);
        try (MongoCursor<Document> cursor = collections.find()
                .iterator()) {
            while (cursor.hasNext()) {
                System.out.println(cursor.next().toJson());
            }
        }
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

    public BlockingQueue<String> getPendingPages() {
        BlockingQueue<String> pendingPages = new LinkedBlockingQueue<>();
        toVisitCollection.find().projection(Projections.include("URL")).map(document -> document.getString("URL"))
                .into(pendingPages);
        return pendingPages;
    }


    public List<Document> FindWordPages(String word) {
        Document projection = new Document("Pages", 1).append("_id", 0);

        FindIterable<Document> iterable = wordCollection.find(eq("Word", word)).projection(projection);

        List<Document> result = new ArrayList<>();
        iterable.into(result);
        if (!result.isEmpty()) {
            Document firstDocument = result.get(0);
            List<Document> pages = firstDocument.getList("Pages", Document.class);
            return pages;
        }
        return null;
    }

    public void closeConnection() {
        mongoClient.close();
        System.out.println("Connection with MongoDB is closed");
    }

    public boolean containsWord(String word) {
        Document doc = wordCollection.find(eq("word", word)).first();
        if (doc == null)
            return false;

        return true;
    }

    public long getNumPagesInWord(String word) { /// work correct 👌
        return FindWordPages(word).size();
    }

    public List<Document> getWords() { /// work correct 👌
        // get all the documents of words in database
        FindIterable<Document> iterable = wordCollection.find();
        List<Document> result = new ArrayList<>();
        iterable.into(result);
        return result;
    }

    public void updateIDF(double IDF, List<Document> pagesList, String word) {
        Bson updates = Updates.combine(Updates.set("IDF", IDF),
                Updates.set("Pages", pagesList));
        wordCollection.updateOne(eq("Word", word), updates);

    }

    public void updatePagesList(String word, List<Document> list) {
        Bson updates = Updates.combine(Updates.set("Pages", list));
        wordCollection.updateOne(eq("Word", word), updates);
    }

    public List<Document> getCrawlerPages() {
        FindIterable<Document> iterable = pageCollection.find();
        List<Document> result = new ArrayList<>();
        iterable.into(result);
        return result;
    }

    public void setIndexedAsTrue(ObjectId id) {
        Bson updates = Updates.combine(Updates.set("isIndexed", true));
        pageCollection.updateOne(eq("_id", id), updates);
    }

    public List<Document> getnonIndexedPages() {
        FindIterable<Document> iterable = pageCollection.find(eq("isIndexed", false));
        List<Document> result = new ArrayList<>();
        iterable.into(result);
        return result;
    }
    /**
     * Stores page metadata (title, URL, content) in the pages collection
     * @param title Page title
     * @param url Page URL
     * @param normalized Normalized page content
     */
    public void storePageMetaInfo(String title, String url, String normalized) {
        Document pageDoc = new Document()
                .append("title", title)
                .append("url", url)
                .append("content", normalized);
        pagesCollection.insertOne(pageDoc);
    }

    /**
     * Stores term frequencies for a page
     * @param termFrequency Map of terms to their frequencies
     * @param url URL of the page these terms belong to
     */
    public void storeTermFrequencies(Map<String, Integer> termFrequency, String url) {
        for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
            Document termDoc = new Document()
                    .append("term", entry.getKey())
                    .append("url", url)
                    .append("tf", entry.getValue());
            termsCollection.insertOne(termDoc);
        }
    }

    /**
     * Gets term frequencies for a specific URL
     * @param url URL to get term frequencies for
     * @return List of documents containing term frequency data
     */
    public List<Document> getTermFrequencies(String url) {
        return termsCollection.find(eq("url", url)).into(new ArrayList<>());
    }

    /**
     * Gets page metadata for a specific URL
     * @param url URL to get metadata for
     * @return Document containing page metadata or null if not found
     */
    public Document getPageMetaInfo(String url) {
        return pagesCollection.find(eq("url", url)).first();
    }
    /**
     * Adds a visited page to the database with all its metadata
     * @param url The normalized URL of the page
     * @param title The page title
     * @param content The page content
     * @param fingerprint The content fingerprint (compact string)
     */
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

    /**
     * Adds a URL to the pending pages collection
     * @param url The URL to add
     */
    public void addPendingPage(String url) {
        // First check if it's not already in pending or visited
        if (toVisitCollection.countDocuments(eq("URL", url)) == 0 &&
                pageCollection.countDocuments(eq("URL", url)) == 0) {
            Document doc = new Document()
                    .append("URL", url)
                    .append("addedAt", new Date());
            toVisitCollection.insertOne(doc);
        }
    }

    /**
     * Removes a URL from the pending pages collection
     * @param url The URL to remove
     */
    public void removePendingPage(String url) {
        toVisitCollection.deleteOne(eq("URL", url));
    }

    /**
     * Checks if a URL exists in either visited or pending collections
     * @param url The URL to check
     * @return true if exists, false otherwise
     */
    public boolean urlExists(String url) {
        return pageCollection.countDocuments(eq("URL", url)) > 0 ||
                toVisitCollection.countDocuments(eq("URL", url)) > 0;
    }

    /**
     * Gets the count of pending pages
     * @return count of pending pages
     */
    public long getPendingPagesCount() {
        return toVisitCollection.countDocuments();
    }

    /**
     * Gets the count of visited pages
     * @return count of visited pages
     */
    public long getVisitedPagesCount() {
        return pageCollection.countDocuments();
    }

    /**
     * Updates an existing visited page document
     * @param url The URL of the page to update
     * @param title The new title
     * @param content The new content
     * @param fingerprint The new fingerprint
     */
    public void updateVisitedPage(String url, String title, String content, String fingerprint) {
        Bson filter = eq("URL", url);
        Bson update = Updates.combine(
                Updates.set("title", title),
                Updates.set("content", content),
                Updates.set("CompactString", fingerprint),
                Updates.set("lastUpdated", new Date())
        );
        pageCollection.updateOne(filter, update);
    }
   /* public Set<ObjectId> searchByWords(String query) {
        String[] queryWords = query.split("\\s+");
        Set<ObjectId> commonPages = new HashSet<>();
        List<ObjectId> returnedPages = new ArrayList<>();

        for (String queryWord : queryWords) {
            if(ProcessingWords.isStopWord(queryWord))
                continue;
            queryWord = stemmer.stem(queryWord);
            returnedPages = getPageIDsPerWord(queryWord);

            if (returnedPages == null)
                continue;
            if (commonPages.isEmpty()) {
                commonPages.addAll(returnedPages);
            } else {
                commonPages.retainAll(new HashSet<>(returnedPages));
            }
        }
        return commonPages;
    }
*/
    public List<ObjectId> getPageIDsPerWord(String word) {
        List<ObjectId> pagesHavingWord = new ArrayList<>();
        FindIterable<Document> pageDocs;
        Bson filter = Filters.eq("Word", word);
        Bson projection = fields(include("Pages._id"), excludeId());
        pageDocs = wordCollection.find(filter).projection(projection);

        if (pageDocs.first() == null)
            return null;

        List<Document> pageArray = (List<Document>) (pageDocs.first().get("Pages"));

        if (pageArray == null)
            return null;

        for (Document doc : pageArray) {
            pagesHavingWord.add(doc.getObjectId("_id"));
        }
        return pagesHavingWord;
    }

    public void updateIDF() {
        Bson projection = fields(include("Word", "No_pages"), excludeId());
        MongoCursor<Document> cursor = wordCollection.find().projection(projection).iterator();
        int totalPages = 6000;
        while (cursor.hasNext()) {
            Document doc = cursor.next();
            int docFrequency = doc.getInteger("No_pages");
            double idf = log((double) pageCollection.countDocuments() / docFrequency);
            Bson filter = Filters.eq("Word", doc.getString("Word"));
            wordCollection.updateOne(filter, set("IDF", idf));
        }
    }

    public void updateTF() {
        Bson projection = fields(include("Word", "Pages"), excludeId());
        MongoCursor<Document> cursor = wordCollection.find().projection(projection).iterator();
        while (cursor.hasNext()) {
            Document doc = cursor.next();
            String word = doc.getString("Word");
            Object obj = doc.get("Pages");

            for (Document d : (List<Document>) obj) {
                int frequency = d.getInteger("Frequency");
                int totalWords = d.getInteger("Total_Words");
                Bson filter = Filters.and(eq("Word", word), eq("Pages.Doc_Id", d.getInteger("Doc_Id")));
                wordCollection.updateOne(filter, set("Pages.$.TF", (double) frequency / totalWords));
            }
        }
    }

    public void updateRank() {
        Bson projection = fields(include("Word", "IDF", "Pages.Doc_Id", "Pages.TF", "Pages.Rank"), excludeId());
        MongoCursor<Document> cursor = wordCollection.find().projection(projection).iterator();
        while (cursor.hasNext()) {
            Document doc = cursor.next();
            System.out.println(doc.toJson());
            String word = doc.getString("Word");
            double IDF = doc.getDouble("IDF");
            for (Document d1 : (List<Document>) doc.get("Pages")) {
                Bson f = Filters.and(eq("Word", word), eq("Pages.Doc_Id", d1.getInteger("Doc_Id")));
                double TF = d1.getDouble("TF");
                double rank = IDF * TF;
                wordCollection.updateOne(f, set("Pages.$.Rank", rank));
            }
        }
    }

    public MongoCursor<Document> getPagesInfoPerWord(String queryWord) {
        Bson filter = eq("StemmedWord", queryWord);
        Bson projection = fields(include("Word", "Pages.TF_IDF", "Pages._id", "Pages.Tag"), excludeId());
        MongoCursor<Document> cursor = wordCollection.find(filter).projection(projection).iterator();
        return cursor;
    }

    public Document findPageById(ObjectId id) {
        return pageCollection.find(eq("_id", id)).first();
    }

    public long updateQueryHistory(String query) {
        Bson filter = Filters.eq("Query", query);
        Bson update = Updates.inc("Popularity", 1);
        return queryHistoryCollection.updateOne(filter, update).getModifiedCount();
    }

    public MongoCursor<Document> getQueryHistory() {
        return queryHistoryCollection.find().iterator();
    }

    public void updateDocument(Bson filter,Bson update, String collectionName)
    {
        UpdateResult result;
        switch (collectionName) {
            case "VisitedPages":
                result = pageCollection.updateOne(filter,update);
                System.out.println("Updated a document with the following id: "
                        + Objects.requireNonNull(result.getUpsertedId()).asObjectId().getValue());
                break;
            case "Word":
                result = wordCollection.updateOne(filter,update);
                System.out.println("Updated a document with the following id: "
                        + Objects.requireNonNull(result.getUpsertedId()).asObjectId().getValue());
                break;
            case "QueryHistory":
                result = queryHistoryCollection.updateOne(filter,update);
                System.out.println("Updated a document with the following id: "
                        + Objects.requireNonNull(result.getUpsertedId()).asObjectId().getValue());
                break;
            case "PendingPages":
                result = toVisitCollection.updateOne(filter,update);
                System.out.println("Updated a document with the following id: "
                        + Objects.requireNonNull(result.getUpsertedId()).asObjectId().getValue());
                break;
        }    }
}