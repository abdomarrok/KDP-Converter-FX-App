package com.boilerplate.app.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

class StoryTest {

    @Test
    void testStoryCreation() {
        Story story = new Story();
        assertNull(story.getTitle());
        assertNull(story.getAuthor());
        assertNull(story.getScenes());
    }

    @Test
    void testStoryWithData() {
        List<Scene> scenes = new ArrayList<>();
        scenes.add(new Scene("Scene 1", "image1.jpg"));
        scenes.add(new Scene("Scene 2", "image2.jpg"));
        
        Story story = new Story("Test Story", "Test Author", scenes);
        
        assertEquals("Test Story", story.getTitle());
        assertEquals("Test Author", story.getAuthor());
        assertEquals(2, story.getScenes().size());
    }

    @Test
    void testStorySetters() {
        Story story = new Story();
        story.setTitle("New Title");
        story.setAuthor("New Author");
        
        List<Scene> scenes = new ArrayList<>();
        story.setScenes(scenes);
        
        assertEquals("New Title", story.getTitle());
        assertEquals("New Author", story.getAuthor());
        assertEquals(scenes, story.getScenes());
    }

    @Test
    void testStoryEquality() {
        Story story1 = new Story("Title", "Author", new ArrayList<>());
        story1.setId(1);
        
        Story story2 = new Story("Title", "Author", new ArrayList<>());
        story2.setId(1);
        
        Story story3 = new Story("Different", "Author", new ArrayList<>());
        story3.setId(2);
        
        assertEquals(story1, story2);
        assertNotEquals(story1, story3);
    }
}

