package com.boilerplate.app.pipeline;

import java.util.concurrent.CompletableFuture;

/**
 * A single step in the processing pipeline.
 */
public interface PipelineStep {

    /**
     * Executes this step.
     * 
     * @param context The shared pipeline context.
     * @return A CompletableFuture that completes when the step is done.
     * @throws Exception If the step fails.
     */
    CompletableFuture<Void> execute(PipelineContext context) throws Exception;

    /**
     * @return The name of this step (for logging/UI).
     */
    String getName();
}
