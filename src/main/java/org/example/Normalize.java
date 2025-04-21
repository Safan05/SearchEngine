package org.example;

import org.jsoup.nodes.Document;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Base64;

public class Normalize {

    public static String generateSiteFingerprint(Document doc) {
        try {

            String content = doc.title() + doc.select("body").text();

            content = normalizeContent(content);

            return generateMD5Hash(content);
        } catch (NoSuchAlgorithmException e) {
            return doc.location();
        }
    }


    private static String normalizeContent(String content) {
        return Normalizer.normalize(content, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("\\s+", " ") // Collapse whitespace
                .toLowerCase()
                .trim();
    }


    private static String generateMD5Hash(String content) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hashBytes = md.digest(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashBytes);
    }


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