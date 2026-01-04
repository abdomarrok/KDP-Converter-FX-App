package com.boilerplate.app.service;

import com.boilerplate.app.config.AppConfig;
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
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * Service for managing persistent image cache with size limits and cleanup.
 */
public class ImageCacheService {

    private static final Logger logger = LogManager.getLogger(ImageCacheService.class);
    private final Path cacheDir;
    private final ScheduledExecutorService cleanupExecutor;

    private static ImageCacheService instance;
    private final AppConfig configService;

    private ImageCacheService() {
        this.configService = AppConfig.getInstance();

        // Use configured cache directory
        this.cacheDir = configService.getCacheDirectoryPath();

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
                1, 1, TimeUnit.HOURS);

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

        int maxRetries = configService.getExtractionRetryAttempts(); // Reusing for consistent network policy
        int retryDelay = configService.getExtractionRetryDelayMs();
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                // Generate filename from URL hash
                String filename = Math.abs(imageUrl.hashCode()) + ".png";
                Path cachedFile = cacheDir.resolve(filename);

                // Return existing file if it exists and is valid
                if (Files.exists(cachedFile) && Files.size(cachedFile) > 0) {
                    logger.debug("Using cached image: {}", filename);
                    return cachedFile.toUri().toString();
                }

                logger.info("Downloading image (attempt {}/{}): {}",
                        attempt, maxRetries + 1, imageUrl.substring(0, Math.min(60, imageUrl.length())));

                // Download image
                URL url = new URI(imageUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", configService.getHttpUserAgent());
                conn.setRequestProperty("Referer", "https://gemini.google.com/");
                conn.setConnectTimeout(configService.getHttpConnectTimeout());
                conn.setReadTimeout(configService.getHttpReadTimeout());

                try (InputStream in = conn.getInputStream();
                        OutputStream out = Files.newOutputStream(cachedFile)) {
                    in.transferTo(out);
                }

                long fileSize = Files.size(cachedFile);
                logger.info("Cached image: {} ({} bytes)", filename, fileSize);

                // Trigger cleanup if cache is getting large
                if (getCacheSize() > configService.getCacheCleanupThreshold()) {
                    cleanupIfNeeded();
                }

                // Post-processing: Remove watermark if enabled
                if (configService.isImageWatermarkRemovalEnabled()) {
                    try {
                        removeWatermark(cachedFile);
                    } catch (Exception e) {
                        logger.error("Failed to remove watermark from {}", filename, e);
                        // Continue with original image, don't fail the download
                    }
                }

                return cachedFile.toUri().toString();

            } catch (Exception e) {
                lastException = e;
                logger.warn("Download failed (attempt {}/{}): {}", attempt, maxRetries + 1, e.getMessage());

                if (attempt <= maxRetries) {
                    try {
                        // Exponential backoff
                        Thread.sleep(retryDelay * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }

        logger.error("Failed to download image after {} attempts: {}", maxRetries + 1, lastException.getMessage());
        return null;
    }

    private void removeWatermark(Path file) throws IOException {
        BufferedImage original = ImageIO.read(file.toFile());
        if (original == null)
            return; // Not an image format we understand

        int cropHeight = configService.getImageWatermarkCropBottomPixels();
        if (original.getHeight() <= cropHeight)
            return;

        // Crop bottom
        BufferedImage cropped = original.getSubimage(0, 0, original.getWidth(), original.getHeight() - cropHeight);

        // Write back (assuming PNG as we force .png extension)
        ImageIO.write(cropped, "png", file.toFile());
        logger.info("Removed watermark from {} (cropped {}px)", file.getFileName(), cropHeight);
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
            long cleanupThreshold = configService.getCacheCleanupThreshold();
            if (currentSize <= cleanupThreshold) {
                return;
            }

            logger.info("Cache size {} exceeds threshold {}, starting cleanup",
                    currentSize, cleanupThreshold);

            // Sort files by last modified time (oldest first)
            final long[] currentSizeRef = { currentSize }; // Use array to make effectively final
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

                            if (currentSizeRef[0] <= configService.getCacheCleanupThreshold()) {
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
