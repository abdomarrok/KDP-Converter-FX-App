package com.boilerplate.app.pipeline;

import com.boilerplate.app.model.KdpTemplate;
import com.boilerplate.app.model.Story;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared context for pipeline execution.
 * Holds state that needs to be passed between steps.
 */
public class PipelineContext {

    private final Map<String, Object> data = new ConcurrentHashMap<>();

    // Core Domain Objects
    private volatile Story story;
    private volatile KdpTemplate template;
    private volatile File outputFile;
    private volatile boolean cancelled = false;

    public PipelineContext() {
    }

    // === Type-Safe Accessors for Core Types ===

    public Story getStory() {
        return story;
    }

    public void setStory(Story story) {
        this.story = story;
    }

    public KdpTemplate getTemplate() {
        return template;
    }

    public void setTemplate(KdpTemplate template) {
        this.template = template;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    // === Generic Property Access ===

    public void put(String key, Object value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    // === Control Flags ===

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
