package com.example.searchapi.Query_Processor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextSnippetExtractor {

    // Function to get snippets with the word bolded
    public static List<String> getSnippetsWithBoldWord(String url, String word, int snippetLength) throws IOException {
        // Parse the webpage using Jsoup
        Document doc = Jsoup.connect(url).get();
        // Extract all text from the webpage
        String text = doc.body().text();

        // List to store snippets
        List<String> snippets = new ArrayList<>();

        // Create a pattern to find the word (case-insensitive)
        Pattern pattern = Pattern.compile("(?i)\\b" + Pattern.quote(word) + "\\b");
        Matcher matcher = pattern.matcher(text);

        // Find all occurrences of the word
        while (matcher.find()) {
            int wordStart = matcher.start();
            int wordEnd = matcher.end();

            // Calculate the snippet boundaries
            int snippetStart = Math.max(0, wordStart - snippetLength / 2);
            int snippetEnd = Math.min(text.length(), wordEnd + snippetLength / 2);

            // Adjust snippet to start at a word boundary
            if (snippetStart > 0) {
                int spaceIndex = text.lastIndexOf(" ", snippetStart);
                snippetStart = (spaceIndex == -1) ? 0 : spaceIndex + 1;
            }
            if (snippetEnd < text.length()) {
                int spaceIndex = text.indexOf(" ", snippetEnd);
                snippetEnd = (spaceIndex == -1) ? text.length() : spaceIndex;
            }

            // Extract the snippet
            String snippet = text.substring(snippetStart, snippetEnd);

            // Bold the word in the snippet
            String boldedSnippet = snippet.replaceAll("(?i)\\b" + Pattern.quote(word) + "\\b", "<b>$0</b>");

            // Add "..." at the start/end if the snippet is cut off
            if (snippetStart > 0) boldedSnippet = "..." + boldedSnippet;
            if (snippetEnd < text.length()) boldedSnippet = boldedSnippet + "...";

            snippets.add(boldedSnippet);
        }

        return snippets;
    }

    // Example usage
    public static void main(String[] args) throws IOException {
        String url = "https://www.w3schools.com/java/";
        String searchWord = "java";
        int snippetLength = 100; // Total characters around the word

        List<String> snippets = getSnippetsWithBoldWord(url, searchWord, snippetLength);
        if (snippets.isEmpty()) {
            System.out.println("No snippets found for the word: " + searchWord);
        } else {
            for (String snippet : snippets) {
                System.out.println(snippet);
            }
        }
    }
}