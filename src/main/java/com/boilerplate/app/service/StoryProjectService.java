package com.boilerplate.app.service;

import com.boilerplate.app.model.Scene;
import com.boilerplate.app.model.Story;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for saving and loading story projects.
 * Stores stories as JSON files in the user's .storyforge directory.
 */
public class StoryProjectService {

    private static final Logger logger = LogManager.getLogger(StoryProjectService.class);

    private static final Path PROJECTS_DIR = Paths.get(
            System.getProperty("user.home"), ".storyforge", "projects");

    private final ObjectMapper objectMapper;

    public StoryProjectService() {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        ensureProjectsDir();
    }

    /**
     * Save a story to disk.
     * 
     * @return The saved file path
     */
    public File saveStory(Story story) throws IOException {
        String filename = generateFilename(story.getTitle()) + ".json";
        File file = PROJECTS_DIR.resolve(filename).toFile();

        objectMapper.writeValue(file, story);
        logger.info("Story saved: {}", file.getAbsolutePath());

        return file;
    }

    /**
     * Save story to a specific file.
     */
    public void saveStory(Story story, File file) throws IOException {
        objectMapper.writeValue(file, story);
        logger.info("Story saved: {}", file.getAbsolutePath());
    }

    /**
     * Load a story from file.
     */
    public Story loadStory(File file) throws IOException {
        Story story = objectMapper.readValue(file, Story.class);
        logger.info("Story loaded: {} ({} scenes)", story.getTitle(), story.getScenes().size());
        return story;
    }

    /**
     * Get list of saved projects.
     */
    public List<File> getSavedProjects() {
        try {
            return Files.list(PROJECTS_DIR)
                    .filter(p -> p.toString().endsWith(".json"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Failed to list projects", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get the last saved project (most recently modified).
     */
    public File getLastProject() {
        List<File> projects = getSavedProjects();
        if (projects.isEmpty()) {
            return null;
        }

        return projects.stream()
                .max((f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()))
                .orElse(null);
    }

    /**
     * Delete a project file.
     */
    public boolean deleteProject(File file) {
        boolean deleted = file.delete();
        if (deleted) {
            logger.info("Project deleted: {}", file.getName());
        }
        return deleted;
    }

    private void ensureProjectsDir() {
        try {
            if (!Files.exists(PROJECTS_DIR)) {
                Files.createDirectories(PROJECTS_DIR);
                logger.info("Created projects directory: {}", PROJECTS_DIR);
            }
        } catch (IOException e) {
            logger.error("Failed to create projects directory", e);
        }
    }

    private String generateFilename(String title) {
        if (title == null || title.isBlank()) {
            return "story_" + System.currentTimeMillis();
        }
        return title.replaceAll("[^a-zA-Z0-9.-]", "_").toLowerCase();
    }
}
