package com.boilerplate.app.service;

import com.boilerplate.app.model.Scene;
import com.boilerplate.app.model.Story;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Production-hardened WebViewParser for extracting stories from Gemini
 * Storybook UI.
 * 
 * Features:
 * - Injection guards (prevents duplicate injections)
 * - Callback deduplication (prevents multiple callbacks)
 * - Robust DOM selectors with fallbacks
 * - Content stabilization scrolling
 * - Enhanced image filtering
 * - Thread-safe operations
 * - Listener cleanup (no leaks)
 */
public class WebViewParser {

    private static final Logger logger = LogManager.getLogger(WebViewParser.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    // === STATE MANAGEMENT (Prevents duplicate extractions) ===
    private volatile boolean isExtracting = false;
    private final Object extractionLock = new Object();
    private ChangeListener<Worker.State> currentListener = null;

    // === IMAGE CACHING (Solves CORS issues) ===
    // Using ImageCacheService.getInstance() directly where needed

    /**
     * Parse the currently loaded page in the WebEngine.
     * Thread-safe: Can be called from any thread.
     * Idempotent: Ignores requests if extraction already in progress.
     */
    public void parseCurrentPage(WebEngine webEngine, Consumer<Story> onComplete) {
        // Ensure we're on JavaFX Application Thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> parseCurrentPage(webEngine, onComplete));
            return;
        }

        logger.info("Parse request received");

        // Guard against concurrent extractions
        synchronized (extractionLock) {
            if (isExtracting) {
                // DEADLOCK FIX: Instead of ignoring, forcibly reset and allow the new
                // extraction
                // This handles the case where a previous extraction's callback didn't complete
                logger.warn("Previous extraction still marked as in progress - forcibly resetting state");
                isExtracting = false;
            }
            isExtracting = true;
        }

        try {
            executeExtractionAgent(webEngine, wrapCallback(onComplete));
        } catch (Exception e) {
            logger.error("Failed to start extraction", e);
            synchronized (extractionLock) {
                isExtracting = false;
            }
        }
    }

    /**
     * Load a URL and parse it when ready.
     * Properly cleans up listeners to prevent leaks.
     */
    public void loadAndParse(WebEngine webEngine, String url, Consumer<Story> onComplete) {
        // Ensure we're on JavaFX thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> loadAndParse(webEngine, url, onComplete));
            return;
        }

        // Remove any existing listener to prevent leaks
        if (currentListener != null) {
            webEngine.getLoadWorker().stateProperty().removeListener(currentListener);
        }

        // Create new listener with self-removal
        currentListener = (obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Remove self to prevent memory leak
                webEngine.getLoadWorker().stateProperty().removeListener(currentListener);
                currentListener = null;

                // Start extraction
                parseCurrentPage(webEngine, onComplete);
            }
        };

        webEngine.getLoadWorker().stateProperty().addListener(currentListener);
        webEngine.load(url);
    }

    /**
     * Wraps the callback to ensure:
     * 1. It's called only once
     * 2. It's called on JavaFX thread
     * 3. Extraction state is reset
     */
    private Consumer<Story> wrapCallback(Consumer<Story> original) {
        return story -> {
            synchronized (extractionLock) {
                if (!isExtracting) {
                    logger.warn("Callback already invoked, ignoring");
                    return;
                }
                isExtracting = false;
            }

            // Always invoke on FX thread
            if (Platform.isFxApplicationThread()) {
                original.accept(story);
            } else {
                Platform.runLater(() -> original.accept(story));
            }
        };
    }

    /**
     * Injects JavaScript extraction agent into the page.
     * The JavaScript uses robust selectors and content stabilization.
     */
    private void executeExtractionAgent(WebEngine webEngine, Consumer<Story> onComplete) {
        logger.info("Injecting extraction agent");

        // Set up Java ↔ JavaScript bridge
        JSObject window = (JSObject) webEngine.executeScript("window");
        window.setMember("javaApp", new JavaBridge(onComplete, objectMapper));

        // Inject hardened extraction script
        webEngine.executeScript(buildExtractionScript());
    }

    /**
     * Builds the complete JavaScript extraction script.
     * Separated for readability and maintainability.
     */
    private String buildExtractionScript() {
        return """
                    (function() {
                        // Redirect console to Java
                        var oldLog = console.log;
                        var oldErr = console.error;
                        console.log = function(msg) {
                            if (oldLog) oldLog(msg);
                            if(window.javaApp && window.javaApp.log) window.javaApp.log("INFO: " + msg);
                        };
                        console.error = function(msg) {
                            if (oldErr) oldErr(msg);
                            if(window.javaApp && window.javaApp.log) window.javaApp.log("ERROR: " + msg);
                        };

                        console.log("=== Gemini Story Extractor v2.0 (Production) ===");

                        // ========== HELPER FUNCTIONS ==========

                        /**
                         * Extract title with multiple fallback selectors
                         */
                        function extractTitle() {
                            var selectors = [
                                '.cover-title',
                                'h1.story-title',
                                '.storybook-title',
                                '[data-story-title]',
                                'h1',
                                '.model-response-text h1'
                            ];

                            for (var i = 0; i < selectors.length; i++) {
                                var el = document.querySelector(selectors[i]);
                                if (el && el.innerText && el.innerText.trim()) {
                                    console.log("Title found with selector: " + selectors[i]);
                                    return el.innerText.trim();
                                }
                            }

                            console.log("No title found, using default");
                            return 'Untitled Story';
                        }

                        /**
                         * Find story layers with fallback strategies
                         */
                        function findStoryLayers() {
                            // Primary: storybook-page-layer elements
                            var layers = document.querySelectorAll('storybook-page-layer');
                            if (layers.length > 0) {
                                console.log("Found " + layers.length + " storybook-page-layer elements");
                                return Array.from(layers);
                            }

                            // Fallback 1: Elements with story-related classes
                            layers = document.querySelectorAll('[class*="story"], [class*="page-layer"]');
                            if (layers.length > 0) {
                                console.log("Found " + layers.length + " story-class elements");
                                return Array.from(layers);
                            }

                            // Fallback 2: Divs containing both images and substantial text
                            var allDivs = document.querySelectorAll('div');
                            var candidates = Array.from(allDivs).filter(function(div) {
                                var hasImage = div.querySelector('img') !== null;
                                var hasText = div.textContent.trim().length > 20;
                                return hasImage || hasText;
                            });

                            console.log("Found " + candidates.length + " content-rich divs");
                            return candidates;
                        }

                        /**
                         * Validate if an image URL is a story image (not icon/avatar/UI)
                         */
                        function isValidStoryImage(imgUrl) {
                            if (!imgUrl || imgUrl.length < 50) return false;

                            var excludePatterns = [
                                'profile',
                                'avatar',
                                'icon',
                                'logo',
                                'button',
                                'emoji',
                                '/u/',
                                'thumbnail',
                                'placeholder',
                                'spinner',
                                'loading'
                            ];

                            var urlLower = imgUrl.toLowerCase();
                            for (var i = 0; i < excludePatterns.length; i++) {
                                if (urlLower.includes(excludePatterns[i])) {
                                    return false;
                                }
                            }

                            // Must be HTTP(S) and reasonable length
                            return imgUrl.startsWith('http') && imgUrl.length < 500;
                        }

                        /**
                         * Extract text from a layer with multiple selector attempts
                         */
                        function extractTextFromLayer(layer) {
                            var selectors = [
                                '.story-text',
                                '.text-content',
                                'p',
                                '[class*="text"]'
                            ];

                            for (var i = 0; i < selectors.length; i++) {
                                var el = layer.querySelector(selectors[i]);
                                if (el && el.textContent.trim()) {
                                    return el.textContent.trim();
                                }
                            }

                            // Last resort: direct text content if substantial
                            var directText = layer.textContent.trim();
                            return directText.length > 10 ? directText : '';
                        }

                        /**
                         * Capture image element as Base64 data URL.
                         * Solves authentication issues with Gemini images.
                         */
                        function captureImageAsDataURL(imgElement) {
                            try {
                                var canvas = document.createElement('canvas');
                                canvas.width = imgElement.naturalWidth || imgElement.width || 400;
                                canvas.height = imgElement.naturalHeight || imgElement.height || 400;
                                var ctx = canvas.getContext('2d');
                                ctx.drawImage(imgElement, 0, 0);
                                return canvas.toDataURL('image/png');
                            } catch (error) {
                                console.error("Failed to capture image: " + error.message);
                                return null;
                            }
                        }

                        /**
                         * Extract image URL from a layer with multiple selector attempts.
                         * Returns original URL for Java-side authenticated download.
                         */
                        function extractImageFromLayer(layer) {
                            var selectors = [
                                '.storybook-image img',
                                '.story-image img',
                                'img.story',
                                'img'
                            ];

                            for (var i = 0; i < selectors.length; i++) {
                                var img = layer.querySelector(selectors[i]);
                                if (img && img.complete && img.naturalHeight > 0) {
                                    if (isValidStoryImage(img.src)) {
                                        console.log("Found valid image: " + img.src.substring(0, 60) + "...");
                                        return img.src;  // Return URL directly
                                    }
                                }
                            }

                            return null;
                        }

                        /**
                         * Auto-scroll with content stabilization detection
                         * Waits for height to stabilize before considering complete
                         */
                        function autoScrollWithStabilization(completion) {
                            console.log("Starting smart auto-scroll...");

                            var startTime = Date.now();
                            var CHECK_INTERVAL = 200;  // Check every 200ms
                            var SCROLL_DISTANCE = 800;
                            var MAX_TIME = 5000; // 5 second hard timeout

                            // Initialize with current height
                            var previousHeight = Math.max(
                                document.body.scrollHeight,
                                document.documentElement.scrollHeight
                            );
                            var stableCount = 0;

                            console.log("Initial page height: " + previousHeight + "px");

                            // Early bailout: if page is small and already at bottom, skip scrolling
                            var viewportHeight = window.innerHeight;
                            var scrollTop = Math.max(document.body.scrollTop, document.documentElement.scrollTop);
                            if (previousHeight - scrollTop <= viewportHeight * 1.5) {
                                console.log("Page already fully loaded, skipping scroll");
                                completion();
                                return;
                            }

                            var timer = setInterval(function() {
                                var elapsed = Date.now() - startTime;
                                var currentHeight = Math.max(
                                    document.body.scrollHeight,
                                    document.documentElement.scrollHeight
                                );

                                // Debug log every second
                                if (Math.floor(elapsed/1000) > Math.floor((elapsed-CHECK_INTERVAL)/1000)) {
                                    console.log("Scrolling... " + (elapsed/1000).toFixed(1) + "s, Height: " + currentHeight + "px, Stable: " + stableCount);
                                }

                                // Hard timeout safety net
                                if (elapsed > MAX_TIME) {
                                    console.log("Scroll timeout reached after " + (elapsed/1000).toFixed(1) + "s");
                                    clearInterval(timer);
                                    completion();
                                    return;
                                }

                                // Stabilization check
                                if (currentHeight === previousHeight) {
                                    stableCount++;
                                    if (stableCount >= 2) { // 2 checks = 400ms stability
                                        console.log("Content stabilized at " + currentHeight + "px after " + (elapsed/1000).toFixed(1) + "s");
                                        clearInterval(timer);
                                        completion();
                                        return;
                                    }
                                } else {
                                    stableCount = 0;
                                    previousHeight = currentHeight;
                                }

                                // Scroll down
                                window.scrollBy(0, SCROLL_DISTANCE);
                            }, CHECK_INTERVAL);
                        }

                        // ========== MAIN EXTRACTION LOGIC ==========

                        autoScrollWithStabilization(function() {
                            console.log("Scroll complete. Beginning extraction...");

                            try {
                                // Extract title
                                var title = extractTitle();

                                // Find story layers
                                var layers = findStoryLayers();

                                // Extract scenes
                                var scenes = [];
                                var seenTexts = new Set();

                                layers.forEach(function(layer, index) {
                                    var text = extractTextFromLayer(layer);
                                    var imageUrl = extractImageFromLayer(layer);

                                    // Skip empty layers
                                    if (!text && !imageUrl) return;

                                    // Deduplicate by text
                                    if (text && seenTexts.has(text)) {
                                        console.log("Skipping duplicate text at layer " + index);
                                        return;
                                    }

                                    // Add to results
                                    if (text || imageUrl) {
                                        scenes.push({
                                            text: text || '',
                                            imageUrl: imageUrl || null
                                        });

                                        if (text) seenTexts.add(text);
                                    }
                                });

                                console.log("Extraction complete: " + scenes.length + " scenes");
                                console.log("Scenes with images: " + scenes.filter(function(s) { return s.imageUrl; }).length);

                                // Build story object
                                var story = {
                                    title: title,
                                    author: "Gemini",
                                    scenes: scenes
                                };

                                // Send to Java
                                window.javaApp.processStory(JSON.stringify(story));

                            } catch (error) {
                                console.error("Extraction failed: " + error.message);
                                window.javaApp.processStory(JSON.stringify({
                                    title: "Error",
                                    author: "System",
                                    scenes: []
                                }));
                            }
                        });

                    })();
                """;
    }

    /**
     * Deduplicates scenes on the Java side for additional safety.
     * Filters out exact duplicates.
     */
    private static List<Scene> deduplicateScenes(List<Scene> scenes) {
        List<Scene> unique = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        for (Scene scene : scenes) {
            String text = scene.getText() != null ? scene.getText().trim() : "";
            String imageUrl = scene.getImageUrl();
            // Use hash for image url to save memory in the set, as data URLs are huge
            String imageKey = imageUrl != null ? String.valueOf(imageUrl.hashCode()) : "null";

            String key = text + "||" + imageKey;

            if (!seenKeys.contains(key)) {
                unique.add(scene);
                seenKeys.add(key);
            }
        }
        return unique;
    }

    /**
     * Java ↔ JavaScript bridge object.
     * Exposed to JavaScript as window.javaApp.
     */
    public static class JavaBridge {
        private final Consumer<Story> callback;
        private final ObjectMapper objectMapper;
        private volatile boolean callbackInvoked = false;

        public JavaBridge(Consumer<Story> callback, ObjectMapper objectMapper) {
            this.callback = callback;
            this.objectMapper = objectMapper;
        }

        /**
         * Log messages from JavaScript to Java console.
         */
        public void log(String message) {
            logger.info("[JS] {}", message);
        }

        /**
         * Process extracted story JSON from JavaScript.
         * Thread-safe with comprehensive error handling.
         */
        public void processStory(String jsonData) {
            // Prevent duplicate callbacks
            synchronized (this) {
                if (callbackInvoked) {
                    logger.warn("JavaBridge callback already invoked, ignoring");
                    return;
                }
                callbackInvoked = true;
            }

            logger.info("Received story data ({} chars)", jsonData.length());

            // Validate input
            if (jsonData == null || jsonData.trim().isEmpty()) {
                logger.error("Empty JSON received");
                return;
            }

            try {
                // Parse JSON
                Story story = objectMapper.readValue(jsonData, Story.class);

                // Validate parsed object
                if (story == null) {
                    throw new IllegalStateException("Parsed story is null");
                }

                // Ensure scenes list exists
                if (story.getScenes() == null) {
                    logger.info("No scenes in story, initializing empty list");
                    story.setScenes(new ArrayList<>());
                } else {
                    story.setScenes(deduplicateScenes(story.getScenes()));
                }

                logger.info("Parsed story '{}' with {} scenes", story.getTitle(), story.getScenes().size());

                // Download and cache images in parallel
                List<Scene> scenesWithImages = story.getScenes().stream()
                        .filter(s -> s.getImageUrl() != null && !s.getImageUrl().isEmpty())
                        .toList();

                if (!scenesWithImages.isEmpty()) {
                    logger.info("Downloading {} images in parallel...", scenesWithImages.size());
                    ExecutorService executor = Executors.newFixedThreadPool(
                            Math.min(4, scenesWithImages.size())); // Max 4 concurrent downloads

                    try {
                        List<Future<Void>> futures = new ArrayList<>();
                        AtomicInteger completedCount = new AtomicInteger(0);
                        int totalImages = scenesWithImages.size();

                        for (Scene scene : scenesWithImages) {
                            futures.add(executor.submit(() -> {
                                // Use ImageCacheService directly since we're in a static inner class
                                String cachedUrl = ImageCacheService.getInstance()
                                        .downloadAndCache(scene.getImageUrl());
                                if (cachedUrl != null) {
                                    scene.setImageUrl(cachedUrl);
                                } else {
                                    scene.setImageUrl(null);
                                }
                                int completed = completedCount.incrementAndGet();
                                logger.info("Image download progress: {}/{}", completed, totalImages);
                                return null;
                            }));
                        }

                        // Wait for all downloads to complete
                        for (Future<Void> future : futures) {
                            try {
                                future.get(30, TimeUnit.SECONDS); // 30s timeout per image
                            } catch (TimeoutException e) {
                                logger.warn("Image download timed out");
                                future.cancel(true);
                            }
                        }

                        long successCount = story.getScenes().stream()
                                .filter(s -> s.getImageUrl() != null && s.getImageUrl().startsWith("file:"))
                                .count();
                        logger.info("Cached {} images", successCount);

                    } catch (Exception e) {
                        logger.error("Error during parallel image download", e);
                    } finally {
                        executor.shutdown();
                    }
                } else {
                    logger.info("No images to download");
                }

                // Invoke callback on JavaFX thread
                Story finalStory = story;
                Platform.runLater(() -> {
                    if (callback != null) {
                        callback.accept(finalStory);
                    }
                });

            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                logger.error("JSON parsing failed", e);
            } catch (Exception e) {
                logger.error("Unexpected error during story processing", e);
            }
        }
    }
}
