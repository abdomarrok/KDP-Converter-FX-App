package com.boilerplate.app.controller;

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

    public void init(MainController main, WebView webView, Button extractBtn) {
        this.mainController = main;
        this.webView = webView;
        this.extractButton = extractBtn;
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
                    e);
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
                    e);
        }
    }

    @FXML
    public void handleExtract() {
        // Delegate to MainController (Pipeline)
        mainController.handleExtractAction();
    }

    public WebView getWebView() {
        return webView;
    }

    public javafx.scene.web.WebEngine getWebEngine() {
        return webView.getEngine();
    }
}
