package com.boilerplate.app.controller;

import com.boilerplate.app.service.StoryService;
import com.boilerplate.app.util.ErrorHandler;
import com.boilerplate.app.util.UrlValidator;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Controller for the header navigation bar (URL field, edit toggle).
 */
public class NavigationController {

    private static final Logger logger = LogManager.getLogger(NavigationController.class);

    @FXML
    private TextField urlField;
    @FXML
    private ToggleButton editModeToggle;

    private MainController mainController;
    private StoryService storyService;

    public void init(MainController main, StoryService service) {
        this.mainController = main;
        this.storyService = service;
        loadHistory();
    }

    private void loadHistory() {
        mainController.runBackgroundTask(
                () -> storyService.getLastUrl(),
                (lastUrl) -> {
                    if (lastUrl != null && !lastUrl.isEmpty()) {
                        urlField.setText((String) lastUrl);
                        logger.info("Restored last URL: {}", lastUrl);
                    }
                },
                "Error loading history");
    }

    @FXML
    public void handleLoadUrl() {
        String urlInput = urlField.getText();
        if (urlInput == null || urlInput.isBlank()) {
            ErrorHandler.showError(
                "Invalid URL",
                "Please enter a URL",
                "The URL field cannot be empty."
            );
            return;
        }

        // Normalize and validate URL
        String normalizedUrl = UrlValidator.normalizeUrl(urlInput);
        if (normalizedUrl == null) {
            ErrorHandler.showError(
                "Invalid URL",
                "The URL format is invalid",
                "Please enter a valid URL starting with http:// or https://"
            );
            return;
        }

        // Warn if not a Gemini URL
        if (!UrlValidator.isGeminiUrl(normalizedUrl)) {
            boolean proceed = ErrorHandler.showConfirmation(
                "Non-Gemini URL",
                "This doesn't appear to be a Gemini URL. Continue anyway?"
            );
            if (!proceed) {
                return;
            }
            logger.warn("Non-Gemini URL: {}", normalizedUrl);
        }

        mainController.updateStatus("Loading: " + normalizedUrl);
        logger.info("Loading URL: {}", normalizedUrl);

        // Save history to DB
        mainController.runBackgroundAction(
                () -> storyService.saveLastUrl(normalizedUrl),
                () -> logger.debug("URL saved to history"),
                "Error saving history");

        mainController.loadUrlInBrowser(normalizedUrl);
    }

    @FXML
    public void handleRefreshBrowser() {
        mainController.refreshBrowser();
        mainController.updateStatus("Refreshing...");
    }

    @FXML
    public void handleToggleEditMode() {
        boolean editMode = editModeToggle.isSelected();
        mainController.setEditMode(editMode);

        if (editMode) {
            editModeToggle.setText("✏️ Edit Mode ON");
            mainController.updateStatus("Edit mode enabled. Select a scene to edit.");
        } else {
            editModeToggle.setText("✏️ Edit Mode");
            mainController.updateStatus("Edit mode disabled.");
        }
        logger.info("Edit mode: {}", editMode);
    }
}
