package com.boilerplate.app.model;

import java.util.List;

public class Story {
    private String title;
    private String author;
    private List<Scene> scenes;

    public Story() {
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

    public List<Scene> getScenes() {
        return scenes;
    }

    public void setScenes(List<Scene> scenes) {
        this.scenes = scenes;
    }
}
