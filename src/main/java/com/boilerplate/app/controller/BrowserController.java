package com.boilerplate.app.controller;

import com.boilerplate.app.service.WebViewParser;
import javafx.application.Platform;
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
    private final WebViewParser webViewParser = new WebViewParser();

    public void init(MainController main) {
        this.mainController = main;
    }

    public void loadUrl(String url) {
        webView.getEngine().load(url);
    }

    public void refresh() {
        webView.getEngine().reload();
    }

    @FXML
    public void handleExtract() {
        if (webView.getEngine().getLocation() == null || webView.getEngine().getLocation().isEmpty()) {
            mainController.showError("No URL loaded!");
            return;
        }

        mainController.updateStatus("Injecting Extraction Agent...");
        extractButton.setDisable(true);

        webViewParser.parseCurrentPage(webView.getEngine(), story -> {
            Platform.runLater(() -> {
                extractButton.setDisable(false);
                mainController.handleExtractionResult(story);
            });
        });
    }
}
