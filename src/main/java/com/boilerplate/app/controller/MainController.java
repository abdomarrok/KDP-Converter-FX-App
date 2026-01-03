package com.boilerplate.app.controller;

import com.boilerplate.app.model.*;
import com.boilerplate.app.service.*;
import com.boilerplate.app.service.PdfGenerationService;
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

import java.io.File;

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
    // === Services ===
    private final StoryService storyService = new StoryService();
    private final JasperPdfService jasperPdfService = new JasperPdfService();
    private final com.boilerplate.app.service.PipelineService pipelineService = new com.boilerplate.app.service.PipelineService();

    // === Sub-Controllers (Delegates) ===
    private NavigationController navController;
    private SceneListController sceneListController;
    private EditorController editorController;
    private BrowserController browserController;

    // === State ===
    private volatile Story currentStory;
    private KdpTemplate currentTemplate = KdpPresets.getDefault();

    @FXML
    public void initialize() {
        logger.info("MainController initializing");

        // Initialize Delegates manually (Wiring UI components)
        setupDelegates();

        // Initialize UI components directly managed by MainController
        // (persistence/template)
        setupTemplateControls();

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
                .addStep(new com.boilerplate.app.pipeline.steps.ExtractionStep(browserController.getWebEngine()));

        com.boilerplate.app.pipeline.PipelineContext context = new com.boilerplate.app.pipeline.PipelineContext();

        pipelineService.execute(context)
                .thenAccept(ctx -> {
                    Story story = ctx.getStory();
                    handleExtractionResult(story);
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showError("Extraction failed: " + ex.getMessage());
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
        // Also show error dialog for critical errors
        com.boilerplate.app.util.ErrorHandler.showError(
                "Error",
                "An error occurred",
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
        this.currentStory = story;
        Platform.runLater(() -> {
            updateStatus("Extracted: " + story.getTitle());
            sceneListController.displayStory(story);
        });
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
            com.boilerplate.app.util.ErrorHandler.showError(
                    "No Story to Save",
                    "Cannot save story",
                    "Please extract or load a story first.");
            return;
        }
        updateStatus("Saving to database...");
        runBackgroundTask(
                () -> {
                    storyService.saveStoryAsync(currentStory).join();
                    return null;
                },
                (res) -> {
                    updateStatus("✅ Saved: " + currentStory.getTitle());
                    com.boilerplate.app.util.ErrorHandler.showInfo(
                            "Story Saved",
                            "Story '" + currentStory.getTitle() + "' has been saved successfully.");
                },
                "Error saving story");
    }

    @FXML
    private void handleOpenStory() {
        statusLabel.setText("Loading saved stories...");
        storyService.getAllStoriesAsync()
                .thenAccept(stories -> Platform.runLater(() -> {
                    if (stories.isEmpty()) {
                        com.boilerplate.app.util.ErrorHandler.showInfo(
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
                        com.boilerplate.app.util.ErrorHandler.showError(
                                "Load Failed",
                                "Failed to load saved stories",
                                "An error occurred while loading stories from the database.",
                                throwable);
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
            com.boilerplate.app.util.ErrorHandler.showError(
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
            updateStatus("Starting PDF Generation Pipeline...");

            pipelineService.clearSteps();
            pipelineService.addStep(new com.boilerplate.app.pipeline.steps.PdfGenerationStep());

            com.boilerplate.app.pipeline.PipelineContext context = new com.boilerplate.app.pipeline.PipelineContext();
            context.setStory(currentStory);
            context.setTemplate(currentTemplate);
            context.setOutputFile(file);

            pipelineService.execute(context)
                    .thenAccept(ctx -> {
                        Platform.runLater(() -> {
                            updateStatus("✅ Saved PDF: " + file.getName());
                            com.boilerplate.app.util.ErrorHandler.showInfo(
                                    "PDF Generated",
                                    "PDF successfully saved to:\n" + file.getAbsolutePath());
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                            updateStatus("❌ PDF generation failed");
                            com.boilerplate.app.util.ErrorHandler.showError(
                                    "PDF Generation Failed",
                                    "Failed to generate PDF",
                                    cause.getMessage(),
                                    cause);
                        });
                        return null;
                    });
        }
    }

    @FXML
    private void handlePreviewPdf() {
        if (currentStory == null) {
            com.boilerplate.app.util.ErrorHandler.showError(
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
    public <T> void runBackgroundTask(MainController.CallableWithException<T> task,
            MainController.ConsumerWithException<T> onSuccess, String errorPrefix) {
        new Thread(() -> {
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
        }).start();
    }

    public void runBackgroundAction(MainController.RunnableWithException task, Runnable onSuccess, String errorPrefix) {
        new Thread(() -> {
            try {
                task.run();
                Platform.runLater(onSuccess);
            } catch (Exception e) {
                Platform.runLater(() -> showError(errorPrefix + ": " + e.getMessage()));
            }
        }).start();
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
