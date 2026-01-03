package com.boilerplate.app.controller;

import com.boilerplate.app.model.Scene;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Controller for the right-side editor panel.
 */
public class EditorController {

    private static final Logger logger = LogManager.getLogger(EditorController.class);

    @FXML
    private VBox editorPanel;
    @FXML
    private Label sceneIndexLabel;
    @FXML
    private ImageView editorImageView;
    @FXML
    private Label noImageLabel;
    @FXML
    private TextArea sceneTextArea;

    private MainController mainController;
    private Scene currentScene;

    public void init(MainController main) {
        this.mainController = main;
    }

    public void setEditMode(boolean enabled) {
        editorPanel.setVisible(enabled);
        editorPanel.setManaged(enabled);
    }

    public void loadScene(Scene scene, int index, int total) {
        this.currentScene = scene;
        sceneIndexLabel.setText("Scene " + (index + 1) + " of " + total);
        sceneTextArea.setText(scene.getText() != null ? scene.getText() : "");

        if (scene.getImageUrl() != null && !scene.getImageUrl().isEmpty()) {
            try {
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
    }

    public void clear() {
        this.currentScene = null;
        sceneIndexLabel.setText("No scene selected");
        sceneTextArea.setText("");
        editorImageView.setImage(null);
        noImageLabel.setVisible(true);
    }

    @FXML
    public void handleSaveScene() {
        if (currentScene == null)
            return;

        currentScene.setText(sceneTextArea.getText());
        mainController.refreshSceneList();
        mainController.updateStatus("✅ Scene saved");
    }

    @FXML
    public void handleReplaceImage() {
        if (currentScene == null)
            return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        File file = chooser.showOpenDialog(editorPanel.getScene().getWindow());
        if (file != null) {
            String url = file.toURI().toString();
            currentScene.setImageUrl(url);
            // Reload to show new image
            loadScene(currentScene, mainController.getCurrentStory().getScenes().indexOf(currentScene),
                    mainController.getCurrentStory().getScenes().size());
            mainController.refreshSceneList();
            mainController.updateStatus("Image replaced");
        }
    }
}
