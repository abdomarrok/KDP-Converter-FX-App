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
     * 
     * @param webEngine  The WebEngine to parse
     * @param onComplete Callback invoked when extraction completes
     */
    public void parseCurrentPage(WebEngine webEngine, Consumer<Story> onComplete) {
        parseCurrentPage(webEngine, onComplete, null);
    }

    /**
     * Parse the currently loaded page in the WebEngine with a refresh callback.
     * Thread-safe: Can be called from any thread.
     * Idempotent: Ignores requests if extraction already in progress.
     * 
     * @param webEngine  The WebEngine to parse
     * @param onComplete Callback invoked when extraction completes
     * @param onRefresh  Callback invoked after background image downloads complete
     *                   (optional), receives updated story
     */
    public void parseCurrentPage(WebEngine webEngine, Consumer<Story> onComplete, Consumer<Story> onRefresh) {
        // Ensure we're on JavaFX Application Thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> parseCurrentPage(webEngine, onComplete, onRefresh));
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
            executeExtractionAgent(webEngine, wrapCallback(onComplete), onComplete, onRefresh);
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
                parseCurrentPage(webEngine, onComplete, null);
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
    private void executeExtractionAgent(WebEngine webEngine, Consumer<Story> wrappedCallback,
            Consumer<Story> originalCallback, Consumer<Story> onRefresh) {
        logger.info("Injecting extraction agent");

        // Set up Java ↔ JavaScript bridge
        JSObject window = (JSObject) webEngine.executeScript("window");
        window.setMember("javaApp", new JavaBridge(wrappedCallback, originalCallback, onRefresh, objectMapper));

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
                         * Navigate through all storybook pages by clicking "Next page" button
                         * Gemini storybook is PAGINATED, not scrollable!
                         */



                        // Guard
                        if (window.__storyExtractorRunning) return;
                        window.__storyExtractorRunning = true;

                        function navigateAllPagesAndExtract(completion) {
                            console.log("Starting AGGRESSIVE navigation...");

                            var collectedScenes = [];
                            var sameTextCount = 0;
                            var pageCount = 0;
                            var maxPages = 50;


                            function extractAndMove() {
                                // Helper to find TRULY visible element (not just in DOM)
                                function getVisible(selector) {
                                    var els = Array.from(document.querySelectorAll(selector));
                                    return els.find(function(el) {
                                        if (!el.offsetParent) return false; // Element or ancestor is display:none

                                        var rect = el.getBoundingClientRect();
                                        if (rect.width === 0 || rect.height === 0) return false;

                                        var style = window.getComputedStyle(el);
                                        if (style.visibility === 'hidden' || style.opacity === '0') return false;

                                        // Check if element is in viewport (visible on screen)
                                        if (rect.bottom < 0 || rect.top > window.innerHeight) return false;
                                        if (rect.right < 0 || rect.left > window.innerWidth) return false;

                                        return true;
                                    });
                                }

                                // 1. Extract from VISIBLE elements
                                var imgEl = getVisible('div.storybook-image img') ||
                                            getVisible('storybook-page img') ||
                                            getVisible('img[src*="googleusercontent"]'); // Fallback

                                var imageUrl = (imgEl && imgEl.src && imgEl.src.startsWith('http')) ? imgEl.src : null;

                                var textEl = getVisible('div.story-text-container') ||
                                             getVisible('[class*="story-text"]') ||
                                             getVisible('p'); // Broad fallback if specific container missing

                                var text = textEl ? textEl.textContent.trim() : "";

                                console.log("Scanning Page " + (pageCount+1) + " [" + (imageUrl?"IMG":"") + " " + (text?"TXT":"") + "]");
                                console.log("Text preview: " + (text ? text.substring(0, 30) + "..." : "EMPTY"));
                                console.log("Image URL: " + (imageUrl ? imageUrl.substring(0, 50) + "..." : "NONE"));


                                // 2. Check for End (Stuck detection)
                                // If Text AND Image are identical to the previous capture, we are stuck/done.
                                var isSameAsLast = (
                                    collectedScenes.length > 0 &&
                                    text === collectedScenes[collectedScenes.length-1].text &&
                                    imageUrl === collectedScenes[collectedScenes.length-1].imageUrl
                                );

                                if (collectedScenes.length > 0) {
                                    console.log("Last scene text: " + (collectedScenes[collectedScenes.length-1].text ? collectedScenes[collectedScenes.length-1].text.substring(0, 30) + "..." : "EMPTY"));
                                }

                                if (isSameAsLast) {
                                    sameTextCount++;
                                    console.log("Content unchanged (" + sameTextCount + ")");
                                    if (sameTextCount >= 3) {
                                        console.log("Stuck on same content. Finished.");
                                        finish();
                                        return;
                                    }
                                } else {
                                    sameTextCount = 0;
                                    // Collect new scene
                                    collectedScenes.push({ text: text, imageUrl: imageUrl });
                                    console.log("Collected scene #" + collectedScenes.length);
                                }

                                if (pageCount >= maxPages) { finish(); return; }

                                // 3. Find Next Button
                                var nextBtn = document.querySelector('button[aria-label="Next page"]') ||
                                              document.querySelector('[aria-label="Next page"]');

                                if (!nextBtn) {
                                    console.log("Next button GONE.");
                                    finish();
                                    return;
                                }

                                // 4. Click Next
                                console.log("Clicking Next...");
                                try { nextBtn.click(); } catch(e) { console.log("Click fail: " + e); }

                                pageCount++;

                                // 5. Wait longer for page transition (increased to 5s)
                                setTimeout(extractAndMove, 5000);
                            }

                            function finish() {
                                window.__storyExtractorRunning = false;
                                completion(collectedScenes);
                            }


                            // Start
                            rewindAndStart();

                            function rewindAndStart() {
                                console.log("Rewinding to start...");
                                var attempts = 0;
                                var maxRewinds = 30;

                                function doRewind() {
                                    // Try to find the Previous button
                                    var prevBtn = document.querySelector('button[aria-label="Previous page"]') ||
                                                  document.querySelector('[aria-label="Previous page"]');

                                    // Check if we are at start (Previous disabled or missing)
                                    var isStart = !prevBtn ||
                                                  prevBtn.disabled ||
                                                  prevBtn.classList.contains('mat-mdc-button-disabled') ||
                                                  prevBtn.getAttribute('disabled') === 'true';

                                    // Also check page indicator if available (e.g. "1 / 10")
                                    var pageInd = document.body.innerText.match(/(\\d+)\\s*\\/\\s*\\d+/);
                                    if (pageInd && pageInd[1] === '1') {
                                        isStart = true;
                                    }

                                    if (isStart || attempts >= maxRewinds) {
                                        console.log("Rewind done. Starting extraction.");
                                        // Reset state
                                        pageCount = 0;
                                        collectedScenes = [];
                                        setTimeout(extractAndMove, 1000); // Give time for Page 1 to settle
                                        return;
                                    }

                                    // Click Previous
                                    if (prevBtn) {
                                        try { prevBtn.click(); } catch(e) {}
                                    }
                                    attempts++;
                                    setTimeout(doRewind, 200); // Fast rewind (200ms)
                                }
                                doRewind();
                            }
                        }





                        // ========== MAIN EXTRACTION LOGIC ==========
                        // Use paginated navigation instead of scrolling
                        navigateAllPagesAndExtract(function(collectedScenes) {
                            console.log("Navigation complete. Building story...");

                            try {
                                // Extract title
                                var title = extractTitle();

                                console.log("Extraction complete: " + collectedScenes.length + " scenes");
                                console.log("Scenes with images: " + collectedScenes.filter(function(s) { return s.imageUrl; }).length);

                                // Build story object
                                var story = {
                                    title: title,
                                    author: "Gemini",
                                    scenes: collectedScenes
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
        private final Consumer<Story> originalCallback; // For refresh after image download
        private final Consumer<Story> refreshCallback; // UI refresh callback after images download
        private final ObjectMapper objectMapper;
        private volatile boolean callbackInvoked = false;

        public JavaBridge(Consumer<Story> callback, Consumer<Story> originalCallback,
                Consumer<Story> refreshCallback, ObjectMapper objectMapper) {
            this.callback = callback;
            this.originalCallback = originalCallback;
            this.refreshCallback = refreshCallback;
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

                // IMMEDIATELY invoke callback so UI shows scenes right away
                // Images will be downloaded in background and updated progressively
                Story finalStory = story;
                Platform.runLater(() -> {
                    if (callback != null) {
                        callback.accept(finalStory);
                    }
                });

                // Download and cache images in background (non-blocking)
                List<Scene> scenesWithImages = story.getScenes().stream()
                        .filter(s -> s.getImageUrl() != null && !s.getImageUrl().isEmpty()
                                && !s.getImageUrl().startsWith("file:")) // Skip already cached
                        .toList();

                if (!scenesWithImages.isEmpty()) {
                    logger.info("Starting background download of {} images...", scenesWithImages.size());

                    // Use a separate thread for the download coordinator
                    new Thread(() -> {
                        ExecutorService executor = Executors.newFixedThreadPool(
                                Math.min(4, scenesWithImages.size()));
                        try {
                            AtomicInteger completedCount = new AtomicInteger(0);
                            int totalImages = scenesWithImages.size();

                            for (Scene scene : scenesWithImages) {
                                executor.submit(() -> {
                                    String cachedUrl = ImageCacheService.getInstance()
                                            .downloadAndCache(scene.getImageUrl());
                                    if (cachedUrl != null) {
                                        scene.setImageUrl(cachedUrl);
                                    }
                                    int completed = completedCount.incrementAndGet();
                                    logger.info("Image download progress: {}/{}", completed, totalImages);
                                });
                            }

                            executor.shutdown();
                            executor.awaitTermination(5, TimeUnit.MINUTES);

                            long successCount = finalStory.getScenes().stream()
                                    .filter(s -> s.getImageUrl() != null && s.getImageUrl().startsWith("file:"))
                                    .count();
                            logger.info("Background image download complete: {} cached", successCount);

                            // Invoke refresh callback to update UI with cached images
                            if (refreshCallback != null) {
                                Platform.runLater(() -> {
                                    logger.info("Refreshing UI with cached images");
                                    refreshCallback.accept(finalStory);
                                });
                            }

                        } catch (Exception e) {
                            logger.error("Error during background image download", e);
                        }
                    }, "ImageDownloadThread").start();
                } else {
                    logger.info("No images to download");
                }

            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                logger.error("JSON parsing failed", e);
            } catch (Exception e) {
                logger.error("Unexpected error during story processing", e);
            }
        }
    }
}
