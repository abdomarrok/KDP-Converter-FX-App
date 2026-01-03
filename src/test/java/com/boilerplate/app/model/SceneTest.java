package com.boilerplate.app.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SceneTest {

    @Test
    void testSceneCreation() {
        Scene scene = new Scene();
        assertNull(scene.getText());
        assertNull(scene.getImageUrl());
    }

    @Test
    void testSceneWithData() {
        Scene scene = new Scene("Test text", "test.jpg");
        assertEquals("Test text", scene.getText());
        assertEquals("test.jpg", scene.getImageUrl());
    }

    @Test
    void testSceneSetters() {
        Scene scene = new Scene();
        scene.setText("New text");
        scene.setImageUrl("new.jpg");
        
        assertEquals("New text", scene.getText());
        assertEquals("new.jpg", scene.getImageUrl());
    }

    @Test
    void testSceneEquality() {
        Scene scene1 = new Scene("Text", "image.jpg");
        Scene scene2 = new Scene("Text", "image.jpg");
        Scene scene3 = new Scene("Different", "image.jpg");
        
        assertEquals(scene1, scene2);
        assertNotEquals(scene1, scene3);
    }

    @Test
    void testSceneToString() {
        Scene scene = new Scene("Short text", "image.jpg");
        String str = scene.toString();
        assertTrue(str.contains("Scene"));
        assertTrue(str.contains("hasImage"));
    }
}

