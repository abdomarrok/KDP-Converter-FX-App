package com.boilerplate.app.model;

public class Scene {
    private String text;
    private String imageUrl;

    public Scene() {
    }

    public Scene(String text, String imageUrl) {
        this.text = text;
        this.imageUrl = imageUrl;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return "Scene{" +
                "text='" + (text != null && text.length() > 20 ? text.substring(0, 20) + "..." : text) + '\'' +
                ", hasImage=" + (imageUrl != null && !imageUrl.isEmpty()) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Scene scene = (Scene) o;

        if (text != null ? !text.equals(scene.text) : scene.text != null)
            return false;
        return imageUrl != null ? imageUrl.equals(scene.imageUrl) : scene.imageUrl == null;
    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + (imageUrl != null ? imageUrl.hashCode() : 0);
        return result;
    }
}
