package com.boilerplate.app.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UrlValidatorTest {

    @Test
    void testValidUrl() {
        assertTrue(UrlValidator.isValidUrl("https://example.com"));
        assertTrue(UrlValidator.isValidUrl("http://example.com"));
        assertTrue(UrlValidator.isValidUrl("https://gemini.google.com/share"));
    }

    @Test
    void testInvalidUrl() {
        assertFalse(UrlValidator.isValidUrl(null));
        assertFalse(UrlValidator.isValidUrl(""));
        assertFalse(UrlValidator.isValidUrl("not a url"));
        assertFalse(UrlValidator.isValidUrl("ftp://example.com"));
    }

    @Test
    void testGeminiUrl() {
        assertTrue(UrlValidator.isGeminiUrl("https://gemini.google.com/share/abc123"));
        assertTrue(UrlValidator.isGeminiUrl("https://google.com/gemini/story"));
        assertFalse(UrlValidator.isGeminiUrl("https://example.com"));
    }

    @Test
    void testNormalizeUrl() {
        assertEquals("https://example.com", UrlValidator.normalizeUrl("example.com"));
        assertEquals("https://example.com", UrlValidator.normalizeUrl("https://example.com"));
        assertNull(UrlValidator.normalizeUrl(null));
        assertNull(UrlValidator.normalizeUrl(""));
        assertNull(UrlValidator.normalizeUrl("invalid url"));
    }
}

