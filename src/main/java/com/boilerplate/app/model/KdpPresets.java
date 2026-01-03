package com.boilerplate.app.model;

import java.util.Arrays;
import java.util.List;

/**
 * Predefined KDP template presets.
 * Based on Amazon KDP specifications.
 */
public final class KdpPresets {

    private KdpPresets() {
        // Utility class, no instantiation
    }

    /**
     * Standard Paperback 6x9" - Most popular size for novels and non-fiction.
     */
    public static final KdpTemplate PAPERBACK_6X9 = new KdpTemplate(
            "Standard Paperback (6x9)",
            6.0, 9.0,
            ImageLayout.TOP_IMAGE_TEXT_BELOW,
            0.5, 0.5, // top/bottom margins
            0.75, 0.5, // inside/outside margins (binding gutter)
            14,
            "Georgia",
            false);

    /**
     * Children's Picture Book 8.5x8.5" - Square format for picture books.
     */
    public static final KdpTemplate PICTURE_BOOK_SQUARE = new KdpTemplate(
            "Picture Book (8.5x8.5)",
            8.5, 8.5,
            ImageLayout.FULL_PAGE_IMAGE,
            0.375, 0.375, // minimal margins for full-bleed look
            0.5, 0.375,
            18,
            "Comic Sans MS",
            true);

    /**
     * Workbook/Activity Book 8.5x11" - Letter size for workbooks.
     */
    public static final KdpTemplate WORKBOOK_LETTER = new KdpTemplate(
            "Workbook (8.5x11)",
            8.5, 11.0,
            ImageLayout.SIDEBAR_IMAGE,
            0.5, 0.5,
            0.75, 0.5,
            12,
            "Arial",
            false);

    /**
     * Large Print 8x10" - For accessibility.
     */
    public static final KdpTemplate LARGE_PRINT = new KdpTemplate(
            "Large Print (8x10)",
            8.0, 10.0,
            ImageLayout.TOP_IMAGE_TEXT_BELOW,
            0.75, 0.75,
            0.875, 0.625,
            18,
            "Verdana",
            false);

    /**
     * Get all available presets.
     */
    public static List<KdpTemplate> getAllPresets() {
        return Arrays.asList(
                PAPERBACK_6X9,
                PICTURE_BOOK_SQUARE,
                WORKBOOK_LETTER,
                LARGE_PRINT);
    }

    /**
     * Get the default preset (6x9 paperback).
     */
    public static KdpTemplate getDefault() {
        return PAPERBACK_6X9;
    }

    /**
     * Find preset by name.
     */
    public static KdpTemplate findByName(String name) {
        return getAllPresets().stream()
                .filter(t -> t.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(getDefault());
    }
}
