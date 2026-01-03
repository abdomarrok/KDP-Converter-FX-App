package com.boilerplate.app.util.database;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Embedded MariaDB database for standalone/development mode.
 * Uses MariaDB4j for zero-configuration database.
 */
public class EmbeddedDatabase {
    private static final Logger logger = LogManager.getLogger(EmbeddedDatabase.class);

    private DB db;
    private static final int DEFAULT_PORT = 3307;
    private static final String DATA_DIR = "./embedded-db";

    /**
     * Start the embedded database.
     */
    public void startDatabase() throws Exception {
        DBConfigurationBuilder config = DBConfigurationBuilder.newBuilder();
        config.setPort(DEFAULT_PORT);
        config.setDataDir(DATA_DIR);

        db = DB.newEmbeddedDB(config.build());
        db.start();
        db.createDB("storyforge");

        // Configure DatabaseConnection to use embedded database
        DatabaseConnection.configure("localhost", DEFAULT_PORT, "storyforge", "root", "");

        logger.info("Embedded database started on port " + DEFAULT_PORT);
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
