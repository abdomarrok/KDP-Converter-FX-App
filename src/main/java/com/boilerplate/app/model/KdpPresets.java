package com.boilerplate.app.model;

import java.util.Arrays;
import java.util.List;

/**
 * Predefined KDP template presets.
 * Based on proper Amazon KDP specifications regarding bleed and safe zones.
 * 
 * BLEED RULES:
 * If a book has bleed, the page size must represent the Trim Size + Bleed.
 * Width: Trim Width + 0.125"
 * Height: Trim Height + 0.25" (0.125" top + bottom)
 * 
 * MARGIN RULES:
 * - No Bleed: Min 0.25" outside.
 * - With Bleed: Min 0.375" outside (safe zone + bleed).
 * - Gutter (Inside): 0.375" for 24-150 pages.
 */
public final class KdpPresets {

        private KdpPresets() {
                // Utility class
        }

        // ==========================================
        // 6x9" Standard Paperback
        // ==========================================

        /**
         * Standard Paperback 6x9" (No Bleed).
         * Exact Trim Size: 6.0" x 9.0"
         */
        public static final KdpTemplate PAPERBACK_6X9_NO_BLEED = new KdpTemplate(
                        "Paperback 6x9\" (No Bleed)",
                        6.0, 9.0,
                        ImageLayout.TOP_IMAGE_TEXT_BELOW,
                        0.5, 0.5, // Generous top/bottom
                        0.625, 0.5, // Inside (Gutter) / Outside
                        12,
                        "Georgia",
                        false);

        /**
         * Standard Paperback 6x9" (With Bleed).
         * Document Size: 6.125" x 9.25" (Adds 0.125" width, 0.25" height)
         * Margins must be larger to ensure content stays in safe zone.
         */
        public static final KdpTemplate PAPERBACK_6X9_BLEED = new KdpTemplate(
                        "Paperback 6x9\" (Full Bleed)",
                        6.125, 9.25,
                        ImageLayout.FULL_PAGE_IMAGE,
                        0.375, 0.375, // Top/Bottom (includes bleed)
                        0.5, 0.375, // Inside / Outside (includes bleed)
                        12,
                        "Georgia",
                        true);

        // ==========================================
        // 8.5x8.5" Square Picture Book
        // ==========================================

        /**
         * Picture Book 8.5x8.5" (No Bleed).
         */
        public static final KdpTemplate PICTURE_BOOK_8X8_NO_BLEED = new KdpTemplate(
                        "Square 8.5x8.5\" (No Bleed)",
                        8.5, 8.5,
                        ImageLayout.TOP_IMAGE_TEXT_BELOW,
                        0.5, 0.5,
                        0.5, 0.5,
                        14,
                        "Comic Sans MS",
                        false);

        /**
         * Picture Book 8.5x8.5" (With Bleed).
         * Document Size: 8.625" x 8.75"
         */
        public static final KdpTemplate PICTURE_BOOK_8X8_BLEED = new KdpTemplate(
                        "Square 8.5x8.5\" (Full Bleed)",
                        8.625, 8.75,
                        ImageLayout.FULL_PAGE_IMAGE,
                        0.25, 0.25, // Minimal margins because it's mostly image
                        0.375, 0.25,
                        14,
                        "Comic Sans MS",
                        true);

        // ==========================================
        // 8.5x11" Workbook / Activity Book
        // ==========================================

        /**
         * Workbook 8.5x11" (No Bleed).
         */
        public static final KdpTemplate WORKBOOK_LETTER_NO_BLEED = new KdpTemplate(
                        "Workbook 8.5x11\" (No Bleed)",
                        8.5, 11.0,
                        ImageLayout.SIDEBAR_IMAGE,
                        0.5, 0.5,
                        0.75, 0.5,
                        12,
                        "Arial",
                        false);

        /**
         * Workbook 8.5x11" (With Bleed).
         * Document Size: 8.625" x 11.25"
         */
        public static final KdpTemplate WORKBOOK_LETTER_BLEED = new KdpTemplate(
                        "Workbook 8.5x11\" (Full Bleed)",
                        8.625, 11.25,
                        ImageLayout.SIDEBAR_IMAGE,
                        0.375, 0.375,
                        0.75, 0.375,
                        12,
                        "Arial",
                        true);

        /**
         * Get all available presets.
         */
        public static List<KdpTemplate> getAllPresets() {
                return Arrays.asList(
                                PAPERBACK_6X9_NO_BLEED,
                                PAPERBACK_6X9_BLEED,
                                PICTURE_BOOK_8X8_NO_BLEED,
                                PICTURE_BOOK_8X8_BLEED,
                                WORKBOOK_LETTER_NO_BLEED,
                                WORKBOOK_LETTER_BLEED);
        }

        /**
         * Get the default preset.
         */
        public static KdpTemplate getDefault() {
                return PAPERBACK_6X9_NO_BLEED;
        }

        public static KdpTemplate findByName(String name) {
                return getAllPresets().stream()
                                .filter(t -> t.getName().equalsIgnoreCase(name))
                                .findFirst()
                                .orElse(getDefault());
        }
}
