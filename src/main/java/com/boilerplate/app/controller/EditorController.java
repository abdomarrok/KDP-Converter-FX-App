package com.boilerplate.app.controller;

import com.boilerplate.app.model.ImageLayout;
import com.boilerplate.app.model.KdpTemplate;
import com.boilerplate.app.model.Scene;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Controller for the right-side editor panel.
 * Handles Dynamic WYSIWYG Rendering.
 */
public class EditorController {

    private static final Logger logger = LogManager.getLogger(EditorController.class);

    @FXML
    private VBox editorPanel;
    @FXML
    private Label sceneIndexLabel;
    @FXML
    private StackPane pagePreview; // The "Paper"

    // Dynamic Components (Programmatic)
    private final ImageView editorImageView = new ImageView();
    private final TextArea sceneTextArea = new TextArea();
    private final Label noImageLabel = new Label("No Image");
    private final Button replaceImageBtn = new Button("📷 Replace Image");
    private final Button saveBtn = new Button("💾 Save");

    private MainController mainController;
    private Scene currentScene;
    private KdpTemplate currentTemplate; // To track current layout

    public void init(MainController main) {
        this.mainController = main;
        setupComponents();
    }

    private void setupComponents() {
        // Configure Image View
        editorImageView.setPreserveRatio(true);
        editorImageView.setFitWidth(300); // Default base width

        // Configure Text Area
        sceneTextArea.setWrapText(true);
        sceneTextArea.setPromptText("Enter scene text...");
        sceneTextArea.getStyleClass().add("editor-textarea");

        // Configure Buttons
        replaceImageBtn.setOnAction(e -> handleReplaceImage());
        saveBtn.getStyleClass().add("btn-success");
        saveBtn.setOnAction(e -> handleSaveScene());
    }

    public void setEditMode(boolean enabled) {
        editorPanel.setVisible(enabled);
        editorPanel.setManaged(enabled);
    }

    public void updateTemplate(KdpTemplate template) {
        this.currentTemplate = template;
        if (currentScene != null) {
            renderScene();
        }
    }

    public void loadScene(Scene scene, int index, int total, KdpTemplate template) {
        this.currentScene = scene;
        this.currentTemplate = template;

        sceneIndexLabel.setText("Scene " + (index + 1) + " of " + total);

        // Populate Data
        sceneTextArea.setText(scene.getText() != null ? scene.getText() : "");
        loadImage(scene.getImageUrl());

        renderScene();
    }

    private void loadImage(String url) {
        if (url != null && !url.isEmpty()) {
            try {
                // Determine size based on template? For now, use fixed preview size
                // Actual sizing happens in renderScene layout
                Image img = new Image(url, 400, 0, true, true, true);
                editorImageView.setImage(img);
                noImageLabel.setVisible(false);
            } catch (Exception e) {
                logger.warn("Failed to load image: " + url);
                editorImageView.setImage(null);
                noImageLabel.setVisible(true);
            }
        } else {
            editorImageView.setImage(null);
            noImageLabel.setVisible(true);
        }
    }

    /**
     * Renders the scene onto the "Paper" using the current template layout.
     */
    private void renderScene() {
        if (pagePreview == null || currentTemplate == null)
            return;

        pagePreview.getChildren().clear();

        // 1. Set Aspect Ratio of the Page
        double ratio = currentTemplate.getPageWidth() / currentTemplate.getPageHeight();
        double baseHeight = 600;
        double targetWidth = baseHeight * ratio;

        pagePreview.setMaxWidth(targetWidth);
        pagePreview.setMinHeight(baseHeight);
        pagePreview.setMaxHeight(baseHeight);

        // 2. Build Layout based on Type
        ImageLayout layout = currentTemplate.getLayout();
        if (layout == null)
            layout = ImageLayout.TOP_IMAGE_TEXT_BELOW;

        Region layoutNode = buildLayoutNode(layout);

        // Add to Page
        pagePreview.getChildren().add(layoutNode);
    }

    private Region buildLayoutNode(ImageLayout layout) {
        // Common containers
        StackPane imageContainer = new StackPane(editorImageView, noImageLabel);
        imageContainer.setAlignment(Pos.CENTER);

        // Controls underneath image
        VBox imageSection = new VBox(5, imageContainer, replaceImageBtn);
        imageSection.setAlignment(Pos.CENTER);

        VBox textSection = new VBox(5, new Label("Text:"), sceneTextArea, saveBtn);
        VBox.setVgrow(sceneTextArea, Priority.ALWAYS);

        switch (layout) {
            case FULL_PAGE_IMAGE:
                // Stack: Image filling page, Text overlay or bottom?
                // For PDF "Full Page" usually means just image. But we need to edit text.
                // Let's show Image big, and Text as a small overlay at bottom
                StackPane stack = new StackPane();
                editorImageView.setFitHeight(550); // Make it big
                editorImageView.setFitWidth(0); // auto

                VBox overlay = new VBox(sceneTextArea, saveBtn);
                overlay.setMaxHeight(150);
                overlay.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-padding: 10;");
                StackPane.setAlignment(overlay, Pos.BOTTOM_CENTER);

                stack.getChildren().addAll(imageSection, overlay);
                return stack;

            case SIDEBAR_IMAGE:
                // HBox: Image Left, Text Right
                HBox hub = new HBox(15);
                hub.setStyle("-fx-padding: 20;");

                editorImageView.setFitWidth(200); // Smaller width
                editorImageView.setFitHeight(0);

                textSection.setPrefWidth(200);
                HBox.setHgrow(textSection, Priority.ALWAYS);

                hub.getChildren().addAll(imageSection, textSection);
                return hub;

            case TEXT_ONLY:
                VBox vboxText = new VBox(15);
                vboxText.setStyle("-fx-padding: 30;");
                vboxText.getChildren().addAll(sceneTextArea, saveBtn);
                return vboxText;

            case TOP_IMAGE_TEXT_BELOW:
            default:
                // Standard VBox
                VBox vbox = new VBox(15);
                vbox.setStyle("-fx-padding: 20;");

                editorImageView.setFitWidth(300);
                editorImageView.setFitHeight(0);

                vbox.getChildren().addAll(imageSection, textSection);
                return vbox;
        }
    }

    public void clear() {
        this.currentScene = null;
        sceneIndexLabel.setText("No scene selected");
        if (pagePreview != null)
            pagePreview.getChildren().clear();
    }

    private void handleSaveScene() {
        if (currentScene == null)
            return;
        currentScene.setText(sceneTextArea.getText());
        mainController.refreshSceneList();
        mainController.updateStatus("✅ Scene saved");
    }

    private void handleReplaceImage() {
        if (currentScene == null)
            return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(editorPanel.getScene().getWindow());
        if (file != null) {
            currentScene.setImageUrl(file.toURI().toString());
            loadImage(currentScene.getImageUrl());
            mainController.updateStatus("Image replaced");
        }
    }
}
