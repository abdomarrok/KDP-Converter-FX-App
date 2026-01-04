
package com.boilerplate.app.model;

public class Scene {
    private String text;
    private String imageUrl;
    private int imageWidth;
    private int imageHeight;

    public Scene() {
    }

    public Scene(String text, String imageUrl) {
        this.text = text;
        this.imageUrl = imageUrl;
    }

    public Scene(String text, String imageUrl, int imageWidth, int imageHeight) {
        this.text = text;
        this.imageUrl = imageUrl;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
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

    public int getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
    }

    @Override
    public String toString() {
        return "Scene{" +
                "text='" + (text != null ? text.substring(0, Math.min(text.length(), 20)) + "..." : "null") + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
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
