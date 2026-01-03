package com.boilerplate.app;

import com.boilerplate.app.model.Story;
import com.boilerplate.app.repository.DatabaseConnection;
import com.boilerplate.app.repository.EmbeddedDatabase;
import com.boilerplate.app.service.StoryService;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Verification script to test backend logic without JavaFX UI.
 * Proves that Refactoring (Services, Repositories, DB) is sound.
 */
public class HeadlessVerifier {
    public static void main(String[] args) {
        System.out.println("=== Starting Headless Verification ===");
        // Use a unique port and temp directory to avoid locks from running app
        EmbeddedDatabase db = new EmbeddedDatabase(3308, "./target/verify-db");

        try {
            // 1. Start Database
            System.out.println("[1] Starting Database...");
            db.startDatabase();

            // 2. Initialize Service
            System.out.println("[2] Initializing StoryService...");
            StoryService storyService = new StoryService();

            // 3. Test Save
            System.out.println("[3] Testing Save Story...");
            Story story = new Story();
            story.setTitle("Verification Story");
            story.setAuthor("Automated Test");
            story.setScenes(Collections.emptyList());

            storyService.saveStoryAsync(story).join(); // Block for result
            System.out.println("    - Save successful.");

            // 4. Test Load
            System.out.println("[4] Testing Load Stories...");
            List<Story> stories = storyService.getAllStoriesAsync().join();
            System.out.println("    - Found " + stories.size() + " stories.");

            boolean found = stories.stream().anyMatch(s -> "Verification Story".equals(s.getTitle()));
            if (found) {
                System.out.println("    - Verified saved story exists.");
            } else {
                throw new RuntimeException("Saved story not found!");
            }

            // 5. Test PDF Generation (Proves ECJ compiler is working)
            System.out.println("[5] Testing PDF Generation...");
            com.boilerplate.app.service.JasperPdfService pdfService = new com.boilerplate.app.service.JasperPdfService();
            // Create a temporary file
            java.io.File tempPdf = java.io.File.createTempFile("test_story", ".pdf");
            pdfService.exportToPdf(story, tempPdf.getAbsolutePath());

            if (tempPdf.exists() && tempPdf.length() > 0) {
                System.out.println("    - PDF generated successfully: " + tempPdf.getAbsolutePath());
                System.out.println("    - Size: " + tempPdf.length() + " bytes");
            } else {
                throw new RuntimeException("PDF file creation failed");
            }

            System.out.println("=== VERIFICATION SUCCESSFUL ===");

        } catch (Throwable e) {
            System.err.println("=== VERIFICATION FAILED ===");
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                System.out.println("[Cleanup] Stopping Database...");
                db.stopDatabase();
                DatabaseConnection.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.exit(0);
    }
}
