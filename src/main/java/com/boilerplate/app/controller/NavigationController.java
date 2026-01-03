package com.boilerplate.app.controller;

import com.boilerplate.app.service.StoryService;
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
        String url = urlField.getText();
        if (url == null || url.isBlank()) {
            mainController.showError("Please enter a URL");
            return;
        }

        if (!url.contains("gemini.google.com")) {
            logger.warn("Non-Gemini URL: {}", url);
            mainController.updateStatus("⚠️ Warning: Non-Gemini URL");
        } else {
            mainController.updateStatus("Loading: " + url);
        }

        logger.info("Loading URL: {}", url);

        // Save history to DB
        mainController.runBackgroundAction(
                () -> storyService.saveLastUrl(urlField.getText()),
                () -> {
                },
                "Error saving history");

        mainController.loadUrlInBrowser(url);
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
