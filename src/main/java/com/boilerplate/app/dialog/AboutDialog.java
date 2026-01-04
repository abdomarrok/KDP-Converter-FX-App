package com.boilerplate.app.dialog;

import atlantafx.base.theme.Styles;
import com.boilerplate.app.util.KeyboardShortcuts;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

/**
 * About dialog showing application information and system details.
 */
public class AboutDialog {

    private static final String APP_NAME = "StoryForge";
    private static final String APP_VERSION = "1.0.0-SNAPSHOT";
    private static final String APP_DESCRIPTION = "KDP Converter - Transform Gemini stories into beautiful picture books";

    /**
     * Shows the About dialog.
     */
    public static void show() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("About " + APP_NAME);
        dialog.initModality(Modality.APPLICATION_MODAL);

        // Header with app name and version
        Label nameLabel = new Label(APP_NAME);
        nameLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label versionLabel = new Label("Version " + APP_VERSION);
        versionLabel.setStyle("-fx-text-fill: gray;");

        Label descriptionLabel = new Label(APP_DESCRIPTION);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(400);
        descriptionLabel.setStyle("-fx-padding: 10 0 20 0;");

        // System information
        Label systemInfoHeader = new Label("System Information");
        systemInfoHeader.getStyleClass().add(Styles.TITLE_4);

        GridPane systemGrid = new GridPane();
        systemGrid.setHgap(10);
        systemGrid.setVgap(5);

        addSystemInfo(systemGrid, 0, "Java Version:", System.getProperty("java.version"));
        addSystemInfo(systemGrid, 1, "JavaFX Version:", System.getProperty("javafx.version", "N/A"));
        addSystemInfo(systemGrid, 2, "Operating System:", System.getProperty("os.name"));
        addSystemInfo(systemGrid, 3, "OS Version:", System.getProperty("os.version"));
        addSystemInfo(systemGrid, 4, "Architecture:", System.getProperty("os.arch"));
        addSystemInfo(systemGrid, 5, "Available Processors:",
                String.valueOf(Runtime.getRuntime().availableProcessors()));
        addSystemInfo(systemGrid, 6, "Max Memory:",
                formatMemory(Runtime.getRuntime().maxMemory()));

        // Credits section
        Label creditsHeader = new Label("Credits & Technologies");
        creditsHeader.getStyleClass().add(Styles.TITLE_4);
        creditsHeader.setStyle("-fx-padding: 20 0 10 0;");

        TextArea creditsArea = new TextArea(getCreditsText());
        creditsArea.setEditable(false);
        creditsArea.setWrapText(true);
        creditsArea.setPrefRowCount(6);
        creditsArea.setMaxWidth(400);

        // Layout
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(
                nameLabel,
                versionLabel,
                descriptionLabel,
                new Separator(),
                systemInfoHeader,
                systemGrid,
                creditsHeader,
                creditsArea);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    /**
     * Shows keyboard shortcuts dialog.
     */
    public static void showKeyboardShortcuts() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Keyboard Shortcuts");
        dialog.initModality(Modality.APPLICATION_MODAL);

        TextArea shortcutsArea = new TextArea(KeyboardShortcuts.getShortcutsHelpText());
        shortcutsArea.setEditable(false);
        shortcutsArea.setPrefRowCount(20);
        shortcutsArea.setPrefColumnCount(50);
        shortcutsArea.setStyle("-fx-font-family: monospace;");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getChildren().add(shortcutsArea);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    private static void addSystemInfo(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-weight: bold;");
        Label valueNode = new Label(value);

        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }

    private static String formatMemory(long bytes) {
        long mb = bytes / (1024 * 1024);
        return mb + " MB";
    }

    private static String getCreditsText() {
        return """
                JavaFX - Modern UI framework
                AtlantaFX - Beautiful theme library
                JasperReports - PDF generation engine
                MariaDB4j - Embedded database
                HikariCP - Connection pooling
                Ikonli - Icon library
                Log4j2 - Logging framework
                Jackson - JSON processing

                Developed with ❤️ for KDP publishers
                """;
    }
}
