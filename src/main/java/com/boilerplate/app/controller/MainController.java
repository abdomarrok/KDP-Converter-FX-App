package com.boilerplate.app.controller;

import com.boilerplate.app.model.Scene;
import com.boilerplate.app.model.Story;
import com.boilerplate.app.service.JasperPdfService;
import com.boilerplate.app.service.WebViewParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;

import java.io.File;

public class MainController {

    @FXML
    private TextField urlField;
    @FXML
    private WebView webView;
    @FXML
    private Label statusLabel;
    @FXML
    private VBox emptyState;
    @FXML
    private ListView<Scene> sceneListView;

    private final WebViewParser webViewParser = new WebViewParser();
    private final JasperPdfService jasperPdfService = new JasperPdfService();
    private volatile Story currentStory; // Thread-safe access

    @FXML
    public void initialize() {
        System.out.println("MainController: Initializing");
        statusLabel.setText("Ready to forge stories.");

        sceneListView.setCellFactory(param -> new SceneListCell());
    }

    @FXML
    private void handleLoadUrl() {
        String url = urlField.getText();

        // Validation
        if (url == null || url.isBlank()) {
            showError("Please enter a URL");
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            showError("Invalid URL format. Must start with http:// or https://");
            return;
        }

        // Warning for non-Gemini URLs (no blocking dialog)
        if (!url.contains("gemini.google.com")) {
            statusLabel.setText("⚠️ Warning: Non-Gemini URL - may not extract correctly");
            System.out.println("MainController: Non-Gemini URL detected: " + url);
        } else {
            statusLabel.setText("Loading: " + url);
        }

        System.out.println("MainController: Loading URL: " + url);
        statusLabel.setText("Loading: " + url);
        webView.getEngine().load(url);
    }

    @FXML
    private void handleExtract() {
        System.out.println("MainController: Extract Scenes clicked");

        if (webView.getEngine().getLocation() == null || webView.getEngine().getLocation().isEmpty()) {
            System.out.println("MainController: No URL loaded");
            showError("No URL loaded!");
            return;
        }

        System.out.println("MainController: Extracting from " + webView.getEngine().getLocation());
        statusLabel.setText("Injecting Extraction Agent...");

        webViewParser.parseCurrentPage(webView.getEngine(), this::handleExtractionResult);
    }

    @FXML
    private void handleGeneratePdf() {
        if (currentStory == null) {
            showError("No story extracted yet!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        // Safe filename generation
        String safeTitle = generateSafeFilename(currentStory.getTitle());
        fileChooser.setInitialFileName(safeTitle + ".pdf");

        File file = fileChooser.showSaveDialog(statusLabel.getScene().getWindow());

        if (file != null) {
            System.out.println("MainController: Generating PDF to " + file.getAbsolutePath());
            statusLabel.setText("Generating PDF...");

            runBackgroundTask(
                () -> jasperPdfService.exportToPdf(currentStory, file.getAbsolutePath()),
                () -> statusLabel.setText("Saved to: " + file.getName()),
                "Error generating PDF"
            );
        }
    }

    @FXML
    private void handlePreviewPdf() {
        if (currentStory == null) {
            showError("No story extracted yet!");
            return;
        }

        System.out.println("MainController: Launching PDF preview");
        statusLabel.setText("Generating Preview...");

        runBackgroundTask(
            () -> jasperPdfService.viewPdf(currentStory),
            () -> statusLabel.setText("Preview Launched"),
            "Error generating preview"
        );
    }

    /**
     * Generates a safe filename from a title, with fallback for null/empty titles.
     */
    /**
     * Updates empty state visibility based on ListView content.
     */
    private void updateEmptyState() {
        boolean isEmpty = sceneListView.getItems().isEmpty();
        emptyState.setVisible(isEmpty);
        sceneListView.setVisible(!isEmpty);
    }

    /**
     * Handles scene deletion via context menu.
     */
    @FXML
    private void handleDeleteScene() {
        if (currentStory == null) {
            showError("No story loaded");
            return;
        }

        Scene selected = sceneListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a scene to delete");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Scene");
        confirm.setHeaderText("Delete this scene?");
        String preview = selected.getText().substring(0, Math.min(50, selected.getText().length()));
        confirm.setContentText(preview + "...");
        confirm.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            currentStory.getScenes().remove(selected);
            sceneListView.getItems().remove(selected);
            updateEmptyState();
            statusLabel.setText("Scene deleted");
            System.out.println("MainController: Scene deleted. Remaining: " + currentStory.getScenes().size());
        }
    }

    /**
     * Moves selected scene up in the list.
     */
    @FXML
    private void handleMoveSceneUp() {
        if (currentStory == null) {
            showError("No story loaded");
            return;
        }

        int index = sceneListView.getSelectionModel().getSelectedIndex();
        if (index <= 0) {
            statusLabel.setText("Scene is already at the top");
            return;
        }

        Scene scene = currentStory.getScenes().remove(index);
        currentStory.getScenes().add(index - 1, scene);
        sceneListView.getItems().setAll(currentStory.getScenes());
        sceneListView.getSelectionModel().select(index - 1);
        statusLabel.setText("Scene moved up");
        System.out.println("MainController: Scene moved from " + index + " to " + (index - 1));
    }

    /**
     * Moves selected scene down in the list.
     */
    @FXML
    private void handleMoveSceneDown() {
        if (currentStory == null) {
            showError("No story loaded");
            return;
        }

        int index = sceneListView.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= currentStory.getScenes().size() - 1) {
            statusLabel.setText("Scene is already at the bottom");
            return;
        }

        Scene scene = currentStory.getScenes().remove(index);
        currentStory.getScenes().add(index + 1, scene);
        sceneListView.getItems().setAll(currentStory.getScenes());
        sceneListView.getSelectionModel().select(index + 1);
        statusLabel.setText("Scene moved down");
        System.out.println("MainController: Scene moved from " + index + " to " + (index + 1));
    }

    private String generateSafeFilename(String title) {
        if (title == null || title.isBlank()) {
            String fallback = "gemini_story_" + System.currentTimeMillis();
            System.out.println("MainController: Using fallback filename: " + fallback);
            return fallback;
        }
        String safe = title.replaceAll("[^a-zA-Z0-9.-]", "_");
        System.out.println("MainController: Generated safe filename: " + safe);
        return safe;
    }

    /**
     * Shows an error message to the user.
     */
    private void showError(String message) {
        statusLabel.setText("❌ " + message);
        System.err.println("ERROR: " + message);
    }

    private void handleExtractionResult(Story story) {
        System.out.println("MainController: Story received - " + story.getTitle() + " (" + story.getScenes().size() + " scenes)");

        if (story.getScenes().isEmpty()) {
            Platform.runLater(() -> {
                statusLabel.setText("⚠️ No scenes found");
                System.err.println("MainController: No scenes found");
            });
            return;
        }

        this.currentStory = story;
        logSceneDetails(story);

        Platform.runLater(() -> {
            String title = (story.getTitle() == null || story.getTitle().isBlank()) ? "Untitled Story" : story.getTitle();
            statusLabel.setText("Extracted: " + title + " (" + story.getScenes().size() + " scenes)");
            sceneListView.getItems().setAll(story.getScenes());
            updateEmptyState();
        });
    }

    private void logSceneDetails(Story story) {
        System.out.println("MainController: Received " + story.getScenes().size() + " scenes");
        for (int i = 0; i < story.getScenes().size(); i++) {
            Scene s = story.getScenes().get(i);
            String imgUrl = s.getImageUrl();
            String imgStatus = (imgUrl == null) ? "NULL" : (imgUrl.isEmpty() ? "EMPTY" : "LENGTH=" + imgUrl.length());
            String textPreview = (s.getText() == null) ? "NULL" : s.getText().substring(0, Math.min(20, s.getText().length()));
            System.out.println("Scene " + i + ": Text=" + textPreview + "..., Image=" + imgStatus);
        }
    }

    private void runBackgroundTask(RunnableWithException task, Runnable onSuccess, String errorPrefix) {
        new Thread(() -> {
            try {
                task.run();
                Platform.runLater(onSuccess);
            } catch (Exception e) {
                System.err.println("MainController: " + errorPrefix);
                e.printStackTrace();
                Platform.runLater(() -> showError(errorPrefix + ": " + e.getMessage()));
            }
        }).start();
    }

    @FunctionalInterface
    private interface RunnableWithException {
        void run() throws Exception;
    }

    private static class SceneListCell extends ListCell<Scene> {
        private final ImageView imageView = new ImageView();
        private final Label textLabel = new Label();
        private final VBox vbox = new VBox(5, imageView, textLabel);

        public SceneListCell() {
            imageView.setFitHeight(100);
            imageView.setPreserveRatio(true);
            textLabel.setWrapText(true);
            textLabel.setMaxWidth(300);
        }

        @Override
        protected void updateItem(Scene item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                textLabel.setText(item.getText());
                loadImage(item);
                setGraphic(vbox);
            }
        }

        private void loadImage(Scene item) {
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                try {
                    // Background loading with error handling
                    Image image = new Image(item.getImageUrl(), 200, 0, true, true, true);
                    imageView.setImage(image);

                    // Check for errors
                    if (image.isError()) {
                        System.err.println("MainController: Image load error: " + image.getException());
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load image: "
                            + item.getImageUrl().substring(0, Math.min(100, item.getImageUrl().length())));
                    e.printStackTrace();
                    imageView.setImage(null);
                }
            } else {
                imageView.setImage(null);
            }
        }
    }
}
