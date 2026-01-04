package com.boilerplate.app.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SceneTest {

    @Test
    void testSceneCreation() {
        Scene scene = new Scene("Test text", "image.jpg", 800, 600);

        assertThat(scene.getText()).isEqualTo("Test text");
        assertThat(scene.getImageUrl()).isEqualTo("image.jpg");
        assertThat(scene.getImageWidth()).isEqualTo(800);
        assertThat(scene.getImageHeight()).isEqualTo(600);
    }

    @Test
    void testSceneSetters() {
        Scene scene = new Scene();
        scene.setText("New text");
        scene.setImageUrl("new.jpg");
        scene.setImageWidth(1024);
        scene.setImageHeight(768);

        assertThat(scene.getText()).isEqualTo("New text");
        assertThat(scene.getImageUrl()).isEqualTo("new.jpg");
        assertThat(scene.getImageWidth()).isEqualTo(1024);
        assertThat(scene.getImageHeight()).isEqualTo(768);
    }

    @Test
    void testEquality() {
        Scene scene1 = new Scene("text", "img.jpg");
        Scene scene2 = new Scene("text", "img.jpg");
        Scene scene3 = new Scene("other", "img.jpg");

        assertThat(scene1).isEqualTo(scene2);
        assertThat(scene1).isNotEqualTo(scene3);
        assertThat(scene1.hashCode()).isEqualTo(scene2.hashCode());
    }

    @Test
    void testToString() {
        Scene scene = new Scene("Short text", "img.jpg");
        assertThat(scene.toString()).contains("Short text", "img.jpg");
    }

    @Test
    void testToStringTruncation() {
        String longText = "This is a very long text that should be truncated in the string representation";
        Scene scene = new Scene(longText, "img.jpg");
        assertThat(scene.toString()).contains("This is a very long"); // First 20 chars
        assertThat(scene.toString()).contains("...");
    }
}
