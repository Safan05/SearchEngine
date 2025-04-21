package org.example;

import org.jsoup.nodes.Document;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Base64;

public class Normalize {

    /**
     * Generates a fingerprint for web page content to detect duplicates
     * @param doc The Jsoup document object
     * @return Base64-encoded MD5 hash of normalized content
     */
    public static String generateSiteFingerprint(Document doc) {
        try {
            // Get important parts of the page
            String content = doc.title() + doc.select("body").text();

            // Normalize content (remove accents, trim, lowercase)
            content = normalizeContent(content);

            // Create hash fingerprint
            return generateMD5Hash(content);
        } catch (NoSuchAlgorithmException e) {
            return doc.location(); // Fallback to URL if hashing fails
        }
    }

    /**
     * Normalizes text content for comparison
     * @param content Raw text content
     * @return Normalized string
     */
    private static String normalizeContent(String content) {
        return Normalizer.normalize(content, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("\\s+", " ") // Collapse whitespace
                .toLowerCase()
                .trim();
    }

    /**
     * Generates MD5 hash of content
     * @param content Normalized text content
     * @return Base64-encoded hash
     */
    private static String generateMD5Hash(String content) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hashBytes = md.digest(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashBytes);
    }

    /**
     * Normalizes URLs for consistent comparison
     * @param url Raw URL string
     * @return Normalized URL
     */
    public static String normalizeUrl(String url) {
        if (url == null) return "";

        return url.toLowerCase()
                .replaceAll("https?://", "") // Remove protocol
                .replaceAll("www\\.", "")    // Remove www
                .split("#")[0]               // Remove fragments
                .split("\\?")[0]             // Remove query params
                .replaceAll("/+$", "");      // Remove trailing slashes
    }
}