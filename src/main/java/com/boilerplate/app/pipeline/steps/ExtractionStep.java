package com.boilerplate.app.pipeline.steps;

import com.boilerplate.app.model.Story;
import com.boilerplate.app.pipeline.PipelineContext;
import com.boilerplate.app.pipeline.PipelineStep;
import com.boilerplate.app.service.ExtractionService;
import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.web.WebEngine;

import java.util.concurrent.CompletableFuture;

/**
 * Pipeline step to extract story content from the current web page.
 */
public class ExtractionStep implements PipelineStep {

    private final WebEngine webEngine;
    private final ExtractionService extractionService;

    public ExtractionStep(WebEngine webEngine) {
        this.webEngine = webEngine;
        this.extractionService = new ExtractionService();
        this.extractionService.setWebEngine(webEngine);
    }

    @Override
    public CompletableFuture<Void> execute(PipelineContext context) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Platform.runLater(() -> {
            // Check if we have a valid URL/Page
            if (webEngine.getLocation() == null || webEngine.getLocation().isEmpty()) {
                future.completeExceptionally(new IllegalStateException("No URL loaded in browser"));
                return;
            }

            // CRITICAL: Set handlers BEFORE restart() to avoid race condition
            extractionService.setOnSucceeded(e -> {
                Story story = extractionService.getValue();
                if (story != null) {
                    context.setStory(story);
                    future.complete(null);
                } else {
                    future.completeExceptionally(new Exception("Extraction returned null story"));
                }
            });

            extractionService.setOnFailed(e -> {
                future.completeExceptionally(extractionService.getException());
            });

            extractionService.setOnCancelled(e -> {
                future.completeExceptionally(new InterruptedException("Extraction cancelled"));
            });

            // Now start the service
            extractionService.restart();
        });

        return future;
    }

    @Override
    public String getName() {
        return "Extract Story";
    }
}
