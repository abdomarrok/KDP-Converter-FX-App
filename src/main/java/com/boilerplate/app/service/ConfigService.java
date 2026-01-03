package com.boilerplate.app.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Service to load and provide access to application configuration.
 * Reads from application.properties on classpath.
 */
public class ConfigService {
    private static final Logger logger = LogManager.getLogger(ConfigService.class);
    private static final String CONFIG_FILE = "application.properties";

    private static ConfigService instance;
    private final Properties properties;

    private ConfigService() {
        properties = new Properties();
        loadProperties();
    }

    public static synchronized ConfigService getInstance() {
        if (instance == null) {
            instance = new ConfigService();
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                logger.warn("Sorry, unable to find " + CONFIG_FILE);
                return;
            }
            properties.load(input);
            logger.info("Configuration loaded from " + CONFIG_FILE);
        } catch (IOException ex) {
            logger.error("Failed to load configuration", ex);
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null)
            return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer format for key: " + key + ", using default: " + defaultValue);
            return defaultValue;
        }
    }

    public long getLongProperty(String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value == null)
            return defaultValue;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid long format for key: " + key + ", using default: " + defaultValue);
            return defaultValue;
        }
    }

    // === Typed Accessors ===

    public String getDbHost() {
        return getProperty("db.host", "localhost");
    }

    public int getDbPort() {
        return getIntProperty("db.port", 3306);
    }

    public String getDbName() {
        return getProperty("db.name", "storyforge");
    }

    public String getDbUser() {
        return getProperty("db.user", "root");
    }

    public String getDbPassword() {
        return getProperty("db.password", "");
    }

    public Path getCacheDirectory() {
        String dir = getProperty("cache.dir", ".storyforge/storyforge-images");
        if (dir.startsWith(".")) {
            return Paths.get(System.getProperty("user.home"), dir);
        }
        return Paths.get(dir);
    }

    public long getCacheMaxSize() {
        return getLongProperty("cache.max.size.mb", 500) * 1024 * 1024;
    }

    public long getCacheCleanupThreshold() {
        return getLongProperty("cache.cleanup.threshold.mb", 400) * 1024 * 1024;
    }

    public int getHttpConnectTimeout() {
        return getIntProperty("http.connect.timeout", 10000);
    }

    public int getHttpReadTimeout() {
        return getIntProperty("http.read.timeout", 30000);
    }

    public String getHttpUserAgent() {
        return getProperty("http.user.agent", "Mozilla/5.0 (StoryForge/1.0)");
    }
}
