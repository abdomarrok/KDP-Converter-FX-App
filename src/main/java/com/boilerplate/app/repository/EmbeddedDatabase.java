package com.boilerplate.app.repository;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import com.boilerplate.app.config.AppConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Embedded MariaDB database for standalone/development mode.
 * Uses MariaDB4j for zero-configuration database.
 */
public class EmbeddedDatabase {
    private static final Logger logger = LogManager.getLogger(EmbeddedDatabase.class);

    private DB db;
    private final int port;
    private final String dataDir;

    public EmbeddedDatabase() {
        this(3307, "./embedded-db");
    }

    public EmbeddedDatabase(int port, String dataDir) {
        this.port = port;
        this.dataDir = dataDir;
    }

    /**
     * Start the embedded database.
     */
    public void startDatabase() throws Exception {
        // Configure AppConfig to use embedded database
        AppConfig configService = AppConfig.getInstance();
        configService.setProperty("db.host", "localhost");
        configService.setProperty("db.port", String.valueOf(port));
        configService.setProperty("db.name", "storyforge");
        configService.setProperty("db.user", "root");
        configService.setProperty("db.password", "");
        DBConfigurationBuilder config = DBConfigurationBuilder.newBuilder();
        config.setPort(port);
        config.setDataDir(dataDir);

        db = DB.newEmbeddedDB(config.build());
        db.start();
        db.createDB("storyforge");

        logger.info("Embedded database started on port " + port);
    }

    /**
     * Stop the embedded database.
     */
    public void stopDatabase() throws Exception {
        if (db != null) {
            db.stop();
            logger.info("Embedded database stopped.");
        }
    }
}
