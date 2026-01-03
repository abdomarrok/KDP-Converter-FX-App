package com.boilerplate.app.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing persistent image cache with size limits and cleanup.
 */
public class ImageCacheService {
    
    private static final Logger logger = LogManager.getLogger(ImageCacheService.class);
    
    private static final String CACHE_DIR_NAME = "storyforge-images";
    private static final long MAX_CACHE_SIZE = 500 * 1024 * 1024; // 500 MB
    private static final long CLEANUP_THRESHOLD = 400 * 1024 * 1024; // 400 MB (cleanup when exceeds)
    
    private final Path cacheDir;
    private final ScheduledExecutorService cleanupExecutor;
    
    private static ImageCacheService instance;
    
    private ImageCacheService() {
        // Use user's home directory for persistent storage
        String userHome = System.getProperty("user.home");
        Path appDataDir = Paths.get(userHome, ".storyforge");
        this.cacheDir = appDataDir.resolve(CACHE_DIR_NAME);
        
        try {
            Files.createDirectories(cacheDir);
            logger.info("Image cache directory: {}", cacheDir);
        } catch (IOException e) {
            logger.error("Failed to create cache directory", e);
            throw new RuntimeException("Failed to initialize image cache", e);
        }
        
        // Schedule periodic cleanup
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ImageCacheCleanup");
            t.setDaemon(true);
            return t;
        });
        
        cleanupExecutor.scheduleWithFixedDelay(
            this::cleanupIfNeeded,
            1, 1, TimeUnit.HOURS
        );
        
        // Cleanup on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanupIfNeeded));
    }
    
    public static synchronized ImageCacheService getInstance() {
        if (instance == null) {
            instance = new ImageCacheService();
        }
        return instance;
    }
    
    /**
     * Downloads and caches an image from a URL.
     * Returns the local file:// URL or null if download fails.
     */
    public String downloadAndCache(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        
        try {
            // Generate filename from URL hash
            String filename = Math.abs(imageUrl.hashCode()) + ".png";
            Path cachedFile = cacheDir.resolve(filename);
            
            // Return existing file if it exists and is valid
            if (Files.exists(cachedFile) && Files.size(cachedFile) > 0) {
                logger.debug("Using cached image: {}", filename);
                return cachedFile.toUri().toString();
            }
            
            logger.info("Downloading image: {}", imageUrl.substring(0, Math.min(60, imageUrl.length())));
            
            // Download image
            URL url = new URI(imageUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (StoryForge/1.0)");
            conn.setRequestProperty("Referer", "https://gemini.google.com/");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            
            try (InputStream in = conn.getInputStream();
                 OutputStream out = Files.newOutputStream(cachedFile)) {
                in.transferTo(out);
            }
            
            long fileSize = Files.size(cachedFile);
            logger.info("Cached image: {} ({} bytes)", filename, fileSize);
            
            // Trigger cleanup if cache is getting large
            if (getCacheSize() > CLEANUP_THRESHOLD) {
                cleanupIfNeeded();
            }
            
            return cachedFile.toUri().toString();
            
        } catch (Exception e) {
            logger.error("Failed to download image: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets the total size of the cache directory.
     */
    private long getCacheSize() {
        try {
            return Files.walk(cacheDir)
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
        } catch (IOException e) {
            logger.error("Failed to calculate cache size", e);
            return 0;
        }
    }
    
    /**
     * Cleans up old files if cache exceeds threshold.
     * Uses LRU (Least Recently Used) strategy based on file modification time.
     */
    private void cleanupIfNeeded() {
        try {
            long currentSize = getCacheSize();
            if (currentSize <= CLEANUP_THRESHOLD) {
                return;
            }
            
            logger.info("Cache size {} exceeds threshold {}, starting cleanup", 
                currentSize, CLEANUP_THRESHOLD);
            
            // Sort files by last modified time (oldest first)
            final long[] currentSizeRef = {currentSize}; // Use array to make effectively final
            Files.walk(cacheDir)
                .filter(Files::isRegularFile)
                .sorted(Comparator.comparing(path -> {
                    try {
                        return Files.getLastModifiedTime(path);
                    } catch (IOException e) {
                        return FileTime.fromMillis(0);
                    }
                }))
                .forEach(path -> {
                    try {
                        long size = Files.size(path);
                        Files.delete(path);
                        currentSizeRef[0] -= size;
                        logger.debug("Deleted old cache file: {}", path.getFileName());
                        
                        if (currentSizeRef[0] <= CLEANUP_THRESHOLD) {
                            return; // Stop deleting once we're under threshold
                        }
                    } catch (IOException e) {
                        logger.warn("Failed to delete cache file: {}", path, e);
                    }
                });
            
            logger.info("Cleanup complete. New cache size: {}", getCacheSize());
            
        } catch (Exception e) {
            logger.error("Error during cache cleanup", e);
        }
    }
    
    /**
     * Clears all cached images.
     */
    public void clearCache() {
        try {
            Files.walk(cacheDir)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        logger.warn("Failed to delete: {}", path, e);
                    }
                });
            logger.info("Cache cleared");
        } catch (IOException e) {
            logger.error("Failed to clear cache", e);
        }
    }
    
    /**
     * Gets the cache directory path.
     */
    public Path getCacheDirectory() {
        return cacheDir;
    }
    
    /**
     * Shuts down the cleanup executor.
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
    }
}

