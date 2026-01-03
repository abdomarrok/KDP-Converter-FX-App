package com.boilerplate.app.service;

import com.boilerplate.app.model.Story;
import com.boilerplate.app.repository.SettingsRepository;
import com.boilerplate.app.repository.StoryRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Service to handle Story-related business logic and persistence.
 * Acts as the bridge between Controllers and Repositories.
 */
public class StoryService {

    private static final Logger logger = LogManager.getLogger(StoryService.class);

    private final StoryRepository storyRepository;
    private final SettingsRepository settingsRepository;

    public StoryService() {
        this.storyRepository = new StoryRepository();
        this.settingsRepository = new SettingsRepository();
    }

    /**
     * Saves a story asynchronously.
     */
    public CompletableFuture<Void> saveStoryAsync(Story story) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (story == null) {
                    throw new IllegalArgumentException("Story cannot be null");
                }
                storyRepository.saveStory(story);
            } catch (SQLException e) {
                throw new CompletionException("Failed to save story", e);
            }
        });
    }

    /**
     * Loads a story by ID asynchronously.
     */
    public CompletableFuture<Story> loadStoryAsync(int storyId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return storyRepository.loadStory(storyId);
            } catch (SQLException e) {
                throw new CompletionException("Failed to load story", e);
            }
        });
    }

    /**
     * Gets all stories asynchronously.
     */
    public CompletableFuture<List<Story>> getAllStoriesAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return storyRepository.getAllStories();
            } catch (SQLException e) {
                throw new CompletionException("Failed to fetch stories", e);
            }
        });
    }

    /**
     * Deletes a story asynchronously.
     */
    public CompletableFuture<Void> deleteStoryAsync(int storyId) {
        return CompletableFuture.runAsync(() -> {
            try {
                storyRepository.deleteStory(storyId);
            } catch (SQLException e) {
                throw new CompletionException("Failed to delete story", e);
            }
        });
    }

    // === Settings ===

    public void saveLastUrl(String url) {
        settingsRepository.saveSetting("last_url", url);
    }

    public String getLastUrl() {
        return settingsRepository.getSetting("last_url", "");
    }
}
