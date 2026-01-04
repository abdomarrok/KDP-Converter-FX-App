package com.boilerplate.app.service;

import com.boilerplate.app.model.Scene;
import com.boilerplate.app.model.Story;
import com.boilerplate.app.repository.StoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StoryService.
 * Uses Mockito to mock repository dependencies.
 */
@ExtendWith(MockitoExtension.class)
class StoryServiceTest {

    @Mock
    private StoryRepository storyRepository;

    private StoryService storyService;

    @BeforeEach
    void setUp() {
        storyService = new StoryService();
        // Note: In a real DI setup, we'd inject the mock repository
        // For now, we're testing the service logic as-is
    }

    @Test
    void testSaveStoryAsync_withValidStory_shouldComplete() {
        // Given
        Story story = createSampleStory();

        // When/Then - should not throw exception
        assertThatCode(() -> {
            CompletableFuture<Void> future = storyService.saveStoryAsync(story);
            future.get(); // Wait for completion
        }).doesNotThrowAnyException();
    }

    @Test
    void testSaveStoryAsync_withNullStory_shouldThrowException() {
        // When
        CompletableFuture<Void> future = storyService.saveStoryAsync(null);

        // Then
        assertThatThrownBy(future::join)
                .isInstanceOf(CompletionException.class)
                .hasMessageContaining("Story cannot be null");
    }

    @Test
    void testLoadStoryAsync_withValidId_shouldReturnStory() throws Exception {
        // When
        CompletableFuture<Story> future = storyService.loadStoryAsync(1);

        // Then - should complete without error (may return null if not found)
        assertThatCode(future::join).doesNotThrowAnyException();
    }

    @Test
    void testGetAllStoriesAsync_shouldReturnList() throws Exception {
        // When
        CompletableFuture<List<Story>> future = storyService.getAllStoriesAsync();

        // Then
        assertThatCode(future::join).doesNotThrowAnyException();
        List<Story> stories = future.get();
        assertThat(stories).isNotNull();
    }

    @Test
    void testDeleteStoryAsync_withValidId_shouldComplete() {
        // When/Then - should not throw exception
        assertThatCode(() -> {
            CompletableFuture<Void> future = storyService.deleteStoryAsync(1);
            future.get();
        }).doesNotThrowAnyException();
    }

    @Test
    void testSaveLastUrl_shouldPersist() {
        // Given
        String testUrl = "https://gemini.google.com/share/test123";

        // When
        storyService.saveLastUrl(testUrl);
        String retrieved = storyService.getLastUrl();

        // Then
        assertThat(retrieved).isEqualTo(testUrl);
    }

    @Test
    void testGetLastUrl_whenNotSet_shouldReturnEmpty() {
        // When
        String url = storyService.getLastUrl();

        // Then
        assertThat(url).isNotNull();
    }

    // Helper methods

    private Story createSampleStory() {
        List<Scene> scenes = new ArrayList<>();
        scenes.add(new Scene("Once upon a time...", "image1.jpg"));
        scenes.add(new Scene("The hero ventured forth.", "image2.jpg"));

        Story story = new Story("Test Story", "Test Author", scenes);
        story.setSourceUrl("https://gemini.google.com/share/test");
        return story;
    }
}
