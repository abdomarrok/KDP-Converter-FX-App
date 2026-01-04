package com.boilerplate.app.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * Utility class for showing progress dialogs for long-running operations.
 */
public class ProgressDialog {

    private static final Logger logger = LogManager.getLogger(ProgressDialog.class);

    /**
     * Shows a progress dialog for a task.
     * 
     * @param owner   The parent window
     * @param title   The dialog title
     * @param message The initial message
     * @param task    The task to execute
     * @param <T>     The return type of the task
     * @return The result of the task, or null if cancelled
     */
    public static <T> T showProgress(Window owner, String title, String message, Task<T> task) {
        Dialog<T> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);
        dialog.setHeaderText(message);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(-1.0); // Indeterminate progress
        progressBar.setPrefWidth(400);

        VBox vbox = new VBox(10);
        vbox.getChildren().add(progressBar);
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        // Bind progress
        progressBar.progressProperty().bind(task.progressProperty());

        // Update message
        task.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            if (newMsg != null) {
                Platform.runLater(() -> dialog.setHeaderText(newMsg));
            }
        });

        // Handle cancellation
        dialog.setOnCloseRequest(e -> {
            if (task.isRunning()) {
                task.cancel();
            }
        });

        // Show dialog and start task
        // Show dialog and start task
        com.boilerplate.app.service.ThreadPoolService.getInstance().getComputeExecutor().execute(task);

        // Wait for task completion
        task.setOnSucceeded(e -> {
            dialog.setResult(task.getValue());
            dialog.close();
        });

        task.setOnFailed(e -> {
            logger.error("Task failed", task.getException());
            dialog.close();
        });

        task.setOnCancelled(e -> {
            dialog.close();
        });

        Optional<T> result = dialog.showAndWait();
        return result.orElse(null);
    }

    /**
     * Creates a simple progress indicator that can be updated.
     */
    public static class ProgressIndicator {
        private final Dialog<Void> dialog;
        private final ProgressBar progressBar;

        public ProgressIndicator(Window owner, String title, String message) {
            dialog = new Dialog<>();
            dialog.initOwner(owner);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(title);
            dialog.setHeaderText(message);

            progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(400);

            VBox vbox = new VBox(10);
            vbox.getChildren().add(progressBar);
            dialog.getDialogPane().setContent(vbox);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        }

        public void show() {
            Platform.runLater(() -> dialog.show());
        }

        public void updateProgress(double progress) {
            Platform.runLater(() -> progressBar.setProgress(progress));
        }

        public void updateMessage(String message) {
            Platform.runLater(() -> dialog.setHeaderText(message));
        }

        public void close() {
            Platform.runLater(() -> dialog.close());
        }
    }
}
