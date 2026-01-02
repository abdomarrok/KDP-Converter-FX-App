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
    // @FXML
    // private ProgressBar progressBar;
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

        // Custom Cell Factory for Preview List
        sceneListView.setCellFactory(param -> new ListCell<>() {
            private final ImageView imageView = new ImageView();
            private final Label textLabel = new Label();
            private final VBox vbox = new VBox(5, imageView, textLabel);

            {
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
                    if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                        try {
                            // Background loading with error handling
                            imageView.setImage(new Image(item.getImageUrl(), 200, 0, true, true, true));
                        } catch (Exception e) {
                            System.err.println("Failed to load image: " + item.getImageUrl());
                            imageView.setImage(null);
                        }
                    } else {
                        imageView.setImage(null);
                    }
                    setGraphic(vbox);
                }
            }
        });
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
        // progressBar.setVisible(true);

        webViewParser.parseCurrentPage(webView.getEngine(), (story) -> {
            System.out.println("MainController: Story received - " + story.getTitle() + " (" + story.getScenes().size()
                    + " scenes)");

            if (story.getScenes().isEmpty()) {
                Platform.runLater(() -> {
                    // progressBar.setVisible(false);
                    statusLabel.setText("⚠️ No scenes found");
                    System.err.println("MainController: No scenes found");
                });
                return;
            }

            this.currentStory = story;
            Platform.runLater(() -> {
                // progressBar.setVisible(false);
                String title = (story.getTitle() == null || story.getTitle().isBlank())
                        ? "Untitled Story"
                        : story.getTitle();
                statusLabel.setText("Extracted: " + title + " (" + story.getScenes().size() + " scenes)");
                sceneListView.getItems().setAll(story.getScenes());
                updateEmptyState();
            });
        });
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
            // progressBar.setVisible(true);

            new Thread(() -> {
                try {
                    jasperPdfService.exportToPdf(currentStory, file.getAbsolutePath());
                    Platform.runLater(() -> {
                        // progressBar.setVisible(false);
                        statusLabel.setText("Saved to: " + file.getName());
                        System.out.println("MainController: PDF generated successfully");
                    });
                } catch (Exception e) {
                    System.err.println("MainController: Failed to generate PDF");
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        // progressBar.setVisible(false);
                        showError("Error generating PDF: " + e.getMessage());
                    });
                }
            }).start();
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
        // progressBar.setVisible(true);

        new Thread(() -> {
            try {
                jasperPdfService.viewPdf(currentStory);
                Platform.runLater(() -> {
                    // progressBar.setVisible(false);
                    statusLabel.setText("Preview Launched");
                    System.out.println("MainController: PDF preview launched");
                });
            } catch (Exception e) {
                System.err.println("MainController: Failed to generate preview");
                e.printStackTrace();
                Platform.runLater(() -> {
                    // progressBar.setVisible(false);
                    showError("Error generating preview: " + e.getMessage());
                });
            }
        }).start();
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
}
