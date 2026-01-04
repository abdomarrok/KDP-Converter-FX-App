(function () {
    // Redirect console to Java
    var oldLog = console.log;
    var oldErr = console.error;
    console.log = function (msg) {
        if (oldLog) oldLog(msg);
        if (window.javaApp && window.javaApp.log) window.javaApp.log("INFO: " + msg);
    };
    console.error = function (msg) {
        if (oldErr) oldErr(msg);
        if (window.javaApp && window.javaApp.log) window.javaApp.log("ERROR: " + msg);
    };

    console.log("=== Gemini Storybook Extractor v3.0 (Automated) ===");

    // ========== CORE EXTRACTION FUNCTION ==========
    function extractGeminiStorybookPage() {
        var visiblePages = Array.from(document.querySelectorAll('storybook-page')).filter(function (el) {
            var style = window.getComputedStyle(el);
            var rect = el.getBoundingClientRect();
            return style.visibility === 'visible' &&
                rect.width > 0 &&
                rect.right > 0 &&
                rect.left < window.innerWidth;
        });

        // KEY: Use LAST visible element (current page is rendered last in DOM)
        var activePage = visiblePages.length > 0 ? visiblePages[visiblePages.length - 1] : null;

        if (!activePage) {
            return { text: "", imageUrl: null, error: "No active page found" };
        }

        var textEl = activePage.querySelector('div.story-text-container');
        var text = textEl ? textEl.textContent.trim() : "";

        var imgEl = activePage.querySelector('img[src*="googleusercontent"]') ||
            activePage.querySelector('.storybook-image img') ||
            activePage.querySelector('img');
        var imageUrl = imgEl && imgEl.src && imgEl.src.startsWith("http") ? imgEl.src : null;

        var width = 0;
        var height = 0;
        if (imageUrl && imgEl) {
            width = imgEl.naturalWidth || imgEl.width || 0;
            height = imgEl.naturalHeight || imgEl.height || 0;
        }

        return {
            text: text,
            imageUrl: imageUrl,
            imageWidth: width,
            imageHeight: height,
            textLength: text.length
        };
    }

    // ========== AUTOMATED EXTRACTION (NO ASYNC/AWAIT) ==========
    function extractAllPages() {
        var delayMs = 2000;
        var results = [];
        var sleep = function (ms) { return new Promise(function (r) { setTimeout(r, ms); }); };
        var nextBtn = function () { return document.querySelector('button[aria-label="Next page"]'); };
        var prevBtn = function () { return document.querySelector('button[aria-label="Previous page"]'); };

        console.log("Starting automated extraction...");

        // Jump to beginning using promises
        console.log("Rewinding to start...");

        function rewindToStart() {
            var rewindCount = 0;
            function doRewind() {
                if (rewindCount >= 50 || !prevBtn() || prevBtn().disabled) {
                    return sleep(delayMs).then(function () {
                        console.log("Rewind complete");
                    });
                }
                prevBtn().click();
                rewindCount++;
                return sleep(200).then(doRewind);
            }
            return doRewind();
        }

        // Extract pages recursively using promises
        function extractPage(pageNum, lastText, consecutiveDupes) {
            var maxPages = 50;

            if (pageNum >= maxPages) {
                console.log("Reached max pages");
                return Promise.resolve(results);
            }

            return sleep(delayMs).then(function () {
                var data = extractGeminiStorybookPage();

                console.log("Page " + (pageNum + 1) + ": " + data.textLength + " chars, image: " + (data.imageUrl ? "YES" : "NO"));

                // Check for duplicates
                if (lastText && data.text === lastText) {
                    consecutiveDupes++;
                    console.log("Duplicate detected (" + consecutiveDupes + ")");
                    if (consecutiveDupes >= 3) {
                        console.log("Stuck on same content, stopping");
                        return Promise.resolve(results);
                    }
                } else {
                    consecutiveDupes = 0;
                    results.push({
                        text: data.text,
                        imageUrl: data.imageUrl
                    });
                    lastText = data.text;
                }

                // Check next button
                var next = nextBtn();
                if (!next || next.disabled) {
                    console.log("Reached end of book");
                    return Promise.resolve(results);
                }

                // Click next and continue
                next.click();
                return extractPage(pageNum + 1, lastText, consecutiveDupes);
            });
        }

        // Start the extraction chain
        return rewindToStart()
            .then(function () {
                return extractPage(0, null, 0);
            })
            .then(function (finalResults) {
                console.log("Extraction complete: " + finalResults.length + " scenes");

                var story = {
                    title: "Untitled Story",
                    author: "Unknown",
                    scenes: finalResults
                };

                if (window.javaApp && window.javaApp.processStory) {
                    console.log("Sending story to Java...");
                    window.javaApp.processStory(JSON.stringify(story));
                } else {
                    console.error("Java bridge not available!");
                }

                return story;
            });
    }

    // Start extraction
    extractAllPages().catch(function (err) {
        console.error("Extraction failed: " + err);
        if (window.javaApp && window.javaApp.processStory) {
            window.javaApp.processStory(JSON.stringify({
                title: "Error",
                author: "System",
                scenes: []
            }));
        }
    });

})();
