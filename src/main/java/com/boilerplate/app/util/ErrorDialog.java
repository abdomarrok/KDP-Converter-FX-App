package com.boilerplate.app.util;

import atlantafx.base.theme.Styles;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Utility class for displaying user-friendly error dialogs.
 * Provides clear error messages with actionable guidance.
 */
public class ErrorDialog {

    private static final Logger logger = LogManager.getLogger(ErrorDialog.class);

    public enum Severity {
        ERROR, WARNING, INFO
    }

    /**
     * Shows a user-friendly error dialog.
     *
     * @param title     Dialog title
     * @param header    What went wrong
     * @param message   How to fix it
     * @param exception Optional exception for technical details
     * @param severity  Severity level
     */
    public static void show(String title, String header, String message, Throwable exception, Severity severity) {
        Alert alert = switch (severity) {
            case ERROR -> new Alert(Alert.AlertType.ERROR);
            case WARNING -> new Alert(Alert.AlertType.WARNING);
            case INFO -> new Alert(Alert.AlertType.INFORMATION);
        };

        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.initModality(Modality.APPLICATION_MODAL);

        // Add expandable exception details if provided
        if (exception != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            String exceptionText = sw.toString();

            Label label = new Label("Technical Details:");
            label.getStyleClass().add(Styles.TEXT_BOLD);

            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(label, 0, 0);
            expContent.add(textArea, 0, 1);

            alert.getDialogPane().setExpandableContent(expContent);
            alert.getDialogPane().setExpanded(false);
        }

        // Log the error
        logger.error("{}: {} - {}", title, header, message, exception);

        alert.showAndWait();
    }

    /**
     * Shows an error dialog (convenience method).
     */
    public static void showError(String title, String header, String message) {
        show(title, header, message, null, Severity.ERROR);
    }

    /**
     * Shows an error dialog with exception details.
     */
    public static void showError(String title, String header, String message, Throwable exception) {
        show(title, header, message, exception, Severity.ERROR);
    }

    /**
     * Shows a warning dialog.
     */
    public static void showWarning(String title, String header, String message) {
        show(title, header, message, null, Severity.WARNING);
    }

    /**
     * Shows an info dialog.
     */
    public static void showInfo(String title, String message) {
        show(title, null, message, null, Severity.INFO);
    }

    /**
     * Shows extraction error with helpful guidance.
     */
    public static void showExtractionError(Throwable exception) {
        String message = """
                • Verify the URL is correct and the story is publicly accessible
                • Check your internet connection
                • Try refreshing the page (F5)
                • If the problem persists, the story format may have changed
                """;

        show("Extraction Failed",
                "Unable to extract story from the page",
                message,
                exception,
                Severity.ERROR);
    }

    /**
     * Shows URL validation error.
     */
    public static void showInvalidUrlError(String url) {
        String message = """
                The URL you entered is not valid.

                Expected format:
                • https://gemini.google.com/share/...
                • Must start with http:// or https://

                Please check the URL and try again.
                """;

        showError("Invalid URL",
                "The URL format is incorrect",
                message);
    }

    /**
     * Shows save error with helpful guidance.
     */
    public static void showSaveError(Throwable exception) {
        String message = """
                • Ensure you have write permissions
                • Check that disk space is available
                • Try saving to a different location
                • If the problem persists, restart the application
                """;

        show("Save Failed",
                "Unable to save the story",
                message,
                exception,
                Severity.ERROR);
    }

    /**
     * Shows PDF generation error.
     */
    public static void showPdfGenerationError(Throwable exception) {
        String message = """
                • Ensure all required fields are filled
                • Verify that images are downloaded
                • Check that a valid template is selected
                • Try closing other PDF viewers if the file is open
                """;

        show("PDF Generation Failed",
                "Unable to generate the PDF",
                message,
                exception,
                Severity.ERROR);
    }
}
