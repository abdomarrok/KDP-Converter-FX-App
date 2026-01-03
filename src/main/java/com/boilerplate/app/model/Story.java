package com.boilerplate.app.model;

import java.util.List;

public class Story {
    private String title;
    private String author;
    private List<Scene> scenes;

    private int id;

    public Story() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Story(String title, String author, List<Scene> scenes) {
        this.title = title;
        this.author = author;
        this.scenes = scenes;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    @Override
    public String toString() {
        return title + " (" + id + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Story story = (Story) o;

        if (id != story.id)
            return false;
        if (title != null ? !title.equals(story.title) : story.title != null)
            return false;
        return author != null ? author.equals(story.author) : story.author == null;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (author != null ? author.hashCode() : 0);
        result = 31 * result + id;
        return result;
    }

    public List<Scene> getScenes() {
        return scenes;
    }

    public void setScenes(List<Scene> scenes) {
        this.scenes = scenes;
    }
}
