package org.example;
import java.util.*;
import java.util.regex.*;

public class TextProcessor {
    private static final Set<String> stopWords = new HashSet<>(Arrays.asList(
            "the", "is", "in", "at", "of", "on", "and", "a", "to", "an", "by", "for", "with", "it", "as"
    ));

    public static String normalize(String text) {
        text = text.toLowerCase();
        text = text.replaceAll("[^a-z\\s]", ""); // remove punctuation
        StringBuilder builder = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (!stopWords.contains(word) && word.length() > 2) {
                builder.append(stem(word)).append(" ");
            }
        }
        return builder.toString().trim();
    }

    // Basic stemming
    public static String stem(String word) {
        if (word.endsWith("ing") || word.endsWith("ed")) {
            return word.substring(0, word.length() - 3);
        }
        return word;
    }
}
