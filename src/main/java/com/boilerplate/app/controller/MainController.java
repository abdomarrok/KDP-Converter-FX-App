package com.boilerplate.app.controller;

import com.boilerplate.app.model.*;
import com.boilerplate.app.service.*;
import com.boilerplate.app.util.database.StoryDbHelper;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.List;

public class MainController {

    private static final Logger logger = LogManager.getLogger(MainController.class);

    // === Header Components ===
    @FXML
    private TextField urlField;
    @FXML
    private ToggleButton editModeToggle;

    // === Scene List Panel ===
    @FXML
    private ListView<Scene> sceneListView;
    @FXML
    private VBox emptyState;
    @FXML
    private Label sceneCountLabel;
    @FXML
    private Button moveUpBtn;
    @FXML
    private Button moveDownBtn;
    @FXML
    private Button deleteSceneBtn;

    // === KDP Template Panel ===
    @FXML
    private ComboBox<String> templateCombo;
    @FXML
    private ComboBox<String> layoutCombo;
    @FXML
    private Spinner<Integer> fontSizeSpinner;

    // === Editor Panel ===
    @FXML
    private VBox editorPanel;
    @FXML
    private Label editorTitleLabel;
    @FXML
    private Label sceneIndexLabel;
    @FXML
    private ImageView editorImageView;
    @FXML
    private Label noImageLabel;
    @FXML
    private TextArea sceneTextArea;

    // === Browser Panel ===
    @FXML
    private WebView webView;

    // === Footer ===
    @FXML
    private Label statusLabel;
    @FXML
    private Button extractButton;

    // === Services ===
    private final WebViewParser webViewParser = new WebViewParser();
    private final JasperPdfService jasperPdfService = new JasperPdfService();
    private final StoryDbHelper dbHelper = new StoryDbHelper();

    // === State ===
    private volatile Story currentStory;
    private Scene selectedScene;
    private KdpTemplate currentTemplate = KdpPresets.getDefault();

    @FXML
    public void initialize() {
        logger.info("MainController initializing");
        statusLabel.setText("Ready to forge stories.");

        // Initialize scene list
        sceneListView.setCellFactory(param -> new SceneListCell());
        sceneListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> handleSceneSelection(newVal));

        // Initialize template combo
        templateCombo.getSelectionModel().selectFirst();
        templateCombo.setOnAction(e -> handleTemplateChange());

        // Initialize layout combo
        layoutCombo.getSelectionModel().select(1); // Default to "Image Top, Text Below"
        layoutCombo.setOnAction(e -> handleLayoutChange());

        // Initialize font size spinner
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 36, 14, 2);
        fontSizeSpinner.setValueFactory(valueFactory);

        // Load last URL from DB
        runBackgroundTask(
                () -> dbHelper.getSetting("last_url", ""),
                (lastUrl) -> {
                    if (lastUrl != null && !lastUrl.isEmpty()) {
                        urlField.setText((String) lastUrl);
                        logger.info("Restored last URL: {}", lastUrl);
                    }
                },
                "Error loading history");

        logger.info("MainController initialized");
    }

    // === URL Loading ===

    @FXML
    private void handleLoadUrl() {
        String url = urlField.getText();
        if (url == null || url.isBlank()) {
            showError("Please enter a URL");
            return;
        }

        if (!url.contains("gemini.google.com")) {
            logger.warn("Non-Gemini URL: {}", url);
            statusLabel.setText("⚠️ Warning: Non-Gemini URL");
        } else {
            statusLabel.setText("Loading: " + url);
        }

        logger.info("Loading URL: {}", url);

        // Save history to DB
        runBackgroundAction(
                () -> dbHelper.saveSetting("last_url", urlField.getText()),
                () -> {
                },
                "Error saving history");

        webView.getEngine().load(url);
    }

    @FXML
    private void handleRefreshBrowser() {
        webView.getEngine().reload();
        statusLabel.setText("Refreshing...");
    }

    // === Extraction ===

    @FXML
    private void handleExtract() {
        logger.info("Extract Scenes clicked");

        if (webView.getEngine().getLocation() == null ||
                webView.getEngine().getLocation().isEmpty()) {
            showError("No URL loaded!");
            return;
        }

        logger.info("Extracting from {}", webView.getEngine().getLocation());
        statusLabel.setText("Injecting Extraction Agent...");
        extractButton.setDisable(true);

        webViewParser.parseCurrentPage(webView.getEngine(), this::handleExtractionResult);
    }

    private void handleExtractionResult(Story story) {
        logger.info("Story received: {} ({} scenes)", story.getTitle(), story.getScenes().size());

        if (story.getScenes().isEmpty()) {
            Platform.runLater(() -> {
                statusLabel.setText("⚠️ No scenes found");
                extractButton.setDisable(false);
            });
            return;
        }

        this.currentStory = story;

        Platform.runLater(() -> {
            String title = (story.getTitle() == null || story.getTitle().isBlank())
                    ? "Untitled Story"
                    : story.getTitle();
            statusLabel.setText("Extracted: " + title + " (" + story.getScenes().size() + " scenes)");
            sceneCountLabel.setText("(" + story.getScenes().size() + ")");
            sceneListView.getItems().setAll(story.getScenes());
            updateEmptyState();
            extractButton.setDisable(false);

            // Select first scene
            if (!story.getScenes().isEmpty()) {
                sceneListView.getSelectionModel().selectFirst();
            }
        });
    }

    // === Project Persistence (Save/Load) ===

    @FXML
    private void handleSaveStory() {
        if (currentStory == null) {
            showError("No story to save!");
            return;
        }

        logger.info("Saving story to database: {}", currentStory.getTitle());
        statusLabel.setText("Saving to database...");

        runBackgroundAction(
                () -> dbHelper.saveStory(currentStory),
                () -> statusLabel.setText("✅ Saved to Database: " + currentStory.getTitle()),
                "Error saving story");
    }

    @FXML
    private void handleOpenStory() {
        statusLabel.setText("Loading saved stories...");

        // Load stories from DB in background
        runBackgroundTask(
                () -> dbHelper.getAllStories(),
                (storiesObj) -> {
                    @SuppressWarnings("unchecked")
                    List<Story> stories = (List<Story>) storiesObj;

                    if (stories.isEmpty()) {
                        showError("No saved stories found in database.");
                        return;
                    }

                    statusLabel.setText("Select story to open");

                    ChoiceDialog<Story> dialog = new ChoiceDialog<>(stories.get(0), stories);
                    dialog.setTitle("Open Story");
                    dialog.setHeaderText("Select a story to load from Database");
                    dialog.setContentText("Story:");

                    // Show titles in dropdown (Using Story.toString())

                    dialog.showAndWait().ifPresent(selectedMeta -> {
                        loadFullStory(selectedMeta.getId());
                    });
                }, "Error loading story list");

    }

    private void loadFullStory(int id) {
        statusLabel.setText("Loading story content...");
        runBackgroundTask(
                () -> dbHelper.loadStory(id),
                (loadedObj) -> {
                    Story loaded = (Story) loadedObj;
                    this.currentStory = loaded;

                    if (currentStory != null) {
                        String title = currentStory.getTitle() != null ? currentStory.getTitle() : "Untitled";
                        statusLabel.setText("Loaded: " + title);
                        sceneCountLabel.setText("(" + currentStory.getScenes().size() + ")");
                        sceneListView.getItems().setAll(currentStory.getScenes());
                        updateEmptyState();
                        if (!currentStory.getScenes().isEmpty()) {
                            sceneListView.getSelectionModel().selectFirst();
                        }
                    }
                },
                "Error loading story content");
    }

    // === Edit Mode Toggle ===

    @FXML
    private void handleToggleEditMode() {
        boolean editMode = editModeToggle.isSelected();
        editorPanel.setVisible(editMode);
        editorPanel.setManaged(editMode);

        if (editMode) {
            editModeToggle.setText("✏️ Edit Mode ON");
            statusLabel.setText("Edit mode enabled. Select a scene to edit.");
        } else {
            editModeToggle.setText("✏️ Edit Mode");
            statusLabel.setText("Edit mode disabled.");
        }

        logger.info("Edit mode: {}", editMode);
    }

    // === Scene Selection & Editing ===

    private void handleSceneSelection(Scene scene) {
        this.selectedScene = scene;
        boolean hasSelection = scene != null;

        // Enable/disable action buttons
        moveUpBtn.setDisable(!hasSelection);
        moveDownBtn.setDisable(!hasSelection);
        deleteSceneBtn.setDisable(!hasSelection);

        if (scene == null) {
            sceneIndexLabel.setText("No scene selected");
            sceneTextArea.setText("");
            editorImageView.setImage(null);
            noImageLabel.setVisible(true);
            return;
        }

        // Update index label
        int idx = currentStory.getScenes().indexOf(scene) + 1;
        int total = currentStory.getScenes().size();
        sceneIndexLabel.setText("Scene " + idx + " of " + total);

        // Update text area
        sceneTextArea.setText(scene.getText() != null ? scene.getText() : "");

        // Update image preview
        if (scene.getImageUrl() != null && !scene.getImageUrl().isEmpty()) {
            try {
                // Handle local file URLs vs web URLs
                // WebViewParser caches to local files, so usually file:/...
                Image img = new Image(scene.getImageUrl(), 350, 0, true, true, true);
                editorImageView.setImage(img);
                noImageLabel.setVisible(false);
            } catch (Exception e) {
                logger.error("Failed to load image: " + scene.getImageUrl(), e);
                editorImageView.setImage(null);
                noImageLabel.setVisible(true);
            }
        } else {
            editorImageView.setImage(null);
            noImageLabel.setVisible(true);
        }

        logger.debug("Selected scene {}: {}", idx,
                scene.getText().substring(0, Math.min(30, scene.getText().length())));
    }

    @FXML
    private void handleSaveScene() {
        if (selectedScene == null) {
            showError("No scene selected");
            return;
        }

        String newText = sceneTextArea.getText();
        selectedScene.setText(newText);
        sceneListView.refresh(); // Refresh cell display
        statusLabel.setText("✅ Scene saved");
        logger.info("Scene saved");
    }

    @FXML
    private void handleReplaceImage() {
        if (selectedScene == null) {
            showError("No scene selected");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        File file = chooser.showOpenDialog(statusLabel.getScene().getWindow());
        if (file != null) {
            String url = file.toURI().toString();
            selectedScene.setImageUrl(url);
            handleSceneSelection(selectedScene); // Refresh preview
            sceneListView.refresh();
            statusLabel.setText("Image replaced");
        }
    }

    // === Scene Reordering ===

    @FXML
    private void handleMoveSceneUp() {
        if (currentStory == null || selectedScene == null)
            return;

        int index = currentStory.getScenes().indexOf(selectedScene);
        if (index <= 0) {
            statusLabel.setText("Scene is already at top");
            return;
        }

        currentStory.getScenes().remove(index);
        currentStory.getScenes().add(index - 1, selectedScene);
        sceneListView.getItems().setAll(currentStory.getScenes());
        sceneListView.getSelectionModel().select(index - 1);
        statusLabel.setText("Scene moved up");
        logger.info("Scene moved from {} to {}", index, index - 1);
    }

    @FXML
    private void handleMoveSceneDown() {
        if (currentStory == null || selectedScene == null)
            return;

        int index = currentStory.getScenes().indexOf(selectedScene);
        if (index < 0 || index >= currentStory.getScenes().size() - 1) {
            statusLabel.setText("Scene is already at bottom");
            return;
        }

        currentStory.getScenes().remove(index);
        currentStory.getScenes().add(index + 1, selectedScene);
        sceneListView.getItems().setAll(currentStory.getScenes());
        sceneListView.getSelectionModel().select(index + 1);
        statusLabel.setText("Scene moved down");
        logger.info("Scene moved from {} to {}", index, index + 1);
    }

    @FXML
    private void handleDeleteScene() {
        if (currentStory == null || selectedScene == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Scene");
        confirm.setHeaderText("Delete this scene?");
        String preview = selectedScene.getText().substring(0,
                Math.min(50, selectedScene.getText().length()));
        confirm.setContentText(preview + "...");
        confirm.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            currentStory.getScenes().remove(selectedScene);
            sceneListView.getItems().remove(selectedScene);
            sceneCountLabel.setText("(" + currentStory.getScenes().size() + ")");
            updateEmptyState();
            statusLabel.setText("Scene deleted");
            logger.info("Scene deleted. Remaining: {}", currentStory.getScenes().size());
        }
    }

    @FXML
    private void handleAddScene() {
        // TODO: Implement add scene functionality
        showError("Add scene not yet implemented");
    }

    // === KDP Template Configuration ===

    private void handleTemplateChange() {
        String selected = templateCombo.getValue();
        if (selected == null)
            return;

        currentTemplate = KdpPresets.findByName(selected);
        layoutCombo.getSelectionModel().select(currentTemplate.getLayout().getDisplayName());
        fontSizeSpinner.getValueFactory().setValue(currentTemplate.getFontSize());

        statusLabel.setText("Template: " + currentTemplate.getName());
        logger.info("Template changed: {}", currentTemplate.getName());
    }

    private void handleLayoutChange() {
        String selected = layoutCombo.getValue();
        if (selected == null)
            return;

        for (ImageLayout layout : ImageLayout.values()) {
            if (layout.getDisplayName().equals(selected)) {
                currentTemplate.setLayout(layout);
                break;
            }
        }
        logger.info("Layout changed: {}", selected);
    }

    // === PDF Generation ===

    @FXML
    private void handleGeneratePdf() {
        if (currentStory == null) {
            showError("No story extracted yet!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF As");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        String safeTitle = generateSafeFilename(currentStory.getTitle());
        fileChooser.setInitialFileName(safeTitle + ".pdf");

        File file = fileChooser.showSaveDialog(statusLabel.getScene().getWindow());

        if (file != null) {
            logger.info("Generating PDF to {}", file.getAbsolutePath());
            statusLabel.setText("Generating PDF...");

            runBackgroundAction(
                    () -> jasperPdfService.exportToPdf(currentStory, file.getAbsolutePath(), currentTemplate),
                    () -> statusLabel.setText("✅ Saved: " + file.getName()),
                    "Error generating PDF");
        }
    }

    @FXML
    private void handlePreviewPdf() {
        if (currentStory == null) {
            showError("No story to preview!");
            return;
        }

        logger.info("Launching PDF preview");
        statusLabel.setText("Generating Preview...");

        runBackgroundAction(
                () -> jasperPdfService.viewPdf(currentStory, currentTemplate),
                () -> statusLabel.setText("✅ Preview Launched"),
                "Error generating preview");
    }

    // === Helper Methods ===

    private void updateEmptyState() {
        boolean isEmpty = sceneListView.getItems().isEmpty();
        emptyState.setVisible(isEmpty);
        sceneListView.setVisible(!isEmpty);
    }

    private String generateSafeFilename(String title) {
        if (title == null || title.isBlank()) {
            return "gemini_story_" + System.currentTimeMillis();
        }
        return title.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private void showError(String message) {
        statusLabel.setText("❌ " + message);
        logger.error(message);
    }

    // Modernized background task helper with return value support
    private <T> void runBackgroundTask(CallableWithException<T> task, ConsumerWithException<T> onSuccess,
            String errorPrefix) {
        new Thread(() -> {
            try {
                T result = task.call();
                Platform.runLater(() -> {
                    try {
                        onSuccess.accept(result);
                    } catch (Exception e) {
                        logger.error("Error in UI update: " + e.getMessage(), e);
                    }
                });
            } catch (Exception e) {
                logger.error("{}: {}", errorPrefix, e.getMessage(), e);
                Platform.runLater(() -> showError(errorPrefix + ": " + e.getMessage()));
            }
        }).start();
    }

    // Overload for void tasks (Runnable)
    private void runBackgroundAction(RunnableWithException task, Runnable onSuccess, String errorPrefix) {
        new Thread(() -> {
            try {
                task.run();
                Platform.runLater(onSuccess);
            } catch (Exception e) {
                logger.error("{}: {}", errorPrefix, e.getMessage(), e);
                Platform.runLater(() -> showError(errorPrefix + ": " + e.getMessage()));
            }
        }).start();
    }

    @FunctionalInterface
    interface CallableWithException<T> {
        T call() throws Exception;
    }

    @FunctionalInterface
    interface ConsumerWithException<T> {
        void accept(T t) throws Exception;
    }

    @FunctionalInterface
    interface RunnableWithException {
        void run() throws Exception;
    }

    // === Custom Cell for Scene List ===

    private class SceneListCell extends ListCell<Scene> {
        private final HBox container = new HBox(10);
        private final ImageView imageView = new ImageView();
        private final VBox textContainer = new VBox(5);
        private final Label titleLabel = new Label();
        private final Label textLabel = new Label();

        public SceneListCell() {
            imageView.setFitWidth(60);
            imageView.setFitHeight(60);
            imageView.setPreserveRatio(true);

            titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
            textLabel.setWrapText(true);
            textLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11;");
            textLabel.setMaxWidth(200);

            textContainer.getChildren().addAll(titleLabel, textLabel);
            container.getChildren().addAll(imageView, textContainer);
            container.setStyle("-fx-padding: 5; -fx-alignment: CENTER_LEFT;");
        }

        @Override
        protected void updateItem(Scene item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            // Scene number
            int idx = getIndex() + 1;
            titleLabel.setText("Scene " + idx);

            // Text preview
            String text = item.getText() != null ? item.getText() : "";
            // Handle potentially null text
            if (text == null)
                text = "";
            String preview = text.substring(0, Math.min(80, text.length()));
            textLabel.setText(preview + (text.length() > 80 ? "..." : ""));

            // Image
            loadImage(item);

            setGraphic(container);
        }

        private void loadImage(Scene item) {
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                try {
                    Image image = new Image(item.getImageUrl(), 60, 60, true, true, true);
                    imageView.setImage(image);
                } catch (Exception e) {
                    imageView.setImage(null);
                }
            } else {
                imageView.setImage(null);
            }
        }
    }
}
