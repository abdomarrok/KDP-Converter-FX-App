package com.boilerplate.app.controller;

import com.boilerplate.app.model.*;
import com.boilerplate.app.service.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;

import java.io.File;
import java.lang.reflect.Field;

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
    private ImageView editorImageView;
    @FXML
    private Label noImageLabel;
    @FXML
    private TextArea sceneTextArea;

    // === Browser Panel ===
    @FXML
    private WebView webView;
    @FXML
    private Button extractButton;

    // === Footer ===
    @FXML
    private Label statusLabel;

    // === Services ===
    private final StoryService storyService = new StoryService();
    private final JasperPdfService jasperPdfService = new JasperPdfService();

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
        // 1. Navigation Controller (Use reflection or setters to inject fields)
        navController = new NavigationController();
        injectField(navController, "urlField", urlField);
        injectField(navController, "editModeToggle", editModeToggle);
        navController.init(this, storyService);

        // 2. Scene List Controller
        sceneListController = new SceneListController();
        injectField(sceneListController, "sceneListView", sceneListView);
        injectField(sceneListController, "emptyState", emptyState);
        injectField(sceneListController, "sceneCountLabel", sceneCountLabel);
        injectField(sceneListController, "moveUpBtn", moveUpBtn);
        injectField(sceneListController, "moveDownBtn", moveDownBtn);
        injectField(sceneListController, "deleteSceneBtn", deleteSceneBtn);
        sceneListController.init(this);

        // 3. Editor Controller
        editorController = new EditorController();
        injectField(editorController, "editorPanel", editorPanel);
        injectField(editorController, "sceneIndexLabel", sceneIndexLabel);
        injectField(editorController, "editorImageView", editorImageView);
        injectField(editorController, "noImageLabel", noImageLabel);
        injectField(editorController, "sceneTextArea", sceneTextArea);
        editorController.init(this);

        // 4. Browser Controller
        browserController = new BrowserController();
        injectField(browserController, "webView", webView);
        injectField(browserController, "extractButton", extractButton);
        browserController.init(this);
    }

    // Helper to inject FXML fields into delegates since we aren't using nested FXML
    // yet
    private void injectField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            logger.error("Failed to inject field {} into {}", fieldName, target.getClass().getSimpleName(), e);
        }
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
        browserController.handleExtract();
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
    @FXML
    private void handleSaveScene() {
        editorController.handleSaveScene();
    }

    @FXML
    private void handleReplaceImage() {
        editorController.handleReplaceImage();
    }

    // === Main Controller Logic (Template, Persistence, Orchestration) ===

    @FXML
    private void handleTemplateChange() {
        KdpTemplate selected = templateCombo.getValue();
        if (selected == null)
            return;
        currentTemplate = selected;
        updateStatus("Template: " + currentTemplate.getName());
    }

    @FXML
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
    }

    // === Public API for Delegates ===

    public void updateStatus(String msg) {
        statusLabel.setText(msg);
    }

    public void showError(String msg) {
        statusLabel.setText("❌ " + msg);
        logger.error(msg);
    }

    public void loadUrlInBrowser(String url) {
        browserController.loadUrl(url);
    }

    public void refreshBrowser() {
        browserController.refresh();
    }

    public void handleExtractionResult(Story story) {
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
            editorController.loadScene(scene, index, currentStory.getScenes().size());
        } else {
            editorController.clear();
        }
    }

    public void refreshSceneList() {
        sceneListController.refreshList();
    }

    public void setEditMode(boolean enabled) {
        editorController.setEditMode(enabled);
    }

    public Story getCurrentStory() {
        return currentStory;
    }

    // === Persistence (Keep in Main for now as it deals with overall app state) ===

    @FXML
    private void handleSaveStory() {
        if (currentStory == null) {
            showError("No story to save!");
            return;
        }
        updateStatus("Saving to database...");
        runBackgroundTask(
                () -> storyService.saveStoryAsync(currentStory),
                (res) -> updateStatus("✅ Saved: " + currentStory.getTitle()),
                "Error saving story");
    }

    @FXML
    private void handleOpenStory() {
        statusLabel.setText("Loading saved stories...");
        storyService.getAllStoriesAsync()
                .thenAccept(stories -> Platform.runLater(() -> {
                    if (stories.isEmpty()) {
                        showError("No saved stories found.");
                        return;
                    }
                    ChoiceDialog<Story> dialog = new ChoiceDialog<>(stories.get(0), stories);
                    dialog.setTitle("Open Story");
                    dialog.setHeaderText("Select a story");
                    dialog.showAndWait().ifPresent(selected -> loadFullStory(selected.getId()));
                }));
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
            showError("No story loaded!");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF As");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName(currentStory.getTitle().replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf");

        File file = fileChooser.showSaveDialog(statusLabel.getScene().getWindow());
        if (file != null) {
            updateStatus("Generating PDF...");
            new Thread(() -> {
                try {
                    jasperPdfService.exportToPdf(currentStory, file.getAbsolutePath(), currentTemplate);
                    Platform.runLater(() -> updateStatus("✅ Saved PDF: " + file.getName()));
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Error: " + e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    private void handlePreviewPdf() {
        if (currentStory == null)
            return;
        updateStatus("Generating Preview...");
        new Thread(() -> {
            try {
                jasperPdfService.viewPdf(currentStory, currentTemplate);
                Platform.runLater(() -> updateStatus("✅ Preview Launched"));
            } catch (Exception e) {
                Platform.runLater(() -> showError("Error: " + e.getMessage()));
            }
        }).start();
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
