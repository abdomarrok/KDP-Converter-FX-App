package com.boilerplate.app.util;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * Centralized keyboard shortcut definitions for the application.
 */
public class KeyboardShortcuts {

    // File operations
    public static final KeyCombination SAVE_STORY = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination NEW_STORY = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination OPEN_STORY = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN);
    public static final KeyCombination LOAD_STORY = new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN);

    // Edit operations
    public static final KeyCombination TOGGLE_EDIT_MODE = new KeyCodeCombination(KeyCode.E,
            KeyCombination.CONTROL_DOWN);

    // Generation
    public static final KeyCombination GENERATE_PDF = new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN);

    // Navigation
    public static final KeyCombination REFRESH = new KeyCodeCombination(KeyCode.F5);
    public static final KeyCombination FOCUS_URL = new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN);

    // Help
    public static final KeyCombination SHOW_HELP = new KeyCodeCombination(KeyCode.F1);
    public static final KeyCombination SHOW_SHORTCUTS = new KeyCodeCombination(KeyCode.SLASH,
            KeyCombination.CONTROL_DOWN);

    /**
     * Returns a formatted string describing all keyboard shortcuts.
     */
    public static String getShortcutsHelpText() {
        return """
                File Operations:
                  Ctrl+S - Save current story
                  Ctrl+N - Create new story
                  Ctrl+O - Open story list
                  Ctrl+L - Load story from list

                Editing:
                  Ctrl+E - Toggle edit mode

                Generation:
                  Ctrl+G - Generate PDF

                Navigation:
                  F5 - Refresh current page

                Help:
                  F1 - Show help
                  Ctrl+/ - Show keyboard shortcuts
                """;
    }

    /**
     * Returns HTML formatted shortcut reference.
     */
    public static String getShortcutsHtmlTable() {
        return """
                <html>
                <style>
                    body { font-family: Arial, sans-serif; }
                    table { border-collapse: collapse; width: 100%; }
                    th { background-color: #4CAF50; color: white; text-align: left; padding: 8px; }
                    td { border: 1px solid #ddd; padding: 8px; }
                    tr:nth-child(even) { background-color: #f2f2f2; }
                    .shortcut { font-family: monospace; font-weight: bold; }
                </style>
                <body>
                <h2>Keyboard Shortcuts</h2>
                <table>
                    <tr><th>Action</th><th>Shortcut</th></tr>
                    <tr><td>Save story</td><td class='shortcut'>Ctrl+S</td></tr>
                    <tr><td>New story</td><td class='shortcut'>Ctrl+N</td></tr>
                    <tr><td>Open story list</td><td class='shortcut'>Ctrl+O</td></tr>
                    <tr><td>Toggle edit mode</td><td class='shortcut'>Ctrl+E</td></tr>
                    <tr><td>Generate PDF</td><td class='shortcut'>Ctrl+G</td></tr>
                    <tr><td>Refresh page</td><td class='shortcut'>F5</td></tr>
                    <tr><td>Show help</td><td class='shortcut'>F1</td></tr>
                    <tr><td>Show shortcuts</td><td class='shortcut'>Ctrl+/</td></tr>
                </table>
                </body>
                </html>
                """;
    }
}
