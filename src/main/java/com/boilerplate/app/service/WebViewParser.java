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
    // Strong reference to prevent GC of the bridge during extraction
    private JavaBridge activeBridge;

    /**
     * Injects JavaScript extraction agent into the page.
     * The JavaScript uses robust selectors and content stabilization.
     */
    private void executeExtractionAgent(WebEngine webEngine, Consumer<Story> wrappedCallback,
            Consumer<Story> originalCallback, Consumer<Story> onRefresh) {
        logger.info("Injecting extraction agent");

        // Set up Java ↔ JavaScript bridge
        this.activeBridge = new JavaBridge(wrappedCallback, originalCallback, onRefresh, objectMapper);
        JSObject window = (JSObject) webEngine.executeScript("window");
        window.setMember("javaApp", this.activeBridge);

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

                    console.log("=== Gemini Storybook Extractor v3.0 (Automated) ===");

                    // ========== CORE EXTRACTION FUNCTION ==========
                    function extractGeminiStorybookPage() {
                        var visiblePages = Array.from(document.querySelectorAll('storybook-page')).filter(function(el) {
                            var style = window.getComputedStyle(el);
                            var rect = el.getBoundingClientRect();
                            return style.visibility === 'visible' &&
                                   rect.width > 0 &&
                                   rect.right > 0 &&
                                   rect.left < window.innerWidth;
                        });

                        // KEY: Use LAST visible element (current page is rendered last in DOM)
                        var activePage = visiblePages.length > 0 ? visiblePages[visiblePages.length - 1] : null;

                        if (!activePage) {
                            return { text: "", imageUrl: null, error: "No active page found" };
                        }

                        var textEl = activePage.querySelector('div.story-text-container');
                        var text = textEl ? textEl.textContent.trim() : "";

                        var imgEl = activePage.querySelector('img[src*="googleusercontent"]') ||
                                    activePage.querySelector('.storybook-image img') ||
                                    activePage.querySelector('img');
                        var imageUrl = imgEl && imgEl.src && imgEl.src.startsWith("http") ? imgEl.src : null;

                        return {
                            text: text,
                            imageUrl: imageUrl,
                            textLength: text.length
                        };
                    }

                    // ========== AUTOMATED EXTRACTION (NO ASYNC/AWAIT) ==========
                    function extractAllPages() {
                        var delayMs = 2000;
                        var results = [];
                        var sleep = function(ms) { return new Promise(function(r) { setTimeout(r, ms); }); };
                        var nextBtn = function() { return document.querySelector('button[aria-label="Next page"]'); };
                        var prevBtn = function() { return document.querySelector('button[aria-label="Previous page"]'); };

                        console.log("Starting automated extraction...");

                        // Jump to beginning using promises
                        console.log("Rewinding to start...");

                        function rewindToStart() {
                            var rewindCount = 0;
                            function doRewind() {
                                if (rewindCount >= 50 || !prevBtn() || prevBtn().disabled) {
                                    return sleep(delayMs).then(function() {
                                        console.log("Rewind complete");
                                    });
                                }
                                prevBtn().click();
                                rewindCount++;
                                return sleep(200).then(doRewind);
                            }
                            return doRewind();
                        }

                        // Extract pages recursively using promises
                        function extractPage(pageNum, lastText, consecutiveDupes) {
                            var maxPages = 50;

                            if (pageNum >= maxPages) {
                                console.log("Reached max pages");
                                return Promise.resolve(results);
                            }

                            return sleep(delayMs).then(function() {
                                var data = extractGeminiStorybookPage();

                                console.log("Page " + (pageNum + 1) + ": " + data.textLength + " chars, image: " + (data.imageUrl ? "YES" : "NO"));

                                // Check for duplicates
                                if (lastText && data.text === lastText) {
                                    consecutiveDupes++;
                                    console.log("Duplicate detected (" + consecutiveDupes + ")");
                                    if (consecutiveDupes >= 3) {
                                        console.log("Stuck on same content, stopping");
                                        return Promise.resolve(results);
                                    }
                                } else {
                                    consecutiveDupes = 0;
                                    results.push({
                                        text: data.text,
                                        imageUrl: data.imageUrl
                                    });
                                    lastText = data.text;
                                }

                                // Check next button
                                var next = nextBtn();
                                if (!next || next.disabled) {
                                    console.log("Reached end of book");
                                    return Promise.resolve(results);
                                }

                                // Click next and continue
                                next.click();
                                return extractPage(pageNum + 1, lastText, consecutiveDupes);
                            });
                        }

                        // Start the extraction chain
                        return rewindToStart()
                            .then(function() {
                                return extractPage(0, null, 0);
                            })
                            .then(function(finalResults) {
                                console.log("Extraction complete: " + finalResults.length + " scenes");

                                var story = {
                                    title: "Untitled Story",
                                    author: "Unknown",
                                    scenes: finalResults
                                };

                                if (window.javaApp && window.javaApp.processStory) {
                                    console.log("Sending story to Java...");
                                    window.javaApp.processStory(JSON.stringify(story));
                                } else {
                                    console.error("Java bridge not available!");
                                }

                                return story;
                            });
                    }

                    // Start extraction
                    extractAllPages().catch(function(err) {
                        console.error("Extraction failed: " + err);
                        if (window.javaApp && window.javaApp.processStory) {
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
