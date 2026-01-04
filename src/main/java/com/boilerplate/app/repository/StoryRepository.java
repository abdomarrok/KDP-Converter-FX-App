package com.boilerplate.app.repository;

import com.boilerplate.app.model.Scene;
import com.boilerplate.app.model.Story;
import com.boilerplate.app.repository.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Repository for accessing Story and Scene data.
 * Handles database interactions and binary image storage.
 */
public class StoryRepository {

    private static final Logger logger = LogManager.getLogger(StoryRepository.class);
    private static final String APP_CACHE_DIR = ".storyforge/cache";

    /**
     * Saves a story and its scenes to the database.
     * Uses a transaction to ensure integrity.
     */
    public void saveStory(Story story) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            int storyId = upsertStoryMetadata(conn, story);
            story.setId(storyId);

            // For simplicity, we delete existing scenes and re-insert logic
            // Ideally, we would diff and update, but this is safer for consistency
            deleteScenes(conn, storyId);
            insertScenes(conn, storyId, story.getScenes());

            conn.commit();
            logger.info("Saved story '{}' (ID: {}) with {} scenes.", story.getTitle(), storyId,
                    story.getScenes().size());

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Failed to rollback transaction", ex);
                }
            }
            logger.error("Error saving story: {}", story.getTitle(), e);
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Error closing connection", e);
                }
            }
        }
    }

    private int upsertStoryMetadata(Connection conn, Story story) throws SQLException {
        if (story.getId() == 0) {
            String sql = "INSERT INTO stories (title, author) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, story.getTitle());
                stmt.setString(2, story.getAuthor());
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
            throw new SQLException("Creating story failed, no ID obtained.");
        } else {
            String sql = "UPDATE stories SET title = ?, author = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, story.getTitle());
                stmt.setString(2, story.getAuthor());
                stmt.setInt(3, story.getId());
                stmt.executeUpdate();
                return story.getId();
            }
        }
    }

    private void deleteScenes(Connection conn, int storyId) throws SQLException {
        String sql = "DELETE FROM scenes WHERE story_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, storyId);
            stmt.executeUpdate();
        }
    }

    private void insertScenes(Connection conn, int storyId, List<Scene> scenes) throws SQLException {
        String sql = "INSERT INTO scenes (story_id, scene_index, text, image_url, image_data, image_width, image_height) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = 0;
            for (Scene scene : scenes) {
                stmt.setInt(1, storyId);
                stmt.setInt(2, index++);
                stmt.setString(3, scene.getText());
                stmt.setString(4, scene.getImageUrl());

                byte[] imageData = fetchImageBytes(scene.getImageUrl());
                if (imageData != null) {
                    stmt.setBytes(5, imageData);
                } else {
                    stmt.setNull(5, Types.BLOB);
                }

                stmt.setInt(6, scene.getImageWidth());
                stmt.setInt(7, scene.getImageHeight());

                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public Story loadStory(int storyId) throws SQLException {
        Story story = loadStoryMetadata(storyId);
        if (story != null) {
            story.setScenes(loadScenes(storyId));
        }
        return story;
    }

    private Story loadStoryMetadata(int storyId) throws SQLException {
        String sql = "SELECT * FROM stories WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, storyId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Story story = new Story();
                    story.setId(rs.getInt("id"));
                    story.setTitle(rs.getString("title"));
                    story.setAuthor(rs.getString("author"));
                    return story;
                }
            }
        }
        return null; // Not found
    }

    private List<Scene> loadScenes(int storyId) throws SQLException {
        List<Scene> scenes = new ArrayList<>();
        String sql = "SELECT * FROM scenes WHERE story_id = ? ORDER BY scene_index";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, storyId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String storedUrl = rs.getString("image_url");
                    byte[] blobData = rs.getBytes("image_data");
                    String finalUrl = restoreImageFromBlob(storyId, rs.getInt("scene_index"), storedUrl, blobData);

                    int width = rs.getInt("image_width");
                    int height = rs.getInt("image_height");

                    scenes.add(new Scene(rs.getString("text"), finalUrl, width, height));
                }
            }
        }
        return scenes;
    }

    /**
     * Restore image from BLOB to a local temporary file to be usable by JavaFX
     * WebView/ImageView.
     */
    private String restoreImageFromBlob(int storyId, int sceneIndex, String originalUrl, byte[] blobData) {
        if (blobData == null || blobData.length == 0) {
            return originalUrl;
        }

        try {
            // Use unified cache directory from ConfigService
            Path cachePath = com.boilerplate.app.service.ConfigService.getInstance().getCacheDirectory();
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
            }

            // Simple caching strategy using hash of data or ID combination
            // Using ID combo ensures uniqueness per scene version
            String filename = "img_" + storyId + "_" + sceneIndex + ".png";
            File imageFile = cachePath.resolve(filename).toFile();

            // Optimization: Skip writing if file exists and size matches
            if (imageFile.exists() && imageFile.length() == blobData.length) {
                // logger.debug("Image already restored: {}", filename);
                return imageFile.toURI().toString();
            }

            // Write to file
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                fos.write(blobData);
            }

            return imageFile.toURI().toString();
        } catch (IOException e) {
            logger.error("Failed to restore image from BLOB", e);
            return originalUrl; // Fallback
        }
    }

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

    private byte[] fetchImageBytes(String urlString) {
        if (urlString == null || urlString.isEmpty())
            return null;
        try {
            URI uri = URI.create(urlString);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                Path path = Path.of(uri);
                if (Files.exists(path)) {
                    return Files.readAllBytes(path);
                }
            } else {
                // For remote URLs, we might want to be careful about network calls on save
                // But since WebViewParser already caches them to file, we likely only see
                // file:// URLs here
                // If it IS http, we try to download
                try (InputStream in = uri.toURL().openStream()) {
                    return in.readAllBytes();
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to read image bytes for persistence: {}", urlString, e);
        }
        return null;
    }
}
