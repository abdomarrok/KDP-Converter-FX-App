package com.boilerplate.app.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * Utility class for validating URLs.
 */
public class UrlValidator {
    
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^https?://([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([/\\w \\.-]*)*/?$",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Validates if a string is a valid URL.
     * 
     * @param urlString The URL string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidUrl(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            return false;
        }
        
        try {
            URI uri = new URI(urlString);
            return uri.getScheme() != null && 
                   (uri.getScheme().equalsIgnoreCase("http") || 
                    uri.getScheme().equalsIgnoreCase("https"));
        } catch (URISyntaxException e) {
            return false;
        }
    }
    
    /**
     * Validates if a URL is a Gemini shared link.
     * 
     * @param urlString The URL string to validate
     * @return true if it appears to be a Gemini link, false otherwise
     */
    public static boolean isGeminiUrl(String urlString) {
        if (!isValidUrl(urlString)) {
            return false;
        }
        
        String lower = urlString.toLowerCase();
        return lower.contains("gemini.google.com") || 
               lower.contains("gemini") ||
               lower.contains("google.com");
    }
    
    /**
     * Normalizes a URL string (trims whitespace, adds protocol if missing).
     * 
     * @param urlString The URL string to normalize
     * @return Normalized URL or null if invalid
     */
    public static String normalizeUrl(String urlString) {
        if (urlString == null) {
            return null;
        }
        
        String trimmed = urlString.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        
        // Add https:// if no protocol specified
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }
        
        return isValidUrl(trimmed) ? trimmed : null;
    }
}

