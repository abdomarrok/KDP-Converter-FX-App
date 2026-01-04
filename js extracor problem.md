# Prompt for JavaScript Extraction Debugging

## Context

I need to create a JavaScript script that extracts content from a Gemini AI storybook page. The storybook shows one page at a time, with "Next" and "Previous" navigation buttons.

## Current Problem

My extraction script is detecting the same content on every page (same 465-character text) even though the visible page changes when I click "Next". This suggests my visibility detection isn't working correctly - it's probably finding hidden DOM elements from previous/next pages instead of just the currently visible page.

## Requirements

### What to Extract Per Page:

1. **Text**: The story text visible on the current page
2. **Image URL**: The image src attribute for the current page's illustration

### DOM Structure (Gemini Storybook):

- Uses client-side routing (SPA) - clicking "Next" doesn't reload the page
- Multiple pages exist in the DOM simultaneously (hidden/visible)
- Current page content is visible, other pages are hidden (likely with `display:none`, `visibility:hidden`, or positioned off-screen)
- Next button selector: `button[aria-label="Next page"]`
- Text might be in: `div.story-text-container`, `[class*="story-text"]`, or `p` tags
- Images might be in: `div.storybook-image img`, `storybook-page img`, or `img[src*="googleusercontent"]`

### Current Broken Code:

```javascript
(function () {
  // Helper to find TRULY visible element
  function getVisible(selector) {
    var els = Array.from(document.querySelectorAll(selector));
    return els.find(function (el) {
      if (!el.offsetParent) return false;

      var rect = el.getBoundingClientRect();
      if (rect.width === 0 || rect.height === 0) return false;

      var style = window.getComputedStyle(el);
      if (style.visibility === "hidden" || style.opacity === "0") return false;

      // Check if element is in viewport
      if (rect.bottom < 0 || rect.top > window.innerHeight) return false;
      if (rect.right < 0 || rect.left > window.innerWidth) return false;

      return true;
    });
  }

  // Extract image
  var imgEl =
    getVisible("div.storybook-image img") ||
    getVisible("storybook-page img") ||
    getVisible('img[src*="googleusercontent"]');

  var imageUrl =
    imgEl && imgEl.src && imgEl.src.startsWith("http") ? imgEl.src : null;

  // Extract text
  var textEl =
    getVisible("div.story-text-container") ||
    getVisible('[class*="story-text"]') ||
    getVisible("p");

  var text = textEl ? textEl.textContent.trim() : "";

  return { text: text, imageUrl: imageUrl };
})();
```

**This code extracts the same 465 characters on every page, even after clicking "Next".**

## Your Task

1. **Debug the visibility detection**: Figure out why `getVisible()` is finding the same elements repeatedly. Inspect the actual Gemini page DOM and refine the logic.

2. **Test on the live page**: Go to this Gemini storybook URL and test your script:

   - URL: `https://gemini.google.com/share/65d397b7ba12`
   - Open browser DevTools console
   - Paste the script and run it
   - Click "Next" button manually
   - Run the script again - text should be DIFFERENT

3. **Expected behavior**:

   - Page 1: Extract unique text + image
   - Click Next
   - Page 2: Extract DIFFERENT text + image
   - Repeat for all 10 pages

4. **Return the working script**: Once you have a script that correctly extracts different content for each page, give me the final JavaScript code.

## Testing Instructions

```javascript
// 1. Paste this in DevTools console on the Gemini page
var result = (function () {
  /* YOUR SCRIPT HERE */
})();
console.log(
  "Text length:",
  result.text.length,
  "Image:",
  result.imageUrl ? "YES" : "NO"
);
console.log("Text preview:", result.text.substring(0, 50));

// 2. Manually click "Next" button

// 3. Run the script again - text length should be DIFFERENT
```

## Constraints

- Must work in browser console (no external libraries)
- Must return `{ text: string, imageUrl: string|null }`
- Must only extract the CURRENTLY VISIBLE page content
- Should work reliably across all 10 pages of the story
