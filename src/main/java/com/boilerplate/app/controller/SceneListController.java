package com.boilerplate.app.controller;

import com.boilerplate.app.model.Scene;
import com.boilerplate.app.model.Story;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Controller for the scene list sidebar.
 */
public class SceneListController {

    private static final Logger logger = LogManager.getLogger(SceneListController.class);

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

    private MainController mainController;

    public void init(MainController main) {
        this.mainController = main;

        sceneListView.setCellFactory(param -> new SceneListCell());
        sceneListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> mainController.handleSceneSelection(newVal));
    }

    public void displayStory(Story story) {
        if (story == null)
            return;

        sceneCountLabel.setText("(" + story.getScenes().size() + ")");
        sceneListView.getItems().setAll(story.getScenes());
        updateEmptyState();

        if (!story.getScenes().isEmpty()) {
            sceneListView.getSelectionModel().selectFirst();
        }
    }

    public void updateSelectionState(Scene scene) {
        boolean hasSelection = scene != null;
        moveUpBtn.setDisable(!hasSelection);
        moveDownBtn.setDisable(!hasSelection);
        deleteSceneBtn.setDisable(!hasSelection);
    }

    public void refreshList() {
        sceneListView.refresh();
    }

    private void updateEmptyState() {
        boolean isEmpty = sceneListView.getItems().isEmpty();
        emptyState.setVisible(isEmpty);
        sceneListView.setVisible(!isEmpty);
    }

    @FXML
    public void handleMoveSceneUp() {
        Story currentStory = mainController.getCurrentStory();
        Scene selectedScene = sceneListView.getSelectionModel().getSelectedItem();

        if (currentStory == null || selectedScene == null)
            return;

        int index = currentStory.getScenes().indexOf(selectedScene);
        if (index <= 0)
            return;

        currentStory.getScenes().remove(index);
        currentStory.getScenes().add(index - 1, selectedScene);
        sceneListView.getItems().setAll(currentStory.getScenes());
        sceneListView.getSelectionModel().select(index - 1);
        mainController.updateStatus("Scene moved up");
    }

    @FXML
    public void handleMoveSceneDown() {
        Story currentStory = mainController.getCurrentStory();
        Scene selectedScene = sceneListView.getSelectionModel().getSelectedItem();

        if (currentStory == null || selectedScene == null)
            return;

        int index = currentStory.getScenes().indexOf(selectedScene);
        if (index < 0 || index >= currentStory.getScenes().size() - 1)
            return;

        currentStory.getScenes().remove(index);
        currentStory.getScenes().add(index + 1, selectedScene);
        sceneListView.getItems().setAll(currentStory.getScenes());
        sceneListView.getSelectionModel().select(index + 1);
        mainController.updateStatus("Scene moved down");
    }

    @FXML
    public void handleDeleteScene() {
        Story currentStory = mainController.getCurrentStory();
        Scene selectedScene = sceneListView.getSelectionModel().getSelectedItem();

        if (currentStory == null || selectedScene == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Scene");
        confirm.setHeaderText("Delete this scene?");
        confirm.setContentText("Are you sure you want to delete this scene?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            currentStory.getScenes().remove(selectedScene);
            sceneListView.getItems().remove(selectedScene);
            sceneCountLabel.setText("(" + currentStory.getScenes().size() + ")");
            updateEmptyState();
            mainController.updateStatus("Scene deleted");
        }
    }

    @FXML
    public void handleAddScene() {
        mainController.showError("Add scene not yet implemented");
    }

    // Inner class for custom cell
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
            int idx = getIndex() + 1;
            titleLabel.setText("Scene " + idx);
            String text = item.getText() != null ? item.getText() : "";
            textLabel.setText(text.substring(0, Math.min(80, text.length())) + (text.length() > 80 ? "..." : ""));
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
