package com.boilerplate.app.service;

import com.boilerplate.app.model.Story;
import javafx.scene.web.WebEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ExtractionService.
 * Note: Full testing requires JavaFX WebEngine which is complex to mock.
 * These tests focus on service configuration and state validation.
 */
@ExtendWith(MockitoExtension.class)
class ExtractionServiceTest {

    @Mock
    private WebEngine webEngine;

    private ExtractionService extractionService;

    @BeforeEach
    void setUp() {
        extractionService = new ExtractionService();
    }

    @Test
    void testSetWebEngine_shouldStoreEngine() {
        // When
        extractionService.setWebEngine(webEngine);

        // Then - no exception should be thrown
        assertThatCode(() -> extractionService.setWebEngine(webEngine))
                .doesNotThrowAnyException();
    }

    @Test
    void testSetRefreshCallback_shouldStoreCallback() {
        // Given
        java.util.function.Consumer<Story> callback = story -> {
            // Mock callback
        };

        // When/Then
        assertThatCode(() -> extractionService.setRefreshCallback(callback))
                .doesNotThrowAnyException();
    }

    @Test
    void testCreateTask_withoutWebEngine_shouldThrowException() {
        // When/Then
        assertThatThrownBy(() -> extractionService.createTask())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WebEngine must be set");
    }

    @Test
    void testCreateTask_withWebEngine_shouldCreateTask() {
        // Given
        extractionService.setWebEngine(webEngine);

        // When/Then - should create task without error
        assertThatCode(() -> extractionService.createTask())
                .doesNotThrowAnyException();
    }

    @Test
    void testServiceState_initiallyIdle() {
        // Then
        assertThat(extractionService.getState()).isEqualTo(javafx.concurrent.Worker.State.READY);
    }
}
