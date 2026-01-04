# TODO: Gemini Story Extraction Issues

## Critical Problem: Only 1 Scene Extracted Instead of 10

### Current Behavior

1. User clicks "Extract"
2. JavaScript auto-rewinds to Page 1 ✅ (working)
3. Page 1 scanned successfully ✅ (working)
   - Text extracted: "Milo stood on Grandma's porch..."
   - Image URL extracted: "https://lh3.googleusercontent.com/gg/..."
   - Scene #1 collected
4. "Next" button clicked ✅ (working)
5. **setTimeout for next scan NEVER FIRES** ❌ (BROKEN)
6. After 2 minutes, Java timeout kills the extraction
7. Result: Only 1 scene out of 10 extracted

### Root Cause Identified

**Gemini's "Next Page" button destroys the JavaScript execution context.**

When the script clicks the "Next page" button:

```javascript
nextBtn.click();
pageCount++;
setTimeout(extractAndMove, 5000); // This callback NEVER executes!
```

The `setTimeout` callback is destroyed before it can fire. This happens because:

1. **Client-Side Routing**: Gemini likely uses a SPA framework (React/Vue) that unmounts/remounts components when navigating
2. **Context Reset**: The routing navigation clears pending timers and destroys the JavaScript scope
3. **No Re-Injection**: Our script runs once on initial page load and doesn't re-inject itself after navigation

### Evidence

From logs:

```
10:16:40 | Scanning Page 1 [IMG TXT]
10:16:40 | Collected scene #1
10:16:40 | Clicking Next...
[... 2 minute gap ...]
10:18:39 | Pipeline execution failed: Extraction timed out
```

The 2-minute gap proves the setTimeout never fired.

### What We've Tried (And Why It Didn't Work)

#### ❌ Attempt 1: Increase Timeout (3s → 5s)

- **Rationale**: Maybe page transitions need more time
- **Result**: Still times out. The callback isn't delayed, it's destroyed.

#### ❌ Attempt 2: Enhanced Visibility Detection

- **Rationale**: Maybe getVisible() was finding hidden elements from previous pages
- **Added**: Viewport position checks, opacity checks, getBoundingClientRect validation
- **Result**: Still times out. Visibility wasn't the issue.

#### ❌ Attempt 3: Detailed Logging

- **Rationale**: Log text/image previews to debug duplicate detection
- **Result**: Confirmed extraction works on Page 1, but never reaches Page 2

### Solution Approaches

#### Option A: Java-Controlled Iteration (Recommended)

Instead of JavaScript controlling the entire flow, have Java orchestrate:

```java
for (int page = 1; page <= totalPages; page++) {
    // 1. Inject simple extraction script (no setTimeout)
    Scene scene = extractCurrentPage(webEngine);

    // 2. Collect the scene
    collectedScenes.add(scene);

    // 3. Java clicks "Next" button
    webEngine.executeScript("document.querySelector('button[aria-label=\"Next page\"]').click()");

    // 4. Wait for page load (use WebEngine.getLoadWorker())
    waitForPageLoad();
}
```

**Pros**:

- Java maintains state, unaffected by page navigation
- Simpler JavaScript (just extract, don't navigate)
- Can use WebEngine's load state detection

**Cons**:

- Requires refactoring service layer
- More complex Java threading (must be on FX thread)

#### Option B: MutationObserver with Re-Injection

Watch for DOM changes and re-inject the extraction logic:

```javascript
var observer = new MutationObserver(function () {
  if (!window.__extractorActive) {
    window.__extractorActive = true;
    // Re-run extraction logic
    extractAndMove();
  }
});
observer.observe(document.body, { childList: true, subtree: true });
```

**Pros**:

- Pure JavaScript solution
- Survives page navigations

**Cons**:

- May fire multiple times per navigation
- Hard to debug
- Fragile (depends on DOM mutation patterns)

#### Option C: Puppeteer/Playwright (Long-term)

Replace JavaFX WebView with headless browser:

**Pros**:

- Full Chrome/Chromium API
- Better debugging
- Mature navigation handling

**Cons**:

- Major architectural change
- External dependency
- Requires rewriting entire parser

### Recommended Next Step

Implement **Option A** (Java-Controlled Iteration):

1. Create `extractSinglePage(WebEngine)` method that returns one Scene
2. Create `clickNextButton(WebEngine)` helper
3. In `parseCurrentPage()`, loop through pages with Java orchestrating the flow
4. Use `webEngine.getLoadWorker().stateProperty()` to detect when each page finishes loading

This is the most reliable solution and doesn't require complex JavaScript gymnastics.

## Secondary Issue: Image URLs Not Always Extracted

Even when extraction works, some scenes may not have image URLs. This is a separate issue from the pagination problem.

### Possible Causes

1. **Images Still Loading**: Even with visibility checks, images might not have `src` populated yet
2. **Lazy Loading**: Gemini may delay setting the `src` attribute until the image is in viewport
3. **Incorrect Selectors**: Our selectors might not match Gemini's actual DOM structure

### Solution

Add retry logic with brief delays to wait for images to load before extracting.

## Files Involved

- `WebViewParser.java` - Extraction orchestration (needs refactoring)
- `ExtractionService.java` - Service layer (may need threading updates)
- `MainController.java` - UI updates

## Priority

**CRITICAL** - Core functionality completely broken. Extraction returns 1/10 scenes consistently.
