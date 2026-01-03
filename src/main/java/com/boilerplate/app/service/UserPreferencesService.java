package com.boilerplate.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Service for persisting URL history and user preferences.
 * Stores data in user's home directory as JSON.
 */
public class UserPreferencesService {

    private static final Logger logger = LogManager.getLogger(UserPreferencesService.class);
    private static final int MAX_URL_HISTORY = 10;

    private static final Path CONFIG_DIR = Paths.get(
            System.getProperty("user.home"), ".storyforge");
    private static final Path URL_HISTORY_FILE = CONFIG_DIR.resolve("url_history.json");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private LinkedList<String> urlHistory = new LinkedList<>();

    private static UserPreferencesService instance;

    private UserPreferencesService() {
        ensureConfigDir();
        loadUrlHistory();
    }

    public static synchronized UserPreferencesService getInstance() {
        if (instance == null) {
            instance = new UserPreferencesService();
        }
        return instance;
    }

    /**
     * Get the most recent URL (for auto-filling on startup).
     */
    public String getLastUrl() {
        return urlHistory.isEmpty() ? "" : urlHistory.getFirst();
    }

    /**
     * Get all URL history.
     */
    public List<String> getUrlHistory() {
        return new ArrayList<>(urlHistory);
    }

    /**
     * Add a URL to history (moves to top if exists).
     */
    public void addUrl(String url) {
        if (url == null || url.isBlank())
            return;

        // Remove if exists, then add to front
        urlHistory.remove(url);
        urlHistory.addFirst(url);

        // Limit size
        while (urlHistory.size() > MAX_URL_HISTORY) {
            urlHistory.removeLast();
        }

        saveUrlHistory();
        logger.debug("URL added to history: {}", url);
    }

    /**
     * Clear all URL history.
     */
    public void clearHistory() {
        urlHistory.clear();
        saveUrlHistory();
        logger.info("URL history cleared");
    }

    // === Private Methods ===

    private void ensureConfigDir() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
                logger.info("Created config directory: {}", CONFIG_DIR);
            }
        } catch (IOException e) {
            logger.error("Failed to create config directory", e);
        }
    }

    private void loadUrlHistory() {
        if (!Files.exists(URL_HISTORY_FILE)) {
            logger.debug("No URL history file found");
            return;
        }

        try {
            String json = Files.readString(URL_HISTORY_FILE);
            List<String> loaded = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
            urlHistory = new LinkedList<>(loaded);
            logger.info("Loaded {} URLs from history", urlHistory.size());
        } catch (IOException e) {
            logger.error("Failed to load URL history", e);
        }
    }

    private void saveUrlHistory() {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(urlHistory);
            Files.writeString(URL_HISTORY_FILE, json);
            logger.debug("URL history saved");
        } catch (IOException e) {
            logger.error("Failed to save URL history", e);
        }
    }
}
