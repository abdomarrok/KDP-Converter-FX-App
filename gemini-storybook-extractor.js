/**
 * Gemini Storybook Page Extractor - FULLY AUTOMATED
 * 
 * This script extracts ALL pages from a Gemini AI storybook automatically.
 * 
 * HOW TO USE:
 * 1. Open a Gemini storybook page (e.g., https://gemini.google.com/share/xxxxx)
 * 2. Open DevTools console (F12 -> Console)
 * 3. Copy and paste this ENTIRE script
 * 4. Run: extractAllPages().then(r => console.log(r))
 * 
 * The script will:
 * - Jump to the beginning of the book
 * - Extract text and image URL from each page
 * - Click "Next" until it reaches the end
 * - Return an array of all pages with their content
 */

// ============================================================
// CORE EXTRACTION FUNCTION
// ============================================================
function extractGeminiStorybookPage() {
    const visiblePages = Array.from(document.querySelectorAll('storybook-page')).filter(el => {
        const style = window.getComputedStyle(el);
        const rect = el.getBoundingClientRect();
        return style.visibility === 'visible' &&
            rect.width > 0 &&
            rect.right > 0 &&
            rect.left < window.innerWidth;
    });

    // KEY: Use LAST visible element (current page is rendered last in DOM)
    const activePage = visiblePages.length > 0 ? visiblePages[visiblePages.length - 1] : null;

    if (!activePage) {
        return { text: "", imageUrl: null, error: "No active page found" };
    }

    const textEl = activePage.querySelector('div.story-text-container');
    const text = textEl ? textEl.textContent.trim() : "";

    const imgEl = activePage.querySelector('img[src*="googleusercontent"]')
        || activePage.querySelector('.storybook-image img')
        || activePage.querySelector('img');
    const imageUrl = imgEl && imgEl.src && imgEl.src.startsWith("http") ? imgEl.src : null;

    const pageLabel = document.querySelector('.jump-to-page-menu-button');
    const pageInfo = pageLabel ? pageLabel.textContent.trim() : null;

    return {
        pageInfo: pageInfo,
        text: text,
        imageUrl: imageUrl,
        textLength: text.length
    };
}

// ============================================================
// FULLY AUTOMATED EXTRACTION - Run this!
// ============================================================
async function extractAllPages(options = {}) {
    const {
        delayMs = 1500,        // Delay between pages (ms)
        jumpToStart = true,   // Whether to jump to beginning first
        logProgress = true,   // Log progress to console
        downloadImages = false // Download images to files (requires browser support)
    } = options;

    const results = [];
    const sleep = (ms) => new Promise(r => setTimeout(r, ms));
    const nextBtn = () => document.querySelector('button[aria-label="Next page"]');
    const prevBtn = () => document.querySelector('button[aria-label="Previous page"]');
    const pageMenu = () => document.querySelector('.jump-to-page-menu-button');

    // Helper to log with timestamp
    const log = (msg) => {
        if (logProgress) {
            const time = new Date().toLocaleTimeString();
            console.log(`[${time}] ${msg}`);
        }
    };

    log('🚀 Starting Gemini Storybook Extraction...');

    // Jump to beginning if requested
    if (jumpToStart) {
        log('📖 Jumping to beginning of book...');

        // Click the page menu to open it
        if (pageMenu()) {
            pageMenu().click();
            await sleep(500);

            // Find and click "Jump to beginning" or "Cover"
            const menuItems = Array.from(document.querySelectorAll('.mat-mdc-menu-item, [role="menuitem"]'));
            const jumpItem = menuItems.find(el =>
                el.textContent.includes('Jump to beginning') ||
                el.textContent.includes('Cover')
            );

            if (jumpItem) {
                jumpItem.click();
                await sleep(delayMs);
                log('✅ Jumped to beginning');
            } else {
                // Fallback: click Previous until disabled
                log('⏪ Menu not found, navigating backwards...');
                while (prevBtn() && !prevBtn().disabled) {
                    prevBtn().click();
                    await sleep(300);
                }
                await sleep(delayMs);
            }
        }
    }

    // Extract all pages
    let pageNum = 0;
    let lastText = null;

    while (true) {
        await sleep(delayMs);

        const data = extractGeminiStorybookPage();
        pageNum++;

        // Store the result
        results.push({
            pageNumber: pageNum,
            ...data
        });

        log(`📄 Page ${pageNum} (${data.pageInfo || 'unknown'}): ${data.textLength} chars, image: ${data.imageUrl ? '✅' : '❌'}`);

        // Check if Next button exists and is not disabled
        const next = nextBtn();
        if (!next || next.disabled) {
            log('🏁 Reached end of book!');
            break;
        }

        // Click next
        next.click();
    }

    // Generate summary
    const totalChars = results.reduce((sum, r) => sum + r.textLength, 0);
    const pagesWithImages = results.filter(r => r.imageUrl).length;

    log('');
    log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    log(`✨ EXTRACTION COMPLETE!`);
    log(`   📚 Total pages: ${results.length}`);
    log(`   📝 Total characters: ${totalChars}`);
    log(`   🖼️  Pages with images: ${pagesWithImages}/${results.length}`);
    log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

    return results;
}

// ============================================================
// EXPORT UTILITIES
// ============================================================

// Export to JSON and trigger download
function downloadAsJSON(results, filename = 'storybook-export.json') {
    const json = JSON.stringify(results, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
    console.log(`✅ Downloaded: ${filename}`);
}

// Export to Markdown and trigger download
function downloadAsMarkdown(results, filename = 'storybook-export.md') {
    let md = `# Gemini Storybook Export\n\n`;
    md += `*Extracted on ${new Date().toLocaleString()}*\n\n`;
    md += `---\n\n`;

    results.forEach(page => {
        md += `## Page ${page.pageNumber}${page.pageInfo ? ` (${page.pageInfo})` : ''}\n\n`;
        if (page.imageUrl) {
            md += `![Page ${page.pageNumber} Image](${page.imageUrl})\n\n`;
        }
        md += `${page.text}\n\n`;
        md += `---\n\n`;
    });

    const blob = new Blob([md], { type: 'text/markdown' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
    console.log(`✅ Downloaded: ${filename}`);
}

// ============================================================
// QUICK START - Copy everything above, then run:
// ============================================================
/*

// Basic extraction:
extractAllPages().then(results => {
    console.log(results);
});

// With options:
extractAllPages({
    delayMs: 2000,      // Slower for unstable connections
    jumpToStart: true,  // Start from beginning
    logProgress: true   // Show progress in console
}).then(results => {
    // Download as JSON
    downloadAsJSON(results, 'my-storybook.json');
    
    // Or download as Markdown
    downloadAsMarkdown(results, 'my-storybook.md');
});

*/

// Make functions globally available in browser
if (typeof window !== 'undefined') {
    window.extractGeminiStorybookPage = extractGeminiStorybookPage;
    window.extractAllPages = extractAllPages;
    window.downloadAsJSON = downloadAsJSON;
    window.downloadAsMarkdown = downloadAsMarkdown;
    console.log('✅ Gemini Storybook Extractor loaded!');
    console.log('   Run: extractAllPages().then(console.log)');
}
