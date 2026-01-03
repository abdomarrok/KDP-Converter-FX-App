package com.boilerplate.app.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database connection manager using HikariCP connection pooling.
 * Supports both embedded and server database modes.
 */
public class DatabaseConnection {
    private static final Logger logger = LogManager.getLogger(DatabaseConnection.class);

    private static String DATABASE_NAME = "storyforge";
    private static String DATABASE_HOST = "localhost";
    private static int DATABASE_PORT = 3306;
    private static String DATABASE_USER = "root";
    private static String DATABASE_PASS = "";

    private static HikariDataSource dataSource;
    private static DatabaseConnection instance;

    private DatabaseConnection() throws SQLException {
        initializePool();
        createTablesIfNotExist();
    }

    /**
     * Configure database connection parameters before initialization.
     */
    public static void configure(String host, int port, String dbName, String user, String pass) {
        DATABASE_HOST = host;
        DATABASE_PORT = port;
        DATABASE_NAME = dbName;
        DATABASE_USER = user;
        DATABASE_PASS = pass;
    }

    private void initializePool() {
        HikariConfig config = new HikariConfig();
        // JDBC URL for MariaDB
        String jdbcUrl = String.format("jdbc:mariadb://%s:%d/%s",
                DATABASE_HOST, DATABASE_PORT, DATABASE_NAME);

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(DATABASE_USER);
        config.setPassword(DATABASE_PASS);

        // Pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(1800000);

        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
        logger.info("Database connection pool initialized for: " + jdbcUrl);
    }

    /**
     * Get the singleton instance.
     */
    public static synchronized DatabaseConnection getInstance() throws SQLException {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * Get a connection from the pool.
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Shutdown the connection pool.
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed.");
        }
    }

    /**
     * Create default tables if they don't exist.
     */
    private void createTablesIfNotExist() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // Story metadata table
            String createStoriesTable = """
                        CREATE TABLE IF NOT EXISTS stories (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            title VARCHAR(255) NOT NULL,
                            author VARCHAR(255),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        )
                    """;
            stmt.execute(createStoriesTable);

            // Scenes table (relational)
            // Storing scenes as detailed rows with BLOB images
            String createScenesTable = """
                        CREATE TABLE IF NOT EXISTS scenes (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            story_id INT NOT NULL,
                            scene_index INT NOT NULL,
                            text TEXT,
                            image_url TEXT, -- Original URL/Filename reference
                            image_data LONGBLOB, -- Actual image binary
                            FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE
                        )
                    """;
            stmt.execute(createScenesTable);

            // Settings table for key-value storage (e.g. last URL)
            String createSettingsTable = """
                        CREATE TABLE IF NOT EXISTS settings (
                            key_name VARCHAR(50) PRIMARY KEY,
                            value TEXT
                        )
                    """;
            stmt.execute(createSettingsTable);

            logger.info("Database tables verified/created successfully.");
        } catch (SQLException e) {
            logger.error("Error creating tables", e);
        }
    }
}
