package org.example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.Jsoup;

// main to run indexer or run crawler then indexer
public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("If you want to run crawler first press c, if you want to just run indexer press any key except c");
        Scanner scanner = new Scanner(System.in);
        String choice = scanner.nextLine();
        if(choice.equals("c")) {
           // Crawler crawler = new Crawler();
            Crawler.main(args);
        }
        Indexer indexer = new Indexer();
       // indexer
    }
}