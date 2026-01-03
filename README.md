# StoryForge (Gemini to KDP Converter)

A professional desktop application built with **JavaFX** that converts **Google Gemini** generated stories into print-ready PDF picture books for **Amazon KDP**.

## 🚀 Overview

StoryForge acts as a bridge between AI creativity and physical publishing. It extracts structured story content from Gemini shared links, provides a comprehensive **Studio Mode** for editing and layout control, and generates industry-standard PDFs ready for upload to Amazon KDP.

## ✨ Key Features

### 🕵️‍♂️ Hardened Story Extraction

- **Adaptive Parsing**: "System 2.0" logic intelligently parses text and images from various Gemini UI versions.
- **Authenticated Downloads**: Uses the WebView's active session to bypass CORS and download high-quality images.
- **Smart Deduplication**: Prevents duplicate scenes during scrolling extraction.

### 🖋️ Studio Mode (Editor)

- **Split-View Workflow**: Toggle between the **Browser** (Source) and **Editor** (Studio) in a seamless 2-column layout.
- **Scene Management**:
  - Reorder scenes with simple Up/Down controls.
  - Delete unwanted scenes.
  - **Custom Scene Titles**: Rename scenes for better organization.
- **Per-Page Overrides**:
  - **Layout Override**: Force specific pages to be "Full Page Image", "Text Only", etc., independent of the global template.
  - **Font Size Override**: Adjust text size for specific scenes (e.g., dense text pages).
  - **Image Replacement**: Swap out AI images with local files.

### 📚 KDP Template Support

Includes 6 standard presets compliant with Amazon KDP specifications:

- **Paperback 6x9"** (Standard & Full Bleed)
- **Picture Book 8.5x8.5"** (Standard & Full Bleed)
- **Workbook 8.5x11"** (Standard & Full Bleed)

### 💾 Robust Persistence

- **Embedded Database**: Uses an embedded **MariaDB** instance to safely store all your stories and metadata locally.
- **Auto-Save**: Changes to scenes and settings are persisted automatically.
- **URL History**: Remembers your last visited Gemini link for quick resumption.

## 🛠️ Technology Stack

- **Core**: Java 21 (LTS), JavaFX 21
- **UI Architecture**: Modular FXML with Controller Injection
- **Database**: Embedded MariaDB (via `ch.vorburger.mariaDB4j`) + HikariCP
- **PDF Engine**: JasperReports 7.0 (Dynamic Templates)
- **Build Tool**: Maven

## 🏃‍♂️ How to Run

### Quick Start

Simply run the helper script in the root directory:

```powershell
./run.bat
```

### From IDE (IntelliJ / VS Code)

1. Open the project folder.
2. Sync Maven dependencies.
3. Run `App.java`.

## 📄 License

This project is licensed for personal and commercial use.
