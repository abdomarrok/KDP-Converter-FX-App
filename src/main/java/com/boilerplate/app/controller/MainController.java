package com.boilerplate.app.controller;

import com.boilerplate.app.model.*;
import com.boilerplate.app.service.*;
import com.boilerplate.app.service.PdfGenerationService;
import com.boilerplate.app.util.ErrorDialog;
import com.boilerplate.app.util.KeyboardShortcuts;
import com.boilerplate.app.dialog.AboutDialog;
import com.boilerplate.app.service.AutoSaveService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Main Controller acting as the orchestrator.
 * Delegates specific logic to sub-controllers.
 */
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
    private ComboBox<KdpTemplate> templateCombo;
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
    private StackPane pagePreview;

    // === Browser Panel ===
    @FXML
    private VBox browserPanel;
    @FXML
    private WebView webView;
    @FXML
    private Button extractButton;

    // === Footer ===
    @FXML
    private Label statusLabel;

    // === PDF ===
    private final StoryService storyService = new StoryService();
    private final PdfGenerationService pdfService = new PdfGenerationService();
    private final JasperPdfService jasperPdfService = new JasperPdfService();
    private final PipelineService pipelineService = new PipelineService();
    private final AutoSaveService autoSaveService;
    private final ImageCacheService imageCacheService = ImageCacheService.getInstance();

    // === Sub-Controllers (Delegates) ===
    private NavigationController navController;
    private SceneListController sceneListController;
    private EditorController editorController;
    private BrowserController browserController;

    // === State ===
    private volatile Story currentStory;
    private KdpTemplate currentTemplate = KdpPresets.getDefault();

    public MainController() {
        // Services initialized in constructor
        this.autoSaveService = new AutoSaveService(storyService);
    }

    @FXML
    public void initialize() {
        logger.info("MainController initializing");

        // Initialize Delegates manually (Wiring UI components)
        setupDelegates();

        // Initialize UI components directly managed by MainController
        // (persistence/template)
        setupTemplateControls();
        setupKeyboardShortcuts();
        setupTooltips();
        setupAutoSave();

        statusLabel.setText("Ready to forge stories.");
        logger.info("MainController initialized");
    }

    private void setupDelegates() {
        // 1. Navigation Controller
        navController = new NavigationController();
        navController.init(this, storyService, urlField, editModeToggle);

        // 2. Scene List Controller
        sceneListController = new SceneListController();
        sceneListController.init(this, sceneListView, emptyState, sceneCountLabel,
                moveUpBtn, moveDownBtn, deleteSceneBtn);

        // 3. Editor Controller
        editorController = new EditorController();
        editorController.init(this, editorPanel, sceneIndexLabel, pagePreview);

        // 4. Browser Controller
        browserController = new BrowserController();
        browserController.init(this, webView, extractButton);
    }

    private void setupTemplateControls() {
        // Populate Template Combo
        templateCombo.setItems(javafx.collections.FXCollections.observableArrayList(KdpPresets.getAllPresets()));
        templateCombo.setConverter(new javafx.util.StringConverter<KdpTemplate>() {
            @Override
            public String toString(KdpTemplate object) {
                return object != null ? object.getName() : "";
            }

            @Override
            public KdpTemplate fromString(String string) {
                return templateCombo.getItems().stream()
                        .filter(t -> t.getName().equals(string))
                        .findFirst().orElse(null);
            }
        });
        templateCombo.getSelectionModel().select(KdpPresets.getDefault());
        templateCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentTemplate = newVal;
                logger.info("Template selected: {}", newVal.getName());
                // Set default layout based on template type
                if (newVal.getLayout() != null) {
                    String layoutName = newVal.getLayout().name();
                    // Map enum name to display string if needed, or just select similar
                    // Simplified: just picking a default logical for the template
                    // layoutCombo value is String currently
                }
            }
        });

        // Layout Combo
        layoutCombo.getSelectionModel().select(1);

        // Font Size
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 36, 14, 2);
        fontSizeSpinner.setValueFactory(valueFactory);
    }

    private void setupAutoSave() {
        autoSaveService.setStorySupplier(() -> currentStory);
        autoSaveService.setStatusCallback(msg -> {
            // Update status without logging (too verbose)
            updateStatus(msg);
            // Revert status after 3 seconds
            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        if (statusLabel.getText().equals("❌ " + msg) || statusLabel.getText().contains("Auto-saved")) {
                            updateStatus("Ready");
                        }
                    });
                }
            }, 3000);
        });
        autoSaveService.configureFromSettings();
    }

    private void setupKeyboardShortcuts() {
        // Set up master key event handler for the scene
        Platform.runLater(() -> {
            if (urlField.getScene() != null) {
                urlField.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    // Save Story (Ctrl+S)
                    if (KeyboardShortcuts.SAVE_STORY.match(event)) {
                        handleSaveStory();
                        event.consume();
                    }
                    // Toggle Edit Mode (Ctrl+E)
                    else if (KeyboardShortcuts.TOGGLE_EDIT_MODE.match(event)) {
                        handleToggleEditMode();
                        event.consume();
                    }
                    // Generate PDF (Ctrl+G)
                    else if (KeyboardShortcuts.GENERATE_PDF.match(event)) {
                        handleGeneratePdf();
                        event.consume();
                    }
                    // New Story (Ctrl+N)
                    else if (KeyboardShortcuts.NEW_STORY.match(event)) {
                        handleNewStory();
                        event.consume();
                    }
                    // Open Story (Ctrl+O or Ctrl+L)
                    else if (KeyboardShortcuts.OPEN_STORY.match(event) || KeyboardShortcuts.LOAD_STORY.match(event)) {
                        handleOpenStory();
                        event.consume();
                    }
                    // Refresh Browser (F5)
                    else if (KeyboardShortcuts.REFRESH.match(event)) {
                        handleRefreshBrowser();
                        event.consume();
                    }
                    // Show Help (F1)
                    else if (KeyboardShortcuts.SHOW_HELP.match(event)) {
                        AboutDialog.show();
                        event.consume();
                    }
                    // Show Shortcuts (Ctrl+/)
                    else if (KeyboardShortcuts.SHOW_SHORTCUTS.match(event)) {
                        AboutDialog.showKeyboardShortcuts();
                        event.consume();
                    }
                });
                logger.info("Keyboard shortcuts enabled");
            }
        });
    }

    private void setupTooltips() {
        // URL field tooltip
        if (urlField != null) {
            urlField.setTooltip(new Tooltip("Enter Gemini story URL (Ctrl+L to focus)"));
        }

        // Edit mode toggle tooltip
        if (editModeToggle != null) {
            editModeToggle.setTooltip(new Tooltip("Toggle edit mode (Ctrl+E)"));
        }

        // Scene list tooltips
        if (moveUpBtn != null) {
            moveUpBtn.setTooltip(new Tooltip("Move scene up"));
        }
        if (moveDownBtn != null) {
            moveDownBtn.setTooltip(new Tooltip("Move scene down"));
        }
        if (deleteSceneBtn != null) {
            deleteSceneBtn.setTooltip(new Tooltip("Delete selected scene"));
        }

        // Template tooltips
        if (templateCombo != null) {
            templateCombo.setTooltip(new Tooltip("Select KDP template size"));
        }
        if (layoutCombo != null) {
            layoutCombo.setTooltip(new Tooltip("Choose image layout style"));
        }
        if (fontSizeSpinner != null) {
            fontSizeSpinner.setTooltip(new Tooltip("Adjust text font size"));
        }

        logger.info("Tooltips configured");
    }

    /**
     * Creates a new story (clears current state).
     */
    private void handleNewStory() {
        if (currentStory != null && !currentStory.getScenes().isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("New Story");
            confirm.setHeaderText("Clear current story?");
            confirm.setContentText("This will clear the current story. Unsaved changes will be lost.");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    clearCurrentStory();
                }
            });
        } else {
            clearCurrentStory();
        }
    }

    /**
     * Clears the current story state.
     */
    private void clearCurrentStory() {
        currentStory = null;
        sceneListController.clear();
        editorController.clear();
        updateStatus("Ready for new story");
    }

    // === Event Handlers delegated to Sub-Controllers ===

    // Navigation
    @FXML
    private void handleLoadUrl() {
        navController.handleLoadUrl();
    }

    @FXML
    private void handleRefreshBrowser() {
        navController.handleRefreshBrowser();
    }

    @FXML
    private void handleToggleEditMode() {
        navController.handleToggleEditMode();
    }

    // Browser
    @FXML
    private void handleExtract() {
        // This is triggered by FXML, but BrowserController also has a handleExtract
        // The button is in browser_panel.fxml which allows BrowserController to handle
        // it directly
        // But for MainController orchestration, we use handleExtractAction called by
        // Delegate
        browserController.handleExtract();
    }

    /**
     * Called by BrowserController to initiate extraction pipeline.
     */
    public void handleExtractAction() {
        updateStatus("Starting extraction pipeline...");

        pipelineService.clearSteps();
        pipelineService
                .addStep(new com.boilerplate.app.pipeline.steps.ExtractionStep(browserController.getWebEngine(), this));

        com.boilerplate.app.pipeline.PipelineContext context = new com.boilerplate.app.pipeline.PipelineContext();

        pipelineService.execute(context)
                .thenAccept(ctx -> {
                    Story story = ctx.getStory();
                    handleExtractionResult(story);
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        ErrorDialog.showExtractionError(ex);
                        updateStatus("❌ Extraction failed");
                    });
                    return null;
                });
    }

    // Scene List
    @FXML
    private void handleMoveSceneUp() {
        sceneListController.handleMoveSceneUp();
    }

    @FXML
    private void handleMoveSceneDown() {
        sceneListController.handleMoveSceneDown();
    }

    @FXML
    private void handleDeleteScene() {
        sceneListController.handleDeleteScene();
    }

    @FXML
    private void handleAddScene() {
        sceneListController.handleAddScene();
    }

    // Editor
    // === Main Controller Logic (Template, Persistence, Orchestration) ===

    @FXML
    private void handleTemplateSelection() {
        KdpTemplate selected = templateCombo.getValue();
        if (selected == null)
            return;
        currentTemplate = selected;
        editorController.updateTemplate(currentTemplate);
        updateStatus("Template: " + currentTemplate.getName());
    }

    @FXML
    private void handleLayoutSelection() {
        String selected = layoutCombo.getValue();
        if (selected == null)
            return;
        for (ImageLayout layout : ImageLayout.values()) {
            if (layout.getDisplayName().equals(selected)) {
                currentTemplate.setLayout(layout);
                editorController.updateTemplate(currentTemplate);
                break;
            }
        }
    }

    // === Public API for Delegates ===

    public void updateStatus(String msg) {
        statusLabel.setText(msg);
    }

    public void showError(String msg) {
        statusLabel.setText("❌ " + msg);
        logger.error(msg);
        // Show user-friendly error dialog
        ErrorDialog.showError(
                "Error",
                "An operation failed",
                msg);
    }

    public void loadUrlInBrowser(String url) {
        browserController.loadUrl(url);
    }

    public void refreshBrowser() {
        browserController.refresh();
    }

    public void handleExtractionResult(Story story) {
        if (story == null)
            return;

        if (story.getScenes().isEmpty()) {
            Platform.runLater(() -> updateStatus("⚠️ No scenes found"));
            return;
        }

        // If this is the SAME story object (refresh after image download), just refresh
        // UI
        // Only merge if it's a DIFFERENT story object (new extraction)
        if (this.currentStory != null && this.currentStory != story && !this.currentStory.getScenes().isEmpty()) {
            story = mergeScenes(this.currentStory, story);
            logger.info("Merged extraction with existing story: {} scenes", story.getScenes().size());
        }

        this.currentStory = story;
        Story finalStory = story;
        Platform.runLater(() -> {
            updateStatus("Extracted: " + finalStory.getTitle() + " (" + finalStory.getScenes().size() + " scenes)");
            sceneListController.displayStory(finalStory);
        });
    }

    /**
     * Merges new extraction results with existing story.
     * - Matches scenes by text content (first 50 chars)
     * - Updates existing scenes with new images if they were missing
     * - Adds truly new scenes at the end
     */
    private Story mergeScenes(Story existing, Story newExtraction) {
        List<Scene> mergedScenes = new ArrayList<>(existing.getScenes());

        for (Scene newScene : newExtraction.getScenes()) {
            String newText = normalizeText(newScene.getText());
            boolean matched = false;

            // Try to find a matching existing scene
            for (Scene existingScene : mergedScenes) {
                String existingText = normalizeText(existingScene.getText());

                // Match by text similarity (first 50 chars or exact match)
                if (textsMatch(existingText, newText)) {
                    // Update image if existing scene doesn't have one but new one does
                    if ((existingScene.getImageUrl() == null || existingScene.getImageUrl().isEmpty())
                            && newScene.getImageUrl() != null && !newScene.getImageUrl().isEmpty()) {
                        existingScene.setImageUrl(newScene.getImageUrl());
                        logger.debug("Updated existing scene with new image");
                    }
                    matched = true;
                    break;
                }
            }

            // If no match found and scene has unique content, add it
            if (!matched && (newText.length() > 10 || newScene.getImageUrl() != null)) {
                mergedScenes.add(newScene);
                logger.debug("Added new scene: {}", newText.substring(0, Math.min(30, newText.length())));
            }
        }

        // Create merged story
        Story merged = new Story();
        merged.setTitle(existing.getTitle()); // Keep original title
        merged.setAuthor(existing.getAuthor());
        merged.setScenes(mergedScenes);
        merged.setId(existing.getId());

        return merged;
    }

    private String normalizeText(String text) {
        if (text == null)
            return "";
        return text.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private boolean textsMatch(String a, String b) {
        if (a.isEmpty() && b.isEmpty())
            return true;
        if (a.isEmpty() || b.isEmpty())
            return false;

        // Exact match
        if (a.equals(b))
            return true;

        // Prefix match (first 50 chars)
        int compareLen = Math.min(50, Math.min(a.length(), b.length()));
        return a.substring(0, compareLen).equals(b.substring(0, compareLen));
    }

    public void handleSceneSelection(Scene scene) {
        sceneListController.updateSelectionState(scene);

        if (scene != null) {
            int index = currentStory.getScenes().indexOf(scene);
            editorController.loadScene(scene, index, currentStory.getScenes().size(), currentTemplate);
        } else {
            editorController.clear();
        }
    }

    public void refreshSceneList() {
        sceneListController.refreshList();
    }

    /**
     * Refresh the scene list UI to show downloaded images.
     * Called by WebViewParser after background image downloads complete.
     * This method syncs the downloaded image URLs from the extraction story
     * to the currentStory's scenes, then refreshes the UI.
     * 
     * @param updatedStory The story with updated image URLs from background
     *                     download
     */
    public void refreshSceneImages(Story updatedStory) {
        Platform.runLater(() -> {
            if (currentStory == null || updatedStory == null) {
                return;
            }

            logger.debug("Syncing {} downloaded images to currentStory",
                    updatedStory.getScenes().stream()
                            .filter(s -> s.getImageUrl() != null && s.getImageUrl().startsWith("file:"))
                            .count());

            // Sync image URLs from updatedStory to currentStory by matching scene text
            for (Scene currentScene : currentStory.getScenes()) {
                if (currentScene.getImageUrl() != null && currentScene.getImageUrl().startsWith("file:")) {
                    continue; // Already has cached image
                }

                String currentText = normalizeText(currentScene.getText());

                // Find matching scene in updatedStory
                for (Scene updatedScene : updatedStory.getScenes()) {
                    if (updatedScene.getImageUrl() != null && updatedScene.getImageUrl().startsWith("file:")) {
                        String updatedText = normalizeText(updatedScene.getText());
                        if (textsMatch(currentText, updatedText)) {
                            currentScene.setImageUrl(updatedScene.getImageUrl());
                            logger.debug("Synced image to scene: {}",
                                    currentText.substring(0, Math.min(30, currentText.length())));
                            break;
                        }
                    }
                }
            }

            // Refresh the scene list UI
            sceneListController.refreshList();

            // Also refresh the current editor if a scene is selected
            Scene selectedScene = sceneListView.getSelectionModel().getSelectedItem();
            if (selectedScene != null) {
                int index = currentStory.getScenes().indexOf(selectedScene);
                editorController.loadScene(selectedScene, index, currentStory.getScenes().size(), currentTemplate);
            }

            updateStatus("✅ Images loaded (" +
                    currentStory.getScenes().stream()
                            .filter(s -> s.getImageUrl() != null && s.getImageUrl().startsWith("file:"))
                            .count()
                    + " images)");
        });
    }

    public void setEditMode(boolean enabled) {
        editorController.setEditMode(enabled);
        if (browserPanel != null) {
            browserPanel.setVisible(!enabled);
            browserPanel.setManaged(!enabled);
        }
    }

    public Story getCurrentStory() {
        return currentStory;
    }

    // === Persistence (Keep in Main for now as it deals with overall app state) ===

    @FXML
    private void handleSaveStory() {
        if (currentStory == null) {
            ErrorDialog.showError(
                    "No Story to Save",
                    "Cannot save story",
                    "Please extract or load a story first.");
            return;
        }

        if (currentStory.getTitle() == null || currentStory.getTitle().trim().isEmpty()) {
            ErrorDialog.showError(
                    "Validation Error",
                    "Missing Title",
                    "Please provide a title for the story before saving.");
            return;
        }

        if (currentStory.getScenes() == null || currentStory.getScenes().isEmpty()) {
            ErrorDialog.showError(
                    "Validation Error",
                    "No Content",
                    "The story is empty. Please add scenes before saving.");
            return;
        }

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Saving story to database...");
                storyService.saveStoryAsync(currentStory).join();
                return null;
            }
        };

        com.boilerplate.app.util.ProgressDialog.showProgress(
                statusLabel.getScene().getWindow(),
                "Saving Story",
                "Saving story...",
                saveTask);

        if (saveTask.getException() != null) {
            updateStatus("❌ Error saving story");
            ErrorDialog.showSaveError(saveTask.getException());
        } else if (saveTask.isDone()) { // Success
            updateStatus("✅ Saved: " + currentStory.getTitle());
            ErrorDialog.showInfo(
                    "Story Saved",
                    "Story '" + currentStory.getTitle() + "' has been saved successfully.");
        }
    }

    @FXML
    private void handleOpenStory() {
        statusLabel.setText("Loading saved stories...");
        storyService.getAllStoriesAsync()
                .thenAccept(stories -> Platform.runLater(() -> {
                    if (stories.isEmpty()) {
                        ErrorDialog.showInfo(
                                "No Stories Found",
                                "No saved stories found. Extract a story first to save it.");
                        return;
                    }
                    ChoiceDialog<Story> dialog = new ChoiceDialog<>(stories.get(0), stories);
                    dialog.setTitle("Open Story");
                    dialog.setHeaderText("Select a story to open");
                    dialog.showAndWait().ifPresent(selected -> loadFullStory(selected.getId()));
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        logger.error("Failed to load stories", throwable);
                        ErrorDialog.showError(
                                "Load Failed",
                                "Failed to load saved stories",
                                "An error occurred while loading stories from the database.");
                    });
                    return null;
                });
    }

    private void loadFullStory(int id) {
        updateStatus("Loading story content...");
        storyService.loadStoryAsync(id).thenAccept(loaded -> Platform.runLater(() -> {
            this.currentStory = loaded;
            handleExtractionResult(loaded);
            updateStatus("Loaded: " + loaded.getTitle());
        }));
    }

    // === PDF ===

    @FXML
    private void handleGeneratePdf() {
        if (currentStory == null) {
            ErrorDialog.showError(
                    "No Story Loaded",
                    "Cannot generate PDF",
                    "Please extract or load a story first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF As");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName(currentStory.getTitle().replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf");

        File file = fileChooser.showSaveDialog(statusLabel.getScene().getWindow());
        if (file != null) {
            javafx.concurrent.Task<Void> pdfTask = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() throws Exception {
                    updateMessage("Initializing PDF pipeline...");

                    pipelineService.clearSteps();
                    pipelineService.addStep(new com.boilerplate.app.pipeline.steps.PdfGenerationStep());

                    com.boilerplate.app.pipeline.PipelineContext context = new com.boilerplate.app.pipeline.PipelineContext();
                    context.setStory(currentStory);
                    context.setTemplate(currentTemplate);
                    context.setOutputFile(file);

                    updateMessage("Generating, please wait...");
                    pipelineService.execute(context).join();
                    return null;
                }
            };

            com.boilerplate.app.util.ProgressDialog.showProgress(
                    statusLabel.getScene().getWindow(),
                    "Generating PDF",
                    "Starting PDF generation...",
                    pdfTask);

            if (pdfTask.getException() != null) {
                Throwable cause = pdfTask.getException();
                // Unwrap CompletionException if present
                if (cause instanceof java.util.concurrent.CompletionException) {
                    cause = cause.getCause();
                }

                updateStatus("❌ PDF generation failed");
                updateStatus("❌ PDF generation failed");
                ErrorDialog.showError(
                        "PDF Generation Failed",
                        "Failed to generate PDF",
                        cause.getMessage());
            } else if (pdfTask.isDone()) {
                updateStatus("✅ Saved PDF: " + file.getName());
                ErrorDialog.showInfo(
                        "PDF Generated",
                        "PDF successfully saved to:\n" + file.getAbsolutePath());
            }
        }
    }

    @FXML
    private void handlePreviewPdf() {
        if (currentStory == null) {
            ErrorDialog.showError(
                    "No Story Loaded",
                    "Cannot preview PDF",
                    "Please extract or load a story first.");
            return;
        }

        updateStatus("Generating Preview...");
        runBackgroundAction(
                () -> jasperPdfService.viewPdf(currentStory, currentTemplate),
                () -> updateStatus("✅ Preview Launched"),
                "Error generating preview");
    }

    // Background helpers
    // Uses centralized ThreadPoolService for compute-bound tasks

    public <T> void runBackgroundTask(MainController.CallableWithException<T> task,
            MainController.ConsumerWithException<T> onSuccess, String errorPrefix) {
        ThreadPoolService.getInstance().submitCompute(() -> {
            try {
                T result = task.call();
                Platform.runLater(() -> {
                    try {
                        onSuccess.accept(result);
                    } catch (Exception e) {
                        logger.error("UI Error", e);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError(errorPrefix + ": " + e.getMessage()));
            }
        });
    }

    public void runBackgroundAction(MainController.RunnableWithException task, Runnable onSuccess, String errorPrefix) {
        ThreadPoolService.getInstance().submitCompute(() -> {
            try {
                task.run();
                Platform.runLater(onSuccess);
            } catch (Exception e) {
                Platform.runLater(() -> showError(errorPrefix + ": " + e.getMessage()));
            }
        });
    }

    @FunctionalInterface
    public interface CallableWithException<T> {
        T call() throws Exception;
    }

    @FunctionalInterface
    public interface ConsumerWithException<T> {
        void accept(T t) throws Exception;
    }

    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws Exception;
    }
}
