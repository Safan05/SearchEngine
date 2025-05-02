package org.example;

import org.jsoup.nodes.Document;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Base64;
import java.net.URI;
import java.net.URISyntaxException;

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
        if (url == null || url.isEmpty()) {
            return "";
        }

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "http";
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
            String path = uri.getPath() != null ? uri.getPath() : "";

            // Remove www subdomain if present
            host = host.startsWith("www.") ? host.substring(4) : host;

            // Normalize path
            path = path.replaceAll("/+", "/");  // Remove duplicate slashes
            path = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;

            // Reconstruct URL
            return scheme + "://" + host + path;
        } catch (URISyntaxException e) {
            // Fallback to simple normalization if URI parsing fails
            return url.toLowerCase()
                    .replaceAll("https?://", "")
                    .replaceAll("www\\.", "")
                    .split("#")[0]
                    .split("\\?")[0]
                    .replaceAll("/+$", "");
        }
    }

    public static String getDomain(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }

        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            if (domain == null) {
                return "";
            }

            // Remove www subdomain if present
            domain = domain.startsWith("www.") ? domain.substring(4) : domain;

            // For country-code TLDs (e.g., .co.uk)
            if (domain.split("\\.").length > 2) {
                String[] parts = domain.split("\\.");
                domain = parts[parts.length - 2] + "." + parts[parts.length - 1];
            }

            return domain.toLowerCase();
        } catch (URISyntaxException e) {
            // Fallback to simple domain extraction
            String normalized = normalizeUrl(url);
            if (normalized.contains("/")) {
                normalized = normalized.split("/")[0];
            }
            return normalized;
        }
    }

    public static String getBaseUrl(String url) {
        String normalized = normalizeUrl(url);
        String domain = getDomain(url);

        if (normalized.startsWith(domain)) {
            int pathStart = normalized.indexOf('/', domain.length());
            if (pathStart > 0) {
                return normalized.substring(0, pathStart);
            }
        }
        return domain;
    }

    public static boolean isSameDomain(String url1, String url2) {
        return getDomain(url1).equals(getDomain(url2));
    }
}