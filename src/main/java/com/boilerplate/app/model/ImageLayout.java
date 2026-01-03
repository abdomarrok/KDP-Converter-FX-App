package com.boilerplate.app.model;

/**
 * Image layout options for KDP templates.
 * Defines how images and text are positioned on each page.
 */
public enum ImageLayout {
    /**
     * Image takes entire page, text on next page.
     * Best for: Children's picture books with large illustrations.
     */
    FULL_PAGE_IMAGE("Full Page Image", "Image on one page, text on the next"),

    /**
     * Image at top (60%), text below (40%).
     * Best for: Children's books with text under illustrations.
     */
    TOP_IMAGE_TEXT_BELOW("Image Top, Text Below", "Image at top, text underneath"),

    /**
     * Image on left (40%), text on right (60%).
     * Best for: Chapter books with inline illustrations.
     */
    SIDEBAR_IMAGE("Sidebar Image", "Image on side, text beside it"),

    /**
     * No image, full text page.
     * Best for: Novels, text-heavy content.
     */
    TEXT_ONLY("Text Only", "Full text, no image");

    private final String displayName;
    private final String description;

    ImageLayout(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
