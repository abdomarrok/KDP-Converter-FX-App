package com.boilerplate.app.repository;

import com.boilerplate.app.repository.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SettingsRepository {

    private static final Logger logger = LogManager.getLogger(SettingsRepository.class);

    public void saveSetting(String key, String value) {
        String sql = "INSERT INTO settings (key_name, value) VALUES (?, ?) ON DUPLICATE KEY UPDATE value = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key);
            stmt.setString(2, value);
            stmt.setString(3, value);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error saving setting: {} = {}", key, value, e);
        }
    }

    public String getSetting(String key, String defaultValue) {
        String sql = "SELECT value FROM settings WHERE key_name = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving setting: {}", key, e);
        }
        return defaultValue;
    }
}
