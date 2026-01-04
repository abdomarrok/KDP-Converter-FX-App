package com.boilerplate.app.repository;

import com.boilerplate.app.config.AppConfig;
import com.boilerplate.app.model.Scene;
import com.boilerplate.app.model.Story;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Test ensuring StoryService and Repository work with a real
 * database.
 * Uses Embedded MariaDB (MariaDB4j) for isolated testing.
 */
class DatabaseIntegrationTest {

    private static EmbeddedDatabase embeddedDb;
    private static final int TEST_PORT = 3308; // Use different port than dev
    private StoryRepository repository;

    @BeforeAll
    static void startDatabase() throws Exception {
        // Start embedded MariaDB
        // Clean up previous test dir if exists
        deleteDirectory(Path.of("target/test-db"));

        embeddedDb = new EmbeddedDatabase(TEST_PORT, "target/test-db");
        embeddedDb.startDatabase();

        // Configure AppConfig to point to this DB
        // (EmbeddedDatabase.startDatabase does this, but for default port. We need to
        // override.)
        AppConfig config = AppConfig.getInstance();
        config.setProperty("db.host", "localhost");
        config.setProperty("db.port", String.valueOf(TEST_PORT));
        config.setProperty("db.name", "storyforge");
        config.setProperty("db.user", "root");
        config.setProperty("db.password", "");

        // Force pool re-initialization?
        // DatabaseConnection is a singleton initialized in constructor.
        // We might need to "reset" it or use reflection if it's already initialized.
        // But for tests running in their own JVM/classloader, it should be fresh.
        // If DatabaseConnection is already initialized by other tests, we have a
        // problem.
        // We added a shutdown() method to DatabaseConnection. We should use it.
    }

    @AfterAll
    static void stopDatabase() throws Exception {
        DatabaseConnection.shutdown();
        if (embeddedDb != null) {
            embeddedDb.stopDatabase();
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        // Ensure connection pool is initialized (if shutdown previously)
        DatabaseConnection.getInstance();

        repository = new StoryRepository();

        // Clear tables
        repository.getAllStories().forEach(s -> {
            try {
                repository.deleteStory(s.getId());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    void testSaveAndLoadStory() throws SQLException {
        // Given
        Story story = new Story();
        story.setTitle("Integration Test Story");
        story.setAuthor("Tester");

        List<Scene> scenes = new ArrayList<>();
        scenes.add(new Scene("Scene 1", "http://example.com/1.jpg"));
        scenes.add(new Scene("Scene 2", "http://example.com/2.jpg"));
        story.setScenes(scenes);

        // When
        repository.saveStory(story);

        // Then
        assertThat(story.getId()).isGreaterThan(0);

        Story loaded = repository.loadStory(story.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getTitle()).isEqualTo("Integration Test Story");
        assertThat(loaded.getScenes()).hasSize(2);
        assertThat(loaded.getScenes().get(0).getText()).isEqualTo("Scene 1");
    }

    @Test
    void testUpdateStory() throws SQLException {
        // Given
        Story story = new Story("Original Title", "Author", new ArrayList<>());
        repository.saveStory(story);
        int id = story.getId();

        // When
        story.setTitle("Updated Title");
        repository.saveStory(story);

        // Then
        Story loaded = repository.loadStory(id);
        assertThat(loaded.getTitle()).isEqualTo("Updated Title");
        assertThat(repository.getAllStories()).hasSize(1);
    }

    private static void deleteDirectory(Path path) {
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
