package org.example.indexer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoDatabase {
    private static MongoClient mongoClient = null;
    private static MongoDatabase database = null;

    public static MongoDatabase getConnection() {
        if (database != null) {
            return database;
        }

        String connectionString = "mongodb://localhost:27017";
        String dbName = "searchengineapp";

        try {
            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase(dbName);
            return database;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}