package com.boilerplate.app.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application configuration management.
 * Loads settings from application.properties and provides typed access.
 */
public class AppConfig {

    private static final Logger logger = LogManager.getLogger(AppConfig.class);
    private static final AppConfig INSTANCE = new AppConfig();

    private final Properties properties;

    private AppConfig() {
        properties = new Properties();
        loadProperties();
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                logger.warn("application.properties not found, using defaults");
                return;
            }
            properties.load(input);
            logger.info("Configuration loaded successfully");
        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
        }
    }

    // === Application Info ===

    public String getAppName() {
        return getString("app.name", "StoryForge");
    }

    public String getAppVersion() {
        return getString("app.version", "1.0.0");
    }

    // === Cache Settings ===

    public String getCacheDirectory() {
        return getString("cache.dir", ".storyforge/storyforge-images");
    }

    /**
     * Returns the absolute path to the cache directory.
     * Resolves relative paths against user home.
     */
    public java.nio.file.Path getCacheDirectoryPath() {
        String cacheDir = getCacheDirectory();
        if (cacheDir.startsWith(".")) {
            return java.nio.file.Paths.get(System.getProperty("user.home"), cacheDir);
        }
        return java.nio.file.Paths.get(cacheDir);
    }

    public int getCacheMaxSizeMB() {
        return getInt("cache.max.size.mb", 500);
    }

    public long getCacheMaxSize() {
        return getCacheMaxSizeMB() * 1024L * 1024L;
    }

    public int getCacheCleanupThresholdMB() {
        return getInt("cache.cleanup.threshold.mb", 400);
    }

    public long getCacheCleanupThreshold() {
        return getCacheCleanupThresholdMB() * 1024L * 1024L;
    }

    // === Setter for Overrides (Testing/Embedded DB) ===

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    // === Network Settings ===

    public int getHttpConnectTimeout() {
        return getInt("http.connect.timeout", 10000);
    }

    public int getHttpReadTimeout() {
        return getInt("http.read.timeout", 30000);
    }

    public String getHttpUserAgent() {
        return getString("http.user.agent", "Mozilla/5.0 (StoryForge/1.0)");
    }

    // === Extraction Settings ===

    public int getExtractionTimeoutSeconds() {
        return getInt("extraction.timeout.seconds", 300);
    }

    public int getExtractionRetryAttempts() {
        return getInt("extraction.retry.attempts", 3);
    }

    public int getExtractionRetryDelayMs() {
        return getInt("extraction.retry.delay.ms", 1000);
    }

    // === Image Download Settings ===

    public int getImageDownloadTimeoutSeconds() {
        return getInt("image.download.timeout.seconds", 30);
    }

    public int getImageDownloadMaxConcurrent() {
        return getInt("image.download.max.concurrent", 5);
    }

    public int getImageMaxWidth() {
        return getInt("image.max.width", 2048);
    }

    public int getImageMaxHeight() {
        return getInt("image.max.height", 2048);
    }

    public boolean isImageWatermarkRemovalEnabled() {
        return getBoolean("image.watermark.remove", false); // Default false to be safe
    }

    public int getImageWatermarkCropBottomPixels() {
        return getInt("image.watermark.crop.bottom", 40); // 40px seems reasonable for most AI watermarks
    }

    // === PDF Generation Settings ===

    public String getPdfJasperTempDir() {
        String dir = getString("pdf.jasper.temp.dir", "${java.io.tmpdir}");
        // Replace system property placeholders
        if (dir.contains("${java.io.tmpdir}")) {
            dir = dir.replace("${java.io.tmpdir}", System.getProperty("java.io.tmpdir"));
        }
        return dir;
    }

    public String getPdfDefaultTemplate() {
        return getString("pdf.default.template", "6x9_no_bleed");
    }

    public boolean isPdfCompressionEnabled() {
        return getBoolean("pdf.compression.enabled", true);
    }

    // === UI Settings ===

    public String getUiTheme() {
        return getString("ui.theme", "dark");
    }

    public int getUiDefaultFontSize() {
        return getInt("ui.default.font.size", 14);
    }

    public int getUiWindowWidth() {
        return getInt("ui.window.width", 1200);
    }

    public int getUiWindowHeight() {
        return getInt("ui.window.height", 800);
    }

    public boolean isAutoSaveEnabled() {
        return getBoolean("ui.auto.save.enabled", false);
    }

    public int getAutoSaveIntervalSeconds() {
        return getInt("ui.auto.save.interval.seconds", 300);
    }

    // === Thread Pool Settings ===

    public int getThreadPoolCoreSize() {
        return getInt("threadpool.core.size", 4);
    }

    public int getThreadPoolMaxSize() {
        return getInt("threadpool.max.size", 8);
    }

    public int getThreadPoolKeepAliveSeconds() {
        return getInt("threadpool.keep.alive.seconds", 60);
    }

    // === Database Settings ===

    public String getDbHost() {
        return getString("db.host", "localhost");
    }

    public int getDbPort() {
        return getInt("db.port", 3306);
    }

    public String getDbName() {
        return getString("db.name", "storyforge");
    }

    public String getDbUser() {
        return getString("db.user", "root");
    }

    public String getDbPassword() {
        return getString("db.password", "");
    }

    // === Helper Methods ===

    private String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    private int getInt(String key, int defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for {}, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Reloads configuration from properties file.
     */
    public void reload() {
        properties.clear();
        loadProperties();
        logger.info("Configuration reloaded");
    }
}
