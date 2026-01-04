package com.boilerplate.app.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/**
 * Additional tests for Scene model using AssertJ.
 */
class SceneTest {

    @Test
    void testSceneCreation() {
        // When
        Scene scene = new Scene();

        // Then
        assertThat(scene.getText()).isNull();
        assertThat(scene.getImageUrl()).isNull();
    }

    @Test
    void testSceneWithData() {
        // Given
        String text = "Once upon a time...";
        String imageUrl = "image.jpg";

        // When
        Scene scene = new Scene(text, imageUrl);

        // Then
        assertThat(scene.getText()).isEqualTo(text);
        assertThat(scene.getImageUrl()).isEqualTo(imageUrl);
    }

    @Test
    void testSceneSetters() {
        // Given
        Scene scene = new Scene();

        // When
        scene.setText("New text");
        scene.setImageUrl("new-image.jpg");
        scene.setImagePath("/path/to/image.jpg");
        scene.setPageNumber(1);

        // Then
        assertThat(scene.getText()).isEqualTo("New text");
        assertThat(scene.getImageUrl()).isEqualTo("new-image.jpg");
        assertThat(scene.getImagePath()).isEqualTo("/path/to/image.jpg");
        assertThat(scene.getPageNumber()).isEqualTo(1);
    }

    @Test
    void testSceneValidation_emptyText() {
        // Given
        Scene scene = new Scene("", "image.jpg");

        // Then
        assertThat(scene.getText()).isEmpty();
    }

    @Test
    void testSceneValidation_nullImageUrl() {
        // Given
        Scene scene = new Scene("Some text", null);

        // Then
        assertThat(scene.getImageUrl()).isNull();
    }

    @Test
    void testSceneEquality() {
        // Given
        Scene scene1 = new Scene("Text", "image.jpg");
        scene1.setId(1);

        Scene scene2 = new Scene("Text", "image.jpg");
        scene2.setId(1);

        Scene scene3 = new Scene("Different", "other.jpg");
        scene3.setId(2);

        // Then
        assertThat(scene1).isEqualTo(scene2);
        assertThat(scene1).isNotEqualTo(scene3);
    }

    @Test
    void testSceneToString() {
        // Given
        Scene scene = new Scene("Test text", "test.jpg");
        scene.setPageNumber(1);

        // When
        String result = scene.toString();

        // Then
        assertThat(result).contains("Scene");
        assertThat(result).contains("Test text");
    }
}
