package com.boilerplate.app.service;

import com.boilerplate.app.pipeline.PipelineContext;
import com.boilerplate.app.pipeline.PipelineStep;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Service to orchestrate pipeline execution.
 */
public class PipelineService {

    private static final Logger logger = LogManager.getLogger(PipelineService.class);

    private final List<PipelineStep> steps = new ArrayList<>();

    public PipelineService addStep(PipelineStep step) {
        steps.add(step);
        return this; // Builder style
    }

    public CompletableFuture<PipelineContext> execute(PipelineContext context) {
        logger.info("Starting pipeline execution with {} steps", steps.size());

        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);

        for (PipelineStep step : steps) {
            chain = chain.thenCompose(v -> {
                if (context.isCancelled()) {
                    logger.info("Pipeline cancelled before step: {}", step.getName());
                    throw new CompletionException(new InterruptedException("Pipeline cancelled"));
                }

                logger.info("Executing step: {}", step.getName());
                try {
                    return step.execute(context);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            });
        }

        return chain.thenApply(v -> {
            logger.info("Pipeline execution completed successfully");
            return context;
        }).exceptionally(ex -> {
            logger.error("Pipeline execution failed", ex);
            throw (ex instanceof CompletionException) ? (CompletionException) ex : new CompletionException(ex);
        });
    }

    public void clearSteps() {
        steps.clear();
    }
}
