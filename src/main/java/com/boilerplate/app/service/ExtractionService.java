package com.boilerplate.app.service;

import com.boilerplate.app.model.Story;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.web.WebEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * JavaFX Service for extracting stories from WebView.
 * Provides cancellation support and progress updates.
 */
public class ExtractionService extends Service<Story> {
    
    private static final Logger logger = LogManager.getLogger(ExtractionService.class);
    
    private WebEngine webEngine;
    private final WebViewParser parser = new WebViewParser();
    
    public void setWebEngine(WebEngine webEngine) {
        this.webEngine = webEngine;
    }
    
    @Override
    protected Task<Story> createTask() {
        if (webEngine == null) {
            throw new IllegalStateException("WebEngine must be set before starting extraction");
        }
        
        return new Task<Story>() {
            @Override
            protected Story call() throws Exception {
                updateMessage("Initializing extraction...");
                
                final Story[] result = new Story[1];
                final Exception[] exception = new Exception[1];
                final Object lock = new Object();
                
                parser.parseCurrentPage(webEngine, story -> {
                    synchronized (lock) {
                        result[0] = story;
                        lock.notify();
                    }
                });
                
                // Wait for extraction to complete (with timeout)
                synchronized (lock) {
                    try {
                        lock.wait(60000); // 60 second timeout
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Extraction interrupted", e);
                    }
                }
                
                if (result[0] == null) {
                    throw new RuntimeException("Extraction timed out or failed");
                }
                
                updateMessage("Extraction complete: " + result[0].getScenes().size() + " scenes found");
                return result[0];
            }
        };
    }
}

