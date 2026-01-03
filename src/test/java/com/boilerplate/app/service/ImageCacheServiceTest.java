package com.boilerplate.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

class ImageCacheServiceTest {

    @Test
    void testGetInstance() {
        ImageCacheService service1 = ImageCacheService.getInstance();
        ImageCacheService service2 = ImageCacheService.getInstance();
        
        // Should return same instance (singleton)
        assertSame(service1, service2);
    }

    @Test
    void testGetCacheDirectory() {
        ImageCacheService service = ImageCacheService.getInstance();
        Path cacheDir = service.getCacheDirectory();
        
        assertNotNull(cacheDir);
        assertTrue(cacheDir.toString().contains("storyforge"));
    }

    @Test
    void testDownloadAndCacheWithInvalidUrl() {
        ImageCacheService service = ImageCacheService.getInstance();
        
        // Invalid URL should return null
        assertNull(service.downloadAndCache(null));
        assertNull(service.downloadAndCache(""));
        assertNull(service.downloadAndCache("not-a-url"));
    }

    // Note: Testing actual downloads would require network access
    // and is better suited for integration tests
}

