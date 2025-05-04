package org.example.tests;
public class ExactSentenceSearch {

    public static int findExactSentence(String sentence, String text) {
        sentence = sentence.trim();
        int sentenceLength = sentence.length();
        int textLength = text.length();

        if (sentenceLength == 0) {
            return textLength == 0 ? 0 : -1;
        }

        // Search through the text for exact matches
        int index = 0;
        while (index < textLength) {
            // Find the next occurrence of the sentence
            index = text.indexOf(sentence, index);
            if (index == -1) {
                break;
            }

            // Check boundaries - must be either:
            // 1. At start of text and sentence ends at text boundary or is followed by whitespace/punctuation
            // 2. Preceded by whitespace/punctuation and ends at text boundary or is followed by whitespace/punctuation
            boolean validStart = (index == 0) ||
                    isBoundaryCharacter(text.charAt(index - 1));
            boolean validEnd = (index + sentenceLength == textLength) ||
                    isBoundaryCharacter(text.charAt(index + sentenceLength));

            if (validStart && validEnd) {
                return index;
            }

            index++;
        }

        return -1;
    }

    private static boolean isBoundaryCharacter(char c) {
        // Consider whitespace or punctuation as boundary characters
        return Character.isWhitespace(c) ||
                c == '.' || c == '!' || c == '?' ||
                c == ',' || c == ';' || c == ':' ||
                c == '(' || c == ')' || c == '[' ||
                c == ']' || c == '{' || c == '}';
    }

    public static void main(String[] args) {
        // Test cases
        String text = "Hello worlds! This is a test. hello again. hello!";
        String [] arr = text.split("[\\s\\-_.!@#]+");
        for(String s: arr){
            System.out.println(s);
        }


    }
}