package com.boilerplate.app.util.database;

import com.boilerplate.app.model.Scene;
import com.boilerplate.app.model.Story;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Database helper for Story and Scene operations.
 * Handles BLOB storage for images.
 */
public class StoryDbHelper {
    private static final Logger logger = LogManager.getLogger(StoryDbHelper.class);
    private static final String APP_CACHE_DIR = ".storyforge/cache";

    /**
     * Save a story and its scenes to the database.
     */
    public void saveStory(Story story) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // Transaction

            int storyId = story.getId();

            if (storyId == 0) {
                // Insert new story
                String insertStory = "INSERT INTO stories (title, author) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertStory, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, story.getTitle());
                    stmt.setString(2, story.getAuthor());
                    stmt.executeUpdate();

                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            storyId = keys.getInt(1);
                            story.setId(storyId);
                        }
                    }
                }
            } else {
                // Update existing story
                String updateStory = "UPDATE stories SET title = ?, author = ? WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateStory)) {
                    stmt.setString(1, story.getTitle());
                    stmt.setString(2, story.getAuthor());
                    stmt.setInt(3, storyId);
                    stmt.executeUpdate();
                }

                // Delete existing scenes logic (simplified update)
                String deleteScenes = "DELETE FROM scenes WHERE story_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteScenes)) {
                    stmt.setInt(1, storyId);
                    stmt.executeUpdate();
                }
            }

            // Insert scenes with BLOB images
            String insertScene = "INSERT INTO scenes (story_id, scene_index, text, image_url, image_data) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertScene)) {
                int index = 0;
                for (Scene scene : story.getScenes()) {
                    stmt.setInt(1, storyId);
                    stmt.setInt(2, index++);
                    stmt.setString(3, scene.getText());
                    stmt.setString(4, scene.getImageUrl()); // Keep URL as ref

                    // Convert image to bytes
                    byte[] imageData = null;
                    if (scene.getImageUrl() != null && !scene.getImageUrl().isEmpty()) {
                        try {
                            imageData = fetchImageBytes(scene.getImageUrl());
                        } catch (Exception e) {
                            logger.warn("Failed to read image bytes for persistence: " + scene.getImageUrl(), e);
                        }
                    }

                    if (imageData != null) {
                        stmt.setBytes(5, imageData);
                    } else {
                        stmt.setNull(5, Types.BLOB);
                    }

                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            conn.commit();
            logger.info("Saved story '{}' (ID: {}) with {} scenes.", story.getTitle(), storyId,
                    story.getScenes().size());

        } catch (SQLException e) {
            if (conn != null)
                conn.rollback();
            logger.error("Error saving story", e);
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * Load a story and its scenes by ID.
     */
    public Story loadStory(int storyId) throws SQLException {
        Story story = null;
        String selectStory = "SELECT * FROM stories WHERE id = ?";
        String selectScenes = "SELECT * FROM scenes WHERE story_id = ? ORDER BY scene_index";

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {

            // Load Story Metadata
            try (PreparedStatement stmt = conn.prepareStatement(selectStory)) {
                stmt.setInt(1, storyId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        story = new Story();
                        story.setId(rs.getInt("id"));
                        story.setTitle(rs.getString("title"));
                        story.setAuthor(rs.getString("author"));
                    }
                }
            }

            if (story != null) {
                // Load Scenes
                List<Scene> scenes = new ArrayList<>();
                try (PreparedStatement stmt = conn.prepareStatement(selectScenes)) {
                    stmt.setInt(1, storyId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String storedUrl = rs.getString("image_url");
                            byte[] blobData = rs.getBytes("image_data");

                            // Restore image logic
                            String finalUrl = storedUrl;
                            if (blobData != null && blobData.length > 0) {
                                try {
                                    // Write blob to a local cache file
                                    File cacheDir = new File(System.getProperty("user.home"), APP_CACHE_DIR);
                                    if (!cacheDir.exists())
                                        cacheDir.mkdirs();

                                    // Generate filename hash
                                    String filename = "db_img_" + storyId + "_" + rs.getInt("scene_index") + "_"
                                            + System.currentTimeMillis() + ".png";
                                    File tempFile = new File(cacheDir, filename);

                                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                                        fos.write(blobData);
                                    }

                                    finalUrl = tempFile.toURI().toString();
                                } catch (IOException e) {
                                    logger.error("Failed to write image blob to temp file", e);
                                    // Fallback to original URL if available
                                }
                            }

                            Scene scene = new Scene(
                                    rs.getString("text"),
                                    finalUrl);
                            scenes.add(scene);
                        }
                    }
                }
                story.setScenes(scenes);
            }
        }
        return story;
    }

    /**
     * Get list of all stories.
     */
    public List<Story> getAllStories() throws SQLException {
        List<Story> stories = new ArrayList<>();
        String sql = "SELECT * FROM stories ORDER BY updated_at DESC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Story s = new Story();
                s.setId(rs.getInt("id"));
                s.setTitle(rs.getString("title"));
                s.setAuthor(rs.getString("author"));
                stories.add(s);
            }
        }
        return stories;
    }

    public void deleteStory(int storyId) throws SQLException {
        String sql = "DELETE FROM stories WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, storyId);
            stmt.executeUpdate();
        }
    }

    // --- Settings ---

    public void saveSetting(String key, String value) {
        String sql = "INSERT INTO settings (key_name, value) VALUES (?, ?) ON DUPLICATE KEY UPDATE value = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key);
            stmt.setString(2, value);
            stmt.setString(3, value);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error saving setting", e);
        }
    }

    public String getSetting(String key, String defaultValue) {
        String sql = "SELECT value FROM settings WHERE key_name = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getString("value");
            }
        } catch (SQLException e) {
            logger.error("Error getting setting", e);
        }
        return defaultValue;
    } // Helper to fetch bytes from URL (local or remote)

    private byte[] fetchImageBytes(String urlString) throws IOException {
        URI uri = URI.create(urlString);
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            Path path = Path.of(uri);
            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            }
        } else {
            // Assume http/https
            try (InputStream in = uri.toURL().openStream()) {
                return in.readAllBytes();
            }
        }
        return null;
    }
}
