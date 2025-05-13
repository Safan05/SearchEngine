package org.example;
import java.io.IOException;
import java.util.*;

// main to run indexer or run crawler then indexer
public class Main {
    public static void main(String[] args) throws IOException {
/*
        System.out.println("If you want to run crawler first press c, if you want to just run indexer press any key except c");
        Scanner scanner = new Scanner(System.in);
        String choice = scanner.nextLine();
        if(choice.equals("c")) {
           // Crawler crawler = new Crawler();
            Crawler.main(args);
        }
        Indexer indexer = new Indexer();
       // indexer

 */
        System.out.println(stemWord("Umm"));
    }
    public static String stemWord(String word) {
        Stemmer stemmer = new Stemmer();
        stemmer.add(word.toCharArray(), word.length());
        stemmer.stem();
        String stemmed = stemmer.toString();
        return stemmed;
    }
}