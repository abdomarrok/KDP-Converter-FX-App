# StoryForge - Future Improvements Roadmap

## 🎯 High Priority (Next Steps)

### 1. **Image Management Enhancements**

- **Improvements**:
  - Implement image compression/optimization before PDF generation
  - Support multiple image formats (WebP, AVIF)
  - Add strict size limits and tunable LRU eviction policies
  - Implement image deduplication (hash-based)
  - ✅ Remove watermark from AI generated images

### 2. **Database Schema & Migrations**

- **Improvements**:
  - Add indexes on frequently queried columns (story_id, updated_at)
  - Implement soft deletes instead of hard deletes
  - Add database backup/export functionality
  - Add data validation constraints

### 3. **Code Organization & Architecture**

- **Improvements**:
  - Fix `refreshSceneList()` logic clarification
  - Replace reflection-based field injection with proper dependency injection (Spring/Guice)
  - Extract constants to configuration classes
  - Split large methods (e.g., `WebViewParser.injectExtractionScript()`)
  - Use builder pattern for complex objects

### 4. **PDF Generation Enhancements**

- **Improvements**:
  - Support per-scene layout overrides
  - Add page numbering options
  - Support custom fonts (TTF/OTF loading)
  - Add cover page generation
  - Support multi-column text layouts
  - Add watermark support
  - Implement PDF/A compliance option

---

## � Medium Priority (Enhancements)

### 5. **User Experience**

- **Improvements**:
  - Implement undo/redo for scene edits
  - Add drag-and-drop for scene reordering
  - Add story preview before PDF generation
  - Add recent stories menu
  - Add story search/filter functionality

### 6. **Export & Import**

- **Improvements**:
  - Export stories to JSON format
  - Import stories from JSON
  - Support multiple PDF export formats (single-page, spreads)
  - Export to EPUB format
  - Add batch export for multiple stories
  - Support cloud storage integration (Google Drive, Dropbox)

### 7. **UI/UX Polish**

- **Improvements**:
  - Add dark mode support (fully integrated themes)
  - Improve responsive layout for different window sizes
  - Implement scene thumbnails in list view
  - Add zoom controls for page preview
  - Show image loading indicators
  - Add splash screen on startup
  - Improve empty states with helpful illustrations

### 8. **Performance Optimizations**

- **Improvements**:
  - Lazy load scenes in list view (virtual scrolling)
  - Implement image lazy loading in editor
  - Cache compiled JasperReports templates
  - Add database connection pooling metrics
  - Optimize image processing (resize on download, not on display)
  - Add memory usage monitoring

---

## 🟢 Low Priority (Long-term)

### 9. **Feature Requests**

- **AI Integration**: Direct API integration with Gemini/Claude/GPT
- **Collaboration**: Multi-user support, story sharing
- **Advanced Editing**: Rich text editor, spell checker, image editing
- **Analytics**: Word count, reading level, export stats

### 10. **Documentation**

- **Improvements**:
  - Add JavaDoc to all public methods
  - Create architecture diagram
  - Create video tutorials
  - Document all configuration options

### 11. **Security**

- **Improvements**:
  - Add rate limiting for image downloads
  - Implement secure storage for sensitive data
  - Scan downloaded images for malicious content

### 12. **Accessibility**

- **Improvements**:
  - Add ARIA labels (where applicable in JavaFX)
  - Support full keyboard navigation
  - Add high contrast mode
  - Implement font size scaling

---

## 📦 Dependencies to Consider (Future)

- **DI**: Google Guice or Dagger
- **Validation**: Bean Validation (JSR-303)
- **Bytecode/PDF**: iText (alternative to JasperReports)
