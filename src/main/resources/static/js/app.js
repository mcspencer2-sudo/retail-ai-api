    console.log("UNIVERSAL STYLIST v16.0 LUXURY UI LOADED");

const API = {
    auth: "/api/v1/saas/auth",
    stylist: "/api/v1/macy-stylist",
    merchant: "/api/v1/merchant/inventory"
};

const RETAILER_CONFIG = window.UniversalStylistRetailers;

if (!RETAILER_CONFIG) {
   throw new Error("retailers.js did not load. Make sure <script src='/retailers.js'></script> appears before the main app script.");
}

const STORE_OPTIONS = RETAILER_CONFIG.getStoreOptions();

const DEMO_SCAN_MODE = false;

const DEMO_RETAILER_KEYS = new Set([
    "MCS003",
    "MCS004"
]);

const DEMO_RFID_SUFFIX_BY_CATEGORY = {
    TOP: "TOP-001",
    BOTTOM: "BOT-001",
    SHOES: "SHOE-001",
    OUTERWEAR: "OUT-001"
};
    let currentRfid = "";
    let currentLoadedItem = null;
    let lastScannedItem = null;
    let activityRefreshStarted = false;
    let savedRfids = new Set();
    let merchantInventoryPage = 0;
    let importJobDetailsRefreshTimer = null;

    window.currentLookVariation = 0;
    window.currentLookState = {
        topRfid: "",
        bottomRfid: "",
        shoesRfid: "",
        outerwearRfid: ""
    };
    window.generatedLooks = [];
    window.currentLookIndex = -1;
    window.lastSwapCategory = "";
    window.activeSavedLookId = "";
    window.savedCurrentLookSignature = "";
    window.lastSavedLooks = [];

    function escapeHtml(value) {
        return String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function getHeaderOffset() {
        const stickyHeader =
            document.querySelector(".navbar.fixed-top") ||
            document.querySelector(".sticky-top") ||
            document.querySelector("header");

        const headerHeight = stickyHeader
            ? stickyHeader.getBoundingClientRect().height
            : 0;

        return Math.max(72, Math.round(headerHeight + 24));
    }

    function jumpToElement(elementId, offset = getHeaderOffset()) {
        const target = document.getElementById(elementId);

        if (!target) {
            console.warn(`Scroll target not found: ${elementId}`);
            return;
        }

        const runJump = () => {
            const rect = target.getBoundingClientRect();
            const top = rect.top + window.scrollY - offset;

            window.scrollTo({
                top: Math.max(0, top),
                behavior: "smooth"
            });
        };

        requestAnimationFrame(() => {
            runJump();

            window.setTimeout(runJump, 120);
            window.setTimeout(runJump, 280);
        });
    }

    function runAfterOffcanvasClosed(offcanvasId, callback) {
        const panel = document.getElementById(offcanvasId);

        if (!panel) {
            callback();
            return;
        }

        const instance =
            bootstrap.Offcanvas.getInstance(panel) ||
            new bootstrap.Offcanvas(panel);

        const isOpen = panel.classList.contains("show");

        if (!isOpen) {
            callback();
            return;
        }

        panel.addEventListener(
            "hidden.bs.offcanvas",
            () => {
                requestAnimationFrame(() => {
                    window.setTimeout(callback, 80);
                });
            },
            { once: true }
        );

        instance.hide();
    }

    function scrollToScannedItemResult() {
        jumpToElement("scanResultSection", getHeaderOffset());
    }

    function scrollToFullOutfitResult() {
        jumpToElement("fullOutfitContainer", getHeaderOffset());
    }

    function scrollToSuggestionsResult() {
        jumpToElement("suggestionsRow", getHeaderOffset());
    }

    function scrollToScanConsole() {
        jumpToElement("scanConsole", getHeaderOffset());
    }

function populateRetailerSelect(selectId, selectedValue = "MACY001", includeAllOption = false, allValue = "") {
    const select = document.getElementById(selectId);
    if (!select) return;

     const allOption = includeAllOption
         ? `<option value="${escapeHtml(allValue)}">All Retailers</option>`
         : "";

     const options = RETAILER_CONFIG.getRetailerEntries()
         .map(retailer => {
              const selected = retailer.key === selectedValue ? " selected" : "";

              return `
                 <option value="${escapeHtml(retailer.key)}"${selected}>
                       ${escapeHtml(retailer.name)}
                 </option>
            `;
        })
         .join("");

      select.innerHTML = `${allOption}${options}`;

     if (includeAllOption && selectedValue === allValue) {
       select.value = allValue;
        return;
        }

      const exists = RETAILER_CONFIG.getRetailerEntries().some(retailer => retailer.key === selectedValue);
      select.value = exists ? selectedValue : "MACY001";
       }

function populateActivityRetailerFilter() {
     const select = document.getElementById("activityRetailerFilter");
        if (!select) return;

     const options = RETAILER_CONFIG.getRetailerEntries()
          .map(retailer => `
        <option value="${escapeHtml(retailer.key)}">
           ${escapeHtml(retailer.name)}
        </option>
    `)
     .join("");

       select.innerHTML = `
         <option value="ALL">Current Store Retailers</option>
       ${options}
      `;
   }

function getRecentScansStorageKey() {
    const context = getJwtContext();
    const userPart = context.userId || context.email || getStoredLoginEmail() || "anonymous";
    const storePart = context.storeCode || "global";

    return `recentScans:${userPart}:${storePart}`;
}

function getRecentScans() {
    try {
        const raw = sessionStorage.getItem(getRecentScansStorageKey()) || "[]";
        const parsed = JSON.parse(raw);

        return Array.isArray(parsed) ? parsed : [];
    } catch (error) {
        console.error("Failed to read recent scans:", error);
        return [];
    }
}

function setRecentScans(scans) {
    sessionStorage.setItem(
        getRecentScansStorageKey(),
        JSON.stringify(Array.isArray(scans) ? scans.slice(0, 6) : [])
    );
}

function addRecentScan(item, vibe = "Casual") {
    if (!item || typeof item !== "object") {
        return;
    }

    const rfid = getItemField(item, "rfid", "itemRfid", "productRfid", "id");

    if (!rfid) {
        return;
    }

    const scan = {
        rfid,
        name: getItemField(item, "name", "itemName") || "Scanned Item",
        retailer: getItemField(item, "retailer", "retailerName") || "Retailer",
        category: getItemField(item, "category") || "Item",
        color: getItemField(item, "color") || "",
        vibe,
        scannedAt: new Date().toISOString()
    };

    const existing = getRecentScans()
        .filter(entry => entry && entry.rfid !== rfid);

    setRecentScans([scan, ...existing]);
    renderRecentScanChips();
}

function clearRecentScans() {
    setRecentScans([]);
    renderRecentScanChips();
    showToast("Recent scans cleared.", "info");
}

function renderRecentScanChips() {
    const container = document.getElementById("recentScanChips");

    if (!container) {
        return;
    }

    const scans = dedupeScanHistoryForDisplay(getRecentScans());

    if (!scans.length) {
        container.innerHTML = `<span class="small text-muted">No recent scans yet.</span>`;
        return;
    }

    container.innerHTML = `
        ${scans.map(scan => `
            <button
                type="button"
                class="recent-scan-chip"
                data-rfid="${escapeHtml(scan.rfid)}"
                data-vibe="${escapeHtml(scan.vibe || "Casual")}"
                title="${escapeHtml(scan.retailer || "")} • ${escapeHtml(scan.category || "")}"
            >
                ${escapeHtml(scan.name || scan.rfid)}
            </button>
        `).join("")}

        <button
            type="button"
            class="recent-scan-clear"
            id="clearRecentScansBtn"
        >
            Clear
        </button>
    `;

    container.querySelectorAll(".recent-scan-chip").forEach(button => {
        button.addEventListener("click", () => {
            const rfid = button.dataset.rfid || "";
            const vibe = button.dataset.vibe || "Casual";

            const rfidInput = document.getElementById("rfidInput");
            const vibeSelect = document.getElementById("vibeSelect");

            if (rfidInput) {
                rfidInput.value = rfid;
            }

            if (vibeSelect) {
                vibeSelect.value = vibe;
            }

            handleScan();
        });
    });

    document.getElementById("clearRecentScansBtn")?.addEventListener("click", clearRecentScans);
}

const CUSTOMER_SCAN_HISTORY_API = "/api/v1/customer/scan-history";

function formatScanHistoryDate(value) {
    if (!value) return "Recently";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return String(value);
    }

    return date.toLocaleString();
}

function normalizeScanHistoryEntry(entry) {
    if (!entry || typeof entry !== "object") {
        return null;
    }

    const rfid = getItemField(entry, "rfid", "itemRfid", "productRfid", "id");

    if (!rfid) {
        return null;
    }

    return {
        id: entry.id || "",
        rfid,
        itemName: getItemField(entry, "itemName", "name") || "Scanned Item",
        brand: getItemField(entry, "brand") || "",
        category: getItemField(entry, "category") || "Item",
        color: getItemField(entry, "color") || "",
        price: safeNumber(getItemField(entry, "price")),
        imageUrl: getItemField(entry, "imageUrl", "image_url", "image") || "",
        retailerName: getItemField(entry, "retailerName", "retailer") || "Retailer",
        retailerKey: getItemField(entry, "retailerKey") || "",
        storeCode: getItemField(entry, "storeCode") || "",
        storeName: getItemField(entry, "storeName") || "",
        vibe: getItemField(entry, "vibe") || "Casual",
        matchScore: safeNumber(getItemField(entry, "matchScore")) || 0,
        createdAt: getItemField(entry, "createdAt", "scannedAt") || ""
    };
}

async function fetchScanHistory() {
    requireToken();

    const response = await fetch(CUSTOMER_SCAN_HISTORY_API, {
        method: "GET",
        headers: getAuthHeaders({
            Accept: "application/json"
        })
    });

    await assertAuthorizedResponse(response, "Unable to load scan history.");

    const data = await response.json().catch(() => []);

    return Array.isArray(data)
        ? data.map(normalizeScanHistoryEntry).filter(Boolean)
        : [];
}

async function clearBackendScanHistory() {
    const confirmed = window.confirm("Clear your scan history for this logged-in store?");

    if (!confirmed) {
        return;
    }

    const button = document.getElementById("clearScanHistoryDrawerBtn");
    const originalText = button?.textContent || "Clear History";

    try {
        requireToken();

        if (button) {
            button.disabled = true;
            button.textContent = "Clearing...";
        }

        const response = await fetch(CUSTOMER_SCAN_HISTORY_API, {
            method: "DELETE",
            headers: getAuthHeaders()
        });

        await assertAuthorizedResponse(response, "Unable to clear scan history.");

        showToast("Scan history cleared.", "info");

        clearRecentScans();

        await renderScanHistoryDrawer();
        await renderRecentScansFromBackend();
    } catch (error) {
        console.error("Clear Scan History Error:", error);
        showToast(error.message || "Unable to clear scan history.", "error");
    } finally {
        if (button) {
            button.disabled = false;
            button.textContent = originalText;
        }
    }
}

async function renderRecentScansFromBackend() {
    const container = document.getElementById("recentScanChips");

    if (!container) {
        return;
    }

    if (!getToken()) {
        container.innerHTML = `<span class="small text-muted">No recent scans yet.</span>`;
        return;
    }

    try {
        const history = await fetchScanHistory();
        const scans = dedupeScanHistoryForDisplay(history).slice(0, 4);

        if (!scans.length) {
            container.innerHTML = `<span class="small text-muted">No recent scans yet.</span>`;
            return;
        }

        container.innerHTML = `
            ${scans.map(scan => {
                const rfid = getItemField(scan, "rfid", "itemRfid", "productRfid", "id");
                const name = getItemField(scan, "itemName", "name") || rfid;
                const vibe = getItemField(scan, "vibe") || "Casual";

                return `
                    <button
                        type="button"
                        class="recent-scan-chip"
                        data-rfid="${escapeHtml(rfid)}"
                        data-vibe="${escapeHtml(vibe)}"
                        title="${escapeHtml(rfid)}"
                    >
                        ${escapeHtml(name)}
                    </button>
                `;
            }).join("")}

            <button
                type="button"
                class="recent-scan-clear"
                id="viewAllScanHistoryBtn"
                data-bs-toggle="offcanvas"
                data-bs-target="#scanHistorySidebar"
            >
                View All
            </button>
        `;

        container.querySelectorAll(".recent-scan-chip").forEach(button => {
            button.addEventListener("click", () => {
                reopenScanFromHistory(
                    button.dataset.rfid || "",
                    button.dataset.vibe || "Casual"
                );
            });
        });

        document.getElementById("viewAllScanHistoryBtn")?.addEventListener("click", renderScanHistoryDrawer);
    } catch (error) {
        console.error("Recent Backend Scans Error:", error);
        container.innerHTML = `<span class="small text-muted">No recent scans yet.</span>`;
    }
}

function dedupeScanHistoryForDisplay(scans) {
    const seen = new Set();
    const safeScans = Array.isArray(scans) ? scans : [];

    return safeScans.filter(scan => {
        const rfid = getItemField(scan, "rfid", "itemRfid", "productRfid", "id");

        if (!rfid) {
            return false;
        }

        const normalizedRfid = String(rfid).trim().toUpperCase();

        if (seen.has(normalizedRfid)) {
            return false;
        }

        seen.add(normalizedRfid);
        return true;
    });
}

async function deleteBackendScanHistoryItem(scanHistoryId) {
    const safeId = String(scanHistoryId || "").trim();

    if (!safeId) {
        showToast("Scan history id is missing.", "error");
        return;
    }

    const confirmed = window.confirm("Remove this scan from your history?");

    if (!confirmed) {
        return;
    }

    try {
        requireToken();

        const response = await fetch(`/api/v1/customer/scan-history/${encodeURIComponent(safeId)}`, {
            method: "DELETE",
            headers: getAuthHeaders()
        });

        await assertAuthorizedResponse(response, "Unable to remove scan history item.");

        const message = await response.text().catch(() => "");

        showToast(message || "Scan removed from history.", "info");

        await Promise.allSettled([
            renderScanHistoryDrawer(),
            renderRecentScansFromBackend()
        ]);
    } catch (error) {
        console.error("Delete Scan History Item Error:", error);
        showToast(error.message || "Unable to remove scan history item.", "error");
    }
}

async function renderScanHistoryDrawer() {
    const container = document.getElementById("scanHistoryContent");

    if (!container) {
        return;
    }

    if (!getToken()) {
        container.innerHTML = `
            <div class="bag-empty-shell">
                <div class="bag-empty-icon">🔐</div>
                <div class="bag-empty-title">Login required</div>
                <p class="bag-empty-text">Please log in to view your scan history.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = `<div class="loading-state">Loading scan history...</div>`;

    try {
        const history = dedupeScanHistoryForDisplay(await fetchScanHistory());

        if (!history.length) {
            container.innerHTML = `
                <div class="bag-empty-shell">
                    <div class="bag-empty-icon">🕘</div>
                    <div class="bag-empty-title">No scan history yet</div>
                    <p class="bag-empty-text">Scanned items from this store will appear here.</p>
                </div>
            `;
            return;
        }

        container.innerHTML = `
            <div class="saved-look-toolbar">
                <button type="button" class="saved-look-clear-btn" id="clearScanHistoryDrawerBtn">
                    Clear History
                </button>
            </div>

            <div class="scan-history-list">
                ${history.map(scan => {
                    const imageUrl = safeImageUrl(
                        scan.imageUrl,
                        "https://placehold.co/96x96?text=Scan"
                    );

                    return `
                        <div class="scan-history-card">
                            <img
                                src="${imageUrl}"
                                alt="${escapeHtml(scan.itemName)}"
                                class="scan-history-img"
                                onerror="this.src='https://placehold.co/96x96?text=Scan';"
                            />

                            <div class="scan-history-main">
                                <div class="scan-history-topline">
                                    <div>
                                        <div class="scan-history-name">${escapeHtml(scan.itemName)}</div>
                                        <div class="scan-history-meta">
                                            ${escapeHtml(scan.brand || scan.retailerName)} •
                                            ${escapeHtml(scan.category)} •
                                            ${escapeHtml(scan.color || "Neutral")}
                                        </div>
                                    </div>

                                  <div class="scan-history-score">
                                      ${Number(scan.matchScore) > 0 ? `${Number(scan.matchScore)}%` : "New"}
                                  </div>
                                </div>

                                <div class="scan-history-footer">
                                    <span class="scan-history-date">
                                        ${escapeHtml(formatScanHistoryDate(scan.createdAt))}
                                    </span>

                                    <div class="d-flex gap-2 flex-wrap justify-content-end">
                                        <button
                                            type="button"
                                            class="merchant-inline-btn danger delete-scan-history-item-btn"
                                            data-scan-history-id="${escapeHtml(scan.id)}"
                                        >
                                            Remove
                                        </button>

                                        <button
                                            type="button"
                                            class="merchant-inline-btn primary reopen-scan-history-btn"
                                            data-rfid="${escapeHtml(scan.rfid)}"
                                            data-vibe="${escapeHtml(scan.vibe || "Casual")}"
                                        >
                                            Reopen Scan
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    `;
                }).join("")}
            </div>
        `;

                document.getElementById("clearScanHistoryDrawerBtn")?.addEventListener("click", clearBackendScanHistory);

                container.querySelectorAll(".reopen-scan-history-btn").forEach(button => {
                    button.addEventListener("click", () => {
                        reopenScanFromHistory(button.dataset.rfid || "", button.dataset.vibe || "Casual");
                    });
                });

                container.querySelectorAll(".delete-scan-history-item-btn").forEach(button => {
                    button.addEventListener("click", () => {
                        deleteBackendScanHistoryItem(button.dataset.scanHistoryId || "");
                    });
                });
            } catch (error) {
                console.error("Scan History Drawer Error:", error);

                container.innerHTML = `
                    <div class="bag-empty-shell">
                        <div class="bag-empty-icon">⚠️</div>
                        <div class="bag-empty-title">Unable to load scan history</div>
                        <p class="bag-empty-text">${escapeHtml(error.message || "Please try again.")}</p>
                    </div>
                `;
            }
        }

function reopenScanFromHistory(rfid, vibe = "Casual") {
    const safeRfid = String(rfid || "").trim();
    const safeVibe = String(vibe || "Casual").trim() || "Casual";

    if (!safeRfid) {
        showToast("Scan history item is missing RFID.", "error");
        return;
    }

    const rfidInput = document.getElementById("rfidInput");
    const vibeSelect = document.getElementById("vibeSelect");

    if (rfidInput) {
        rfidInput.value = safeRfid;
    }

    if (vibeSelect) {
        vibeSelect.value = safeVibe;
    }

    const sidebar = document.getElementById("scanHistorySidebar");
    const instance = sidebar ? bootstrap.Offcanvas.getInstance(sidebar) : null;

    if (instance) {
        sidebar.addEventListener(
            "hidden.bs.offcanvas",
            () => {
                scrollToScanConsole();

                handleScan();
            },
            { once: true }
        );

        instance.hide();
        return;
    }

   scrollToScanConsole();

    handleScan();
}

function getCurrentDemoRetailerKey() {
    const context = getJwtContext();

    const retailerKey = String(
        context.retailerKey ||
        window.loggedInRetailerKey ||
        ""
    ).trim().toUpperCase();

    if (retailerKey) {
        return retailerKey;
    }

    return "MCS003";
}

function getCurrentDemoStoreCode() {
    const context = getJwtContext();

    const storeCode = String(
        context.storeCode ||
        window.loggedInStoreCode ||
        ""
    ).trim().toUpperCase();

    return storeCode;
}

function getDemoRfidForCurrentStore(category = "TOP") {
    const retailerKey = getCurrentDemoRetailerKey();
    const normalizedCategory = String(category || "TOP").trim().toUpperCase();

    const suffix = DEMO_RFID_SUFFIX_BY_CATEGORY[normalizedCategory] || DEMO_RFID_SUFFIX_BY_CATEGORY.TOP;

    return `${retailerKey}-${suffix}`;
}

function buildQuickScanItemsForCurrentStore() {
    const retailerKey = getCurrentDemoRetailerKey();

    return [
        {
            label: "Demo Top",
            category: "TOP",
            retailerKey,
            rfid: getDemoRfidForCurrentStore("TOP")
        },
        {
            label: "Demo Bottom",
            category: "BOTTOM",
            retailerKey,
            rfid: getDemoRfidForCurrentStore("BOTTOM")
        },
        {
            label: "Demo Shoes",
            category: "SHOES",
            retailerKey,
            rfid: getDemoRfidForCurrentStore("SHOES")
        },
        {
            label: "Demo Outerwear",
            category: "OUTERWEAR",
            retailerKey,
            rfid: getDemoRfidForCurrentStore("OUTERWEAR")
        }
    ];
}

function renderQuickScanButtons() {
    const container = document.getElementById("quickScanButtons");
    if (!container) return;

    const quickScans = buildQuickScanItemsForCurrentStore();

    container.innerHTML = quickScans.map((item, index) => {
        const btnClass = index === 0 ? "btn-dark" : "btn-outline-dark";

        return `
            <button
                class="btn ${btnClass} btn-sm me-2 mb-2 quick-scan-btn"
                type="button"
                data-retailer-key="${escapeHtml(item.retailerKey)}"
                data-category="${escapeHtml(item.category)}"
                data-rfid="${escapeHtml(item.rfid)}"
                title="${escapeHtml(item.rfid)}"
            >
                ${escapeHtml(item.label)} ⚡
            </button>
        `;
    }).join("");

    container.querySelectorAll(".quick-scan-btn").forEach(button => {
        button.addEventListener("click", () => {
            const selectedRetailerKey = button.dataset.retailerKey || getCurrentDemoRetailerKey();
            const selectedRfid = button.dataset.rfid || "";

            quickScan(selectedRetailerKey, selectedRfid);
        });
    });
}

    function getStoreLabel(retailerKey, storeCode) {
      const stores = STORE_OPTIONS[retailerKey] || [];
      const store = stores.find(item => item.code === storeCode);

     return store?.label || storeCode || "";
  }
    function getJwtPayload() {
      return parseJwtPayload(getToken()) || {};
  }

    function getSelectedRetailerKey() {
       return (
           window.loggedInRetailerKey ||
           getJwtContext().retailerKey ||
           ""
    );
}

   function getSelectedStoreCode() {
      return (
          window.loggedInStoreCode ||
          getJwtContext().storeCode ||
          ""
    );
}
    function populateStoreOptions(retailerKey, preferredStoreCode = "") {
        const storeSelect = document.getElementById("storeCodeSelect");
        if (!storeSelect) return;

        const stores = STORE_OPTIONS[retailerKey] || [];
        const fallbackStoreCode = preferredStoreCode || stores[0]?.code || "";

        if (!stores.length) {
            storeSelect.innerHTML = `<option value="">No Stores</option>`;
            return;
        }

        storeSelect.innerHTML = stores.map(store => `
            <option value="${escapeHtml(store.code)}">${escapeHtml(store.label)}</option>
        `).join("");

        const exists = stores.some(store => store.code === fallbackStoreCode);
        storeSelect.value = exists ? fallbackStoreCode : stores[0].code;
    }

    function populateUploadStoreOptions(retailerKey, preferredStoreCode = "") {
        const storeSelect = document.getElementById("uploadStoreCodeSelect");
        if (!storeSelect) return;

        const stores = STORE_OPTIONS[retailerKey] || [];
        const fallbackStoreCode = preferredStoreCode || stores[0]?.code || "";

        if (!stores.length) {
            storeSelect.innerHTML = `<option value="">No Stores</option>`;
            return;
        }

        storeSelect.innerHTML = stores.map(store => `
            <option value="${escapeHtml(store.code)}">${escapeHtml(store.label)}</option>
        `).join("");

        const exists = stores.some(store => store.code === fallbackStoreCode);
        storeSelect.value = exists ? fallbackStoreCode : stores[0].code;
    }

    function populateInventoryStoreOptions(retailerKey, preferredStoreCode = "") {
        const storeSelect = document.getElementById("inventoryStoreCodeSelect");
        if (!storeSelect) return;

        const stores = STORE_OPTIONS[retailerKey] || [];
        const fallbackStoreCode = preferredStoreCode || stores[0]?.code || "";

        if (!stores.length) {
            storeSelect.innerHTML = `<option value="">No Stores</option>`;
            return;
        }

        storeSelect.innerHTML = stores.map(store => `
            <option value="${escapeHtml(store.code)}">${escapeHtml(store.label)}</option>
        `).join("");

        const exists = stores.some(store => store.code === fallbackStoreCode);
        storeSelect.value = exists ? fallbackStoreCode : stores[0].code;
    }

    function buildImagePlaceholder(text = "Inventory Item") {
        const safeText = String(text || "Inventory Item")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");

        const svg = `
            <svg xmlns="http://www.w3.org/2000/svg" width="500" height="620" viewBox="0 0 500 620">
                <rect width="500" height="620" fill="#f3f4f6"/>
                <rect x="80" y="120" width="340" height="380" rx="28" fill="#e5e7eb"/>
                <text x="250" y="295" text-anchor="middle" font-family="Arial, sans-serif" font-size="24" font-weight="700" fill="#6b7280">
                     ${safeText}
            </text>
            <text x="250" y="330" text-anchor="middle" font-family="Arial, sans-serif" font-size="15" fill="#9ca3af">
                Image unavailable
            </text>
        </svg>
    `;

    return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
  }

     function safeImageUrl(url, fallbackText = "Inventory Item") {
    const value = String(url || "").trim();

    const buildFallback = () => {
        const normalizedFallback = String(fallbackText || "Inventory Item").trim();

        if (
            normalizedFallback.startsWith("http://") ||
            normalizedFallback.startsWith("https://") ||
            normalizedFallback.startsWith("data:")
        ) {
            return normalizedFallback;
        }

        return buildImagePlaceholder(normalizedFallback);
    };

    if (!value) {
        return buildFallback();
    }

    if (value.startsWith("file://") || value.startsWith("/Users/") || value.startsWith("C:\\")) {
        return buildFallback();
    }

    try {
        const parsed = new URL(value, window.location.origin);

        if (
            parsed.protocol === "http:" ||
            parsed.protocol === "https:" ||
            parsed.protocol === "data:" ||
            parsed.protocol === "blob:"
        ) {
            return parsed.href;
        }
    } catch (error) {
        return buildFallback();
    }

    return buildFallback();
}

    function safeNumber(value) {
        const num = Number(value);
        return Number.isFinite(num) ? num : 0;
    }

    function formatPrice(value) {
        const number = Number(value);
        return Number.isFinite(number) ? `$${number.toFixed(2)}` : "$0.00";
    }

    function normalizeCategoryName(category) {
        const raw = String(category || "").trim().toLowerCase();

        if (["top", "tops", "shirt", "shirts", "tee", "t-shirt", "hoodie", "sweater", "knit", "blouse"].includes(raw)) return "tops";
        if (["bottom", "bottoms", "pants", "trousers", "jeans", "cargo", "shorts", "skirt"].includes(raw)) return "bottoms";
        if (["shoe", "shoes", "sneaker", "sneakers", "boot", "boots", "loafer", "loafers", "heels", "heel", "sandals", "sandal"].includes(raw)) return "shoes";
        if (["outerwear", "coat", "jacket", "blazer", "parka"].includes(raw)) return "outerwear";

        return raw;
    }

    function renderBudgetPreferenceNote(item) {
   const preferences = getLocalPreferences();

    const price = Number(getItemField(item, "price") || 0);
    const budgetMin = Number(preferences.budgetMin || 0);
    const budgetMax = Number(preferences.budgetMax || 0);

    if (!Number.isFinite(price) || price <= 0) {
        return "";
    }

    if (budgetMin > 0 && price < budgetMin) {
        return `
            <div class="small fw-bold text-muted mt-1">
                Below preferred budget
            </div>
        `;
    }

    if (budgetMax > 0 && price > budgetMax) {
        return `
            <div class="budget-warning-pill"> Above preferred budget
            </div>
        `;
    }

    if ((budgetMin > 0 || budgetMax > 0)
            && (budgetMin <= 0 || price >= budgetMin)
            && (budgetMax <= 0 || price <= budgetMax)) {
        return `
            <div class="budget-match-pill"> Within preferred budget
            </div>
        `;
    }

    return "";
}

    function getSwapHeadingCopy(swapCategory = "") {
        const normalized = normalizeCategoryName(swapCategory);

        switch (normalized) {
            case "tops":
                return {
                    kicker: "Focused alternatives for your current top",
                    heading: "Top Alternatives"
                };
            case "bottoms":
                return {
                    kicker: "Focused alternatives for your current bottom",
                    heading: "Bottom Alternatives"
                };
            case "shoes":
                return {
                    kicker: "Focused alternatives for your current shoes",
                    heading: "Shoe Alternatives"
                };
            case "outerwear":
                return {
                    kicker: "Focused alternatives for your outer layer",
                    heading: "Outerwear Alternatives"
                };
            default:
                return {
                    kicker: "Curated alternatives",
                    heading: "Recommended Next Pieces"
                };
        }
    }

    function updateSuggestionsHeading(swapCategory = "") {
        const kickerEl = document.getElementById("suggestionsKicker");
        const headingEl = document.getElementById("suggestionsHeading");
        const copy = getSwapHeadingCopy(swapCategory);

        if (kickerEl) kickerEl.textContent = copy.kicker;
        if (headingEl) headingEl.textContent = copy.heading;
    }

    function showMerchantUploadSection() {
        document.getElementById("merchantUploadSection")?.classList.remove("d-none");
    }

    function hideMerchantUploadSection() {
        document.getElementById("merchantUploadSection")?.classList.add("d-none");
    }

    function showMerchantInventorySection() {
        document.getElementById("merchantInventorySection")?.classList.remove("d-none");
    }

    function hideMerchantInventorySection() {
        document.getElementById("merchantInventorySection")?.classList.add("d-none");

        const summary = document.getElementById("merchantInventorySummary");
        if (summary) {
            summary.innerHTML = "";
       }
    }

    function setInventoryUploadStatus(message, type = "muted") {
        const status = document.getElementById("inventoryUploadStatus");
        if (!status) return;

        const classMap = {
            success: "text-success",
            danger: "text-danger",
            muted: "text-muted"
        };

        status.innerHTML = message
            ? `<div class="${classMap[type] || "text-muted"} fw-semibold">${escapeHtml(message)}</div>`
            : "";
    }

    function renderInventoryUploadResult(result) {
        const container = document.getElementById("inventoryUploadResult");
        if (!container) return;

        if (!result || typeof result !== "object") {
            container.innerHTML = "";
            return;
        }

        const successCount = Number(result.successCount || 0);
        const failureCount = Number(result.failureCount || 0);
        const errors = Array.isArray(result.errors) ? result.errors : [];

        container.innerHTML = `
            <div class="inventory-upload-result-card">
                <div class="inventory-upload-summary">
                    <div class="inventory-upload-stat">
                        <div class="inventory-upload-stat-label">Imported</div>
                        <div class="inventory-upload-stat-value">${successCount}</div>
                    </div>
                    <div class="inventory-upload-stat">
                        <div class="inventory-upload-stat-label">Failed</div>
                        <div class="inventory-upload-stat-value">${failureCount}</div>
                    </div>
                </div>

                ${
                    errors.length
                        ? `
                        <div class="inventory-upload-errors">
                            ${errors.map(error => `
                                <div class="inventory-upload-error">
                                    <div class="inventory-upload-error-row">Row ${Number(error.rowNumber || 0)}</div>
                                    <div class="inventory-upload-error-message">${escapeHtml(error.message || "Unknown import error")}</div>
                                </div>
                            `).join("")}
                        </div>
                        `
                        : `<div class="text-success fw-semibold">Inventory import completed with no row errors.</div>`
                }
            </div>
        `;
    }

    function setMerchantInventoryStatus(message, type = "muted") {
        const status = document.getElementById("merchantInventoryStatus");
        if (!status) return;

        const classMap = {
            success: "text-success",
            danger: "text-danger",
            muted: "text-muted"
        };

        status.innerHTML = message
            ? `<div class="${classMap[type] || "text-muted"} fw-semibold">${escapeHtml(message)}</div>`
            : "";
    }

    function showToast(message, type = "info") {
        const shell = document.getElementById("toastShell");
        if (!shell || !message) return;

        const toast = document.createElement("div");
        toast.className = `toast-card toast-${type}`;
        toast.textContent = message;
        shell.appendChild(toast);

        window.setTimeout(() => {
            toast.remove();
        }, 2800);
    }

   const AUTH_TOKEN_KEY = "retailai_token";

   function extractTokenFromLoginResponse(rawText) {
       const cleanText = String(rawText || "").trim();

       if (!cleanText) {
           return "";
       }

       const cleanToken = value => {
           return String(value || "")
               .trim()
               .replace(/^Bearer\s+/i, "")
               .replace(/^"|"$/g, "")
               .trim();
       };

       try {
           const parsed = JSON.parse(cleanText);

           if (typeof parsed === "string") {
               return cleanToken(parsed);
           }

           if (parsed && typeof parsed === "object") {
               return cleanToken(
                   parsed.token ||
                   parsed.jwt ||
                   parsed.accessToken ||
                   parsed.access_token ||
                   parsed.authToken ||
                   parsed.idToken ||
                   ""
               );
           }
       } catch {
           // Login returned raw JWT text, not JSON.
       }

       return cleanToken(cleanText);
   }

function getToken() {
    return (
        sessionStorage.getItem("token") ||
        localStorage.getItem("token") ||
        sessionStorage.getItem("jwt") ||
        localStorage.getItem("jwt") ||
        sessionStorage.getItem("authToken") ||
        localStorage.getItem("authToken") ||
        ""
    );
}

function setToken(token) {
    const cleanToken = String(token || "").trim();

    sessionStorage.removeItem("token");
    localStorage.removeItem("token");
    sessionStorage.removeItem("jwt");
    localStorage.removeItem("jwt");
    sessionStorage.removeItem("authToken");
    localStorage.removeItem("authToken");

    if (cleanToken) {
        sessionStorage.setItem("token", cleanToken);
    }
}

function clearToken() {
    sessionStorage.removeItem("token");
    localStorage.removeItem("token");
    sessionStorage.removeItem("jwt");
    localStorage.removeItem("jwt");
    sessionStorage.removeItem("authToken");
    localStorage.removeItem("authToken");
}

     function parseJwtPayload(token) {
    if (!token || !token.includes(".")) {
        return null;
    }

    try {
        const base64Url = token.split(".")[1];
        const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
        const jsonPayload = decodeURIComponent(
            atob(base64)
                .split("")
                .map(char => `%${`00${char.charCodeAt(0).toString(16)}`.slice(-2)}`)
                .join("")
        );

        return JSON.parse(jsonPayload);
    } catch (error) {
        console.error("Failed to parse JWT payload:", error);
        return null;
    }
}

function getCurrentUserRole() {
    const payload = parseJwtPayload(getToken());

    return (
        payload?.role ||
        payload?.authority ||
        payload?.authorities?.[0] ||
        ""
    );
}

   function isOwnerUser() {
       const role = String(getCurrentUserRole() || "").toUpperCase();
       return role === "OWNER" || role === "ROLE_OWNER";
   }

   function getJwtContext() {
       const payload = parseJwtPayload(getToken()) || {};
       const restored = window.loggedInAuthContext || {};

       return {
           userId: payload.userId || restored.userId || "",
           tenantId: payload.tenantId || restored.tenantId || "",
           storeId: payload.storeId || restored.storeId || "",
           email: payload.email || payload.sub || restored.email || "",
           fullName: payload.fullName || restored.fullName || "",
           role: payload.role || restored.role || "",
           businessName: payload.businessName || restored.businessName || "",
           tenantSlug: payload.tenantSlug || restored.tenantSlug || "",
           plan: payload.plan || restored.plan || "",
           retailerKey: payload.retailerKey || restored.retailerKey || window.loggedInRetailerKey || "",
           storeCode: payload.storeCode || restored.storeCode || window.loggedInStoreCode || "",
           storeName: payload.storeName || restored.storeName || "",
           location: payload.location || restored.location || ""
       };
   }

     function getCurrentRetailerKey() {
        return getJwtContext().retailerKey || "";
    }

     function getCurrentStoreCode() {
        return getJwtContext().storeCode || "";
     }

     function getCurrentStoreName() {
        const context = getJwtContext();
        return context.storeName || context.storeCode || "Current Store";
     }

     function updateSecureStoreLabels() {
        const context = getJwtContext();

        const storeName = context.storeName || "Current Store";
        const storeCode = context.storeCode || "JWT store context";
        const retailerKey = context.retailerKey || "Secure retailer";
        const businessName = context.businessName || "Your Store";

        const currentStoreName = document.getElementById("currentStoreName");
        const currentStoreCode = document.getElementById("currentStoreCode");
        const uploadSecureStoreLabel = document.getElementById("uploadSecureStoreLabel");

     if (currentStoreName) {
        currentStoreName.textContent = storeName;
    }

     if (currentStoreCode) {
        currentStoreCode.textContent = `${retailerKey} • ${storeCode}`;
    }

     if (uploadSecureStoreLabel) {
        uploadSecureStoreLabel.textContent = `${businessName} • ${storeName} • ${storeCode}`;
     }
 }

   async function restoreLoggedInStoreContext() {
    try {
        requireToken();

        const resp = await fetch(`${API.stylist}/debug/auth`, {
            headers: getAuthHeaders()
        });

        await assertAuthorizedResponse(resp, "Session expired. Please log in again.");

        const auth = await resp.json();

        const retailerKey = String(auth.jwtRetailerKey || "").trim();
        const storeCode = String(auth.jwtStoreCode || "").trim();

        if (!retailerKey || !storeCode) {
            throw new Error("Logged-in store context is missing.");
        }

        window.loggedInRetailerKey = retailerKey;
        window.loggedInStoreCode = storeCode;

        window.loggedInAuthContext = {
            userId: String(auth.jwtUserId || "").trim(),
            tenantId: String(auth.jwtTenantId || "").trim(),
            storeId: String(auth.jwtStoreId || "").trim(),
            email: String(auth.jwtEmail || "").trim(),
            role: String(auth.jwtRole || "").trim(),
            retailerKey,
            storeCode,
            storeName: getStoreLabel(retailerKey, storeCode) || storeCode,
            canManageInventory: Boolean(auth.canManageInventory)
        };

        updateSecureStoreLabels();
        renderQuickScanButtons();

        return {
            retailerKey,
            storeCode
        };
    } catch (error) {
        console.warn("Could not restore logged-in store context:", error);
        return null;
    }
}

    function getStoredLoginEmail() {
        return sessionStorage.getItem("loginEmail") || "";
    }

    function setStoredLoginEmail(email) {
        sessionStorage.setItem("loginEmail", email || "");
    }

    function clearStoredLoginEmail() {
        sessionStorage.removeItem("loginEmail");
    }

    function getAuthHeaders(extra = {}) {
        const token = getToken();
        return {
            ...extra,
            ...(token ? { Authorization: `Bearer ${token}` } : {})
        };
    }

    function requireToken() {
        const token = getToken();

        if (!token) {
            handleExpiredSession();
            throw new Error("Your session has expired. Please log in again.");
        }

        return token;
    }

   function handleExpiredSession() {
       const hadToken = !!getToken();

       clearToken();
       sessionStorage.removeItem("loginEmail");
       sessionStorage.removeItem("user");
       sessionStorage.removeItem("username");
       localStorage.removeItem("loginEmail");
       localStorage.removeItem("user");
       localStorage.removeItem("username");

       window.loggedInRetailerKey = "";
       window.loggedInStoreCode = "";
       window.loggedInAuthContext = null;
       window.currentCustomerPreferences = null;
       window.activeSavedLookId = "";

       updateAuthStatus();
       updateAuthUI();
       resetScanExperience();

       clearBagUi(
           "Login required",
           "Please log in to view your style bag."
       );

       const savedLooksContent = document.getElementById("savedLooksContent");
       if (savedLooksContent) {
           savedLooksContent.innerHTML = `
               <div class="saved-look-empty">
                   <div class="bag-empty-icon">✨</div>
                   <div class="bag-empty-title">Saved looks unavailable</div>
                   <p class="bag-empty-text">Please log in again to continue your session.</p>
               </div>
           `;
       }

       const orderHistoryContent = document.getElementById("orderHistoryContent");
       if (orderHistoryContent) {
           orderHistoryContent.innerHTML = `
               <div class="bag-empty-shell">
                   <div class="bag-empty-icon">🔐</div>
                   <div class="bag-empty-title">Order history unavailable</div>
                   <p class="bag-empty-text">Please log in again to view completed checkouts.</p>
               </div>
           `;
       }

       const preferencesContent = document.getElementById("preferencesContent");
       if (preferencesContent) {
           preferencesContent.innerHTML = `
               <div class="bag-empty-shell">
                   <div class="bag-empty-icon">🔐</div>
                   <div class="bag-empty-title">Preferences unavailable</div>
                   <p class="bag-empty-text">Please log in again to manage your styling preferences.</p>
               </div>
           `;
       }

       const scanHistoryContent = document.getElementById("scanHistoryContent");
       if (scanHistoryContent) {
           scanHistoryContent.innerHTML = `
               <div class="bag-empty-shell">
                   <div class="bag-empty-icon">🕘</div>
                   <div class="bag-empty-title">Scan history unavailable</div>
                   <p class="bag-empty-text">Please log in again to view your scan history.</p>
               </div>
           `;
       }

       if (hadToken) {
           showToast("Your session has expired. Please log in again.", "error");
       }
   }

 async function assertAuthorizedResponse(response, fallbackMessage = "Request failed.") {
    if (response.ok) {
        return;
    }

    if (response.status === 401 || response.status === 403) {
        handleExpiredSession();
        throw new Error("Your session expired. Please log in again.");
    }

    let message = fallbackMessage;

    try {
        const contentType = response.headers.get("Content-Type") || "";

        if (contentType.includes("application/json")) {
            const data = await response.json();

            if (typeof data === "string") {
                message = data;
            } else if (data && typeof data === "object") {
                message =
                    data.message ||
                    data.detail ||
                    data.title ||
                    data.error ||
                    fallbackMessage;
            }
        } else {
            const text = await response.text();
            message = text || fallbackMessage;
        }
    } catch (parseError) {
        message = fallbackMessage;
    }

    message = cleanApiErrorMessage(message);

    if (response.status === 422 && (!message || message === fallbackMessage)) {
        message = "This item is not currently available in inventory.";
    }

    throw new Error(message);
}

async function fetchJsonOrNull(url, options = {}) {
    const response = await fetch(url, options);
    const text = await response.text().catch(() => "");

    const readMessage = value => {
        if (!value) return "";

        try {
            const parsed = JSON.parse(value);

            if (typeof parsed === "string") {
                return parsed;
            }

            return parsed?.message || parsed?.error || parsed?.detail || parsed?.title || "";
        } catch {
            return String(value).replace(/^"|"$/g, "").trim();
        }
    };

    if (!response.ok) {
        return {
            ok: false,
            status: response.status,
            data: null,
            message:
                readMessage(text) ||
                cleanApiErrorMessage(text) ||
                `Request failed with status ${response.status}`
        };
    }

    let data = null;

    try {
        data = text ? JSON.parse(text) : null;
    } catch {
        data = null;
    }

    return {
        ok: true,
        status: response.status,
        data,
        message: ""
    };
}

function cleanApiErrorMessage(message) {
    const text = String(message || "").trim();

    if (!text) {
        return "Request failed.";
    }

    const quotedMatch = text.match(/"([^"]+)"/);
    if (quotedMatch && quotedMatch[1]) {
        return quotedMatch[1];
    }

    return text
        .replace(/^422\s+UNPROCESSABLE_ENTITY\s*/i, "")
        .replace(/^400\s+BAD_REQUEST\s*/i, "")
        .replace(/^401\s+UNAUTHORIZED\s*/i, "")
        .replace(/^403\s+FORBIDDEN\s*/i, "")
        .replace(/^404\s+NOT_FOUND\s*/i, "")
        .replace(/^500\s+INTERNAL_SERVER_ERROR\s*/i, "")
        .trim();
}

    function updateAuthStatus() {
        const status = document.getElementById("authStatus");
        if (!status) return;
        status.textContent = getToken() ? "Logged in" : "Not logged in";
    }

    function getDisplayNameFromEmail(email) {
        if (!email) return "";

        const namePart = String(email).split("@")[0] || "";
        const cleaned = namePart.replace(/[._-]+/g, " ").trim();

        if (!cleaned) return "";

        return cleaned
            .split(" ")
            .filter(Boolean)
            .map(part => part.charAt(0).toUpperCase() + part.slice(1))
            .join(" ");
    }

    function updateAuthUI() {
        const loggedIn = !!getToken();
        const authSection = document.getElementById("authSection");
        const welcomeSection = document.getElementById("welcomeSection");
        const welcomeTitle = document.getElementById("welcomeTitle");
        const bagBtn = document.getElementById("viewBagBtn");
        const savedLooksBtn = document.getElementById("viewSavedLooksBtn");
        const orderHistoryBtn = document.getElementById("viewOrderHistoryBtn");
        const preferencesBtn = document.getElementById("viewPreferencesBtn");
        const email = getStoredLoginEmail();
        const displayName = getDisplayNameFromEmail(email);

        if (authSection) authSection.classList.toggle("d-none", loggedIn);
        if (welcomeSection) welcomeSection.classList.toggle("d-none", !loggedIn);

        if (welcomeTitle) {
            welcomeTitle.textContent = loggedIn
                ? (displayName ? `Welcome back, ${displayName}` : "Welcome back")
                : "Welcome back";
        }

        if (bagBtn) bagBtn.disabled = !loggedIn;
        if (savedLooksBtn) savedLooksBtn.disabled = !loggedIn;
        if (orderHistoryBtn) orderHistoryBtn.disabled = !loggedIn;
        if (preferencesBtn) preferencesBtn.disabled = !loggedIn;

        if (loggedIn && isOwnerUser()) {
            showMerchantUploadSection();
            showMerchantInventorySection();
        } else {
            hideMerchantUploadSection();
            hideMerchantInventorySection();
        }
            updateSecureStoreLabels();
    }

    function setButtonBusy(button, busyText) {
        if (!button) return;
        button.disabled = true;
        button.setAttribute("aria-busy", "true");
        if (busyText) button.innerHTML = busyText;
    }

    function clearButtonBusy(button, normalText) {
        if (!button) return;
        button.disabled = false;
        button.removeAttribute("aria-busy");
        if (normalText) button.innerHTML = normalText;
    }

    function getSaveButton() {
        return document.getElementById("saveToBagBtn");
    }

    function setSaveButtonDefault(disabled = false) {
        const saveBtn = getSaveButton();
        if (!saveBtn) return;

        saveBtn.className = "btn-result-primary";
        saveBtn.textContent = "Save to Bag";
        saveBtn.disabled = disabled;
        saveBtn.removeAttribute("aria-busy");
    }

    function setSaveButtonSaved() {
        const saveBtn = getSaveButton();
        if (!saveBtn) return;

        saveBtn.className = "btn-result-primary saved-state";
        saveBtn.textContent = "Saved to Bag ✓";
        saveBtn.disabled = true;
        saveBtn.removeAttribute("aria-busy");
    }

    function getItemField(item, ...keys) {
        for (const key of keys) {
            if (item && item[key] !== undefined && item[key] !== null && item[key] !== "") {
                return item[key];
            }
        }
        return "";
    }

    function isCurrentItemSaved(item) {
        const rfid = getItemField(item, "rfid", "itemRfid", "productRfid", "id");
        return !!rfid && savedRfids.has(rfid);
    }

    function setLiveDotState(state) {
        document.querySelectorAll(".live-dot").forEach(dot => {
            dot.className = "live-dot";
            if (["ready", "scanning", "success", "error"].includes(state)) {
                dot.classList.add(state);
            }
        });
    }

    function setScanStatus(message, type = "muted") {
        const status = document.getElementById("scanStatus");
        if (!status) return;

        if (!message) {
            status.innerHTML = "";
            return;
        }

        const dotStateMap = {
            muted: "ready",
            success: "success",
            danger: "error"
        };

        const classMap = {
            success: "text-success",
            danger: "text-danger",
            muted: "text-muted"
        };

        status.innerHTML = `
            <div class="scan-status-live ${classMap[type] || "text-muted"}">
                <span class="live-dot ${dotStateMap[type] || "ready"}"></span>
                <span>${escapeHtml(message)}</span>
            </div>
        `;

        setLiveDotState(dotStateMap[type] || "ready");
    }

    function showLoadingState() {
        document.getElementById("loadingState")?.classList.remove("d-none");
        document.getElementById("scanConsole")?.classList.add("scanning-mode");
        document.getElementById("scanBtn")?.classList.add("scanning");
    }

    function hideLoadingState() {
        document.getElementById("loadingState")?.classList.add("d-none");
        document.getElementById("scanConsole")?.classList.remove("scanning-mode");
        document.getElementById("scanBtn")?.classList.remove("scanning");
    }

    function showScanResultSection() {
        document.getElementById("scanResultSection")?.classList.remove("d-none");
    }

    function hideScanResultSection() {
        document.getElementById("scanResultSection")?.classList.add("d-none");
    }

    function generateStylingAdvice(item, vibe) {
        const category = getItemField(item, "category") || "piece";
        const color = getItemField(item, "color") || "";
        const selected = String(vibe || "").toLowerCase();

        if (selected === "casual") {
            return `Pair this ${color ? `${color} ` : ""}${category} with clean sneakers and relaxed essentials for an effortless everyday look.`;
        }
        if (selected === "streetwear") {
            return "Layer this piece with cargos, statement sneakers, and bold outerwear for a sharper streetwear fit.";
        }
        if (selected === "formal") {
            return "Style this item with structured layers and refined accessories to create a polished formal direction.";
        }
        if (selected === "luxury") {
            return "Combine this with premium textures, elevated tailoring, and minimal accessories for a luxury-forward outfit.";
        }
        if (selected === "date night") {
            return "Balance this piece with sharper silhouettes and elevated accessories for a confident date-night look.";
        }

        return `This item is versatile and can anchor a strong ${vibe || "modern"} look.`;
    }

    function generateWhyItWorks(item, vibe) {
        const color = getItemField(item, "color") || "color";
        const category = getItemField(item, "category") || "silhouette";
        return `The ${color}, ${category}, and ${vibe || "overall"} styling direction make this piece easy to build around across multiple outfit combinations.`;
    }

    function resetLookHistory() {
        window.generatedLooks = [];
        window.currentLookIndex = -1;
    }
function buildLookEntry(lookPayload, fallbackVariation = 0) {
    if (!lookPayload || typeof lookPayload !== "object") return null;

    const fullOutfit = lookPayload.fullOutfit || null;
    const suggestions = Array.isArray(lookPayload.suggestions) ? lookPayload.suggestions : [];
    const variation = safeNumber(lookPayload.variation ?? fallbackVariation);

    if (!fullOutfit && suggestions.length === 0) return null;

    return {
        variation,
        fullOutfit,
        suggestions,

        stylingNote: lookPayload.stylingNote || fullOutfit?.stylingNote || "",
        occasionNote: lookPayload.occasionNote || fullOutfit?.occasionNote || "",
        seasonNote: lookPayload.seasonNote || fullOutfit?.seasonNote || "",
        colorNote: lookPayload.colorNote || fullOutfit?.colorNote || "",
        fitNote: lookPayload.fitNote || fullOutfit?.fitNote || "",
        materialNote: lookPayload.materialNote || fullOutfit?.materialNote || "",
        preferenceNote: lookPayload.preferenceNote || fullOutfit?.preferenceNote || ""
    };
}

 function setActiveLookByIndex(index) {
     const looks = Array.isArray(window.generatedLooks) ? window.generatedLooks : [];

     if (!looks.length) {
         window.currentLookIndex = -1;
         renderLookCarousel();
         renderAnchorPiece(lastScannedItem);
         renderFullOutfit(null, null);
         renderSuggestions([], null, "");
         updateSaveLookButtonState(true);
         return;
     }

     const safeIndex = Math.max(0, Math.min(index, looks.length - 1));
     window.currentLookIndex = safeIndex;

     const activeLook = looks[safeIndex];

     renderLookCarousel();
     renderAnchorPiece(lastScannedItem);
     renderFullOutfit(activeLook?.fullOutfit || null, activeLook);
     renderSuggestions(
         activeLook?.suggestions || [],
         activeLook?.fullOutfit || null,
         window.lastSwapCategory || ""
     );

     updateSaveLookButtonState();
 }

    function pushLookToHistory(lookPayload, fallbackVariation = 0) {
        const entry = buildLookEntry(lookPayload, fallbackVariation);
        if (!entry) return;

        if (!Array.isArray(window.generatedLooks)) {
            window.generatedLooks = [];
        }

        window.generatedLooks.push(entry);
        window.savedCurrentLookSignature = "";
        setActiveLookByIndex(window.generatedLooks.length - 1);
    }

   function replaceCurrentLookInHistory(lookPayload, fallbackVariation = 0) {
       const entry = buildLookEntry(lookPayload, fallbackVariation);
       if (!entry) return;

       window.savedCurrentLookSignature = "";
       window.activeSavedLookId = "";

       if (!Array.isArray(window.generatedLooks) || !window.generatedLooks.length || window.currentLookIndex < 0) {
           pushLookToHistory(lookPayload, fallbackVariation);
           return;
       }

       window.generatedLooks[window.currentLookIndex] = entry;
       setActiveLookByIndex(window.currentLookIndex);
   }

    function renderLookCarousel() {
        const container = document.getElementById("lookCarouselContainer");
        if (!container) return;

        const looks = Array.isArray(window.generatedLooks) ? window.generatedLooks : [];
        const currentIndex = safeNumber(window.currentLookIndex);

        if (!looks.length) {
            container.innerHTML = "";
            return;
        }

        const chips = looks.map((look, index) => {
            const isActive = index === currentIndex;
            const label = `Look ${index + 1}`;
            const score = safeNumber(look?.fullOutfit?.overallScore);

            return `
                <button
                    type="button"
                    class="look-chip ${isActive ? "active" : ""}"
                    data-look-index="${index}"
                    aria-pressed="${isActive ? "true" : "false"}"
                >
                    <span class="look-chip-title">${escapeHtml(label)}</span>
                    <span class="look-chip-score">${score}%</span>
                </button>
            `;
        }).join("");

        container.innerHTML = `
            <div class="look-carousel-card">
                <div class="look-carousel-header">
                    <div>
                        <div class="look-carousel-eyebrow">LOOK HISTORY</div>
                        <h4 class="look-carousel-title">Browse Generated Looks</h4>
                    </div>
                    <div class="look-carousel-controls">
                        <button type="button" class="look-carousel-nav" id="prevLookBtn" ${currentIndex <= 0 ? "disabled" : ""}>Prev</button>
                        <button type="button" class="look-carousel-nav" id="nextLookBtn" ${currentIndex >= looks.length - 1 ? "disabled" : ""}>Next</button>
                    </div>
                </div>
                <div class="look-carousel-track">${chips}</div>
            </div>
        `;

        document.getElementById("prevLookBtn")?.addEventListener("click", () => setActiveLookByIndex(currentIndex - 1));
        document.getElementById("nextLookBtn")?.addEventListener("click", () => setActiveLookByIndex(currentIndex + 1));

        container.querySelectorAll("[data-look-index]").forEach(button => {
            button.addEventListener("click", () => {
                const idx = safeNumber(button.getAttribute("data-look-index"));
                setActiveLookByIndex(idx);
            });
        });
    }

    function syncCurrentLookState(fullOutfit) {
        window.currentLookState = {
            topRfid: getItemField(fullOutfit?.top, "rfid", "itemRfid", "productRfid", "id") || "",
            bottomRfid: getItemField(fullOutfit?.bottom, "rfid", "itemRfid", "productRfid", "id") || "",
            shoesRfid: getItemField(fullOutfit?.shoes, "rfid", "itemRfid", "productRfid", "id") || "",
            outerwearRfid: getItemField(fullOutfit?.outerwear, "rfid", "itemRfid", "productRfid", "id") || ""
        };
    }

   function renderAnchorPiece(scannedItem) {
    const container = document.getElementById("anchorPieceContainer");
    if (!container) return;

    if (!scannedItem) {
        container.innerHTML = "";
        return;
    }

    const name = getItemField(scannedItem, "name", "itemName") || "Scanned Item";
    const brand = getItemField(scannedItem, "brand") || "";
    const storeName = getItemField(scannedItem, "storeName") || getSelectedStoreCode();
    const retailer = getItemField(scannedItem, "retailer", "retailerName") || "";
    const category = getItemField(scannedItem, "category") || "Item";
    const color = getItemField(scannedItem, "color") || "Neutral";
    const price = getItemField(scannedItem, "price");

    const imageUrl = safeImageUrl(
        getItemField(scannedItem, "imageUrl", "image_url", "image", "photoUrl", "productImageUrl"),
        "https://placehold.co/500x620?text=Anchor+Piece"
    );

    const subline =
        brand && retailer && brand.trim().toLowerCase() !== retailer.trim().toLowerCase()
            ? `${brand} • ${retailer}`
            : (brand || retailer || "");

    const advice =
        getItemField(scannedItem, "stylingAdvice") ||
        "This is the anchor piece the look is built around.";

    container.innerHTML = `
        <div class="anchor-piece-card">
            <div class="anchor-piece-header">
                <div>
                    <div class="anchor-piece-eyebrow">Anchor Piece</div>
                    <h3 class="anchor-piece-title">Built Around Your Scan</h3>
                </div>
                <div class="anchor-piece-pill">Scanned Item</div>
            </div>

            <div class="anchor-piece-grid">
                <div class="anchor-piece-image-wrap">
                    <img
                        src="${imageUrl}"
                        alt="${escapeHtml(name)}"
                        class="anchor-piece-image"
                        onerror="this.src='https://placehold.co/500x620?text=Anchor+Piece';"
                    />
                </div>

                <div class="anchor-piece-body">
                    <div class="anchor-piece-info">
                        <div class="anchor-piece-info-label">Item</div>
                        <h4 class="anchor-piece-name">${escapeHtml(name)}</h4>
                        <p class="anchor-piece-sub">${escapeHtml(subline || category)}</p>
                    </div>

                    <div class="anchor-piece-meta">
                        <span class="anchor-piece-chip">${escapeHtml(category)}</span>
                        <span class="anchor-piece-chip">${escapeHtml(color)}</span>
                        <span class="anchor-piece-chip">${formatPrice(price)}</span>
                        <span class="anchor-piece-chip">${escapeHtml(storeName)}</span>
                    </div>

                    <div class="anchor-piece-info">
                        <div class="anchor-piece-info-label">Why it anchors the look</div>
                        <p class="anchor-piece-copy">${escapeHtml(advice)}</p>
                    </div>
                </div>
            </div>
        </div>
    `;
}

    function clearAnchorPiece() {
        const container = document.getElementById("anchorPieceContainer");
        if (container) container.innerHTML = "";
    }

    function renderFullOutfitItem(item, role) {
        if (!item) {
            return `
                <div class="full-outfit-item empty">
                    <div class="full-outfit-item-body">
                        <div class="full-outfit-role">${escapeHtml(role)}</div>
                        <div class="full-outfit-empty">No item selected</div>
                    </div>
                </div>
            `;
        }

        const name = getItemField(item, "name", "itemName") || role;
        const brand = getItemField(item, "brand") || "";
        const retailer = getItemField(item, "retailer", "retailerName") || "";
        const price = getItemField(item, "price");
        const imageUrl = safeImageUrl(
            getItemField(item, "imageUrl"),
            "https://placehold.co/500x620?text=No+Image"
        );

        const metaText =
            brand && retailer && brand.trim().toLowerCase() !== retailer.trim().toLowerCase()
                ? `${brand} • ${retailer}`
                : (brand || retailer || "");

        const priceText =
            typeof price === "number" || !Number.isNaN(Number(price))
                ? `$${Number(price).toFixed(2)}`
                : "$0.00";

        return `
            <div class="full-outfit-item">
                <img
                    src="${imageUrl}"
                    alt="${escapeHtml(name)}"
                    onerror="this.src='https://placehold.co/500x620?text=No+Image'"
                />
                <div class="full-outfit-item-body">
                    <div class="full-outfit-role">${escapeHtml(role)}</div>
                    <div class="full-outfit-item-name">${escapeHtml(name)}</div>
                    <div class="full-outfit-item-meta">${escapeHtml(metaText)}</div>
                   <div class="full-outfit-item-price">${priceText}</div>
                   ${renderBudgetPreferenceNote(item)}
                </div>
            </div>
        `;
    }

    function renderFullOutfitScore(label, score, variant) {
        const safeScore = Math.max(0, Math.min(100, safeNumber(score)));

        return `
            <div class="full-outfit-score-box">
                <div class="full-outfit-score-row">
                    <span>${escapeHtml(label)}</span>
                    <span>${safeScore}%</span>
                </div>
                <div class="full-outfit-score-track">
                    <div class="full-outfit-score-fill ${variant}" style="width:${safeScore}%"></div>
                </div>
            </div>
        `;
    }

   function getSavedLooksStorageKey() {
    const context = getJwtContext();
    const userPart = context.userId || context.email || "anonymous";
    const storePart = context.storeCode || "no-store";

    return `savedLooks:${userPart}:${storePart}`;
}

async function getSavedLooks() {
    try {
        requireToken();

        const response = await fetch(`${API.stylist}/saved-looks`, {
            method: "GET",
            headers: getAuthHeaders()
        });

        await assertAuthorizedResponse(response, "Unable to load saved looks.");

        const data = await response.json();

        return Array.isArray(data)
            ? data.map(normalizeBackendSavedLook).filter(Boolean)
            : [];
    } catch (error) {
        console.error("Failed to load backend saved looks:", error);
        showToast(error.message || "Unable to load saved looks.", "error");
        return [];
    }
}

function setSavedLooks(looks) {
    /*
     * Backend persistence is now the source of truth.
     * This function is kept only so older calls do not break.
     */
    sessionStorage.setItem(
        getSavedLooksStorageKey(),
        JSON.stringify(Array.isArray(looks) ? looks : [])
    );
}

function normalizeBackendSavedLook(entry) {
    if (!entry || typeof entry !== "object") {
        return null;
    }

    const look = entry.look || null;
    const anchor = entry.anchor || buildFallbackAnchorFromLook(look);
    const vibe = String(entry.vibe || "").trim() || "Casual";

    const rawTags = Array.isArray(entry.tags)
        ? entry.tags
        : String(entry.tags || "")
            .split(",")
            .map(tag => tag.trim())
            .filter(Boolean);

    const shareToken = String(entry.shareToken || "").trim();
    const publicShareEnabled = Boolean(entry.publicShareEnabled);
    const backendShareUrl = String(entry.shareUrl || "").trim();

    const shareUrl = backendShareUrl
        ? `${window.location.origin}${backendShareUrl.startsWith("/") ? backendShareUrl : `/${backendShareUrl}`}`
        : (
            publicShareEnabled && shareToken
                ? `${window.location.origin}/api/v1/macy-stylist/saved-looks/shared/${encodeURIComponent(shareToken)}`
                : ""
        );

    return {
        id: entry.id != null ? String(entry.id) : "",
        title: String(entry.title || entry.name || "Saved Look").trim(),
        notes: String(entry.notes || "").trim(),
        tags: rawTags,
        savedAt: entry.savedAt || new Date().toISOString(),
        anchor,
        look,
        suggestions: Array.isArray(entry.suggestions) ? entry.suggestions : [],
        score: safeNumber(entry.score || look?.overallScore),
        vibe,
        retailerKey: entry.retailerKey || getItemField(anchor, "retailerKey") || "",
        storeCode: entry.storeCode || getItemField(anchor, "storeCode") || "",
        storeName: entry.storeName || getItemField(anchor, "storeName") || "",
        active: Boolean(entry.active),
        availability: entry.availability || null,

        shareToken,
        publicShareEnabled,
        shareCreatedAt: entry.shareCreatedAt || "",
        shareUrl
    };
}

    function cloneSavedItem(item) {
        if (!item || typeof item !== "object") return null;

        return {
            rfid: getItemField(item, "rfid", "itemRfid", "productRfid", "id") || "",
            name: getItemField(item, "name", "itemName") || "",
            brand: getItemField(item, "brand") || "",
            retailer: getItemField(item, "retailer", "retailerName") || "",
            retailerKey: getItemField(item, "retailerKey") || "",
            storeCode: getItemField(item, "storeCode") || "",
            storeName: getItemField(item, "storeName") || "",
            category: getItemField(item, "category") || "",
            color: getItemField(item, "color") || "",
            price: getItemField(item, "price") || 0,
            imageUrl: getItemField(item, "imageUrl") || "",
            stylingAdvice: getItemField(item, "stylingAdvice") || "",
            whyItWorks: getItemField(item, "whyItWorks") || ""
        };
    }

    function buildFallbackAnchorFromLook(look) {
        if (!look) return null;

        const firstAvailable =
            look.top ||
            look.bottom ||
            look.shoes ||
            look.outerwear ||
            null;

        return firstAvailable ? cloneSavedItem(firstAvailable) : null;
    }

    function normalizeSavedLookEntry(entry) {
        if (!entry || typeof entry !== "object") return null;

        const look = entry.look || null;
        const anchor = cloneSavedItem(entry.anchor) || buildFallbackAnchorFromLook(look);
        const vibe = String(entry.vibe || "").trim() || "Casual";

        return {
            id: entry.id || `look-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
            savedAt: entry.savedAt || new Date().toISOString(),
            anchor,
            look,
            suggestions: Array.isArray(entry.suggestions) ? entry.suggestions : [],
            score: safeNumber(entry.score || look?.overallScore),
            vibe,
            retailerKey: entry.retailerKey || getItemField(anchor, "retailerKey") || "",
            storeCode: entry.storeCode || getItemField(anchor, "storeCode") || "",
            storeName: entry.storeName || getItemField(anchor, "storeName") || ""
        };
    }

   const SAVED_LOOK_TAG_OPTIONS = [
       { value: "casual", label: "Casual" },
       { value: "work", label: "Work" },
       { value: "date-night", label: "Date Night" },
       { value: "travel", label: "Travel" }
   ];

   function normalizeSavedLookTag(value) {
       return String(value || "")
           .trim()
           .toLowerCase()
           .replace(/\s+/g, "-");
   }

   function getSavedLookPieces(entry) {
       const look = entry?.look || {};
       const anchorRfid = getItemField(entry?.anchor, "rfid", "itemRfid", "productRfid", "id");

       const pieces = [
           { role: "Anchor", item: entry?.anchor },
           { role: "Top", item: look.top },
           { role: "Bottom", item: look.bottom },
           { role: "Shoes", item: look.shoes },
           { role: "Outerwear", item: look.outerwear }
       ];

       const seen = new Set();

       return pieces.filter(piece => {
           const rfid = getItemField(piece.item, "rfid", "itemRfid", "productRfid", "id");

           if (!piece.item || !rfid) {
               return false;
           }

           const key = String(rfid).trim().toUpperCase();

           if (seen.has(key)) {
               return false;
           }

           seen.add(key);

           if (piece.role !== "Anchor" && anchorRfid && String(rfid) === String(anchorRfid)) {
               return false;
           }

           return true;
       });
   }

   function renderSavedLookCollage(entry) {
       const pieces = getSavedLookPieces(entry).slice(0, 4);

       if (!pieces.length) {
           return `
               <div class="saved-look-collage empty">
                   <div>No preview</div>
               </div>
           `;
       }

       while (pieces.length < 4) {
           pieces.push({ role: "Piece", item: null });
       }

       return `
           <div class="saved-look-collage">
               ${pieces.map(piece => {
                   const name = getItemField(piece.item, "name", "itemName") || piece.role;
                   const imageUrl = safeImageUrl(
                       getItemField(piece.item, "imageUrl", "image_url", "image"),
                       "https://placehold.co/240x240?text=Look"
                   );

                   return `
                       <div class="saved-look-collage-tile">
                           ${
                               piece.item
                                   ? `
                                       <img
                                           src="${imageUrl}"
                                           alt="${escapeHtml(name)}"
                                           onerror="this.src='https://placehold.co/240x240?text=Look';"
                                       />
                                       <span>${escapeHtml(piece.role)}</span>
                                   `
                                   : `<div class="saved-look-collage-placeholder">+</div>`
                           }
                       </div>
                   `;
               }).join("")}
           </div>
       `;
   }

   function renderSavedLookTags(tags = []) {
       const safeTags = Array.isArray(tags) ? tags : [];

       if (!safeTags.length) {
           return `<div class="saved-look-tag-row muted">No tags yet</div>`;
       }

       return `
           <div class="saved-look-tag-row">
               ${safeTags.map(tag => `
                   <span class="saved-look-tag">
                       ${escapeHtml(String(tag).replace(/-/g, " "))}
                   </span>
               `).join("")}
           </div>
       `;
   }

   function renderSavedLookTagEditor(entry) {
       const activeTags = new Set(
           Array.isArray(entry.tags)
               ? entry.tags.map(normalizeSavedLookTag)
               : []
       );

       return `
           <div class="saved-look-tag-editor">
               ${SAVED_LOOK_TAG_OPTIONS.map(tag => {
                   const checked = activeTags.has(tag.value);

                   return `
                       <label class="saved-look-tag-check">
                           <input
                               type="checkbox"
                               value="${escapeHtml(tag.value)}"
                               ${checked ? "checked" : ""}
                           />
                           <span>${escapeHtml(tag.label)}</span>
                       </label>
                   `;
               }).join("")}
           </div>
       `;
   }

   function collectSavedLookEditorPayload(card) {
       if (!card) {
           throw new Error("Saved look editor is missing.");
       }

       const title = card.querySelector(".saved-look-title-input")?.value.trim() || "Saved Look";
       const notes = card.querySelector(".saved-look-notes-input")?.value.trim() || "";

       const tags = [...card.querySelectorAll(".saved-look-tag-editor input:checked")]
           .map(input => normalizeSavedLookTag(input.value))
           .filter(Boolean);

       return {
           title,
           notes,
           tags
       };
   }

   function getCurrentLookSignature(fullOutfit = null) {
       const outfit =
           fullOutfit ||
           (
               Array.isArray(window.generatedLooks) && window.currentLookIndex >= 0
                   ? window.generatedLooks[window.currentLookIndex]?.fullOutfit
                   : null
           );

       const anchorRfid = getItemField(lastScannedItem, "rfid", "itemRfid", "productRfid", "id") || "";

       const rfids = [
           anchorRfid,
           getItemField(outfit?.top, "rfid", "itemRfid", "productRfid", "id"),
           getItemField(outfit?.bottom, "rfid", "itemRfid", "productRfid", "id"),
           getItemField(outfit?.shoes, "rfid", "itemRfid", "productRfid", "id"),
           getItemField(outfit?.outerwear, "rfid", "itemRfid", "productRfid", "id")
       ]
           .filter(Boolean)
           .map(value => String(value).trim().toUpperCase())
           .sort();

       if (!rfids.length) {
           return "";
       }

       const vibe = document.getElementById("vibeSelect")?.value || "Casual";

       return `${String(vibe).trim().toUpperCase()}::${rfids.join("|")}`;
   }

    function buildSavedLookPayload(fullOutfit) {
    const activeLook =
        Array.isArray(window.generatedLooks) && window.currentLookIndex >= 0
            ? window.generatedLooks[window.currentLookIndex]
            : null;

    const vibe = document.getElementById("vibeSelect")?.value || "Casual";
    const context = getJwtContext();

    return normalizeSavedLookEntry({
        id: window.activeSavedLookId || `look-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        savedAt: new Date().toISOString(),
        anchor: cloneSavedItem({
            ...(lastScannedItem || {}),
            retailerKey: context.retailerKey,
            storeCode: context.storeCode,
            storeName: getItemField(lastScannedItem, "storeName") || context.storeName || context.storeCode || ""
        }),
        look: fullOutfit,
        suggestions: activeLook?.suggestions || [],
        score: safeNumber(fullOutfit?.overallScore),
        vibe,
        retailerKey: context.retailerKey,
        storeCode: context.storeCode,
        storeName: context.storeName || context.storeCode || ""
    });
}

  function updateSaveLookButtonState(forceDefault = false) {
      const btn = document.getElementById("saveFullLookBtn");
      if (!btn) return;

      const currentSignature = getCurrentLookSignature();

      const isSaved =
          !forceDefault &&
          currentSignature &&
          window.savedCurrentLookSignature &&
          currentSignature === window.savedCurrentLookSignature;

      btn.classList.toggle("saved-state", isSaved);
      btn.textContent = isSaved ? "Saved ✓" : "Save Look";
      btn.disabled = false;
      btn.removeAttribute("aria-busy");
  }

   async function saveFullLook(fullOutfit) {
    try {
        requireToken();

        if (!fullOutfit) {
            showToast("No look available to save.", "error");
            return;
        }
       const currentSignature = getCurrentLookSignature(fullOutfit);

       if (
           currentSignature &&
           window.savedCurrentLookSignature &&
           currentSignature === window.savedCurrentLookSignature
       ) {
           updateSaveLookButtonState();
           showToast("This look is already saved.", "info");
           return;
       }

        const payload = buildSavedLookPayload(fullOutfit);

        const response = await fetch(`${API.stylist}/saved-looks`, {
            method: "POST",
            headers: getAuthHeaders({
                "Content-Type": "application/json"
            }),
            body: JSON.stringify({
                vibe: payload.vibe,
                retailerKey: payload.retailerKey,
                storeCode: payload.storeCode,
                storeName: payload.storeName,
                anchor: payload.anchor,
                look: payload.look,
                suggestions: payload.suggestions,
                score: payload.score
            })
        });

        await assertAuthorizedResponse(response, "Unable to save look.");

        const savedLook = await response.json();
        const normalized = normalizeBackendSavedLook(savedLook);

        if (normalized?.id) {
            window.activeSavedLookId = String(normalized.id);
            window.savedCurrentLookSignature = currentSignature;
        }

        await renderSavedLooksDrawer();
        updateSaveLookButtonState();

        showToast("Look saved to your account.", "success");
    } catch (error) {
        console.error("Failed to save full look:", error);
        showToast(error.message || "Could not save look.", "error");
    }
}

    function renderSavedLookPiece(item, role) {
        const safeItem = cloneSavedItem(item);

        if (!safeItem) {
            return `
                <div class="saved-look-piece">
                    <div class="saved-look-piece-body">
                        <div class="saved-look-piece-role">${escapeHtml(role)}</div>
                        <div class="saved-look-piece-name muted">—</div>
                    </div>
                </div>
            `;
        }

        const name = getItemField(safeItem, "name", "itemName") || role;
        const imageUrl = safeImageUrl(
            getItemField(safeItem, "imageUrl"),
            "https://placehold.co/200x160?text=Look"
        );

        return `
            <div class="saved-look-piece">
                <img
                    src="${imageUrl}"
                    alt="${escapeHtml(name)}"
                    onerror="this.src='https://placehold.co/200x160?text=Look';"
                />
                <div class="saved-look-piece-body">
                    <div class="saved-look-piece-role">${escapeHtml(role)}</div>
                    <div class="saved-look-piece-name">${escapeHtml(name)}</div>
                </div>
            </div>
        `;
    }

   async function clearAllSavedLooks() {
    const confirmed = window.confirm("Clear all saved looks from your account?");

    if (!confirmed) {
        return;
    }

    try {
        requireToken();

        const response = await fetch(`${API.stylist}/saved-looks`, {
            method: "DELETE",
            headers: getAuthHeaders()
        });

        await assertAuthorizedResponse(response, "Unable to clear saved looks.");

        window.activeSavedLookId = "";
        window.savedCurrentLookSignature = "";

        await renderSavedLooksDrawer();
        updateSaveLookButtonState();

        showToast("All saved looks cleared.", "info");
    } catch (error) {
        console.error("Clear Saved Looks Error:", error);
        showToast(error.message || "Unable to clear saved looks.", "error");
    }
}

   function getUniqueRfidsFromSavedLook(entry) {
       const look = entry?.look || {};

       const candidates = [
           entry?.anchor,
           look?.top,
           look?.bottom,
           look?.shoes,
           look?.outerwear
       ];

       const seen = new Set();

       return candidates
           .map(item => getItemField(item, "rfid", "itemRfid", "productRfid", "id"))
           .filter(Boolean)
           .map(rfid => String(rfid).trim())
           .filter(rfid => {
               const key = rfid.toUpperCase();

               if (seen.has(key)) {
                   return false;
               }

               seen.add(key);
               return true;
           });
   }

   async function addSavedLookToBagById(lookId, triggerButton = null) {
       const safeLookId = String(lookId || "").trim();

       if (!safeLookId) {
           showToast("Saved look id is missing.", "error");
           return;
       }

       const originalText = triggerButton?.textContent || "Add Look to Bag";

       try {
           requireToken();

           if (triggerButton) {
               triggerButton.disabled = true;
               triggerButton.textContent = "Adding...";
           }

           const response = await fetch(`${API.stylist}/saved-looks/${encodeURIComponent(safeLookId)}`, {
               method: "GET",
               headers: getAuthHeaders({
                   Accept: "application/json"
               })
           });

           await assertAuthorizedResponse(response, "Unable to load saved look.");

           const rawEntry = await response.json();
           const entry = normalizeBackendSavedLook(rawEntry);

           if (!entry) {
               throw new Error("Saved look not found.");
           }

           const rfids = getUniqueRfidsFromSavedLook(entry);

           if (!rfids.length) {
               throw new Error("This saved look has no items to add.");
           }

           let addedCount = 0;
           let skippedCount = 0;
           const errors = [];

           for (const rfid of rfids) {
               try {
                   if (savedRfids.has(rfid)) {
                       skippedCount += 1;
                       continue;
                   }

                   const saveResponse = await fetch(`${API.stylist}/save/${encodeURIComponent(rfid)}`, {
                       method: "POST",
                       headers: getAuthHeaders()
                   });

                   if (!saveResponse.ok) {
                       const text = await saveResponse.text().catch(() => "");
                       errors.push(
                           parseBackendMessage(text) ||
                           cleanApiErrorMessage(text) ||
                           `Unable to add ${rfid}.`
                       );
                       continue;
                   }

                   savedRfids.add(rfid);
                   addedCount += 1;
               } catch (error) {
                   errors.push(error.message || `Unable to add ${rfid}.`);
               }
           }

           await Promise.allSettled([
               loadBag(),
               loadAllInsights()
           ]);

           if (addedCount > 0) {
               showToast(
                   `${addedCount} item${addedCount === 1 ? "" : "s"} added to your bag.`,
                   "success"
               );
           } else if (skippedCount > 0 && !errors.length) {
               showToast("This saved look is already in your bag.", "info");
           }

           if (errors.length) {
               showToast(errors[0], "error");
               console.warn("Saved look add-to-bag partial errors:", errors);
           }

           const savedLooksPanel = document.getElementById("savedLooksSidebar");

           if (savedLooksPanel) {
               const instance =
                   bootstrap.Offcanvas.getInstance(savedLooksPanel) ||
                   new bootstrap.Offcanvas(savedLooksPanel);

               instance.hide();

               window.setTimeout(() => {
                   openBagDrawer();
               }, 350);
           } else {
               openBagDrawer();
           }
       } catch (error) {
           console.error("Add Saved Look To Bag Error:", error);
           showToast(error.message || "Unable to add saved look to bag.", "error");
       } finally {
           if (triggerButton) {
               triggerButton.disabled = false;
               triggerButton.textContent = originalText;
           }
       }
   }

   async function renderSavedLooksDrawer() {
       const container = document.getElementById("savedLooksContent");
       if (!container) return;

       if (!getToken()) {
           container.innerHTML = `
               <div class="saved-look-empty">
                   <div class="bag-empty-icon">🔐</div>
                   <div class="bag-empty-title">Login required</div>
                   <p class="bag-empty-text">Please log in to view your saved looks.</p>
               </div>
           `;
           return;
       }

       container.innerHTML = `<div class="loading-state">Loading saved looks...</div>`;

       let savedLooks = [];

       try {
           savedLooks = await getSavedLooks();
           window.lastSavedLooks = savedLooks;
       } catch (error) {
           console.warn("Saved looks unavailable:", error);
           savedLooks = [];
       }

       if (!savedLooks.length) {
           container.innerHTML = `
               <div class="saved-look-empty">
                   <div class="bag-empty-icon">✨</div>
                   <div class="bag-empty-title">No saved looks yet</div>
                   <p class="bag-empty-text">Generate a look and tap “Save Look” to store it to your account.</p>
               </div>
           `;
           return;
       }

       container.innerHTML = `
           <div class="saved-look-toolbar">
               <button type="button" class="saved-look-clear-btn" id="clearAllSavedLooksBtn">
                   Clear All Saved Looks
               </button>
           </div>

           <div class="saved-look-list">
               ${savedLooks.map(entry => {
                   const look = entry.look || {};
                   const savedDate = entry.savedAt
                       ? new Date(entry.savedAt).toLocaleString()
                       : "Recently saved";

                   const isActive = String(entry.id) === String(window.activeSavedLookId);
                   const anchorRfid = getItemField(entry.anchor, "rfid", "itemRfid", "productRfid", "id");

                   const pieces = [];

                   pieces.push(renderSavedLookPiece(entry.anchor, "Anchor"));

                   if (look.top && getItemField(look.top, "rfid", "itemRfid", "productRfid", "id") !== anchorRfid) {
                       pieces.push(renderSavedLookPiece(look.top, "Top"));
                   }

                   if (look.bottom && getItemField(look.bottom, "rfid", "itemRfid", "productRfid", "id") !== anchorRfid) {
                       pieces.push(renderSavedLookPiece(look.bottom, "Bottom"));
                   }

                   if (look.shoes && getItemField(look.shoes, "rfid", "itemRfid", "productRfid", "id") !== anchorRfid) {
                       pieces.push(renderSavedLookPiece(look.shoes, "Shoes"));
                   }

                   if (look.outerwear && getItemField(look.outerwear, "rfid", "itemRfid", "productRfid", "id") !== anchorRfid) {
                       pieces.push(renderSavedLookPiece(look.outerwear, "Outerwear"));
                   }

                   while (pieces.length < 4) {
                       pieces.push(renderSavedLookPiece(null, "Piece"));
                   }

                   return `
                       <div class="saved-look-card ${isActive ? "active" : ""}" data-look-id="${escapeHtml(entry.id)}">
                           <div class="saved-look-header">
                               <div>
                                   <h5 class="saved-look-title">${escapeHtml(entry.title || "Saved Look")}</h5>
                                   <div class="saved-look-date">${escapeHtml(savedDate)}</div>
                                   <div class="saved-look-date">Vibe: ${escapeHtml(entry.vibe || "Casual")}</div>
                                   ${isActive ? `<div class="saved-look-status">Currently Loaded</div>` : ""}
                               </div>

                               <div class="saved-look-score">
                                   ${safeNumber(entry.score || look.overallScore)}%
                               </div>
                           </div>

                           ${renderSavedLookCollage(entry)}

                           ${renderSavedLookTags(entry.tags)}

                           ${
                               entry.notes
                                   ? `<div class="saved-look-notes-display">${escapeHtml(entry.notes)}</div>`
                                   : `<div class="saved-look-notes-display muted">No notes added yet.</div>`
                           }

                           <details class="saved-look-editor">
                               <summary>Edit name, notes, and tags</summary>

                               <div class="saved-look-editor-body">
                                   <label class="saved-look-editor-label">
                                       Look Name
                                       <input
                                           class="saved-look-title-input"
                                           type="text"
                                           maxlength="80"
                                           value="${escapeHtml(entry.title || "Saved Look")}"
                                       />
                                   </label>

                                   <label class="saved-look-editor-label">
                                       Notes
                                       <textarea
                                           class="saved-look-notes-input"
                                           maxlength="400"
                                           rows="3"
                                           placeholder="Add styling notes, occasion ideas, or reminders..."
                                       >${escapeHtml(entry.notes || "")}</textarea>
                                   </label>

                                   <div class="saved-look-editor-label">
                                       Tags
                                       ${renderSavedLookTagEditor(entry)}
                                   </div>

                                   <button
                                       type="button"
                                       class="saved-look-btn primary update-saved-look-btn"
                                       data-look-id="${escapeHtml(entry.id)}"
                                   >
                                       Save Details
                                   </button>
                               </div>
                           </details>

                           <div class="saved-look-grid">
                               ${pieces.slice(0, 4).join("")}
                           </div>

                           <div class="saved-look-actions upgraded">
                               <button
                                   type="button"
                                   class="saved-look-btn primary ${isActive ? "active" : ""} open-saved-look-btn"
                                   data-look-id="${escapeHtml(entry.id)}"
                               >
                                   ${isActive ? "Loaded" : "Open Look"}
                               </button>

                               <button
                                   type="button"
                                   class="saved-look-btn secondary add-saved-look-to-bag-btn"
                                   data-look-id="${escapeHtml(entry.id)}"
                               >
                                   Add Look to Bag
                               </button>

                               <button
                                   type="button"
                                   class="saved-look-btn secondary regenerate-saved-look-btn"
                                   data-look-id="${escapeHtml(entry.id)}"
                               >
                                   Regenerate
                               </button>

                               <button
                                   type="button"
                                   class="saved-look-btn secondary check-saved-look-availability-btn"
                                   data-look-id="${escapeHtml(entry.id)}"
                               >
                                   Check Availability
                               </button>

                               <button
                                   type="button"
                                   class="saved-look-btn secondary share-saved-look-btn"
                                   data-look-id="${escapeHtml(entry.id)}"
                               >
                                   Share Look
                               </button>

                               <button
                                   type="button"
                                   class="saved-look-btn secondary delete-saved-look-btn"
                                   data-look-id="${escapeHtml(entry.id)}"
                               >
                                   Remove
                               </button>
                           </div>
                       </div>
                   `;
               }).join("")}
           </div>
       `;

       document.getElementById("clearAllSavedLooksBtn")?.addEventListener("click", clearAllSavedLooks);

       container.querySelectorAll(".open-saved-look-btn").forEach(button => {
           button.addEventListener("click", () => {
               applySavedLookById(button.dataset.lookId || "");
           });
       });

       container.querySelectorAll(".add-saved-look-to-bag-btn").forEach(button => {
           button.addEventListener("click", () => {
               addSavedLookToBagById(button.dataset.lookId || "", button);
           });
       });

       container.querySelectorAll(".update-saved-look-btn").forEach(button => {
           button.addEventListener("click", () => {
               updateSavedLookMetadata(button.dataset.lookId || "", button);
           });
       });

       container.querySelectorAll(".regenerate-saved-look-btn").forEach(button => {
           button.addEventListener("click", () => {
               regenerateFromSavedLook(button.dataset.lookId || "", button);
           });
       });

       container.querySelectorAll(".check-saved-look-availability-btn").forEach(button => {
           button.addEventListener("click", () => {
               checkSavedLookAvailability(button.dataset.lookId || "", button);
           });
       });

       container.querySelectorAll(".share-saved-look-btn").forEach(button => {
           button.addEventListener("click", () => {
               shareSavedLookPlaceholder(button.dataset.lookId || "", button);
           });
       });

       container.querySelectorAll(".delete-saved-look-btn").forEach(button => {
           button.addEventListener("click", () => {
               deleteSavedLookById(button.dataset.lookId || "");
           });
       });
   }

   const CUSTOMER_PREFERENCES_API = "/api/v1/customer/preferences";

function getDefaultPreferences() {
    return {
        sizeTop: "",
        sizeBottom: "",
        shoeSize: "",
        budgetMin: "",
        budgetMax: "",
        favoriteColors: "",
        avoidedColors: "",
        fitPreference: "Regular",
        genderStyle: "Any",
        preferredMaterials: "",
        dislikedMaterials: "",
        styleKeywords: "",
        dislikedStyles: "",
        occasionPriority: "Everyday",
        notes: ""
    };
}

function normalizePreferences(preferences) {
    return {
        ...getDefaultPreferences(),
        ...(preferences && typeof preferences === "object" ? preferences : {})
    };
}

async function fetchBackendPreferences() {
    requireToken();

    const response = await fetch("/api/v1/customer/preferences", {
        method: "GET",
        headers: getAuthHeaders({
            Accept: "application/json"
        })
    });

    await assertAuthorizedResponse(response, "Unable to load preferences.");

    if (response.status === 204) {
        return getDefaultPreferences();
    }

    const data = await response.json().catch(() => null);

    return normalizePreferences(data);
}

async function saveBackendPreferences(preferences) {
    requireToken();

    const response = await fetch("/api/v1/customer/preferences", {
        method: "PUT",
        headers: getAuthHeaders({
            "Content-Type": "application/json",
            Accept: "application/json"
        }),
        body: JSON.stringify(preferences)
    });

    await assertAuthorizedResponse(response, "Unable to save preferences.");

    const data = await response.json().catch(() => null);

    return normalizePreferences(data);
}

async function resetBackendPreferences() {
    requireToken();

    const response = await fetch("/api/v1/customer/preferences", {
        method: "DELETE",
        headers: getAuthHeaders()
    });

    await assertAuthorizedResponse(response, "Unable to reset preferences.");
}

function getLocalPreferences() {
    const cached = window.currentCustomerPreferences;

    if (cached && typeof cached === "object") {
        return normalizePreferences(cached);
    }

    return getDefaultPreferences();
}

function setLocalPreferences(preferences) {
    window.currentCustomerPreferences = normalizePreferences(preferences);
}

function buildPreferencesQueryParams() {
    const preferences = getLocalPreferences();
    const params = new URLSearchParams();

    Object.entries(preferences).forEach(([key, value]) => {
        const cleanValue = String(value ?? "").trim();

        if (cleanValue) {
            params.set(key, cleanValue);
        }
    });

    return params;
}

function collectPreferencesFromForm() {
    return {
        sizeTop: document.getElementById("prefSizeTop")?.value.trim() || "",
        sizeBottom: document.getElementById("prefSizeBottom")?.value.trim() || "",
        shoeSize: document.getElementById("prefShoeSize")?.value.trim() || "",
        budgetMin: document.getElementById("prefBudgetMin")?.value.trim() || "",
        budgetMax: document.getElementById("prefBudgetMax")?.value.trim() || "",
        favoriteColors: document.getElementById("prefFavoriteColors")?.value.trim() || "",
        avoidedColors: document.getElementById("prefAvoidedColors")?.value.trim() || "",
        fitPreference: document.getElementById("prefFitPreference")?.value || "Regular",
        genderStyle: document.getElementById("prefGenderStyle")?.value || "Any",
        preferredMaterials: document.getElementById("prefPreferredMaterials")?.value.trim() || "",
        dislikedMaterials: document.getElementById("prefDislikedMaterials")?.value.trim() || "",
        styleKeywords: document.getElementById("prefStyleKeywords")?.value.trim() || "",
        dislikedStyles: document.getElementById("prefDislikedStyles")?.value.trim() || "",
        occasionPriority: document.getElementById("prefOccasionPriority")?.value || "Everyday",
        notes: document.getElementById("prefNotes")?.value.trim() || ""
    };
}

function validatePreferences(preferences) {
    const min = preferences.budgetMin ? Number(preferences.budgetMin) : null;
    const max = preferences.budgetMax ? Number(preferences.budgetMax) : null;

    if (min !== null && (!Number.isFinite(min) || min < 0)) {
        throw new Error("Budget minimum must be a valid positive number.");
    }

    if (max !== null && (!Number.isFinite(max) || max < 0)) {
        throw new Error("Budget maximum must be a valid positive number.");
    }

    if (min !== null && max !== null && min > max) {
        throw new Error("Budget minimum cannot be greater than budget maximum.");
    }
}

function renderPreferencesForm(preferences = getDefaultPreferences()) {
    const container = document.getElementById("preferencesContent");
    if (!container) return;

    const prefs = normalizePreferences(preferences);

    container.innerHTML = `
        <div class="secure-context-box mb-3">
            <div class="secure-context-title">Preference Scope</div>
            <div class="secure-context-value">
                Preferences are saved to your account for this logged-in store.
            </div>
        </div>

        <div class="auth-card mb-3">
            <h5 class="auth-title">Sizing</h5>

            <div class="row g-3">
                <div class="col-12">
                    <label class="form-label fw-bold small text-muted" for="prefSizeTop">Top Size</label>
                    <select id="prefSizeTop" class="form-select auth-input">
                        <option value="">Top size</option>
                        <option value="XS" ${prefs.sizeTop === "XS" ? "selected" : ""}>XS</option>
                        <option value="S" ${prefs.sizeTop === "S" ? "selected" : ""}>S</option>
                        <option value="M" ${prefs.sizeTop === "M" ? "selected" : ""}>M</option>
                        <option value="L" ${prefs.sizeTop === "L" ? "selected" : ""}>L</option>
                        <option value="XL" ${prefs.sizeTop === "XL" ? "selected" : ""}>XL</option>
                        <option value="XXL" ${prefs.sizeTop === "XXL" ? "selected" : ""}>XXL</option>
                        <option value="40R" ${prefs.sizeTop === "40R" ? "selected" : ""}>40R</option>
                        <option value="42R" ${prefs.sizeTop === "42R" ? "selected" : ""}>42R</option>
                        <option value="44R" ${prefs.sizeTop === "44R" ? "selected" : ""}>44R</option>
                    </select>
                </div>

                <div class="col-12">
                    <label class="form-label fw-bold small text-muted" for="prefSizeBottom">Bottom Size</label>
                    <select id="prefSizeBottom" class="form-select auth-input">
                        <option value="">Bottom size</option>
                        <option value="XS" ${prefs.sizeBottom === "XS" ? "selected" : ""}>XS</option>
                        <option value="S" ${prefs.sizeBottom === "S" ? "selected" : ""}>S</option>
                        <option value="M" ${prefs.sizeBottom === "M" ? "selected" : ""}>M</option>
                        <option value="L" ${prefs.sizeBottom === "L" ? "selected" : ""}>L</option>
                        <option value="XL" ${prefs.sizeBottom === "XL" ? "selected" : ""}>XL</option>
                        <option value="30x30" ${prefs.sizeBottom === "30x30" ? "selected" : ""}>30x30</option>
                        <option value="32x30" ${prefs.sizeBottom === "32x30" ? "selected" : ""}>32x30</option>
                        <option value="32x32" ${prefs.sizeBottom === "32x32" ? "selected" : ""}>32x32</option>
                        <option value="34x32" ${prefs.sizeBottom === "34x32" ? "selected" : ""}>34x32</option>
                        <option value="36x32" ${prefs.sizeBottom === "36x32" ? "selected" : ""}>36x32</option>
                    </select>
                </div>

                <div class="col-12">
                    <label class="form-label fw-bold small text-muted" for="prefShoeSize">Shoe Size</label>
                    <select id="prefShoeSize" class="form-select auth-input">
                        <option value="">Shoe size</option>
                        <option value="6" ${prefs.shoeSize === "6" ? "selected" : ""}>6</option>
                        <option value="6.5" ${prefs.shoeSize === "6.5" ? "selected" : ""}>6.5</option>
                        <option value="7" ${prefs.shoeSize === "7" ? "selected" : ""}>7</option>
                        <option value="7.5" ${prefs.shoeSize === "7.5" ? "selected" : ""}>7.5</option>
                        <option value="8" ${prefs.shoeSize === "8" ? "selected" : ""}>8</option>
                        <option value="8.5" ${prefs.shoeSize === "8.5" ? "selected" : ""}>8.5</option>
                        <option value="9" ${prefs.shoeSize === "9" ? "selected" : ""}>9</option>
                        <option value="9.5" ${prefs.shoeSize === "9.5" ? "selected" : ""}>9.5</option>
                        <option value="10" ${prefs.shoeSize === "10" ? "selected" : ""}>10</option>
                        <option value="10.5" ${prefs.shoeSize === "10.5" ? "selected" : ""}>10.5</option>
                        <option value="11" ${prefs.shoeSize === "11" ? "selected" : ""}>11</option>
                        <option value="12" ${prefs.shoeSize === "12" ? "selected" : ""}>12</option>
                        <option value="13" ${prefs.shoeSize === "13" ? "selected" : ""}>13</option>
                    </select>
                </div>
            </div>
        </div>

        <div class="auth-card mb-3">
            <h5 class="auth-title">Budget & Colors</h5>

            <div class="row g-3">
                <div class="col-12">
                    <label class="form-label fw-bold small text-muted" for="prefBudgetMin">Budget Min</label>
                    <input
                        id="prefBudgetMin"
                        type="number"
                        min="0"
                        step="1"
                        class="form-control auth-input"
                        placeholder="50"
                        value="${escapeHtml(prefs.budgetMin)}"
                    />
                </div>

                <div class="col-12">
                    <label class="form-label fw-bold small text-muted" for="prefBudgetMax">Budget Max</label>
                    <input
                        id="prefBudgetMax"
                        type="number"
                        min="0"
                        step="1"
                        class="form-control auth-input"
                        placeholder="250"
                        value="${escapeHtml(prefs.budgetMax)}"
                    />
                </div>

                <div class="col-12">
                    <label class="form-label fw-bold small text-muted" for="prefFavoriteColors">Favorite Colors</label>
                    <input
                        id="prefFavoriteColors"
                        class="form-control auth-input"
                        placeholder="Black, cream, navy, olive..."
                        value="${escapeHtml(prefs.favoriteColors)}"
                    />
                </div>

                <div class="col-12">
                    <label class="form-label fw-bold small text-muted" for="prefAvoidedColors">Avoided Colors</label>
                    <input
                        id="prefAvoidedColors"
                        class="form-control auth-input"
                        placeholder="Neon, orange..."
                        value="${escapeHtml(prefs.avoidedColors)}"
                    />
                </div>
            </div>
        </div>

        <div class="auth-card mb-3">
            <h5 class="auth-title">Style Direction</h5>

            <div class="row g-3">
                <div class="col-md-6">
                    <label class="form-label fw-bold small text-muted" for="prefFitPreference">Fit Preference</label>
                    <select id="prefFitPreference" class="form-select auth-input">
                        <option value="Slim" ${prefs.fitPreference === "Slim" ? "selected" : ""}>Slim</option>
                        <option value="Regular" ${prefs.fitPreference === "Regular" ? "selected" : ""}>Regular</option>
                        <option value="Relaxed" ${prefs.fitPreference === "Relaxed" ? "selected" : ""}>Relaxed</option>
                        <option value="Oversized" ${prefs.fitPreference === "Oversized" ? "selected" : ""}>Oversized</option>
                    </select>
                </div>

                <div class="col-md-6">
                    <label class="form-label fw-bold small text-muted" for="prefGenderStyle">Gender / Style Preference</label>
                    <select id="prefGenderStyle" class="form-select auth-input">
                        <option value="Any" ${prefs.genderStyle === "Any" ? "selected" : ""}>Any</option>
                        <option value="Menswear" ${prefs.genderStyle === "Menswear" ? "selected" : ""}>Menswear</option>
                        <option value="Womenswear" ${prefs.genderStyle === "Womenswear" ? "selected" : ""}>Womenswear</option>
                        <option value="Unisex" ${prefs.genderStyle === "Unisex" ? "selected" : ""}>Unisex</option>
                    </select>
                </div>

                <div class="col-md-6">
                    <label class="form-label fw-bold small text-muted" for="prefOccasionPriority">Primary Occasion</label>
                    <select id="prefOccasionPriority" class="form-select auth-input">
                        <option value="Everyday" ${prefs.occasionPriority === "Everyday" ? "selected" : ""}>Everyday</option>
                        <option value="Work" ${prefs.occasionPriority === "Work" ? "selected" : ""}>Work</option>
                        <option value="Date Night" ${prefs.occasionPriority === "Date Night" ? "selected" : ""}>Date Night</option>
                        <option value="Travel" ${prefs.occasionPriority === "Travel" ? "selected" : ""}>Travel</option>
                        <option value="Formal" ${prefs.occasionPriority === "Formal" ? "selected" : ""}>Formal</option>
                    </select>
                </div>

                <div class="col-md-6">
                    <label class="form-label fw-bold small text-muted" for="prefPreferredMaterials">Preferred Materials</label>
                    <input
                        id="prefPreferredMaterials"
                        class="form-control auth-input"
                        placeholder="Cotton, wool, leather, denim..."
                        value="${escapeHtml(prefs.preferredMaterials)}"
                    />
                </div>

                <div class="col-md-6">
                    <label class="form-label fw-bold small text-muted" for="prefDislikedMaterials">Materials to Avoid</label>
                    <input
                        id="prefDislikedMaterials"
                        class="form-control auth-input"
                        placeholder="Polyester, wool, leather..."
                        value="${escapeHtml(prefs.dislikedMaterials)}"
                    />
                </div>

                <div class="col-md-6">
                    <label class="form-label fw-bold small text-muted" for="prefStyleKeywords">Style Keywords</label>
                    <input
                        id="prefStyleKeywords"
                        class="form-control auth-input"
                        placeholder="Minimal, luxury, streetwear..."
                        value="${escapeHtml(prefs.styleKeywords)}"
                    />
                </div>

                <div class="col-md-6">
                    <label class="form-label fw-bold small text-muted" for="prefDislikedStyles">Styles to Avoid</label>
                    <input
                        id="prefDislikedStyles"
                        class="form-control auth-input"
                        placeholder="Skinny jeans, loud logos..."
                        value="${escapeHtml(prefs.dislikedStyles)}"
                    />
                </div>

                <div class="col-12">
                    <label class="form-label fw-bold small text-muted" for="prefNotes">Extra Notes</label>
                    <textarea
                        id="prefNotes"
                        class="form-control auth-input"
                        rows="4"
                        placeholder="Anything the stylist should know..."
                    >${escapeHtml(prefs.notes)}</textarea>
                </div>
            </div>
        </div>

        <div class="d-grid gap-2">
            <button id="savePreferencesBtn" class="btn merchant-inline-btn primary" type="button">
                Save Preferences
            </button>

            <button id="resetPreferencesBtn" class="btn merchant-inline-btn" type="button">
                Reset Preferences
            </button>
        </div>
    `;

    document.getElementById("savePreferencesBtn")?.addEventListener("click", savePreferences);
    document.getElementById("resetPreferencesBtn")?.addEventListener("click", resetPreferences);
}

async function loadPreferences() {
    const container = document.getElementById("preferencesContent");
    if (!container) return;

    if (!getToken()) {
        container.innerHTML = `
            <div class="bag-empty-shell">
                <div class="bag-empty-icon">🔐</div>
                <div class="bag-empty-title">Login required</div>
                <p class="bag-empty-text">Please log in to manage your styling preferences.</p>
            </div>
        `;
        return;
    }

    try {
        container.innerHTML = `<div class="loading-state">Loading preferences...</div>`;

        const preferences = await fetchBackendPreferences();

        setLocalPreferences(preferences);
        renderPreferencesForm(preferences);
    } catch (error) {
        console.error("Load Preferences Error:", error);

        container.innerHTML = `
            <div class="bag-empty-shell">
                <div class="bag-empty-icon">⚠️</div>
                <div class="bag-empty-title">Unable to load preferences</div>
                <p class="bag-empty-text">${escapeHtml(error.message || "Please try again.")}</p>
            </div>
        `;

        showToast(error.message || "Unable to load preferences.", "error");
    }
}

async function savePreferences() {
    const button = document.getElementById("savePreferencesBtn");

    try {
        requireToken();

        const preferences = collectPreferencesFromForm();
        validatePreferences(preferences);

        if (button) {
            button.disabled = true;
            button.textContent = "Saving...";
        }

        const saved = await saveBackendPreferences(preferences);

        setLocalPreferences(saved);
        renderPreferencesForm(saved);

        showToast("Preferences saved to your account.", "success");
    } catch (error) {
        console.error("Save Preferences Error:", error);
        showToast(error.message || "Unable to save preferences.", "error");
    } finally {
        if (button) {
            button.disabled = false;
            button.textContent = "Save Preferences";
        }
    }
}

async function resetPreferences() {
    const confirmed = window.confirm("Reset your styling preferences?");

    if (!confirmed) return;

    const button = document.getElementById("resetPreferencesBtn");

    try {
        requireToken();

        if (button) {
            button.disabled = true;
            button.textContent = "Resetting...";
        }

        await resetBackendPreferences();

        const defaults = getDefaultPreferences();

        setLocalPreferences(defaults);
        renderPreferencesForm(defaults);

        showToast("Preferences reset.", "info");
    } catch (error) {
        console.error("Reset Preferences Error:", error);
        showToast(error.message || "Unable to reset preferences.", "error");
    } finally {
        if (button) {
            button.disabled = false;
            button.textContent = "Reset Preferences";
        }
    }
}

   async function applySavedLookById(lookId) {
       try {
           requireToken();

           if (!lookId) {
               showToast("Saved look id is missing.", "error");
               return;
           }

           const response = await fetch(`${API.stylist}/saved-looks/${encodeURIComponent(lookId)}`, {
               method: "GET",
               headers: getAuthHeaders()
           });

           await assertAuthorizedResponse(response, "Unable to open saved look.");

           const rawEntry = await response.json();
           const entry = normalizeBackendSavedLook(rawEntry);

           if (!entry || !entry.look) {
               showToast("Saved look not found.", "error");
               return;
           }

           window.activeSavedLookId = String(entry.id);

           const savedVibe = entry.vibe || "Casual";
           const vibeSelect = document.getElementById("vibeSelect");

           if (vibeSelect) {
               vibeSelect.value = savedVibe;
           }

           const anchor =
               entry.anchor ||
               buildFallbackAnchorFromLook(entry.look) ||
               entry.look.top ||
               entry.look.bottom ||
               entry.look.shoes ||
               entry.look.outerwear ||
               {};

           const anchorRfid = getItemField(anchor, "rfid", "itemRfid", "productRfid", "id") || "";

           currentRfid = anchorRfid;
           lastScannedItem = anchor;
           currentLoadedItem = anchor;

           const payload = {
               ...anchor,
               fullOutfit: entry.look,
               suggestions: Array.isArray(entry.suggestions) ? entry.suggestions : [],
               stylingNote: entry.stylingNote || entry.look?.stylingNote || "",
               occasionNote: entry.occasionNote || entry.look?.occasionNote || "",
               seasonNote: entry.seasonNote || entry.look?.seasonNote || "",
               colorNote: entry.colorNote || entry.look?.colorNote || "",
               fitNote: entry.fitNote || entry.look?.fitNote || "",
               materialNote: entry.materialNote || entry.look?.materialNote || "",
               preferenceNote: entry.preferenceNote || entry.look?.preferenceNote || ""
           };

           renderScanResult(payload, savedVibe, {
               suppressAutoScroll: true
           });

           window.activeSavedLookId = String(entry.id);
           window.savedCurrentLookSignature = getCurrentLookSignature(entry.look);

           await renderSavedLooksDrawer();
           updateSaveLookButtonState();
           updateSecureStoreLabels();

           runAfterOffcanvasClosed("savedLooksSidebar", () => {
               scrollToScannedItemResult();
           });

           showToast("Saved look loaded.", "success");
       } catch (error) {
           console.error("Open Saved Look Error:", error);
           showToast(error.message || "Unable to open saved look.", "error");
       }
   }

   async function updateSavedLookMetadata(lookId, triggerButton = null) {
       const safeLookId = String(lookId || "").trim();

       if (!safeLookId) {
           showToast("Saved look id is missing.", "error");
           return;
       }

       const card = triggerButton?.closest(".saved-look-card");
       const originalText = triggerButton?.textContent || "Save Details";

       try {
           requireToken();

           const payload = collectSavedLookEditorPayload(card);

           if (triggerButton) {
               triggerButton.disabled = true;
               triggerButton.textContent = "Saving...";
           }

           const response = await fetch(`${API.stylist}/saved-looks/${encodeURIComponent(safeLookId)}`, {
               method: "PATCH",
               headers: getAuthHeaders({
                   "Content-Type": "application/json",
                   Accept: "application/json"
               }),
               body: JSON.stringify(payload)
           });

           await assertAuthorizedResponse(response, "Unable to update saved look.");

           showToast("Saved look updated.", "success");

           await renderSavedLooksDrawer();
       } catch (error) {
           console.error("Update Saved Look Error:", error);
           showToast(error.message || "Unable to update saved look.", "error");
       } finally {
           if (triggerButton) {
               triggerButton.disabled = false;
               triggerButton.textContent = originalText;
           }
       }
   }

async function checkSavedLookAvailability(lookId, triggerButton = null) {
    const safeLookId = String(lookId || "").trim();

    if (!safeLookId) {
        showToast("Saved look id is missing.", "error");
        return;
    }

    const originalText = triggerButton?.textContent || "Check Availability";

    try {
        requireToken();

        if (triggerButton) {
            triggerButton.disabled = true;
            triggerButton.textContent = "Checking...";
            triggerButton.setAttribute("aria-busy", "true");
        }

        const response = await fetch(
            `${API.stylist}/saved-looks/${encodeURIComponent(safeLookId)}/availability`,
            {
                method: "GET",
                headers: getAuthHeaders({
                    Accept: "application/json"
                })
            }
        );

        await assertAuthorizedResponse(response, "Unable to check saved look availability.");

        const availability = await response.json().catch(() => null);

        if (!availability || typeof availability !== "object") {
            throw new Error("Availability response was empty.");
        }

        const totalCount = safeNumber(availability.totalCount);
        const availableCount = safeNumber(availability.availableCount);
        const unavailableCount = safeNumber(availability.unavailableCount);

        if (availability.allAvailable) {
            showToast(
                availability.message || `All ${availableCount || totalCount} saved look item(s) are currently available.`,
                "success"
            );
            return;
        }

        const unavailableNames = Array.isArray(availability.unavailableItems)
            ? availability.unavailableItems
                .map(item => item.name || item.rfid || "Item")
                .slice(0, 3)
                .join(", ")
            : "";

        showToast(
            availability.message ||
            `${unavailableCount} of ${totalCount} item(s) are unavailable${unavailableNames ? `: ${unavailableNames}` : "."}`,
            "error"
        );
    } catch (error) {
        console.error("Saved Look Availability Error:", error);
        showToast(error.message || "Unable to check saved look availability.", "error");
    } finally {
        if (triggerButton) {
            triggerButton.disabled = false;
            triggerButton.textContent = originalText;
            triggerButton.removeAttribute("aria-busy");
        }
    }
}

  async function regenerateFromSavedLook(lookId, triggerButton = null) {
      const safeLookId = String(lookId || "").trim();

      if (!safeLookId) {
          showToast("Saved look id is missing.", "error");
          return;
      }

      const originalText = triggerButton?.textContent || "Regenerate";

      const renderEntry = entry => {
          if (!entry || !entry.look) {
              throw new Error("Saved look did not return a usable outfit.");
          }

          const vibe = entry.vibe || "Casual";
          const vibeSelect = document.getElementById("vibeSelect");

          if (vibeSelect) {
              vibeSelect.value = vibe;
          }

          const anchor =
              entry.anchor ||
              buildFallbackAnchorFromLook(entry.look) ||
              entry.look.top ||
              entry.look.bottom ||
              entry.look.shoes ||
              entry.look.outerwear ||
              {};

          const anchorRfid = getItemField(anchor, "rfid", "itemRfid", "productRfid", "id") || "";

          currentRfid = anchorRfid;
          lastScannedItem = anchor;
          currentLoadedItem = anchor;

          const payload = {
              ...anchor,
              fullOutfit: entry.look,
              suggestions: Array.isArray(entry.suggestions) ? entry.suggestions : [],
              stylingNote: entry.stylingNote || entry.look?.stylingNote || "",
              occasionNote: entry.occasionNote || entry.look?.occasionNote || "",
              seasonNote: entry.seasonNote || entry.look?.seasonNote || "",
              colorNote: entry.colorNote || entry.look?.colorNote || "",
              fitNote: entry.fitNote || entry.look?.fitNote || "",
              materialNote: entry.materialNote || entry.look?.materialNote || "",
              preferenceNote: entry.preferenceNote || entry.look?.preferenceNote || ""
          };

          renderScanResult(payload, vibe, {
              suppressAutoScroll: true
          });

          window.activeSavedLookId = "";
          window.savedCurrentLookSignature = "";

          updateSaveLookButtonState(true);
      };

      try {
          requireToken();

          if (triggerButton) {
              triggerButton.disabled = true;
              triggerButton.textContent = "Opening...";
              triggerButton.setAttribute("aria-busy", "true");
          }

          const cachedEntry = Array.isArray(window.lastSavedLooks)
              ? window.lastSavedLooks.find(entry => String(entry.id) === safeLookId)
              : null;

          if (cachedEntry) {
              renderEntry(cachedEntry);

              runAfterOffcanvasClosed("savedLooksSidebar", () => {
                  scrollToScannedItemResult();
              });

              showToast("Saved look reopened instantly.", "success");
              return;
          }

          const response = await fetch(
              `${API.stylist}/saved-looks/${encodeURIComponent(safeLookId)}/regenerate`,
              {
                  method: "POST",
                  headers: getAuthHeaders({
                      Accept: "application/json"
                  })
              }
          );

          await assertAuthorizedResponse(response, "Unable to regenerate saved look.");

          const body = await response.json().catch(() => null);
          const rawLook = body?.look || body;
          const entry = normalizeBackendSavedLook(rawLook);

          renderEntry(entry);

          runAfterOffcanvasClosed("savedLooksSidebar", () => {
              scrollToScannedItemResult();
          });

          showToast(body?.message || "Saved look reopened as a fresh styling workspace.", "success");
      } catch (error) {
          console.error("Regenerate Saved Look Error:", error);
          showToast(error.message || "Unable to regenerate saved look.", "error");
      } finally {
          if (triggerButton) {
              triggerButton.disabled = false;
              triggerButton.textContent = originalText;
              triggerButton.removeAttribute("aria-busy");
          }
      }
  }

   async function shareSavedLookPlaceholder(lookId, triggerButton = null) {
       const safeLookId = String(lookId || "").trim();

       if (!safeLookId) {
           showToast("Saved look id is missing.", "error");
           return;
       }

       const originalText = triggerButton?.textContent || "Share Look";

       try {
           requireToken();

           if (triggerButton) {
               triggerButton.disabled = true;
               triggerButton.textContent = "Creating Link...";
               triggerButton.setAttribute("aria-busy", "true");
           }

           const response = await fetch(
               `${API.stylist}/saved-looks/${encodeURIComponent(safeLookId)}/share`,
               {
                   method: "POST",
                   headers: getAuthHeaders({
                       Accept: "application/json"
                   })
               }
           );

           await assertAuthorizedResponse(response, "Unable to create share link.");

           const share = await response.json().catch(() => null);
           const token = String(share?.shareToken || "").trim();

           if (!token) {
               throw new Error("Share token was not returned.");
           }

           const shareUrl = `${window.location.origin}/api/v1/macy-stylist/saved-looks/shared/${encodeURIComponent(token)}`;

           try {
               await navigator.clipboard.writeText(shareUrl);
               showToast("Public share link copied.", "success");
           } catch (clipboardError) {
               console.warn("Clipboard copy failed:", clipboardError);
               window.prompt("Copy this public share link:", shareUrl);
               showToast("Public share link created.", "success");
           }

           if (triggerButton) {
               triggerButton.textContent = "Link Copied ✓";

               window.setTimeout(() => {
                   triggerButton.textContent = originalText;
               }, 1400);
           }

           await renderSavedLooksDrawer();
       } catch (error) {
           console.error("Share Saved Look Error:", error);
           showToast(error.message || "Unable to create share link.", "error");
       } finally {
           if (triggerButton) {
               triggerButton.disabled = false;
               triggerButton.removeAttribute("aria-busy");

               if (triggerButton.textContent !== "Link Copied ✓") {
                   triggerButton.textContent = originalText;
               }
           }
       }
   }


   async function deleteSavedLookById(lookId) {
    try {
        requireToken();

        if (!lookId) {
            showToast("Saved look id is missing.", "error");
            return;
        }

        const response = await fetch(`${API.stylist}/saved-looks/${encodeURIComponent(lookId)}`, {
            method: "DELETE",
            headers: getAuthHeaders()
        });

        await assertAuthorizedResponse(response, "Unable to remove saved look.");

        if (String(window.activeSavedLookId) === String(lookId)) {
            window.activeSavedLookId = "";
            window.savedCurrentLookSignature = "";
            updateSaveLookButtonState(true);
        }

        await renderSavedLooksDrawer();

        showToast("Saved look removed.", "info");
    } catch (error) {
        console.error("Delete Saved Look Error:", error);
        showToast(error.message || "Unable to remove saved look.", "error");
    }
}

    function updateSwapButtonStates(activeCategory = "") {
        const normalized = normalizeCategoryName(activeCategory);

        const buttonMap = {
            tops: document.getElementById("swapTopBtn"),
            bottoms: document.getElementById("swapBottomBtn"),
            shoes: document.getElementById("swapShoesBtn"),
            outerwear: document.getElementById("swapOuterwearBtn")
        };

        Object.entries(buttonMap).forEach(([category, button]) => {
            if (!button) return;

            const isActive = normalized === category;

            button.classList.toggle("active-swap", isActive);
            button.setAttribute("aria-pressed", isActive ? "true" : "false");
        });
    }

   function renderLookStylingNotes(activeLook) {
    if (!activeLook || typeof activeLook !== "object") {
        return "";
    }

    const fullOutfit = activeLook.fullOutfit || activeLook;

    const getNoteValue = (...keys) => {
        for (const key of keys) {
            const value =
                activeLook?.[key] ??
                fullOutfit?.[key];

            if (String(value || "").trim()) {
                return value;
            }
        }

        return "";
    };

    const notes = [
        {
            label: "Styling Note",
            value: getNoteValue("stylingNote")
        },
        {
            label: "Occasion Fit",
            value: getNoteValue("occasionNote")
        },
        {
            label: "Season Fit",
            value: getNoteValue("seasonNote")
        },
        {
            label: "Color Pairing",
            value: getNoteValue("colorNote", "colorPairingNote")
        },
        {
            label: "Fit Guidance",
            value: getNoteValue("fitNote")
        },
        {
            label: "Texture Note",
            value: getNoteValue("materialNote", "textureNote")
        },
        {
            label: "Preference Ready",
            value: getNoteValue("preferenceNote")
        }
    ].filter(note => String(note.value || "").trim());

    if (!notes.length) {
        return "";
    }

    return `
        <div class="full-outfit-notes-grid">
            ${notes.map(note => `
                <div class="full-outfit-note-card">
                    <div class="full-outfit-note-label">${escapeHtml(note.label)}</div>
                    <p class="full-outfit-note-copy">${escapeHtml(note.value)}</p>
                </div>
            `).join("")}
        </div>
    `;
}

function renderFullOutfit(fullOutfit, activeLook = null) {
    const container = document.getElementById("fullOutfitContainer");
    if (!container) return;

    if (!fullOutfit) {
        container.innerHTML = "";
        window.currentLookState = {
            topRfid: "",
            bottomRfid: "",
            shoesRfid: "",
            outerwearRfid: ""
        };
        return;
    }

    const lookForNotes = activeLook || {
        fullOutfit,
        stylingNote: fullOutfit.stylingNote,
        occasionNote: fullOutfit.occasionNote,
        seasonNote: fullOutfit.seasonNote,
        colorNote: fullOutfit.colorNote,
        fitNote: fullOutfit.fitNote,
        materialNote: fullOutfit.materialNote,
        preferenceNote: fullOutfit.preferenceNote
    };

    syncCurrentLookState(fullOutfit);

    const scannedRfid =
        currentRfid ||
        getItemField(lastScannedItem, "rfid", "itemRfid", "productRfid", "id") ||
        "";

    const topRfid = getItemField(fullOutfit?.top, "rfid", "itemRfid", "productRfid", "id") || "";
    const bottomRfid = getItemField(fullOutfit?.bottom, "rfid", "itemRfid", "productRfid", "id") || "";
    const shoesRfid = getItemField(fullOutfit?.shoes, "rfid", "itemRfid", "productRfid", "id") || "";
    const outerwearRfid = getItemField(fullOutfit?.outerwear, "rfid", "itemRfid", "productRfid", "id") || "";

    const canSwapTop = !!fullOutfit?.top && !!topRfid && topRfid !== scannedRfid;
    const canSwapBottom = !!fullOutfit?.bottom && !!bottomRfid && bottomRfid !== scannedRfid;
    const canSwapShoes = !!fullOutfit?.shoes && !!shoesRfid && shoesRfid !== scannedRfid;
    const canSwapOuterwear = !!fullOutfit?.outerwear && !!outerwearRfid && outerwearRfid !== scannedRfid;

    const visibleCards = [];

    if (fullOutfit.top && topRfid !== scannedRfid) {
        visibleCards.push(renderFullOutfitItem(fullOutfit.top, "Top"));
    }

    if (fullOutfit.bottom && bottomRfid !== scannedRfid) {
        visibleCards.push(renderFullOutfitItem(fullOutfit.bottom, "Bottom"));
    }

    if (fullOutfit.shoes && shoesRfid !== scannedRfid) {
        visibleCards.push(renderFullOutfitItem(fullOutfit.shoes, "Shoes"));
    }

    if (fullOutfit.outerwear && outerwearRfid !== scannedRfid) {
        visibleCards.push(renderFullOutfitItem(fullOutfit.outerwear, "Outerwear"));
    }

    const cardsMarkup = visibleCards.length
        ? visibleCards.join("")
        : `
            <div class="full-outfit-item empty">
                <div class="full-outfit-item-body">
                    <div class="full-outfit-role">Look</div>
                    <div class="full-outfit-empty">Additional styled pieces will appear here.</div>
                </div>
            </div>
        `;

    container.innerHTML = `
        <div class="full-outfit-card">
            <div class="full-outfit-header">
                <div>
                    <div class="full-outfit-eyebrow">AI STYLED LOOK</div>
                    <h3 class="full-outfit-title">Complete Look</h3>
                    <p class="full-outfit-subtitle">Complementary pieces curated around your scanned anchor item</p>
                </div>
                <div class="full-outfit-score">Outfit Score: ${safeNumber(fullOutfit.overallScore)}%</div>
            </div>

            <div class="full-outfit-grid">
                ${cardsMarkup}
            </div>

            <div class="full-outfit-explanation">
                <div class="full-outfit-label">Why this works</div>
                <p>${escapeHtml(fullOutfit.explanation || "")}</p>
            </div>

            ${renderLookStylingNotes(lookForNotes)}

            <div class="full-outfit-breakdown">
                ${renderFullOutfitScore("Style Match", fullOutfit.styleScore, "style")}
                ${renderFullOutfitScore("Color Match", fullOutfit.colorScore, "color")}
                ${renderFullOutfitScore("Occasion Match", fullOutfit.occasionScore, "occasion")}
            </div>

            <div class="full-outfit-actions">
                <button class="full-outfit-btn primary" id="saveFullLookBtn" type="button">
                    ${window.activeSavedLookId ? "Replace Saved Look" : "Save Look"}
                </button>

                <button class="full-outfit-btn add-look-to-bag" id="addFullLookToBagBtn" type="button">
                    Add Full Look to Bag
                </button>
                ${canSwapTop ? `<button class="full-outfit-btn secondary" id="swapTopBtn" type="button">Swap Top</button>` : ""}
                ${canSwapBottom ? `<button class="full-outfit-btn secondary" id="swapBottomBtn" type="button">Swap Bottom</button>` : ""}
                ${canSwapShoes ? `<button class="full-outfit-btn secondary" id="swapShoesBtn" type="button">Swap Shoes</button>` : ""}
                ${canSwapOuterwear ? `<button class="full-outfit-btn secondary" id="swapOuterwearBtn" type="button">Swap Outerwear</button>` : ""}
                <button class="full-outfit-btn secondary" id="generateAgainBtn" type="button">Generate Again</button>
            </div>
        </div>
    `;

    document.getElementById("saveFullLookBtn")?.addEventListener("click", () => saveFullLook(fullOutfit));
    document.getElementById("addFullLookToBagBtn")?.addEventListener("click", event => {
        addFullLookToBag(fullOutfit, event.currentTarget);
    });
    updateSaveLookButtonState();

    document.getElementById("swapTopBtn")?.addEventListener("click", e => handleSwapCategory("tops", e.currentTarget));
    document.getElementById("swapBottomBtn")?.addEventListener("click", e => handleSwapCategory("bottoms", e.currentTarget));
    document.getElementById("swapShoesBtn")?.addEventListener("click", e => handleSwapCategory("shoes", e.currentTarget));
    document.getElementById("swapOuterwearBtn")?.addEventListener("click", e => handleSwapCategory("outerwear", e.currentTarget));

    updateSwapButtonStates(window.lastSwapCategory || "");

    const generateAgainBtn = document.getElementById("generateAgainBtn");

    if (generateAgainBtn) {
        generateAgainBtn.onclick = async () => {
            if (!lastScannedItem) {
                showToast("Scan an item first.", "error");
                return;
            }

            const originalText = generateAgainBtn.textContent;
            generateAgainBtn.disabled = true;
            generateAgainBtn.textContent = "Generating...";

            try {
                const vibe = document.getElementById("vibeSelect")?.value || "Casual";
                const rfid =
                    currentRfid ||
                    getItemField(lastScannedItem, "rfid", "itemRfid", "productRfid", "id");

                if (!rfid) {
                    throw new Error("Missing RFID for generate again.");
                }

                requireToken();

                const nextVariation = safeNumber(window.currentLookVariation || 0) + 1;

                const params = await buildPreferencesQueryParams();
                params.set("vibe", vibe);
                params.set("variation", String(nextVariation));

                const resp = await fetch(
                    `${API.stylist}/look/${encodeURIComponent(rfid)}/again?${params.toString()}`,
                    { headers: getAuthHeaders() }
                );

                await assertAuthorizedResponse(resp, `Generate Again failed with status ${resp.status}`);
                const look = await resp.json();

                window.currentLookVariation = safeNumber(look?.variation || nextVariation);
                window.lastSwapCategory = "";

                pushLookToHistory(look, window.currentLookVariation);
                updateSwapButtonStates("");

                scrollToFullOutfitResult();

                showToast("New outfit generated.", "success");
            } catch (error) {
                console.error("Generate Again Error:", error);
                showToast(error.message || "Unable to generate a new outfit.", "error");
            } finally {
                generateAgainBtn.disabled = false;
                generateAgainBtn.textContent = originalText;
            }
        };
    }
}

    async function handleSwapCategory(swapCategory, buttonEl) {
        if (!lastScannedItem) {
            showToast("Scan an item first.", "error");
            return;
        }

        const originalText = buttonEl?.textContent || "Swapping...";
        if (buttonEl) {
            buttonEl.disabled = true;
            buttonEl.textContent = "Swapping...";
        }

        try {
            const vibe = document.getElementById("vibeSelect")?.value || "Casual";
            const rfid =
                currentRfid ||
                getItemField(lastScannedItem, "rfid", "itemRfid", "productRfid", "id");

            if (!rfid) throw new Error("Missing RFID for swap.");

            requireToken();

            const params = await buildPreferencesQueryParams();

            params.set("vibe", vibe);
            params.set("swapCategory", swapCategory);
            params.set("currentTopRfid", window.currentLookState?.topRfid || "");
            params.set("currentBottomRfid", window.currentLookState?.bottomRfid || "");
            params.set("currentShoesRfid", window.currentLookState?.shoesRfid || "");
            params.set("currentOuterwearRfid", window.currentLookState?.outerwearRfid || "");

            const resp = await fetch(
                `${API.stylist}/look/${encodeURIComponent(rfid)}/swap?${params.toString()}`,
                { headers: getAuthHeaders() }
            );

            await assertAuthorizedResponse(resp, `Swap failed with status ${resp.status}`);
            const data = await resp.json();

            window.lastSwapCategory = normalizeCategoryName(swapCategory);
            replaceCurrentLookInHistory(data, safeNumber(window.currentLookVariation || 0));

           scrollToSuggestionsResult();

            const labelMap = {
                tops: "Top",
                bottoms: "Bottom",
                shoes: "Shoes",
                outerwear: "Outerwear"
            };

            showToast(`${labelMap[window.lastSwapCategory] || "Item"} swap options loaded.`, "success");
        } catch (error) {
            console.error(`Swap ${swapCategory} Error:`, error);
            showToast(error.message || "Unable to swap item.", "error");
        } finally {
            if (buttonEl) {
                buttonEl.disabled = false;
                buttonEl.textContent = originalText;
            }
        }
    }

    function normalizeRfid(value) {
        return String(value || "").trim().toUpperCase();
    }

    function getSuggestionRfid(item) {
        return getItemField(item, "rfid", "itemRfid", "productRfid", "id");
    }

    function getSuggestionCategory(item) {
        return normalizeCategoryName(getItemField(item, "category"));
    }

    function isSuggestionAvailable(item) {
        if (!item || typeof item !== "object") {
            return false;
        }

        const stockQuantity = Number(
            item.stockQuantity ??
            item.stock ??
            item.quantity ??
            item.inventoryCount ??
            1
        );

        return !(
            item.available === false ||
            item.active === false ||
            item.enabled === false ||
            item.outOfStock === true ||
            item.discontinued === true ||
            item.inStock === false ||
            (Number.isFinite(stockQuantity) && stockQuantity <= 0)
        );
    }

    function splitPreferenceTokens(value) {
        return String(value || "")
            .split(",")
            .map(token => token.trim().toLowerCase())
            .filter(Boolean);
    }

    function textContainsAny(sourceText, tokens) {
        const haystack = String(sourceText || "").toLowerCase();

        return tokens.some(token => {
            return token && haystack.includes(token);
        });
    }

    function getSuggestionSearchText(item) {
        return [
            getItemField(item, "name", "itemName"),
            getItemField(item, "brand"),
            getItemField(item, "category"),
            getItemField(item, "color"),
            getItemField(item, "material"),
            getItemField(item, "fit"),
            getItemField(item, "gender"),
            getItemField(item, "season"),
            getItemField(item, "occasion"),
            getItemField(item, "styleTags"),
            getItemField(item, "pattern"),
            getItemField(item, "reason"),
            getItemField(item, "whyItWorks"),
            getItemField(item, "stylingAdvice")
        ]
            .filter(Boolean)
            .join(" ")
            .toLowerCase();
    }

    function getPreferenceMatchDetails(item) {
        const preferences = getLocalPreferences();
        const details = [];
        const searchText = getSuggestionSearchText(item);

        const color = String(getItemField(item, "color") || "").toLowerCase();
        const price = Number(getItemField(item, "price") || 0);

        const favoriteColors = splitPreferenceTokens(preferences.favoriteColors);
        const avoidedColors = splitPreferenceTokens(preferences.avoidedColors);
        const styleKeywords = splitPreferenceTokens(preferences.styleKeywords);
        const dislikedStyles = splitPreferenceTokens(preferences.dislikedStyles);
        const preferredMaterials = splitPreferenceTokens(preferences.preferredMaterials);
        const dislikedMaterials = splitPreferenceTokens(preferences.dislikedMaterials);

        const budgetMin = Number(preferences.budgetMin || 0);
        const budgetMax = Number(preferences.budgetMax || 0);

        if (favoriteColors.length && textContainsAny(color, favoriteColors)) {
            details.push({
                type: "positive",
                label: "Favorite color",
                text: "Color matches your saved preferences."
            });
        }

        if (avoidedColors.length && textContainsAny(color, avoidedColors)) {
            details.push({
                type: "negative",
                label: "Avoided color",
                text: "Color appears in your avoided colors."
            });
        }

        if (styleKeywords.length && textContainsAny(searchText, styleKeywords)) {
            details.push({
                type: "positive",
                label: "Style preference",
                text: "Style language matches your profile."
            });
        }

        if (dislikedStyles.length && textContainsAny(searchText, dislikedStyles)) {
            details.push({
                type: "negative",
                label: "Avoided style",
                text: "This may overlap with a style you avoid."
            });
        }

        if (preferredMaterials.length && textContainsAny(searchText, preferredMaterials)) {
            details.push({
                type: "positive",
                label: "Preferred material",
                text: "Material aligns with your saved preferences."
            });
        }

        if (dislikedMaterials.length && textContainsAny(searchText, dislikedMaterials)) {
            details.push({
                type: "negative",
                label: "Avoided material",
                text: "Material may conflict with your saved preferences."
            });
        }

        if (Number.isFinite(price) && price > 0) {
            if (budgetMin > 0 && price < budgetMin) {
                details.push({
                    type: "neutral",
                    label: "Below budget",
                    text: "Price is below your preferred range."
                });
            }

            if (budgetMax > 0 && price > budgetMax) {
                details.push({
                    type: "negative",
                    label: "Above budget",
                    text: "Price is above your preferred range."
                });
            }

            if (
                (budgetMin > 0 || budgetMax > 0) &&
                (budgetMin <= 0 || price >= budgetMin) &&
                (budgetMax <= 0 || price <= budgetMax)
            ) {
                details.push({
                    type: "positive",
                    label: "Budget match",
                    text: "Price sits inside your preferred budget."
                });
            }
        }

        if (
            preferences.occasionPriority &&
            preferences.occasionPriority !== "Everyday" &&
            textContainsAny(searchText, [preferences.occasionPriority.toLowerCase()])
        ) {
            details.push({
                type: "positive",
                label: "Occasion match",
                text: `Works for your ${preferences.occasionPriority} priority.`
            });
        }

        if (
            preferences.fitPreference &&
            preferences.fitPreference !== "Regular" &&
            textContainsAny(searchText, [preferences.fitPreference.toLowerCase()])
        ) {
            details.push({
                type: "positive",
                label: "Fit match",
                text: `Matches your ${preferences.fitPreference.toLowerCase()} fit preference.`
            });
        }

        if (
            preferences.genderStyle &&
            preferences.genderStyle !== "Any" &&
            textContainsAny(searchText, [preferences.genderStyle.toLowerCase()])
        ) {
            details.push({
                type: "positive",
                label: "Style scope",
                text: `Aligns with your ${preferences.genderStyle} preference.`
            });
        }

        return details;
    }

    function getPreferenceScore(item) {
        const explicitScore = Number(
            getItemField(item, "preferenceMatch", "preferenceMatchScore")
        );

        if (Number.isFinite(explicitScore) && explicitScore > 0) {
            return Math.max(0, Math.min(100, explicitScore));
        }

        const details = getPreferenceMatchDetails(item);
        const positiveCount = details.filter(detail => detail.type === "positive").length;
        const negativeCount = details.filter(detail => detail.type === "negative").length;

        if (!positiveCount && !negativeCount) {
            return 0;
        }

        return Math.max(0, Math.min(100, 70 + positiveCount * 8 - negativeCount * 14));
    }

    function renderPreferenceLabelPills(item) {
        const details = getPreferenceMatchDetails(item);

        if (!details.length) {
            return "";
        }

        return `
            <div class="preference-mini-grid mt-2">
                ${details.slice(0, 4).map(detail => `
                    <span class="preference-mini-pill ${escapeHtml(detail.type)}">
                        ${escapeHtml(detail.label)}
                    </span>
                `).join("")}
            </div>
        `;
    }

    function buildWhyThisMatchesYou(item, fullOutfit = null, swapCategory = "") {
        const backendReason =
            getItemField(item, "reason", "whyItWorks", "stylingAdvice") || "";

        const preferenceDetails = getPreferenceMatchDetails(item)
            .filter(detail => detail.type === "positive")
            .map(detail => detail.text);

        const category = getItemField(item, "category") || "piece";
        const color = getItemField(item, "color") || "neutral";
        const brand = getItemField(item, "brand") || "";
        const normalizedSwap = normalizeCategoryName(swapCategory);

        const currentLookColors = [
            getItemField(fullOutfit?.top, "color"),
            getItemField(fullOutfit?.bottom, "color"),
            getItemField(fullOutfit?.shoes, "color"),
            getItemField(fullOutfit?.outerwear, "color")
        ]
            .filter(Boolean)
            .map(value => String(value).toLowerCase());

        const colorConnection = currentLookColors.includes(String(color).toLowerCase())
            ? `It repeats the ${color} tone already present in the look.`
            : `The ${color} color adds a clean styling contrast.`;

        const swapCopy = normalizedSwap
            ? `It is prioritized as a ${normalizedSwap.replace(/s$/, "")} alternative.`
            : `It expands the outfit without duplicating your current pieces.`;

        const lines = [
            backendReason,
            brand ? `${brand} gives this ${category} a stronger retail identity.` : "",
            colorConnection,
            swapCopy,
            ...preferenceDetails
        ].filter(Boolean);

        return lines.slice(0, 3).join(" ");
    }

    function getCurrentFullOutfit() {
        if (
            Array.isArray(window.generatedLooks) &&
            window.currentLookIndex >= 0 &&
            window.generatedLooks[window.currentLookIndex]?.fullOutfit
        ) {
            return window.generatedLooks[window.currentLookIndex].fullOutfit;
        }

        return null;
    }

    function getLookSlotForSuggestion(item, swapCategory = "") {
        const normalizedSwap = normalizeCategoryName(swapCategory);
        const normalizedItemCategory = getSuggestionCategory(item);

        if (["tops", "bottoms", "shoes", "outerwear"].includes(normalizedSwap)) {
            return normalizedSwap;
        }

        if (["tops", "bottoms", "shoes", "outerwear"].includes(normalizedItemCategory)) {
            return normalizedItemCategory;
        }

        return "";
    }

    function cloneLookWithSuggestion(fullOutfit, item, swapCategory = "") {
        if (!fullOutfit || !item) {
            return null;
        }

        const slot = getLookSlotForSuggestion(item, swapCategory);

        if (!slot) {
            return null;
        }

        const nextLook = {
            ...fullOutfit,
            top: fullOutfit.top ? { ...fullOutfit.top } : null,
            bottom: fullOutfit.bottom ? { ...fullOutfit.bottom } : null,
            shoes: fullOutfit.shoes ? { ...fullOutfit.shoes } : null,
            outerwear: fullOutfit.outerwear ? { ...fullOutfit.outerwear } : null
        };

        const normalizedItem = {
            ...item,
            rfid: getSuggestionRfid(item),
            name: getItemField(item, "name", "itemName") || "Suggested Item",
            itemName: getItemField(item, "itemName", "name") || "Suggested Item",
            category: getItemField(item, "category") || ""
        };

        if (slot === "tops") {
            nextLook.top = normalizedItem;
        }

        if (slot === "bottoms") {
            nextLook.bottom = normalizedItem;
        }

        if (slot === "shoes") {
            nextLook.shoes = normalizedItem;
        }

        if (slot === "outerwear") {
            nextLook.outerwear = normalizedItem;
        }

        const scoreCandidates = [
            safeNumber(getItemField(item, "matchScore")),
            safeNumber(fullOutfit.overallScore),
            88
        ].filter(score => score > 0);

        nextLook.overallScore = Math.round(
            scoreCandidates.reduce((sum, score) => sum + score, 0) / scoreCandidates.length
        );

        nextLook.explanation = buildWhyThisMatchesYou(item, fullOutfit, swapCategory);

        return nextLook;
    }

    async function addSuggestionToCurrentLook(item, triggerButton = null) {
        const fullOutfit = getCurrentFullOutfit();

        if (!fullOutfit) {
            showToast("Create a full look before adding a suggestion.", "error");
            return;
        }

        if (!isSuggestionAvailable(item)) {
            showToast("This suggestion is not currently available.", "error");
            return;
        }

        const nextLook = cloneLookWithSuggestion(fullOutfit, item, window.lastSwapCategory || "");

        if (!nextLook) {
            showToast("This suggestion does not match a replaceable outfit slot.", "error");
            return;
        }

        const originalText = triggerButton?.textContent || "Add to Current Look";

        try {
            if (triggerButton) {
                triggerButton.disabled = true;
                triggerButton.textContent = "Adding...";
            }

            const currentEntry =
                Array.isArray(window.generatedLooks) && window.currentLookIndex >= 0
                    ? window.generatedLooks[window.currentLookIndex]
                    : {};

            const suggestionRfid = normalizeRfid(getSuggestionRfid(item));

            const remainingSuggestions = Array.isArray(currentEntry?.suggestions)
                ? currentEntry.suggestions.filter(suggestion => {
                    return normalizeRfid(getSuggestionRfid(suggestion)) !== suggestionRfid;
                })
                : [];

            replaceCurrentLookInHistory({
                variation: safeNumber(currentEntry?.variation || window.currentLookVariation || 0),
                fullOutfit: nextLook,
                suggestions: remainingSuggestions,
                stylingNote: currentEntry?.stylingNote || "",
                occasionNote: currentEntry?.occasionNote || "",
                seasonNote: currentEntry?.seasonNote || "",
                colorNote: currentEntry?.colorNote || "",
                fitNote: currentEntry?.fitNote || "",
                materialNote: currentEntry?.materialNote || "",
                preferenceNote: currentEntry?.preferenceNote || ""
            }, safeNumber(currentEntry?.variation || window.currentLookVariation || 0));

            window.activeSavedLookId = "";
            window.savedCurrentLookSignature = "";

            updateSaveLookButtonState(true);

            scrollToFullOutfitResult();

            showToast("Suggestion added to the current look.", "success");
        } catch (error) {
            console.error("Add Suggestion To Look Error:", error);
            showToast(error.message || "Unable to add suggestion to look.", "error");
        } finally {
            if (triggerButton) {
                triggerButton.disabled = false;
                triggerButton.textContent = originalText;
            }
        }
    }

    function filterOutCurrentLookItems(suggestions, fullOutfit) {
        if (!Array.isArray(suggestions) || suggestions.length === 0) {
            return [];
        }

        const currentRfids = new Set(
            [
                currentRfid,
                getItemField(lastScannedItem, "rfid", "itemRfid", "productRfid", "id"),
                getItemField(fullOutfit?.top, "rfid", "itemRfid", "productRfid", "id"),
                getItemField(fullOutfit?.bottom, "rfid", "itemRfid", "productRfid", "id"),
                getItemField(fullOutfit?.shoes, "rfid", "itemRfid", "productRfid", "id"),
                getItemField(fullOutfit?.outerwear, "rfid", "itemRfid", "productRfid", "id")
            ]
                .filter(Boolean)
                .map(normalizeRfid)
        );

        const seenSuggestions = new Set();

        return suggestions.filter(item => {
            const rfid = normalizeRfid(getSuggestionRfid(item));

            if (!rfid) {
                return false;
            }

            if (currentRfids.has(rfid)) {
                return false;
            }

            if (seenSuggestions.has(rfid)) {
                return false;
            }

            seenSuggestions.add(rfid);

            return isSuggestionAvailable(item);
        });
    }

    function sortSuggestionsForSwap(suggestions, swapCategory = "") {
        if (!Array.isArray(suggestions) || suggestions.length === 0) {
            return [];
        }

        const normalizedSwap = normalizeCategoryName(swapCategory);

        return [...suggestions].sort((a, b) => {
            const aCategory = getSuggestionCategory(a);
            const bCategory = getSuggestionCategory(b);

            const aCategoryBoost = normalizedSwap && aCategory === normalizedSwap ? 1000 : 0;
            const bCategoryBoost = normalizedSwap && bCategory === normalizedSwap ? 1000 : 0;

            const aPreference = getPreferenceScore(a);
            const bPreference = getPreferenceScore(b);

            const aMatch = safeNumber(getItemField(a, "matchScore")) || 0;
            const bMatch = safeNumber(getItemField(b, "matchScore")) || 0;

            const aBudget = safeNumber(getItemField(a, "budgetMatch")) || 0;
            const bBudget = safeNumber(getItemField(b, "budgetMatch")) || 0;

            const aScore = aCategoryBoost + aPreference * 1.4 + aMatch + aBudget * 0.35;
            const bScore = bCategoryBoost + bPreference * 1.4 + bMatch + bBudget * 0.35;

            return bScore - aScore;
        });
    }

    function renderSuggestions(suggestions, fullOutfit = null, swapCategory = "") {
        const container = document.getElementById("suggestionsRow");
        if (!container) return;

        updateSuggestionsHeading(swapCategory);

        const activeFullOutfit = fullOutfit || getCurrentFullOutfit();
        const filteredSuggestions = filterOutCurrentLookItems(suggestions, activeFullOutfit);
        const orderedSuggestions = sortSuggestionsForSwap(filteredSuggestions, swapCategory);

        if (!Array.isArray(orderedSuggestions) || orderedSuggestions.length === 0) {
            const copy = normalizeCategoryName(swapCategory);

            let message = "No available curated pieces are available right now.";
            if (copy === "tops") message = "No available premium top alternatives are available right now.";
            if (copy === "bottoms") message = "No available premium bottom alternatives are available right now.";
            if (copy === "shoes") message = "No available premium shoe alternatives are available right now.";
            if (copy === "outerwear") message = "No available premium outerwear alternatives are available right now.";

            container.innerHTML = `
                <div class="col-12">
                    <div class="result-empty-state">${escapeHtml(message)}</div>
                </div>
            `;
            return;
        }

        container.innerHTML = orderedSuggestions.map((item, index) => {
            const rfid = getSuggestionRfid(item);
            const retailerName = escapeHtml(getItemField(item, "retailerName", "retailer") || "Retailer");
            const itemName = escapeHtml(getItemField(item, "itemName", "name") || "Unnamed Item");
            const brand = escapeHtml(getItemField(item, "brand") || retailerName);
            const categoryRaw = getItemField(item, "category") || "Item";
            const category = escapeHtml(categoryRaw);
            const color = escapeHtml(getItemField(item, "color") || "Neutral");

            const price = Number(getItemField(item, "price") || 0);
            const matchScore = Number(getItemField(item, "matchScore") || 88);
            const styleMatch = Number(getItemField(item, "styleMatch") || 0);
            const colorMatch = Number(getItemField(item, "colorMatch") || 0);
            const occasionMatch = Number(getItemField(item, "occasionMatch") || 0);

            const preferenceMatch = getPreferenceScore(item);
            const budgetMatch = Number(getItemField(item, "budgetMatch") || 0);
            const sizeMatch = Number(getItemField(item, "sizeMatch") || 0);
            const fitMatch = Number(getItemField(item, "fitMatch") || 0);
            const materialMatch = Number(getItemField(item, "materialMatch") || 0);

            const preferenceNote =
                getItemField(item, "preferenceNote", "matchedPreferenceReason") ||
                getPreferenceMatchDetails(item)
                    .filter(detail => detail.type === "positive")
                    .map(detail => detail.text)
                    .slice(0, 2)
                    .join(" ");

            const reason = buildWhyThisMatchesYou(item, activeFullOutfit, swapCategory);

            const imageUrl = safeImageUrl(
                getItemField(item, "imageUrl"),
                "https://placehold.co/400x200?text=Outfit"
            );

            const alreadySaved = !!rfid && savedRfids.has(rfid);
            const canAddToLook = !!getCurrentFullOutfit() && !!getLookSlotForSuggestion(item, swapCategory);
            const raw = encodeURIComponent(JSON.stringify(item));

            return `
                <div class="col-12 col-md-6 col-xl-4 d-flex">
                    <div
                        class="suggestion-card h-100 w-100"
                        data-rfid="${escapeHtml(rfid || `suggestion-${index}`)}"
                        data-category="${escapeHtml(categoryRaw)}"
                    >
                        <img
                            src="${imageUrl}"
                            alt="${itemName}"
                            class="suggestion-img"
                            onerror="this.src='https://placehold.co/400x200?text=Outfit';"
                        />

                        <div class="suggestion-body d-flex flex-column h-100">
                            <div class="suggestion-retailer">${retailerName}</div>
                            <div class="suggestion-name">${itemName}</div>

                            <div class="small text-muted mb-1">${brand}</div>
                            <div class="small text-muted mb-2">${category} • ${color}</div>

                            <div class="suggestion-footer mb-2">
                                <span class="suggestion-badge">${category}</span>
                                <span class="suggestion-price">${formatPrice(price)}</span>
                            </div>

                            <div class="suggestion-pill-row mb-2">
                                <span class="suggestion-match">${matchScore}% Match</span>

                                ${
                                    preferenceMatch > 0
                                        ? `<span class="suggestion-match preference">${preferenceMatch}% Preference</span>`
                                        : `<span class="suggestion-match preference muted">Preference</span>`
                                }

                                <span class="suggestion-match available">Available</span>
                            </div>

                            ${renderPreferenceLabelPills(item)}

                            <div class="small text-muted my-2" style="line-height:1.5;">
                                ${escapeHtml(reason)}
                            </div>

                            ${
                                preferenceNote
                                    ? `
                                        <div class="preference-reason-box mb-3">
                                            <div class="preference-reason-label">Why this matches you</div>
                                            <div class="preference-reason-copy">${escapeHtml(preferenceNote)}</div>
                                        </div>
                                    `
                                    : ""
                            }

                            <div class="mb-3 mt-auto">
                                <div class="small fw-semibold text-muted mb-2">Score Breakdown</div>

                                <div class="d-flex justify-content-between align-items-center small mb-1">
                                    <span>Style Match</span>
                                    <span class="fw-bold">${styleMatch}%</span>
                                </div>
                                <div class="progress mb-2" style="height: 7px;">
                                    <div class="progress-bar bg-dark" style="width: ${Math.max(0, Math.min(100, styleMatch))}%;"></div>
                                </div>

                                <div class="d-flex justify-content-between align-items-center small mb-1">
                                    <span>Color Match</span>
                                    <span class="fw-bold">${colorMatch}%</span>
                                </div>
                                <div class="progress mb-2" style="height: 7px;">
                                    <div class="progress-bar bg-secondary" style="width: ${Math.max(0, Math.min(100, colorMatch))}%;"></div>
                                </div>

                                <div class="d-flex justify-content-between align-items-center small mb-1">
                                    <span>Occasion Match</span>
                                    <span class="fw-bold">${occasionMatch}%</span>
                                </div>
                                <div class="progress mb-2" style="height: 7px;">
                                    <div class="progress-bar bg-danger" style="width: ${Math.max(0, Math.min(100, occasionMatch))}%;"></div>
                                </div>

                                ${
                                    preferenceMatch > 0
                                        ? `
                                            <div class="d-flex justify-content-between align-items-center small mb-1">
                                                <span>Preference Match</span>
                                                <span class="fw-bold">${preferenceMatch}%</span>
                                            </div>
                                            <div class="progress mb-2" style="height: 7px;">
                                                <div class="progress-bar bg-success" style="width: ${Math.max(0, Math.min(100, preferenceMatch))}%;"></div>
                                            </div>
                                        `
                                        : ""
                                }

                                ${
                                    budgetMatch > 0
                                        ? `
                                            <div class="d-flex justify-content-between align-items-center small mb-1">
                                                <span>Budget Match</span>
                                                <span class="fw-bold">${budgetMatch}%</span>
                                            </div>
                                            <div class="progress mb-2" style="height: 7px;">
                                                <div class="progress-bar bg-info" style="width: ${Math.max(0, Math.min(100, budgetMatch))}%;"></div>
                                            </div>
                                        `
                                        : ""
                                }

                                ${
                                    sizeMatch > 0 || fitMatch > 0 || materialMatch > 0
                                        ? `
                                            <div class="preference-mini-grid mt-2">
                                                ${sizeMatch > 0 ? `<span class="preference-mini-pill">Size ${sizeMatch}%</span>` : ""}
                                                ${fitMatch > 0 ? `<span class="preference-mini-pill">Fit ${fitMatch}%</span>` : ""}
                                                ${materialMatch > 0 ? `<span class="preference-mini-pill">Material ${materialMatch}%</span>` : ""}
                                            </div>
                                        `
                                        : ""
                                }
                            </div>

                            <div class="suggestion-button-actions">
                                <button
                                    class="btn alt-save-btn add-suggestion-to-look-btn ${canAddToLook ? "btn-dark" : "btn-outline-dark"}"
                                    type="button"
                                    data-item="${raw}"
                                    ${canAddToLook ? "" : "disabled"}
                                >
                                    ${canAddToLook ? "Add to Current Look" : "Create Look First"}
                                </button>

                                <button
                                    class="btn alt-save-btn save-suggestion-btn ${alreadySaved ? "btn-dark" : "btn-outline-dark"}"
                                    type="button"
                                    data-item="${raw}"
                                    data-rfid="${escapeHtml(rfid || "")}"
                                    data-index="${index}"
                                    ${alreadySaved ? "disabled" : ""}
                                >
                                    ${alreadySaved ? "Saved ✓" : "Save Suggested Piece"}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }).join("");

        container.querySelectorAll(".save-suggestion-btn").forEach(button => {
            button.addEventListener("click", async () => {
                try {
                    const parsed = JSON.parse(decodeURIComponent(button.dataset.item || ""));
                    await saveSuggestedItem(parsed, button);
                } catch (error) {
                    console.error("Suggestion parse error:", error);
                    showToast("Unable to save suggestion.", "error");
                }
            });
        });

        container.querySelectorAll(".add-suggestion-to-look-btn").forEach(button => {
            button.addEventListener("click", async () => {
                try {
                    const parsed = JSON.parse(decodeURIComponent(button.dataset.item || ""));
                    await addSuggestionToCurrentLook(parsed, button);
                } catch (error) {
                    console.error("Add suggestion parse error:", error);
                    showToast("Unable to add suggestion to look.", "error");
                }
            });
        });
    }

   function renderScanResult(item, selectedVibe, options = {}) {
        lastScannedItem = item || null;
        currentLoadedItem = item || null;
        currentRfid = getItemField(item, "rfid", "itemRfid", "productRfid", "id") || currentRfid;

        window.currentLookVariation = 0;
        window.lastSwapCategory = "";
        window.savedCurrentLookSignature = "";
        window.currentLookState = {
            topRfid: "",
            bottomRfid: "",
            shoesRfid: "",
            outerwearRfid: ""
        };
        resetLookHistory();

        const rfid = getItemField(item, "rfid") || "RFID";
        const name = getItemField(item, "name", "itemName") || "Scanned Item";
        const brand = getItemField(item, "brand") || "Brand";
        const category = getItemField(item, "category") || "Category";
        const color = getItemField(item, "color") || "Color";
        const retailer = getItemField(item, "retailer", "retailerName") || "Unknown Retailer";
        const price = getItemField(item, "price");
        const advice = getItemField(item, "stylingAdvice") || generateStylingAdvice(item, selectedVibe);
        const why = getItemField(item, "whyItWorks") || generateWhyItWorks(item, selectedVibe);
        const score = getItemField(item, "matchScore") || 92;
        const imageUrl = safeImageUrl(getItemField(item, "imageUrl", "image_url", "image", "photoUrl", "productImageUrl"));

        document.getElementById("scanSuccessTitle").textContent = `${rfid} scanned successfully`;
        document.getElementById("scanSuccessMeta").textContent = "1 item analyzed • Ready for styling";
        document.getElementById("resultItemName").textContent = name;
        document.getElementById("resultMeta").textContent = `${brand} • ${category} • ${color}`;
        document.getElementById("resultRetailer").textContent = retailer;
        document.getElementById("resultPrice").textContent =
            typeof price === "number" || !Number.isNaN(Number(price))
                ? `$${Number(price).toFixed(2)}`
                : "$0.00";
        document.getElementById("resultVibe").textContent = selectedVibe || "Casual";

        const appliedPreferences = getLocalPreferences();
        const hasPreferencesApplied = Object.values(appliedPreferences).some(value => {
            return String(value ?? "").trim() !== "";
        });

        document.getElementById("resultAdvice").textContent = hasPreferencesApplied
        ? `${advice} Preferences applied from your styling profile.`
        : advice;

         document.getElementById("resultWhy").textContent = why;
        document.getElementById("resultScore").textContent = `${score}%`;

        if (isCurrentItemSaved(item)) {
            setSaveButtonSaved();
        } else {
            setSaveButtonDefault(false);
        }

        const resultImage = document.getElementById("resultImage");
        resultImage.src = imageUrl;
        resultImage.alt = name;
        resultImage.onerror = function () {
            this.src = "https://placehold.co/500x620?text=Scanned+Item";
        };

        renderAnchorPiece(item);

        const initialLookPayload = {
            variation: 0,
            fullOutfit: item?.fullOutfit || null,
            suggestions: item?.suggestions || [],

            stylingNote: item?.stylingNote || item?.fullOutfit?.stylingNote || "",
            occasionNote: item?.occasionNote || item?.fullOutfit?.occasionNote || "",
            seasonNote: item?.seasonNote || item?.fullOutfit?.seasonNote || "",
            colorNote: item?.colorNote || item?.fullOutfit?.colorNote || "",
            fitNote: item?.fitNote || item?.fullOutfit?.fitNote || "",
            materialNote: item?.materialNote || item?.fullOutfit?.materialNote || "",
            preferenceNote: item?.preferenceNote || item?.fullOutfit?.preferenceNote || ""
        };

        if (initialLookPayload.fullOutfit || (Array.isArray(initialLookPayload.suggestions) && initialLookPayload.suggestions.length)) {
            pushLookToHistory(initialLookPayload, 0);
        } else {
            renderLookCarousel();
            renderFullOutfit(null);
            renderSuggestions(item?.suggestions || [], item?.fullOutfit || null, "");
        }

        showScanResultSection();

        if (!options.suppressAutoScroll) {
            scrollToScannedItemResult();
        }

        }

    function resetScanExperience() {
        currentRfid = "";
        currentLoadedItem = null;
        lastScannedItem = null;
        window.currentLookVariation = 0;
        window.lastSwapCategory = "";
        window.savedCurrentLookSignature = "";
        window.activeSavedLookId = "";
        window.currentLookState = {
            topRfid: "",
            bottomRfid: "",
            shoesRfid: "",
            outerwearRfid: ""
        };
        resetLookHistory();

        const scanSuccessTitle = document.getElementById("scanSuccessTitle");
        const scanSuccessMeta = document.getElementById("scanSuccessMeta");
        const resultItemName = document.getElementById("resultItemName");
        const resultMeta = document.getElementById("resultMeta");
        const resultRetailer = document.getElementById("resultRetailer");
        const resultPrice = document.getElementById("resultPrice");
        const resultVibe = document.getElementById("resultVibe");
        const resultAdvice = document.getElementById("resultAdvice");
        const resultWhy = document.getElementById("resultWhy");
        const resultScore = document.getElementById("resultScore");
        const resultImage = document.getElementById("resultImage");
        const suggestionsRow = document.getElementById("suggestionsRow");
        const fullOutfitContainer = document.getElementById("fullOutfitContainer");
        const lookCarouselContainer = document.getElementById("lookCarouselContainer");

        if (scanSuccessTitle) scanSuccessTitle.textContent = "RFID scanned successfully";
        if (scanSuccessMeta) scanSuccessMeta.textContent = "1 item analyzed • Ready for styling";
        if (resultItemName) resultItemName.textContent = "Item Name";
        if (resultMeta) resultMeta.textContent = "Brand • Category • Color";
        if (resultRetailer) resultRetailer.textContent = "Retailer";
        if (resultPrice) resultPrice.textContent = "$0.00";
        if (resultVibe) resultVibe.textContent = "Casual";
        if (resultAdvice) resultAdvice.textContent = "This item works well for a casual look.";
        if (resultWhy) resultWhy.textContent = "The fit, tone, and versatility make it easy to style.";
        if (resultScore) resultScore.textContent = "92%";

        if (resultImage) {
            resultImage.src = "https://placehold.co/500x620?text=Scanned+Item";
            resultImage.alt = "Scanned item";
        }

        if (lookCarouselContainer) lookCarouselContainer.innerHTML = "";
        if (fullOutfitContainer) fullOutfitContainer.innerHTML = "";
        clearAnchorPiece();

        updateSuggestionsHeading("");

        if (suggestionsRow) {
            suggestionsRow.innerHTML = `
                <div class="col-12">
                    <div class="result-empty-state">
                        Create a full look to see curated premium alternatives.
                    </div>
                </div>
            `;
        }

        setSaveButtonDefault(true);
        hideScanResultSection();
        hideLoadingState();
    }

    function getMerchantItemId(item) {
        return getItemField(item, "id", "inventoryId", "productId", "rfid");
    }

    function getMerchantItemStock(item) {
        const candidates = [
            item?.stockQuantity,
            item?.quantity,
            item?.stock,
            item?.inventoryCount
        ];
        for (const candidate of candidates) {
            const parsed = Number(candidate);
            if (Number.isFinite(parsed)) return parsed;
        }
        return 0;
    }

    function isMerchantItemActive(item) {
        if (typeof item?.active === "boolean") return item.active;
        if (typeof item?.isActive === "boolean") return item.isActive;
        if (typeof item?.enabled === "boolean") return item.enabled;
        return true;
    }

    function isMerchantItemSynced(item) {
        if (typeof item?.synced === "boolean") return item.synced;
        if (typeof item?.isSynced === "boolean") return item.isSynced;
        return true;
    }

    function getMerchantStockInputValue(itemId) {
        const input = document.querySelector(`[data-stock-input-id="${CSS.escape(String(itemId))}"]`);
        return input ? Number(input.value) : NaN;
    }

    function getMerchantOrderItems(order) {
    if (!order || typeof order !== "object") {
        return [];
    }

    if (Array.isArray(order.items)) return order.items;
    if (Array.isArray(order.orderItems)) return order.orderItems;
    if (Array.isArray(order.lineItems)) return order.lineItems;

    return [];
}

function getMerchantOrderNumber(order) {
    return (
        order?.orderNumber ||
        order?.receiptNumber ||
        order?.id ||
        order?.orderId ||
        "Order"
    );
}

function getMerchantOrderDate(order) {
    return (
        order?.createdAt ||
        order?.orderedAt ||
        order?.checkoutAt ||
        order?.completedAt ||
        order?.date ||
        ""
    );
}

function getMerchantOrderStatus(order) {
    return (
        order?.status ||
        order?.orderStatus ||
        "COMPLETED"
    );
}

function formatMerchantSalesDate(value) {
    if (!value) return "Recently";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return String(value);
    }

    return date.toLocaleString();
}

const MERCHANT_SALES_DASHBOARD_API = "/api/v1/merchant/inventory/sales";

function getDashboardNumber(dashboard, ...keys) {
    for (const key of keys) {
        const value = dashboard?.[key];

        if (value !== undefined && value !== null && value !== "") {
            return safeNumber(value);
        }
    }

    return 0;
}

function getDashboardText(dashboard, ...keys) {
    for (const key of keys) {
        const value = dashboard?.[key];

        if (value !== undefined && value !== null && String(value).trim()) {
            return String(value).trim();
        }
    }

    return "";
}

function getDashboardOrders(dashboard) {
    if (Array.isArray(dashboard?.recentOrders)) {
        return dashboard.recentOrders;
    }

    if (Array.isArray(dashboard?.orders)) {
        return dashboard.orders;
    }

    if (Array.isArray(dashboard)) {
        return dashboard;
    }

    return [];
}

function renderMiniMerchantChart(title, chart, variant = "revenue") {
    const labels = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
    const points = Array.isArray(chart) ? chart : [];

    const valueByLabel = new Map();

    points.forEach(point => {
        const label = String(point.label || point.day || point.name || "").trim();
        const value = safeNumber(point.value ?? point.count ?? point.total ?? 0);

        if (label) {
            valueByLabel.set(label.slice(0, 3), value);
        }
    });

    const safeValues = labels.map(label => {
        return safeNumber(
            valueByLabel.get(label) ??
            valueByLabel.get(label.toLowerCase()) ??
            valueByLabel.get(label.toUpperCase()) ??
            0
        );
    });

    const maxValue = Math.max(...safeValues, 1);

    return `
        <div class="merchant-sales-summary-card chart-card">
            <div class="merchant-sales-summary-label">${escapeHtml(title)}</div>

            <div class="merchant-mini-chart ${escapeHtml(variant)}">
                ${safeValues.map((value, index) => {
                    const height = Math.max(7, Math.round((value / maxValue) * 58));
                    const label = labels[index];

                    return `
                        <div class="merchant-mini-chart-bar-wrap" title="${escapeHtml(label)}: ${value}">
                            <div class="merchant-mini-chart-bar" style="height: ${height}px;"></div>
                            <div class="merchant-mini-chart-label">${escapeHtml(label)}</div>
                        </div>
                    `;
                }).join("")}
            </div>
        </div>
    `;
}

function renderLowStockPriorityItems(items) {
    const safeItems = Array.isArray(items) ? items : [];

    if (!safeItems.length) {
        return "";
    }

    return `
        <div class="merchant-sales-summary-card low-stock-priority">
            <div class="merchant-sales-summary-label">Low-Stock Priority</div>

            <div class="merchant-low-stock-list">
                ${safeItems.slice(0, 5).map(item => {
                    const name = item.itemName || item.name || "Inventory Item";
                    const rfid = item.rfid || "";
                    const stock = safeNumber(item.stockQuantity ?? item.currentStock);
                    const reorder = safeNumber(item.suggestedReorderQuantity);
                    const alert = item.inventoryAlert || item.alert || "Needs reorder.";

                    return `
                        <div class="merchant-low-stock-row">
                            <div class="merchant-low-stock-main">
                                <div class="merchant-low-stock-name">${escapeHtml(name)}</div>
                                <div class="merchant-low-stock-meta">
                                    ${escapeHtml(rfid)} • Stock ${stock}
                                </div>
                                <div class="merchant-low-stock-alert">
                                    ${escapeHtml(alert)}
                                </div>
                            </div>

                            <div class="merchant-low-stock-reorder">
                                +${reorder}
                            </div>
                        </div>
                    `;
                }).join("")}
            </div>
        </div>
    `;
}

function renderMerchantSalesSummary(dashboardOrOrders) {
    const container = document.getElementById("merchantSalesSummary");

    if (!container) {
        return;
    }

    const isDashboard =
        dashboardOrOrders &&
        typeof dashboardOrOrders === "object" &&
        !Array.isArray(dashboardOrOrders) &&
        (
            dashboardOrOrders.summary ||
            dashboardOrOrders.recentOrders ||
            dashboardOrOrders.revenueChart ||
            dashboardOrOrders.scanChart ||
            dashboardOrOrders.saveChart
        );

    if (!isDashboard) {
        const safeOrders = Array.isArray(dashboardOrOrders) ? dashboardOrOrders : [];

        const fallbackDashboard = {
            recentOrders: safeOrders,
            revenue: safeOrders.reduce((sum, order) => sum + safeNumber(order?.total), 0),
            orderCount: safeOrders.length,
            checkoutCount: safeOrders.length,
            itemCount: safeOrders.reduce((sum, order) => {
                const items = getMerchantOrderItems(order);
                const declaredCount = Number(order?.itemCount);

                if (Number.isFinite(declaredCount) && declaredCount > 0) {
                    return sum + declaredCount;
                }

                return sum + items.reduce((itemSum, item) => {
                    return itemSum + Math.max(1, safeNumber(item?.quantity || item?.qty || 1));
                }, 0);
            }, 0)
        };

        fallbackDashboard.averageOrderValue = fallbackDashboard.orderCount > 0
            ? fallbackDashboard.revenue / fallbackDashboard.orderCount
            : 0;

        renderMerchantSalesSummary(fallbackDashboard);
        return;
    }

    const dashboard = dashboardOrOrders || {};
    const summary = dashboard.summary || {};

    const revenue = getDashboardNumber(dashboard, "revenue", "totalRevenue") || safeNumber(summary.totalRevenue);
    const subtotal = getDashboardNumber(dashboard, "subtotal");
    const tax = getDashboardNumber(dashboard, "tax");

    const orderCount =
        getDashboardNumber(dashboard, "orderCount", "checkoutCount") ||
        safeNumber(summary.totalOrders);

    const itemCount =
        getDashboardNumber(dashboard, "itemCount", "totalItemsSold") ||
        safeNumber(summary.totalItemsSold);

    const averageOrderValue =
        getDashboardNumber(dashboard, "averageOrderValue") ||
        safeNumber(summary.averageOrderValue);

    const scanCount = getDashboardNumber(dashboard, "scanCount");
    const saveCount = getDashboardNumber(dashboard, "saveCount");
    const lowStockCount = getDashboardNumber(dashboard, "lowStockCount");
    const outOfStockCount = getDashboardNumber(dashboard, "outOfStockCount");
    const inventoryValueAtRisk = getDashboardNumber(dashboard, "inventoryValueAtRisk");

    const topSellingItem = getDashboardText(dashboard, "topSellingItem") || "No sales yet";
    const topSellingQuantity = getDashboardNumber(dashboard, "topSellingQuantity");

    const topScannedItem = getDashboardText(dashboard, "topScannedItem") || "No scans yet";
    const topScannedCount = getDashboardNumber(dashboard, "topScannedCount");

    const topSavedItem = getDashboardText(dashboard, "topSavedItem") || "No saves yet";
    const topSavedCount = getDashboardNumber(dashboard, "topSavedCount");

    container.innerHTML = `
        <div class="merchant-sales-summary-card revenue">
            <div class="merchant-sales-summary-label">Revenue</div>
            <div class="merchant-sales-summary-value">${formatPrice(revenue)}</div>
            <div class="merchant-sales-summary-sub">
                Subtotal ${formatPrice(subtotal)} • Tax ${formatPrice(tax)}
            </div>
        </div>

        <div class="merchant-sales-summary-card">
            <div class="merchant-sales-summary-label">Orders</div>
            <div class="merchant-sales-summary-value">${orderCount}</div>
            <div class="merchant-sales-summary-sub">Completed store checkouts</div>
        </div>

        <div class="merchant-sales-summary-card">
            <div class="merchant-sales-summary-label">Items Sold</div>
            <div class="merchant-sales-summary-value">${itemCount}</div>
            <div class="merchant-sales-summary-sub">Across recent orders</div>
        </div>

        <div class="merchant-sales-summary-card">
            <div class="merchant-sales-summary-label">Avg Order</div>
            <div class="merchant-sales-summary-value">${formatPrice(averageOrderValue)}</div>
            <div class="merchant-sales-summary-sub">Average checkout value</div>
        </div>

        <div class="merchant-sales-summary-card highlight">
            <div class="merchant-sales-summary-label">Top-Selling Item</div>
            <div class="merchant-sales-summary-value small-value">
                ${escapeHtml(topSellingItem)}
            </div>
            <div class="merchant-sales-summary-sub">
                ${topSellingQuantity > 0 ? `${topSellingQuantity} sold` : "Based on completed orders"}
            </div>
        </div>

        <div class="merchant-sales-summary-card">
            <div class="merchant-sales-summary-label">Scans</div>
            <div class="merchant-sales-summary-value">${scanCount}</div>
            <div class="merchant-sales-summary-sub">Store scan history</div>
        </div>

        <div class="merchant-sales-summary-card highlight">
            <div class="merchant-sales-summary-label">Top-Scanned Item</div>
            <div class="merchant-sales-summary-value small-value">
                ${escapeHtml(topScannedItem)}
            </div>
            <div class="merchant-sales-summary-sub">
                ${topScannedCount > 0 ? `${topScannedCount} scan${topScannedCount === 1 ? "" : "s"}` : "No scan activity yet"}
            </div>
        </div>

        <div class="merchant-sales-summary-card">
            <div class="merchant-sales-summary-label">Saves</div>
            <div class="merchant-sales-summary-value">${saveCount}</div>
            <div class="merchant-sales-summary-sub">Items saved to bag</div>
        </div>

        <div class="merchant-sales-summary-card highlight">
            <div class="merchant-sales-summary-label">Top-Saved Item</div>
            <div class="merchant-sales-summary-value small-value">
                ${escapeHtml(topSavedItem)}
            </div>
            <div class="merchant-sales-summary-sub">
                ${topSavedCount > 0 ? `${topSavedCount} save${topSavedCount === 1 ? "" : "s"}` : "No saved items yet"}
            </div>
        </div>

        <div class="merchant-sales-summary-card warning">
            <div class="merchant-sales-summary-label">Low Stock</div>
            <div class="merchant-sales-summary-value">${lowStockCount}</div>
            <div class="merchant-sales-summary-sub">Items at reorder threshold</div>
        </div>

        <div class="merchant-sales-summary-card danger">
            <div class="merchant-sales-summary-label">Out of Stock</div>
            <div class="merchant-sales-summary-value">${outOfStockCount}</div>
            <div class="merchant-sales-summary-sub">Unavailable units needing reorder</div>
        </div>

        <div class="merchant-sales-summary-card">
            <div class="merchant-sales-summary-label">Value At Risk</div>
            <div class="merchant-sales-summary-value">${formatPrice(inventoryValueAtRisk)}</div>
            <div class="merchant-sales-summary-sub">Estimated reorder exposure</div>
        </div>

        ${renderMiniMerchantChart("Revenue Trend", dashboard.revenueChart, "revenue")}
        ${renderMiniMerchantChart("Scan Trend", dashboard.scanChart, "scan")}
        ${renderMiniMerchantChart("Save Trend", dashboard.saveChart, "save")}
        ${renderLowStockPriorityItems(dashboard.lowStockPriorityItems)}
    `;
}

function openMerchantSaleInOrderHistory(orderNumber) {
    const orderHistoryBtn = document.getElementById("viewOrderHistoryBtn");

    if (!orderNumber) {
        orderHistoryBtn?.click();
        return;
    }

    sessionStorage.setItem("merchantOrderFocus", orderNumber);

    orderHistoryBtn?.click();
}

    function renderMerchantInventorySummary(result) {
    const container = document.getElementById("merchantInventorySummary");

    if (!container) {
        return;
    }

    const items = Array.isArray(result?.items) ? result.items : [];

    if (!items.length) {
        container.innerHTML = "";
        return;
    }

    const totalItems = Number(result?.totalItems || items.length);

    const totalStock = items.reduce((sum, item) => {
        return sum + getMerchantItemStock(item);
    }, 0);

    const lowStockItems = items.filter(item => {
        const stock = getMerchantItemStock(item);
        return stock > 0 && stock <= 3;
    });

    const outOfStockItems = items.filter(item => {
        const stock = getMerchantItemStock(item);
        return stock <= 0;
    });

    const inventoryValue = items.reduce((sum, item) => {
        const stock = getMerchantItemStock(item);
        const price = safeNumber(item?.price);
        return sum + stock * price;
    }, 0);

    container.innerHTML = `
        <div class="merchant-summary-card success">
            <div class="merchant-summary-label">Total Items</div>
            <div class="merchant-summary-value">${totalItems}</div>
           <div class="merchant-summary-sub">${items.length} shown • ${totalStock} units in current view</div>
        </div>

        <div class="merchant-summary-card warning">
            <div class="merchant-summary-label">Low Stock</div>
            <div class="merchant-summary-value">${lowStockItems.length}</div>
            <div class="merchant-summary-sub">Items at 1–3 units</div>
        </div>

        <div class="merchant-summary-card danger">
            <div class="merchant-summary-label">Out of Stock</div>
            <div class="merchant-summary-value">${outOfStockItems.length}</div>
            <div class="merchant-summary-sub">Items requiring reorder</div>
        </div>

        <div class="merchant-summary-card">
            <div class="merchant-summary-label">Inventory Value</div>
            <div class="merchant-summary-value">${formatPrice(inventoryValue)}</div>
            <div class="merchant-summary-sub">Current page estimated value</div>
        </div>
    `;
}

    function renderMerchantInventory(result) {
    const container = document.getElementById("merchantInventoryResult");

    if (!container) {
        return;
    }

    const items = Array.isArray(result?.items) ? result.items : [];

    if (!items.length) {
        container.innerHTML = `
            <div class="merchant-empty-admin">
                No inventory items matched this filter.
            </div>
        `;
        return;
    }

    container.innerHTML = `
        <div class="merchant-inventory-grid">
            ${items.map(item => {
                const itemId = getMerchantItemId(item);
                const stockQuantity = getMerchantItemStock(item);
                const isActive = isMerchantItemActive(item);
                const isSynced = isMerchantItemSynced(item);
                const isLowStock = item.lowStock === true || (stockQuantity > 0 && stockQuantity <= 3);
                const isOutOfStock = item.outOfStock === true || stockQuantity <= 0;

                const suggestedReorderQuantity = Number(
                    item.suggestedReorderQuantity || Math.max(0, 12 - stockQuantity)
                );

                const inventoryAlert = item.inventoryAlert || (
                    isOutOfStock
                        ? "Out of stock — reorder immediately."
                        : isLowStock
                            ? `Low stock — suggested reorder: ${suggestedReorderQuantity} units.`
                            : "Stock level is healthy."
                );

                const imageUrl = safeImageUrl(
                    getItemField(
                        item,
                        "imageUrl",
                        "image_url",
                        "image",
                        "photoUrl",
                        "productImageUrl"
                    ),
                    "https://placehold.co/400x220?text=Inventory"
                );

                const itemName = item.itemName || item.name || "Unnamed Item";
                const retailerName = item.retailerName || item.retailer || "Retailer";
                const brand = item.brand || "Brand";
                const category = item.category || "Category";
                const color = item.color || "Color";
                const storeName = item.storeName || item.storeCode || "Store";
                const rfid = item.rfid || "RFID";

                return `
                    <div class="merchant-inventory-card ${!isActive ? "is-inactive" : ""} ${isLowStock ? "is-low-stock" : ""}">
                        <img
                            src="${imageUrl}"
                            alt="${escapeHtml(itemName)}"
                            class="merchant-inventory-image"
                            onerror="this.src='https://placehold.co/400x220?text=Inventory';"
                        />

                        <div class="merchant-inventory-body">
                            <div class="merchant-inventory-topline">
                                <div class="merchant-inventory-retailer">
                                    ${escapeHtml(retailerName)}
                                </div>

                                <div class="merchant-inventory-status-group">
                                    <span class="merchant-status-pill ${isActive ? "active" : "inactive"}">
                                        ${isActive ? "Active" : "Inactive"}
                                    </span>

                                    ${isOutOfStock ? `<span class="merchant-status-pill danger">Out of Stock</span>` : ""}

                                    ${
                                        !isOutOfStock && isLowStock
                                            ? `<span class="merchant-status-pill low-stock">Needs Reorder</span>`
                                            : ""
                                    }

                                    ${isSynced ? `<span class="merchant-status-pill synced">Synced</span>` : ""}
                                </div>
                            </div>

                            <div class="merchant-inventory-name">
                                ${escapeHtml(itemName)}
                            </div>

                            <div class="merchant-inventory-meta">
                                ${escapeHtml(brand)} •
                                ${escapeHtml(category)} •
                                ${escapeHtml(color)}
                            </div>

                            <div class="merchant-inventory-identity">
                                <div class="merchant-inventory-row">
                                    <span class="merchant-inventory-row-label">RFID</span>
                                    <span class="merchant-inventory-row-value">${escapeHtml(rfid)}</span>
                                </div>

                                <div class="merchant-inventory-row">
                                    <span class="merchant-inventory-row-label">Store</span>
                                    <span class="merchant-inventory-row-value">${escapeHtml(storeName)}</span>
                                </div>

                                <div class="merchant-inventory-row">
                                    <span class="merchant-inventory-row-label">Price</span>
                                    <span class="merchant-inventory-row-value">${formatPrice(item.price)}</span>
                                </div>
                            </div>

                            <div class="merchant-inventory-footer">
                                <span class="merchant-inventory-pill">${escapeHtml(category)}</span>
                                <span class="merchant-inventory-stock">Stock ${stockQuantity}</span>
                            </div>

                            <div class="merchant-inventory-row mt-2">
                                <span class="merchant-inventory-row-label">Reorder Alert</span>
                                <span class="merchant-inventory-row-value">${escapeHtml(inventoryAlert)}</span>
                            </div>

                            ${
                                isOutOfStock || isLowStock
                                    ? `
                                        <div class="merchant-inventory-row mt-2">
                                            <span class="merchant-inventory-row-label">Suggested Reorder</span>
                                            <span class="merchant-inventory-row-value">${suggestedReorderQuantity} units</span>
                                        </div>
                                    `
                                    : ""
                            }

                            <div class="merchant-stock-editor">
                                <div class="merchant-stock-input-group">
                                    <label
                                        class="merchant-stock-label"
                                        for="stock-input-${escapeHtml(itemId)}"
                                    >
                                        Update Stock
                                    </label>

                                    <input
                                        id="stock-input-${escapeHtml(itemId)}"
                                        type="number"
                                        min="0"
                                        step="1"
                                        value="${stockQuantity}"
                                        class="merchant-stock-input"
                                        data-stock-input-id="${escapeHtml(itemId)}"
                                    />
                                </div>

                                <button
                                    type="button"
                                    class="merchant-inline-btn primary merchant-stock-save-btn"
                                    data-item-id="${escapeHtml(itemId)}"
                                >
                                    Save Stock
                                </button>
                            </div>

                           <div class="merchant-inline-actions">
                               <button
                                   type="button"
                                   class="merchant-inline-btn primary merchant-edit-btn"
                                   data-item="${encodeURIComponent(JSON.stringify(item))}"
                               >
                                   Edit Item
                               </button>

                               <button
                                   type="button"
                                   class="merchant-inline-btn ${isActive ? "danger" : "success"} merchant-toggle-btn"
                                   data-item-id="${escapeHtml(itemId)}"
                                   data-active="${isActive ? "true" : "false"}"
                               >
                                   ${isActive ? "Deactivate" : "Reactivate"}
                               </button>

                               <button
                                   type="button"
                                   class="merchant-inline-btn warning merchant-resync-btn"
                                   data-item-id="${escapeHtml(itemId)}"
                               >
                                   Resync Item
                               </button>
                           </div>
                        </div>
                    </div>
                `;
            }).join("")}
        </div>
    `;

    container.querySelectorAll(".merchant-stock-save-btn").forEach(button => {
        button.addEventListener("click", async () => {
            const itemId = button.dataset.itemId || "";
            const stockQuantity = getMerchantStockInputValue(itemId);

            await updateMerchantStock(itemId, stockQuantity, button);
        });
    });

    container.querySelectorAll(".merchant-toggle-btn").forEach(button => {
        button.addEventListener("click", async () => {
            const itemId = button.dataset.itemId || "";
            const active = button.dataset.active === "true";

            await toggleMerchantInventoryActive(itemId, !active, button);
        });
    });

    container.querySelectorAll(".merchant-resync-btn").forEach(button => {
        button.addEventListener("click", async () => {
            const itemId = button.dataset.itemId || "";

            await resyncMerchantInventoryItem(itemId, button);
        });
    });

    container.querySelectorAll(".merchant-edit-btn").forEach(button => {
        button.addEventListener("click", () => {
            try {
                const item = JSON.parse(decodeURIComponent(button.dataset.item || ""));
                openInventoryEditModal(item);
            } catch (error) {
                console.error("Inventory edit parse error:", error);
                showToast("Unable to open item editor.", "error");
            }
        });
    });
}

    function renderMerchantInventoryPagination(result) {
        const container = document.getElementById("merchantInventoryPagination");
        if (!container) return;

        const page = Number(result?.page || 0);
        const totalPages = Number(result?.totalPages || 1);
        const totalItems = Number(result?.totalItems || 0);

        if (totalPages <= 1) {
            container.innerHTML = totalItems
                ? `<div class="merchant-inventory-pagination"><div class="text-muted small fw-semibold">Showing ${totalItems} item(s)</div></div>`
                : "";
            return;
        }

        container.innerHTML = `
            <div class="merchant-inventory-pagination">
                <div class="text-muted small fw-semibold">
                    ${totalItems} item(s) • Page ${page + 1} of ${totalPages}
                </div>

                <div class="d-flex gap-2">
                    <button
                        id="merchantInventoryPrevBtn"
                        class="btn btn-outline-dark btn-sm"
                        ${page <= 0 ? "disabled" : ""}
                    >
                        Prev
                    </button>

                    <button
                        id="merchantInventoryNextBtn"
                        class="btn btn-outline-dark btn-sm"
                        ${page >= totalPages - 1 ? "disabled" : ""}
                    >
                        Next
                    </button>
                </div>
            </div>
        `;

        document.getElementById("merchantInventoryPrevBtn")?.addEventListener("click", () => {
            merchantInventoryPage = Math.max(0, merchantInventoryPage - 1);
            loadMerchantInventory();
        });

        document.getElementById("merchantInventoryNextBtn")?.addEventListener("click", () => {
            merchantInventoryPage = merchantInventoryPage + 1;
            loadMerchantInventory();
        });
    }

    async function seedDemoInventory() {
    const button = document.getElementById("seedDemoInventoryBtn");

    try {
        requireToken();

        setButtonBusy(button, "Seeding...");
        setMerchantInventoryStatus("Seeding demo inventory...", "muted");

        const resp = await fetch(`${API.merchant}/seed-demo`, {
            method: "POST",
            headers: getAuthHeaders()
        });

        await assertAuthorizedResponse(resp, "Failed to seed demo inventory.");

        const message = await resp.text();

        setMerchantInventoryStatus(message || "Demo inventory seeded.", "success");
        showToast(message || "Demo inventory seeded.", "success");

        merchantInventoryPage = 0;
        await loadMerchantInventory();
    } catch (error) {
        console.error("Seed Demo Inventory Error:", error);
        setMerchantInventoryStatus(error.message || "Failed to seed demo inventory.", "danger");
        showToast(error.message || "Failed to seed demo inventory.", "error");
    } finally {
        clearButtonBusy(button, "Seed Demo Inventory");
    }
}

async function clearDemoInventory() {
    const button = document.getElementById("clearDemoInventoryBtn");

    const confirmed = window.confirm(
        "Clear demo inventory items? This removes the seeded demo RFID products from the database."
    );

    if (!confirmed) return;

    try {
        requireToken();

        setButtonBusy(button, "Clearing...");
        setMerchantInventoryStatus("Clearing demo inventory...", "muted");

        const resp = await fetch(`${API.merchant}/demo`, {
            method: "DELETE",
            headers: getAuthHeaders()
        });

        await assertAuthorizedResponse(resp, "Failed to clear demo inventory.");

        const message = await resp.text();

        setMerchantInventoryStatus(message || "Demo inventory cleared.", "success");
        showToast(message || "Demo inventory cleared.", "success");

        merchantInventoryPage = 0;
        await loadMerchantInventory();
    } catch (error) {
        console.error("Clear Demo Inventory Error:", error);
        setMerchantInventoryStatus(error.message || "Failed to clear demo inventory.", "danger");
        showToast(error.message || "Failed to clear demo inventory.", "error");
    } finally {
        clearButtonBusy(button, "Clear Demo Inventory");
    }
}

async function exportLowStockInventoryCsv() {
    const query = document.getElementById("inventorySearchInput")?.value || "";
    const category = document.getElementById("inventoryCategoryFilter")?.value || "";
    const button = document.getElementById("exportLowStockBtn");

    try {
        requireToken();

        setButtonBusy(button, "Downloading...");
        setMerchantInventoryStatus("Preparing low stock CSV export...", "muted");

        const params = new URLSearchParams({
            q: query,
            category,
            threshold: "3"
        });

        const resp = await fetch(`${API.merchant}/export/low-stock?${params.toString()}`, {
            method: "GET",
            headers: getAuthHeaders()
        });

        await assertAuthorizedResponse(resp, "Failed to export low stock inventory.");

        const blob = await resp.blob();

        let filename = "merchant-low-stock-inventory.csv";
        const disposition = resp.headers.get("Content-Disposition");

        if (disposition) {
            const filenameMatch = disposition.match(/filename="?([^"]+)"?/);
            if (filenameMatch && filenameMatch[1]) {
                filename = filenameMatch[1];
            }
        }

        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement("a");

        link.href = downloadUrl;
        link.download = filename;
        document.body.appendChild(link);
        link.click();

        link.remove();
        window.URL.revokeObjectURL(downloadUrl);

        setMerchantInventoryStatus("Low stock CSV downloaded.", "success");
        showToast("Low stock CSV downloaded.", "success");
    } catch (error) {
        console.error("Low Stock Export Error:", error);
        setMerchantInventoryStatus(error.message || "Failed to export low stock inventory.", "danger");
        showToast(error.message || "Failed to export low stock inventory.", "error");
    } finally {
        clearButtonBusy(button, "Low Stock CSV");
    }
}

async function exportInventoryCsv() {
    const button = document.getElementById("exportInventoryCsvBtn");
    const query = document.getElementById("inventorySearchInput")?.value || "";
    const category = document.getElementById("inventoryCategoryFilter")?.value || "";

    try {
        requireToken();

        setButtonBusy(button, "Exporting...");
        setMerchantInventoryStatus("Preparing CSV export...", "muted");

        const params = new URLSearchParams({
            q: query,
            category
        });

        const resp = await fetch(`${API.merchant}/export?${params.toString()}`, {
            method: "GET",
            headers: getAuthHeaders()
        });

        await assertAuthorizedResponse(resp, "Failed to export inventory CSV.");

        const blob = await resp.blob();
        const downloadUrl = window.URL.createObjectURL(blob);

        const timestamp = new Date()
            .toISOString()
            .replaceAll(":", "-")
            .replaceAll(".", "-");

        const link = document.createElement("a");
        link.href = downloadUrl;
        link.download = `merchant-inventory-${timestamp}.csv`;

        document.body.appendChild(link);
        link.click();
        link.remove();

        window.URL.revokeObjectURL(downloadUrl);

        setMerchantInventoryStatus("Inventory CSV exported.", "success");
        showToast("Inventory CSV exported.", "success");
    } catch (error) {
        console.error("Export Inventory CSV Error:", error);
        setMerchantInventoryStatus(error.message || "Failed to export inventory CSV.", "danger");
        showToast(error.message || "Failed to export inventory CSV.", "error");
    } finally {
        clearButtonBusy(button, "Export CSV");
    }
}

function setInventoryEditStatus(message, type = "muted") {
    const status = document.getElementById("inventoryEditStatus");
    if (!status) return;

    const classMap = {
        success: "text-success",
        danger: "text-danger",
        muted: "text-muted"
    };

    status.innerHTML = message
        ? `<div class="${classMap[type] || "text-muted"} fw-semibold">${escapeHtml(message)}</div>`
        : "";
}

function setInputValue(id, value) {
    const element = document.getElementById(id);
    if (!element) return;
    element.value = value ?? "";
}

function getInputValue(id) {
    return document.getElementById(id)?.value?.trim() || "";
}

function getInputNumber(id, fallback = 0) {
    const raw = getInputValue(id);

    if (!raw) {
        return fallback;
    }

    const parsed = Number(raw);

    return Number.isFinite(parsed) ? parsed : fallback;
}

function getInputBoolean(id, fallback = false) {
    const raw = getInputValue(id).toLowerCase();

    if (raw === "true") return true;
    if (raw === "false") return false;

    return fallback;
}

function openInventoryEditModal(item) {
    if (!item || typeof item !== "object") {
        showToast("Inventory item is missing.", "error");
        return;
    }

    const rfid = getItemField(item, "rfid", "itemRfid", "productRfid", "id");

    if (!rfid) {
        showToast("Inventory item RFID is missing.", "error");
        return;
    }

    setInventoryEditStatus("");

    setInputValue("inventoryEditOriginalRfid", rfid);
    setInputValue("inventoryEditRfid", rfid);
    setInputValue("inventoryEditItemName", getItemField(item, "itemName", "name"));
    setInputValue("inventoryEditBrand", getItemField(item, "brand"));
    setInputValue("inventoryEditCategory", getItemField(item, "category"));
    setInputValue("inventoryEditColor", getItemField(item, "color"));
    setInputValue("inventoryEditPrice", getItemField(item, "price"));
    setInputValue("inventoryEditImageUrl", getItemField(item, "imageUrl", "image_url", "image"));
    setInputValue("inventoryEditStockQuantity", getItemField(item, "stockQuantity", "stock", "quantity"));
    setInputValue("inventoryEditReorderThreshold", getItemField(item, "reorderThreshold"));
    setInputValue("inventoryEditIdealStockLevel", getItemField(item, "idealStockLevel"));

    setInputValue("inventoryEditSize", getItemField(item, "size"));
    setInputValue("inventoryEditFit", getItemField(item, "fit"));
    setInputValue("inventoryEditMaterial", getItemField(item, "material"));
    setInputValue("inventoryEditGender", getItemField(item, "gender"));
    setInputValue("inventoryEditSeason", getItemField(item, "season"));
    setInputValue("inventoryEditOccasion", getItemField(item, "occasion"));
    setInputValue("inventoryEditStyleTags", getItemField(item, "styleTags"));
    setInputValue("inventoryEditPattern", getItemField(item, "pattern"));

    setInputValue("inventoryEditActive", String(item.active !== false));
    setInputValue("inventoryEditAvailable", String(item.available !== false));

    const modalElement = document.getElementById("inventoryEditModal");

    if (!modalElement) {
        showToast("Inventory edit modal is missing from index.html.", "error");
        return;
    }

    const modal =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);

    modal.show();
}

function buildInventoryEditPayload() {
    const itemName = getInputValue("inventoryEditItemName");
    const category = getInputValue("inventoryEditCategory");
    const price = getInputNumber("inventoryEditPrice", 0);
    const stockQuantity = getInputNumber("inventoryEditStockQuantity", 0);
    const reorderThreshold = getInputNumber("inventoryEditReorderThreshold", 3);
    const idealStockLevel = getInputNumber("inventoryEditIdealStockLevel", 12);

    if (!itemName) {
        throw new Error("Item name is required.");
    }

    if (!category) {
        throw new Error("Category is required.");
    }

    if (price < 0) {
        throw new Error("Price cannot be negative.");
    }

    if (!Number.isInteger(stockQuantity) || stockQuantity < 0) {
        throw new Error("Stock quantity must be a whole number greater than or equal to 0.");
    }

    return {
        rfid: getInputValue("inventoryEditRfid"),
        itemName,
        brand: getInputValue("inventoryEditBrand"),
        category,
        color: getInputValue("inventoryEditColor"),
        price,
        imageUrl: getInputValue("inventoryEditImageUrl"),
        stockQuantity,
        reorderThreshold,
        idealStockLevel,

        size: getInputValue("inventoryEditSize"),
        fit: getInputValue("inventoryEditFit"),
        material: getInputValue("inventoryEditMaterial"),
        gender: getInputValue("inventoryEditGender"),
        season: getInputValue("inventoryEditSeason"),
        occasion: getInputValue("inventoryEditOccasion"),
        styleTags: getInputValue("inventoryEditStyleTags"),
        pattern: getInputValue("inventoryEditPattern"),

        active: getInputBoolean("inventoryEditActive", true),
        available: getInputBoolean("inventoryEditAvailable", true)
    };
}

async function saveInventoryEdit() {
    const saveBtn = document.getElementById("saveInventoryEditBtn");
    const originalText = saveBtn?.textContent || "Save Item Changes";

    try {
        requireToken();

        const originalRfid = getInputValue("inventoryEditOriginalRfid");

        if (!originalRfid) {
            throw new Error("Original RFID is missing.");
        }

        const payload = buildInventoryEditPayload();

        if (saveBtn) {
            saveBtn.disabled = true;
            saveBtn.textContent = "Saving...";
        }

        setInventoryEditStatus("Saving inventory item...", "muted");

        const response = await fetch(`${API.merchant}/${encodeURIComponent(originalRfid)}`, {
            method: "PUT",
            headers: getAuthHeaders({
                "Content-Type": "application/json",
                Accept: "application/json"
            }),
            body: JSON.stringify(payload)
        });

        await assertAuthorizedResponse(response, "Unable to update inventory item.");

        const updatedItem = await response.json();

        setInventoryEditStatus("Inventory item updated.", "success");
        showToast(`Updated ${updatedItem.itemName || updatedItem.name || "inventory item"}.`, "success");

        const modalElement = document.getElementById("inventoryEditModal");
        const modal = modalElement ? bootstrap.Modal.getInstance(modalElement) : null;

        if (modal) {
            modal.hide();
        }

        await loadMerchantInventory();
    } catch (error) {
        console.error("Save Inventory Edit Error:", error);
        setInventoryEditStatus(error.message || "Unable to update inventory item.", "danger");
        showToast(error.message || "Unable to update inventory item.", "error");
    } finally {
        if (saveBtn) {
            saveBtn.disabled = false;
            saveBtn.textContent = originalText;
        }
    }
}

    async function exportReorderReportCsv() {
        const query = document.getElementById("inventorySearchInput")?.value || "";
        const category = document.getElementById("inventoryCategoryFilter")?.value || "";
        const button = document.getElementById("exportReorderReportBtn");

        try {
            requireToken();

            setButtonBusy(button, "Downloading...");
            setMerchantInventoryStatus("Preparing reorder report...", "muted");

            const params = new URLSearchParams({
                q: query,
                category
            });

            const resp = await fetch(`${API.merchant}/export/reorder-report?${params.toString()}`, {
                method: "GET",
                headers: getAuthHeaders()
            });

            await assertAuthorizedResponse(resp, "Failed to export reorder report.");

            const blob = await resp.blob();

            let filename = "merchant-reorder-report.csv";
            const disposition = resp.headers.get("Content-Disposition");

            if (disposition) {
                const filenameMatch = disposition.match(/filename="?([^"]+)"?/);

                if (filenameMatch && filenameMatch[1]) {
                    filename = filenameMatch[1];
                }
            }

            const downloadUrl = window.URL.createObjectURL(blob);
            const link = document.createElement("a");

            link.href = downloadUrl;
            link.download = filename;
            document.body.appendChild(link);
            link.click();

            link.remove();
            window.URL.revokeObjectURL(downloadUrl);

            setMerchantInventoryStatus("Reorder report downloaded.", "success");
            showToast("Reorder report downloaded.", "success");
        } catch (error) {
            console.error("Reorder Report Export Error:", error);
            setMerchantInventoryStatus(error.message || "Failed to export reorder report.", "danger");
            showToast(error.message || "Failed to export reorder report.", "error");
        } finally {
            clearButtonBusy(button, "Download Reorder Report");
        }
    }

    async function loadMerchantInventory() {
        const query = document.getElementById("inventorySearchInput")?.value || "";
        const category = document.getElementById("inventoryCategoryFilter")?.value || "";
        const loadBtn = document.getElementById("loadInventoryBtn");
        const topLoadBtn = document.getElementById("loadInventoryBtnTop");

        try {
            requireToken();

            setButtonBusy(loadBtn, "Loading...");
            setButtonBusy(topLoadBtn, "Refreshing...");
            setMerchantInventoryStatus("Loading merchant inventory...", "muted");

            const params = new URLSearchParams({
                q: query,
                category,
                page: String(merchantInventoryPage),
                size: "12"
            });

            const resp = await fetch(`${API.merchant}?${params.toString()}`, {
                headers: getAuthHeaders()
            });

            await assertAuthorizedResponse(resp, "Failed to load merchant inventory.");

            const result = await resp.json();

            renderMerchantInventorySummary(result);
            renderMerchantInventory(result);
            renderMerchantInventoryPagination(result);
            setMerchantInventoryStatus("Merchant inventory loaded.", "success");

            await loadMerchantSalesActivity();
        } catch (error) {
            console.error("Merchant Inventory Error:", error);
            setMerchantInventoryStatus(error.message || "Failed to load inventory.", "danger");
            showToast(error.message || "Failed to load inventory.", "error");
        } finally {
            clearButtonBusy(loadBtn, "Load");
            clearButtonBusy(topLoadBtn, "Refresh");
        }
    }

    async function updateMerchantStock(itemId, stockQuantity, triggerButton = null) {
        if (!itemId) {
            showToast("Missing inventory item id.", "error");
            return;
        }

        if (!Number.isInteger(stockQuantity) || stockQuantity < 0) {
            showToast("Enter a valid stock quantity.", "error");
            return;
        }

        const originalText = triggerButton?.textContent || "Save Stock";

        try {
            requireToken();

            if (triggerButton) {
                triggerButton.disabled = true;
                triggerButton.textContent = "Saving...";
            }

            const resp = await fetch(`${API.merchant}/${encodeURIComponent(itemId)}/stock`, {
                method: "PUT",
                headers: getAuthHeaders({ "Content-Type": "application/json" }),
                body: JSON.stringify({ stockQuantity })
            });

            await assertAuthorizedResponse(resp, "Failed to update stock.");
            setMerchantInventoryStatus("Stock updated successfully.", "success");
            showToast("Stock updated.", "success");
            await loadMerchantInventory();
        } catch (error) {
            console.error("Update Merchant Stock Error:", error);
            setMerchantInventoryStatus(error.message || "Failed to update stock.", "danger");
            showToast(error.message || "Failed to update stock.", "error");
        } finally {
            if (triggerButton) {
                triggerButton.disabled = false;
                triggerButton.textContent = originalText;
            }
        }
    }

    async function toggleMerchantInventoryActive(itemId, active, triggerButton = null) {
        if (!itemId) {
            showToast("Missing inventory item id.", "error");
            return;
        }

        const originalText = triggerButton?.textContent || (active ? "Reactivate" : "Deactivate");

        try {
            requireToken();

            if (triggerButton) {
                triggerButton.disabled = true;
                triggerButton.textContent = "Updating...";
            }

            const resp = await fetch(`${API.merchant}/${encodeURIComponent(itemId)}/status`, {
                method: "PATCH",
                headers: getAuthHeaders({ "Content-Type": "application/json" }),
                body: JSON.stringify({ active })
            });

            await assertAuthorizedResponse(resp, "Failed to update inventory status.");
            setMerchantInventoryStatus(`Item ${active ? "reactivated" : "deactivated"} successfully.`, "success");
            showToast(`Item ${active ? "reactivated" : "deactivated"}.`, "success");
            await loadMerchantInventory();
        } catch (error) {
            console.error("Toggle Merchant Inventory Error:", error);
            setMerchantInventoryStatus(error.message || "Failed to update item status.", "danger");
            showToast(error.message || "Failed to update item status.", "error");
        } finally {
            if (triggerButton) {
                triggerButton.disabled = false;
                triggerButton.textContent = originalText;
            }
        }
    }

    async function resyncMerchantInventoryItem(itemId, triggerButton = null) {
        if (!itemId) {
            showToast("Missing inventory item id.", "error");
            return;
        }

        const originalText = triggerButton?.textContent || "Resync Item";

        try {
            requireToken();

            if (triggerButton) {
                triggerButton.disabled = true;
                triggerButton.textContent = "Resyncing...";
            }

            const resp = await fetch(`${API.merchant}/${encodeURIComponent(itemId)}/resync`, {
                method: "POST",
                headers: getAuthHeaders()
            });

            await assertAuthorizedResponse(resp, "Failed to resync item.");
            setMerchantInventoryStatus("Item resynced successfully.", "success");
            showToast("Item resynced.", "success");
            await loadMerchantInventory();
        } catch (error) {
            console.error("Resync Merchant Inventory Error:", error);
            setMerchantInventoryStatus(error.message || "Failed to resync item.", "danger");
            showToast(error.message || "Failed to resync item.", "error");
        } finally {
            if (triggerButton) {
                triggerButton.disabled = false;
                triggerButton.textContent = originalText;
            }
        }
    }

    function downloadInventoryImportTemplate() {
    const headers = [
        "rfid",
        "item_name",
        "brand",
        "category",
        "color",
        "price",
        "image_url",
        "stock_quantity",
        "retailer_key",
        "retailer_name",
        "store_code",
        "store_name",
        "active",
        "available"
    ];

    const sampleRows = [
        [
            "RFID1001",
            "Oxford Shirt",
            "Polo Ralph Lauren",
            "Tops",
            "Blue",
            "80.0",
            "/images/products/oxford-shirt.jpg",
            "12",
            "MACY001",
            "Macy's",
            "MACY-NYC-01",
            "Herald Square",
            "TRUE",
            "TRUE"
        ],
        [
            "RFID1002",
            "Slim Chino",
            "Polo Ralph Lauren",
            "Bottoms",
            "Khaki",
            "95.0",
            "/images/products/slim-chino.jpg",
            "8",
            "MACY001",
            "Macy's",
            "MACY-NYC-01",
            "Herald Square",
            "TRUE",
            "TRUE"
        ]
    ];

    const csvRows = [
        headers.join(","),
        ...sampleRows.map(row => row.map(value => {
            const text = String(value ?? "");
            return text.includes(",") || text.includes("\"") || text.includes("\n")
                ? `"${text.replace(/"/g, '""')}"`
                : text;
        }).join(","))
    ];

    const csv = csvRows.join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement("a");

    link.href = downloadUrl;
    link.download = "merchant-inventory-import-template.csv";
    document.body.appendChild(link);
    link.click();

    link.remove();
    window.URL.revokeObjectURL(downloadUrl);

    showToast("CSV import template downloaded.", "success");
}

function formatImportHistoryDate(value) {
    if (!value) return "Unknown time";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return String(value);
    }

    return date.toLocaleString();
}

function renderImportHistory(history) {
    const list = document.getElementById("inventoryImportHistoryList");
    if (!list) return;

    if (!Array.isArray(history) || history.length === 0) {
        list.innerHTML = `
            <div class="import-history-empty">
                No recent uploads yet.
            </div>
        `;
        return;
    }

    list.innerHTML = history.map(log => {
        const filename = log.originalFilename || "inventory.csv";
        const createdAt = formatImportHistoryDate(log.createdAt);
        const retailerKey = log.retailerKey || "Retailer";
        const storeCode = log.storeCode || "Store";
        const successCount = Number(log.successCount || 0);
        const failureCount = Number(log.failureCount || 0);
        const totalRows = Number(log.totalRows || successCount + failureCount || 0);
        const status = log.status || "COMPLETED";

        const statusClass =
            failureCount > 0 || status === "FAILED"  || status === "COMPLETED_WITH_ERRORS"
                ? "error"
                : "success";

        return `
            <div class="import-history-row">
                <div class="import-history-main">
                    <div class="import-history-file">${escapeHtml(filename)}</div>
                    <div class="import-history-meta">
                        ${escapeHtml(retailerKey)} • ${escapeHtml(storeCode)} • ${escapeHtml(createdAt)}
                    </div>
                </div>

                <div class="import-history-stats">
                    <span class="import-history-pill success">${successCount} imported</span>
                    <span class="import-history-pill ${failureCount > 0 ? "error" : "neutral"}">${failureCount} failed</span>
                    <span class="import-history-pill neutral">${totalRows} rows</span>
                    <span class="import-history-pill ${statusClass}">${escapeHtml(status)}</span>
                </div>
            </div>
        `;
    }).join("");
}

async function loadImportHistory() {
    const refreshBtn = document.getElementById("refreshImportHistoryBtn");

    try {
        requireToken();

        if (refreshBtn) {
            refreshBtn.disabled = true;
            refreshBtn.textContent = "Refreshing...";
        }

        const resp = await fetch(`${API.merchant}/import-history`, {
            method: "GET",
            headers: getAuthHeaders()
        });

        await assertAuthorizedResponse(resp, "Failed to load import history.");

        const history = await resp.json();
        renderImportHistory(history);
    } catch (error) {
        console.error("Import History Error:", error);

        const list = document.getElementById("inventoryImportHistoryList");
        if (list) {
            list.innerHTML = `
                <div class="import-history-empty">
                    Unable to load import history.
                </div>
            `;
        }
    } finally {
        if (refreshBtn) {
            refreshBtn.disabled = false;
            refreshBtn.textContent = "Refresh History";
        }
    }
}

function renderImportJobs(jobs) {
    const list = document.getElementById("inventoryImportJobsList");
    if (!list) return;

    if (!Array.isArray(jobs) || jobs.length === 0) {
        list.innerHTML = `
            <div class="import-history-empty">
                No import jobs yet.
            </div>
        `;
        return;
    }

    list.innerHTML = jobs.map(job => {
        const filename = job.originalFilename || "inventory.csv";
        const createdAt = formatImportHistoryDate(job.createdAt);
        const retailerKey = job.retailerKey || "Retailer";
        const storeCode = job.storeCode || "Store";
        const status = String(job.status || "QUEUED").toUpperCase();
        const mode = String(job.mode || "STANDARD").toUpperCase();
        const processedRows = Number(job.processedRows || 0);
        const totalRows = Number(job.totalRows || 0);
        const successCount = Number(job.successCount || 0);
        const failureCount = Number(job.failureCount || 0);
        const message = job.message || "";

        const progressPercent =
            totalRows > 0
                ? Math.min(100, Math.round((processedRows / totalRows) * 100))
                : 0;

        const statusClass =
            failureCount > 0 || status === "FAILED" || status === "COMPLETED_WITH_ERRORS"
                ? "error"
                : status === "RUNNING" || status === "QUEUED"
                    ? "neutral"
                    : "success";

        const modeClass = mode === "BULK" ? "success" : "neutral";

        return `
           <div
               class="import-history-row"
               role="button"
               tabindex="0"
               onclick="showImportJobDetails('${escapeHtml(job.jobId || "")}')"
               onkeydown="handleImportJobRowKeydown(event, '${escapeHtml(job.jobId || "")}')"
           >
                <div class="import-history-main">
                    <div class="import-history-file">${escapeHtml(filename)}</div>
                    <div class="import-history-meta">
                        ${escapeHtml(retailerKey)} • ${escapeHtml(storeCode)} • ${escapeHtml(createdAt)}
                    </div>
                    ${
                        message
                            ? `<div class="import-history-meta">${escapeHtml(message)}</div>`
                            : ""
                    }
                </div>

                <div class="import-history-stats">
                    <span class="import-history-pill ${modeClass}">${escapeHtml(mode)}</span>
                    <span class="import-history-pill neutral">${progressPercent}% processed</span>
                    <span class="import-history-pill neutral">${processedRows}/${totalRows} rows</span>
                    <span class="import-history-pill success">${successCount} imported</span>
                    <span class="import-history-pill ${failureCount > 0 ? "error" : "neutral"}">${failureCount} failed</span>
                    <span class="import-history-pill ${statusClass}">${escapeHtml(status)}</span>
                </div>
            </div>
        `;
    }).join("");
}

function handleImportJobRowKeydown(event, jobId) {
    if (!event || !jobId) return;

    if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        showImportJobDetails(jobId);
    }
}

async function copyImportJobId(jobId) {
    if (!jobId) {
        showToast("No import job id to copy.", "error");
        return;
    }

    try {
        await navigator.clipboard.writeText(jobId);
        showToast("Import job id copied.", "success");
    } catch (error) {
        console.error("Copy Import Job ID Error:", error);
        showToast("Unable to copy import job id.", "error");
    }
}

function stopImportJobDetailsAutoRefresh() {
    if (importJobDetailsRefreshTimer) {
        window.clearTimeout(importJobDetailsRefreshTimer);
        importJobDetailsRefreshTimer = null;
    }
}

async function cancelImportJob(jobId) {
    if (!jobId) {
        showToast("No import job id to cancel.", "error");
        return;
    }

    const confirmed = window.confirm("Cancel this import job? Completed jobs cannot be cancelled.");

    if (!confirmed) return;

    try {
        requireToken();

        const response = await fetch(`${API.merchant}/import-jobs/${encodeURIComponent(jobId)}/cancel`, {
            method: "POST",
            headers: getAuthHeaders()
        });

        await assertAuthorizedResponse(response, "Unable to cancel import job.");

        showToast("Import job cancelled.", "success");

        await loadImportJobs();
        await showImportJobDetails(jobId);
    } catch (error) {
        console.error("Cancel Import Job Error:", error);
        showToast(error.message || "Unable to cancel import job.", "error");
    }
}

async function showImportJobDetails(jobId) {
    if (!jobId) {
        showToast("Import job id is missing.", "error");
        return;
    }

    const modalEl = document.getElementById("importJobDetailsModal");
    const bodyEl = document.getElementById("importJobDetailsModalBody");
    const refreshBtn = document.getElementById("refreshImportJobDetailsBtn");
    const copyBtn = document.getElementById("copyImportJobIdBtn");
    const cancelBtn = document.getElementById("cancelImportJobBtn");

    if (!modalEl || !bodyEl) {
        showToast("Import job details modal is missing.", "error");
        return;
    }

    modalEl.dataset.jobId = jobId;

    bodyEl.innerHTML = `
        <div class="text-muted fw-semibold">
            Loading job details...
        </div>
    `;

    const modal =
        bootstrap.Modal.getInstance(modalEl) ||
        new bootstrap.Modal(modalEl);

    modal.show();

    if (refreshBtn) {
        refreshBtn.onclick = () => showImportJobDetails(jobId);
    }

    if (copyBtn) {
        copyBtn.onclick = () => copyImportJobId(jobId);
    }

    if (cancelBtn) {
        cancelBtn.onclick = () => cancelImportJob(jobId);
    }
    try {
        requireToken();

        const response = await fetch(`${API.merchant}/import-jobs/${encodeURIComponent(jobId)}`, {
            headers: getAuthHeaders()
        });

        await assertAuthorizedResponse(response, "Unable to load import job details.");

        const job = await response.json();

        const mode = String(job.mode || "STANDARD").toUpperCase();
        const status = String(job.status || "QUEUED").toUpperCase();
        const canCancel = status === "QUEUED" || status === "RUNNING";

        if (cancelBtn) {
           cancelBtn.classList.toggle("d-none", !canCancel);
       }
        stopImportJobDetailsAutoRefresh();

        if (status === "QUEUED" || status === "RUNNING") {
            importJobDetailsRefreshTimer = window.setTimeout(() => {
               showImportJobDetails(jobId);
               loadImportJobs();
           }, 2000);
       }
        const filename = job.originalFilename || "inventory.csv";
        const retailerKey = job.retailerKey || "Retailer";
        const storeCode = job.storeCode || "Store";
        const totalRows = Number(job.totalRows || 0);
        const processedRows = Number(job.processedRows || 0);
        const successCount = Number(job.successCount || 0);
        const failureCount = Number(job.failureCount || 0);
        const message = job.message || "No message.";
        const progressPercent =
            totalRows > 0
                ? Math.min(100, Math.round((processedRows / totalRows) * 100))
                : 0;

        const modeClass = mode === "BULK" ? "success" : "neutral";
        const statusClass =
            failureCount > 0 || status === "FAILED" || status === "COMPLETED_WITH_ERRORS"
                ? "error"
                : status === "RUNNING" || status === "QUEUED"
                    ? "neutral"
                    : "success";

        bodyEl.innerHTML = `
            <div class="d-flex gap-2 flex-wrap mb-3">
                <span class="import-history-pill ${modeClass}">${escapeHtml(mode)}</span>
                <span class="import-history-pill ${statusClass}">${escapeHtml(status)}</span>
                <span class="import-history-pill neutral">${progressPercent}% processed</span>
            </div>

            <div class="csv-rules-grid">
                <div class="csv-rule-box">
                    <div class="csv-rule-label">Job ID</div>
                    <div class="fw-bold text-break">${escapeHtml(job.jobId || "N/A")}</div>
                </div>

                <div class="csv-rule-box">
                    <div class="csv-rule-label">File</div>
                    <div class="fw-bold text-break">${escapeHtml(filename)}</div>
                </div>

                <div class="csv-rule-box">
                    <div class="csv-rule-label">Location</div>
                    <div class="fw-bold text-break">${escapeHtml(retailerKey)} • ${escapeHtml(storeCode)}</div>
                </div>

                <div class="csv-rule-box">
                    <div class="csv-rule-label">Rows</div>
                    <div class="fw-bold">${processedRows}/${totalRows}</div>
                </div>

                <div class="csv-rule-box">
                    <div class="csv-rule-label">Imported</div>
                    <div class="fw-bold text-success">${successCount}</div>
                </div>

                <div class="csv-rule-box">
                    <div class="csv-rule-label">Failed</div>
                    <div class="fw-bold ${failureCount > 0 ? "text-danger" : "text-muted"}">${failureCount}</div>
                </div>

                <div class="csv-rule-box">
                    <div class="csv-rule-label">Created</div>
                    <div class="fw-bold text-break">${escapeHtml(formatImportHistoryDate(job.createdAt))}</div>
                </div>

                <div class="csv-rule-box">
                    <div class="csv-rule-label">Started</div>
                    <div class="fw-bold text-break">${escapeHtml(formatImportHistoryDate(job.startedAt))}</div>
                </div>

                <div class="csv-rule-box">
                    <div class="csv-rule-label">Completed</div>
                    <div class="fw-bold text-break">${escapeHtml(formatImportHistoryDate(job.completedAt))}</div>
                </div>
            </div>

            <div class="csv-bad-example mt-3" style="background: rgba(15,23,42,0.03); border-color: rgba(15,23,42,0.08); color: #475467;">
                <strong style="color:#111827;">Message:</strong>
                ${escapeHtml(message)}
            </div>
        `;
    } catch (error) {
        console.error("Import Job Detail Error:", error);

        bodyEl.innerHTML = `
            <div class="import-history-empty">
                Unable to load import job details.
            </div>
        `;

        showToast(error.message || "Unable to load import job details.", "error");
    }
}
async function loadImportJobs() {
    const refreshBtn = document.getElementById("refreshImportJobsBtn");

    try {
        requireToken();

        if (refreshBtn) {
            refreshBtn.disabled = true;
            refreshBtn.textContent = "Refreshing...";
        }

        const resp = await fetch(`${API.merchant}/import-jobs`, {
            method: "GET",
            headers: getAuthHeaders()
        });

        await assertAuthorizedResponse(resp, "Failed to load import jobs.");

        const jobs = await resp.json();
        renderImportJobs(jobs);
    } catch (error) {
        console.error("Import Jobs Error:", error);

        const list = document.getElementById("inventoryImportJobsList");
        if (list) {
            list.innerHTML = `
                <div class="import-history-empty">
                    Unable to load import jobs.
                </div>
            `;
        }
    } finally {
        if (refreshBtn) {
            refreshBtn.disabled = false;
            refreshBtn.textContent = "Refresh Jobs";
        }
    }
}
    async function uploadInventoryCsv() {
        const fileInput = document.getElementById("inventoryCsvFile");
        const uploadBtn = document.getElementById("uploadInventoryBtn");
        const file = fileInput?.files?.[0];

        if (!file) {
            setInventoryUploadStatus("Choose a CSV file first.", "danger");
            showToast("Choose a CSV file first.", "error");
            return;
        }

        try {
            requireToken();

            const formData = new FormData();
            formData.append("file", file);

            setButtonBusy(uploadBtn, "Uploading...");
            setInventoryUploadStatus("Uploading inventory CSV...", "muted");
            renderInventoryUploadResult(null);

            const resp = await fetch(`${API.merchant}/upload`, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${getToken()}`
                },
                body: formData
            });

            await assertAuthorizedResponse(resp, "Inventory upload failed.");
            const result = await resp.json();

            renderInventoryUploadResult(result);
            setInventoryUploadStatus("Inventory upload completed.", "success");
            await loadImportHistory();
            await loadImportJobs();

            if (fileInput) fileInput.value = "";

            merchantInventoryPage = 0;
            await loadMerchantInventory();
        } catch (error) {
            console.error("Inventory Upload Error:", error);
            setInventoryUploadStatus(error.message || "Inventory upload failed.", "danger");
            showToast(error.message || "Inventory upload failed.", "error");
        } finally {
            clearButtonBusy(uploadBtn, "Upload CSV");
        }
    }

  async function uploadInventoryCsvBulk() {
    const fileInput = document.getElementById("inventoryCsvFile");
    const bulkUploadBtn = document.getElementById("bulkUploadInventoryBtn");
    const file = fileInput?.files?.[0];

    if (!file) {
        setInventoryUploadStatus("Choose a CSV file first.", "danger");
        showToast("Choose a CSV file first.", "error");
        return;
    }

    try {
        requireToken();

        const formData = new FormData();
        formData.append("file", file);

        setButtonBusy(bulkUploadBtn, "Starting...");
        setInventoryUploadStatus("Starting async bulk import...", "muted");
        renderInventoryUploadResult(null);

        const resp = await fetch(`${API.merchant}/upload/bulk`, {
            method: "POST",
            headers: {
                Authorization: `Bearer ${getToken()}`
            },
            body: formData
        });

        await assertAuthorizedResponse(resp, "Bulk inventory upload failed.");

        const job = await resp.json();
        const jobId = job?.jobId || "";

        setInventoryUploadStatus(
            jobId
                ? `Bulk import started. Job ID: ${jobId}`
                : "Bulk import started.",
            "success"
        );

        showToast("Bulk import started.", "success");

        if (fileInput) fileInput.value = "";

        await loadImportJobs();

        document.getElementById("inventoryImportJobs")?.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });

        window.setTimeout(() => {
            loadImportJobs();
            loadMerchantInventory();
        }, 1500);
        window.setTimeout(() => {
            loadImportJobs();
            loadMerchantInventory();
        }, 3500);
    } catch (error) {
        console.error("Bulk Inventory Upload Error:", error);
        setInventoryUploadStatus(error.message || "Bulk inventory upload failed.", "danger");
        showToast(error.message || "Bulk inventory upload failed.", "error");
    } finally {
        clearButtonBusy(bulkUploadBtn, "Bulk Upload");
    }
}
   async function login() {
       const email = document.getElementById("loginEmail")?.value.trim();
       const password = document.getElementById("loginPassword")?.value.trim();
       const loginBtn = document.getElementById("loginBtn");

       if (!email || !password) {
           showToast("Enter email and password.", "error");
           return;
       }

       try {
           clearToken();
           clearStoredLoginEmail();

           setButtonBusy(loginBtn, "Logging in...");

           const resp = await fetch(`${API.auth}/login`, {
               method: "POST",
               headers: { "Content-Type": "application/json" },
               body: JSON.stringify({ email, password })
           });

           if (!resp.ok) {
               const msg = await resp.text().catch(() => "");
               throw new Error(msg || "Login failed.");
           }

           const rawLoginResponse = await resp.text();
           const token = extractTokenFromLoginResponse(rawLoginResponse);

           if (!token || !token.includes(".")) {
               throw new Error("Login succeeded but no valid JWT token was returned.");
           }

           const payload = parseJwtPayload(token);

           if (!payload) {
               throw new Error("Login returned an unreadable JWT token.");
           }

           console.log("JWT payload after login:", payload);

           setToken(token);
           setStoredLoginEmail(email);

           await restoreLoggedInStoreContext();

           updateAuthStatus();
           updateAuthUI();

           await loadBag();
           await renderSavedLooksDrawer();

           if (isOwnerUser()) {
               merchantInventoryPage = 0;
               await loadImportHistory();
               await loadImportJobs();
               await loadMerchantInventory();
           }

           showToast("Logged in successfully.", "success");
       } catch (error) {
           clearToken();

           console.error("Login Error:", error);
           showToast(error.message || "Login failed.", "error");
       } finally {
           clearButtonBusy(loginBtn, "Login");
       }
   }

    async function signup() {
        const signupBtn = document.getElementById("signupBtn");

        const payload = {
            fullName: document.getElementById("signupFullName")?.value.trim(),
            businessName: document.getElementById("signupBusinessName")?.value.trim(),
            slug: document.getElementById("signupSlug")?.value.trim(),
            tenantEmail: document.getElementById("signupTenantEmail")?.value.trim(),
            storeName: document.getElementById("signupStoreName")?.value.trim(),
            location: document.getElementById("signupLocation")?.value.trim(),
            retailerKey: document.getElementById("signupRetailerKey")?.value.trim(),
            userEmail: document.getElementById("signupUserEmail")?.value.trim(),
            password: document.getElementById("signupPassword")?.value.trim()
        };

        const missing = Object.values(payload).some(value => !value);
        if (missing) {
            showToast("Fill in all signup fields.", "error");
            return;
        }

        try {
            setButtonBusy(signupBtn, "Creating...");

            const resp = await fetch(`${API.auth}/signup`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });

            if (!resp.ok) {
                const msg = await resp.text().catch(() => "");
                throw new Error(msg || "Signup failed.");
            }

            await resp.text().catch(() => "");
            showToast("Signup successful. Now log in.", "success");
        } catch (error) {
            console.error("Signup Error:", error);
            showToast(error.message || "Signup failed.", "error");
        } finally {
            clearButtonBusy(signupBtn, "Create Account");
        }
    }

   function logout() {
       clearToken();
       clearStoredLoginEmail();

       sessionStorage.removeItem("user");
       sessionStorage.removeItem("username");
       localStorage.removeItem("loginEmail");
       localStorage.removeItem("user");
       localStorage.removeItem("username");

       window.loggedInRetailerKey = "";
       window.loggedInStoreCode = "";
       window.loggedInAuthContext = null;
       window.currentCustomerPreferences = null;
       window.activeSavedLookId = "";

       updateAuthStatus();
       updateAuthUI();
       resetScanExperience();

       clearBagUi(
           "Logged out",
           "Log in to access your saved bag and insights."
       );

       const savedLooksContent = document.getElementById("savedLooksContent");
       if (savedLooksContent) {
           savedLooksContent.innerHTML = `
               <div class="saved-look-empty">
                   <div class="bag-empty-icon">✨</div>
                   <div class="bag-empty-title">Logged out</div>
                   <p class="bag-empty-text">Log in to view your saved looks.</p>
               </div>
           `;
       }

       const orderHistoryContent = document.getElementById("orderHistoryContent");
       if (orderHistoryContent) {
           orderHistoryContent.innerHTML = `
               <div class="bag-empty-shell">
                   <div class="bag-empty-icon">🧾</div>
                   <div class="bag-empty-title">Logged out</div>
                   <p class="bag-empty-text">Log in to view your order history.</p>
               </div>
           `;
       }

       const preferencesContent = document.getElementById("preferencesContent");
       if (preferencesContent) {
           preferencesContent.innerHTML = `
               <div class="bag-empty-shell">
                   <div class="bag-empty-icon">🔐</div>
                   <div class="bag-empty-title">Logged out</div>
                   <p class="bag-empty-text">Log in to manage your styling preferences.</p>
               </div>
           `;
       }

       showToast("Logged out.", "info");
   }

  async function handleScan(scanOverride = null) {
    const selectedRetailer = getSelectedRetailerKey();
    const selectedStoreCode = getSelectedStoreCode();

    const retailer =
        scanOverride?.retailerKey ||
        selectedRetailer;

    const storeCode =
        scanOverride?.storeCode ||
        selectedStoreCode;

    const rfid = document.getElementById("rfidInput")?.value.trim();
    const vibe = document.getElementById("vibeSelect")?.value || "Casual";
    const scanBtn = document.getElementById("scanBtn");

    if (!rfid) {
        setScanStatus("Enter or scan an RFID tag first.", "danger");
        resetScanExperience();
        setTimeout(() => setScanStatus("Ready to scan.", "muted"), 1800);
        return;
    }

    setScanStatus("Analyzing item...", "muted");
    setLiveDotState("scanning");
    showLoadingState();

    if (scanBtn) {
        scanBtn.disabled = true;
        scanBtn.setAttribute("aria-busy", "true");
        scanBtn.innerHTML = "Analyzing<span class='dot-anim'></span>";
    }

    try {
        requireToken();

        window.activeSavedLookId = "";
        updateSaveLookButtonState();

       const params = await buildPreferencesQueryParams();
        params.set("vibe", vibe);

        /*
         * Normal secured mode:
         * Backend uses JWT retailer/store context.
         *
         * Demo scan mode:
         * Backend can optionally use retailerKey/storeCode override
         * if your controller/service supports demo context switching.
         */
        if (DEMO_SCAN_MODE) {
            if (retailer) {
                params.set("retailerKey", retailer);
            }

            if (storeCode) {
                params.set("storeCode", storeCode);
            }
        }

        const resp = await fetch(
            `${API.stylist}/scan/${encodeURIComponent(rfid)}?${params.toString()}`,
            {
                method: "GET",
                headers: getAuthHeaders()
            }
        );

        await assertAuthorizedResponse(resp, "Could not scan this item for the selected store.");
        const data = await resp.json();

        hideLoadingState();
        renderScanResult(data, vibe);
        addRecentScan(data, vibe);
        await renderRecentScansFromBackend();
        setLiveDotState("success");

        const rfidInput = document.getElementById("rfidInput");
        if (rfidInput) rfidInput.value = "";

        setScanStatus(
            `Loaded ${data?.itemName || data?.name || "item"} from ${data?.retailerName || data?.retailer || "retailer"}.`,
            "success"
        );

        setTimeout(() => {
            setLiveDotState("ready");
            setScanStatus("Ready to scan.", "muted");
        }, 1800);
    } catch (error) {
        hideLoadingState();
        resetScanExperience();
        setLiveDotState("error");

        const message =
            typeof error === "string"
                ? error
                : error?.message || JSON.stringify(error) || "Unable to scan item.";

        setScanStatus(message, "danger");
        showToast(message, "error");

        console.error("Scan request failed:", {
            error,
            message: error?.message || String(error),
            stack: error?.stack,
            retailer,
            storeCode,
            rfid,
            vibe,
            demoScanMode: DEMO_SCAN_MODE
        });

        setTimeout(() => {
            setLiveDotState("ready");
            setScanStatus("Ready to scan.", "muted");
        }, 1800);
    } finally {
        if (scanBtn) {
            scanBtn.disabled = false;
            scanBtn.removeAttribute("aria-busy");
            setTimeout(() => {
                scanBtn.textContent = "Scan Item";
            }, 300);
        }
    }
}

function quickScan(retailerKey, rfid) {
    const context = getJwtContext();

    const safeRetailerKey = String(
        retailerKey ||
        context.retailerKey ||
        window.loggedInRetailerKey ||
        ""
    ).trim().toUpperCase();

    const safeStoreCode = String(
        context.storeCode ||
        window.loggedInStoreCode ||
        ""
    ).trim().toUpperCase();

    const safeRfid = String(rfid || "").trim().toUpperCase();

    if (!safeRetailerKey) {
        setScanStatus("Quick scan is missing retailer context.", "danger");
        showToast("Quick scan is missing retailer context.", "error");
        return;
    }

    if (!safeStoreCode) {
        setScanStatus("Quick scan is missing store context. Log out and log back in.", "danger");
        showToast("Quick scan is missing store context. Log out and log back in.", "error");
        return;
    }

    if (!safeRfid || safeRfid === "UNDEFINED") {
        setScanStatus("Quick scan is missing RFID.", "danger");
        showToast("Quick scan is missing RFID.", "error");
        return;
    }

    const rfidInput = document.getElementById("rfidInput");
    const vibeSelect = document.getElementById("vibeSelect");

    if (rfidInput) {
        rfidInput.value = safeRfid;
    }

    if (vibeSelect) {
        vibeSelect.value = "Casual";
    }

    setScanStatus(`Scanning ${safeRfid} from ${safeRetailerKey}...`, "muted");

    handleScan({
        retailerKey: safeRetailerKey,
        storeCode: safeStoreCode
    });
}
    async function saveToBag() {
        if (!currentRfid || !currentLoadedItem) {
            setScanStatus("Scan an item before saving it to your bag.", "danger");
            showToast("Scan an item first before saving.", "error");
            return;
        }

        const saveBtn = getSaveButton();
        setButtonBusy(saveBtn, "Saving...");

        try {
            requireToken();

            const resp = await fetch(`${API.stylist}/save/${encodeURIComponent(currentRfid)}`, {
                method: "POST",
                headers: getAuthHeaders()
            });

            await assertAuthorizedResponse(resp, `Save request failed with status ${resp.status}`);

            const msg = await resp.text();
            savedRfids.add(currentRfid);
            setSaveButtonSaved();

            setScanStatus(msg || "Item saved to bag.", "success");
            showToast(msg || "Item saved to bag.", "success");

            await Promise.allSettled([loadBag(), loadAllInsights()]);

            setTimeout(() => {
                setScanStatus("Ready to scan.", "muted");
            }, 1800);
        } catch (error) {
            console.error("Save Error:", error);
            setScanStatus("Unable to save item right now.", "danger");
            showToast(error.message || "Unable to save item right now.", "error");
            setSaveButtonDefault(false);
        }
    }

    function getFullOutfitItemsForBag(fullOutfit) {
        if (!fullOutfit || typeof fullOutfit !== "object") {
            return [];
        }

        const scannedRfid =
            currentRfid ||
            getItemField(lastScannedItem, "rfid", "itemRfid", "productRfid", "id") ||
            "";

        const pieces = [
            { role: "Top", item: fullOutfit.top },
            { role: "Bottom", item: fullOutfit.bottom },
            { role: "Shoes", item: fullOutfit.shoes },
            { role: "Outerwear", item: fullOutfit.outerwear }
        ];

        const seenRfids = new Set();

        return pieces
            .map(piece => {
                const rfid = getItemField(piece.item, "rfid", "itemRfid", "productRfid", "id");

                if (!piece.item || !rfid) {
                    return null;
                }

                const normalizedRfid = String(rfid).trim();

                if (!normalizedRfid) {
                    return null;
                }

                if (seenRfids.has(normalizedRfid)) {
                    return null;
                }

                seenRfids.add(normalizedRfid);

                const isScannedAnchor = scannedRfid && normalizedRfid === scannedRfid;
                const available = piece.item.available !== false && piece.item.active !== false;
                const stockQuantity = Number(
                    piece.item.stockQuantity ??
                    piece.item.stock ??
                    piece.item.quantity ??
                    1
                );

                return {
                    role: piece.role,
                    item: piece.item,
                    rfid: normalizedRfid,
                    isScannedAnchor,
                    available,
                    stockQuantity: Number.isFinite(stockQuantity) ? stockQuantity : 1
                };
            })
            .filter(Boolean);
    }

    async function saveItemToBagByRfid(rfid) {
        const safeRfid = String(rfid || "").trim();

        if (!safeRfid) {
            throw new Error("RFID is required to add an item to the bag.");
        }

        requireToken();

        const response = await fetch(`${API.stylist}/save/${encodeURIComponent(safeRfid)}`, {
            method: "POST",
            headers: getAuthHeaders()
        });

        await assertAuthorizedResponse(response, `Save request failed with status ${response.status}`);

        const message = await response.text().catch(() => "");

        savedRfids.add(safeRfid);

        return message;
    }

    async function addFullLookToBag(fullOutfit, triggerButton = null) {
        const pieces = getFullOutfitItemsForBag(fullOutfit);

        if (!pieces.length) {
            showToast("No outfit pieces are available to add.", "error");
            return;
        }

        const availablePieces = pieces.filter(piece => {
            return piece.available && piece.stockQuantity > 0;
        });

        if (!availablePieces.length) {
            showToast("No available outfit pieces can be added to your bag.", "error");
            return;
        }

        const originalText = triggerButton?.textContent || "Add Full Look to Bag";

        let addedCount = 0;
        let skippedCount = pieces.length - availablePieces.length;
        let failedCount = 0;

        try {
            requireToken();

            if (triggerButton) {
                triggerButton.disabled = true;
                triggerButton.textContent = "Adding Look...";
                triggerButton.setAttribute("aria-busy", "true");
            }

            for (const piece of availablePieces) {
                try {
                    if (savedRfids.has(piece.rfid)) {
                        skippedCount += 1;
                        continue;
                    }

                    await saveItemToBagByRfid(piece.rfid);
                    addedCount += 1;
                } catch (error) {
                    console.error(`Failed to add ${piece.role} to bag:`, error);
                    failedCount += 1;
                }
            }

            await Promise.allSettled([
                loadBag(),
                loadAllInsights()
            ]);

            if (addedCount > 0) {
                showToast(
                    `Added ${addedCount} look item${addedCount === 1 ? "" : "s"} to your bag.`,
                    "success"
                );
            } else if (failedCount > 0) {
                showToast("Unable to add this full look to your bag.", "error");
            } else {
                showToast("All available look items are already in your bag.", "info");
            }

            if (skippedCount > 0 || failedCount > 0) {
                console.info("Add Full Look summary:", {
                    addedCount,
                    skippedCount,
                    failedCount
                });
            }

            if (triggerButton && addedCount > 0) {
                triggerButton.textContent = "Added Look ✓";

                window.setTimeout(() => {
                    triggerButton.textContent = originalText;
                }, 1600);
            }
        } catch (error) {
            console.error("Add Full Look To Bag Error:", error);
            showToast(error.message || "Unable to add full look to bag.", "error");
        } finally {
            if (triggerButton) {
                triggerButton.disabled = false;
                triggerButton.removeAttribute("aria-busy");

                if (triggerButton.textContent !== "Added Look ✓") {
                    triggerButton.textContent = originalText;
                }
            }
        }
    }

    async function saveSuggestedItem(item, triggerButton = null) {
        const rfid = getSuggestionRfid(item);

        if (!rfid) {
            showToast("Suggested item is missing RFID.", "error");
            return;
        }

        if (!isSuggestionAvailable(item)) {
            showToast("This suggested item is not currently available.", "error");
            return;
        }

        const originalText = triggerButton ? triggerButton.textContent : "";

        try {
            requireToken();

            if (triggerButton) {
                triggerButton.disabled = true;
                triggerButton.textContent = "Saving...";
            }

            const resp = await fetch(`${API.stylist}/save/${encodeURIComponent(rfid)}`, {
                method: "POST",
                headers: getAuthHeaders()
            });

            await assertAuthorizedResponse(resp, `Save request failed with status ${resp.status}`);

            const msg = await resp.text();
            savedRfids.add(rfid);

            showToast(msg || "Suggested item saved.", "success");

            if (triggerButton) {
                triggerButton.textContent = "Saved ✓";
                triggerButton.classList.remove("btn-outline-dark");
                triggerButton.classList.add("btn-dark");
                triggerButton.disabled = true;
            }

            await Promise.allSettled([
                loadBag(),
                loadAllInsights()
            ]);
        } catch (error) {
            console.error("Save Suggestion Error:", error);
            showToast(error.message || "Unable to save suggestion.", "error");

            if (triggerButton) {
                triggerButton.disabled = false;
                triggerButton.textContent = originalText || "Save Suggested Piece";
            }
        }
    }

    async function removeFromBag(id) {
        try {
            requireToken();

            const resp = await fetch(`${API.stylist}/bag/${encodeURIComponent(id)}`, {
                method: "DELETE",
                headers: getAuthHeaders()
            });

            await assertAuthorizedResponse(resp, `Remove request failed with status ${resp.status}`);

            await Promise.allSettled([loadBag(), loadAllInsights()]);

            if (currentLoadedItem) {
                if (isCurrentItemSaved(currentLoadedItem)) {
                    setSaveButtonSaved();
                } else {
                    setSaveButtonDefault(false);
                }
            }

            showToast("Item removed from bag.", "info");
        } catch (error) {
            console.error("Remove Bag Item Error:", error);
            showToast(error.message || "Unable to remove item right now.", "error");
        }
    }

    async function clearBag() {
        try {
            requireToken();

            const resp = await fetch(`${API.stylist}/bag`, {
                method: "DELETE",
                headers: getAuthHeaders()
            });

            await assertAuthorizedResponse(resp, `Clear bag request failed with status ${resp.status}`);

            savedRfids = new Set();

            await Promise.allSettled([loadBag(), loadAllInsights()]);

            if (currentLoadedItem) {
                setSaveButtonDefault(false);
            }

            showToast("Bag cleared.", "info");
        } catch (error) {
            console.error("Clear Bag Error:", error);
            showToast(error.message || "Unable to clear bag right now.", "error");
        }
    }

   let lastLoadedBag = null;
   window.lastLoadedOrders = [];

   async function removeUnavailableBagItems() {
       const button = document.getElementById("removeUnavailableBagItemsBtn");
       const originalText = button?.textContent || "Remove Unavailable Items";

       try {
           requireToken();

           if (button) {
               button.disabled = true;
               button.textContent = "Checking...";
           }

           const response = await fetch(`${API.stylist}/bag/remove-unavailable`, {
               method: "DELETE",
               headers: getAuthHeaders({
                   Accept: "application/json"
               })
           });

           const rawText = await response.text();

           if (!response.ok) {
               const message =
                   parseBackendMessage(rawText) ||
                   cleanApiErrorMessage(rawText) ||
                   "Unable to remove unavailable items.";

               throw new Error(message);
           }

           const result = rawText ? JSON.parse(rawText) : {};
           const removedCount = safeNumber(result.removedCount || result.count || 0);

           await Promise.allSettled([
               loadBag(),
               loadAllInsights()
           ]);

           if (removedCount > 0) {
               showToast(`${removedCount} unavailable item${removedCount === 1 ? "" : "s"} removed.`, "success");
           } else {
               showToast("No unavailable items found in your bag.", "info");
           }
       } catch (error) {
           console.error("Remove Unavailable Bag Items Error:", error);
           showToast(error.message || "Unable to remove unavailable items.", "error");
       } finally {
           if (button) {
               button.disabled = false;
               button.textContent = originalText;
           }
       }
   }

   function getBagItemsFromLastLoad() {
       return Array.isArray(lastLoadedBag?.items) ? lastLoadedBag.items : [];
   }

   async function updateBagItemQuantity(bagItemId, quantity, triggerButton = null) {
       const safeId = String(bagItemId || "").trim();
       const safeQuantity = Number(quantity);

       if (!safeId) {
           showToast("Missing bag item id.", "error");
           return;
       }

       if (!Number.isInteger(safeQuantity) || safeQuantity < 1) {
           showToast("Quantity must be at least 1.", "error");
           return;
       }

       const originalText = triggerButton?.textContent || "";

       try {
           requireToken();

           if (triggerButton) {
               triggerButton.disabled = true;
               triggerButton.textContent = "…";
           }

           const response = await fetch(`${API.stylist}/bag/${encodeURIComponent(safeId)}/quantity`, {
               method: "PATCH",
               headers: getAuthHeaders({
                   "Content-Type": "application/json",
                   Accept: "application/json"
               }),
               body: JSON.stringify({
                   quantity: safeQuantity
               })
           });

           const rawText = await response.text();

           if (!response.ok) {
               const message =
                   parseBackendMessage(rawText) ||
                   cleanApiErrorMessage(rawText) ||
                   `Unable to update bag quantity. Server returned ${response.status}.`;

               throw new Error(message);
           }

           showToast("Bag quantity updated.", "success");

           await Promise.allSettled([
               loadBag(),
               loadAllInsights()
           ]);
       } catch (error) {
           console.error("Update Bag Quantity Error:", error);
           showToast(error.message || "Unable to update bag quantity.", "error");
       } finally {
           if (triggerButton) {
               triggerButton.disabled = false;
               triggerButton.textContent = originalText;
           }
       }
   }

   function getItemQuantity(item) {
       const quantity = Number(
           item?.quantity ??
           item?.qty ??
           item?.count ??
           1
       );

       return Number.isFinite(quantity) && quantity > 0
           ? Math.floor(quantity)
           : 1;
   }

   function calculateCheckoutTotals(items) {
       const safeItems = Array.isArray(items) ? items : [];

       const subtotal = safeItems.reduce((sum, item) => {
           const quantity = getItemQuantity(item);
           const price = safeNumber(item?.price);
           return sum + price * quantity;
       }, 0);

       const tax = subtotal * 0.0825;
       const total = subtotal + tax;

       return {
           subtotal,
           tax,
           total
       };
   }

   function openCheckoutModal() {
       const items = getBagItemsFromLastLoad();

       if (!items.length) {
           showToast("Your bag is empty.", "error");
           return;
       }

       const modalEl = document.getElementById("checkoutModal");
       const bodyEl = document.getElementById("checkoutModalBody");
       const confirmBtn = document.getElementById("confirmCheckoutBtn");

       if (!modalEl || !bodyEl || !confirmBtn) {
           showToast("Checkout modal is missing.", "error");
           return;
       }

       const totals = calculateCheckoutTotals(items);

       bodyEl.innerHTML = `
           <div class="checkout-review-list">
               ${items.map(item => {
                   const name = item.itemName || item.name || "Unnamed Item";
                   const retailer = item.retailerName || item.retailer || "Retailer";
                   const category = item.category || "Style Item";
                   const imageUrl = safeImageUrl(item.imageUrl || "", "https://placehold.co/76x76?text=Item");
                   const price = safeNumber(item.price);
                   const quantity = getItemQuantity(item);
                   const lineTotal = price * quantity;

                   return `
                       <div class="checkout-review-item">
                           <img
                               src="${imageUrl}"
                               alt="${escapeHtml(name)}"
                               class="checkout-review-img"
                               onerror="this.src='https://placehold.co/76x76?text=Item';"
                           />

                           <div class="min-width-0">
                               <div class="checkout-review-name">${escapeHtml(name)}</div>
                               <div class="checkout-review-meta">
                                   ${escapeHtml(retailer)} • ${escapeHtml(category)}${quantity > 1 ? ` • Qty ${quantity}` : ""}
                               </div>
                           </div>

                           <div class="checkout-review-price">${formatPrice(lineTotal)}</div>
                       </div>
                   `;
               }).join("")}
           </div>

           <div class="checkout-summary-box">
               <div class="checkout-summary-row">
                   <span>Subtotal</span>
                   <strong>${formatPrice(totals.subtotal)}</strong>
               </div>

               <div class="checkout-summary-row">
                   <span>Estimated Tax</span>
                   <strong>${formatPrice(totals.tax)}</strong>
               </div>

               <div class="checkout-summary-row final">
                   <span>Total</span>
                   <span>${formatPrice(totals.total)}</span>
               </div>
           </div>

           <div class="small text-muted fw-semibold mt-3">
               Inventory availability will be checked before checkout is completed.
           </div>
       `;

       confirmBtn.disabled = false;
       confirmBtn.textContent = "Confirm Checkout";
       confirmBtn.onclick = completeCheckout;

       const modal =
           bootstrap.Modal.getInstance(modalEl) ||
           new bootstrap.Modal(modalEl);

       modal.show();
   }

   async function validateCurrentBagBeforeCheckout() {
       const items = getBagItemsFromLastLoad();

       if (!items.length) {
           throw new Error("Your bag is empty.");
       }

       const response = await fetch("/api/v1/orders/checkout/validate", {
           method: "POST",
           headers: getAuthHeaders({
               Accept: "application/json"
           })
       });

       const text = await response.text().catch(() => "");

       if (!response.ok) {
           throw new Error(
               parseBackendMessage(text) ||
               cleanApiErrorMessage(text) ||
               "One or more bag items are no longer available."
           );
       }

       try {
           return text ? JSON.parse(text) : { ok: true };
       } catch {
           return { ok: true, message: text };
       }
   }

   async function completeCheckout() {
       const confirmBtn = document.getElementById("confirmCheckoutBtn");
       const bodyEl = document.getElementById("checkoutModalBody");

       const originalText = confirmBtn?.textContent || "Confirm Checkout";

       try {
           if (bodyEl) {
               bodyEl.querySelectorAll(".checkout-error-message").forEach(errorBox => {
                   errorBox.remove();
               });
           }

           if (confirmBtn) {
               confirmBtn.disabled = true;
               confirmBtn.textContent = "Checking inventory...";
           }

           requireToken();

           await validateCurrentBagBeforeCheckout();

           if (confirmBtn) {
               confirmBtn.textContent = "Completing checkout...";
           }

           const response = await fetch("/api/v1/orders/checkout", {
               method: "POST",
               headers: getAuthHeaders({
                   Accept: "application/json"
               })
           });

           const rawText = await response.text().catch(() => "");

           if (!response.ok) {
               const message =
                   parseBackendMessage(rawText) ||
                   cleanApiErrorMessage(rawText) ||
                   `Checkout failed with status ${response.status}`;

               throw new Error(message);
           }

           let order = null;

           try {
               order = rawText ? JSON.parse(rawText) : null;
           } catch {
               order = null;
           }

           if (!order) {
               throw new Error("Checkout succeeded but no order receipt was returned.");
           }

           const purchasedItems = getOrderItems(order);

           if (bodyEl) {
               bodyEl.innerHTML = `
                   <div class="checkout-success-card">
                       <div class="checkout-success-icon">✔</div>
                       <div class="checkout-success-title">Checkout Complete</div>

                       <p class="checkout-success-copy">
                           Your order was saved, inventory was updated, and your current store bag has been cleared.
                       </p>

                       ${
                           purchasedItems.length
                               ? `
                                   <div class="checkout-review-list mb-3">
                                       ${purchasedItems.map(item => {
                                           const name = item.itemName || item.name || "Purchased Item";
                                           const retailer = item.retailerName || order.retailerName || "Retailer";
                                           const category = item.category || "Style Item";
                                           const quantity = getItemQuantity(item);
                                           const imageUrl = safeImageUrl(
                                               item.imageUrl || "",
                                               "https://placehold.co/76x76?text=Item"
                                           );
                                           const price = safeNumber(item.lineTotal ?? item.unitPrice ?? item.price);

                                           return `
                                               <div class="checkout-review-item">
                                                   <img
                                                       src="${imageUrl}"
                                                       alt="${escapeHtml(name)}"
                                                       class="checkout-review-img"
                                                       onerror="this.src='https://placehold.co/76x76?text=Item';"
                                                   />

                                                   <div class="min-width-0">
                                                       <div class="checkout-review-name">${escapeHtml(name)}</div>
                                                       <div class="checkout-review-meta">
                                                           ${escapeHtml(retailer)} • ${escapeHtml(category)}${quantity > 1 ? ` • Qty ${quantity}` : ""}
                                                       </div>
                                                   </div>

                                                   <div class="checkout-review-price">${formatPrice(price)}</div>
                                               </div>
                                           `;
                                       }).join("")}
                                   </div>
                               `
                               : ""
                       }

                       <div class="checkout-summary-box text-start mb-3">
                           <div class="checkout-summary-row">
                               <span>Items Purchased</span>
                               <strong>${safeNumber(order.itemCount || purchasedItems.length)}</strong>
                           </div>

                           <div class="checkout-summary-row">
                               <span>Subtotal</span>
                               <strong>${formatPrice(safeNumber(order.subtotal))}</strong>
                           </div>

                           <div class="checkout-summary-row">
                               <span>Estimated Tax</span>
                               <strong>${formatPrice(safeNumber(order.tax))}</strong>
                           </div>

                           <div class="checkout-summary-row final">
                               <span>Total</span>
                               <span>${formatPrice(safeNumber(order.total))}</span>
                           </div>
                       </div>

                       <div class="checkout-receipt-code">
                           Receipt ${escapeHtml(order.orderNumber || "Saved")}
                       </div>

                       <div class="d-grid gap-2 mt-3">
                           <button
                               type="button"
                               class="merchant-inline-btn primary"
                               id="checkoutPrintReceiptBtn"
                           >
                               Print / Save Receipt
                           </button>

                           <button
                               type="button"
                               class="merchant-inline-btn"
                               id="checkoutSendReceiptBtn"
                           >
                               Send Simulated Receipt
                           </button>
                       </div>
                   </div>
               `;

               document.getElementById("checkoutPrintReceiptBtn")?.addEventListener("click", () => {
                   printOrderReceipt(order);
               });

               document.getElementById("checkoutSendReceiptBtn")?.addEventListener("click", () => {
                   sendOrderReceipt(order.orderNumber);
               });
           }

           if (confirmBtn) {
               confirmBtn.disabled = true;
               confirmBtn.textContent = "Checkout Complete";
           }

           showToast("Checkout completed successfully.", "success");

           await refreshBagAfterCheckout();

           await loadOrderHistory().catch(error => {
               console.warn("Order history refresh after checkout failed:", error);
           });

           if (isOwnerUser()) {
               await loadMerchantSalesActivity().catch(error => {
                   console.warn("Merchant sales activity refresh after checkout failed:", error);
               });

               await loadMerchantInventory().catch(error => {
                   console.warn("Merchant inventory refresh after checkout failed:", error);
               });
           }
       } catch (error) {
           console.error("Backend Checkout Error:", error);

           const message =
               error.message ||
               "Unable to complete checkout. One or more items may no longer be available.";

           showToast(message, "error");

           if (bodyEl) {
               bodyEl.querySelectorAll(".checkout-error-message").forEach(errorBox => {
                   errorBox.remove();
               });

               bodyEl.insertAdjacentHTML(
                   "beforeend",
                   `
                       <div class="alert alert-danger mt-3 fw-semibold checkout-error-message">
                           ${escapeHtml(message)}
                       </div>
                   `
               );
           }

           if (confirmBtn) {
               confirmBtn.disabled = false;
               confirmBtn.textContent = originalText;
           }

           await loadBag().catch(() => {});
       }
   }

   async function refreshBagAfterCheckout() {
       try {
           lastLoadedBag = {
               items: []
           };

           await loadBag();

           const bagCountEls = document.querySelectorAll(
               "#bagCount, .bag-count, [data-bag-count]"
           );

           bagCountEls.forEach(el => {
               el.textContent = "0";
           });
       } catch (error) {
           console.warn("Bag refresh after checkout failed:", error);
       }
   }

   function formatOrderDate(value) {
       if (!value) return "Recently";

       const date = new Date(value);

       if (Number.isNaN(date.getTime())) {
           return String(value);
       }

       return date.toLocaleString();
   }

   function getOrderItems(order) {
       if (!order || typeof order !== "object") {
           return [];
       }

       if (Array.isArray(order.items)) return order.items;
       if (Array.isArray(order.orderItems)) return order.orderItems;
       if (Array.isArray(order.lineItems)) return order.lineItems;

       return [];
   }

   function getOrderNumber(order) {
       return (
           order?.orderNumber ||
           order?.receiptNumber ||
           order?.id ||
           order?.orderId ||
           ""
       );
   }

   function getOrderCreatedAt(order) {
       return (
           order?.createdAt ||
           order?.orderedAt ||
           order?.checkoutAt ||
           order?.completedAt ||
           order?.date ||
           ""
       );
   }

   function getOrderStatus(order) {
       return (
           order?.status ||
           order?.orderStatus ||
           "COMPLETED"
       );
   }

   function isTerminalOrderStatus(status) {
       const safeStatus = String(status || "").trim().toUpperCase();

       return (
           safeStatus === "CANCELLED" ||
           safeStatus === "RETURNED" ||
           safeStatus === "REFUNDED"
       );
   }

   function parseBackendMessage(value) {
       if (!value) return "";

       try {
           const parsed = JSON.parse(value);

           if (typeof parsed === "string") {
               return parsed;
           }

           return parsed.message || parsed.error || parsed.detail || parsed.title || "";
       } catch {
           return String(value).replace(/^"|"$/g, "").trim();
       }
   }

   async function fetchOrderDetails(orderNumber) {
       const safeOrderNumber = String(orderNumber || "").trim();

       if (!safeOrderNumber) {
           throw new Error("Missing order number.");
       }

       const response = await fetch(`/api/v1/orders/${encodeURIComponent(safeOrderNumber)}`, {
           method: "GET",
           headers: getAuthHeaders({
               Accept: "application/json"
           })
       });

       await assertAuthorizedResponse(response, "Unable to load order details.");

       return await response.json();
   }

   async function updateOrderLifecycleStatus(orderNumber, action) {
       const safeOrderNumber = String(orderNumber || "").trim();
       const safeAction = String(action || "").trim().toLowerCase();

       if (!safeOrderNumber) {
           throw new Error("Missing order number.");
       }

       if (!["cancel", "return", "refund"].includes(safeAction)) {
           throw new Error("Unsupported order action.");
       }

       const response = await fetch(`/api/v1/orders/${encodeURIComponent(safeOrderNumber)}/${safeAction}`, {
           method: "POST",
           headers: getAuthHeaders({
               Accept: "application/json"
           })
       });

       const text = await response.text();

       if (!response.ok) {
           throw new Error(
               parseBackendMessage(text) ||
               `Unable to ${safeAction} order.`
           );
       }

       const parsed = text ? JSON.parse(text) : null;

       return parsed?.order || parsed || null;
   }

   async function sendOrderReceipt(orderNumber) {
       const safeOrderNumber = String(orderNumber || "").trim();

       if (!safeOrderNumber) {
           showToast("Missing order number.", "error");
           return;
       }

       try {
           requireToken();

           const response = await fetch(`/api/v1/orders/${encodeURIComponent(safeOrderNumber)}/send-receipt`, {
               method: "POST",
               headers: getAuthHeaders({
                   Accept: "application/json"
               })
           });

           const text = await response.text();

           if (!response.ok) {
               throw new Error(
                   parseBackendMessage(text) ||
                   "Unable to send simulated receipt."
               );
           }

           const parsed = text ? JSON.parse(text) : {};
           showToast(parsed.message || "Simulated receipt sent.", "success");
       } catch (error) {
           console.error("Send Receipt Error:", error);
           showToast(error.message || "Unable to send simulated receipt.", "error");
       }
   }

   function ensureOrderDetailsActionStyles() {
       if (document.getElementById("orderDetailsActionStyles")) {
           return;
       }

       const style = document.createElement("style");
       style.id = "orderDetailsActionStyles";

       style.textContent = `
           #orderDetailsModal .order-details-actions {
               display: grid !important;
               grid-template-columns: repeat(3, minmax(0, 1fr)) !important;
               gap: 10px !important;
               width: 100% !important;
               margin-top: 14px !important;
           }

           #orderDetailsModal .order-details-actions .merchant-inline-btn {
               width: 100% !important;
               min-height: 44px !important;
               border-radius: 12px !important;
               border: 0 !important;
               font-size: 14px !important;
               font-weight: 800 !important;
               line-height: 1.1 !important;
               padding: 12px 14px !important;
               display: flex !important;
               align-items: center !important;
               justify-content: center !important;
               text-align: center !important;
               box-shadow: none !important;
               appearance: none !important;
           }

           #orderDetailsModal .order-details-actions .merchant-inline-btn.primary {
               background: #0d6efd !important;
               color: #ffffff !important;
           }

           #orderDetailsModal .order-details-actions .merchant-inline-btn.success {
               background: #16a34a !important;
               color: #ffffff !important;
           }

           #orderDetailsModal .order-details-actions .merchant-inline-btn.warning {
               background: #f59e0b !important;
               color: #111827 !important;
           }

           #orderDetailsModal .order-details-actions .merchant-inline-btn.danger {
               background: #dc2626 !important;
               color: #ffffff !important;
           }

           #orderDetailsModal .order-details-actions .merchant-inline-btn:not(.primary):not(.success):not(.warning):not(.danger) {
               background: #ffffff !important;
               color: #111827 !important;
               border: 1px solid rgba(15, 23, 42, 0.14) !important;
           }

           #orderDetailsModal .order-details-terminal-note {
               grid-column: 1 / -1 !important;
               padding: 10px 2px !important;
           }

           @media (max-width: 768px) {
               #orderDetailsModal .order-details-actions {
                   grid-template-columns: 1fr !important;
               }
           }
       `;

       document.head.appendChild(style);
   }

   function renderOrderDetailsModal(order) {
       ensureOrderDetailsActionStyles();

       const modalEl = document.getElementById("orderDetailsModal");
       const bodyEl = document.getElementById("orderDetailsModalBody");
       const titleEl = document.getElementById("orderDetailsModalLabel");

       if (!modalEl || !bodyEl) {
           showToast("Order details modal is missing from index.html.", "error");
           return;
       }

       const orderNumber = getOrderNumber(order);
       const createdAt = getOrderCreatedAt(order);
       const status = getOrderStatus(order);
       const items = getOrderItems(order);
       const terminal = isTerminalOrderStatus(status);

       const subtotal = safeNumber(order?.subtotal);
       const tax = safeNumber(order?.tax);
       const total = safeNumber(order?.total);
       const itemCount = safeNumber(order?.itemCount || items.length);

       if (titleEl) {
           titleEl.textContent = `Receipt ${orderNumber}`;
       }

       bodyEl.innerHTML = `
           <div class="merchant-sale-detail-hero">
               <div>
                   <div class="merchant-sale-detail-kicker">Order Details</div>
                   <div class="merchant-sale-detail-receipt">
                       Receipt ${escapeHtml(orderNumber)}
                   </div>
                   <div class="merchant-sale-detail-meta">
                       ${escapeHtml(formatOrderDate(createdAt))} • ${escapeHtml(status)}
                   </div>
               </div>

               <div class="merchant-sale-detail-total">
                   ${formatPrice(total)}
               </div>
           </div>

           <div class="merchant-sale-detail-grid">
               <div class="merchant-sale-detail-stat">
                   <div class="merchant-sale-detail-stat-label">Status</div>
                   <div class="merchant-sale-detail-stat-value">${escapeHtml(status)}</div>
               </div>

               <div class="merchant-sale-detail-stat">
                   <div class="merchant-sale-detail-stat-label">Items</div>
                   <div class="merchant-sale-detail-stat-value">${itemCount}</div>
               </div>

               <div class="merchant-sale-detail-stat">
                   <div class="merchant-sale-detail-stat-label">Total</div>
                   <div class="merchant-sale-detail-stat-value">${formatPrice(total)}</div>
               </div>
           </div>

           <div class="section-kicker mb-2">Purchased Items</div>

           <div class="merchant-sale-detail-items">
               ${
                   items.length
                       ? items.map(item => {
                           const name = item.itemName || item.name || item.productName || "Purchased Item";
                           const retailer = item.retailerName || item.retailer || order.retailerName || "Retailer";
                           const category = item.category || "Style Item";
                           const quantity = getItemQuantity(item);
                           const price = safeNumber(item.lineTotal ?? item.unitPrice ?? item.price);
                           const imageUrl = safeImageUrl(
                               item.imageUrl || item.image_url || item.image || "",
                               "https://placehold.co/76x76?text=Item"
                           );

                           return `
                               <div class="merchant-sale-detail-item">
                                   <img
                                       src="${imageUrl}"
                                       alt="${escapeHtml(name)}"
                                       class="merchant-sale-detail-img"
                                       onerror="this.src='https://placehold.co/76x76?text=Item';"
                                   />

                                   <div class="merchant-sale-detail-item-main">
                                       <div class="merchant-sale-detail-item-name">${escapeHtml(name)}</div>
                                       <div class="merchant-sale-detail-item-meta">
                                           ${escapeHtml(retailer)} • ${escapeHtml(category)} • Qty ${quantity}
                                       </div>
                                   </div>

                                   <div class="merchant-sale-detail-item-price">
                                       ${formatPrice(price)}
                                   </div>
                               </div>
                           `;
                       }).join("")
                       : `
                           <div class="merchant-sales-empty">
                               No line items were returned for this order.
                           </div>
                       `
               }
           </div>

           <div class="merchant-sale-detail-summary">
               <div class="merchant-sale-detail-summary-row">
                   <span>Subtotal</span>
                   <strong>${formatPrice(subtotal)}</strong>
               </div>

               <div class="merchant-sale-detail-summary-row">
                   <span>Estimated Tax</span>
                   <strong>${formatPrice(tax)}</strong>
               </div>

               <div class="merchant-sale-detail-summary-row final">
                   <span>Total</span>
                   <span>${formatPrice(total)}</span>
               </div>
           </div>

           <div class="order-details-actions">
               <button
                   type="button"
                   class="merchant-inline-btn primary"
                   id="orderDetailsPrintBtn"
               >
                   Print / Save Receipt
               </button>

               <button
                   type="button"
                   class="merchant-inline-btn"
                   id="orderDetailsSendReceiptBtn"
               >
                   Send Simulated Receipt
               </button>

               <button
                   type="button"
                   class="merchant-inline-btn success"
                   id="orderDetailsBuyAgainBtn"
               >
                   Buy Again
               </button>

               ${
                   !terminal
                       ? `
                           <button
                               type="button"
                               class="merchant-inline-btn warning"
                               id="orderDetailsCancelBtn"
                           >
                               Cancel
                           </button>

                           <button
                               type="button"
                               class="merchant-inline-btn warning"
                               id="orderDetailsReturnBtn"
                           >
                               Return
                           </button>

                           <button
                               type="button"
                               class="merchant-inline-btn danger"
                               id="orderDetailsRefundBtn"
                           >
                               Refund
                           </button>
                       `
                       : `
                           <div class="small text-muted fw-semibold order-details-terminal-note">
                               This order is already ${escapeHtml(status)}.
                           </div>
                       `
               }
           </div>
       `;

       document.getElementById("orderDetailsPrintBtn")?.addEventListener("click", () => {
           printOrderReceipt(order);
       });

       document.getElementById("orderDetailsSendReceiptBtn")?.addEventListener("click", () => {
           sendOrderReceipt(orderNumber);
       });

       document.getElementById("orderDetailsBuyAgainBtn")?.addEventListener("click", async event => {
           const button = event.currentTarget;
           const originalText = button.textContent;

           try {
               button.disabled = true;
               button.textContent = "Adding...";

               await reorderFromHistory(order);

               button.textContent = "Added ✓";
           } catch (error) {
               button.disabled = false;
               button.textContent = originalText;
               showToast(error.message || "Unable to buy again.", "error");
           }
       });

       const bindLifecycleButton = (id, action, confirmText) => {
           document.getElementById(id)?.addEventListener("click", async event => {
               const confirmed = window.confirm(confirmText);

               if (!confirmed) {
                   return;
               }

               const button = event.currentTarget;
               const originalText = button.textContent;

               try {
                   button.disabled = true;
                   button.textContent = "Updating...";

                   const updatedOrder = await updateOrderLifecycleStatus(orderNumber, action);

                   showToast(`Order ${action} completed.`, "success");

                   if (updatedOrder) {
                       renderOrderDetailsModal(updatedOrder);
                   }

                   await loadOrderHistory();

                   if (isOwnerUser()) {
                       await Promise.allSettled([
                           loadMerchantSalesActivity(),
                           loadMerchantInventory()
                       ]);
                   }
               } catch (error) {
                   console.error(`Order ${action} error:`, error);
                   button.disabled = false;
                   button.textContent = originalText;
                   showToast(error.message || `Unable to ${action} order.`, "error");
               }
           });
       };

       bindLifecycleButton(
           "orderDetailsCancelBtn",
           "cancel",
           "Cancel this order and restore inventory?"
       );

       bindLifecycleButton(
           "orderDetailsReturnBtn",
           "return",
           "Mark this order as returned and restore inventory?"
       );

       bindLifecycleButton(
           "orderDetailsRefundBtn",
           "refund",
           "Refund this order and restore inventory?"
       );

       const modal =
           bootstrap.Modal.getInstance(modalEl) ||
           new bootstrap.Modal(modalEl);

       modal.show();
   }

   async function openOrderDetails(orderNumber) {
       try {
           requireToken();

           const fallbackOrder = window.lastLoadedOrders.find(order => {
               return String(getOrderNumber(order)) === String(orderNumber);
           });

           renderOrderDetailsModal(fallbackOrder || { orderNumber });

           const order = await fetchOrderDetails(orderNumber);
           renderOrderDetailsModal(order);
       } catch (error) {
           console.error("Open Order Details Error:", error);
           showToast(error.message || "Unable to open order details.", "error");
       }
   }

   function renderOrderHistory(orders) {
       const container = document.getElementById("orderHistoryContent");
       if (!container) return;

       const safeOrders = Array.isArray(orders) ? orders : [];
       window.lastLoadedOrders = safeOrders;

       if (!safeOrders.length) {
           container.innerHTML = `
               <div class="bag-empty-shell">
                   <div class="bag-empty-icon">🧾</div>
                   <div class="bag-empty-title">No orders yet</div>
                   <p class="bag-empty-text">
                       Complete a checkout from your style bag and your order history will appear here.
                   </p>
               </div>
           `;
           return;
       }

       container.innerHTML = `
           <div class="order-history-toolbar">
               <div class="section-kicker">Recent Orders</div>
               <button type="button" class="merchant-inline-btn" id="refreshOrderHistoryBtn">
                   Refresh Orders
               </button>
           </div>

           <div class="order-history-list">
               ${safeOrders.map(order => {
                   const orderNumber = getOrderNumber(order);
                   const createdAt = getOrderCreatedAt(order);
                   const status = getOrderStatus(order);
                   const items = getOrderItems(order);
                   const subtotal = safeNumber(order?.subtotal);
                   const tax = safeNumber(order?.tax);
                   const total = safeNumber(order?.total);
                   const itemCount = safeNumber(order?.itemCount || items.length);
                   const terminal = isTerminalOrderStatus(status);

                   return `
                       <div class="order-history-card ${terminal ? "is-terminal" : ""}">
                           <div class="order-history-header">
                               <div>
                                   <div class="order-history-number">
                                       Receipt ${escapeHtml(orderNumber)}
                                   </div>
                                   <div class="order-history-date">
                                       ${escapeHtml(formatOrderDate(createdAt))}
                                   </div>
                               </div>

                               <div class="order-history-total">
                                   ${formatPrice(total)}
                               </div>
                           </div>

                           <div class="order-history-meta-grid">
                               <div class="order-history-meta-box">
                                   <div class="order-history-meta-label">Status</div>
                                   <div class="order-history-meta-value">${escapeHtml(status)}</div>
                               </div>

                               <div class="order-history-meta-box">
                                   <div class="order-history-meta-label">Items</div>
                                   <div class="order-history-meta-value">${itemCount}</div>
                               </div>

                               <div class="order-history-meta-box">
                                   <div class="order-history-meta-label">Tax</div>
                                   <div class="order-history-meta-value">${formatPrice(tax)}</div>
                               </div>
                           </div>

                           ${
                               items.length
                                   ? `
                                       <div class="order-history-items">
                                           ${items.slice(0, 3).map(item => {
                                               const name = item.itemName || item.name || "Purchased Item";
                                               const retailer = item.retailerName || item.retailer || order.retailerName || "Retailer";
                                               const category = item.category || "Style Item";
                                               const quantity = getItemQuantity(item);
                                               const imageUrl = safeImageUrl(
                                                   item.imageUrl || "",
                                                   "https://placehold.co/76x76?text=Item"
                                               );
                                               const price = safeNumber(item.lineTotal ?? item.unitPrice ?? item.price);

                                               return `
                                                   <div class="order-history-item">
                                                       <img
                                                           src="${imageUrl}"
                                                           alt="${escapeHtml(name)}"
                                                           class="order-history-img"
                                                           onerror="this.src='https://placehold.co/76x76?text=Item';"
                                                       />

                                                       <div class="min-width-0">
                                                           <div class="order-history-item-name">
                                                               ${escapeHtml(name)}
                                                           </div>
                                                           <div class="order-history-item-meta">
                                                               ${escapeHtml(retailer)} • ${escapeHtml(category)}${quantity > 1 ? ` • Qty ${quantity}` : ""}
                                                           </div>
                                                       </div>

                                                       <div class="order-history-item-price">
                                                           ${formatPrice(price)}
                                                       </div>
                                                   </div>
                                               `;
                                           }).join("")}

                                           ${
                                               items.length > 3
                                                   ? `
                                                       <div class="small text-muted fw-semibold mt-2">
                                                           +${items.length - 3} more item${items.length - 3 === 1 ? "" : "s"}
                                                       </div>
                                                   `
                                                   : ""
                                           }
                                       </div>
                                   `
                                   : `
                                       <div class="result-empty-state">
                                           No line items were returned for this order.
                                       </div>
                                   `
                           }

                           <div class="checkout-summary-box text-start mt-3">
                               <div class="checkout-summary-row">
                                   <span>Subtotal</span>
                                   <strong>${formatPrice(subtotal)}</strong>
                               </div>

                               <div class="checkout-summary-row">
                                   <span>Estimated Tax</span>
                                   <strong>${formatPrice(tax)}</strong>
                               </div>

                               <div class="checkout-summary-row final">
                                   <span>Total</span>
                                   <span>${formatPrice(total)}</span>
                               </div>
                           </div>

                           <div class="order-history-actions">
                               <button
                                   type="button"
                                   class="order-history-action-btn primary view-order-details-btn"
                                   data-order-number="${escapeHtml(orderNumber)}"
                               >
                                   View Details
                               </button>

                               <button
                                   type="button"
                                   class="order-history-action-btn primary print-order-receipt-btn"
                                   data-order-number="${escapeHtml(orderNumber)}"
                               >
                                   Print / Save Receipt
                               </button>

                               <button
                                   type="button"
                                   class="order-history-action-btn success reorder-order-btn"
                                   data-order-number="${escapeHtml(orderNumber)}"
                               >
                                   Buy Again
                               </button>

                               <button
                                   type="button"
                                   class="order-history-action-btn secondary send-order-receipt-btn"
                                   data-order-number="${escapeHtml(orderNumber)}"
                               >
                                   Send Receipt
                               </button>
                           </div>
                       </div>
                   `;
               }).join("")}
           </div>
       `;

       document.getElementById("refreshOrderHistoryBtn")?.addEventListener("click", loadOrderHistory);

       container.querySelectorAll(".view-order-details-btn").forEach(button => {
           button.addEventListener("click", () => {
               openOrderDetails(button.dataset.orderNumber || "");
           });
       });

       container.querySelectorAll(".print-order-receipt-btn").forEach(button => {
           button.addEventListener("click", () => {
               const orderNumber = button.dataset.orderNumber || "";
               const order = safeOrders.find(item => getOrderNumber(item) === orderNumber);

               if (!order) {
                   showToast("Unable to find this order receipt.", "error");
                   return;
               }

               printOrderReceipt(order);
           });
       });

       container.querySelectorAll(".send-order-receipt-btn").forEach(button => {
           button.addEventListener("click", () => {
               sendOrderReceipt(button.dataset.orderNumber || "");
           });
       });

       container.querySelectorAll(".reorder-order-btn").forEach(button => {
           button.addEventListener("click", async () => {
               const orderNumber = button.dataset.orderNumber || "";
               const order = safeOrders.find(item => getOrderNumber(item) === orderNumber);

               if (!order) {
                   showToast("Unable to find this order.", "error");
                   return;
               }

               const originalText = button.textContent.trim() || "Buy Again";

               try {
                   button.disabled = true;
                   button.textContent = "Adding...";

                   await reorderFromHistory(order);

                   button.textContent = "Added ✓";
                   button.classList.remove("success");
                   button.classList.add("primary");
               } catch (error) {
                   console.error("Buy Again Error:", error);

                   button.disabled = false;
                   button.textContent = originalText;

                   showToast(
                       error.message || "Unable to add items back to your bag.",
                       "error"
                   );
               }
           });
       });
   }

   async function loadOrderHistory() {
       const container = document.getElementById("orderHistoryContent");
       const refreshBtn = document.getElementById("refreshOrderHistoryBtn");

       if (!container) return;

       if (!getToken()) {
           container.innerHTML = `
               <div class="bag-empty-shell">
                   <div class="bag-empty-icon">🔐</div>
                   <div class="bag-empty-title">Login required</div>
                   <p class="bag-empty-text">Please log in to view your completed checkouts.</p>
               </div>
           `;
           return;
       }

       try {
           if (refreshBtn) {
               refreshBtn.disabled = true;
               refreshBtn.textContent = "Refreshing...";
           }

           container.innerHTML = `<div class="loading-state">Loading order history...</div>`;

           const response = await fetch("/api/v1/orders/history", {
               method: "GET",
               headers: getAuthHeaders({
                   Accept: "application/json"
               })
           });

           await assertAuthorizedResponse(response, "Unable to load order history.");

           const orders = await response.json();
           window.lastLoadedOrders = Array.isArray(orders) ? orders : [];

           renderOrderHistory(window.lastLoadedOrders);
       } catch (error) {
           console.error("Order History Error:", error);

           container.innerHTML = `
               <div class="bag-empty-shell">
                   <div class="bag-empty-icon">⚠️</div>
                   <div class="bag-empty-title">Unable to load orders</div>
                   <p class="bag-empty-text">${escapeHtml(error.message || "Please try again in a moment.")}</p>
               </div>
           `;
       } finally {
           if (refreshBtn) {
               refreshBtn.disabled = false;
               refreshBtn.textContent = "Refresh Orders";
           }
       }
   }

   function closeOrderHistoryDrawer() {
       const orderHistorySidebar = document.getElementById("orderHistorySidebar");

       if (!orderHistorySidebar) return;

       const instance =
           bootstrap.Offcanvas.getInstance(orderHistorySidebar) ||
           new bootstrap.Offcanvas(orderHistorySidebar);

       instance.hide();
   }

   function openBagDrawer() {
       const bagSidebar = document.getElementById("bagSidebar");

       if (!bagSidebar) return;

       const instance =
           bootstrap.Offcanvas.getInstance(bagSidebar) ||
           new bootstrap.Offcanvas(bagSidebar);

       instance.show();
   }

   async function reorderFromHistory(order) {
       const orderNumber = getOrderNumber(order);

       if (!orderNumber) {
           throw new Error("Missing order number.");
       }

       const response = await fetch(`/api/v1/orders/${encodeURIComponent(orderNumber)}/reorder`, {
           method: "POST",
           headers: getAuthHeaders()
       });

       const resultText = await response.text();

       if (!response.ok) {
           const message =
               parseBackendMessage(resultText) ||
               `Buy Again failed with status ${response.status}`;

           throw new Error(message);
       }

       const message =
           parseBackendMessage(resultText) ||
           "Items added back to your bag.";

       showToast(message, "success");

       await loadBag();

       closeOrderHistoryDrawer();

       window.setTimeout(() => {
           openBagDrawer();
       }, 350);
   }

     function clearBagUi(messageTitle = "Bag unavailable", messageText = "Log in to view your current store bag.") {
         const bagContent = document.getElementById("bagContent");
         const bagFooter = document.getElementById("bagFooter");

         lastLoadedBag = null;
         savedRfids = new Set();

         if (bagContent) {
             bagContent.innerHTML = `
                 <div class="bag-empty-shell">
                     <div class="bag-empty-icon">🔐</div>
                     <div class="bag-empty-title">${escapeHtml(messageTitle)}</div>
                     <p class="bag-empty-text">${escapeHtml(messageText)}</p>
                 </div>
             `;
         }

         if (bagFooter) {
             bagFooter.innerHTML = "";
         }
     }

     async function loadBag() {
         const container = document.getElementById("bagContent");
         const footer = document.getElementById("bagFooter");

         if (!container) return;

         if (!getToken()) {
             savedRfids = new Set();
             lastLoadedBag = null;

             container.innerHTML = `
                 <div class="bag-empty-shell">
                     <div class="bag-empty-icon">🔐</div>
                     <div class="bag-empty-title">Login required</div>
                     <p class="bag-empty-text">Please log in to view your style bag.</p>
                 </div>
             `;

             if (footer) {
                 footer.innerHTML = "";
             }

             return;
         }

         container.innerHTML = `<div class="loading-state">Refreshing bag...</div>`;

         if (footer) {
             footer.innerHTML = "";
         }

         try {
             const resp = await fetch(`${API.stylist}/bag`, {
                 headers: getAuthHeaders()
             });

             await assertAuthorizedResponse(resp, `Bag request failed with status ${resp.status}`);

             const bag = await resp.json();
             lastLoadedBag = bag;

             const items = Array.isArray(bag.items) ? bag.items : [];

             savedRfids = new Set(
                 items
                     .map(item => item.rfid || item.itemRfid || item.productRfid)
                     .filter(Boolean)
             );

             if (currentLoadedItem) {
                 if (isCurrentItemSaved(currentLoadedItem)) {
                     setSaveButtonSaved();
                 } else {
                     setSaveButtonDefault(false);
                 }
             }

             if (items.length === 0) {
                 container.innerHTML = `
                     <div class="bag-empty-shell">
                         <div class="bag-empty-icon">🛍️</div>
                         <div class="bag-empty-title">Your style bag is empty</div>
                         <p class="bag-empty-text">
                             Scan items from this store and save them here to build your store-scoped look.
                         </p>
                     </div>
                 `;

                 if (footer) {
                     footer.innerHTML = "";
                 }

                 return;
             }

             const fallbackTotals = calculateCheckoutTotals(items);

             const subtotal = Number.isFinite(Number(bag.subtotal))
                 ? safeNumber(bag.subtotal)
                 : fallbackTotals.subtotal;

             const tax = Number.isFinite(Number(bag.tax))
                 ? safeNumber(bag.tax)
                 : fallbackTotals.tax;

             const total = Number.isFinite(Number(bag.total))
                 ? safeNumber(bag.total)
                 : fallbackTotals.total;

             const itemCards = items.map(item => {
                 const rawId = item.id ?? "";
                 const id = escapeHtml(rawId);

                 const name = escapeHtml(item.itemName || item.name || "Unnamed Item");
                 const retailer = escapeHtml(item.retailerName || item.retailer || "Current Store");
                 const category = escapeHtml(item.category || "Style Item");
                 const vibe = escapeHtml(item.vibe || item.source || "Styled");

                 const quantity = getItemQuantity(item);
                 const unitPrice = safeNumber(item.price);
                 const lineTotal = unitPrice * quantity;

                 const imageUrl = safeImageUrl(
                     item.imageUrl || "",
                     "https://placehold.co/120x120?text=Item"
                 );

                 return `
                     <div class="bag-item-card">
                         <img
                             class="bag-item-img"
                             src="${imageUrl}"
                             alt="${name}"
                             onerror="this.src='https://placehold.co/120x120?text=Item';"
                         />

                         <div class="bag-item-main">
                             <div class="bag-item-retailer">${retailer}</div>
                             <div class="bag-item-name">${name}</div>

                             <div class="bag-item-meta">
                                 <span class="bag-pill">${category}</span>
                                 <span class="bag-pill">${vibe}</span>
                                 <span class="bag-pill">Qty ${quantity}</span>
                                 <span class="bag-price">${formatPrice(lineTotal)}</span>
                                 ${
                                     quantity > 1
                                         ? `<span class="bag-item-unit-price">${formatPrice(unitPrice)} each</span>`
                                         : ""
                                 }
                             </div>

                             <div class="bag-qty-controls" aria-label="Quantity controls for ${name}">
                                 <button
                                     class="bag-qty-btn decrease-bag-qty-btn"
                                     type="button"
                                     data-id="${id}"
                                     data-quantity="${quantity}"
                                     ${quantity <= 1 ? "disabled" : ""}
                                     aria-label="Decrease quantity"
                                 >
                                     −
                                 </button>

                                 <span class="bag-qty-value">${quantity}</span>

                                 <button
                                     class="bag-qty-btn increase-bag-qty-btn"
                                     type="button"
                                     data-id="${id}"
                                     data-quantity="${quantity}"
                                     aria-label="Increase quantity"
                                 >
                                     +
                                 </button>
                             </div>
                         </div>

                         <div class="bag-actions">
                             <button
                                 class="btn btn-outline-danger bag-remove-btn remove-bag-item-btn"
                                 type="button"
                                 data-id="${id}"
                             >
                                 Remove
                             </button>
                         </div>
                     </div>
                 `;
             }).join("");

             container.innerHTML = `
                 <div class="bag-section-label">Saved Pieces</div>
                 <div class="bag-list">${itemCards}</div>
             `;

             if (footer) {
                 footer.innerHTML = `
                     <div class="premium-total-box">
                         <div class="total-kicker">Checkout Summary</div>

                         <div class="total-row">
                             <span>Subtotal</span>
                             <strong>${formatPrice(subtotal)}</strong>
                         </div>

                         <div class="total-row">
                             <span>Estimated Tax</span>
                             <strong>${formatPrice(tax)}</strong>
                         </div>

                         <div class="total-row final">
                             <span>Total</span>
                             <span>${formatPrice(total)}</span>
                         </div>

                         <button
                             class="btn top-button w-100 mt-3"
                             type="button"
                             id="checkoutBagBtn"
                         >
                             Checkout
                         </button>

                         <button
                             class="btn btn-outline-dark w-100 clear-bag-btn mt-2"
                             type="button"
                             id="removeUnavailableBagItemsBtn"
                         >
                             Remove Unavailable Items
                         </button>

                         <button
                             class="btn btn-outline-dark w-100 clear-bag-btn mt-2"
                             type="button"
                             id="clearBagBtn"
                         >
                             Clear Bag
                         </button>
                     </div>
                 `;
             }

             container.querySelectorAll(".remove-bag-item-btn").forEach(button => {
                 button.addEventListener("click", () => {
                     removeFromBag(button.dataset.id || "");
                 });
             });

             container.querySelectorAll(".decrease-bag-qty-btn").forEach(button => {
                 button.addEventListener("click", () => {
                     const id = button.dataset.id || "";
                     const currentQuantity = safeNumber(button.dataset.quantity);
                     const nextQuantity = Math.max(1, currentQuantity - 1);

                     updateBagItemQuantity(id, nextQuantity, button);
                 });
             });

             container.querySelectorAll(".increase-bag-qty-btn").forEach(button => {
                 button.addEventListener("click", () => {
                     const id = button.dataset.id || "";
                     const currentQuantity = safeNumber(button.dataset.quantity);
                     const nextQuantity = currentQuantity + 1;

                     updateBagItemQuantity(id, nextQuantity, button);
                 });
             });

             document.getElementById("removeUnavailableBagItemsBtn")?.addEventListener("click", removeUnavailableBagItems);
             document.getElementById("clearBagBtn")?.addEventListener("click", clearBag);
             document.getElementById("checkoutBagBtn")?.addEventListener("click", openCheckoutModal);
         } catch (error) {
             console.error("Bag Load Error:", error);

             lastLoadedBag = null;

             container.innerHTML = `
                 <div class="bag-empty-shell">
                     <div class="bag-empty-icon">⚠️</div>
                     <div class="bag-empty-title">Unable to load bag</div>
                     <p class="bag-empty-text">${escapeHtml(error.message || "Please try again in a moment.")}</p>
                 </div>
             `;

             if (footer) {
                 footer.innerHTML = "";
             }
         }
     }

    async function loadAnalyticsSummary() {
        const container = document.getElementById("analyticsSummary");
        if (!container) return;

        container.innerHTML = `
            <div class="summary-card">
                <div class="summary-label">Loading</div>
                <div class="summary-value">Syncing...</div>
            </div>
        `;

        try {
            requireToken();

            const resp = await fetch(`${API.stylist}/admin/summary`, {
                headers: getAuthHeaders()
            });

            await assertAuthorizedResponse(resp, `Summary request failed with status ${resp.status}`);
            const summary = await resp.json();

            container.innerHTML = `
                <div class="summary-card">
                    <div class="summary-label">Total Scans</div>
                    <div class="summary-value">${Number(summary.totalScans ?? 0)}</div>
                </div>
                <div class="summary-card">
                    <div class="summary-label">Total Saves</div>
                    <div class="summary-value">${Number(summary.totalSaves ?? 0)}</div>
                </div>
                <div class="summary-card">
                    <div class="summary-label">Conversion Rate</div>
                    <div class="summary-value">${Number(summary.conversionRate ?? 0).toFixed(2)}%</div>
                </div>
                <div class="summary-card">
                    <div class="summary-label">Top Retailer</div>
                    <div class="summary-value">${escapeHtml(summary.topRetailer || "N/A")}</div>
                </div>
                <div class="summary-card">
                    <div class="summary-label">Top Scanned Item</div>
                    <div class="summary-value">${escapeHtml(summary.topScannedItem || "N/A")}</div>
                </div>
                <div class="summary-card">
                    <div class="summary-label">Top Saved Item</div>
                    <div class="summary-value">${escapeHtml(summary.topSavedItem || "N/A")}</div>
                </div>
            `;
        } catch (error) {
            console.error("Analytics Summary Error:", error);
            container.innerHTML = `
                <div class="summary-card">
                    <div class="summary-label">Error</div>
                    <div class="summary-value">Unable to load summary</div>
                </div>
            `;
        }
    }

    async function fetchRetailerData() {
        requireToken();

        const resp = await fetch(`${API.stylist}/admin/retailers`, {
            headers: getAuthHeaders()
        });

        await assertAuthorizedResponse(resp, `Retailer stats failed: ${resp.status}`);
        const data = await resp.json();
        return Array.isArray(data) ? data : [];
    }

    async function loadRetailerStats() {
        const container = document.getElementById("retailerStats");
        if (!container) return;

        container.innerHTML = `<div class="text-light opacity-75 small">Loading retailer stats...</div>`;

        try {
            const data = await fetchRetailerData();

            if (data.length === 0) {
                container.innerHTML = `<div class="text-light opacity-75 small">No retailer data.</div>`;
                return;
            }

            const rows = data.map(r => `
                <tr>
                    <td>${escapeHtml(r.retailer)}</td>
                    <td>${Number(r.scans || 0)}</td>
                    <td>${Number(r.saves || 0)}</td>
                    <td>${Number(r.conversionRate ?? 0).toFixed(2)}%</td>
                </tr>
            `).join("");

            container.innerHTML = `
                <div class="activity-table-shell">
                    <table class="activity-table">
                        <thead>
                            <tr>
                                <th>Retailer</th>
                                <th>Scans</th>
                                <th>Saves</th>
                                <th>Conversion</th>
                            </tr>
                        </thead>
                        <tbody>${rows}</tbody>
                    </table>
                </div>
            `;
        } catch (err) {
            console.error("Retailer Stats Error:", err);
            container.innerHTML = `<div class="text-danger small">Failed to load retailer stats</div>`;
        }
    }

    async function loadRetailerChart() {
        const container = document.getElementById("retailerChart");
        if (!container) return;

        container.innerHTML = `<div class="text-light opacity-75 small">Loading chart...</div>`;

        try {
            const data = await fetchRetailerData();

            if (data.length === 0) {
                container.innerHTML = `<div class="text-light opacity-75 small">No data available.</div>`;
                return;
            }

            const max = Math.max(
                ...data.map(r => Math.max(Number(r.scans || 0), Number(r.saves || 0))),
                1
            );

            let html = "";
            data.forEach(r => {
                const scans = Number(r.scans || 0);
                const saves = Number(r.saves || 0);
                const scanWidth = (scans / max) * 100;
                const saveWidth = (saves / max) * 100;

                html += `
                    <div class="chart-row">
                        <div class="chart-header">
                            <div class="chart-label">${escapeHtml(r.retailer)}</div>
                            <div class="chart-meta">
                                ${scans} scans • ${saves} saves • ${Number(r.conversionRate || 0).toFixed(2)}%
                            </div>
                        </div>

                        <div class="chart-bars">
                            <div class="chart-bar-track">
                                <div class="chart-bar-scan" style="width:${scanWidth}%"></div>
                            </div>

                            <div class="chart-bar-track mt-1">
                                <div class="chart-bar-save" style="width:${saveWidth}%"></div>
                            </div>
                        </div>
                    </div>
                `;
            });

            container.innerHTML = html + `
                <div class="chart-legend mt-3">
                    <div class="legend-item"><span class="legend-dot legend-scan"></span> Scans</div>
                    <div class="legend-item"><span class="legend-dot legend-save"></span> Saves</div>
                </div>
            `;
        } catch (err) {
            console.error("Retailer Chart Error:", err);
            container.innerHTML = `<div class="text-danger small">Chart failed</div>`;
        }
    }

    async function fetchActivityData() {
        requireToken();

        const eventType = document.getElementById("activityEventFilter")?.value || "ALL";
        const retailer = document.getElementById("activityRetailerFilter")?.value || "ALL";

        const resp = await fetch(
            `${API.stylist}/admin/activity?eventType=${encodeURIComponent(eventType)}&retailer=${encodeURIComponent(retailer)}`,
            { headers: getAuthHeaders() }
        );

        await assertAuthorizedResponse(resp, `Activity request failed with status ${resp.status}`);
        const data = await resp.json();
        return Array.isArray(data) ? data : [];
    }

    async function loadActivity() {
        const container = document.getElementById("activityFeed");
        if (!container) return;

        container.innerHTML = `<div class="text-light opacity-75 small">Loading activity...</div>`;

        try {
            const data = await fetchActivityData();

            if (data.length === 0) {
                container.innerHTML = `<div class="text-light opacity-75 small">No matching activity.</div>`;
                return;
            }

            let html = "";
            data.forEach(item => {
                const icon = item.eventType === "SCAN" ? "🟢" : "🔵";
                html += `
                    <div class="activity-row">
                        <div>
                            <div class="activity-main">${icon} <strong>${escapeHtml(item.eventType)}</strong> ${escapeHtml(item.item)}</div>
                            <div class="store-label mt-1">${escapeHtml(item.retailer)}</div>
                        </div>
                        <div class="activity-meta">${escapeHtml(item.timeAgo || "Just now")}</div>
                    </div>
                `;
            });

            container.innerHTML = html;
        } catch (error) {
            console.error("Activity Feed Error:", error);
            container.innerHTML = `<div class="text-danger small">Failed to load activity.</div>`;
        }
    }

    function formatTimestamp(rawTimestamp) {
        if (!rawTimestamp) return "N/A";

        const date = new Date(rawTimestamp);
        if (Number.isNaN(date.getTime())) return String(rawTimestamp);

        return date.toLocaleString();
    }

    async function loadActivityTable() {
        const container = document.getElementById("activityTableContainer");
        if (!container) return;

        container.innerHTML = `<div class="text-light opacity-75 small p-3">Loading recent activity...</div>`;

        try {
            const data = await fetchActivityData();

            if (data.length === 0) {
                container.innerHTML = `<div class="text-light opacity-75 small p-3">No matching activity.</div>`;
                return;
            }

            const rows = data.map(item => {
                const pillClass = item.eventType === "SCAN" ? "event-pill event-scan" : "event-pill event-save";
                const exactTime = formatTimestamp(item.createdAt);

                return `
                    <tr>
                        <td><span class="${pillClass}">${escapeHtml(item.eventType)}</span></td>
                        <td>${escapeHtml(item.retailer)}</td>
                        <td>${escapeHtml(item.item)}</td>
                        <td>${escapeHtml(exactTime)}</td>
                    </tr>
                `;
            }).join("");

            container.innerHTML = `
                <table class="activity-table">
                    <thead>
                        <tr>
                            <th>Event</th>
                            <th>Retailer</th>
                            <th>Item</th>
                            <th>Timestamp</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            `;
        } catch (error) {
            console.error("Activity Table Error:", error);
            container.innerHTML = `<div class="text-danger small p-3">Failed to load activity table.</div>`;
        }
    }

    async function loadTrends() {
        const container = document.getElementById("trendContent");
        if (!container) return;

        container.innerHTML = `<div class="text-light opacity-75 small">Syncing live analytics...</div>`;

        try {
            requireToken();

            const resp = await fetch(`${API.stylist}/admin/trends`, {
                headers: getAuthHeaders()
            });

            await assertAuthorizedResponse(resp, `Trend request failed with status ${resp.status}`);
            const trendList = await resp.json();

            if (!Array.isArray(trendList) || trendList.length === 0) {
                container.innerHTML = `<div class="text-light opacity-75 small">No trend data yet.</div>`;
                return;
            }

            let html = "";

            for (let i = 0; i < trendList.length; i += 1) {
                const trend = trendList[i];
                const store = escapeHtml(trend.store || "Retailer");
                const item = escapeHtml(trend.item || "Product");
                const count = Number(trend.count) || 0;
                const saveLabel = count === 1 ? "Save" : "Saves";

                html += `
                    <div class="trend-row d-flex justify-content-between align-items-center gap-3">
                        <div class="d-flex align-items-start flex-grow-1">
                            <div class="trend-rank">#${i + 1}</div>
                            <div>
                                <div class="store-label">${store}</div>
                                <div class="trend-item">${item}</div>
                            </div>
                        </div>
                        <span class="trend-badge">${count} ${saveLabel}</span>
                    </div>
                `;
            }

            container.innerHTML = html;
        } catch (error) {
            console.error("Dashboard Render Error:", error);
            container.innerHTML = `<span class="text-danger small">Sync error: ${escapeHtml(error.message)}</span>`;
        }
    }

    async function loadAllInsights() {
        await Promise.allSettled([
            loadAnalyticsSummary(),
            loadRetailerStats(),
            loadRetailerChart(),
            loadActivity(),
            loadActivityTable(),
            loadTrends()
        ]);
    }

    function startActivityRefresh() {
        if (activityRefreshStarted) return;
        activityRefreshStarted = true;

        setInterval(() => {
            const panel = document.getElementById("adminPanel");
            if (panel && panel.classList.contains("show") && getToken()) {
                loadAllInsights();
            }
        }, 5000);
    }

    let lastMerchantSales = [];
    let merchantSalesSearchTerm = "";
    let merchantSalesStatusFilter = "";

    function normalizeMerchantSaleOrder(order) {
        if (!order || typeof order !== "object") {
            return null;
        }

        const items = getMerchantOrderItems(order);
        const orderNumber = String(getMerchantOrderNumber(order) || "").trim();

        if (!orderNumber) {
            return null;
        }

        const subtotal = safeNumber(order.subtotal);
        const tax = safeNumber(order.tax);
        const total = safeNumber(
            order.total ||
            order.grandTotal ||
            order.orderTotal ||
            subtotal + tax
        );

        const itemCount = safeNumber(
            order.itemCount ||
            order.totalItems ||
            items.reduce((sum, item) => {
                return sum + Math.max(1, safeNumber(item.quantity || item.qty || 1));
            }, 0)
        );

        return {
            ...order,
            orderNumber,
            createdAt: getMerchantOrderDate(order),
            status: getMerchantOrderStatus(order),
            items,
            subtotal,
            tax,
            total,
            itemCount
        };
    }

    function openMerchantSaleDetails(orderNumber) {
        const safeOrderNumber = String(orderNumber || "").trim();

        if (!safeOrderNumber) {
            showToast("Missing receipt number.", "error");
            return;
        }

        const order =
            lastMerchantSales.find(item => String(item.orderNumber) === safeOrderNumber) ||
            (Array.isArray(window.lastMerchantSalesOrders)
                ? window.lastMerchantSalesOrders
                    .map(normalizeMerchantSaleOrder)
                    .filter(Boolean)
                    .find(item => String(item.orderNumber) === safeOrderNumber)
                : null);

        if (!order) {
            openMerchantSaleInOrderHistory(safeOrderNumber);
            return;
        }

        const modalEl = document.getElementById("merchantSaleDetailsModal");
        const bodyEl = document.getElementById("merchantSaleDetailsBody");
        const titleEl = document.getElementById("merchantSaleDetailsModalLabel");

        if (!modalEl || !bodyEl) {
            openMerchantSaleInOrderHistory(safeOrderNumber);
            return;
        }

        const items = Array.isArray(order.items) ? order.items : [];
        const subtotal = safeNumber(order.subtotal);
        const tax = safeNumber(order.tax);
        const total = safeNumber(order.total);
        const itemCount = safeNumber(order.itemCount || items.length);

        if (titleEl) {
            titleEl.textContent = `Receipt ${safeOrderNumber}`;
        }

        bodyEl.innerHTML = `
            <div class="merchant-sale-detail-hero">
                <div>
                    <div class="merchant-sale-detail-kicker">Store Checkout</div>
                    <div class="merchant-sale-detail-receipt">
                        Receipt ${escapeHtml(safeOrderNumber)}
                    </div>
                    <div class="merchant-sale-detail-meta">
                        ${escapeHtml(formatOrderDate(order.createdAt))} • ${escapeHtml(order.status)}
                    </div>
                </div>

                <div class="merchant-sale-detail-total">
                    ${formatPrice(total)}
                </div>
            </div>

            <div class="merchant-sale-detail-grid">
                <div class="merchant-sale-detail-stat">
                    <div class="merchant-sale-detail-stat-label">Status</div>
                    <div class="merchant-sale-detail-stat-value">${escapeHtml(order.status)}</div>
                </div>

                <div class="merchant-sale-detail-stat">
                    <div class="merchant-sale-detail-stat-label">Items</div>
                    <div class="merchant-sale-detail-stat-value">${itemCount}</div>
                </div>

                <div class="merchant-sale-detail-stat">
                    <div class="merchant-sale-detail-stat-label">Total</div>
                    <div class="merchant-sale-detail-stat-value">${formatPrice(total)}</div>
                </div>
            </div>

            <div class="section-kicker mb-2">Purchased Items</div>

            <div class="merchant-sale-detail-items">
                ${
                    items.length
                        ? items.map(item => {
                            const name = item.itemName || item.name || item.productName || "Purchased Item";
                            const retailer = item.retailerName || item.retailer || order.retailerName || "Retailer";
                            const category = item.category || "Style Item";
                            const quantity = getItemQuantity(item);
                            const price = safeNumber(item.lineTotal ?? item.unitPrice ?? item.price);
                            const imageUrl = safeImageUrl(
                                item.imageUrl || item.image_url || item.image || "",
                                "https://placehold.co/76x76?text=Item"
                            );

                            return `
                                <div class="merchant-sale-detail-item">
                                    <img
                                        src="${imageUrl}"
                                        alt="${escapeHtml(name)}"
                                        class="merchant-sale-detail-img"
                                        onerror="this.src='https://placehold.co/76x76?text=Item';"
                                    />

                                    <div class="merchant-sale-detail-item-main">
                                        <div class="merchant-sale-detail-item-name">${escapeHtml(name)}</div>
                                        <div class="merchant-sale-detail-item-meta">
                                            ${escapeHtml(retailer)} • ${escapeHtml(category)} • Qty ${quantity}
                                        </div>
                                    </div>

                                    <div class="merchant-sale-detail-item-price">
                                        ${formatPrice(price)}
                                    </div>
                                </div>
                            `;
                        }).join("")
                        : `
                            <div class="merchant-sales-empty">
                                No line items were returned for this receipt.
                            </div>
                        `
                }
            </div>

            <div class="merchant-sale-detail-summary">
                <div class="merchant-sale-detail-summary-row">
                    <span>Subtotal</span>
                    <strong>${formatPrice(subtotal)}</strong>
                </div>

                <div class="merchant-sale-detail-summary-row">
                    <span>Estimated Tax</span>
                    <strong>${formatPrice(tax)}</strong>
                </div>

                <div class="merchant-sale-detail-summary-row final">
                    <span>Total</span>
                    <span>${formatPrice(total)}</span>
                </div>
            </div>
        `;

        document.getElementById("printMerchantSaleReceiptBtn")?.addEventListener("click", () => {
            printOrderReceipt(order);
        }, { once: true });

        const modal =
            bootstrap.Modal.getInstance(modalEl) ||
            new bootstrap.Modal(modalEl);

        modal.show();
    }

    function getFilteredMerchantSales(orders) {
        const safeOrders = Array.isArray(orders) ? orders : [];
        const search = String(merchantSalesSearchTerm || "").trim().toLowerCase();
        const statusFilter = String(merchantSalesStatusFilter || "ALL").trim().toUpperCase();

        return safeOrders.filter(order => {
            const orderNumber = String(order.orderNumber || "").toLowerCase();
            const status = String(order.status || "").toUpperCase();
            const items = Array.isArray(order.items) ? order.items : [];

            const searchableText = [
                orderNumber,
                status,
                order.retailerName,
                order.storeName,
                ...items.flatMap(item => [
                    item.itemName,
                    item.name,
                    item.productName,
                    item.rfid,
                    item.category,
                    item.brand
                ])
            ]
                .filter(Boolean)
                .join(" ")
                .toLowerCase();

            const matchesSearch = !search || searchableText.includes(search);
            const matchesStatus = statusFilter === "ALL" || status === statusFilter;

            return matchesSearch && matchesStatus;
        });
    }

    function renderMerchantSalesFilterBar() {
        return `
            <div class="merchant-sales-filter-bar">
                <input
                    id="merchantSalesSearchInput"
                    class="form-control merchant-sales-search-input"
                    type="search"
                    placeholder="Search receipt, item, brand, RFID..."
                    value="${escapeHtml(merchantSalesSearchTerm)}"
                />

                <select
                    id="merchantSalesStatusFilter"
                    class="form-select merchant-sales-status-select"
                >
                    <option value="ALL" ${merchantSalesStatusFilter === "ALL" ? "selected" : ""}>All Statuses</option>
                    <option value="COMPLETED" ${merchantSalesStatusFilter === "COMPLETED" ? "selected" : ""}>Completed</option>
                    <option value="CANCELLED" ${merchantSalesStatusFilter === "CANCELLED" ? "selected" : ""}>Cancelled</option>
                    <option value="RETURNED" ${merchantSalesStatusFilter === "RETURNED" ? "selected" : ""}>Returned</option>
                    <option value="REFUNDED" ${merchantSalesStatusFilter === "REFUNDED" ? "selected" : ""}>Refunded</option>
                </select>
            </div>
        `;
    }

 function renderMerchantSalesActivityList(orders) {
     const container = document.getElementById("merchantSalesActivityList");

     if (!container) {
         return;
     }

     const normalizedOrders = Array.isArray(orders)
         ? orders
             .map(normalizeMerchantSaleOrder)
             .filter(Boolean)
         : [];

     lastMerchantSales = normalizedOrders;

     const searchTerm = String(merchantSalesSearchTerm || "").trim().toLowerCase();
     const statusFilter = String(merchantSalesStatusFilter || "").trim().toUpperCase();

     const filteredOrders = normalizedOrders.filter(order => {
         const status = String(order.status || "").trim().toUpperCase();

         const searchableText = [
             order.orderNumber,
             order.status,
             order.total,
             order.createdAt,
             ...(order.items || []).map(item => item.itemName || item.name || item.productName || ""),
             ...(order.items || []).map(item => item.category || ""),
             ...(order.items || []).map(item => item.rfid || "")
         ]
             .filter(Boolean)
             .join(" ")
             .toLowerCase();

         const matchesSearch = !searchTerm || searchableText.includes(searchTerm);
         const matchesStatus = !statusFilter || status === statusFilter;

         return matchesSearch && matchesStatus;
     });

     const filterBar = `
         <div class="merchant-sales-filter-bar">
             <input
                 id="merchantSalesSearchInput"
                 class="form-control merchant-sales-search-input"
                 type="search"
                 placeholder="Search receipt, item, RFID, status..."
                 value="${escapeHtml(merchantSalesSearchTerm)}"
             />

             <select
                 id="merchantSalesStatusFilter"
                 class="form-select merchant-sales-status-select"
             >
                 <option value="" ${!merchantSalesStatusFilter ? "selected" : ""}>All statuses</option>
                 <option value="COMPLETED" ${merchantSalesStatusFilter === "COMPLETED" ? "selected" : ""}>Completed</option>
                 <option value="CANCELLED" ${merchantSalesStatusFilter === "CANCELLED" ? "selected" : ""}>Cancelled</option>
                 <option value="RETURNED" ${merchantSalesStatusFilter === "RETURNED" ? "selected" : ""}>Returned</option>
                 <option value="REFUNDED" ${merchantSalesStatusFilter === "REFUNDED" ? "selected" : ""}>Refunded</option>
             </select>
         </div>
     `;

     if (!normalizedOrders.length) {
         container.innerHTML = `
             ${filterBar}
             <div class="merchant-sales-empty">
                 No recent checkouts for this store yet.
             </div>
         `;
         bindMerchantSalesFilters();
         return;
     }

     if (!filteredOrders.length) {
         container.innerHTML = `
             ${filterBar}
             <div class="merchant-sales-empty">
                 No receipts matched your filters.
             </div>
         `;
         bindMerchantSalesFilters();
         return;
     }

     const recentOrders = filteredOrders.slice(0, 8);

     container.innerHTML = `
         ${filterBar}

         ${recentOrders.map(order => {
             const orderNumber = order.orderNumber;
             const createdAt = order.createdAt;
             const status = order.status;
             const items = Array.isArray(order.items) ? order.items : [];
             const total = safeNumber(order.total);
             const itemCount = safeNumber(order.itemCount || items.length);

             const itemPills = items.length
                 ? items.slice(0, 4).map(item => {
                     const name =
                         item.itemName ||
                         item.name ||
                         item.productName ||
                         "Item";

                     const quantity = safeNumber(item.quantity || item.qty || 1);

                     return `
                         <span class="merchant-sale-item-pill" title="${escapeHtml(name)}">
                             ${escapeHtml(name)}${quantity > 1 ? ` × ${quantity}` : ""}
                         </span>
                     `;
                 }).join("")
                 : `<span class="merchant-sale-item-pill">No line items</span>`;

             const extraCount = Math.max(0, items.length - 4);

             return `
                 <button
                     type="button"
                     class="merchant-sale-row"
                     data-order-number="${escapeHtml(orderNumber)}"
                     aria-label="Open receipt ${escapeHtml(orderNumber)}"
                 >
                     <div class="merchant-sale-main">
                         <div class="merchant-sale-order merchant-sale-receipt">
                             Receipt ${escapeHtml(orderNumber)}
                         </div>

                         <div class="merchant-sale-meta">
                             ${escapeHtml(formatOrderDate(createdAt))} • ${escapeHtml(status)} • ${itemCount} item${itemCount === 1 ? "" : "s"}
                         </div>

                         <div class="merchant-sale-items">
                             ${itemPills}
                             ${
                                 extraCount > 0
                                     ? `<span class="merchant-sale-item-pill">+${extraCount} more</span>`
                                     : ""
                             }
                         </div>
                     </div>

                     <div class="merchant-sale-total">
                         ${formatPrice(total)}
                     </div>
                 </button>
             `;
         }).join("")}
     `;

     bindMerchantSalesFilters();

     container.querySelectorAll(".merchant-sale-row").forEach(row => {
         row.addEventListener("click", () => {
             openMerchantSaleDetails(row.dataset.orderNumber || "");
         });

         row.addEventListener("keydown", event => {
             if (event.key === "Enter" || event.key === " ") {
                 event.preventDefault();
                 openMerchantSaleDetails(row.dataset.orderNumber || "");
             }
         });
     });
 }

 function bindMerchantSalesFilters() {
     const searchInput = document.getElementById("merchantSalesSearchInput");
     const statusSelect = document.getElementById("merchantSalesStatusFilter");

     if (searchInput) {
         searchInput.addEventListener("input", event => {
             merchantSalesSearchTerm = event.target.value || "";
             renderMerchantSalesActivityList(lastMerchantSales);
         });
     }

     if (statusSelect) {
         statusSelect.addEventListener("change", event => {
             merchantSalesStatusFilter = event.target.value || "";
             renderMerchantSalesActivityList(lastMerchantSales);
         });
     }
 }

 async function loadMerchantSalesActivity() {
     const container = document.getElementById("merchantSalesActivityList");
     const refreshBtn = document.getElementById("refreshMerchantSalesBtn");

     if (!container) {
         return;
     }

     if (!getToken()) {
         renderMerchantSalesSummary({});

         container.innerHTML = `
             <div class="merchant-sales-empty">
                 Login required to view sales activity.
             </div>
         `;
         return;
     }

     try {
         requireToken();

         if (refreshBtn) {
             refreshBtn.disabled = true;
             refreshBtn.textContent = "Refreshing...";
         }

         container.innerHTML = `
             <div class="merchant-sales-empty">
                 Loading merchant dashboard analytics...
             </div>
         `;

         const response = await fetch(MERCHANT_SALES_DASHBOARD_API, {
             method: "GET",
             headers: getAuthHeaders({
                 Accept: "application/json"
             })
         });

         await assertAuthorizedResponse(response, "Unable to load merchant dashboard analytics.");

         const dashboard = await response.json();
         const recentOrders = getDashboardOrders(dashboard);

         window.lastMerchantSalesDashboard = dashboard;
         window.lastMerchantSalesOrders = recentOrders;

         renderMerchantSalesSummary(dashboard);
         renderMerchantSalesActivityList(recentOrders);
     } catch (error) {
         console.error("Merchant Sales Dashboard Error:", error);

         renderMerchantSalesSummary({});

         container.innerHTML = `
             <div class="merchant-sales-empty">
                 ${escapeHtml(error.message || "Unable to load merchant dashboard analytics.")}
             </div>
         `;
     } finally {
         if (refreshBtn) {
             refreshBtn.disabled = false;
             refreshBtn.textContent = "Refresh Sales";
         }
     }
 }

    function buildReceiptPrintHtml(order) {
        const orderNumber = getOrderNumber(order) || order?.orderNumber || "Receipt";
        const createdAt = getOrderCreatedAt(order) || order?.createdAt || "";
        const status = getOrderStatus(order) || order?.status || "COMPLETED";
        const items = getOrderItems(order);

        const subtotal = safeNumber(order?.subtotal);
        const tax = safeNumber(order?.tax);
        const total = safeNumber(order?.total);
        const itemCount = safeNumber(order?.itemCount || items.length);

        const rows = items.length
            ? items.map(item => {
                const name = item.itemName || item.name || item.productName || "Purchased Item";
                const retailer = item.retailerName || item.retailer || order.retailerName || "Retailer";
                const category = item.category || "Style Item";
                const quantity = getItemQuantity(item);
                const unitPrice = safeNumber(item.unitPrice ?? item.price);
                const lineTotal = safeNumber(item.lineTotal ?? unitPrice * quantity);

                return `
                    <tr>
                        <td>
                            <strong>${escapeHtml(name)}</strong>
                            <div class="muted">${escapeHtml(retailer)} • ${escapeHtml(category)}</div>
                        </td>
                        <td class="center">${quantity}</td>
                        <td class="right">${formatPrice(unitPrice)}</td>
                        <td class="right">${formatPrice(lineTotal)}</td>
                    </tr>
                `;
            }).join("")
            : `
                <tr>
                    <td colspan="4" class="center muted">No line items were returned for this order.</td>
                </tr>
            `;

        return `
            <!doctype html>
            <html lang="en">
            <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
                <title>Receipt ${escapeHtml(orderNumber)}</title>

                <style>
                    * {
                        box-sizing: border-box;
                    }

                    body {
                        margin: 0;
                        padding: 32px;
                        color: #111827;
                        background: #ffffff;
                        font-family: Arial, Helvetica, sans-serif;
                    }

                    .receipt {
                        max-width: 760px;
                        margin: 0 auto;
                        border: 1px solid #e5e7eb;
                        border-radius: 18px;
                        overflow: hidden;
                    }

                    .header {
                        padding: 28px;
                        color: #ffffff;
                        background: #0f172a;
                    }

                    .kicker {
                        margin-bottom: 8px;
                        color: #cbd5e1;
                        font-size: 11px;
                        font-weight: 800;
                        letter-spacing: 0.16em;
                        text-transform: uppercase;
                    }

                    h1 {
                        margin: 0;
                        font-size: 26px;
                        line-height: 1.2;
                    }

                    .meta {
                        margin-top: 8px;
                        color: #cbd5e1;
                        font-size: 14px;
                        font-weight: 700;
                    }

                    .summary {
                        display: grid;
                        grid-template-columns: repeat(3, 1fr);
                        gap: 12px;
                        padding: 20px 28px;
                        background: #f8fafc;
                        border-bottom: 1px solid #e5e7eb;
                    }

                    .box {
                        padding: 14px;
                        border: 1px solid #e5e7eb;
                        border-radius: 14px;
                        background: #ffffff;
                    }

                    .label {
                        margin-bottom: 6px;
                        color: #64748b;
                        font-size: 10px;
                        font-weight: 900;
                        letter-spacing: 0.14em;
                        text-transform: uppercase;
                    }

                    .value {
                        font-size: 16px;
                        font-weight: 900;
                    }

                    .content {
                        padding: 28px;
                    }

                    table {
                        width: 100%;
                        border-collapse: collapse;
                    }

                    th {
                        padding: 12px 10px;
                        color: #64748b;
                        border-bottom: 1px solid #e5e7eb;
                        font-size: 11px;
                        font-weight: 900;
                        letter-spacing: 0.12em;
                        text-align: left;
                        text-transform: uppercase;
                    }

                    td {
                        padding: 14px 10px;
                        border-bottom: 1px solid #f1f5f9;
                        font-size: 14px;
                        vertical-align: top;
                    }

                    .muted {
                        margin-top: 4px;
                        color: #64748b;
                        font-size: 12px;
                        font-weight: 600;
                    }

                    .center {
                        text-align: center;
                    }

                    .right {
                        text-align: right;
                    }

                    .totals {
                        width: 320px;
                        margin-left: auto;
                        margin-top: 24px;
                        padding: 18px;
                        color: #ffffff;
                        background: #0f172a;
                        border-radius: 16px;
                    }

                    .total-row {
                        display: flex;
                        justify-content: space-between;
                        gap: 16px;
                        padding: 8px 0;
                        font-size: 14px;
                        font-weight: 800;
                    }

                    .total-row.final {
                        margin-top: 8px;
                        padding-top: 14px;
                        border-top: 1px solid rgba(255,255,255,0.22);
                        font-size: 18px;
                    }

                    .footer {
                        padding: 18px 28px 28px;
                        color: #64748b;
                        font-size: 12px;
                        font-weight: 700;
                        text-align: center;
                    }

                    @media print {
                        body {
                            padding: 0;
                        }

                        .receipt {
                            border: none;
                            border-radius: 0;
                        }
                    }
                </style>
            </head>

            <body>
                <main class="receipt">
                    <section class="header">
                        <div class="kicker">Universal Stylist Sales Receipt</div>
                        <h1>Receipt ${escapeHtml(orderNumber)}</h1>
                        <div class="meta">
                            ${escapeHtml(formatOrderDate(createdAt))} • ${escapeHtml(status)}
                        </div>
                    </section>

                    <section class="summary">
                        <div class="box">
                            <div class="label">Status</div>
                            <div class="value">${escapeHtml(status)}</div>
                        </div>

                        <div class="box">
                            <div class="label">Items</div>
                            <div class="value">${itemCount}</div>
                        </div>

                        <div class="box">
                            <div class="label">Total</div>
                            <div class="value">${formatPrice(total)}</div>
                        </div>
                    </section>

                    <section class="content">
                        <table>
                            <thead>
                                <tr>
                                    <th>Item</th>
                                    <th class="center">Qty</th>
                                    <th class="right">Unit</th>
                                    <th class="right">Line Total</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${rows}
                            </tbody>
                        </table>

                        <section class="totals">
                            <div class="total-row">
                                <span>Subtotal</span>
                                <span>${formatPrice(subtotal)}</span>
                            </div>

                            <div class="total-row">
                                <span>Estimated Tax</span>
                                <span>${formatPrice(tax)}</span>
                            </div>

                            <div class="total-row final">
                                <span>Total</span>
                                <span>${formatPrice(total)}</span>
                            </div>
                        </section>
                    </section>

                    <footer class="footer">
                        Thank you for shopping with Universal Stylist.
                    </footer>
                </main>
            </body>
            </html>
        `;
    }

  function printOrderReceipt(order) {
      if (!order || typeof order !== "object") {
          showToast("No receipt data available to print.", "error");
          return;
      }

      const printHtml = buildReceiptPrintHtml(order);
      const printWindow = window.open("", "_blank", "width=900,height=1100");

      if (!printWindow) {
          showToast("Popup blocked. Allow popups for localhost, then try again.", "error");
          return;
      }

      printWindow.document.open();
      printWindow.document.write(printHtml);
      printWindow.document.close();

      const runPrint = () => {
          try {
              printWindow.focus();
              printWindow.print();

              window.setTimeout(() => {
                  if (!printWindow.closed) {
                      printWindow.close();
                  }
              }, 1000);
          } catch (error) {
              console.error("Print Receipt Error:", error);
              showToast("Unable to open print dialog.", "error");
          }
      };

      if (printWindow.document.readyState === "complete") {
          window.setTimeout(runPrint, 350);
      } else {
          printWindow.addEventListener("load", () => {
              window.setTimeout(runPrint, 350);
          });

          window.setTimeout(runPrint, 900);
      }
  }

    /* =========================================================
       MIRROR MODE PAGE LINK
       ========================================================= */

   function openMirrorPage() {
     const storeNameText =
       document.getElementById("currentStoreName")?.textContent?.trim() || "";

     const storeCodeText =
       document.getElementById("currentStoreCode")?.textContent?.trim() || "";

     let retailerKey = "";
     let storeCode = "";

     // Expected text example:
     // MCS245 • MCS245-KINGS-BOUTIQUE
     if (storeCodeText.includes("•")) {
       const parts = storeCodeText.split("•").map(part => part.trim());
       retailerKey = parts[0] || "";
       storeCode = parts[1] || "";
     } else {
       storeCode = storeCodeText;
     }

     // Fallbacks in case your app already stores these somewhere else
     retailerKey =
       retailerKey ||
       localStorage.getItem("retailerKey") ||
       localStorage.getItem("currentRetailerKey") ||
       localStorage.getItem("selectedRetailerKey") ||
       "";

     storeCode =
       storeCode ||
       localStorage.getItem("storeCode") ||
       localStorage.getItem("currentStoreCode") ||
       localStorage.getItem("selectedStoreCode") ||
       "";

     const vibe = document.getElementById("vibeSelect")?.value || "Casual";

     // Save context so mirror.html can still recover it after refresh
     if (retailerKey) {
       localStorage.setItem("retailerKey", retailerKey);
       localStorage.setItem("currentRetailerKey", retailerKey);
     }

     if (storeCode) {
       localStorage.setItem("storeCode", storeCode);
       localStorage.setItem("currentStoreCode", storeCode);
     }

     if (storeNameText) {
       localStorage.setItem("currentStoreName", storeNameText);
     }

     const params = new URLSearchParams();

     if (retailerKey) {
       params.set("retailer", retailerKey);
     }

     if (storeCode) {
       params.set("storeCode", storeCode);
     }

     if (storeNameText) {
       params.set("storeName", storeNameText);
     }

     if (vibe) {
       params.set("vibe", vibe);
     }

     window.location.href = `/mirror.html?${params.toString()}`;
   }

    function bindEvents() {
        document.getElementById("loginBtn")?.addEventListener("click", login);
        document.getElementById("logoutBtn")?.addEventListener("click", logout);
        document.getElementById("signupBtn")?.addEventListener("click", signup);
        document.getElementById("scanBtn")?.addEventListener("click", handleScan);
        document.getElementById("saveToBagBtn")?.addEventListener("click", saveToBag);
        document.getElementById("viewBagBtn")?.addEventListener("click", loadBag);
        document.getElementById("viewSavedLooksBtn")?.addEventListener("click", renderSavedLooksDrawer);
        document.getElementById("viewOrderHistoryBtn")?.addEventListener("click", loadOrderHistory);
        document.getElementById("viewPreferencesBtn")?.addEventListener("click", loadPreferences);
        document.getElementById("viewScanHistoryBtn")?.addEventListener("click", renderScanHistoryDrawer);

        document.getElementById("openMirrorModeBtn")?.addEventListener("click", openMirrorPage);
        document.getElementById("seedDemoInventoryBtn")?.addEventListener("click", seedDemoInventory);
        document.getElementById("clearDemoInventoryBtn")?.addEventListener("click", clearDemoInventory);
        document.getElementById("exportInventoryCsvBtn")?.addEventListener("click", exportInventoryCsv);
        document.getElementById("exportLowStockBtn")?.addEventListener("click", exportLowStockInventoryCsv);
        document.getElementById("exportReorderReportBtn")?.addEventListener("click", exportReorderReportCsv);

        document.getElementById("uploadInventoryBtn")?.addEventListener("click", uploadInventoryCsv);
        document.getElementById("bulkUploadInventoryBtn")?.addEventListener("click", uploadInventoryCsvBulk);
        document.getElementById("refreshImportHistoryBtn")?.addEventListener("click", loadImportHistory);
        document.getElementById("refreshImportJobsBtn")?.addEventListener("click", loadImportJobs);
        document.getElementById("downloadInventoryTemplateBtn")?.addEventListener("click", downloadInventoryImportTemplate);
        document.getElementById("downloadInventoryTemplateBtnSecondary")?.addEventListener("click", downloadInventoryImportTemplate);
        document.getElementById("refreshMerchantSalesBtn")?.addEventListener("click", loadMerchantSalesActivity);
        document.getElementById("saveInventoryEditBtn")?.addEventListener("click", saveInventoryEdit);

        document.getElementById("loadInventoryBtn")?.addEventListener("click", () => {
            merchantInventoryPage = 0;
            loadMerchantInventory();
        });

        document.getElementById("loadInventoryBtnTop")?.addEventListener("click", () => {
            merchantInventoryPage = 0;
            loadMerchantInventory();
        });

        document.getElementById("inventorySearchInput")?.addEventListener("keydown", event => {
            if (event.key === "Enter") {
                event.preventDefault();
                merchantInventoryPage = 0;
                loadMerchantInventory();
            }
        });

        document.getElementById("inventoryCategoryFilter")?.addEventListener("change", () => {
            merchantInventoryPage = 0;
            loadMerchantInventory();
        });

        document.getElementById("welcomeLogoutBtn")?.addEventListener("click", logout);

        document.getElementById("welcomeScanBtn")?.addEventListener("click", () => {
            scrollToScanConsole();
        });

        document.getElementById("createLookBtn")?.addEventListener("click", async () => {
            if (!lastScannedItem) {
                setScanStatus("Scan an item first to create a full look.", "danger");
                return;
            }

            const itemName = getItemField(lastScannedItem, "name", "itemName") || "this item";
            const vibe = document.getElementById("vibeSelect")?.value || "Casual";
            const rfid =
                currentRfid ||
                getItemField(lastScannedItem, "rfid", "itemRfid", "productRfid", "id");

            const createLookBtn = document.getElementById("createLookBtn");

            if (!rfid) {
                showToast("Missing RFID for look generation.", "error");
                console.error("Create Look failed: no RFID found", lastScannedItem);
                return;
            }

            const originalText = createLookBtn?.textContent || "Create Full Look";

            try {
                requireToken();

                if (createLookBtn) {
                    createLookBtn.disabled = true;
                    createLookBtn.textContent = "Generating...";
                }

                setScanStatus(`Generating a full look for ${itemName}...`, "success");

                window.currentLookVariation = 0;
                window.lastSwapCategory = "";
                resetLookHistory();

               const params = await buildPreferencesQueryParams();
                params.set("vibe", vibe);

                const resp = await fetch(
                    `${API.stylist}/look/${encodeURIComponent(rfid)}?${params.toString()}`,
                    { headers: getAuthHeaders() }
                );

                await assertAuthorizedResponse(resp, `Look request failed with status ${resp.status}`);

                const look = await resp.json();

                window.currentLookVariation = safeNumber(look?.variation || 0);
                pushLookToHistory(look, window.currentLookVariation);

                const fullOutfitContainer = document.getElementById("fullOutfitContainer");

                if (fullOutfitContainer && fullOutfitContainer.innerHTML.trim()) {
                    requestAnimationFrame(() => {
                        fullOutfitContainer.scrollIntoView({
                            behavior: "smooth",
                            block: "start"
                        });
                    });
                } else {
                    document.getElementById("scanResultSection")?.scrollIntoView({
                        behavior: "smooth",
                        block: "center"
                    });
                }

                showToast(`Full look created for ${itemName}.`, "success");
                setScanStatus(`Full look created for ${itemName}.`, "success");

                window.setTimeout(() => {
                    setScanStatus("Ready to scan.", "muted");
                }, 1800);
            } catch (error) {
                console.error("Create Look Error:", error);
                showToast(error.message || "Unable to generate full look.", "error");
                setScanStatus(error.message || "Unable to generate full look.", "danger");
            } finally {
                if (createLookBtn) {
                    createLookBtn.disabled = false;
                    createLookBtn.textContent = originalText;
                }
            }
        });

         document.getElementById("scanAnotherBtn")?.addEventListener("click", () => {
         const rfidInput = document.getElementById("rfidInput");

          if (rfidInput) {
               rfidInput.value = "";
               rfidInput.focus();
          }

           resetScanExperience();

         scrollToScanConsole();

           setScanStatus("Ready to scan.", "muted");
          });

         document.getElementById("adminPanel")?.addEventListener("shown.bs.collapse", () => {
            if (getToken()) {
                loadAllInsights();
            }
        });

        document.getElementById("importJobDetailsModal")?.addEventListener("hidden.bs.modal", () => {
            stopImportJobDetailsAutoRefresh();
        });

        document.getElementById("rfidInput")?.addEventListener("keydown", event => {
            if (event.key === "Enter") {
                event.preventDefault();
                handleScan();
            }
        });

        document.getElementById("activityEventFilter")?.addEventListener("change", () => {
            loadActivity();
            loadActivityTable();
        });

        document.getElementById("activityRetailerFilter")?.addEventListener("change", () => {
            loadActivity();
            loadActivityTable();
        });

        populateActivityRetailerFilter();
        renderQuickScanButtons();
        renderRecentScansFromBackend();
    }

  async function initializeApp() {
      bindEvents();
      startActivityRefresh();

      updateAuthStatus();
      setScanStatus("Ready to scan.", "muted");
      resetScanExperience();

      let restoredContext = null;

      if (getToken()) {
          try {
              restoredContext = await restoreLoggedInStoreContext();
          } catch (error) {
              console.warn("Startup context restore failed:", error);
              restoredContext = null;
          }
      }

      updateAuthUI();
      updateSecureStoreLabels();

      if (!getToken()) {
          return;
      }

      if (!restoredContext) {
          console.warn("Skipping startup backend loads because logged-in store context is incomplete.");
          return;
      }

      try {
          await renderRecentScansFromBackend();
      } catch (error) {
          console.warn("Startup recent scans preload skipped:", error);
      }

      try {
          await renderSavedLooksDrawer();
      } catch (error) {
          console.warn("Startup saved looks preload skipped:", error);
      }

      if (isOwnerUser()) {
          merchantInventoryPage = 0;

          await Promise.allSettled([
              loadImportHistory(),
              loadImportJobs(),
              loadMerchantInventory()
          ]);
      }
  }

 window.quickScan = quickScan;
 window.showImportJobDetails = showImportJobDetails;
 window.handleImportJobRowKeydown = handleImportJobRowKeydown;

 initializeApp();

 function wireManagerInsightsFooterToggle() {
     const adminPanel = document.getElementById("adminPanel");
     const bagFooter = document.getElementById("bagFooter");

     if (!adminPanel || !bagFooter) return;

     adminPanel.addEventListener("show.bs.collapse", () => {
         bagFooter.classList.add("bag-footer-hidden-by-insights");
     });

     adminPanel.addEventListener("hide.bs.collapse", () => {
         bagFooter.classList.remove("bag-footer-hidden-by-insights");
     });

     adminPanel.addEventListener("hidden.bs.collapse", () => {
         bagFooter.classList.remove("bag-footer-hidden-by-insights");
     });
 }

 if (document.readyState === "loading") {
     document.addEventListener("DOMContentLoaded", wireManagerInsightsFooterToggle);
 } else {
     wireManagerInsightsFooterToggle();
 }