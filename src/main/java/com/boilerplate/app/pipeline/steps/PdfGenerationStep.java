package com.boilerplate.app.pipeline.steps;

import com.boilerplate.app.model.KdpTemplate;
import com.boilerplate.app.model.Story;
import com.boilerplate.app.pipeline.PipelineContext;
import com.boilerplate.app.pipeline.PipelineStep;
import com.boilerplate.app.service.PdfGenerationService;
import javafx.application.Platform;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Pipeline step to generate a PDF from the story.
 */
public class PdfGenerationStep implements PipelineStep {

    private final PdfGenerationService pdfService;

    public PdfGenerationStep() {
        this.pdfService = new PdfGenerationService();
    }

    @Override
    public CompletableFuture<Void> execute(PipelineContext context) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Story story = context.getStory();
        KdpTemplate template = context.getTemplate();
        File outputFile = context.getOutputFile();

        if (story == null) {
            future.completeExceptionally(new IllegalStateException("Context has no Story"));
            return future;
        }
        if (template == null) {
            future.completeExceptionally(new IllegalStateException("Context has no Template"));
            return future;
        }
        if (outputFile == null) {
            future.completeExceptionally(new IllegalStateException("Context has no Output File"));
            return future;
        }

        Platform.runLater(() -> {
            pdfService.setStory(story);
            pdfService.setTemplate(template);
            pdfService.setOutputFile(outputFile);

            pdfService.setOnSucceeded(e -> future.complete(null));

            pdfService.setOnFailed(e -> future.completeExceptionally(pdfService.getException()));

            pdfService.restart();
        });

        return future;
    }

    @Override
    public String getName() {
        return "Generate PDF";
    }
}
