package com.boilerplate.app;

import com.boilerplate.app.model.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for the Gemini extraction workflow.
 * Uses TestFX to simulate user interaction.
 *
 * NOTE: This test requires an active internet connection and a valid Gemini
 * URL.
 */
public class ExtractionIntegrationTest extends ApplicationTest {

    private static final String TEST_URL = "https://gemini.google.com/share/65d397b7ba12"; // User's test URL

    private com.boilerplate.app.repository.EmbeddedDatabase db;

    @Override
    public void init() throws Exception {
        db = new com.boilerplate.app.repository.EmbeddedDatabase();
        db.startDatabase();
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/boilerplate/app/view/main_view.fxml"));
        Parent root = loader.load();
        stage.setScene(new javafx.scene.Scene(root, 1200, 750));
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        if (db != null) {
            db.stopDatabase();
        }
        com.boilerplate.app.repository.DatabaseConnection.shutdown();
        super.stop();
    }

    @BeforeEach
    public void setUp() throws Exception {
    }

    @AfterEach
    public void tearDown() throws TimeoutException {
        FxToolkit.hideStage();
        release(new KeyCode[] {});
        mouseDebugPoint(0, 0); // Reset mouse
    }

    private void mouseDebugPoint(int i, int i1) {
    }

    @Test
    public void testFullExtractionFlow() throws InterruptedException {
        // 1. Enter URL (Clear existing text first)
        clickOn("#urlField");
        push(KeyCode.CONTROL, KeyCode.A); // Select all
        push(KeyCode.BACK_SPACE); // Clear
        write(TEST_URL);

        // 2. Click Load (simulating enter or button)
        clickOn("Load");

        // Wait for page to load (approximate)
        Thread.sleep(5000);

        // 3. Click Extract
        clickOn("#extractButton");

        // 4. Wait for extraction to complete
        // The timeout is now 120s, so we should wait sufficiently long.
        // We poll the list view size.

        ListView<Scene> list = lookup("#sceneListView").query();

        long startTime = System.currentTimeMillis();
        boolean extracted = false;

        while (System.currentTimeMillis() - startTime < 130000) { // Wait up to 130s
            if (!list.getItems().isEmpty()) {
                extracted = true;
                break;
            }
            Thread.sleep(2000);
        }

        assertTrue(extracted, "Extraction failed or timed out - Scene list is empty");

        // 5. Verify Images
        boolean hasImages = false;
        for (Scene scene : list.getItems()) {
            if (scene.getImageUrl() != null && !scene.getImageUrl().isEmpty()) {
                hasImages = true;
                System.out.println("Found image in scene: " + scene.getImageUrl());
                // Basic validation it's a data URL or http URL
                assertTrue(scene.getImageUrl().startsWith("data:") || scene.getImageUrl().startsWith("http"),
                        "Invalid image URL format: " + scene.getImageUrl());
            }
        }

        assertTrue(hasImages, "Scenes were extracted but NO images were found!");
    }
}
