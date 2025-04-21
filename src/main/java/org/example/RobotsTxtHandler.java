package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RobotsTxtHandler {
    private static final String USER_AGENT = "MyCrawler/1.0";
    private static final Pattern DISALLOW_PATTERN = Pattern.compile("Disallow:\\s*(.+)");
    private List<String> disallowedPaths = new ArrayList<>();
    private String baseUrl;

    public RobotsTxtHandler(String url) {
        this.baseUrl = getBaseUrl(url);
        fetchRobotsTxt();
    }

    private String getBaseUrl(String url) {
        try {
            URL u = new URL(url);
            return u.getProtocol() + "://" + u.getHost();
        } catch (Exception e) {
            return url;
        }
    }

    private void fetchRobotsTxt() {
        String robotsUrl = baseUrl + "/robots.txt";
        try {
            Document doc = Jsoup.connect(robotsUrl)
                    .userAgent(USER_AGENT)
                    .ignoreContentType(true)
                    .timeout(5000)
                    .get();

            // Parse robots.txt
            String[] lines = doc.text().split("\n");
            boolean ourUserAgent = false;

            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("User-agent:")) {
                    ourUserAgent = line.substring(11).trim().equals("*") ||
                            line.substring(11).trim().equals(USER_AGENT);
                } else if (ourUserAgent && DISALLOW_PATTERN.matcher(line).matches()) {
                    String path = line.substring(9).trim();
                    disallowedPaths.add(path);
                }
            }
        } catch (IOException e) {
            System.out.println("No robots.txt found or error fetching: " + robotsUrl);
        }
    }

    public boolean isAllowed(String url) {
        if (!url.startsWith(baseUrl)) {
            return true; // Different domain, no restrictions
        }

        String path = url.substring(baseUrl.length());
        for (String disallowed : disallowedPaths) {
            if (disallowed.equals("/")) {
                return false; // Entire site disallowed
            }
            if (path.startsWith(disallowed)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canCrawl(String url) {
        return new RobotsTxtHandler(url).isAllowed(url);
    }
}
