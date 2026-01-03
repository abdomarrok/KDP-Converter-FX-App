package com.boilerplate.app.controller;

import com.boilerplate.app.service.ExtractionService;
import com.boilerplate.app.util.ErrorHandler;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.web.WebView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Controller for the embedded browser and extraction logic.
 */
public class BrowserController {

    private static final Logger logger = LogManager.getLogger(BrowserController.class);

    @FXML
    private WebView webView;
    @FXML
    private Button extractButton;

    private MainController mainController;
    private final ExtractionService extractionService = new ExtractionService();

    public void init(MainController main) {
        this.mainController = main;
        
        // Setup extraction service handlers
        extractionService.setOnSucceeded(e -> {
            extractButton.setDisable(false);
            mainController.handleExtractionResult(extractionService.getValue());
        });
        
        extractionService.setOnFailed(e -> {
            extractButton.setDisable(false);
            Throwable exception = extractionService.getException();
            logger.error("Extraction failed", exception);
            ErrorHandler.showError(
                "Extraction Failed",
                "Failed to extract story from page",
                "An error occurred while extracting the story. Please ensure the page is fully loaded and try again.",
                exception
            );
            mainController.updateStatus("❌ Extraction failed");
        });
        
        extractionService.setOnCancelled(e -> {
            extractButton.setDisable(false);
            mainController.updateStatus("Extraction cancelled");
        });
    }

    public void loadUrl(String url) {
        try {
            webView.getEngine().load(url);
        } catch (Exception e) {
            logger.error("Failed to load URL", e);
            ErrorHandler.showError(
                "Load Failed",
                "Failed to load URL",
                "Could not load the specified URL. Please check the URL and try again.",
                e
            );
        }
    }

    public void refresh() {
        try {
            webView.getEngine().reload();
        } catch (Exception e) {
            logger.error("Failed to refresh", e);
            ErrorHandler.showError(
                "Refresh Failed",
                "Failed to refresh page",
                "Could not refresh the current page.",
                e
            );
        }
    }

    @FXML
    public void handleExtract() {
        if (webView.getEngine().getLocation() == null || webView.getEngine().getLocation().isEmpty()) {
            ErrorHandler.showError(
                "No URL Loaded",
                "Cannot extract story",
                "Please load a URL first before attempting extraction."
            );
            return;
        }

        if (extractionService.isRunning()) {
            ErrorHandler.showWarning(
                "Extraction in Progress",
                "An extraction is already in progress. Please wait for it to complete."
            );
            return;
        }

        mainController.updateStatus("Starting extraction...");
        extractButton.setDisable(true);
        
        extractionService.setWebEngine(webView.getEngine());
        extractionService.restart();
        
        // Update status based on service message
        extractionService.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            if (newMsg != null) {
                mainController.updateStatus(newMsg);
            }
        });
    }
    
    public void cancelExtraction() {
        if (extractionService.isRunning()) {
            extractionService.cancel();
        }
    }
}
