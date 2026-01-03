# StoryForge (Gemini to KDP Converter)

A powerful desktop application built with **JavaFX** that converts **Google Gemini** generated stories into professional, print-ready PDF books for **Amazon KDP**.

## 🚀 Overview

StoryForge streamlines the process of publishing AI-generated children's books. It intelligently extracts content from Gemini shared links, provides a robust **Scene Editor**, and generates KDP-compliant PDFs with dynamic layouts.

## ✨ Key Features

### 📖 Story Extraction & Editing

- **Smart Extraction**: Automatically parses text and images from Gemini shared URLs.
- **Scene Editor**:
  - Edit scene text and preview images.
  - Reorder scenes via buttons (Up/Down).
  - Delete unwanted scenes.
  - **Save/Load Projects**: Persist your work to JSON files (`~/.storyforge/projects`) and resume anytime.

### 🎨 Visual Layout & KDP Support

- **Dynamic KDP Templates**: Supports standard KDP book sizes:
  - Standard Paperback (6x9")
  - Picture Book (8.5x8.5")
  - Workbook (8.5x11")
  - Large Print (8x10")
- **Layout Options**:
  - **Full Page Image**: Image dominates the page, text on next page or overlay (layout dependent).
  - **Top Image, Text Below**: Classic picture book style.
  - **Sidebar Image**: Image on left, text on right.
  - **Text Only**: For chapter books.
- **Dynamic Resizing**: PDF engine (JasperReports) automatically adjusts margins and content bounds to fit the selected page size.

### 🛠️ Advanced Technical Features

- **URL History**: Automatically remembers last loaded URL (`~/.storyforge/url_history.json`).
- **Authenticated Downloads**: Bypasses CORS restrictions by using the WebView's authenticated session.
- **Local Caching**: Caches images for fast previews.
- **Parallel Processing**: Background tasks ensure UI responsiveness.

## 🛠️ Technology Stack

- **Language**: Java 21 (LTS)
- **Framework**: JavaFX 21
- **Build Tool**: Maven
- **PDF Engine**: JasperReports 7.0 (Dynamic Design)
- **JSON Processing**: Jackson Databind
- **Logging**: Log4j2

## 📋 Prerequisites

- **Java 21 SDK** (Required for modern JavaFX features)
- **Maven** (Apache Maven 3.8+)

## 🏃‍♂️ How to Run

### Using IDE (Recommended)

Open the project in **IntelliJ IDEA** or **VS Code**:

1. Reload/Sync Maven project.
2. Run `com.boilerplate.app.App`.

### Using Command Line

```powershell
mvn clean javafx:run
```

### Using Helper Script

Run `run.bat` in the root directory.

## 📄 License

This project is licensed for personal and commercial use.
