package com.boilerplate.app.service;

import com.boilerplate.app.config.AppConfig;
import com.boilerplate.app.model.Story;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Service that periodically saves the current story if enabled in
 * configuration.
 */
public class AutoSaveService extends ScheduledService<Void> {

    private static final Logger logger = LogManager.getLogger(AutoSaveService.class);

    private final StoryService storyService;
    private Supplier<Story> storySupplier;
    private Consumer<String> statusCallback;

    public AutoSaveService(StoryService storyService) {
        this.storyService = storyService;
        setPeriod(Duration.seconds(AppConfig.getInstance().getAutoSaveIntervalSeconds()));

        // Initial check for enabled state
        if (!AppConfig.getInstance().isAutoSaveEnabled()) {
            cancel();
        }
    }

    public void setStorySupplier(Supplier<Story> storySupplier) {
        this.storySupplier = storySupplier;
    }

    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
    }

    /**
     * Updates the period from configuration and restarts if enabled.
     */
    public void configureFromSettings() {
        if (AppConfig.getInstance().isAutoSaveEnabled()) {
            setPeriod(Duration.seconds(AppConfig.getInstance().getAutoSaveIntervalSeconds()));
            if (!isRunning()) {
                restart();
                logger.info("Auto-save service started with interval: {}s", getPeriod().toSeconds());
            }
        } else {
            cancel();
            logger.info("Auto-save service disabled");
        }
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Don't save if disabled (double check)
                if (!AppConfig.getInstance().isAutoSaveEnabled()) {
                    return null;
                }

                Story story = (storySupplier != null) ? storySupplier.get() : null;

                if (story != null) {
                    logger.debug("Auto-saving story: {}", story.getTitle());
                    // Block execution to ensure save completes before next period?
                    // Or simply fire and forget?
                    // StoryService.saveStoryAsync returns a CompletableFuture.
                    // We join it here to ensure the task "runs" while saving.
                    storyService.saveStoryAsync(story).join();

                    if (statusCallback != null) {
                        javafx.application.Platform.runLater(() -> statusCallback.accept("✅ Auto-saved"));
                    }
                }
                return null;
            }
        };
    }
}
