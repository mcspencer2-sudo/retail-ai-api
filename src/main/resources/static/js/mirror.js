const API = {
  stylist: "/api/v1/macy-stylist"
};

window.PixelMirrorBootError = null;

const PIXEL_MIRROR_DEBUG_ENABLED =
  window.location.hostname === "localhost" ||
  window.location.search.includes("debugMirror=true") ||
  localStorage.getItem("pixelMirrorDebugEnabled") === "true";

const DEFAULT_PIXEL_STORES = {
  MACY001: [
    {
      code: "MACY-DEMO",
      label: "Macy's Demo Store"
    }
  ],
  KINGS001: [
    {
      code: "KINGS-DEMO",
      label: "Kings Boutique Demo Store"
    }
  ],
  NICKS001: [
    {
      code: "NICKS-DEMO",
      label: "Nicks Boutique Demo Store"
    }
  ]
};

const DEFAULT_PIXEL_RETAILERS = [
  {
    key: "MACY001",
    name: "Macy's Demo"
  },
  {
    key: "KINGS001",
    name: "Kings Boutique"
  },
  {
    key: "NICKS001",
    name: "Nicks Boutique"
  }
];

const RETAILER_CONFIG = window.UniversalStylistRetailers || {};

if (typeof RETAILER_CONFIG.getStoreOptions !== "function") {
  RETAILER_CONFIG.getStoreOptions = function getStoreOptionsFallback() {
    return DEFAULT_PIXEL_STORES;
  };
}

if (typeof RETAILER_CONFIG.getRetailerName !== "function") {
  RETAILER_CONFIG.getRetailerName = function getRetailerNameFallback(retailerKey) {
    const match = DEFAULT_PIXEL_RETAILERS.find(retailer => retailer.key === retailerKey);
    return match ? match.name : retailerKey || "Demo Retailer";
  };
}

if (typeof RETAILER_CONFIG.buildRetailerOptionsHtml !== "function") {
  RETAILER_CONFIG.buildRetailerOptionsHtml = function buildRetailerOptionsHtmlFallback(selectedValue = "MACY001") {
    return DEFAULT_PIXEL_RETAILERS.map(retailer => `
      <option value="${retailer.key}" ${retailer.key === selectedValue ? "selected" : ""}>
        ${retailer.name}
      </option>
    `).join("");
  };
}

const STORE_OPTIONS = RETAILER_CONFIG.getStoreOptions();

  function safeParseJson(value) {
  try {
    return value ? JSON.parse(value) : null;
  } catch (_) {
    return null;
  }
}

function readStorageValue(key) {
  return (
    localStorage.getItem(key) ||
    sessionStorage.getItem(key) ||
    ""
  );
}

function readStorageObject(key) {
  return (
    safeParseJson(localStorage.getItem(key)) ||
    safeParseJson(sessionStorage.getItem(key)) ||
    null
  );
}

function findRetailerKeyByStoreCode(storeCode) {
  const normalizedStoreCode = String(storeCode || "").trim().toLowerCase();

  if (!normalizedStoreCode) {
    return "";
  }

  for (const [retailerKey, stores] of Object.entries(STORE_OPTIONS || {})) {
    const match = stores.find(store =>
      String(store.code || "").trim().toLowerCase() === normalizedStoreCode
    );

    if (match) {
      return retailerKey;
    }
  }

  return "";
}

function getLoggedInMirrorContext() {
  const possibleObjects = [
    readStorageObject("retailerContext"),
    readStorageObject("currentRetailerContext"),
    readStorageObject("storeContext"),
    readStorageObject("currentStore"),
    readStorageObject("currentUser"),
    readStorageObject("user"),
    readStorageObject("authUser")
  ].filter(Boolean);

  let retailerKey =
    readStorageValue("retailerKey") ||
    readStorageValue("currentRetailerKey") ||
    "";

  let storeCode =
    readStorageValue("storeCode") ||
    readStorageValue("currentStoreCode") ||
    "";

  let storeName =
    readStorageValue("storeName") ||
    readStorageValue("currentStoreName") ||
    "";

  let retailerName =
    readStorageValue("retailerName") ||
    readStorageValue("currentRetailerName") ||
    "";

  for (const obj of possibleObjects) {
    retailerKey =
      retailerKey ||
      obj.retailerKey ||
      obj.currentRetailerKey ||
      obj.retailerCode ||
      obj.tenantKey ||
      "";

    storeCode =
      storeCode ||
      obj.storeCode ||
      obj.currentStoreCode ||
      obj.store?.storeCode ||
      obj.store?.code ||
      "";

    storeName =
      storeName ||
      obj.storeName ||
      obj.currentStoreName ||
      obj.store?.storeName ||
      obj.store?.name ||
      "";

    retailerName =
      retailerName ||
      obj.retailerName ||
      obj.currentRetailerName ||
      obj.businessName ||
      obj.tenantName ||
      "";
  }

  if (!retailerKey && storeCode) {
    retailerKey = findRetailerKeyByStoreCode(storeCode);
  }

  return {
    retailerKey: String(retailerKey || "").trim(),
    storeCode: String(storeCode || "").trim(),
    storeName: String(storeName || "").trim(),
    retailerName: String(retailerName || "").trim()
  };
}

function applyLoggedInMirrorContext() {
  const context = getLoggedInMirrorContext();

  if (!context.retailerKey && !context.storeCode) {
    return false;
  }

  const retailerSelect = document.getElementById("retailerSelect");
  const storeSelect = document.getElementById("storeCodeSelect");

  const retailerKey = context.retailerKey || findRetailerKeyByStoreCode(context.storeCode) || "MACY001";

  populateRetailerSelect(retailerKey);
  populateStoreOptions(retailerKey, context.storeCode);

  if (retailerSelect) {
    retailerSelect.value = retailerKey;
  }

  if (storeSelect && context.storeCode) {
    const storeExists = Array.from(storeSelect.options).some(option => option.value === context.storeCode);

    if (storeExists) {
      storeSelect.value = context.storeCode;
    }
  }

  return true;
}

 let currentRfid = "";
 let currentLoadedItem = null;
 let lastScannedItem = null;
 let currentMirrorMainFullOutfit = null;
 let currentMirrorMainPendingFullOutfit = null;
 let savedRfids = new Set();
 let ambientIdleTimer = null;
const AMBIENT_IDLE_DELAY_MS = 90000;

function getAmbientIdleDelayMs() {
  const customDelay = Number(localStorage.getItem("pixelMirrorIdleDelayMs"));

  if (Number.isFinite(customDelay) && customDelay >= 3000) {
    return customDelay;
  }

  return AMBIENT_IDLE_DELAY_MS;
}

  function escapeHtml(value) {
    return String(value ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  function getToken() {
    return (
      localStorage.getItem("token") ||
      localStorage.getItem("jwt") ||
      localStorage.getItem("authToken") ||
      localStorage.getItem("accessToken") ||
      sessionStorage.getItem("token") ||
      sessionStorage.getItem("jwt") ||
      sessionStorage.getItem("authToken") ||
      sessionStorage.getItem("accessToken") ||
      ""
    );
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
      throw new Error("Please log in from the Merchant App first, then return to Mirror Mode.");
    }

    return token;
  }

  function getSelectedRetailerKey() {
    return document.getElementById("retailerSelect")?.value || "MACY001";
  }

  function getSelectedStoreCode() {
    return document.getElementById("storeCodeSelect")?.value || "";
  }

  function getItemField(item, ...keys) {
    for (const key of keys) {
      if (item && item[key] !== undefined && item[key] !== null && item[key] !== "") {
        return item[key];
      }
    }

    return "";
  }

  function safeNumber(value, fallback = 0) {
    const number = Number(value);
    return Number.isFinite(number) ? number : fallback;
  }

  function formatPrice(value) {
    const number = Number(value);
    return Number.isFinite(number) ? `$${number.toFixed(2)}` : "$0.00";
  }

  function safeImageUrl(url, fallback = "https://placehold.co/700x900?text=Scanned+Item") {
    const value = String(url || "").trim();

    if (!value) {
      return fallback;
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
    } catch (_) {
      return fallback;
    }

    return fallback;
  }

function normalizeMirrorRfid(value) {
  return String(value || "")
    .trim()
    .replace(/^[✓✔✅\s]+/u, "")
    .replace(/^RFID[:#\s-]*/i, "")
    .trim();
}

function populateRetailerSelect(selectedValue = "MACY001") {
  const retailerSelect = document.getElementById("retailerSelect");
  if (!retailerSelect) return;

  const params = new URLSearchParams(window.location.search);

  const urlRetailerKey = params.get("retailer") || "";
  const urlStoreName = params.get("storeName") || "";
  const fallbackValue = selectedValue || urlRetailerKey || "MACY001";

  const knownRetailerHtml = RETAILER_CONFIG
    ? RETAILER_CONFIG.buildRetailerOptionsHtml(fallbackValue, false)
    : "";

  retailerSelect.innerHTML = knownRetailerHtml;

  const selectedExists = Array.from(retailerSelect.options).some(
    option => option.value === fallbackValue
  );

  if (!selectedExists && fallbackValue) {
    const customOption = document.createElement("option");
    customOption.value = fallbackValue;
    customOption.textContent = beautifyRetailerName(urlStoreName || fallbackValue);
    retailerSelect.prepend(customOption);
  }

  Array.from(retailerSelect.options).forEach(option => {
    option.textContent = beautifyRetailerName(option.textContent || option.value);
  });

  retailerSelect.value = fallbackValue;
}

function populateStoreOptions(retailerKey, preferredStoreCode = "") {
  const storeSelect = document.getElementById("storeCodeSelect");
  if (!storeSelect) return;

  const params = new URLSearchParams(window.location.search);

  const urlStoreCode = params.get("storeCode") || "";
  const urlStoreName = params.get("storeName") || "";
  const resolvedStoreCode = preferredStoreCode || urlStoreCode || "";

  const stores = STORE_OPTIONS[retailerKey] || [];

  if (!stores.length && resolvedStoreCode) {
    storeSelect.innerHTML = `
      <option value="${escapeHtml(resolvedStoreCode)}">
        ${escapeHtml(beautifyStoreName(urlStoreName || resolvedStoreCode))}
      </option>
    `;

    storeSelect.value = resolvedStoreCode;
    return;
  }

  if (!stores.length) {
    storeSelect.innerHTML = `<option value="">No Stores</option>`;
    return;
  }

  storeSelect.innerHTML = stores.map(store => `
    <option value="${escapeHtml(store.code)}">
      ${escapeHtml(beautifyStoreName(store.label || store.code))}
    </option>
  `).join("");

  const fallbackStoreCode = resolvedStoreCode || stores[0]?.code || "";
  const exists = stores.some(store => store.code === fallbackStoreCode);

  storeSelect.value = exists ? fallbackStoreCode : stores[0].code;
}
  function getLocalScanActivity() {
    try {
      const stored = JSON.parse(localStorage.getItem("universalStylistScanActivity") || "[]");
      return Array.isArray(stored) ? stored : [];
    } catch (_) {
      return [];
    }
  }

  function saveLocalScanActivity(activity) {
    localStorage.setItem("universalStylistScanActivity", JSON.stringify(activity));
  }

  function logScanActivity(item, vibe, source = "mirror") {
    if (!item) return;

    const existingActivity = getLocalScanActivity();

    const rfid = getItemField(item, "rfid", "itemRfid", "productRfid", "id") || currentRfid || "";
    const retailerKey = getSelectedRetailerKey();
    const retailer =
      getItemField(item, "retailer", "retailerName") ||
      RETAILER_CONFIG.getRetailerName(retailerKey) ||
      "Unknown Retailer";

    const scanRecord = {
      id: crypto.randomUUID(),
      timestamp: new Date().toISOString(),
      source,
      rfid,
      retailer,
      retailerKey,
      storeCode: getSelectedStoreCode(),
      name: getItemField(item, "name", "itemName") || "Scanned Item",
      brand: getItemField(item, "brand") || "Brand",
      category: getItemField(item, "category") || "Category",
      color: getItemField(item, "color") || "",
      price: Number(getItemField(item, "price")) || 0,
      vibe: vibe || "Casual",
      imageUrl: getItemField(item, "imageUrl", "image_url", "image", "photoUrl", "productImageUrl") || "",
      savedToBag: false
    };

    const updatedActivity = [scanRecord, ...existingActivity].slice(0, 250);
    saveLocalScanActivity(updatedActivity);
  }

  function markLatestScanSavedToBag(rfid) {
    if (!rfid) return;

    const activity = getLocalScanActivity();
    const normalizedRfid = String(rfid).toLowerCase();

    let hasMarkedLatest = false;

    const updatedActivity = activity.map(record => {
      const recordRfid = String(record.rfid || "").toLowerCase();

      if (!hasMarkedLatest && recordRfid === normalizedRfid && !record.savedToBag) {
        hasMarkedLatest = true;

        return {
          ...record,
          savedToBag: true,
          savedAt: new Date().toISOString()
        };
      }

      return record;
    });

    saveLocalScanActivity(updatedActivity);
  }

  async function assertAuthorizedResponse(response, fallbackMessage = "Request failed.") {
    if (response.ok) return;

    if (response.status === 401 || response.status === 403) {
      throw new Error("Your session expired. Please log in again from the Merchant App.");
    }

    let message = fallbackMessage;

    try {
      const contentType = response.headers.get("Content-Type") || "";

      if (contentType.includes("application/json")) {
        const data = await response.json();
        message = data?.message || data?.detail || data?.error || fallbackMessage;
      } else {
        message = await response.text() || fallbackMessage;
      }
    } catch (_) {
      message = fallbackMessage;
    }

    throw new Error(cleanApiErrorMessage(message));
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
  const authStatus = document.getElementById("authStatus");
  const liveDot = document.getElementById("liveDot");

  if (!authStatus) return;

  const hasToken = !!getToken();

  authStatus.textContent = hasToken
    ? "Mirror session ready"
    : "Session will verify on scan";

  if (liveDot) {
    liveDot.classList.toggle("error-dot", false);
  }
}

  function setStatus(message, type = "ready") {
    const status = document.getElementById("scanStatus");
    if (!status) return;

    const className = type === "success" ? "status success" : type === "error" ? "status error" : "status";
    const dotClass = type === "error" ? "dot error-dot" : "dot";

    status.className = className;
    status.innerHTML = `
      <span class="${dotClass}"></span>
      <span>${escapeHtml(message)}</span>
    `;
  }

  function showToast(message, type = "info") {
    const shell = document.getElementById("toastShell");
    if (!shell || !message) return;

    const toast = document.createElement("div");
    toast.className = `toast-card ${type}`;
    toast.textContent = message;

    shell.appendChild(toast);

    window.setTimeout(() => {
      toast.remove();
    }, 3200);
  }

  function setLoading(isLoading) {
    const loading = document.getElementById("loadingState");
    const scanBtn = document.getElementById("scanBtn");
    const readyCard = document.getElementById("readyCard");

    loading?.classList.toggle("show", isLoading);

    if (readyCard) {
      readyCard.style.display = isLoading ? "none" : "";
    }

    if (scanBtn) {
      scanBtn.disabled = isLoading;
      scanBtn.classList.toggle("scanning", isLoading);
      scanBtn.textContent = isLoading ? "Analyzing..." : "Scan Item";
    }
  }

  function revealPanel(panel) {
    if (!panel) return;

    panel.classList.add("show");
    panel.classList.remove("scan-reveal");
    void panel.offsetWidth;
    panel.classList.add("scan-reveal");
  }

  function scrollToPanel(panel, options = {}) {
    if (!panel) return;

    const { gap = 76, behavior = "smooth" } = options;
    const nav = document.querySelector(".mirror-nav");
    const navHeight = nav ? nav.getBoundingClientRect().height : 0;
    const stickyTop = 14;
    const panelTop = panel.getBoundingClientRect().top + window.scrollY;
    const targetY = panelTop - navHeight - stickyTop - gap;

    window.scrollTo({
      top: Math.max(0, targetY),
      behavior
    });
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

    return `The ${color}, ${category}, and ${vibe || "overall"} styling direction make this piece easy to build around.`;
  }

  function isCurrentItemSaved(item) {
    const rfid = getItemField(item, "rfid", "itemRfid", "productRfid", "id");
    return !!rfid && savedRfids.has(rfid);
  }

 function getMirrorSaveButtons() {
   return [
     document.getElementById("saveToBagBtn"),
     document.getElementById("mirrorMainAddToLookBtn")
   ].filter(Boolean);
 }

 function setSaveButtonSaved() {
   const buttons = getMirrorSaveButtons();

   buttons.forEach(button => {
     button.textContent = "Saved to Bag ✓";
     button.disabled = true;
     button.classList.add("is-saved");
   });
 }

 function setSaveButtonDefault(disabled = false) {
   const buttons = getMirrorSaveButtons();

   buttons.forEach(button => {
     button.textContent = "Save to Bag";
     button.disabled = disabled;
     button.classList.remove("is-saved");
   });
 }

  function updateResultBadges(item) {
    const inventoryBadge = document.getElementById("resultInventoryBadge");
    const retailBadge = document.getElementById("resultRetailBadge");

    const stockValue = getItemField(item, "stock", "quantity", "inventory", "availableQuantity");
    const stock = stockValue === "" ? null : safeNumber(stockValue, null);
    const retailer =
      getItemField(item, "retailer", "retailerName") ||
      RETAILER_CONFIG.getRetailerName(getSelectedRetailerKey()) ||
      "Store";

    if (inventoryBadge) {
      if (stock === null) {
        inventoryBadge.textContent = "Inventory Available";
        inventoryBadge.className = "product-badge green";
      } else if (stock > 0) {
        inventoryBadge.textContent = `${stock} In Stock`;
        inventoryBadge.className = "product-badge green";
      } else {
        inventoryBadge.textContent = "Check Availability";
        inventoryBadge.className = "product-badge";
      }
    }

    if (retailBadge) {
      retailBadge.textContent = `${retailer} Match`;
    }
  }

  function textIncludesAny(source, tokens) {
  const haystack = String(source || "").toLowerCase();

  return tokens.some(token => {
    return token && haystack.includes(String(token).toLowerCase());
  });
}

function getItemSearchText(item) {
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
    getItemField(item, "stylingAdvice"),
    getItemField(item, "whyItWorks")
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();
}

function getPreferenceMatchInsights(item) {
  const prefs = normalizeMirrorPreferences(window.currentMirrorPreferences || {});
  const insights = [];

  const color = String(getItemField(item, "color") || "").toLowerCase();
  const price = Number(getItemField(item, "price") || 0);
  const searchText = getItemSearchText(item);

  const favoriteColors = splitPreferenceList(prefs.favoriteColors);
  const avoidedColors = splitPreferenceList(prefs.avoidedColors);
  const preferredMaterials = splitPreferenceList(prefs.preferredMaterials);
  const dislikedMaterials = splitPreferenceList(prefs.dislikedMaterials);
  const styleKeywords = splitPreferenceList(prefs.styleKeywords);
  const dislikedStyles = splitPreferenceList(prefs.dislikedStyles);

  const budgetMin = Number(prefs.budgetMin || 0);
  const budgetMax = Number(prefs.budgetMax || 0);

  if (favoriteColors.length && textIncludesAny(color, favoriteColors)) {
    insights.push({
      type: "positive",
      icon: "✓",
      title: "Favorite color match",
      text: "This item uses a color saved in the shopper profile."
    });
  }

  if (avoidedColors.length && textIncludesAny(color, avoidedColors)) {
    insights.push({
      type: "warning",
      icon: "!",
      title: "Avoided color warning",
      text: "This color appears in the shopper’s avoided color list."
    });
  }

  if (preferredMaterials.length && textIncludesAny(searchText, preferredMaterials)) {
    insights.push({
      type: "positive",
      icon: "✓",
      title: "Preferred material match",
      text: "The material or texture aligns with saved preferences."
    });
  }

  if (dislikedMaterials.length && textIncludesAny(searchText, dislikedMaterials)) {
    insights.push({
      type: "warning",
      icon: "!",
      title: "Material caution",
      text: "This item may include a material the shopper prefers to avoid."
    });
  }

  if (styleKeywords.length && textIncludesAny(searchText, styleKeywords)) {
    insights.push({
      type: "positive",
      icon: "✓",
      title: "Style identity match",
      text: "The item matches the shopper’s saved style keywords."
    });
  }

  if (dislikedStyles.length && textIncludesAny(searchText, dislikedStyles)) {
    insights.push({
      type: "warning",
      icon: "!",
      title: "Style caution",
      text: "This item may overlap with a style the shopper avoids."
    });
  }

  if (Number.isFinite(price) && price > 0) {
    if (
      (budgetMin > 0 || budgetMax > 0) &&
      (budgetMin <= 0 || price >= budgetMin) &&
      (budgetMax <= 0 || price <= budgetMax)
    ) {
      insights.push({
        type: "positive",
        icon: "✓",
        title: "Budget match",
        text: "The price sits inside the shopper’s preferred budget range."
      });
    }

    if (budgetMax > 0 && price > budgetMax) {
      insights.push({
        type: "warning",
        icon: "!",
        title: "Above preferred budget",
        text: "The item is priced above the saved budget maximum."
      });
    }
  }

  if (
    prefs.occasionPriority &&
    prefs.occasionPriority !== "Everyday" &&
    textIncludesAny(searchText, [prefs.occasionPriority])
  ) {
    insights.push({
      type: "positive",
      icon: "✓",
      title: "Occasion match",
      text: `This item supports the shopper’s ${prefs.occasionPriority} priority.`
    });
  }

  if (
    prefs.fitPreference &&
    prefs.fitPreference !== "Regular" &&
    textIncludesAny(searchText, [prefs.fitPreference])
  ) {
    insights.push({
      type: "positive",
      icon: "✓",
      title: "Fit preference match",
      text: `This item aligns with the shopper’s ${prefs.fitPreference.toLowerCase()} fit preference.`
    });
  }

  if (!insights.length) {
    insights.push({
      type: "neutral",
      icon: "i",
      title: "Profile-aware styling active",
      text: "No exact preference match was detected, but this scan used the shopper profile when available."
    });
  }

  return insights.slice(0, 5);
}

function renderPreferenceMatchInsights(item) {
  const container = document.getElementById("preferenceMatchContent");

  if (!container) return;

  const insights = getPreferenceMatchInsights(item);

  container.innerHTML = insights.map(insight => `
    <div class="preference-match-row ${escapeHtml(insight.type)}">
      <div class="preference-match-icon">${escapeHtml(insight.icon)}</div>
      <div class="preference-match-text">
        <strong>${escapeHtml(insight.title)}</strong>
        <span>${escapeHtml(insight.text)}</span>
      </div>
    </div>
  `).join("");
}

 function renderScanResult(item, selectedVibe) {
   hideAmbientIdleMode();
   setMirrorMainScanningState(false);

   lastScannedItem = item || null;
   currentLoadedItem = item || null;
   currentRfid = getItemField(item, "rfid", "itemRfid", "productRfid", "id") || currentRfid;

   /*
     Important:
     Some scan responses already include fullOutfit.
     We store it, but we do NOT show it during scan.
     The complete look should only open after the customer clicks Create Full Outfit.
   */
   currentMirrorMainPendingFullOutfit = item?.fullOutfit || null;
   currentMirrorMainFullOutfit = null;

   document.getElementById("outfitPanel")?.classList.remove("show", "scan-reveal");

   if (typeof hideMirrorMainOutfitShowcase === "function") {
     hideMirrorMainOutfitShowcase();
   }

   if (typeof setMirrorMainLookSaveButtonState === "function") {
     setMirrorMainLookSaveButtonState(false);
   }

   const name = getItemField(item, "name", "itemName", "productName", "title") || "Scanned Item";
   const brand = getItemField(item, "brand") || "Brand";
   const category = getItemField(item, "category") || "Category";
   const color = getItemField(item, "color") || "Color";

   const retailer =
     getItemField(item, "retailer", "retailerName") ||
     RETAILER_CONFIG.getRetailerName(getSelectedRetailerKey()) ||
     "Unknown Retailer";

   const price = getItemField(item, "price");
   const advice = getItemField(item, "stylingAdvice") || generateStylingAdvice(item, selectedVibe);
   const why = getItemField(item, "whyItWorks") || generateWhyItWorks(item, selectedVibe);
   const score = getItemField(item, "matchScore") || 92;

   const imageUrl = safeImageUrl(
     getItemField(item, "imageUrl", "image_url", "image", "photoUrl", "productImageUrl")
   );

   const resultItemName = document.getElementById("resultItemName");
   const resultMeta = document.getElementById("resultMeta");
   const resultRetailer = document.getElementById("resultRetailer");
   const resultPrice = document.getElementById("resultPrice");
   const resultVibe = document.getElementById("resultVibe");
   const resultAdvice = document.getElementById("resultAdvice");
   const resultWhy = document.getElementById("resultWhy");
   const resultScore = document.getElementById("resultScore");

   if (resultItemName) resultItemName.textContent = name;
   if (resultMeta) resultMeta.textContent = `${brand} • ${category} • ${color}`;
   if (resultRetailer) resultRetailer.textContent = retailer;
   if (resultPrice) resultPrice.textContent = formatPrice(price);
   if (resultVibe) resultVibe.textContent = selectedVibe || "Casual";
   if (resultAdvice) resultAdvice.textContent = advice;
   if (resultWhy) resultWhy.textContent = why;
   if (resultScore) resultScore.textContent = `${score}%`;

   renderPreferenceMatchInsights(item);
   rememberTryOnScan(item, selectedVibe);
   renderMirrorMainRecentScans();
   renderMirrorMainTimeline();
   updateResultBadges(item);

   if (typeof updatePixelAssociateRecommendationsForProduct === "function") {
     updatePixelAssociateRecommendationsForProduct(item, selectedVibe || "Casual");
   }

   speakPixelConcierge("scan", {
     itemName: name || "This item"
   });

   const image = document.getElementById("resultImage");

   if (image) {
     image.src = imageUrl;
     image.alt = name;
     image.onerror = function () {
       this.src = "https://placehold.co/700x900?text=Scanned+Item";
     };
   }

   if (isCurrentItemSaved(item)) {
     setSaveButtonSaved();
   } else {
     setSaveButtonDefault(false);
   }

   const resultPanel = document.getElementById("resultPanel");
   revealPanel(resultPanel);

   if (typeof updateMirrorMainProductCard === "function") {
     document.body.classList.remove("mirror-main-demo-product-active");
     updateMirrorMainProductCard();
     updateMirrorMainBagCount?.();
   }

   if (window.MirrorCustomerJourney?.product) {
     window.MirrorCustomerJourney.product(item);
   }

   window.requestAnimationFrame(() => {
     scrollToPanel(resultPanel, { gap: 90 });
   });
 }

  function getOutfitItems(fullOutfit) {
  if (!fullOutfit || typeof fullOutfit !== "object") {
    return [];
  }

  return [
    fullOutfit.top,
    fullOutfit.bottom,
    fullOutfit.shoes,
    fullOutfit.outerwear
  ].filter(Boolean);
}

function getOutfitTotalPrice(fullOutfit) {
  return getOutfitItems(fullOutfit).reduce((sum, item) => {
    return sum + safeNumber(getItemField(item, "price"));
  }, 0);
}

function getUniqueLowercaseValues(items, fieldName) {
  return [
    ...new Set(
      items
        .map(item => String(getItemField(item, fieldName) || "").trim())
        .filter(Boolean)
        .map(value => value.toLowerCase())
    )
  ];
}

function getOutfitSearchText(fullOutfit) {
  return getOutfitItems(fullOutfit)
    .map(item => getItemSearchText(item))
    .join(" ")
    .toLowerCase();
}

function getOutfitPreferenceInsights(fullOutfit) {
  const prefs = normalizeMirrorPreferences(window.currentMirrorPreferences || {});
  const items = getOutfitItems(fullOutfit);
  const insights = [];

  const totalPrice = getOutfitTotalPrice(fullOutfit);
  const outfitText = getOutfitSearchText(fullOutfit);
  const outfitColors = getUniqueLowercaseValues(items, "color");

  const favoriteColors = splitPreferenceList(prefs.favoriteColors);
  const avoidedColors = splitPreferenceList(prefs.avoidedColors);
  const preferredMaterials = splitPreferenceList(prefs.preferredMaterials);
  const dislikedMaterials = splitPreferenceList(prefs.dislikedMaterials);
  const styleKeywords = splitPreferenceList(prefs.styleKeywords);
  const dislikedStyles = splitPreferenceList(prefs.dislikedStyles);

  const budgetMin = Number(prefs.budgetMin || 0);
  const budgetMax = Number(prefs.budgetMax || 0);

  if (items.length) {
    insights.push({
      type: "neutral",
      icon: "i",
      title: "Complete look built from live styling pieces",
      text: `Pixel analyzed ${items.length} outfit piece${items.length === 1 ? "" : "s"} around the scanned item.`
    });
  }

  if (
    Number.isFinite(totalPrice) &&
    totalPrice > 0 &&
    (budgetMin > 0 || budgetMax > 0)
  ) {
    if (
      (budgetMin <= 0 || totalPrice >= budgetMin) &&
      (budgetMax <= 0 || totalPrice <= budgetMax)
    ) {
      insights.push({
        type: "positive",
        icon: "✓",
        title: "Full look fits budget",
        text: `The estimated outfit total of ${formatPrice(totalPrice)} is inside the shopper’s preferred range.`
      });
    }

    if (budgetMax > 0 && totalPrice > budgetMax) {
      insights.push({
        type: "warning",
        icon: "!",
        title: "Full look is above budget",
        text: `The estimated outfit total of ${formatPrice(totalPrice)} is above the saved budget maximum.`
      });
    }
  }

  const matchedFavoriteColors = favoriteColors.filter(color => {
    return outfitColors.some(outfitColor => outfitColor.includes(color.toLowerCase()));
  });

  if (matchedFavoriteColors.length) {
    insights.push({
      type: "positive",
      icon: "✓",
      title: "Color palette match",
      text: `The look includes preferred color direction: ${matchedFavoriteColors.join(", ")}.`
    });
  }

  const matchedAvoidedColors = avoidedColors.filter(color => {
    return outfitColors.some(outfitColor => outfitColor.includes(color.toLowerCase()));
  });

  if (matchedAvoidedColors.length) {
    insights.push({
      type: "warning",
      icon: "!",
      title: "Avoided color detected",
      text: `The look may include colors the shopper avoids: ${matchedAvoidedColors.join(", ")}.`
    });
  }

  const matchedMaterials = preferredMaterials.filter(material => {
    return outfitText.includes(material.toLowerCase());
  });

  if (matchedMaterials.length) {
    insights.push({
      type: "positive",
      icon: "✓",
      title: "Preferred texture alignment",
      text: `The look includes preferred material signals: ${matchedMaterials.join(", ")}.`
    });
  }

  const matchedDislikedMaterials = dislikedMaterials.filter(material => {
    return outfitText.includes(material.toLowerCase());
  });

  if (matchedDislikedMaterials.length) {
    insights.push({
      type: "warning",
      icon: "!",
      title: "Material caution",
      text: `The look may include material signals to avoid: ${matchedDislikedMaterials.join(", ")}.`
    });
  }

  const matchedStyles = styleKeywords.filter(style => {
    return outfitText.includes(style.toLowerCase());
  });

  if (matchedStyles.length) {
    insights.push({
      type: "positive",
      icon: "✓",
      title: "Style identity match",
      text: `The outfit aligns with saved style keywords: ${matchedStyles.join(", ")}.`
    });
  }

  const matchedDislikedStyles = dislikedStyles.filter(style => {
    return outfitText.includes(style.toLowerCase());
  });

  if (matchedDislikedStyles.length) {
    insights.push({
      type: "warning",
      icon: "!",
      title: "Style caution",
      text: `The outfit may overlap with avoided style direction: ${matchedDislikedStyles.join(", ")}.`
    });
  }

  if (
    prefs.occasionPriority &&
    prefs.occasionPriority !== "Everyday" &&
    outfitText.includes(String(prefs.occasionPriority).toLowerCase())
  ) {
    insights.push({
      type: "positive",
      icon: "✓",
      title: "Occasion-ready outfit",
      text: `This look supports the shopper’s ${prefs.occasionPriority} priority.`
    });
  }

  if (
    prefs.fitPreference &&
    prefs.fitPreference !== "Regular" &&
    outfitText.includes(String(prefs.fitPreference).toLowerCase())
  ) {
    insights.push({
      type: "positive",
      icon: "✓",
      title: "Fit preference alignment",
      text: `This look includes signals matching the shopper’s ${prefs.fitPreference.toLowerCase()} fit preference.`
    });
  }

  if (!insights.length) {
    insights.push({
      type: "neutral",
      icon: "i",
      title: "Profile-aware look generation active",
      text: "The full look was generated with shopper preferences attached when available."
    });
  }

  return insights.slice(0, 7);
}

function renderOutfitPreferenceInsights(fullOutfit) {
  const content = document.getElementById("outfitPreferenceContent");
  const totalPrice = document.getElementById("outfitTotalPrice");

  if (!content || !totalPrice) return;

  const total = getOutfitTotalPrice(fullOutfit);
  totalPrice.textContent = formatPrice(total);

  const insights = getOutfitPreferenceInsights(fullOutfit);

  content.innerHTML = insights.map(insight => `
    <div class="outfit-intel-row ${escapeHtml(insight.type)}">
      <div class="outfit-intel-icon">${escapeHtml(insight.icon)}</div>
      <div class="outfit-intel-text">
        <strong>${escapeHtml(insight.title)}</strong>
        <span>${escapeHtml(insight.text)}</span>
      </div>
    </div>
  `).join("");
}

  function clampScore(value, min = 0, max = 100) {
  const number = Number(value);
  if (!Number.isFinite(number)) return min;

  return Math.max(min, Math.min(max, Math.round(number)));
}

function getItemStockValue(item) {
  const stockValue = getItemField(
    item,
    "stock",
    "quantity",
    "inventory",
    "availableQuantity",
    "stockQuantity",
    "onHand"
  );

  if (stockValue === "") {
    return null;
  }

  const stock = Number(stockValue);
  return Number.isFinite(stock) ? stock : null;
}

function scoreColorHarmony(fullOutfit) {
  const items = getOutfitItems(fullOutfit);
  const colors = getUniqueLowercaseValues(items, "color");

  if (!items.length) {
    return {
      score: 0,
      label: "Color Harmony",
      detail: "No outfit pieces available to analyze."
    };
  }

  if (!colors.length) {
    return {
      score: 74,
      label: "Color Harmony",
      detail: "Color data is limited, but the look can still be styled visually."
    };
  }

  if (colors.length === 1) {
    return {
      score: 96,
      label: "Color Harmony",
      detail: "A single-color palette creates a clean monochrome direction."
    };
  }

  if (colors.length === 2) {
    return {
      score: 92,
      label: "Color Harmony",
      detail: "Two dominant colors create a focused, easy-to-style palette."
    };
  }

  if (colors.length === 3) {
    return {
      score: 86,
      label: "Color Harmony",
      detail: "Three colors give the look depth while staying coordinated."
    };
  }

  return {
    score: 76,
    label: "Color Harmony",
    detail: "The look uses several colors, creating a more expressive outfit."
  };
}

function scoreBudgetFit(fullOutfit) {
  const prefs = normalizeMirrorPreferences(window.currentMirrorPreferences || {});
  const totalPrice = getOutfitTotalPrice(fullOutfit);

  const budgetMin = Number(prefs.budgetMin || 0);
  const budgetMax = Number(prefs.budgetMax || 0);

  if (!totalPrice) {
    return {
      score: 72,
      label: "Budget Fit",
      detail: "Price data is limited for this outfit."
    };
  }

  if (!budgetMin && !budgetMax) {
    return {
      score: 84,
      label: "Budget Fit",
      detail: `Estimated outfit total is ${formatPrice(totalPrice)}. No saved budget limit is active.`
    };
  }

  if (
    (budgetMin <= 0 || totalPrice >= budgetMin) &&
    (budgetMax <= 0 || totalPrice <= budgetMax)
  ) {
    return {
      score: 96,
      label: "Budget Fit",
      detail: `The full look total of ${formatPrice(totalPrice)} fits the saved budget range.`
    };
  }

  if (budgetMax > 0 && totalPrice > budgetMax) {
    const overBy = totalPrice - budgetMax;
    const penalty = Math.min(42, Math.round((overBy / Math.max(budgetMax, 1)) * 100));

    return {
      score: clampScore(82 - penalty),
      label: "Budget Fit",
      detail: `The full look is ${formatPrice(overBy)} above the saved budget maximum.`
    };
  }

  if (budgetMin > 0 && totalPrice < budgetMin) {
    return {
      score: 88,
      label: "Budget Fit",
      detail: `The full look is below the preferred minimum, leaving room to add premium pieces.`
    };
  }

  return {
    score: 80,
    label: "Budget Fit",
    detail: "Budget fit is acceptable based on available price data."
  };
}

function scoreProfileMatch(fullOutfit) {
  const prefs = normalizeMirrorPreferences(window.currentMirrorPreferences || {});
  const items = getOutfitItems(fullOutfit);
  const outfitText = getOutfitSearchText(fullOutfit);
  const outfitColors = getUniqueLowercaseValues(items, "color");

  const positiveSignals = [
    ...splitPreferenceList(prefs.favoriteColors).filter(color =>
      outfitColors.some(outfitColor => outfitColor.includes(color.toLowerCase()))
    ),
    ...splitPreferenceList(prefs.preferredMaterials).filter(material =>
      outfitText.includes(material.toLowerCase())
    ),
    ...splitPreferenceList(prefs.styleKeywords).filter(style =>
      outfitText.includes(style.toLowerCase())
    )
  ];

  const cautionSignals = [
    ...splitPreferenceList(prefs.avoidedColors).filter(color =>
      outfitColors.some(outfitColor => outfitColor.includes(color.toLowerCase()))
    ),
    ...splitPreferenceList(prefs.dislikedMaterials).filter(material =>
      outfitText.includes(material.toLowerCase())
    ),
    ...splitPreferenceList(prefs.dislikedStyles).filter(style =>
      outfitText.includes(style.toLowerCase())
    )
  ];

  if (!hasMeaningfulPreferences(prefs)) {
    return {
      score: 82,
      label: "Profile Match",
      detail: "No detailed profile is saved yet, so Pixel scored this from outfit compatibility."
    };
  }

  let score = 78 + positiveSignals.length * 7 - cautionSignals.length * 12;

  if (
    prefs.occasionPriority &&
    prefs.occasionPriority !== "Everyday" &&
    outfitText.includes(String(prefs.occasionPriority).toLowerCase())
  ) {
    score += 6;
  }

  if (
    prefs.fitPreference &&
    prefs.fitPreference !== "Regular" &&
    outfitText.includes(String(prefs.fitPreference).toLowerCase())
  ) {
    score += 5;
  }

  const detail = cautionSignals.length
    ? `Matches ${positiveSignals.length} saved preference signal${positiveSignals.length === 1 ? "" : "s"} with ${cautionSignals.length} caution signal${cautionSignals.length === 1 ? "" : "s"}.`
    : `Matches ${positiveSignals.length} saved preference signal${positiveSignals.length === 1 ? "" : "s"} with no major avoid conflicts.`;

  return {
    score: clampScore(score),
    label: "Profile Match",
    detail
  };
}

function scoreStoreAvailability(fullOutfit) {
  const items = getOutfitItems(fullOutfit);

  if (!items.length) {
    return {
      score: 0,
      label: "Store Availability",
      detail: "No outfit inventory is available to check."
    };
  }

  const stockValues = items.map(getItemStockValue);
  const knownStockValues = stockValues.filter(value => value !== null);

  if (!knownStockValues.length) {
    return {
      score: 86,
      label: "Store Availability",
      detail: `Inventory is scoped to ${getMirrorStoreDisplayName()}, but exact stock counts are limited.`
    };
  }

  const availableCount = knownStockValues.filter(stock => stock > 0).length;
  const availabilityRatio = availableCount / knownStockValues.length;

  if (availabilityRatio === 1) {
    return {
      score: 98,
      label: "Store Availability",
      detail: `All checked pieces appear available at ${getMirrorStoreDisplayName()}.`
    };
  }

  if (availabilityRatio >= 0.75) {
    return {
      score: 86,
      label: "Store Availability",
      detail: "Most checked pieces appear available in this store."
    };
  }

  if (availabilityRatio >= 0.5) {
    return {
      score: 68,
      label: "Store Availability",
      detail: "Some outfit pieces may need availability confirmation."
    };
  }

  return {
    score: 48,
    label: "Store Availability",
    detail: "Several outfit pieces may be low or unavailable."
  };
}

function scoreStylingBalance(fullOutfit) {
  const items = getOutfitItems(fullOutfit);
  const categories = items
    .map(item => String(getItemField(item, "category") || "").toLowerCase())
    .join(" ");

  let score = 68;
  const reasons = [];

  if (categories.includes("top") || categories.includes("shirt") || categories.includes("blouse") || categories.includes("jacket")) {
    score += 8;
    reasons.push("upper-body anchor");
  }

  if (categories.includes("bottom") || categories.includes("pant") || categories.includes("jean") || categories.includes("skirt")) {
    score += 8;
    reasons.push("bottom layer");
  }

  if (categories.includes("shoe") || categories.includes("sneaker") || categories.includes("boot") || categories.includes("heel")) {
    score += 8;
    reasons.push("footwear");
  }

  if (categories.includes("outerwear") || categories.includes("coat") || categories.includes("jacket")) {
    score += 6;
    reasons.push("layering piece");
  }

  if (items.length >= 3) {
    score += 7;
  }

  if (items.length >= 4) {
    score += 5;
  }

  return {
    score: clampScore(score),
    label: "Styling Balance",
    detail: reasons.length
      ? `The look includes ${reasons.join(", ")} for a complete outfit structure.`
      : "The look has a basic structure and can be refined with more categories."
  };
}

function getAdvancedOutfitScores(fullOutfit) {
  const scores = [
    {
      key: "color",
      className: "",
      ...scoreColorHarmony(fullOutfit)
    },
    {
      key: "budget",
      className: "green",
      ...scoreBudgetFit(fullOutfit)
    },
    {
      key: "profile",
      className: "purple",
      ...scoreProfileMatch(fullOutfit)
    },
    {
      key: "availability",
      className: "gold",
      ...scoreStoreAvailability(fullOutfit)
    },
    {
      key: "balance",
      className: "slate",
      ...scoreStylingBalance(fullOutfit)
    }
  ];

  const average = scores.length
    ? Math.round(scores.reduce((sum, item) => sum + safeNumber(item.score), 0) / scores.length)
    : 0;

  return {
    overallScore: clampScore(average),
    scores
  };
}

function renderOutfitScoreBreakdown(fullOutfit) {
  const grid = document.getElementById("outfitScoreBreakdownGrid");
  const advancedScore = document.getElementById("advancedOutfitScore");

  if (!grid || !advancedScore) return;

  const analysis = getAdvancedOutfitScores(fullOutfit);
  advancedScore.textContent = `${analysis.overallScore}%`;

  grid.innerHTML = analysis.scores.map(scoreItem => {
    const score = clampScore(scoreItem.score);

    return `
      <div class="score-breakdown-card ${escapeHtml(scoreItem.className || "")}">
        <div class="score-breakdown-label">${escapeHtml(scoreItem.label)}</div>
        <div class="score-breakdown-value">${score}%</div>
        <div class="score-meter">
          <div class="score-meter-fill" style="width: ${score}%;"></div>
        </div>
        <div class="score-breakdown-sub">${escapeHtml(scoreItem.detail)}</div>
      </div>
    `;
  }).join("");
}

  function getScoreByKey(analysis, key) {
  return analysis?.scores?.find(score => score.key === key) || null;
}

function hasOutfitCategorySignal(fullOutfit, tokens) {
  const categoryText = getOutfitItems(fullOutfit)
    .map(item => [
      getItemField(item, "category"),
      getItemField(item, "name", "itemName"),
      getItemField(item, "styleTags")
    ].filter(Boolean).join(" "))
    .join(" ")
    .toLowerCase();

  return tokens.some(token => categoryText.includes(String(token).toLowerCase()));
}

function getOutfitCautionSignals(fullOutfit) {
  const prefs = normalizeMirrorPreferences(window.currentMirrorPreferences || {});
  const items = getOutfitItems(fullOutfit);
  const outfitText = getOutfitSearchText(fullOutfit);
  const outfitColors = getUniqueLowercaseValues(items, "color");

  const avoidedColors = splitPreferenceList(prefs.avoidedColors).filter(color =>
    outfitColors.some(outfitColor => outfitColor.includes(color.toLowerCase()))
  );

  const dislikedMaterials = splitPreferenceList(prefs.dislikedMaterials).filter(material =>
    outfitText.includes(material.toLowerCase())
  );

  const dislikedStyles = splitPreferenceList(prefs.dislikedStyles).filter(style =>
    outfitText.includes(style.toLowerCase())
  );

  return {
    avoidedColors,
    dislikedMaterials,
    dislikedStyles,
    total:
      avoidedColors.length +
      dislikedMaterials.length +
      dislikedStyles.length
  };
}

function buildOutfitImprovementSuggestions(fullOutfit) {
  const analysis = getAdvancedOutfitScores(fullOutfit);
  const suggestions = [];
  const totalPrice = getOutfitTotalPrice(fullOutfit);
  const prefs = normalizeMirrorPreferences(window.currentMirrorPreferences || {});
  const cautionSignals = getOutfitCautionSignals(fullOutfit);

  const colorScore = getScoreByKey(analysis, "color");
  const budgetScore = getScoreByKey(analysis, "budget");
  const profileScore = getScoreByKey(analysis, "profile");
  const availabilityScore = getScoreByKey(analysis, "availability");
  const balanceScore = getScoreByKey(analysis, "balance");

  const hasTop = hasOutfitCategorySignal(fullOutfit, ["top", "shirt", "blouse", "tee", "sweater", "jacket"]);
  const hasBottom = hasOutfitCategorySignal(fullOutfit, ["bottom", "pant", "jean", "trouser", "skirt", "short"]);
  const hasShoes = hasOutfitCategorySignal(fullOutfit, ["shoe", "sneaker", "boot", "heel", "loafer"]);
  const hasOuterwear = hasOutfitCategorySignal(fullOutfit, ["outerwear", "coat", "jacket", "blazer"]);

  if (colorScore && colorScore.score < 84) {
    suggestions.push({
      type: "color",
      icon: "C",
      title: "Tighten the color palette",
      detail: "The look uses a more expressive palette. Pixel would improve harmony by swapping one piece into a neutral, matching, or shopper-preferred color.",
      action: "Try a color-aligned swap"
    });
  }

  if (budgetScore && budgetScore.score < 82) {
    const budgetMax = Number(prefs.budgetMax || 0);

    suggestions.push({
      type: "budget",
      icon: "$",
      title: "Reduce the full-look price",
      detail: budgetMax > 0
        ? `The look total is ${formatPrice(totalPrice)}. Pixel would look for a similar piece that brings the outfit closer to the saved ${formatPrice(budgetMax)} budget.`
        : "Pixel would look for a similar lower-price alternative while preserving the outfit direction.",
      action: "Swap highest-price piece"
    });
  }

  if (profileScore && profileScore.score < 86) {
    suggestions.push({
      type: "profile",
      icon: "P",
      title: "Strengthen profile alignment",
      detail: "The outfit can better reflect saved shopper preferences by prioritizing favorite colors, preferred materials, or style keywords.",
      action: "Prioritize profile matches"
    });
  }

  if (availabilityScore && availabilityScore.score < 86) {
    suggestions.push({
      type: "availability",
      icon: "A",
      title: "Improve store availability",
      detail: `Some pieces may need stock confirmation. Pixel would swap uncertain items for pieces currently available at ${getMirrorStoreDisplayName()}.`,
      action: "Choose in-stock alternatives"
    });
  }

  if (balanceScore && balanceScore.score < 88) {
    const missing = [];

    if (!hasTop) missing.push("top");
    if (!hasBottom) missing.push("bottom");
    if (!hasShoes) missing.push("shoes");
    if (!hasOuterwear) missing.push("outerwear/layer");

    suggestions.push({
      type: "balance",
      icon: "B",
      title: missing.length ? "Complete the outfit structure" : "Improve silhouette balance",
      detail: missing.length
        ? `Pixel would improve this look by adding or replacing the missing ${missing.join(", ")} category.`
        : "Pixel would rebalance the proportions so the full look feels more intentional from head to toe.",
      action: "Complete missing category"
    });
  }

 if (cautionSignals.total > 0) {
  const cautionParts = [
    cautionSignals.avoidedColors.length
      ? `avoided colors: ${cautionSignals.avoidedColors.join(", ")}`
      : "",
    cautionSignals.dislikedMaterials.length
      ? `disliked materials: ${cautionSignals.dislikedMaterials.join(", ")}`
      : "",
    cautionSignals.dislikedStyles.length
      ? `avoided styles: ${cautionSignals.dislikedStyles.join(", ")}`
      : ""
  ].filter(Boolean);

  suggestions.unshift({
    type: "caution",
    icon: "!",
    title: "Resolve shopper avoid signals",
    detail: `This look includes ${cautionParts.join("; ")}. Pixel would replace those pieces before recommending this as the final outfit.`,
    action: "Swap caution pieces"
  });
}

  if (!suggestions.length) {
    suggestions.push({
      type: "profile",
      icon: "✓",
      title: "This look is already strong",
      detail: "Pixel does not detect any major styling, budget, profile, or availability issue. The next best improvement would be optional accessories.",
      action: "Optional: add accessories"
    });

    if (!hasOuterwear) {
      suggestions.push({
        type: "balance",
        icon: "+",
        title: "Optional layering upgrade",
        detail: "Add a jacket, blazer, cardigan, or coat to create a more complete smart-mirror presentation.",
        action: "Add outerwear"
      });
    }
  }

  return suggestions.slice(0, 5);
}

function renderOutfitImprovementSuggestions(fullOutfit) {
  const container = document.getElementById("outfitImprovementContent");

  if (!container) return;

  const suggestions = buildOutfitImprovementSuggestions(fullOutfit);

  container.innerHTML = suggestions.map(suggestion => `
    <div class="outfit-improvement-row ${escapeHtml(suggestion.type)}">
      <div class="outfit-improvement-icon">${escapeHtml(suggestion.icon)}</div>

      <div>
        <div class="outfit-improvement-title">${escapeHtml(suggestion.title)}</div>
        <div class="outfit-improvement-detail">${escapeHtml(suggestion.detail)}</div>
        <div class="outfit-improvement-action">${escapeHtml(suggestion.action)}</div>
      </div>
    </div>
  `).join("");
}

  function getOutfitMissingCategories(fullOutfit) {
  const missing = [];

  const hasTop = hasOutfitCategorySignal(fullOutfit, [
    "top",
    "shirt",
    "blouse",
    "tee",
    "sweater",
    "jacket"
  ]);

  const hasBottom = hasOutfitCategorySignal(fullOutfit, [
    "bottom",
    "pant",
    "jean",
    "trouser",
    "skirt",
    "short"
  ]);

  const hasShoes = hasOutfitCategorySignal(fullOutfit, [
    "shoe",
    "sneaker",
    "boot",
    "heel",
    "loafer"
  ]);

  if (!hasTop) missing.push("top");
  if (!hasBottom) missing.push("bottom");
  if (!hasShoes) missing.push("shoes");

  return missing;
}

function getOutfitUnknownStockCount(fullOutfit) {
  return getOutfitItems(fullOutfit).filter(item => getItemStockValue(item) === null).length;
}

function getOutfitOutOfStockCount(fullOutfit) {
  return getOutfitItems(fullOutfit).filter(item => {
    const stock = getItemStockValue(item);
    return stock !== null && stock <= 0;
  }).length;
}

function getOutfitLowStockCount(fullOutfit) {
  return getOutfitItems(fullOutfit).filter(item => {
    const stock = getItemStockValue(item);
    return stock !== null && stock > 0 && stock <= 2;
  }).length;
}

function getOutfitRiskChecks(fullOutfit) {
  const risks = [];
  const prefs = normalizeMirrorPreferences(window.currentMirrorPreferences || {});
  const totalPrice = getOutfitTotalPrice(fullOutfit);
  const budgetMax = Number(prefs.budgetMax || 0);
  const missingCategories = getOutfitMissingCategories(fullOutfit);
  const cautionSignals = getOutfitCautionSignals(fullOutfit);
  const unknownStockCount = getOutfitUnknownStockCount(fullOutfit);
  const lowStockCount = getOutfitLowStockCount(fullOutfit);
  const outOfStockCount = getOutfitOutOfStockCount(fullOutfit);
  const advancedScores = getAdvancedOutfitScores(fullOutfit);

  if (budgetMax > 0 && totalPrice > budgetMax) {
    risks.push({
      severity: "high",
      icon: "$",
      title: "Above shopper budget",
      detail: `The full look total is ${formatPrice(totalPrice)}, which is above the saved budget maximum of ${formatPrice(budgetMax)}.`,
      label: "Price Risk"
    });
  } else if (budgetMax > 0 && totalPrice > budgetMax * 0.9) {
    risks.push({
      severity: "medium",
      icon: "$",
      title: "Near budget ceiling",
      detail: `The full look total is ${formatPrice(totalPrice)}, close to the shopper’s saved budget maximum.`,
      label: "Budget Watch"
    });
  }

  if (outOfStockCount > 0) {
    risks.push({
      severity: "high",
      icon: "!",
      title: "Unavailable pieces detected",
      detail: `${outOfStockCount} outfit piece${outOfStockCount === 1 ? "" : "s"} may be out of stock at ${getMirrorStoreDisplayName()}.`,
      label: "Stock Risk"
    });
  }

  if (lowStockCount > 0) {
    risks.push({
      severity: "medium",
      icon: "!",
      title: "Low stock warning",
      detail: `${lowStockCount} outfit piece${lowStockCount === 1 ? "" : "s"} may have very limited quantity available.`,
      label: "Low Stock"
    });
  }

  if (unknownStockCount > 0) {
    risks.push({
      severity: "medium",
      icon: "?",
      title: "Inventory confidence limited",
      detail: `${unknownStockCount} outfit piece${unknownStockCount === 1 ? " has" : "s have"} no exact stock count attached.`,
      label: "Check Stock"
    });
  }

  if (cautionSignals.total > 0) {
    const parts = [
      cautionSignals.avoidedColors.length ? `${cautionSignals.avoidedColors.length} avoided color` : "",
      cautionSignals.dislikedMaterials.length ? `${cautionSignals.dislikedMaterials.length} disliked material` : "",
      cautionSignals.dislikedStyles.length ? `${cautionSignals.dislikedStyles.length} avoided style` : ""
    ].filter(Boolean);

    risks.push({
      severity: "high",
      icon: "!",
      title: "Shopper preference conflict",
      detail: `The outfit overlaps with ${parts.join(", ")} signal${cautionSignals.total === 1 ? "" : "s"}.`,
      label: "Profile Conflict"
    });
  }

  if (missingCategories.length) {
    risks.push({
      severity: missingCategories.includes("shoes") ? "high" : "medium",
      icon: "+",
      title: "Missing outfit category",
      detail: `The outfit may be incomplete because it is missing: ${missingCategories.join(", ")}.`,
      label: "Incomplete Look"
    });
  }

  if (advancedScores.overallScore < 70) {
    risks.push({
      severity: "high",
      icon: "%",
      title: "Low recommendation confidence",
      detail: `The advanced outfit score is ${advancedScores.overallScore}%, so Pixel should refine this before final recommendation.`,
      label: "Low Confidence"
    });
  } else if (advancedScores.overallScore < 82) {
    risks.push({
      severity: "medium",
      icon: "%",
      title: "Moderate recommendation confidence",
      detail: `The advanced outfit score is ${advancedScores.overallScore}%. The look is usable, but there is room to improve it.`,
      label: "Medium Confidence"
    });
  }

  if (!risks.length) {
    risks.push({
      severity: "low",
      icon: "✓",
      title: "Low styling risk",
      detail: "Pixel does not detect major budget, stock, profile, or outfit-completion issues.",
      label: "Ready"
    });
  }

  return risks.slice(0, 6);
}

function getOverallOutfitRiskLevel(risks) {
  if (risks.some(risk => risk.severity === "high")) {
    return {
      level: "high",
      label: "High Risk"
    };
  }

  if (risks.some(risk => risk.severity === "medium")) {
    return {
      level: "medium",
      label: "Medium Risk"
    };
  }

  return {
    level: "low",
    label: "Low Risk"
  };
}

function renderOutfitRiskDetector(fullOutfit) {
  const content = document.getElementById("outfitRiskContent");
  const levelBadge = document.getElementById("outfitRiskLevel");

  if (!content || !levelBadge) return;

  const risks = getOutfitRiskChecks(fullOutfit);
  const riskLevel = getOverallOutfitRiskLevel(risks);

  levelBadge.textContent = riskLevel.label;
  levelBadge.className = `outfit-risk-level ${riskLevel.level}`;

  content.innerHTML = risks.map(risk => `
    <div class="outfit-risk-row ${escapeHtml(risk.severity)}">
      <div class="outfit-risk-icon">${escapeHtml(risk.icon)}</div>

      <div>
        <div class="outfit-risk-title">${escapeHtml(risk.title)}</div>
        <div class="outfit-risk-detail">${escapeHtml(risk.detail)}</div>
      </div>

      <div class="outfit-risk-pill">${escapeHtml(risk.label)}</div>
    </div>
  `).join("");
}

function getPixelAssociateRecommendationStoreKey() {
  const runtime =
    typeof getMirrorRuntime === "function"
      ? getMirrorRuntime()
      : window.MirrorRuntimeState || {};

  const retailerKey =
    runtime.retailerKey ||
    getSelectedRetailerKey?.() ||
    "unknown-retailer";

  const storeCode =
    runtime.storeCode ||
    getSelectedStoreCode?.() ||
    "unknown-store";

  return `pixelAssociateRecommendations:${retailerKey}:${storeCode}`;
}

function getPixelAssociateItemName(item, fallback = "this item") {
  return (
    getItemField(item, "name", "itemName", "productName", "title") ||
    fallback
  );
}

function getPixelAssociateItemCategory(item, fallback = "item") {
  return (
    getItemField(item, "category", "productType", "type") ||
    fallback
  );
}

function getPixelAssociateItemRfid(item) {
  return (
    getItemField(item, "rfid", "itemRfid", "productRfid", "id") ||
    ""
  );
}

function getPixelAssociatePrimaryActionForCategory(category = "") {
  const text = String(category || "").toLowerCase();

  if (text.includes("shoe") || text.includes("sneaker") || text.includes("boot") || text.includes("loafer")) {
    return {
      title: "Build upward from footwear",
      detail: "Use this shoe as the anchor and recommend a bottom, top, and optional layer that support the same color and occasion.",
      action: "Offer full outfit from shoes"
    };
  }

  if (text.includes("pant") || text.includes("jean") || text.includes("trouser") || text.includes("bottom") || text.includes("skirt")) {
    return {
      title: "Complete the silhouette",
      detail: "Recommend a top and footwear that balance the bottom shape, color, and shopper’s selected vibe.",
      action: "Suggest top and shoes"
    };
  }

  if (text.includes("shirt") || text.includes("top") || text.includes("blouse") || text.includes("sweater") || text.includes("tee")) {
    return {
      title: "Complete the base outfit",
      detail: "Recommend a bottom and shoes that match the top’s color, texture, and occasion.",
      action: "Suggest bottom and shoes"
    };
  }

  if (text.includes("jacket") || text.includes("coat") || text.includes("outerwear") || text.includes("layer")) {
    return {
      title: "Style as the finishing layer",
      detail: "Use this layer to elevate a complete outfit and confirm the customer has a compatible base look.",
      action: "Pair with base outfit"
    };
  }

  return {
    title: "Create a complete look",
    detail: "Use this item as the anchor and let Pixel recommend complementary pieces from current store inventory.",
    action: "Create full outfit"
  };
}

function buildProductAssociateRecommendations(item, vibe = "Casual") {
  if (!item || typeof item !== "object") {
    return [];
  }

  const name = getPixelAssociateItemName(item);
  const category = getPixelAssociateItemCategory(item);
  const stock = typeof getItemStockValue === "function" ? getItemStockValue(item) : null;
  const price = Number(getItemField(item, "price") || 0);
  const prefs = normalizeMirrorPreferences(window.currentMirrorPreferences || {});
  const budgetMax = Number(prefs.budgetMax || 0);
  const preferenceInsights =
    typeof getPreferenceMatchInsights === "function"
      ? getPreferenceMatchInsights(item)
      : [];

  const recommendations = [];

  const categoryAction = getPixelAssociatePrimaryActionForCategory(category);

  recommendations.push({
    id: "product-category-next-best-action",
    scope: "product",
    priority: "high",
    title: categoryAction.title,
    detail: categoryAction.detail,
    action: categoryAction.action,
    source: "category",
    itemName: name,
    itemCategory: category
  });

  if (stock !== null && stock <= 0) {
    recommendations.push({
      id: "product-stock-unavailable",
      scope: "product",
      priority: "high",
      title: "Confirm availability before recommending",
      detail: `${name} appears unavailable or low-confidence in current stock. Offer an alternative before the customer commits.`,
      action: "Check inventory or swap item",
      source: "inventory",
      itemName: name
    });
  } else if (stock !== null && stock <= 2) {
    recommendations.push({
      id: "product-stock-low",
      scope: "product",
      priority: "medium",
      title: "Low stock urgency",
      detail: `${name} has limited quantity. Let the customer know availability may be time-sensitive.`,
      action: "Confirm size and reserve",
      source: "inventory",
      itemName: name
    });
  } else {
    recommendations.push({
      id: "product-stock-ready",
      scope: "product",
      priority: "low",
      title: "Inventory confidence looks good",
      detail: stock === null
        ? "Exact stock is not attached, but the item is available enough to continue the styling flow."
        : `${name} has ${stock} unit${stock === 1 ? "" : "s"} available.`,
      action: "Proceed with styling",
      source: "inventory",
      itemName: name
    });
  }

  const positiveInsights = preferenceInsights.filter(insight => insight.type === "positive");
  const warningInsights = preferenceInsights.filter(insight => insight.type === "warning");

  if (positiveInsights.length) {
    recommendations.push({
      id: "product-profile-match",
      scope: "product",
      priority: "medium",
      title: "Mention shopper preference match",
      detail: positiveInsights[0].text || `${name} aligns with saved shopper preferences.`,
      action: "Use preference match in conversation",
      source: "shopper-profile",
      itemName: name
    });
  }

  if (warningInsights.length) {
    recommendations.push({
      id: "product-profile-caution",
      scope: "product",
      priority: "high",
      title: "Address shopper preference caution",
      detail: warningInsights[0].text || `${name} may conflict with a saved avoid signal.`,
      action: "Offer a safer alternative",
      source: "shopper-profile",
      itemName: name
    });
  }

  if (budgetMax > 0 && price > budgetMax) {
    recommendations.push({
      id: "product-budget-warning",
      scope: "product",
      priority: "high",
      title: "Above shopper budget",
      detail: `${name} is ${formatPrice(price)}, above the saved budget maximum of ${formatPrice(budgetMax)}.`,
      action: "Offer lower-price alternative",
      source: "budget",
      itemName: name
    });
  }

  recommendations.push({
    id: "product-save-intent",
    scope: "product",
    priority: "low",
    title: "Capture customer intent",
    detail: `If the customer likes this ${category.toLowerCase()}, save it to the bag so the associate can continue the session.`,
    action: "Save to bag if interested",
    source: "commerce",
    itemName: name,
    vibe
  });

  return recommendations.slice(0, 6);
}

function buildOutfitAssociateRecommendations(fullOutfit) {
  if (!fullOutfit || typeof fullOutfit !== "object") {
    return [];
  }

  const risks =
    typeof getOutfitRiskChecks === "function"
      ? getOutfitRiskChecks(fullOutfit)
      : [];

  const riskLevel =
    typeof getOverallOutfitRiskLevel === "function"
      ? getOverallOutfitRiskLevel(risks)
      : { level: "low", label: "Low Risk" };

  const analysis =
    typeof getAdvancedOutfitScores === "function"
      ? getAdvancedOutfitScores(fullOutfit)
      : null;

  const totalPrice =
    typeof getOutfitTotalPrice === "function"
      ? getOutfitTotalPrice(fullOutfit)
      : 0;

  const pieces =
    typeof getMirrorMainOutfitBagPieces === "function"
      ? getMirrorMainOutfitBagPieces(fullOutfit)
      : getMirrorMainOutfitPieces(fullOutfit).map(([role, item]) => ({
          role,
          item,
          rfid: getPixelAssociateItemRfid(item),
          name: getPixelAssociateItemName(item, role)
        }));

  const recommendations = [];

  if (riskLevel.level === "high") {
    const highRisk = risks.find(risk => risk.severity === "high");

    recommendations.push({
      id: "outfit-high-risk-review",
      scope: "outfit",
      priority: "high",
      title: "Review before presenting as final",
      detail: highRisk?.detail || "Pixel detected a high-risk issue in this complete look.",
      action: highRisk?.label === "Stock Risk"
        ? "Check inventory or swap unavailable pieces"
        : "Refine look before customer commitment",
      source: "risk",
      riskLevel: riskLevel.label
    });
  } else if (riskLevel.level === "medium") {
    const mediumRisk = risks.find(risk => risk.severity === "medium");

    recommendations.push({
      id: "outfit-medium-risk-followup",
      scope: "outfit",
      priority: "medium",
      title: "Mention one styling caveat",
      detail: mediumRisk?.detail || "This outfit is usable, but one area may benefit from associate confirmation.",
      action: "Confirm fit, stock, or preference fit",
      source: "risk",
      riskLevel: riskLevel.label
    });
  } else {
    recommendations.push({
      id: "outfit-ready-to-present",
      scope: "outfit",
      priority: "low",
      title: "Ready to present",
      detail: "Pixel does not detect major budget, stock, profile, or completion risks.",
      action: "Present look and offer to save",
      source: "risk",
      riskLevel: riskLevel.label
    });
  }

  if (analysis?.overallScore) {
    recommendations.push({
      id: "outfit-score-talk-track",
      scope: "outfit",
      priority: analysis.overallScore >= 85 ? "low" : "medium",
      title: `Use the ${analysis.overallScore}% outfit score`,
      detail: "Explain that Pixel checked color harmony, budget fit, profile match, availability, and styling balance.",
      action: "Use score as confidence cue",
      source: "outfit-score",
      outfitScore: analysis.overallScore
    });
  }

  if (pieces.length) {
    recommendations.push({
      id: "outfit-save-complete-look",
      scope: "outfit",
      priority: "medium",
      title: "Save the complete look",
      detail: `This outfit has ${pieces.length} RFID-backed piece${pieces.length === 1 ? "" : "s"} and can be saved into the customer bag.`,
      action: "Save complete look",
      source: "commerce",
      pieceCount: pieces.length
    });
  }

  if (totalPrice > 0) {
    recommendations.push({
      id: "outfit-total-price",
      scope: "outfit",
      priority: "low",
      title: "Mention total look value",
      detail: `The full outfit total is ${formatPrice(totalPrice)}.`,
      action: "Use total as checkout context",
      source: "commerce",
      totalPrice
    });
  }

  const missingCategories =
    typeof getOutfitMissingCategories === "function"
      ? getOutfitMissingCategories(fullOutfit)
      : [];

  if (missingCategories.length) {
    recommendations.push({
      id: "outfit-complete-missing-category",
      scope: "outfit",
      priority: "medium",
      title: "Complete missing category",
      detail: `The outfit may be stronger with ${missingCategories.join(", ")}.`,
      action: "Offer complementary piece",
      source: "styling-balance",
      missingCategories
    });
  }

  return recommendations.slice(0, 6);
}

function setPixelAssociateRecommendations(payload) {
  const safePayload = {
    updatedAt: new Date().toISOString(),
    storeName:
      getMirrorShowroomDisplayStore?.() ||
      getMirrorStoreDisplayName?.() ||
      "Current Store",
    retailerKey: getSelectedRetailerKey?.() || "",
    storeCode: getSelectedStoreCode?.() || "",
    ...payload,
    recommendations: Array.isArray(payload?.recommendations)
      ? payload.recommendations
      : []
  };

  window.PixelAssociateRecommendations = safePayload;

  try {
    localStorage.setItem(
      getPixelAssociateRecommendationStoreKey(),
      JSON.stringify(safePayload)
    );
  } catch (error) {
    console.warn("Unable to persist associate recommendations:", error);
  }

  console.group("Pixel Associate Recommendations");
  console.table(
    safePayload.recommendations.map(item => ({
      priority: item.priority,
      scope: item.scope,
      title: item.title,
      action: item.action,
      source: item.source
    }))
  );
  console.log("Full recommendations payload:", safePayload);
  console.groupEnd();

  return safePayload;
}

function updatePixelAssociateRecommendationsForProduct(item, vibe = "Casual") {
  const recommendations = buildProductAssociateRecommendations(item, vibe);

  return setPixelAssociateRecommendations({
    scope: "product",
    item: item || null,
    outfit: null,
    recommendations
  });
}

function updatePixelAssociateRecommendationsForOutfit(fullOutfit) {
  const recommendations = buildOutfitAssociateRecommendations(fullOutfit);

  return setPixelAssociateRecommendations({
    scope: "outfit",
    item: currentLoadedItem || lastScannedItem || null,
    outfit: fullOutfit || null,
    recommendations
  });
}

function getPixelAssociateRecommendations() {
  if (window.PixelAssociateRecommendations) {
    return window.PixelAssociateRecommendations;
  }

  try {
    return safeParseJson(localStorage.getItem(getPixelAssociateRecommendationStoreKey())) || {
      recommendations: []
    };
  } catch (_) {
    return {
      recommendations: []
    };
  }
}

function renderOutfitCard(item, role = "Piece") {
  if (!item || typeof item !== "object") {
    return `
      <article class="outfit-card">
        <div class="outfit-image">
          <img
            src="https://placehold.co/400x500/f7f3ec/171411?text=Item"
            alt="${escapeHtml(role)}"
          >
        </div>

        <div class="outfit-card-body">
          <p class="outfit-role">${escapeHtml(role)}</p>
          <h4>${escapeHtml(role)}</h4>
          <span>Recommended piece</span>
        </div>
      </article>
    `;
  }

  const name =
    getItemField(item, "name", "itemName", "productName", "title") ||
    role ||
    "Outfit Item";

  const brand =
    getItemField(item, "brand", "retailer", "retailerName") ||
    getMirrorStoreDisplayName?.() ||
    "Store";

  const category = getItemField(item, "category", "productType", "type") || "";
  const color = getItemField(item, "color") || "";
  const price = getItemField(item, "price");

  const imageUrl = safeImageUrl(
    getItemField(
      item,
      "imageUrl",
      "image_url",
      "image",
      "photoUrl",
      "productImageUrl",
      "primaryImage",
      "primaryImageUrl",
      "thumbnailUrl"
    ),
    `https://placehold.co/400x500/f7f3ec/171411?text=${encodeURIComponent(name)}`
  );

  const meta = [brand, category, color].filter(Boolean).join(" • ");

  return `
    <article class="outfit-card">
      <div class="outfit-image">
        <img
          src="${escapeHtml(imageUrl)}"
          alt="${escapeHtml(name)}"
          loading="lazy"
          onerror="this.src='https://placehold.co/400x500/f7f3ec/171411?text=Item';"
        >
      </div>

      <div class="outfit-card-body">
        <p class="outfit-role">${escapeHtml(role)}</p>
        <h4>${escapeHtml(name)}</h4>
        <span>
          ${escapeHtml(meta || "Recommended piece")}
          ${Number.isFinite(Number(price)) ? ` • ${escapeHtml(formatPrice(price))}` : ""}
        </span>
      </div>
    </article>
  `;
}

 function renderFullOutfit(fullOutfit, shouldScroll = true) {
  const panel = document.getElementById("outfitPanel");
  const grid = document.getElementById("outfitGrid");
  const explanation = document.getElementById("outfitExplanation");
  const score = document.getElementById("outfitScore");

  if (!panel || !grid || !explanation || !score) return;

  if (!fullOutfit) {
    panel.classList.remove("show");

    if (typeof updateMirrorConciergeForOutfit === "function") {
      updateMirrorConciergeForOutfit(null);
    }

    return;
  }

  const scannedRfid =
    currentRfid ||
    getItemField(lastScannedItem, "rfid", "itemRfid", "productRfid", "id") ||
    "";

  const pieces = [
    ["Top", fullOutfit.top],
    ["Bottom", fullOutfit.bottom],
    ["Shoes", fullOutfit.shoes],
    ["Outerwear", fullOutfit.outerwear]
  ].filter(([, item]) => {
    const rfid = getItemField(item, "rfid", "itemRfid", "productRfid", "id");
    return item && rfid !== scannedRfid;
  });

  grid.innerHTML = pieces.length
    ? pieces.map(([role, item]) => renderOutfitCard(item, role)).join("")
    : `<div class="empty">Additional styled pieces will appear here.</div>`;

  explanation.textContent =
    fullOutfit.explanation || "This outfit is built around your scanned item.";

  score.textContent = `${safeNumber(fullOutfit.overallScore)}%`;

  renderOutfitPreferenceInsights(fullOutfit);
  renderOutfitScoreBreakdown(fullOutfit);
  renderOutfitImprovementSuggestions(fullOutfit);
  renderOutfitRiskDetector(fullOutfit);

    if (typeof updatePixelAssociateRecommendationsForOutfit === "function") {
      updatePixelAssociateRecommendationsForOutfit(fullOutfit);
    }

    if (typeof updateMirrorConciergeForOutfit === "function") {
      updateMirrorConciergeForOutfit(fullOutfit);
    }

    if (typeof renderMirrorMainOutfitShowcase === "function") {
      renderMirrorMainOutfitShowcase(fullOutfit);
    }

    revealPanel(panel);

  if (shouldScroll) {
    window.requestAnimationFrame(() => {
      scrollToPanel(panel, { gap: 96 });
    });
  }
}

 function resetMirror() {
   handleAmbientWakeInteraction();

   currentRfid = "";
   currentLoadedItem = null;
   lastScannedItem = null;

   const rfidInput = document.getElementById("rfidInput");
   if (rfidInput) rfidInput.value = "";

   document.getElementById("resultPanel")?.classList.remove("show", "scan-reveal");
   document.getElementById("outfitPanel")?.classList.remove("show", "scan-reveal");

   setSaveButtonDefault(true);

   const hasToken = !!getToken();

   setStatus(
     hasToken ? "Ready for a new scan." : "Login from the Merchant App first.",
     hasToken ? "ready" : "error"
   );

   speakPixelConcierge("reset");

   addTryOnTimelineEvent(
     "session",
     "Mirror reset",
     "Pixel cleared the current scan and returned to the ready state.",
     [getMirrorStoreDisplayName()]
   );

   const scanPanel = document.querySelector(".scan-panel");

   window.requestAnimationFrame(() => {
     scrollToPanel(scanPanel, { gap: 70 });
   });

   resetAmbientIdleTimer();
 }

async function handleScan() {
  handleAmbientWakeInteraction();

  const retailer = getSelectedRetailerKey();
  const storeCode = getSelectedStoreCode();
  const rfidInput = document.getElementById("rfidInput");
  const rfid = normalizeMirrorRfid(rfidInput?.value || "");

  if (rfidInput) {
    rfidInput.value = rfid;
  }
  const vibe = document.getElementById("vibeSelect")?.value || "Casual";

  if (!rfid) {
    setStatus("Enter or scan an RFID tag first.", "error");
    showToast("Enter or scan an RFID tag first.", "error");
    speakPixelConcierge("scanBlocked");
    focusRfidInput();
    return;
  }

  if (!storeCode) {
    setStatus("Select a store first.", "error");
    showToast("Select a store first.", "error");

    speakPixelConcierge("error", {
      errorMessage: "Select a store before scanning."
    });

    return;
  }

  try {
    requireToken();

    setLoading(true);
    setStatus("Analyzing item with Universal Stylist...", "ready");

    speakPixelConcierge("thinking");

    const params = buildMirrorPreferenceQueryParams();

    params.set("retailerKey", retailer);
    params.set("storeCode", storeCode);
    params.set("vibe", vibe);

    const response = await fetch(
      `${API.stylist}/scan/${encodeURIComponent(rfid)}?${params.toString()}`,
      {
        method: "GET",
        headers: getAuthHeaders({
          Accept: "application/json"
        })
      }
    );

    await assertAuthorizedResponse(
      response,
      "Could not scan this item for the selected retailer."
    );

    const data = await response.json();

    if (!data || typeof data !== "object") {
      throw new Error("Scan returned an empty item response.");
    }

    renderScanResult(data, vibe);
    logScanActivity(data, vibe, "mirror");
    pulseScanPanelSuccess();

    if (window.location.search) {
      window.history.replaceState({}, document.title, window.location.pathname);
    }

    const input = document.getElementById("rfidInput");
    if (input) {
      input.value = "";
    }

    const scannedItemName =
      getItemField(data, "name", "itemName", "productName", "title") ||
      "item";

    setStatus(`Loaded ${scannedItemName} successfully.`, "success");
    showToast("Item scanned successfully.", "success");

    speakPixelConcierge("scan", {
      itemName: scannedItemName
    });
  } catch (error) {
    console.error("Mirror scan failed:", error);

    const message = error.message || "Unable to scan item.";

    setStatus(message, "error");
    showToast(message, "error");

    speakPixelConcierge("scanError", {
      errorMessage: message
    });

    if (typeof addTryOnTimelineEvent === "function") {
      addTryOnTimelineEvent(
        "session",
        "Scan failed",
        message,
        [getMirrorStoreDisplayName()]
      );
    }
  } finally {
    setLoading(false);
  }
}

 async function createFullLook() {
   if (!lastScannedItem) {
     setStatus("Scan an item first to create a full look.", "error");
     showToast("Scan an item first.", "error");
     speakPixelConcierge("outfitBlocked");
     return;
   }

   const rfid = normalizeMirrorRfid(
     currentRfid ||
       getItemField(lastScannedItem, "rfid", "itemRfid", "productRfid", "id")
   );

   const retailerKey = getSelectedRetailerKey();
   const storeCode = getSelectedStoreCode();
   const vibe = document.getElementById("vibeSelect")?.value || "Casual";

   const legacyButton = document.getElementById("createLookBtn");
   const mainButton = document.getElementById("mirrorMainCreateLookBtn");
   const buttons = [legacyButton, mainButton].filter(Boolean);

   if (window.MirrorCustomerJourney?.outfit) {
     window.MirrorCustomerJourney.outfit(lastScannedItem || currentLoadedItem);
   } else if (typeof setMirrorCustomerStage === "function") {
     setMirrorCustomerStage(MIRROR_CUSTOMER_STAGES.OUTFIT, {
       item: lastScannedItem || currentLoadedItem
     });
   }

   if (!rfid) {
     const message = "Missing RFID for look generation.";
     showToast(message, "error");
     setStatus(message, "error");

     if (typeof showMirrorMainOutfitError === "function") {
       showMirrorMainOutfitError(message);
     }

     return;
   }

   if (!storeCode) {
     const message = "Missing store code for look generation.";
     showToast(message, "error");
     setStatus(message, "error");

     if (typeof showMirrorMainOutfitError === "function") {
       showMirrorMainOutfitError(message);
     }

     return;
   }

   try {
     requireToken();

     buttons.forEach(button => {
       button.disabled = true;
       button.textContent = "Creating Look...";
     });

     setStatus("Creating a complete outfit...", "ready");
     speakPixelConcierge("creatingLook");

     if (currentMirrorMainPendingFullOutfit) {
       const fullOutfit = currentMirrorMainPendingFullOutfit;

       console.log("Using fullOutfit returned from scan response:", fullOutfit);

       renderFullOutfit(fullOutfit, true);
       incrementTryOnLooksCreated();

       const outfitAnalysis =
         typeof getAdvancedOutfitScores === "function"
           ? getAdvancedOutfitScores(fullOutfit)
           : null;

       setStatus("Full look created.", "success");
       showToast("Full look created.", "success");

       speakPixelConcierge("outfit", {
         totalPrice:
           typeof getOutfitTotalPrice === "function"
             ? getOutfitTotalPrice(fullOutfit)
             : 0
       });

       if (outfitAnalysis?.overallScore) {
         window.setTimeout(() => {
           speakPixelConcierge("outfitScore", {
             score: outfitAnalysis.overallScore
           });
         }, 1800);
       }

       return;
     }

     const params = buildMirrorPreferenceQueryParams();

     params.set("retailerKey", retailerKey);
     params.set("storeCode", storeCode);
     params.set("vibe", vibe);

     const lookUrl = `${API.stylist}/look/${encodeURIComponent(rfid)}?${params.toString()}`;

     console.group("Pixel full-look request");
     console.table({
       rfid,
       retailerKey,
       storeCode,
       vibe,
       lookUrl,
       hasToken: !!getToken(),
       scannedItemName:
         getItemField(lastScannedItem, "name", "itemName", "productName", "title") ||
         "Unknown"
     });
     console.groupEnd();

     const response = await fetch(lookUrl, {
       method: "GET",
       headers: getAuthHeaders({
         Accept: "application/json"
       })
     });

     const rawText = await response.text();

     let look = null;

     try {
       look = rawText ? JSON.parse(rawText) : null;
     } catch (_) {
       look = null;
     }

     if (!response.ok) {
       console.group("Pixel full-look backend error");
       console.table({
         status: response.status,
         statusText: response.statusText,
         url: lookUrl
       });
       console.log("Raw backend response:", rawText);
       console.log("Parsed backend response:", look);
       console.groupEnd();

       const backendMessage =
         look?.message ||
         look?.detail ||
         look?.error ||
         rawText ||
         "Unable to generate full look.";

       throw new Error(cleanApiErrorMessage(backendMessage));
     }

     if (!look || typeof look !== "object") {
       throw new Error("Look endpoint returned an empty response.");
     }

     const fullOutfit = look.fullOutfit || look;

     const outfitPieces = getMirrorMainOutfitPieces(fullOutfit);

     if (!outfitPieces.length) {
       console.log("Look response did not contain usable outfit pieces:", look);
       throw new Error("Look endpoint returned no outfit pieces for this RFID.");
     }

     renderFullOutfit(fullOutfit, true);
     incrementTryOnLooksCreated();

     const outfitAnalysis =
       typeof getAdvancedOutfitScores === "function"
         ? getAdvancedOutfitScores(fullOutfit)
         : null;

     setStatus("Full look created.", "success");
     showToast("Full look created.", "success");

     speakPixelConcierge("outfit", {
       totalPrice:
         typeof getOutfitTotalPrice === "function"
           ? getOutfitTotalPrice(fullOutfit)
           : 0
     });

     if (outfitAnalysis?.overallScore) {
       window.setTimeout(() => {
         speakPixelConcierge("outfitScore", {
           score: outfitAnalysis.overallScore
         });
       }, 1800);
     }
   } catch (error) {
     console.error("Create look failed:", error);

     const message =
       error.message ||
       "Unable to generate this complete look. Check RFID, store code, and backend outfit inventory.";

     if (typeof showMirrorMainOutfitError === "function") {
       showMirrorMainOutfitError(message);
     }

     setStatus(message, "error");
     showToast(message, "error");

     speakPixelConcierge("outfitError", {
       errorMessage: message
     });
   } finally {
     buttons.forEach(button => {
       button.disabled = false;
       button.textContent =
         button.id === "mirrorMainCreateLookBtn"
           ? "Create Full Outfit"
           : "Create Full Look";
     });
   }
 }

 async function saveToBag() {
   if (!currentRfid || !currentLoadedItem) {
     setStatus("Scan an item before saving it to your bag.", "error");
     showToast("Scan an item first.", "error");
     speakPixelConcierge("saveBlocked");
     return false;
   }

   const saveButtons = typeof getMirrorSaveButtons === "function"
     ? getMirrorSaveButtons()
     : [document.getElementById("saveToBagBtn"), document.getElementById("mirrorMainAddToLookBtn")]
         .filter(Boolean);

   const savedItemName =
     getItemField(currentLoadedItem, "name", "itemName", "productName", "title") ||
     "This item";

   try {
     requireToken();

     saveButtons.forEach(button => {
       button.disabled = true;
       button.textContent = "Saving...";
     });

     speakPixelConcierge("saving", {
       itemName: savedItemName
     });

     const response = await fetch(`${API.stylist}/save/${encodeURIComponent(currentRfid)}`, {
       method: "POST",
       headers: getAuthHeaders()
     });

     await assertAuthorizedResponse(response, "Unable to save item.");

     const message = await response.text();

     savedRfids.add(currentRfid);
     markLatestScanSavedToBag(currentRfid);
     setSaveButtonSaved();
     incrementTryOnSavesToBag();

     setStatus(message || "Item saved to bag.", "success");
     showToast(message || "Item saved to bag.", "success");

     speakPixelConcierge("save", {
       itemName: savedItemName,
       bagCount: savedRfids.size
     });

     await loadBag(false);

     updateMirrorMainBagCount?.();
     updateMirrorMainProductCard?.();

     if (
       document.getElementById("mirrorMainBagDrawer")?.classList.contains("is-active") &&
       typeof renderMirrorMainBagDrawer === "function"
     ) {
       await renderMirrorMainBagDrawer();
     }

     if (window.MirrorCustomerJourney?.saved) {
       window.MirrorCustomerJourney.saved(currentLoadedItem);
     }

     return true;
   } catch (error) {
     console.error("Save failed:", error);

     const message = error.message || "Unable to save item.";

     setSaveButtonDefault(false);

     saveButtons.forEach(button => {
       button.disabled = false;

       if (button.id === "mirrorMainAddToLookBtn") {
         button.textContent = "Save to Bag";
       }
     });

     setStatus(message, "error");
     showToast(message, "error");

     speakPixelConcierge("saveError", {
       errorMessage: message
     });

     updateMirrorMainBagCount?.();

     return false;
   }
 }

  async function loadBag(showPanel = true) {
    const bagPanel = document.getElementById("bagPanel");
    const container = document.getElementById("bagContent");

    if (!container) return;

    if (!getToken()) {
      savedRfids = new Set();

      container.innerHTML = `
        <div class="empty">
          Login from the Merchant App to view your bag.
        </div>
      `;

      if (showPanel) {
        revealPanel(bagPanel);

        window.requestAnimationFrame(() => {
          scrollToPanel(bagPanel, { gap: 86 });
        });
      }

      return;
    }

    container.innerHTML = `<div class="empty">Refreshing bag...</div>`;

    if (showPanel) {
      speakPixelConcierge("loadingBag");
    }

    try {
      const response = await fetch(`${API.stylist}/bag`, {
        headers: getAuthHeaders({
          Accept: "application/json"
        })
      });

      await assertAuthorizedResponse(response, "Unable to load bag.");

      const bag = await response.json();
      const items = Array.isArray(bag.items) ? bag.items : [];

      savedRfids = new Set(
        items
          .map(item => item.rfid || item.itemRfid || item.productRfid)
          .filter(Boolean)
      );

      if (showPanel) {
        speakPixelConcierge("bag", {
          bagCount: items.length
        });
      }

      if (!items.length) {
        container.innerHTML = `
          <div class="empty">
            Your style bag is empty. Scan and save pieces to build your look.
          </div>
        `;
      } else {
        container.innerHTML = items.map(item => {
          const id = item.id || "";
          const name = item.itemName || item.name || "Unnamed Item";
          const retailer = item.retailerName || item.retailer || "Retailer";
          const category = item.category || "Item";
          const price = item.price;
          const imageUrl = safeImageUrl(
            item.imageUrl,
            "https://placehold.co/120x120?text=Item"
          );

          return `
            <div class="bag-item">
              <img
                src="${imageUrl}"
                alt="${escapeHtml(name)}"
                onerror="this.src='https://placehold.co/120x120?text=Item';"
              />

              <div>
                <div class="bag-name">${escapeHtml(name)}</div>
                <div class="bag-meta">
                  ${escapeHtml(retailer)} • ${escapeHtml(category)} • ${formatPrice(price)}
                </div>
              </div>

              <button
                class="mirror-btn"
                type="button"
                data-remove-id="${escapeHtml(id)}"
                data-remove-name="${escapeHtml(name)}"
              >
                Remove
              </button>
            </div>
          `;
        }).join("");

        container.querySelectorAll("[data-remove-id]").forEach(button => {
          button.addEventListener("click", () => {
            removeFromBag(
              button.dataset.removeId || "",
              button.dataset.removeName || "This item"
            );
          });
        });
      }

      if (currentLoadedItem) {
        if (isCurrentItemSaved(currentLoadedItem)) {
          setSaveButtonSaved();
        } else {
          setSaveButtonDefault(false);
        }
      }

      if (showPanel) {
        revealPanel(bagPanel);

        window.requestAnimationFrame(() => {
          scrollToPanel(bagPanel, { gap: 86 });
        });
      }
    } catch (error) {
      console.error("Bag load failed:", error);

      const message = error.message || "Unable to load bag.";

      container.innerHTML = `
        <div class="empty">
          Unable to load bag right now.
        </div>
      `;

      if (showPanel) {
        revealPanel(bagPanel);

        window.requestAnimationFrame(() => {
          scrollToPanel(bagPanel, { gap: 86 });
        });
      }

      showToast(message, "error");

      speakPixelConcierge("error", {
        errorMessage: message
      });
    }
  }

 async function removeFromBag(id, itemName = "This item") {
   if (!id) {
     showToast("Missing bag item id.", "error");

     speakPixelConcierge("removeError", {
       errorMessage: "Missing bag item id."
     });

     return;
   }

   try {
     requireToken();

     const response = await fetch(`${API.stylist}/bag/${encodeURIComponent(id)}`, {
       method: "DELETE",
       headers: getAuthHeaders()
     });

     await assertAuthorizedResponse(response, "Unable to remove item.");

     const nextBagCount = Math.max(0, savedRfids.size - 1);

     showToast(`${itemName} removed from bag.`, "success");

     speakPixelConcierge("remove", {
       itemName,
       bagCount: nextBagCount
     });

     addTryOnTimelineEvent(
       "remove",
       "Removed item from bag",
       `${itemName} was removed from the shopper’s style bag.`,
       [
         getMirrorStoreDisplayName(),
         `${nextBagCount} saved piece${nextBagCount === 1 ? "" : "s"} left`
       ]
     );

     await loadBag(true);
   } catch (error) {
     console.error("Remove failed:", error);

     const message = error.message || "Unable to remove item.";

     showToast(message, "error");

     speakPixelConcierge("removeError", {
       errorMessage: message
     });
   }
 }

  function quickScan(retailerKey, rfid, storeCodeOverride = "") {
  const retailerSelect = document.getElementById("retailerSelect");
  const storeSelect = document.getElementById("storeCodeSelect");
  const rfidInput = document.getElementById("rfidInput");
  const vibeSelect = document.getElementById("vibeSelect");

  const currentStoreCode =
    storeCodeOverride ||
    getSelectedStoreCode() ||
    localStorage.getItem("currentStoreCode") ||
    localStorage.getItem("storeCode") ||
    "";

  const safeRetailerKey =
    retailerKey ||
    getSelectedRetailerKey() ||
    findRetailerKeyByStoreCode(currentStoreCode) ||
    "MACY001";

  if (retailerSelect) {
    retailerSelect.value = safeRetailerKey;
  }

  populateStoreOptions(safeRetailerKey, currentStoreCode);

  if (storeSelect && currentStoreCode) {
    const storeExists = Array.from(storeSelect.options).some(
      option => option.value === currentStoreCode
    );

    if (storeExists) {
      storeSelect.value = currentStoreCode;
    }
  }

 if (rfidInput) {
   rfidInput.value = normalizeMirrorRfid(rfid);
 }

  if (vibeSelect && !vibeSelect.value) {
    vibeSelect.value = "Casual";
  }

  if (!getSelectedStoreCode()) {
    setStatus("Quick scan is missing store context.", "error");
    showToast("Quick scan is missing store context.", "error");

    speakPixelConcierge("error", {
    errorMessage: "Quick scan is missing store context."

      });

    return;
  }

  handleScan();
}

  function hydrateFromUrlParams() {
    const params = new URLSearchParams(window.location.search);

    const retailer = params.get("retailer") || "";
    const storeCode = params.get("storeCode") || "";
    const storeName = params.get("storeName") || "";
    const rfid = params.get("rfid") || "";
    const vibe = params.get("vibe") || "";
    const autoScan = params.get("autoScan") === "true";

    const retailerSelect = document.getElementById("retailerSelect");
    const storeCodeSelect = document.getElementById("storeCodeSelect");
    const rfidInput = document.getElementById("rfidInput");
    const vibeSelect = document.getElementById("vibeSelect");

    const resolvedRetailer =
      retailer ||
      localStorage.getItem("retailerKey") ||
      localStorage.getItem("currentRetailerKey") ||
      "MACY001";

    const resolvedStoreCode =
      storeCode ||
      localStorage.getItem("storeCode") ||
      localStorage.getItem("currentStoreCode") ||
      "";

    const resolvedStoreName =
      storeName ||
      localStorage.getItem("currentStoreName") ||
      localStorage.getItem("storeName") ||
      resolvedStoreCode ||
      "";

    if (resolvedRetailer) {
      localStorage.setItem("retailerKey", resolvedRetailer);
      localStorage.setItem("currentRetailerKey", resolvedRetailer);
    }

    if (resolvedStoreCode) {
      localStorage.setItem("storeCode", resolvedStoreCode);
      localStorage.setItem("currentStoreCode", resolvedStoreCode);
    }

    if (resolvedStoreName) {
      localStorage.setItem("storeName", beautifyStoreName(resolvedStoreName));
      localStorage.setItem("currentStoreName", beautifyStoreName(resolvedStoreName));
    }

    populateRetailerSelect(resolvedRetailer);
    populateStoreOptions(resolvedRetailer, resolvedStoreCode);

    if (retailerSelect) {
      const retailerExists = Array.from(retailerSelect.options).some(
        option => option.value === resolvedRetailer
      );

      if (!retailerExists && resolvedRetailer) {
        const option = document.createElement("option");
        option.value = resolvedRetailer;
        option.textContent = beautifyRetailerName(resolvedStoreName || resolvedRetailer);
        retailerSelect.prepend(option);
      }

      retailerSelect.value = resolvedRetailer;
    }

    if (storeCodeSelect && resolvedStoreCode) {
      const storeExists = Array.from(storeCodeSelect.options).some(
        option => option.value === resolvedStoreCode
      );

      if (!storeExists) {
        const option = document.createElement("option");
        option.value = resolvedStoreCode;
        option.textContent = beautifyStoreName(resolvedStoreName || resolvedStoreCode);
        storeCodeSelect.prepend(option);
      }

      storeCodeSelect.value = resolvedStoreCode;
    }

    if (rfid && rfidInput) {
      rfidInput.value = rfid;
    }

    if (vibe && vibeSelect) {
      const vibeExists = Array.from(vibeSelect.options).some(
        option => option.value === vibe
      );

      if (vibeExists) {
        vibeSelect.value = vibe;
      }
    }

    if (resolvedStoreName || resolvedStoreCode || resolvedRetailer) {
      setStatus(
        `Mirror loaded for ${beautifyStoreName(resolvedStoreName || resolvedStoreCode || resolvedRetailer)}.`,
        "success"
      );
    }

    if (autoScan && rfid) {
      setStatus("Mirror test loaded from inventory. Starting scan...", "ready");

      window.setTimeout(() => {
        handleScan();
      }, 450);
    }
  }

function beautifyStoreName(value) {
  const text = String(value || "").trim();

  if (!text) return "";

  return text
    .replace(/^MCS\d+-/i, "")
    .replace(/\s+/g, " ")
    .replace(/-/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, letter => letter.toUpperCase());
}

function beautifyRetailerName(value) {
  const text = String(value || "").trim();

  if (!text) return "";

  return text
    .replace(/^MCS\d+$/i, "Nicks Boutique")
    .replace(/^MACY\d+$/i, "Macy's")
    .replace(/^KINGS\d+$/i, "Kings Boutique")
    .replace(/^NICKS\d+$/i, "Nicks Boutique")
    .replace(/-/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, letter => letter.toUpperCase())
    .replace(/\bMacy'S\b/g, "Macy's");
}

 function getMirrorStoreDisplayName() {
   const storeSelect = document.getElementById("storeCodeSelect");
   const selectedStoreName =
     storeSelect?.selectedOptions?.[0]?.textContent?.trim() || "";

   const selectedStoreCode = getSelectedStoreCode();
   const params = new URLSearchParams(window.location.search);

   const rawName =
     selectedStoreName ||
     params.get("storeName") ||
     localStorage.getItem("currentStoreName") ||
     localStorage.getItem("storeName") ||
     selectedStoreCode ||
     params.get("storeCode") ||
     getSelectedRetailerKey() ||
     "Current Store";

   return beautifyStoreName(rawName);
 }

 function persistCurrentMirrorStoreContext() {
   const retailerKey = getSelectedRetailerKey();
   const storeCode = getSelectedStoreCode();
   const storeName = beautifyStoreName(getMirrorStoreDisplayName());

   if (retailerKey) {
     localStorage.setItem("retailerKey", retailerKey);
     localStorage.setItem("currentRetailerKey", retailerKey);
   }

   if (storeCode) {
     localStorage.setItem("storeCode", storeCode);
     localStorage.setItem("currentStoreCode", storeCode);
   }

   if (storeName) {
     localStorage.setItem("storeName", storeName);
     localStorage.setItem("currentStoreName", storeName);
   }

   return {
     retailerKey,
     storeCode,
     storeName
   };
 }

  function getMirrorReadyCopy() {
  const storeName = getMirrorStoreDisplayName();
  const retailerName =
    RETAILER_CONFIG?.getRetailerName?.(getSelectedRetailerKey()) ||
    getSelectedRetailerKey() ||
    "Store";

  return {
    eyebrow: `${storeName} Smart Mirror`,
    title: `${storeName} mirror is ready.`,
    body: `This mirror is locked to ${storeName}. Scan RFID items, create complete looks, and save pieces using this store’s live inventory.`,
    status: "Mirror session ready",
    retailerName,
    storeName
  };
}

function renderStoreOwnedReadyState() {
  const copy = getMirrorReadyCopy();

  const heroEyebrow = document.querySelector(".hero-copy .eyebrow");
  const heroSubtitle = document.querySelector(".hero-subtitle");
  const panelLabel = document.querySelector(".scan-panel .panel-label");
  const scanTitle = document.querySelector(".scan-title");
  const scanCopy = document.querySelector(".scan-copy");
  const readyCard = document.getElementById("readyCard");

  if (heroEyebrow) {
    heroEyebrow.textContent = `${copy.storeName} • Store-Locked Smart Mirror`;
  }

  if (heroSubtitle) {
    heroSubtitle.textContent =
      `A premium smart mirror experience for ${copy.storeName}, turning this store’s live inventory into styling advice, complete outfit recommendations, and saved shopper intent.`;
  }

  if (panelLabel) {
    panelLabel.textContent = "Store-Locked Scan Console";
  }

  if (scanTitle) {
    scanTitle.textContent = "Place a store item near the smart mirror.";
  }

  if (scanCopy) {
    scanCopy.textContent =
      `This session is locked to ${copy.storeName}. Scan an RFID tag or enter one manually to style available inventory from this store only.`;
  }

  if (readyCard) {
    readyCard.innerHTML = `
      <div class="ready-card-top">
        <div class="ready-orb">🪞</div>
        <div class="ready-status">
          <span class="dot"></span>
          ${escapeHtml(copy.status)}
        </div>
      </div>

      <div class="ready-store-lock">
        <span>Store Lock</span>
        <strong>${escapeHtml(copy.storeName)}</strong>
      </div>

      <h3>${escapeHtml(copy.title)}</h3>
      <p>${escapeHtml(copy.body)}</p>
    `;
  }
}

  function renderMirrorQuickScans() {
    const quickScanContainer = document.querySelector(".quick-scans");
    if (!quickScanContainer) return;

    const params = new URLSearchParams(window.location.search);

    const retailerKey =
      params.get("retailer") ||
      getSelectedRetailerKey() ||
      "MACY001";

    const storeName = beautifyStoreName(
      params.get("storeName") ||
      document.getElementById("storeCodeSelect")?.selectedOptions?.[0]?.textContent ||
      RETAILER_CONFIG?.getRetailerName?.(retailerKey) ||
      "Current Store"
    );

    const demoInventory = getMirrorDemoInventoryForRetailer(retailerKey);

    if (!demoInventory.length) {
      quickScanContainer.innerHTML = `
        <button
          class="quick-btn"
          type="button"
          disabled
        >
          ${escapeHtml(storeName)}
        </button>
      `;
      return;
    }

    quickScanContainer.innerHTML = demoInventory.map(item => `
      <button
        class="quick-btn"
        type="button"
        data-retailer="${escapeHtml(retailerKey)}"
        data-rfid="${escapeHtml(item.rfid)}"
      >
        ${escapeHtml(item.label || item.name || item.rfid)}
      </button>
    `).join("");

    quickScanContainer.querySelectorAll(".quick-btn").forEach(button => {
      button.addEventListener("click", () => {
        quickScan(
          button.dataset.retailer || retailerKey,
          button.dataset.rfid || "",
          getSelectedStoreCode()
          );
      });
    });
  }

  function getMirrorDemoInventoryForRetailer(retailerKey) {
   const storedInventory = safeParseJson(localStorage.getItem("merchantInventory")) || [];

    if (!Array.isArray(storedInventory)) {
      return [];
    }

    return storedInventory
      .filter(item => {
        const itemRetailer =
          item.retailerKey ||
          item.retailer ||
          item.retailerCode ||
          "";

        return String(itemRetailer).toLowerCase() === String(retailerKey).toLowerCase();
      })
      .slice(0, 4)
      .map(item => ({
        rfid: item.rfid || item.itemRfid || item.productRfid || "",
        name: item.itemName || item.name || "Quick Scan",
        label: item.itemName || item.name || item.rfid || "Quick Scan"
      }))
      .filter(item => item.rfid);
  }

    const CUSTOMER_PREFERENCES_API = "/api/v1/customer/preferences";

  function normalizePreferenceText(value, fallback = "Not set") {
    const text = String(value ?? "").trim();
    return text || fallback;
  }

  function splitPreferenceList(value) {
    return String(value || "")
      .split(",")
      .map(item => item.trim())
      .filter(Boolean);
  }

  function renderPreferenceChips(value, fallback = "No preferences saved") {
    const items = splitPreferenceList(value);

    if (!items.length) {
      return `<div class="shopper-profile-sub">${escapeHtml(fallback)}</div>`;
    }

    return `
      <div class="shopper-chip-row">
        ${items.slice(0, 8).map(item => `
          <span class="shopper-chip">${escapeHtml(item)}</span>
        `).join("")}
      </div>
    `;
  }

  function getDefaultMirrorPreferences() {
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

  function normalizeMirrorPreferences(preferences) {
    return {
      ...getDefaultMirrorPreferences(),
      ...(preferences && typeof preferences === "object" ? preferences : {})
    };
  }

  async function fetchMirrorPreferences() {
    requireToken();

    const response = await fetch(CUSTOMER_PREFERENCES_API, {
      method: "GET",
      headers: getAuthHeaders({
        Accept: "application/json"
      })
    });

    await assertAuthorizedResponse(response, "Unable to load shopper preferences.");

    if (response.status === 204) {
      return getDefaultMirrorPreferences();
    }

    const data = await response.json().catch(() => null);
    return normalizeMirrorPreferences(data);
  }

  function hasMeaningfulPreferences(preferences) {
    const prefs = normalizeMirrorPreferences(preferences);

    return Object.entries(prefs).some(([key, value]) => {
      if (key === "fitPreference" && value === "Regular") return false;
      if (key === "genderStyle" && value === "Any") return false;
      if (key === "occasionPriority" && value === "Everyday") return false;

      return String(value ?? "").trim() !== "";
    });
  }

  function renderShopperProfile(preferences) {
  const container = document.getElementById("shopperProfileContent");
  if (!container) return;

  const prefs = normalizeMirrorPreferences(preferences);
  const hasPrefs = hasMeaningfulPreferences(prefs);

  if (!hasPrefs) {
    container.innerHTML = `
      <div class="empty">
        No detailed shopper preferences have been saved yet. Open Preferences in the Merchant App to personalize sizing, colors, budget, materials, and style direction.
      </div>
    `;

    if (typeof updateMirrorConciergeForProfile === "function") {
      updateMirrorConciergeForProfile(prefs);
    }

    return;
  }

  const budgetText =
    prefs.budgetMin || prefs.budgetMax
      ? `${prefs.budgetMin ? `$${escapeHtml(prefs.budgetMin)}` : "$0"} – ${prefs.budgetMax ? `$${escapeHtml(prefs.budgetMax)}` : "No max"}`
      : "No budget set";

  const sizeText = [
    prefs.sizeTop ? `Top ${prefs.sizeTop}` : "",
    prefs.sizeBottom ? `Bottom ${prefs.sizeBottom}` : "",
    prefs.shoeSize ? `Shoe ${prefs.shoeSize}` : ""
  ].filter(Boolean).join(" • ") || "No sizes saved";

  container.innerHTML = `
    <div class="shopper-profile-grid">
      <div class="shopper-profile-card">
        <div class="shopper-profile-label">Sizing</div>
        <div class="shopper-profile-value">${escapeHtml(sizeText)}</div>
        <div class="shopper-profile-sub">
          Fit preference: ${escapeHtml(normalizePreferenceText(prefs.fitPreference, "Regular"))}
        </div>
      </div>

      <div class="shopper-profile-card green">
        <div class="shopper-profile-label">Budget</div>
        <div class="shopper-profile-value">${budgetText}</div>
        <div class="shopper-profile-sub">
          Mirror will highlight pieces that stay inside this range.
        </div>
      </div>

      <div class="shopper-profile-card purple">
        <div class="shopper-profile-label">Style Direction</div>
        <div class="shopper-profile-value">
          ${escapeHtml(normalizePreferenceText(prefs.occasionPriority, "Everyday"))}
        </div>
        <div class="shopper-profile-sub">
          ${escapeHtml(normalizePreferenceText(prefs.genderStyle, "Any"))} • ${escapeHtml(normalizePreferenceText(prefs.fitPreference, "Regular"))}
        </div>
      </div>

      <div class="shopper-profile-card gold">
        <div class="shopper-profile-label">Color Intelligence</div>
        <div class="shopper-profile-value">Favorite Colors</div>
        ${renderPreferenceChips(prefs.favoriteColors, "No favorite colors saved")}
      </div>
    </div>

    <div class="shopper-profile-grid">
      <div class="shopper-profile-card">
        <div class="shopper-profile-label">Preferred Materials</div>
        <div class="shopper-profile-value">Texture Profile</div>
        ${renderPreferenceChips(prefs.preferredMaterials, "No preferred materials saved")}
      </div>

      <div class="shopper-profile-card">
        <div class="shopper-profile-label">Style Keywords</div>
        <div class="shopper-profile-value">Aesthetic Signals</div>
        ${renderPreferenceChips(prefs.styleKeywords, "No style keywords saved")}
      </div>

      <div class="shopper-profile-card">
        <div class="shopper-profile-label">Avoid</div>
        <div class="shopper-profile-value">Do Not Prioritize</div>
        ${renderPreferenceChips(
          [prefs.avoidedColors, prefs.dislikedMaterials, prefs.dislikedStyles].filter(Boolean).join(", "),
          "No avoid preferences saved"
        )}
      </div>

      <div class="shopper-profile-card green">
        <div class="shopper-profile-label">Mirror Personalization</div>
        <div class="shopper-profile-value">Preference-aware styling active</div>
        <div class="shopper-profile-sub">
          Saved profile data will be sent with scan and look requests when available.
        </div>
      </div>
    </div>

    ${
      prefs.notes
        ? `
          <div class="shopper-profile-note">
            <strong>Stylist Notes:</strong>
            ${escapeHtml(prefs.notes)}
          </div>
        `
        : ""
    }
  `;

  if (typeof updateMirrorConciergeForProfile === "function") {
    updateMirrorConciergeForProfile(prefs);
  }
}

  async function loadMirrorShopperProfile() {
    const container = document.getElementById("shopperProfileContent");
    const button = document.getElementById("refreshShopperProfileBtn");

    if (!container) return;

    if (!getToken()) {
      container.innerHTML = `
        <div class="empty">
          Login from the Merchant App to load shopper preferences inside Mirror Mode.
        </div>
      `;
      return;
    }

    try {
      if (button) {
        button.disabled = true;
        button.textContent = "Refreshing...";
      }

      container.innerHTML = `<div class="empty">Loading shopper intelligence...</div>`;
      speakPixelConcierge("loadingProfile");

      const preferences = await fetchMirrorPreferences();

      window.currentMirrorPreferences = preferences;
      renderShopperProfile(preferences);

      if (hasMeaningfulPreferences(preferences)) {
        speakPixelConcierge("profile");
      } else {
        speakPixelConcierge("profileEmpty");
      }

      addTryOnTimelineEvent(
        "profile",
        "Shopper profile refreshed",
        "Mirror personalization was updated from saved preferences.",
        [getMirrorStoreDisplayName()]
  );
    } catch (error) {
      console.error("Mirror shopper profile failed:", error);

      container.innerHTML = `
        <div class="empty">
          Unable to load shopper preferences right now.
        </div>
      `;

      showToast(error.message || "Unable to load shopper profile.", "error");
      speakPixelConcierge("error", {
        errorMessage: error.message || "Unable to load shopper profile."
      });
    } finally {
      if (button) {
        button.disabled = false;
        button.textContent = "Refresh Profile";
      }
    }
  }

  function buildMirrorPreferenceQueryParams() {
    const preferences = normalizeMirrorPreferences(window.currentMirrorPreferences || {});
    const params = new URLSearchParams();

    Object.entries(preferences).forEach(([key, value]) => {
      const cleanValue = String(value ?? "").trim();

      if (cleanValue) {
        params.set(key, cleanValue);
      }
    });

    return params;
  }

  const TRY_ON_SESSION_KEY = "pixelMirrorTryOnSession";

function getTryOnSessionId() {
  const storeCode = getSelectedStoreCode() || "no-store";
  const retailerKey = getSelectedRetailerKey() || "no-retailer";
  return `${retailerKey}:${storeCode}`;
}

function getDefaultTryOnSession() {
  return {
    sessionId: getTryOnSessionId(),
    startedAt: new Date().toISOString(),
    storeName: getMirrorStoreDisplayName(),
    retailerKey: getSelectedRetailerKey(),
    storeCode: getSelectedStoreCode(),
    scans: [],
    looksCreated: 0,
    savesToBag: 0,
    timeline: []
  };
}

function readTryOnSession() {
  const stored = safeParseJson(localStorage.getItem(TRY_ON_SESSION_KEY));
  const currentSessionId = getTryOnSessionId();

  if (!stored || stored.sessionId !== currentSessionId) {
    return getDefaultTryOnSession();
  }

  return {
    ...getDefaultTryOnSession(),
    ...stored,
    scans: Array.isArray(stored.scans) ? stored.scans : [],
    timeline: Array.isArray(stored.timeline) ? stored.timeline : [],
    looksCreated: Number(stored.looksCreated || 0),
    savesToBag: Number(stored.savesToBag || 0)
  };
}

function writeTryOnSession(session) {
  localStorage.setItem(TRY_ON_SESSION_KEY, JSON.stringify({
    ...session,
    sessionId: getTryOnSessionId(),
    storeName: getMirrorStoreDisplayName(),
    retailerKey: getSelectedRetailerKey(),
    storeCode: getSelectedStoreCode()
  }));
}

function getTimelineIcon(type) {
  const icons = {
    scan: "⌁",
    look: "◇",
    checkout: "$",
    inventory: "▤",
    save: "✓",
    remove: "−",
    profile: "◎",
    vibe: "◐",
    store: "⌂",
    command: "⌘",
    pixel: "✦",
    error: "!",
    session: "•"
  };

  return icons[type] || "•";
}

function addTryOnTimelineEvent(type, title, detail = "", meta = []) {
  const session = readTryOnSession();

  const cleanType = String(type || "session").trim() || "session";
  const cleanTitle = String(title || "Mirror event").trim() || "Mirror event";
  const cleanDetail = String(detail || "").trim();
  const cleanMeta = Array.isArray(meta)
    ? meta
        .map(item => String(item || "").trim())
        .filter(Boolean)
        .slice(0, 4)
    : [];

  const timeline = Array.isArray(session.timeline) ? session.timeline : [];
  const now = Date.now();

  const repeatedSystemTitles = new Set([
    "Shopper profile refreshed",
    "Ambient idle mode started",
    "Shortcut help opened",
    "Shortcut help closed",
    "Command used: Help",
    "Command used: Shortcut Help",
    "Command used: Show Bag",
    "Command used: Refresh Profile",
    "Command used: Reset Mirror",
    "Command used: Cinematic Mode",
    "Pixel: Mirror help opened",
    "Pixel: Ambient idle mode",
    "Pixel: Mirror awakened",
    "Pixel: Session reset",
    "Pixel: Store lock updated",
    "Pixel: Cinematic mode active",
    "Pixel: Cinematic mode closed"
  ]);

  const highValueTypes = new Set([
    "scan",
    "look",
    "save",
    "remove",
    "profile",
    "store",
    "vibe",
    "error"
  ]);

  const isImportantEvent =
    highValueTypes.has(cleanType) ||
    cleanTitle.toLowerCase().includes("failed") ||
    cleanTitle.toLowerCase().includes("error") ||
    cleanTitle.toLowerCase().includes("removed") ||
    cleanTitle.toLowerCase().includes("saved") ||
    cleanTitle.toLowerCase().includes("scanned") ||
    cleanTitle.toLowerCase().includes("created full look");

  const recentDuplicate = timeline.find(event => {
    if (!event) return false;

    const eventTime = new Date(event.timestamp).getTime();

    if (Number.isNaN(eventTime)) return false;

    const sameType = event.type === cleanType;
    const sameTitle = event.title === cleanTitle;
    const sameDetail = event.detail === cleanDetail;
    const secondsSinceEvent = (now - eventTime) / 1000;

    if (sameType && sameTitle && sameDetail && secondsSinceEvent < 30) {
      return true;
    }

    if (
      repeatedSystemTitles.has(cleanTitle) &&
      event.title === cleanTitle &&
      secondsSinceEvent < 300
    ) {
      return true;
    }

    if (
      !isImportantEvent &&
      sameType &&
      sameTitle &&
      secondsSinceEvent < 20
    ) {
      return true;
    }

    return false;
  });

  if (recentDuplicate) {
    return;
  }

  const event = {
    id: crypto.randomUUID(),
    type: cleanType,
    title: cleanTitle,
    detail: cleanDetail,
    meta: cleanMeta,
    timestamp: new Date().toISOString()
  };

  session.timeline = [event, ...timeline].slice(0, 14);

  writeTryOnSession(session);
  renderTryOnMemory();
  renderTryOnTimeline();
}

function renderTryOnTimeline() {
  const container = document.getElementById("tryOnTimelineContent");
  if (!container) return;

  const session = readTryOnSession();
  const timeline = Array.isArray(session.timeline) ? session.timeline : [];

  if (!timeline.length) {
    container.innerHTML = `
      <div class="empty">
        Timeline is ready. Scan an item, create a look, or save to bag to begin.
      </div>
    `;
    return;
  }

  container.innerHTML = timeline.slice(0, 10).map(event => {
    const type = escapeHtml(event.type || "session");
    const icon = getTimelineIcon(event.type);
    const meta = Array.isArray(event.meta) ? event.meta : [];

    return `
      <div class="tryon-timeline-event ${type}">
        <div class="tryon-timeline-icon">${escapeHtml(icon)}</div>

        <div>
          <div class="tryon-timeline-title">${escapeHtml(event.title)}</div>

          ${
            event.detail
              ? `<div class="tryon-timeline-detail">${escapeHtml(event.detail)}</div>`
              : ""
          }

          ${
            meta.length
              ? `
                <div class="tryon-timeline-meta">
                  ${meta.map(item => `
                    <span class="tryon-timeline-pill">${escapeHtml(item)}</span>
                  `).join("")}
                </div>
              `
              : ""
          }
        </div>

        <div class="tryon-timeline-time">
          ${escapeHtml(formatSessionTime(event.timestamp))}
        </div>
      </div>
    `;
  }).join("");
}

function clearMirrorTimelineOnly() {
  const session = readTryOnSession();

  session.timeline = [];

  writeTryOnSession(session);
  renderTryOnMemory();
  renderTryOnTimeline();

  showToast("Mirror timeline cleared.", "success");
  speakPixelConcierge("reset");

  return session;
}

function formatSessionTime(isoValue) {
  if (!isoValue) return "Just now";

  const date = new Date(isoValue);

  if (Number.isNaN(date.getTime())) {
    return "Just now";
  }

  return date.toLocaleTimeString([], {
    hour: "numeric",
    minute: "2-digit"
  });
}

function getSessionDurationText(startedAt) {
  const start = new Date(startedAt);
  const now = new Date();

  if (Number.isNaN(start.getTime())) {
    return "Active now";
  }

  const diffMs = Math.max(0, now.getTime() - start.getTime());
  const minutes = Math.floor(diffMs / 60000);

  if (minutes < 1) {
    return "Started moments ago";
  }

  if (minutes === 1) {
    return "Active for 1 minute";
  }

  if (minutes < 60) {
    return `Active for ${minutes} minutes`;
  }

  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;

  return `${hours}h ${remainingMinutes}m active`;
}

function buildTryOnScanRecord(item, vibe) {
  const rfid = getItemField(item, "rfid", "itemRfid", "productRfid", "id") || currentRfid || "";
  const name = getItemField(item, "name", "itemName") || "Scanned Item";
  const brand = getItemField(item, "brand") || "Brand";
  const category = getItemField(item, "category") || "Category";
  const color = getItemField(item, "color") || "";
  const price = getItemField(item, "price");
  const imageUrl = getItemField(item, "imageUrl", "image_url", "image", "photoUrl", "productImageUrl") || "";

  return {
    id: crypto.randomUUID(),
    timestamp: new Date().toISOString(),
    rfid,
    name,
    brand,
    category,
    color,
    price: safeNumber(price),
    vibe: vibe || "Casual",
    imageUrl
  };
}

function rememberTryOnScan(item, vibe) {
  if (!item) return;

  const session = readTryOnSession();
  const record = buildTryOnScanRecord(item, vibe);

  session.scans = [
    record,
    ...session.scans.filter(scan => scan.rfid !== record.rfid)
  ].slice(0, 12);

  writeTryOnSession(session);
  renderTryOnMemory();

  addTryOnTimelineEvent(
    "scan",
    `Scanned ${record.name}`,
    `${record.brand} • ${record.category} • ${formatPrice(record.price)}`,
    [
      record.vibe,
      record.color,
      record.rfid ? `RFID ${record.rfid}` : ""
    ]
  );
}

function incrementTryOnLooksCreated() {
  const session = readTryOnSession();
  session.looksCreated += 1;

  writeTryOnSession(session);
  renderTryOnMemory();

  const itemName =
    getItemField(lastScannedItem, "name", "itemName") ||
    "scanned item";

  addTryOnTimelineEvent(
    "look",
    "Created full look",
    `Generated a complete outfit around ${itemName}.`,
    [
      document.getElementById("vibeSelect")?.value || "Casual",
      getMirrorStoreDisplayName()
    ]
  );
}

function incrementTryOnSavesToBag() {
  const session = readTryOnSession();
  session.savesToBag += 1;

  writeTryOnSession(session);
  renderTryOnMemory();

  const itemName =
    getItemField(currentLoadedItem, "name", "itemName") ||
    "item";

  addTryOnTimelineEvent(
    "save",
    "Saved item to bag",
    `${itemName} was saved to the shopper’s style bag.`,
    [
      getMirrorStoreDisplayName(),
      currentRfid ? `RFID ${currentRfid}` : ""
    ]
  );
}

function clearTryOnMemory() {
  localStorage.removeItem(TRY_ON_SESSION_KEY);
  writeTryOnSession(getDefaultTryOnSession());
  renderTryOnMemory();

  addTryOnTimelineEvent(
    "session",
    "Session memory cleared",
    "A fresh mirror journey has started.",
    [getMirrorStoreDisplayName()]
  );

  showToast("Mirror session memory cleared.", "success");
}

function renderTryOnMemory() {
  const container = document.getElementById("tryOnMemoryContent");
  if (!container) return;

  const session = readTryOnSession();
  const scans = Array.isArray(session.scans) ? session.scans : [];
  const mostRecentScans = scans.slice(0, 5);

  const scanListHtml = mostRecentScans.length
    ? `
      <div class="tryon-memory-list">
        ${mostRecentScans.map(scan => {
          const imageUrl = safeImageUrl(scan.imageUrl, "https://placehold.co/120x120?text=Scan");

          return `
            <div class="tryon-memory-item">
              <img
                src="${imageUrl}"
                alt="${escapeHtml(scan.name)}"
                onerror="this.src='https://placehold.co/120x120?text=Scan';"
              />

              <div>
                <div class="tryon-memory-name">${escapeHtml(scan.name)}</div>
                <div class="tryon-memory-meta">
                  ${escapeHtml(scan.brand)} • ${escapeHtml(scan.category)} • ${formatPrice(scan.price)}
                </div>
              </div>

              <div class="tryon-memory-time">${escapeHtml(formatSessionTime(scan.timestamp))}</div>
            </div>
          `;
        }).join("")}
      </div>
    `
    : `<div class="empty">No scans in this mirror session yet.</div>`;

  container.innerHTML = `
    <div class="tryon-memory-grid">
      <div class="tryon-memory-card">
        <div class="tryon-memory-label">Store Session</div>
        <div class="tryon-memory-value">${escapeHtml(session.storeName || "Store")}</div>
        <div class="tryon-memory-sub">${escapeHtml(session.storeCode || "No store code")}</div>
      </div>

      <div class="tryon-memory-card green">
        <div class="tryon-memory-label">Scans</div>
        <div class="tryon-memory-value">${scans.length}</div>
        <div class="tryon-memory-sub">Items explored this session</div>
      </div>

      <div class="tryon-memory-card purple">
        <div class="tryon-memory-label">Looks</div>
        <div class="tryon-memory-value">${safeNumber(session.looksCreated)}</div>
        <div class="tryon-memory-sub">Complete outfits generated</div>
      </div>

      <div class="tryon-memory-card gold">
        <div class="tryon-memory-label">Bag Saves</div>
        <div class="tryon-memory-value">${safeNumber(session.savesToBag)}</div>
        <div class="tryon-memory-sub">${escapeHtml(getSessionDurationText(session.startedAt))}</div>
      </div>
    </div>

    ${scanListHtml}
  `;
}

 function logMirrorCommand(commandName, detail = "") {
   if (typeof addTryOnTimelineEvent !== "function") return;

   const cleanCommandName = String(commandName || "Mirror Command").trim();
   const cleanDetail = String(detail || "Mirror command dock action triggered.").trim();

   addTryOnTimelineEvent(
     "command",
     `Command used: ${cleanCommandName}`,
     cleanDetail,
     [getMirrorStoreDisplayName()]
   );
 }

function focusRfidInput() {
  const input = document.getElementById("rfidInput");
  if (!input) return;

  input.focus();
  input.select();

  const scanPanel = document.querySelector(".scan-panel");
  window.requestAnimationFrame(() => {
    scrollToPanel(scanPanel, { gap: 72 });
  });
}

function runMirrorCommand(command) {
  const normalizedCommand = String(command || "").trim().toLowerCase();

  if (!normalizedCommand) return;

  handleAmbientWakeInteraction();

  if (normalizedCommand === "scan") {
    const rfid = document.getElementById("rfidInput")?.value.trim();

    logMirrorCommand(
      "Scan Item",
      rfid
        ? "Started scan from the command dock."
        : "Focused the RFID scan input."
    );

    if (rfid) {
      handleScan();
      return;
    }

    focusRfidInput();
    setStatus("RFID input focused. Scan or enter a tag.", "ready");
    showToast("RFID input focused.", "info");
    speakPixelConcierge("scanBlocked");
    return;
  }

  if (normalizedCommand === "create-look") {
    logMirrorCommand(
      "Create Look",
      "Requested a full outfit from the current scanned item."
    );

    createFullLook();
    return;
  }

  if (normalizedCommand === "save-bag") {
    logMirrorCommand(
      "Save to Bag",
      "Requested save for the current scanned item."
    );

    saveToBag();
    return;
  }

  if (normalizedCommand === "show-bag") {
    logMirrorCommand(
      "Show Bag",
      "Opened the shopper style bag."
    );

    loadBag(true);
    return;
  }

  if (normalizedCommand === "refresh-profile") {
    logMirrorCommand(
      "Refresh Profile",
      "Requested shopper preference refresh."
    );

    loadMirrorShopperProfile();
    return;
  }

  if (normalizedCommand === "reset") {
    logMirrorCommand(
      "Reset Mirror",
      "Started a fresh scan state."
    );

    resetMirror();
    return;
  }

  if (normalizedCommand === "cinematic") {
    logMirrorCommand(
      "Cinematic Mode",
      isCinematicModeActive()
        ? "Exited customer-facing mirror presentation mode."
        : "Entered customer-facing mirror presentation mode."
    );

    toggleCinematicMode();
    return;
  }

  if (normalizedCommand === "help") {
    logMirrorCommand(
      "Help",
      "Opened the mirror shortcut help panel."
    );

    openShortcutHelp();
    return;
  }

  speakPixelConcierge("error", {
    errorMessage: `Unknown mirror command: ${normalizedCommand}`
  });

  showToast(`Unknown mirror command: ${normalizedCommand}`, "error");

  if (typeof addTryOnTimelineEvent === "function") {
    addTryOnTimelineEvent(
      "error",
      "Unknown mirror command",
      `Pixel could not route command: ${normalizedCommand}`,
      [getMirrorStoreDisplayName()]
    );
  }
}

function bindMirrorCommandDock() {
  document.querySelectorAll("[data-command]").forEach(button => {
    button.addEventListener("click", () => {
      runMirrorCommand(button.dataset.command || "");
    });
  });
}

  function isTypingInEditableField(event) {
  const target = event.target;

  if (!target) return false;

  const tagName = String(target.tagName || "").toLowerCase();

  return (
    tagName === "input" ||
    tagName === "textarea" ||
    tagName === "select" ||
    target.isContentEditable
  );
}

function openShortcutHelp() {
  const overlay = document.getElementById("shortcutHelpOverlay");
  if (!overlay) return;

  const alreadyOpen = overlay.classList.contains("show");

  overlay.classList.add("show");
  overlay.setAttribute("aria-hidden", "false");

  speakPixelConcierge("help");

  if (!alreadyOpen) {
    addTryOnTimelineEvent(
      "command",
      "Shortcut help opened",
      "Pixel opened the mirror shortcuts panel.",
      [getMirrorStoreDisplayName()]
    );
  }
}

function closeShortcutHelp() {
  const overlay = document.getElementById("shortcutHelpOverlay");
  if (!overlay) return;

  const wasOpen = overlay.classList.contains("show");

  overlay.classList.remove("show");
  overlay.setAttribute("aria-hidden", "true");

  if (wasOpen) {
    speakPixelConcierge("command", {
      commandName: "Close Help"
    });

    addTryOnTimelineEvent(
      "command",
      "Shortcut help closed",
      "Returned from the mirror shortcuts panel to the main mirror experience.",
      [getMirrorStoreDisplayName()]
    );
  }
}

function toggleShortcutHelp() {
  const overlay = document.getElementById("shortcutHelpOverlay");
  if (!overlay) return;

  if (overlay.classList.contains("show")) {
    closeShortcutHelp();
  } else {
    openShortcutHelp();
  }
}

function getMirrorShortcutCommand(key) {
  const normalizedKey = String(key || "").toLowerCase();

  const shortcutMap = {
    s: "scan",
    l: "create-look",
    g: "save-bag",
    b: "show-bag",
    p: "refresh-profile",
    r: "reset",
    c: "cinematic",
  };

  return shortcutMap[normalizedKey] || "";
}

function handleMirrorKeyboardShortcut(event) {
  const key = event.key;
  const shortcutOverlay = document.getElementById("shortcutHelpOverlay");
  const shortcutHelpOpen = shortcutOverlay?.classList.contains("show");

  if (key === "Escape") {
    event.preventDefault();

    if (shortcutHelpOpen) {
      closeShortcutHelp();
      return;
    }

    if (isCinematicModeActive()) {
      exitCinematicMode();
      return;
    }

    return;
  }

  if (key === "?" || (key === "/" && event.shiftKey)) {
    event.preventDefault();
    toggleShortcutHelp();
    return;
  }

  if (isTypingInEditableField(event)) {
    return;
  }

  const command = getMirrorShortcutCommand(key);

  if (!command) {
    return;
  }

  event.preventDefault();
  runMirrorCommand(command);
}

function bindMirrorKeyboardShortcuts() {
  document.addEventListener("keydown", handleMirrorKeyboardShortcut);

  document.getElementById("closeShortcutHelpBtn")?.addEventListener("click", closeShortcutHelp);

  document.getElementById("shortcutHelpOverlay")?.addEventListener("click", event => {
    if (event.target?.id === "shortcutHelpOverlay") {
      closeShortcutHelp();
    }
  });
}

  function setVoiceCommandResult(message, type = "ready") {
  const result = document.getElementById("voiceCommandResult");
  if (!result) return;

  result.className =
    type === "success"
      ? "voice-command-result success"
      : type === "error"
        ? "voice-command-result error"
        : "voice-command-result";

  result.textContent = message;
}

function parseMirrorVoiceCommand(phrase) {
  const text = String(phrase || "")
    .toLowerCase()
    .replace(/[^\w\s]/g, " ")
    .replace(/\s+/g, " ")
    .trim();

  if (!text) {
    return {
      command: "",
      label: "",
      confidence: 0
    };
  }

  const commandPatterns = [
  {
    command: "scan",
    label: "Scan Item",
    confidence: 96,
    patterns: ["scan", "scan this", "scan item", "read tag", "read rfid", "identify item"]
  },
  {
    command: "create-look",
    label: "Create Look",
    confidence: 94,
    patterns: ["build outfit", "create look", "make look", "style me", "complete outfit", "generate outfit", "build my outfit"]
  },
  {
    command: "save-bag",
    label: "Save to Bag",
    confidence: 94,
    patterns: ["save", "save this", "save item", "add to bag", "put in bag", "keep this"]
  },
  {
    command: "show-bag",
    label: "Show Bag",
    confidence: 94,
    patterns: ["open bag", "show bag", "view bag", "my bag", "show my bag"]
  },
  {
    command: "refresh-profile",
    label: "Refresh Profile",
    confidence: 92,
    patterns: ["refresh profile", "sync profile", "load profile", "update preferences", "refresh preferences"]
  },
  {
    command: "reset",
    label: "Reset Mirror",
    confidence: 90,
    patterns: ["reset", "start over", "clear screen", "new scan", "restart mirror"]
  },
  {
    command: "cinematic",
    label: "Cinematic Mode",
    confidence: 90,
    patterns: ["cinematic", "cinematic mode", "full screen", "fullscreen", "presentation mode", "mirror display"]
  },
  {
    command: "help",
    label: "Show Help",
    confidence: 92,
    patterns: ["help", "show help", "commands", "shortcuts", "what can i say"]
  }
];

  for (const entry of commandPatterns) {
    if (entry.patterns.some(pattern => text === pattern || text.includes(pattern))) {
      return {
        command: entry.command,
        label: entry.label,
        confidence: entry.confidence
      };
    }
  }

  return {
    command: "",
    label: "",
    confidence: 0
  };
}

function runParsedVoiceCommand(rawPhrase) {
  const parsed = parseMirrorVoiceCommand(rawPhrase);
  const cleanPhrase = String(rawPhrase || "").trim();

  if (!parsed.command) {
    const message =
      "Command not recognized. Try: scan this, build outfit, save this, open bag, refresh profile, or start over.";

    setVoiceCommandResult(message, "error");
    showToast("Voice command not recognized.", "error");

    speakPixelConcierge("error", {
      errorMessage: cleanPhrase
        ? `I heard "${cleanPhrase}", but I could not match it to a mirror command.`
        : "No voice command text was entered."
    });

    if (typeof addTryOnTimelineEvent === "function") {
      addTryOnTimelineEvent(
        "session",
        "Voice command not recognized",
        cleanPhrase || "Empty voice command input.",
        [getMirrorStoreDisplayName()]
      );
    }

    return;
  }

  if (parsed.command === "help") {
    setVoiceCommandResult("Opening shortcut help.", "success");
    openShortcutHelp();

    addTryOnTimelineEvent(
      "session",
      "Voice recognized: Show Help",
      `Parsed phrase: "${cleanPhrase}"`,
      [getMirrorStoreDisplayName()]
    );

    return;
  }

  setVoiceCommandResult(
    `Recognized: ${parsed.label} (${parsed.confidence}% confidence).`,
    "success"
  );

  runMirrorCommand(parsed.command);

  addTryOnTimelineEvent(
    "session",
    `Voice recognized: ${parsed.label}`,
    `Parsed phrase: "${cleanPhrase}"`,
    [getMirrorStoreDisplayName()]
  );
}

function bindVoiceCommandParser() {
  const input = document.getElementById("voiceCommandInput");
  const button = document.getElementById("runVoiceCommandBtn");

  button?.addEventListener("click", () => {
    runParsedVoiceCommand(input?.value || "");
  });

  input?.addEventListener("keydown", event => {
    if (event.key === "Enter") {
      event.preventDefault();
      runParsedVoiceCommand(input.value || "");
    }
  });
}

  function isCinematicModeActive() {
    return document.body.classList.contains("cinematic-mode");
  }

  function updateCinematicExitButton(isActive) {
    const button = document.getElementById("exitCinematicBtn");
    if (!button) return;

    button.hidden = !isActive;
    button.setAttribute("aria-hidden", String(!isActive));
  }

  function enterCinematicMode() {
    handleAmbientWakeInteraction();

    if (isCinematicModeActive()) {
      speakPixelConcierge("cinematic");
      return;
    }

    document.body.classList.add("cinematic-mode");
    document.body.classList.remove("ambient-idle-active");

    updateCinematicExitButton(true);

    let fullscreenRequested = false;

    try {
      if (!document.fullscreenElement && document.documentElement.requestFullscreen) {
        fullscreenRequested = true;

        document.documentElement.requestFullscreen().catch(() => {
          showToast(
            "Fullscreen was blocked by the browser, but cinematic mode is still active.",
            "info"
          );
        });
      }
    } catch (_) {
      fullscreenRequested = false;
    }

    setStatus("Cinematic mirror mode enabled.", "success");
    showToast("Cinematic mode enabled.", "success");
    speakPixelConcierge("cinematic");

    addTryOnTimelineEvent(
      "session",
      "Entered Cinematic Mode",
      fullscreenRequested
        ? "Mirror switched into full-screen customer presentation mode."
        : "Mirror switched into customer presentation mode. Fullscreen was not available.",
      [getMirrorStoreDisplayName()]
    );

    const hero = document.querySelector(".hero");

    if (hero) {
      window.requestAnimationFrame(() => {
        scrollToPanel(hero, {
          gap: 24,
          behavior: "smooth"
        });
      });
    }
  }

  function exitCinematicMode() {
    if (!isCinematicModeActive() && !document.fullscreenElement) {
      return;
    }

    document.body.classList.remove("cinematic-mode");
    updateCinematicExitButton(false);

    try {
      if (document.fullscreenElement && document.exitFullscreen) {
        document.exitFullscreen().catch(() => {});
      }
    } catch (_) {}

    setStatus("Cinematic mirror mode exited.", "ready");
    showToast("Cinematic mode exited.", "info");
    speakPixelConcierge("cinematicExit");

    addTryOnTimelineEvent(
      "session",
      "Exited Cinematic Mode",
      "Returned to associate control mode.",
      [getMirrorStoreDisplayName()]
    );

    resetAmbientIdleTimer();
  }

  function toggleCinematicMode() {
    if (isCinematicModeActive()) {
      exitCinematicMode();
    } else {
      enterCinematicMode();
    }
  }

  function syncCinematicModeFromFullscreen() {
    if (!document.fullscreenElement && isCinematicModeActive()) {
      document.body.classList.remove("cinematic-mode");
      updateCinematicExitButton(false);
      speakPixelConcierge("cinematicExit");
      resetAmbientIdleTimer();
    }
  }

function isAmbientIdleActive() {
  return document.body.classList.contains("ambient-idle-active");
}

function shouldAllowAmbientIdle() {
  const resultVisible = document.getElementById("resultPanel")?.classList.contains("show");
  const outfitVisible = document.getElementById("outfitPanel")?.classList.contains("show");
  const bagVisible = document.getElementById("bagPanel")?.classList.contains("show");
  const shortcutOpen = document.getElementById("shortcutHelpOverlay")?.classList.contains("show");
  const cinematicActive = isCinematicModeActive();

  return !resultVisible && !outfitVisible && !bagVisible && !shortcutOpen && !cinematicActive;
}

function updateAmbientIdleCopy() {
  const storeName = document.getElementById("ambientStoreName");
  if (!storeName) return;

  storeName.textContent = `${getMirrorStoreDisplayName()} Smart Mirror`;
}

function getLastTimelineEvent() {
  if (typeof readTryOnSession !== "function") {
    return null;
  }

  const session = readTryOnSession();
  const timeline = Array.isArray(session?.timeline) ? session.timeline : [];

  return timeline.length ? timeline[0] : null;
}

function logAmbientIdleStartedOnce() {
  if (typeof addTryOnTimelineEvent !== "function") {
    return;
  }

  const lastEvent = getLastTimelineEvent();

  const alreadyLoggedAmbientIdle =
    lastEvent &&
    lastEvent.title === "Ambient idle mode started" &&
    lastEvent.detail === "Mirror entered branded idle display after inactivity.";

  if (alreadyLoggedAmbientIdle) {
    return;
  }

  addTryOnTimelineEvent(
    "session",
    "Ambient idle mode started",
    "Mirror entered branded idle display after inactivity.",
    [getMirrorStoreDisplayName()]
  );
}

function showAmbientIdleMode() {
  if (isAmbientIdleActive()) {
    return;
  }

  if (!shouldAllowAmbientIdle()) {
    resetAmbientIdleTimer();
    return;
  }

  const overlay = document.getElementById("ambientIdleOverlay");
  if (!overlay) return;

  updateAmbientIdleCopy();

  overlay.classList.add("show");
  overlay.setAttribute("aria-hidden", "false");
  document.body.classList.add("ambient-idle-active");

  logAmbientIdleStartedOnce();
  speakPixelConcierge("idle");
}

function hideAmbientIdleMode() {
  if (!isAmbientIdleActive()) {
    return;
  }

  const overlay = document.getElementById("ambientIdleOverlay");
  if (!overlay) return;

  overlay.classList.remove("show");
  overlay.setAttribute("aria-hidden", "true");
  document.body.classList.remove("ambient-idle-active");
}

function getAmbientIdleDelayMs() {
  const override = Number(localStorage.getItem("pixelMirrorIdleDelayMs"));

  if (Number.isFinite(override) && override >= 3000) {
    return override;
  }

  return AMBIENT_IDLE_DELAY_MS;
}

function resetAmbientIdleTimer() {
  window.clearTimeout(ambientIdleTimer);
  ambientIdleTimer = null;

  hideAmbientIdleMode();

  ambientIdleTimer = window.setTimeout(() => {
    if (!isAmbientIdleActive()) {
      showAmbientIdleMode();
    }
  }, getAmbientIdleDelayMs());
}

function handleAmbientWakeInteraction() {
  const wasIdle = isAmbientIdleActive();

  if (wasIdle) {
    hideAmbientIdleMode();
    showToast("Mirror awakened.", "info");
    speakPixelConcierge("wake");
  }

  resetAmbientIdleTimer();
}

function bindAmbientIdleMode() {
  const wakeEvents = ["pointerdown", "keydown", "touchstart"];

  wakeEvents.forEach(eventName => {
    document.addEventListener(eventName, handleAmbientWakeInteraction, {
      passive: true
    });
  });

  document.addEventListener(
    "mousemove",
    () => {
      resetAmbientIdleTimer();
    },
    {
      passive: true
    }
  );

  document.getElementById("ambientIdleOverlay")?.addEventListener("click", () => {
    handleAmbientWakeInteraction();
    focusRfidInput();
  });

  resetAmbientIdleTimer();
}

   function pulseScanPanelSuccess() {
  const panel = document.querySelector(".scan-panel");
  if (!panel) return;

  panel.classList.remove("scan-success-glow");
  void panel.offsetWidth;
  panel.classList.add("scan-success-glow");

  window.setTimeout(() => {
    panel.classList.remove("scan-success-glow");
  }, 950);
}

function setMirrorConciergeMessage(message) {
  const messageEl = document.getElementById("mirrorConciergeMessage");
  const panel = document.getElementById("mirrorConciergePanel");

  if (!messageEl) return;

  messageEl.textContent = message;

  if (panel) {
    panel.classList.remove("pixel-message-updated");
    void panel.offsetWidth;
    panel.classList.add("pixel-message-updated");
  }
}

function getMirrorConciergeWelcomeMessage() {
  const storeName = getMirrorStoreDisplayName();
  const vibeSelect = document.getElementById("vibeSelect");
  const vibe = vibeSelect?.value || "Everyday";

  return `Welcome to ${storeName}. Scan any store item and Pixel will build a ${vibe.toLowerCase()} look using this store’s live inventory.`;
}

function updateMirrorConciergeForProfile(preferences = null) {
  const storeName = getMirrorStoreDisplayName();

  if (!preferences || !hasMeaningfulPreferences(preferences)) {
    setMirrorConciergeMessage(
      `Pixel is ready for ${storeName}. Add shopper preferences to personalize styling, sizing, colors, budget, and avoid signals.`
    );
    return;
  }

  const style =
    preferences.styleDirection ||
    preferences.occasionPriority ||
    preferences.style ||
    "everyday";

  const colors = splitPreferenceList(preferences.favoriteColors).slice(0, 2);
  const colorCopy = colors.length
    ? ` Favorite colors like ${colors.join(" and ")} will be prioritized.`
    : "";

  setMirrorConciergeMessage(
    `Shopper profile is active. Pixel will style toward ${style} preferences and use ${storeName} inventory only.${colorCopy}`
  );
}

function updateMirrorConciergeForScan(item) {
  if (!item) {
    setMirrorConciergeMessage(getMirrorConciergeWelcomeMessage());
    return;
  }

  const itemName =
    getItemField(item, "name", "itemName", "productName", "title") ||
    "this item";

  const category =
    getItemField(item, "category", "productType", "type") ||
    "piece";

  const storeName = getMirrorStoreDisplayName();

  setMirrorConciergeMessage(
    `${itemName} is now on the mirror. Pixel can complete this ${category.toLowerCase()} into a full look using available pieces from ${storeName}.`
  );
}

function updateMirrorConciergeForOutfit(outfit) {
  const items = getOutfitItems(outfit);

  if (!items.length) {
    setMirrorConciergeMessage(
      "Pixel can create a complete outfit once a store item is scanned or selected."
    );
    return;
  }

  const missingCategories = typeof getOutfitMissingCategories === "function"
    ? getOutfitMissingCategories(outfit)
    : [];

  if (missingCategories.length) {
    setMirrorConciergeMessage(
      `This look has ${items.length} piece${items.length === 1 ? "" : "s"} and could be improved with ${missingCategories.slice(0, 2).join(" and ")}.`
    );
    return;
  }

  const totalPrice = typeof getOutfitTotalPrice === "function"
    ? getOutfitTotalPrice(outfit)
    : 0;

  const priceCopy = totalPrice ? ` Total look value is $${totalPrice.toFixed(2)}.` : "";

  setMirrorConciergeMessage(
    `This complete look is ready to save or send to the shopper bag.${priceCopy}`
  );
}

 function getPixelConciergeStoreName() {
   return typeof getMirrorStoreDisplayName === "function"
     ? getMirrorStoreDisplayName()
     : "this store";
 }

  function getPixelConciergeVibe() {
    const vibeSelect = document.getElementById("vibeSelect");
    return vibeSelect?.value || "Everyday";
  }

 function speakPixelConcierge(action, data = {}) {
   const storeName = getPixelConciergeStoreName();
   const vibe = getPixelConciergeVibe().toLowerCase();

   const itemName = String(data.itemName || "This item").trim();
   const totalPrice = Number(data.totalPrice || 0);
   const score = Number(data.score || 0);
   const bagCount = Number(data.bagCount || 0);
   const commandName = String(data.commandName || "").trim();
   const errorMessage = String(data.errorMessage || "").trim();

   const readableCommandName = commandName
     ? commandName.replace(/-/g, " ")
     : "";

   const messages = {
     ready:
       `Pixel is live for ${storeName}. Scan a product and I’ll build a ${vibe} look from this store’s inventory.`,

     thinking:
       `Pixel is analyzing the scanned item, shopper preferences, ${vibe} styling direction, and available inventory from ${storeName}.`,

     creatingLook:
       `Pixel is building a complete ${vibe} outfit from ${storeName}. I’m checking color harmony, price, category balance, and availability.`,

     saving:
       `${itemName} is being saved to the shopper bag. Pixel is updating this mirror session now.`,

     loadingBag:
       `Pixel is opening the shopper bag and checking saved pieces from this mirror session.`,

     loadingProfile:
       `Pixel is refreshing shopper intelligence so future scans can use sizing, colors, budget, materials, and avoid signals.`,

     command:
       readableCommandName
         ? `Command received: ${readableCommandName}. Pixel is routing the mirror action now.`
         : `Command received. Pixel is routing the mirror action now.`,

     help:
       `Mirror help is open. Use shortcuts, voice-style commands, or the command cards to scan, build looks, save items, open the bag, refresh profile, reset, or enter cinematic mode.`,

     scan:
       `${itemName} is on the mirror. I’m checking color, category, shopper preferences, and matching pieces from ${storeName}.`,

     scanBlocked:
       `Scan input is ready. Enter or scan an RFID tag first, then Pixel can style it from ${storeName} inventory.`,

     scanError:
       errorMessage
         ? `Pixel could not complete the scan. ${errorMessage}`
         : `Pixel could not complete the scan. Check the RFID, store lock, or inventory connection.`,

     outfit:
       totalPrice
         ? `I built a complete ${vibe} outfit from ${storeName}. Total look value is ${formatPrice(totalPrice)}. Review the look, then save it to the shopper bag.`
         : `I built a complete ${vibe} outfit using available pieces from ${storeName}. Review the look, then save it to the shopper bag.`,

     outfitScore:
       score
         ? `This outfit scored ${score}%. Pixel checked color harmony, budget fit, profile match, availability, and styling balance.`
         : `Pixel analyzed this outfit for color harmony, budget fit, profile match, availability, and styling balance.`,

     outfitBlocked:
       `Scan an item first. Once Pixel has a store item on the mirror, I can build a complete ${vibe} outfit around it.`,

     outfitError:
       errorMessage
         ? `Pixel could not create the full look. ${errorMessage}`
         : `Pixel could not create the full look right now. Try scanning the item again or checking store inventory.`,

     save:
       bagCount
         ? `${itemName} has been saved. The shopper bag now has ${bagCount} piece${bagCount === 1 ? "" : "s"}.`
         : `${itemName} has been saved. I’ll remember this piece for the shopper’s session.`,

     saveBlocked:
       `Scan an item first, then Pixel can save it to the shopper bag.`,

     saveError:
       errorMessage
         ? `Pixel could not save this item. ${errorMessage}`
         : `Pixel could not save this item right now. Try again after checking the session.`,

     remove:
       bagCount
         ? `${itemName} was removed from the shopper bag. The bag now has ${bagCount} saved piece${bagCount === 1 ? "" : "s"}.`
         : `${itemName} was removed from the shopper bag.`,

     removeError:
       errorMessage
         ? `Pixel could not remove this bag item. ${errorMessage}`
         : `Pixel could not remove this bag item right now.`,

     bag:
       bagCount
         ? `The shopper bag is open with ${bagCount} saved piece${bagCount === 1 ? "" : "s"}.`
         : `The shopper bag is ready. Saved pieces will appear here for checkout or associate follow-up.`,

     profile:
       `Shopper intelligence is active. I’ll use style, size, colors, budget, and avoid signals to guide every recommendation.`,

     profileEmpty:
       `No detailed shopper profile is saved yet. Pixel can still style from ${storeName}, but preferences will make recommendations stronger.`,

     store:
       `Mirror is now locked to ${storeName}. Future scans will use this store’s live inventory only.`,

     cinematic:
       `Cinematic mirror mode is active. This view is optimized for customer-facing styling and showroom presentation.`,

     cinematicExit:
       `Cinematic mode is closed. Pixel is back in associate control mode.`,

     reset:
       `Session reset. Pixel is ready for a fresh scan at ${storeName}.`,

     idle:
       `Welcome to ${storeName}. Step up to the mirror, scan an item, and I’ll style a complete look in seconds.`,

     wake:
       `Pixel is awake. Scan an RFID tag or choose a command to continue styling.`,

     error:
       errorMessage
         ? `Pixel needs attention. ${errorMessage}`
         : `Pixel needs attention. Check the scan details, store inventory, or browser console if something did not load correctly.`
   };

   const message = messages[action] || messages.ready;

   console.log("Pixel Concierge:", {
     action,
     message,
     storeName,
     vibe,
     data
   });

   const timelineActionLabels = {
     scanError: "Scan issue",
     outfitError: "Outfit issue",
     saveError: "Save issue",
     remove: "Bag item removed",
     removeError: "Bag remove issue",
     cinematic: "Cinematic mode active",
     cinematicExit: "Cinematic mode closed",
     idle: "Ambient idle mode",
     wake: "Mirror awakened",
     reset: "Session reset",
     store: "Store lock updated",
     help: "Mirror help opened"
   };

   if (
     typeof addTryOnTimelineEvent === "function" &&
     action !== "ready" &&
     Object.prototype.hasOwnProperty.call(timelineActionLabels, action)
   ) {
     addTryOnTimelineEvent(
       action.includes("Error") || action === "error" ? "error" : "pixel",
       `Pixel: ${timelineActionLabels[action]}`,
       message,
       [storeName]
     );
   }

   setMirrorConciergeMessage(message);
 }

 function exposePixelMirrorDebugTools() {
   function evalSafeFunction(name) {
     try {
       return Function(`"use strict"; return typeof ${name} === "function" ? ${name} : null;`)();
     } catch (_) {
       return null;
     }
   }

   function getElementCheck(id) {
     const element = document.getElementById(id);

     return {
       id,
       found: !!element,
       text: element?.textContent?.trim().slice(0, 120) || "",
       visible: !!element && element.offsetParent !== null
     };
   }

   function getFunctionCheck(name) {
     return {
       name,
       found: typeof window[name] === "function" || typeof evalSafeFunction(name) === "function"
     };
   }
 function getMirrorDemoProductSvg(label, type = "sweater", options = {}) {
   const bg = options.bg || "#f7f3ec";
   const ink = options.ink || "#171411";
   const accent = options.accent || "#d8c7ad";
   const soft = options.soft || "#ffffff";

   const safeLabel = String(label || "Product")
     .replace(/&/g, "&amp;")
     .replace(/</g, "&lt;")
     .replace(/>/g, "&gt;");

   const productShapes = {
     sweater: `
       <path d="M170 104h160l40 74-58 28v206H188V206l-58-28 40-74Z" fill="${soft}" stroke="${ink}" stroke-opacity=".18" stroke-width="3"/>
       <path d="M205 105c10 34 29 52 45 52s35-18 45-52" fill="none" stroke="${accent}" stroke-width="9" stroke-linecap="round"/>
       <path d="M190 230h120" stroke="${accent}" stroke-opacity=".55" stroke-width="5" stroke-linecap="round"/>
       <path d="M206 278h88" stroke="${accent}" stroke-opacity=".35" stroke-width="4" stroke-linecap="round"/>
     `,

     trouser: `
       <path d="M190 96h120l22 322h-78l-12-190-20 190h-78l24-322Z" fill="${soft}" stroke="${ink}" stroke-opacity=".18" stroke-width="3"/>
       <path d="M190 138h120" stroke="${accent}" stroke-width="9" stroke-linecap="round"/>
       <path d="M250 116v295" stroke="${accent}" stroke-opacity=".35" stroke-width="5" stroke-linecap="round"/>
       <path d="M205 165h34" stroke="${ink}" stroke-opacity=".12" stroke-width="4" stroke-linecap="round"/>
     `,

     sneaker: `
       <path d="M126 276c54-4 88-28 119-66 24 40 60 62 122 66 22 2 36 18 40 42H105c1-24 8-39 21-42Z" fill="${soft}" stroke="${ink}" stroke-opacity=".18" stroke-width="3"/>
       <path d="M149 304h232" stroke="${accent}" stroke-width="10" stroke-linecap="round"/>
       <path d="M225 220l70 52" stroke="${accent}" stroke-width="6" stroke-linecap="round"/>
       <path d="M249 204l72 52" stroke="${accent}" stroke-opacity=".55" stroke-width="5" stroke-linecap="round"/>
       <circle cx="210" cy="245" r="6" fill="${accent}"/>
       <circle cx="236" cy="260" r="6" fill="${accent}"/>
     `,

     jacket: `
       <path d="M172 96h156l52 82-52 34v206H172V212l-52-34 52-82Z" fill="${soft}" stroke="${ink}" stroke-opacity=".18" stroke-width="3"/>
       <path d="M210 100l40 82 40-82" fill="none" stroke="${accent}" stroke-width="8" stroke-linecap="round" stroke-linejoin="round"/>
       <path d="M250 180v220" stroke="${accent}" stroke-opacity=".5" stroke-width="6" stroke-linecap="round"/>
       <path d="M190 238h38" stroke="${accent}" stroke-opacity=".55" stroke-width="5" stroke-linecap="round"/>
       <path d="M272 238h38" stroke="${accent}" stroke-opacity=".55" stroke-width="5" stroke-linecap="round"/>
     `
   };

   const shape = productShapes[type] || productShapes.sweater;

   const svg = `
     <svg xmlns="http://www.w3.org/2000/svg" width="700" height="900" viewBox="0 0 500 620">
       <defs>
         <radialGradient id="bg" cx="50%" cy="18%" r="76%">
           <stop offset="0%" stop-color="#ffffff"/>
           <stop offset="48%" stop-color="${bg}"/>
           <stop offset="100%" stop-color="#eee6da"/>
         </radialGradient>

         <filter id="shadow" x="-30%" y="-30%" width="160%" height="170%">
           <feDropShadow dx="0" dy="28" stdDeviation="22" flood-color="#171411" flood-opacity=".14"/>
         </filter>
       </defs>

       <rect width="500" height="620" rx="46" fill="url(#bg)"/>

       <g filter="url(#shadow)">
         ${shape}
       </g>

       <text x="250" y="520" text-anchor="middle" fill="${ink}" fill-opacity=".72"
         font-family="Inter, Arial, sans-serif" font-size="24" font-weight="800">
         ${safeLabel}
       </text>

       <text x="250" y="552" text-anchor="middle" fill="${ink}" fill-opacity=".38"
         font-family="Inter, Arial, sans-serif" font-size="13" font-weight="700" letter-spacing="6">
         UNIVERSAL STYLIST
       </text>
     </svg>
   `;

   return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
 }

 function getDemoScannedItem() {
   const storeName =
     typeof getMirrorShowroomDisplayStore === "function"
       ? getMirrorShowroomDisplayStore()
       : getMirrorStoreDisplayName();

   return {
     id: "DEMO-CASHMERE-001",
     rfid: "DEMO-CASHMERE-001",
     itemRfid: "DEMO-CASHMERE-001",
     productRfid: "DEMO-CASHMERE-001",
     name: "Cashmere Crewneck",
     itemName: "Cashmere Crewneck",
     productName: "Cashmere Crewneck",
     brand: storeName,
     category: "Sweater",
     color: "Ivory",
     price: 148,
     stock: 6,
     retailer: storeName,
     retailerName: storeName,
     imageUrl: getMirrorDemoProductSvg("Cashmere Crewneck", "sweater", {
       bg: "#f3eadc",
       accent: "#c9ad86",
       soft: "#fffaf3"
     }),
     matchScore: 94,
     stylingAdvice:
       `Pair this ivory cashmere crewneck with relaxed trousers, clean sneakers, and a lightweight jacket for a polished casual look from ${storeName}.`,
     whyItWorks:
       "The neutral color, soft texture, and casual silhouette make this piece easy to build around for an elevated everyday outfit."
   };
 }

 function getDemoFullOutfit() {
   const storeName =
     typeof getMirrorShowroomDisplayStore === "function"
       ? getMirrorShowroomDisplayStore()
       : getMirrorStoreDisplayName();

   return {
     overallScore: 87,
     explanation:
       `Pixel built this complete casual look around the scanned cashmere crewneck using store-locked ${storeName} inventory.`,

     top: getDemoScannedItem(),

     bottom: {
       id: "DEMO-TROUSER-001",
       rfid: "DEMO-TROUSER-001",
       itemRfid: "DEMO-TROUSER-001",
       name: "Relaxed Tailored Trouser",
       itemName: "Relaxed Tailored Trouser",
       brand: storeName,
       category: "Bottom",
       color: "Charcoal",
       price: 168,
       stock: 4,
       retailer: storeName,
       retailerName: storeName,
       imageUrl: getMirrorDemoProductSvg("Tailored Trouser", "trouser", {
         bg: "#e5e0d8",
         accent: "#9b8b7a",
         soft: "#f8f6f1"
       }),
       styleTags: "casual tailored neutral elevated"
     },

     shoes: {
       id: "DEMO-SNEAKER-001",
       rfid: "DEMO-SNEAKER-001",
       itemRfid: "DEMO-SNEAKER-001",
       name: "Minimal Leather Sneaker",
       itemName: "Minimal Leather Sneaker",
       brand: storeName,
       category: "Shoes",
       color: "White",
       price: 188,
       stock: 8,
       retailer: storeName,
       retailerName: storeName,
       imageUrl: getMirrorDemoProductSvg("Leather Sneaker", "sneaker", {
         bg: "#eef3f8",
         accent: "#9fb8d0",
         soft: "#ffffff"
       }),
       styleTags: "clean casual white sneaker"
     },

     outerwear: {
       id: "DEMO-BOMBER-001",
       rfid: "DEMO-BOMBER-001",
       itemRfid: "DEMO-BOMBER-001",
       name: "Lightweight Bomber Jacket",
       itemName: "Lightweight Bomber Jacket",
       brand: storeName,
       category: "Outerwear",
       color: "Navy",
       price: 248,
       stock: 3,
       retailer: storeName,
       retailerName: storeName,
       imageUrl: getMirrorDemoProductSvg("Bomber Jacket", "jacket", {
         bg: "#dce8f5",
         accent: "#8aa8c7",
         soft: "#f7fbff"
       }),
       styleTags: "casual navy outerwear layering"
     }
   };
 }

   function getMirrorHealthReport() {
     const requiredElements = [
       "retailerSelect",
       "storeCodeSelect",
       "vibeSelect",
       "rfidInput",
       "scanBtn",
       "scanStatus",
       "readyCard",
       "mirrorConciergeMessage",
       "resultPanel",
       "outfitPanel",
       "bagPanel",
       "shopperProfileContent",
       "tryOnMemoryContent",
       "tryOnTimelineContent",
       "ambientIdleOverlay"
     ];

     const elements = requiredElements.map(getElementCheck);
     const missingElements = elements.filter(item => !item.found);

     const requiredFunctions = [
       "handleScan",
       "createFullLook",
       "saveToBag",
       "loadBag",
       "resetMirror",
       "runMirrorCommand",
       "speakPixelConcierge",
       "resetAmbientIdleTimer",
       "showAmbientIdleMode",
       "hideAmbientIdleMode",
       "toggleCinematicMode",
       "enterCinematicMode",
       "exitCinematicMode"
     ];

     const functions = requiredFunctions.map(getFunctionCheck);
     const missingFunctions = functions.filter(item => !item.found);

     const storeName = getPixelConciergeStoreName();
     const retailerKey = getSelectedRetailerKey();
     const storeCode = getSelectedStoreCode();
     const vibe = getPixelConciergeVibe();

     return {
       healthy: missingElements.length === 0 && missingFunctions.length === 0,
       checkedAt: new Date().toISOString(),

       store: {
         storeName,
         retailerKey,
         storeCode,
         vibe,
         hasStoreContext: !!storeCode
       },

       auth: {
         hasToken: !!getToken(),
         statusText: document.getElementById("authStatus")?.textContent?.trim() || ""
       },

       pixel: {
         messageMounted: !!document.getElementById("mirrorConciergeMessage"),
         message: document.getElementById("mirrorConciergeMessage")?.textContent?.trim() || "",
         debugToolsReady: !!window.PixelMirrorDebug
       },

       session: {
         currentRfid,
         hasLoadedItem: !!currentLoadedItem,
         hasLastScannedItem: !!lastScannedItem,
         bagCount: savedRfids.size,
         ambientIdleActive: isAmbientIdleActive(),
         cinematicActive: isCinematicModeActive(),
         idleDelayMs:
           typeof getAmbientIdleDelayMs === "function"
             ? getAmbientIdleDelayMs()
             : AMBIENT_IDLE_DELAY_MS
       },

       dom: {
         totalChecked: elements.length,
         missingCount: missingElements.length,
         missing: missingElements.map(item => item.id),
         elements
       },

       functions: {
         totalChecked: functions.length,
         missingCount: missingFunctions.length,
         missing: missingFunctions.map(item => item.name),
         functions
       }
     };
   }

   function printMirrorHealthReport() {
     const report = getMirrorHealthReport();

     console.group(
       report.healthy
         ? "✅ Pixel Mirror Health Check Passed"
         : "⚠️ Pixel Mirror Health Check Needs Attention"
     );

     console.table({
       healthy: report.healthy,
       storeName: report.store.storeName,
       retailerKey: report.store.retailerKey,
       storeCode: report.store.storeCode,
       vibe: report.store.vibe,
       hasToken: report.auth.hasToken,
       missingDomElements: report.dom.missingCount,
       missingFunctions: report.functions.missingCount,
       ambientIdleActive: report.session.ambientIdleActive,
       cinematicActive: report.session.cinematicActive,
       bagCount: report.session.bagCount
     });

     if (report.dom.missing.length) {
       console.warn("Missing DOM elements:", report.dom.missing);
     }

     if (report.functions.missing.length) {
       console.warn("Missing functions:", report.functions.missing);
     }

     console.log("Full report:", report);
     console.groupEnd();

     return report;
   }

   window.PixelMirrorDebug = {
     speak: speakPixelConcierge,

     getMessage() {
       return document.getElementById("mirrorConciergeMessage")?.textContent || "";
     },

     status() {
       return {
         storeName: getPixelConciergeStoreName(),
         vibe: getPixelConciergeVibe(),
         retailerKey: getSelectedRetailerKey(),
         storeCode: getSelectedStoreCode(),
         currentRfid,
         hasLoadedItem: !!currentLoadedItem,
         hasLastScannedItem: !!lastScannedItem,
         bagCount: savedRfids.size,
         ambientIdleActive: isAmbientIdleActive(),
         cinematicActive: isCinematicModeActive(),
         idleDelayMs:
           typeof getAmbientIdleDelayMs === "function"
             ? getAmbientIdleDelayMs()
             : AMBIENT_IDLE_DELAY_MS,
         message: this.getMessage()
       };
     },

     health() {
       return printMirrorHealthReport();
     },

     healthRaw() {
       return getMirrorHealthReport();
     },

     associateRecommendations() {
       const payload =
         typeof getPixelAssociateRecommendations === "function"
           ? getPixelAssociateRecommendations()
           : window.PixelAssociateRecommendations || { recommendations: [] };

       console.table(
         (payload.recommendations || []).map(item => ({
           priority: item.priority,
           scope: item.scope,
           title: item.title,
           action: item.action,
           source: item.source
         }))
       );

       return payload;
     },

     checkoutIntents() {
       const intents =
         typeof getPixelCheckoutIntents === "function"
           ? getPixelCheckoutIntents()
           : [];

       console.table(
         intents.map(intent => ({
           createdAt: intent.createdAt,
           status: intent.status,
           storeName: intent.storeName,
           anchorItem: intent.anchorItem,
           itemCount: intent.itemCount,
           totalValue:
             typeof formatPrice === "function"
               ? formatPrice(intent.totalValue || 0)
               : `$${Number(intent.totalValue || 0).toFixed(2)}`
         }))
       );

       return intents;
     },

     latestCheckoutIntent() {
       const intents =
         typeof getPixelCheckoutIntents === "function"
           ? getPixelCheckoutIntents()
           : [];

       const latest = window.PixelMirrorLatestCheckoutIntent || intents[0] || null;

       console.log("Latest checkout intent:", latest);
       return latest;
     },

     testReady() {
       speakPixelConcierge("ready");
       return this.status();
     },

     testScan(itemName = "Demo Jacket") {
       speakPixelConcierge("scan", { itemName });
       return this.status();
     },

     testThinking() {
       speakPixelConcierge("thinking");
       return this.status();
     },

     testLook(totalPrice = 604, score = 87) {
       speakPixelConcierge("creatingLook");

       window.setTimeout(() => {
         speakPixelConcierge("outfit", { totalPrice });
       }, 900);

       window.setTimeout(() => {
         speakPixelConcierge("outfitScore", { score });
       }, 1800);

       return this.status();
     },

     testSave(itemName = "Cashmere Crewneck", bagCount = 2) {
       speakPixelConcierge("saving", { itemName });

       window.setTimeout(() => {
         speakPixelConcierge("save", {
           itemName,
           bagCount
         });
       }, 900);

       return this.status();
     },

     testBag(bagCount = 2) {
       speakPixelConcierge("loadingBag");

       window.setTimeout(() => {
         speakPixelConcierge("bag", { bagCount });
       }, 900);

       return this.status();
     },

     testProfile(hasProfile = true) {
       speakPixelConcierge("loadingProfile");

       window.setTimeout(() => {
         speakPixelConcierge(hasProfile ? "profile" : "profileEmpty");
       }, 900);

       return this.status();
     },

     testIdle(delayMs = 5000) {
       localStorage.setItem("pixelMirrorIdleDelayMs", String(delayMs));
       resetAmbientIdleTimer();

       console.log(
         `Idle test mode active. Idle overlay should appear after ${delayMs}ms.`
       );

       return this.status();
     },

     stopIdleTest() {
       localStorage.removeItem("pixelMirrorIdleDelayMs");
       resetAmbientIdleTimer();

       console.log("Idle test mode stopped. Idle delay restored to normal.");

       return this.status();
     },

     showIdleNow() {
       showAmbientIdleMode();
       return this.status();
     },

     wakeIdle() {
       handleAmbientWakeInteraction();
       return this.status();
     },

     testCinematic() {
       toggleCinematicMode();
       return this.status();
     },

     enterCinematic() {
       if (!isCinematicModeActive()) {
         enterCinematicMode();
       }

       return this.status();
     },

     exitCinematic() {
       if (isCinematicModeActive()) {
         exitCinematicMode();
       }

       return this.status();
     },

     demoScan() {
       const demoItem = getDemoScannedItem();
       const vibe = getPixelConciergeVibe() || "Casual";

       currentRfid = demoItem.rfid;
       currentLoadedItem = demoItem;
       lastScannedItem = demoItem;

       renderScanResult(demoItem, vibe);
       pulseScanPanelSuccess();

       setStatus(`${demoItem.name} demo scan loaded.`, "success");
       showToast("Demo scan loaded.", "success");

       addTryOnTimelineEvent(
         "scan",
         `Demo scanned ${demoItem.name}`,
         `${demoItem.brand} • ${demoItem.category} • ${formatPrice(demoItem.price)}`,
         [
           vibe,
           demoItem.color,
           `RFID ${demoItem.rfid}`
         ]
       );

       speakPixelConcierge("scan", {
         itemName: demoItem.name
       });

       return this.status();
     },

     demoLook() {
       const demoItem = getDemoScannedItem();
       const demoOutfit = getDemoFullOutfit();

       currentRfid = demoItem.rfid;
       currentLoadedItem = demoItem;
       lastScannedItem = demoItem;

      renderFullOutfit(demoOutfit, true);

      if (typeof renderMirrorMainOutfitShowcase === "function") {
        renderMirrorMainOutfitShowcase(demoOutfit);
      }

      if (window.MirrorCustomerJourney?.outfit) {
        window.MirrorCustomerJourney.outfit(demoItem);
      }

      incrementTryOnLooksCreated();

       const totalPrice = getOutfitTotalPrice(demoOutfit);
       const outfitScore =
         typeof getAdvancedOutfitScores === "function"
           ? getAdvancedOutfitScores(demoOutfit).overallScore
           : demoOutfit.overallScore || 87;

       setStatus("Demo full look created.", "success");
       showToast("Demo full look created.", "success");

       speakPixelConcierge("outfit", {
         totalPrice
       });

       window.setTimeout(() => {
         speakPixelConcierge("outfitScore", {
           score: outfitScore
         });
       }, 1200);

       return this.status();
     },

     demoBag() {
       const demoItem = getDemoScannedItem();

       currentRfid = demoItem.rfid;
       currentLoadedItem = demoItem;
       lastScannedItem = demoItem;

       savedRfids.add(demoItem.rfid);
       setSaveButtonSaved();

       incrementTryOnSavesToBag();

       setStatus("Demo item saved to bag.", "success");
       showToast("Demo item saved to bag.", "success");

       speakPixelConcierge("save", {
         itemName: demoItem.name,
         bagCount: savedRfids.size || 1
       });

       window.setTimeout(() => {
         speakPixelConcierge("bag", {
           bagCount: savedRfids.size || 1
         });
       }, 1000);

       return this.status();
     },

     demoShowcase() {
       console.log("Starting Pixel Mirror visual showcase.");

       this.demoScan();

       window.setTimeout(() => {
         this.demoLook();
       }, 2200);

       window.setTimeout(() => {
         this.demoBag();
       }, 5200);

       window.setTimeout(() => {
         speakPixelConcierge("cinematic");
         setStatus("Demo showcase complete. Cinematic mode is ready.", "success");
       }, 7600);

       return this.status();
     },

     demo() {
       console.log("Starting Pixel Mirror demo mode.");

       const demoSteps = [
         {
           delay: 0,
           label: "Ready",
           run: () => {
             speakPixelConcierge("ready");
             setStatus("Demo started. Pixel is ready.", "ready");
           }
         },
         {
           delay: 1400,
           label: "Visual Scan",
           run: () => {
             this.demoScan();
           }
         },
         {
           delay: 4200,
           label: "Visual Look",
           run: () => {
             this.demoLook();
           }
         },
         {
           delay: 7600,
           label: "Visual Save",
           run: () => {
             this.demoBag();
           }
         },
         {
           delay: 9800,
           label: "Cinematic Ready",
           run: () => {
             speakPixelConcierge("cinematic");
             setStatus("Demo complete. Cinematic mode is ready.", "success");
           }
         }
       ];

       demoSteps.forEach(step => {
         window.setTimeout(() => {
           console.log(`Pixel demo step: ${step.label}`);
           step.run();
         }, step.delay);
       });

       return this.status();
     },

     testErrors() {
       const tests = [
         ["scanError", "Demo scan error."],
         ["outfitError", "Demo outfit error."],
         ["saveError", "Demo save error."],
         ["error", "Demo general error."]
       ];

       tests.forEach(([action, errorMessage], index) => {
         window.setTimeout(() => {
           speakPixelConcierge(action, { errorMessage });
         }, index * 1100);
       });

       return this.status();
     },

     testAll() {
       console.log("Running Pixel Concierge message tests.");

       const tests = [
         () => speakPixelConcierge("ready"),
         () => speakPixelConcierge("scanBlocked"),
         () => speakPixelConcierge("scan", { itemName: "Demo Jacket" }),
         () => speakPixelConcierge("thinking"),
         () => speakPixelConcierge("creatingLook"),
         () => speakPixelConcierge("outfit", { totalPrice: 604 }),
         () => speakPixelConcierge("outfitScore", { score: 87 }),
         () => speakPixelConcierge("saving", { itemName: "Demo Jacket" }),
         () => speakPixelConcierge("save", { itemName: "Demo Jacket", bagCount: 2 }),
         () => speakPixelConcierge("loadingBag"),
         () => speakPixelConcierge("bag", { bagCount: 2 }),
         () => speakPixelConcierge("loadingProfile"),
         () => speakPixelConcierge("profile"),
         () => speakPixelConcierge("profileEmpty"),
         () => speakPixelConcierge("store"),
         () => speakPixelConcierge("cinematic"),
         () => speakPixelConcierge("cinematicExit"),
         () => speakPixelConcierge("idle"),
         () => console.log("Tip: run PixelMirrorDebug.testIdle(5000) to test real idle overlay."),
         () => console.log("Tip: run PixelMirrorDebug.testCinematic() to toggle cinematic mode."),
         () => speakPixelConcierge("wake"),
         () => speakPixelConcierge("reset"),
         () => speakPixelConcierge("scanError", { errorMessage: "Demo scan error." }),
         () => speakPixelConcierge("outfitError", { errorMessage: "Demo outfit error." }),
         () => speakPixelConcierge("saveError", { errorMessage: "Demo save error." }),
         () => speakPixelConcierge("error", { errorMessage: "Demo general error." })
       ];

       tests.forEach((runTest, index) => {
         window.setTimeout(runTest, index * 900);
       });

       return this.status();
     }
   };

   console.log("PixelMirrorDebug is ready. Try: PixelMirrorDebug.health()");
 }

 function bindEvents() {
   document.getElementById("scanBtn")?.addEventListener("click", handleScan);
   document.getElementById("createLookBtn")?.addEventListener("click", createFullLook);
   document.getElementById("saveToBagBtn")?.addEventListener("click", saveToBag);
   document.getElementById("bagToggleBtn")?.addEventListener("click", () => loadBag(true));
   document.getElementById("viewBagInlineBtn")?.addEventListener("click", () => loadBag(true));
   document.getElementById("refreshBagBtn")?.addEventListener("click", () => loadBag(true));
   document.getElementById("newScanBtn")?.addEventListener("click", resetMirror);
   document.getElementById("refreshShopperProfileBtn")?.addEventListener("click", loadMirrorShopperProfile);
   document.getElementById("clearTryOnMemoryBtn")?.addEventListener("click", clearTryOnMemory);
   document.getElementById("exitCinematicBtn")?.addEventListener("click", exitCinematicMode);
   document.addEventListener("fullscreenchange", syncCinematicModeFromFullscreen);

   document.getElementById("rfidInput")?.addEventListener("keydown", event => {
     if (event.key === "Enter") {
       event.preventDefault();
       handleScan();
     }
   });

   document.getElementById("vibeSelect")?.addEventListener("change", event => {
     refreshMirrorRuntime();
     addTryOnTimelineEvent(
       "vibe",
       `Vibe changed to ${event.target.value || "Casual"}`,
       "Future scans and generated looks will use this styling direction.",
       [getMirrorStoreDisplayName()]
     );

     speakPixelConcierge("ready");
   });

   document.getElementById("retailerSelect")?.addEventListener("change", event => {
     const retailerKey = event.target.value || "MACY001";

     populateStoreOptions(retailerKey);

     const context = persistCurrentMirrorStoreContext();
     refreshMirrorRuntime();
     renderStoreOwnedReadyState();
     renderMirrorQuickScans();
     renderTryOnMemory();
     renderTryOnTimeline();

     addTryOnTimelineEvent(
       "store",
       `Retailer changed to ${context.retailerKey}`,
       "Mirror retailer context was updated.",
       [context.storeName]
     );

     speakPixelConcierge("store");
   });

   document.getElementById("storeCodeSelect")?.addEventListener("change", () => {
     const context = persistCurrentMirrorStoreContext();
     refreshMirrorRuntime();

     renderStoreOwnedReadyState();
     renderMirrorQuickScans();
     renderTryOnMemory();
     renderTryOnTimeline();

     addTryOnTimelineEvent(
       "store",
       `Store changed to ${context.storeName || context.storeCode}`,
       "Mirror inventory context was updated.",
       [context.storeCode]
     );

     speakPixelConcierge("store");
   });

   bindMirrorCommandDock();
   bindMirrorKeyboardShortcuts();
   bindVoiceCommandParser();
   bindAmbientIdleMode();
 }

 function bindCommandConsoleToggle() {
   const commandDock = document.querySelector(".mirror-command-dock");
   const toggleButton = document.getElementById("toggleCommandConsole");

   if (!commandDock || !toggleButton) {
     return;
   }

   toggleButton.addEventListener("click", () => {
     const isOpen = commandDock.classList.toggle("show-command-console");

     toggleButton.setAttribute("aria-expanded", String(isOpen));

     if (isOpen) {
       showToast("Command input opened.", "info");
     } else {
       showToast("Command input hidden.", "info");
     }
   });
 }

function init() {
  const usedLoggedInContext = applyLoggedInMirrorContext();

  if (!usedLoggedInContext) {
    populateRetailerSelect("MACY001");
    populateStoreOptions(getSelectedRetailerKey());
  }

  bindEvents();
  bindCommandConsoleToggle();
  updateAuthStatus();
  setSaveButtonDefault(true);

  setStatus(
    getToken() ? "Ready to scan." : "Login from the Merchant App first.",
    getToken() ? "ready" : "error"
  );

  if (getToken()) {
    loadBag(false);
    loadMirrorShopperProfile();
  }

  hydrateFromUrlParams();
  persistCurrentMirrorStoreContext();

  let runtime = null;

  if (typeof refreshMirrorRuntime === "function") {
    runtime = refreshMirrorRuntime();
  }

  renderStoreOwnedReadyState();
  speakPixelConcierge("ready");

  if (PIXEL_MIRROR_DEBUG_ENABLED && typeof exposePixelMirrorDebugTools === "function") {
    exposePixelMirrorDebugTools();
  }

  if (typeof exposeMirrorRuntimeTools === "function") {
    exposeMirrorRuntimeTools();
  }

  if (typeof exposeMirrorCustomerJourneyTools === "function") {
    exposeMirrorCustomerJourneyTools();
  }

  if (typeof exposeMirrorShowroomTools === "function") {
    exposeMirrorShowroomTools();
  }

  if (typeof exposeMirrorMainExperienceTools === "function") {
    exposeMirrorMainExperienceTools();
  }

  renderMirrorQuickScans();
  renderTryOnMemory();
  renderTryOnTimeline();

  if (typeof buildMirrorMainExperience === "function") {
    buildMirrorMainExperience();

    if (runtime && typeof applyMirrorRuntimeToExperience === "function") {
      applyMirrorRuntimeToExperience(runtime);
    }
  }

  if (typeof showMirrorMainExperience === "function") {
    forceMirrorMainPageVisible();

    window.setTimeout(() => {
      try {
        const opened = showMirrorMainExperience();

        if (!opened) {
          console.warn("Mirror main experience did not open cleanly.");
        }

        if (window.MirrorCustomerJourney?.sync) {
          window.MirrorCustomerJourney.sync();
        }

        forceMirrorMainPageVisible();
      } catch (error) {
        console.error("Delayed mirror main boot failed:", error);
        window.PixelMirrorBootError = error;
        forceMirrorMainPageVisible();
      }
    }, 60);

    return;
  }

  if (typeof initializeMirrorLockScreen === "function") {
    initializeMirrorLockScreen();
  } else if (typeof showMirrorWelcomeScreen === "function") {
    showMirrorWelcomeScreen();
  }
}

 /* =========================================================
    Universal Stylist — Customer Journey Engine v1
    Controls the premium customer-facing mirror flow
    ========================================================= */

 const MIRROR_CUSTOMER_STAGES = {
   LANDING: "landing",
   SCANNING: "scanning",
   PRODUCT: "product",
   OUTFIT: "outfit",
   SAVED: "saved",
   ASSISTANCE: "assistance"
 };

 let currentMirrorCustomerStage = MIRROR_CUSTOMER_STAGES.LANDING;

 function getMirrorCustomerStageLabel(stage = currentMirrorCustomerStage) {
   const labels = {
     [MIRROR_CUSTOMER_STAGES.LANDING]: "Editorial Landing",
     [MIRROR_CUSTOMER_STAGES.SCANNING]: "Scan State",
     [MIRROR_CUSTOMER_STAGES.PRODUCT]: "Product Reveal",
     [MIRROR_CUSTOMER_STAGES.OUTFIT]: "Outfit Creation",
     [MIRROR_CUSTOMER_STAGES.SAVED]: "Save / Bag Intent",
     [MIRROR_CUSTOMER_STAGES.ASSISTANCE]: "Associate Assistance"
   };

   return labels[stage] || "Customer Experience";
 }

 function getMirrorCustomerJourneyProductName(item = currentLoadedItem) {
   if (!item) {
     const product = typeof getMirrorMainProduct === "function" ? getMirrorMainProduct() : null;
     return product?.name || "this item";
   }

   return (
     item.name ||
     item.itemName ||
     item.productName ||
     item.title ||
     "this item"
   );
 }

 function setMirrorCustomerStage(stage, detail = {}) {
   if (!Object.values(MIRROR_CUSTOMER_STAGES).includes(stage)) {
     return currentMirrorCustomerStage;
   }

   currentMirrorCustomerStage = stage;

   document.body.dataset.mirrorCustomerStage = stage;

   const overlay = document.getElementById("mirrorMainExperience");
   if (overlay) {
     overlay.dataset.customerStage = stage;
   }

   const stageLabel = getMirrorCustomerStageLabel(stage);
   const productName = getMirrorCustomerJourneyProductName(detail.item);

   if (typeof setStatus === "function") {
     setStatus(stageLabel, "ready");
   }

   updateMirrorCustomerStageCopy(stage, {
     ...detail,
     productName,
     stageLabel
   });

   updateMirrorCustomerJourneyProgress(stage);

   return currentMirrorCustomerStage;
 }

 function updateMirrorCustomerStageCopy(stage, detail = {}) {
   const msgOne = document.getElementById("mirrorMainPixelMsgOne");
   const msgTwo = document.getElementById("mirrorMainPixelMsgTwo");
   const msgThree = document.getElementById("mirrorMainPixelMsgThree");

   if (!msgOne || !msgTwo || !msgThree) {
     return;
   }

   const runtime =
     typeof getMirrorRuntime === "function"
       ? getMirrorRuntime()
       : window.MirrorRuntimeState;

   const storeName =
     runtime?.storeName ||
     (typeof getMirrorShowroomDisplayStore === "function"
       ? getMirrorShowroomDisplayStore()
       : "this store");

   const productName = detail.productName || "this item";

   if (stage === MIRROR_CUSTOMER_STAGES.LANDING) {
     msgOne.textContent = "“Welcome. Scan a tagged item and I’ll style it from this store.”";
     msgTwo.textContent = `“I’ll use ${storeName} inventory, your vibe, and shopper preferences.”`;
     msgThree.textContent = "“Your product reveal will appear here once the mirror reads an item.”";
     return;
   }

   if (stage === MIRROR_CUSTOMER_STAGES.SCANNING) {
     msgOne.textContent = "“I’m listening for a product tag now.”";
     msgTwo.textContent = "“Hold the item near the mirror reader for a clean scan.”";
     msgThree.textContent = "“Once detected, I’ll reveal the product and styling direction.”";
     return;
   }

   if (stage === MIRROR_CUSTOMER_STAGES.PRODUCT) {
     msgOne.textContent = `“${productName} is on the mirror.”`;
     msgTwo.textContent = `“I’m curating ${storeName} pieces that complement the color, category, and styling direction.”`;
     msgThree.textContent = "“Create a full outfit when you’re ready, and I’ll complete the look.”";
     return;
   }

   if (stage === MIRROR_CUSTOMER_STAGES.OUTFIT) {
     msgOne.textContent = `“I’m building a complete look around ${productName}.”`;
     msgTwo.textContent = "“I’ll balance color, category, silhouette, price, and availability.”";
     msgThree.textContent = "“The final look can be saved or shared with an associate.”";
     return;
   }

   if (stage === MIRROR_CUSTOMER_STAGES.SAVED) {
     msgOne.textContent = `“${productName} has been added to the customer bag.”`;
     msgTwo.textContent = "“The associate view can help with sizes, availability, or checkout.”";
     msgThree.textContent = "“Scan another item to continue the styling session.”";
     return;
   }

   if (stage === MIRROR_CUSTOMER_STAGES.ASSISTANCE) {
     msgOne.textContent = "“Associate assistance is ready.”";
     msgTwo.textContent = "“They can view session context, product interest, and next best actions.”";
     msgThree.textContent = "“The customer experience remains calm while controls stay tucked away.”";
   }
 }

 function updateMirrorCustomerJourneyProgress(stage = currentMirrorCustomerStage) {
   const order = [
     MIRROR_CUSTOMER_STAGES.LANDING,
     MIRROR_CUSTOMER_STAGES.SCANNING,
     MIRROR_CUSTOMER_STAGES.PRODUCT,
     MIRROR_CUSTOMER_STAGES.OUTFIT,
     MIRROR_CUSTOMER_STAGES.SAVED
   ];

   const activeIndex = Math.max(order.indexOf(stage), 0);

   document.querySelectorAll("[data-mirror-journey-step]").forEach(step => {
     const stepName = step.dataset.mirrorJourneyStep;
     const stepIndex = order.indexOf(stepName);

     step.classList.toggle("is-active", stepName === stage);
     step.classList.toggle("is-complete", stepIndex >= 0 && stepIndex < activeIndex);
   });
 }

 function syncMirrorCustomerJourney() {
   if (currentLoadedItem) {
     return setMirrorCustomerStage(MIRROR_CUSTOMER_STAGES.PRODUCT, {
       item: currentLoadedItem
     });
   }

   if (document.body.classList.contains("mirror-main-demo-product-active")) {
     return setMirrorCustomerStage(MIRROR_CUSTOMER_STAGES.PRODUCT);
   }

   return setMirrorCustomerStage(MIRROR_CUSTOMER_STAGES.LANDING);
 }

 function addMirrorCustomerJourneyTimelineEvent(stage, message, detail = "") {
   if (typeof addTryOnTimelineEvent !== "function") {
     return;
   }

   addTryOnTimelineEvent(
     "customer-journey",
     message,
     detail || getMirrorCustomerStageLabel(stage),
     [
       typeof getMirrorShowroomDisplayStore === "function"
         ? getMirrorShowroomDisplayStore()
         : "Smart Mirror"
     ]
   );
 }

 function exposeMirrorCustomerJourneyTools() {
   window.MirrorCustomerJourney = {
     stages: MIRROR_CUSTOMER_STAGES,

     get() {
       return currentMirrorCustomerStage;
     },

     status() {
       return {
         stage: currentMirrorCustomerStage,
         label: getMirrorCustomerStageLabel(),
         hasProduct: !!currentLoadedItem,
         runtime:
           typeof getMirrorRuntime === "function"
             ? getMirrorRuntime()
             : window.MirrorRuntimeState || null
       };
     },

     sync() {
       return syncMirrorCustomerJourney();
     },

     landing() {
       addMirrorCustomerJourneyTimelineEvent(
         MIRROR_CUSTOMER_STAGES.LANDING,
         "Customer landed on the editorial mirror experience."
       );

       return setMirrorCustomerStage(MIRROR_CUSTOMER_STAGES.LANDING);
     },

     scan() {
       addMirrorCustomerJourneyTimelineEvent(
         MIRROR_CUSTOMER_STAGES.SCANNING,
         "Customer started a scan."
       );

       if (typeof speakPixelConcierge === "function") {
         speakPixelConcierge("scan");
       }

       return setMirrorCustomerStage(MIRROR_CUSTOMER_STAGES.SCANNING);
     },

     product(item = currentLoadedItem) {
       const productName = getMirrorCustomerJourneyProductName(item);

       addMirrorCustomerJourneyTimelineEvent(
         MIRROR_CUSTOMER_STAGES.PRODUCT,
         `${productName} revealed on the customer mirror.`,
         "Product reveal"
       );

       return setMirrorCustomerStage(MIRROR_CUSTOMER_STAGES.PRODUCT, {
         item
       });
     },

     outfit(item = currentLoadedItem) {
       const productName = getMirrorCustomerJourneyProductName(item);

       addMirrorCustomerJourneyTimelineEvent(
         MIRROR_CUSTOMER_STAGES.OUTFIT,
         `Outfit creation started around ${productName}.`,
         "Pixel Intelligence"
       );

       return setMirrorCustomerStage(MIRROR_CUSTOMER_STAGES.OUTFIT, {
         item
       });
     },

     saved(item = currentLoadedItem) {
       const productName = getMirrorCustomerJourneyProductName(item);

       addMirrorCustomerJourneyTimelineEvent(
         MIRROR_CUSTOMER_STAGES.SAVED,
         `${productName} saved to the customer bag.`,
         "Bag intent"
       );

       return setMirrorCustomerStage(MIRROR_CUSTOMER_STAGES.SAVED, {
         item
       });
     },

     assistance() {
       addMirrorCustomerJourneyTimelineEvent(
         MIRROR_CUSTOMER_STAGES.ASSISTANCE,
         "Associate assistance opened from the customer mirror."
       );

       return setMirrorCustomerStage(MIRROR_CUSTOMER_STAGES.ASSISTANCE);
     },

     health() {
       const overlay = document.getElementById("mirrorMainExperience");

       const report = {
         healthy: !!overlay && !!window.MirrorCustomerJourney,
         stage: currentMirrorCustomerStage,
         label: getMirrorCustomerStageLabel(),
         hasOverlay: !!overlay,
         hasPixelMessages:
           !!document.getElementById("mirrorMainPixelMsgOne") &&
           !!document.getElementById("mirrorMainPixelMsgTwo") &&
           !!document.getElementById("mirrorMainPixelMsgThree")
       };

       console.table(report);
       return report;
     }
   };

   console.log("MirrorCustomerJourney ready. Try: MirrorCustomerJourney.health()");
 }

 /* =========================================================
    Universal Stylist — Customer Main Experience v1
    UXCanvas-inspired vanilla overlay
    Paste above the final DOMContentLoaded block
    ========================================================= */

 const MIRROR_MAIN_DEMO_IMAGES = {
   sweater: "https://placehold.co/700x900/f3eadc/1a1814?text=Cashmere+Turtleneck",
   trousers: "https://placehold.co/700x900/e8e3dc/1a1814?text=Wide-Leg+Trousers",
   heels: "https://placehold.co/700x900/f7f5f2/1a1814?text=Pointed+Heels"
 };

 function getMirrorMainBagCount() {
   if (savedRfids && typeof savedRfids.size === "number") {
     return savedRfids.size;
   }

   return 0;
 }

function focusMirrorScanInput() {
  const mainInput = document.getElementById("mirrorMainRfidInput");

  if (mainInput) {
    mainInput.focus();
    mainInput.select?.();
    return;
  }

  const rfidInput = document.getElementById("rfidInput");

  if (rfidInput) {
    rfidInput.focus();
    rfidInput.select?.();
  }
}

function handleMirrorWelcomeScanStart() {
  if (typeof hideMirrorWelcomeScreen === "function") {
    hideMirrorWelcomeScreen();
  }

  window.setTimeout(() => {
    focusMirrorScanInput();

    if (typeof speakPixelConcierge === "function") {
      speakPixelConcierge("scanBlocked");
    }

    if (typeof setStatus === "function") {
      setStatus("Hold an item near the RFID reader or enter an RFID to scan.", "ready");
    }
  }, 520);

  return true;
}

function handleMirrorWelcomeExploreStart() {
  if (typeof hideMirrorWelcomeScreen === "function") {
    hideMirrorWelcomeScreen();
  }

  window.setTimeout(() => {
    if (typeof showMirrorMainExperience === "function") {
      showMirrorMainExperience();
    }

    if (typeof speakPixelConcierge === "function") {
      speakPixelConcierge("ready");
    }

    if (typeof setStatus === "function") {
      setStatus("Customer Main Experience is active.", "ready");
    }
  }, 520);

  return true;
}

function getMirrorShowroomDisplayStore() {
  const runtime = window.MirrorRuntimeState || currentMirrorRuntime;

  if (runtime?.storeName) {
    return runtime.storeName;
  }

  if (typeof getMirrorStoreDisplayName === "function") {
    return getMirrorStoreDisplayName();
  }

  if (typeof getPixelConciergeStoreName === "function") {
    return getPixelConciergeStoreName();
  }

  const params = new URLSearchParams(window.location.search);
  const storeName = params.get("storeName");

  return storeName ? beautifyStoreName(storeName.replace(/\+/g, " ")) : "Nicks Boutique";
}

function escapeMirrorShowroomHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function getMirrorInventoryImageUrl(item, fallbackText = "Product Image") {
  if (!item || typeof item !== "object") {
    return `https://placehold.co/700x900/f7f3ec/171411?text=${encodeURIComponent(fallbackText)}`;
  }

  const directImage = getItemField(
    item,
    "imageUrl",
    "image_url",
    "image",
    "photoUrl",
    "productImageUrl",
    "primaryImage",
    "primaryImageUrl",
    "thumbnailUrl",
    "mediaUrl",
    "imageSrc",
    "pictureUrl"
  );

  if (directImage) {
    return safeImageUrl(
      directImage,
      `https://placehold.co/700x900/f7f3ec/171411?text=${encodeURIComponent(fallbackText)}`
    );
  }

  const possibleNestedImages = [
    item.product?.imageUrl,
    item.product?.image,
    item.product?.primaryImage,
    item.product?.primaryImageUrl,
    item.product?.thumbnailUrl,
    item.inventoryItem?.imageUrl,
    item.inventoryItem?.image,
    item.media?.[0]?.url,
    item.media?.[0]?.imageUrl,
    item.images?.[0]?.url,
    item.images?.[0]?.imageUrl,
    item.images?.[0]
  ].filter(Boolean);

  if (possibleNestedImages.length) {
    return safeImageUrl(
      possibleNestedImages[0],
      `https://placehold.co/700x900/f7f3ec/171411?text=${encodeURIComponent(fallbackText)}`
    );
  }

  return `https://placehold.co/700x900/f7f3ec/171411?text=${encodeURIComponent(fallbackText)}`;
}

 function getMirrorMainProduct() {
   if (currentLoadedItem) {
     return {
       name:
         getItemField(currentLoadedItem, "name", "itemName", "productName", "title") ||
         "Scanned Item",
       store:
         getItemField(currentLoadedItem, "retailer", "retailerName") ||
         getMirrorShowroomDisplayStore(),
       price: formatPrice(getItemField(currentLoadedItem, "price")),
       meta: [
         getItemField(currentLoadedItem, "category"),
         getItemField(currentLoadedItem, "color")
       ].filter(Boolean).join(" · ") || "Store item",
       imageUrl: getMirrorInventoryImageUrl(currentLoadedItem, "Product Image")
     };
   }

   return {
     name: "No item scanned yet",
     store: getMirrorShowroomDisplayStore(),
     price: "",
     meta: "Scan a real RFID item to begin",
     imageUrl: "https://placehold.co/700x900/f7f3ec/171411?text=Ready+to+Scan"
   };
 }

 function getMirrorMainPixelGlyph() {
   return `
     <svg class="mirror-main-glyph" viewBox="0 0 48 48" fill="none" aria-hidden="true">
       <polygon
         class="outer"
         points="24,4 40,16 40,32 24,44 8,32 8,16"
         stroke="url(#mirrorMainGlyphGrad)"
         stroke-width="1.5"
         fill="none"
       ></polygon>

       <polygon
         class="inner"
         points="24,10 36,18 36,30 24,38 12,30 12,18"
         stroke="url(#mirrorMainGlyphGrad2)"
         stroke-width="1"
         fill="none"
       ></polygon>

       <circle class="core" cx="24" cy="24" r="4" fill="url(#mirrorMainGlyphGrad)"></circle>

       <defs>
         <linearGradient id="mirrorMainGlyphGrad" x1="0" y1="0" x2="1" y2="1">
           <stop offset="0%" stop-color="#4A90D9"></stop>
           <stop offset="100%" stop-color="#C3B8E8"></stop>
         </linearGradient>

         <linearGradient id="mirrorMainGlyphGrad2" x1="1" y1="0" x2="0" y2="1">
           <stop offset="0%" stop-color="#C3B8E8"></stop>
           <stop offset="100%" stop-color="#7DBF8E"></stop>
         </linearGradient>
       </defs>
     </svg>
   `;
 }

function buildMirrorMainExperience() {
  const existing = document.getElementById("mirrorMainExperience");

  if (existing) {
    updateMirrorMainProductCard();
    updateMirrorMainBagCount();
    renderMirrorMainRecentScans();
    renderMirrorMainTimeline();
    return existing;
  }

  const storeName = escapeMirrorShowroomHtml(getMirrorShowroomDisplayStore());
  const product = getMirrorMainProduct();
  const bagCount = getMirrorMainBagCount();

  const main = document.createElement("section");
  main.id = "mirrorMainExperience";
  main.className = "mirror-main-experience";
  main.setAttribute("aria-label", "Universal Stylist customer main experience");
  main.setAttribute("aria-hidden", "true");

  main.innerHTML = `
    <div class="mirror-main-bg-orb one" aria-hidden="true"></div>
    <div class="mirror-main-bg-orb two" aria-hidden="true"></div>

    <nav class="mirror-main-nav">
      <button class="mirror-main-brand" type="button" id="mirrorMainBackToLockBtn">
        Universal Stylist
      </button>

      <div class="mirror-main-store">
        <span class="mirror-main-store-dot" aria-hidden="true"></span>
        <span>${storeName}</span>
      </div>

      <div class="mirror-main-actions">
        <button class="mirror-main-icon-btn" type="button" id="mirrorMainProfileBtn" aria-label="Open shopper profile">
          ♙
        </button>

        <button class="mirror-main-icon-btn" type="button" id="mirrorMainBagBtn" aria-label="Open bag">
          ♧
          <span class="mirror-main-bag-count" id="mirrorMainBagCount">${bagCount}</span>
        </button>

        <button class="mirror-main-icon-btn" type="button" id="mirrorMainShowroomBtn" aria-label="Showroom mode">
          ▣
        </button>

        <button class="mirror-main-icon-btn" type="button" id="mirrorMainExitBtn" aria-label="Exit customer main experience">
          ×
        </button>
      </div>
    </nav>

    <div class="mirror-main-layout">
      <aside class="mirror-main-left">
       <div class="mirror-main-recent" id="mirrorMainRecentScans">
         <span class="mirror-main-recent-label">Recent Scans</span>
       </div>

        <section class="mirror-main-card mirror-main-scan-card">
          <div class="mirror-main-scan-inner">
            <div class="mirror-main-card-top">
              <span class="mirror-main-small-label">Scan an Item</span>
              <button class="mirror-main-close-mini" type="button" id="mirrorMainResetProductBtn" aria-label="Clear current product">
                ×
              </button>
            </div>

            <div class="mirror-main-scan-idle" id="mirrorMainScanIdle">
              <button class="mirror-main-scan-ring" type="button" id="mirrorMainScanRingBtn" aria-label="Start scan">
                <span class="mirror-main-zap">✦</span>
              </button>

              <p class="mirror-main-scan-copy">
                Hold any tagged item near the reader to instantly reveal details.
              </p>

             <div class="mirror-main-rfid-entry">
               <input
                 class="mirror-main-rfid-input"
                 id="mirrorMainRfidInput"
                 type="text"
                 inputmode="text"
                 autocomplete="off"
                 placeholder="Enter or scan RFID"
                 aria-label="Enter RFID"
               >

               <button class="mirror-main-secondary" type="button" id="mirrorMainRunScanBtn">
                 Scan Item
               </button>
             </div>
            </div>

            <div class="mirror-main-product" id="mirrorMainProductCard">
              <div class="mirror-main-product-image">
                <img
                  id="mirrorMainProductImage"
                  src="${escapeMirrorShowroomHtml(product.imageUrl)}"
                  alt="${escapeMirrorShowroomHtml(product.name)}"
                >
              </div>

              <div class="mirror-main-product-row">
                <div>
                  <p class="mirror-main-product-store" id="mirrorMainProductStore">${escapeMirrorShowroomHtml(product.store)}</p>
                  <p class="mirror-main-product-name" id="mirrorMainProductName">${escapeMirrorShowroomHtml(product.name)}</p>
                </div>

                <div class="mirror-main-product-price" id="mirrorMainProductPrice">${escapeMirrorShowroomHtml(product.price)}</div>
              </div>

              <p class="mirror-main-product-meta" id="mirrorMainProductMeta">${escapeMirrorShowroomHtml(product.meta)}</p>

              <div class="mirror-main-product-buttons">
                <button class="mirror-main-primary" type="button" id="mirrorMainCreateLookBtn">
                  Create Full Outfit
                </button>

                <button class="mirror-main-secondary" type="button" id="mirrorMainAddToLookBtn">
                  Save to Bag
                </button>

                <button class="mirror-main-subtle" type="button" id="mirrorMainSeeMoreBtn" hidden>
                  Continue in Associate Mirror
                </button>
              </div>
            </div>
          </div>
        </section>
      </aside>

      <main class="mirror-main-center">
        <section class="mirror-main-editorial">
         <p class="mirror-main-eyebrow">Autumn — Winter Collection</p>

         <h1 class="mirror-main-title mirror-main-title-silence">
           <span>Dressed in</span>
           <em>Silence.</em>
         </h1>

        <p class="mirror-main-editorial-copy">
          Curated selections from ${storeName}, brought to your reflection.
        </p>

          <button class="mirror-main-start" type="button" id="mirrorMainStartScanBtn">
            ✦ Start Scanning
          </button>
        </section>
      </main>

      <aside class="mirror-main-right">
        <section class="mirror-main-card mirror-main-pixel-card">
          <div class="mirror-main-pixel-header">
            <div class="mirror-main-pixel-title">
              ${getMirrorMainPixelGlyph()}
              <span>Pixel Concierge</span>
            </div>

            <button class="mirror-main-pixel-close" type="button" id="mirrorMainPixelCloseBtn" aria-label="Hide Pixel Concierge">
              ×
            </button>
          </div>

         <div class="mirror-main-pixel-messages" id="mirrorMainPixelMessages">
           <p class="mirror-main-pixel-msg featured" id="mirrorMainPixelMsgOne">
             “Scan a tagged item and I’ll build a complete look from this store.”
           </p>

           <p class="mirror-main-pixel-msg" id="mirrorMainPixelMsgTwo">
             “I’ll check color, category, budget, profile signals, and availability.”
           </p>

           <p class="mirror-main-pixel-msg" id="mirrorMainPixelMsgThree">
             “Your recommendations will update as soon as an item appears.”
           </p>
         </div>
        </section>
      </aside>
        </div>

        <section class="mirror-main-outfit-showcase" id="mirrorMainOutfitShowcase" aria-hidden="true">
          <div class="mirror-main-outfit-glass">
            <div class="mirror-main-outfit-head">
              <div>
                <p class="mirror-main-small-label">Pixel Full Outfit</p>
                <h2 id="mirrorMainOutfitTitle">Complete look is ready.</h2>
              </div>

              <div class="mirror-main-outfit-score" id="mirrorMainOutfitScore">0%</div>

              <button class="mirror-main-close-mini" type="button" id="mirrorMainOutfitCloseBtn" aria-label="Close outfit showcase">
                ×
              </button>
            </div>

            <div class="mirror-main-outfit-grid" id="mirrorMainOutfitGrid">
              <div class="mirror-main-outfit-empty">
                Create a full outfit to see Pixel’s styling recommendation.
              </div>
            </div>

            <p class="mirror-main-outfit-explain" id="mirrorMainOutfitExplain">
              Pixel will explain how the look works once it is generated.
            </p>

            <div class="mirror-main-outfit-actions">
             <button class="mirror-main-primary" type="button" id="mirrorMainSaveLookBtn">
               Save Complete Look
             </button>

              <button class="mirror-main-secondary" type="button" id="mirrorMainOutfitAnotherBtn">
                Scan Another Item
              </button>
            </div>
          </div>
        </section>

        <button class="mirror-main-avatar-pill" type="button" id="mirrorMainPixelOpenBtn">
      ${getMirrorMainPixelGlyph()}
      <span>Pixel</span>
      <span class="mirror-main-live-dot" aria-hidden="true"></span>
    </button>

    <section class="mirror-main-timeline">
      <div class="mirror-main-timeline-handle" aria-hidden="true"></div>

      <div class="mirror-main-timeline-row" id="mirrorMainTimelineRow">
        <article class="mirror-main-timeline-item">
          <div>
            <p class="mirror-main-timeline-title">Ready for first scan</p>
            <p class="mirror-main-timeline-sub">Real inventory appears here</p>
          </div>
        </article>
      </div>
    </section>

    <section class="mirror-main-profile-drawer" id="mirrorMainProfileDrawer" aria-label="Shopper profile drawer">
      <button class="mirror-main-dim" type="button" id="mirrorMainProfileDim" aria-label="Close profile drawer"></button>

      <aside class="mirror-main-profile-panel">
        <button class="mirror-main-close-mini" type="button" id="mirrorMainProfileCloseBtn" aria-label="Close profile drawer">
          ×
        </button>

        <h2 class="mirror-main-profile-title">
          Your Style<br>
          <strong>Profile</strong>
        </h2>

        <div class="mirror-main-profile-grid">
          <div class="mirror-main-profile-card">
            <small>Style Archetype</small>
            <strong>Modern Minimalist</strong>
          </div>

          <div class="mirror-main-profile-card">
            <small>Preferred Palette</small>
            <strong>Black · Ivory · Camel · Blue</strong>
          </div>

          <div class="mirror-main-profile-card">
            <small>Sizing</small>
            <strong>Top M · Bottom 28/30 · Shoes EU 38</strong>
          </div>

          <div class="mirror-main-profile-card">
            <small>Pixel Insight</small>
            <strong>Warm neutrals and clean silhouettes are trending in this session.</strong>
          </div>
        </div>
      </aside>
    </section>

    <section class="mirror-main-bag-drawer" id="mirrorMainBagDrawer" aria-label="Shopper bag drawer" aria-hidden="true">
      <button class="mirror-main-bag-dim" type="button" id="mirrorMainBagDim" aria-label="Close bag drawer"></button>

      <aside class="mirror-main-bag-panel">
        <header class="mirror-main-bag-head">
          <div>
            <p class="mirror-main-bag-kicker">Customer Intent</p>
            <h2 class="mirror-main-bag-title">Style Bag</h2>
          </div>

          <button class="mirror-main-close-mini" type="button" id="mirrorMainBagCloseBtn" aria-label="Close bag drawer">
            ×
          </button>
        </header>

       <section class="mirror-main-bag-summary">
         <div class="mirror-main-bag-stat">
           <span>Saved</span>
           <strong id="mirrorMainBagDrawerCount">0</strong>
         </div>

         <div class="mirror-main-bag-stat">
           <span>Value</span>
           <strong id="mirrorMainBagDrawerValue">$0.00</strong>
         </div>

         <div class="mirror-main-bag-stat">
           <span>Categories</span>
           <strong id="mirrorMainBagDrawerCategories">0</strong>
         </div>

         <div class="mirror-main-bag-stat">
           <span>Store</span>
           <strong id="mirrorMainBagDrawerStore">${storeName}</strong>
         </div>
       </section>

       <div class="mirror-main-bag-empty" id="mirrorMainBagIntentNote">
         Pixel will summarize customer intent once saved items appear.
       </div>

        <div class="mirror-main-bag-content" id="mirrorMainBagDrawerContent">
          <div class="mirror-main-bag-empty">
            Saved items will appear here after the customer taps Save to Bag.
          </div>
        </div>

        <footer class="mirror-main-bag-foot">
          <button class="mirror-main-primary" type="button" id="mirrorMainBagDrawerScanBtn">
            Scan Another Item
          </button>

          <button class="mirror-main-secondary" type="button" id="mirrorMainBagDrawerRefreshBtn">
            Refresh Bag
          </button>
        </footer>
      </aside>
    </section>

    <section class="mirror-main-showroom-overlay" id="mirrorMainShowroomOverlay" aria-label="Showroom mode">
      <div class="mirror-main-showroom-content">
        <div>
          <h2>${storeName}</h2>
          <p>Tap anywhere to return</p>
        </div>
      </div>
    </section>
    `;

  document.body.appendChild(main);
  bindMirrorMainExperienceEvents(main);
  updateMirrorMainProductCard();
  updateMirrorMainBagCount();
  renderMirrorMainRecentScans();
  renderMirrorMainTimeline();

  return main;
}

function getMirrorMainBagItemCategory(item) {
  return (
    item.category ||
    item.product?.category ||
    item.inventoryItem?.category ||
    item.productType ||
    item.type ||
    "Item"
  );
}

function getMirrorMainBagItemPrice(item) {
  const price =
    item.price ||
    item.product?.price ||
    item.inventoryItem?.price ||
    item.salePrice ||
    item.retailPrice ||
    0;

  return safeNumber(price, 0);
}

function getMirrorMainBagAnalytics(items = []) {
  const safeItems = Array.isArray(items) ? items : [];

  const totalValue = safeItems.reduce((sum, item) => {
    return sum + getMirrorMainBagItemPrice(item);
  }, 0);

  const categories = [
    ...new Set(
      safeItems
        .map(getMirrorMainBagItemCategory)
        .map(category => String(category || "").trim())
        .filter(Boolean)
    )
  ];

  const topCategories = categories.slice(0, 3);

  return {
    itemCount: safeItems.length,
    totalValue,
    categoryCount: categories.length,
    categories,
    topCategories
  };
}

function buildMirrorMainBagIntentNote(items = []) {
  const analytics = getMirrorMainBagAnalytics(items);
  const storeName = getMirrorShowroomDisplayStore();

  if (!analytics.itemCount) {
    return "Pixel will summarize customer intent once saved items appear.";
  }

  if (analytics.itemCount === 1) {
    const item = items[0];
    const name = getMirrorMainBagItemName(item);
    const category = getMirrorMainBagItemCategory(item);

    return `Pixel sees early interest in ${category.toLowerCase()} through ${name}. Save more pieces to build a stronger customer intent profile.`;
  }

  const categoryText = analytics.topCategories.length
    ? analytics.topCategories.join(", ")
    : "multiple categories";

  if (analytics.categoryCount >= 3) {
    return `Pixel sees a complete styling journey forming at ${storeName}: ${analytics.itemCount} saved pieces across ${analytics.categoryCount} categories, with interest around ${categoryText}.`;
  }

  return `Pixel sees focused customer intent at ${storeName}: ${analytics.itemCount} saved pieces around ${categoryText}, estimated at ${formatPrice(analytics.totalValue)}.`;
}

function renderMirrorMainBagIntentSummary(items = []) {
  const analytics = getMirrorMainBagAnalytics(items);

  const count = document.getElementById("mirrorMainBagDrawerCount");
  const value = document.getElementById("mirrorMainBagDrawerValue");
  const categories = document.getElementById("mirrorMainBagDrawerCategories");
  const store = document.getElementById("mirrorMainBagDrawerStore");
  const session = document.getElementById("mirrorMainBagDrawerSession");
  const note = document.getElementById("mirrorMainBagIntentNote");

  if (count) {
    count.textContent = String(analytics.itemCount);
  }

  if (value) {
    value.textContent = formatPrice(analytics.totalValue);
  }

  if (categories) {
    categories.textContent = String(analytics.categoryCount);
  }

  if (store) {
    store.textContent = getMirrorShowroomDisplayStore();
  }

  if (session) {
    session.textContent = getToken() ? "Active" : "Login Needed";
  }

  if (note) {
    note.textContent = buildMirrorMainBagIntentNote(items);
  }

  return analytics;
}

function buildMirrorMainOutfitShareText(fullOutfit = currentMirrorMainFullOutfit) {
  const storeName =
    typeof getMirrorShowroomDisplayStore === "function"
      ? getMirrorShowroomDisplayStore()
      : "Universal Stylist";

  if (!fullOutfit || typeof fullOutfit !== "object") {
    return `${storeName} Smart Mirror\n\nNo complete outfit is currently available to share.`;
  }

  const pieces = [
    ["Anchor", fullOutfit.top],
    ["Bottom", fullOutfit.bottom],
    ["Shoes", fullOutfit.shoes],
    ["Layer", fullOutfit.outerwear]
  ]
    .filter(([, item]) => !!item)
    .map(([role, item]) => {
      const name =
        getItemField(item, "name", "itemName", "productName", "title") ||
        role;

      const brand =
        getItemField(item, "brand", "retailer", "retailerName") ||
        storeName;

      const price = getItemField(item, "price");
      const priceText = Number.isFinite(Number(price))
        ? ` — ${formatPrice(price)}`
        : "";

      return `${role}: ${name} by ${brand}${priceText}`;
    });

  const score =
    typeof getAdvancedOutfitScores === "function"
      ? getAdvancedOutfitScores(fullOutfit)?.overallScore
      : safeNumber(fullOutfit.overallScore);

  const total =
    typeof getOutfitTotalPrice === "function"
      ? getOutfitTotalPrice(fullOutfit)
      : 0;

  return [
    `${storeName} Smart Mirror`,
    "Universal Stylist Complete Look",
    "",
    "Pixel styled this complete look:",
    "",
    ...pieces,
    "",
    score ? `Outfit score: ${score}%` : "",
    total ? `Estimated total: ${formatPrice(total)}` : "",
    "",
    fullOutfit.explanation ||
      "This look was assembled from live store inventory and real product imagery, balanced by color, category, price, and availability."
  ]
    .filter(line => line !== "")
    .join("\n");
}

async function shareMirrorMainOutfit() {
  const fullOutfit = currentMirrorMainFullOutfit;
  const title = "Universal Stylist Complete Look";
  const text = buildMirrorMainOutfitShareText(fullOutfit);

  if (!fullOutfit) {
    showToast?.("Create a complete look before sharing.", "error");
    setStatus?.("Create a complete look before sharing.", "error");
    return false;
  }

  try {
    if (navigator.share) {
      try {
        await navigator.clipboard?.writeText?.(text);
      } catch (_) {
        // Clipboard is optional. Native share can still continue.
      }

      await navigator.share({
        title,
        text
      });

      showToast?.("Look shared. Summary also copied.", "success");
      setStatus?.("Look shared successfully.", "success");
      return true;
    }

    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);

      showToast?.("Look copied to clipboard.", "success");
      setStatus?.("Look copied to clipboard.", "success");
      return true;
    }

    const textarea = document.createElement("textarea");
    textarea.value = text;
    textarea.setAttribute("readonly", "true");
    textarea.style.position = "fixed";
    textarea.style.left = "-9999px";
    textarea.style.top = "0";

    document.body.appendChild(textarea);
    textarea.focus();
    textarea.select();

    const copied = document.execCommand("copy");
    textarea.remove();

    if (!copied) {
      throw new Error("Sharing and clipboard copy are not available.");
    }

    showToast?.("Look copied to clipboard.", "success");
    setStatus?.("Look copied to clipboard.", "success");
    return true;
  } catch (error) {
    if (error?.name === "AbortError") {
      showToast?.("Share cancelled.", "info");
      setStatus?.("Share cancelled.", "ready");
      return false;
    }

    console.error("Share outfit failed:", error);

    try {
      await navigator.clipboard?.writeText?.(text);

      showToast?.("Unable to open share sheet. Summary copied instead.", "info");
      setStatus?.("Share sheet unavailable. Outfit summary copied.", "ready");
      return true;
    } catch (_) {
      showToast?.("Unable to share this look.", "error");
      setStatus?.("Unable to share this look.", "error");
      return false;
    }
  }
}

function getMirrorMainOutfitBagPieces(fullOutfit = currentMirrorMainFullOutfit) {
  if (!fullOutfit || typeof fullOutfit !== "object") {
    return [];
  }

  const seen = new Set();

  return getMirrorMainOutfitPieces(fullOutfit)
    .map(([role, item]) => {
      const rfid = getItemField(item, "rfid", "itemRfid", "productRfid", "id");
      const name = getItemField(item, "name", "itemName", "productName", "title") || role;

      return {
        role,
        item,
        rfid: String(rfid || "").trim(),
        name
      };
    })
    .filter(piece => {
      if (!piece.rfid) return false;

      const key = piece.rfid.toLowerCase();

      if (seen.has(key)) {
        return false;
      }

      seen.add(key);
      return true;
    });
}

async function saveMirrorMainOutfitPiecesToBag(fullOutfit = currentMirrorMainFullOutfit) {
  const pieces = getMirrorMainOutfitBagPieces(fullOutfit);

  if (!pieces.length) {
    throw new Error("This complete look has no RFID-backed pieces to save.");
  }

  requireToken();

  console.table(
    pieces.map(piece => ({
      role: piece.role,
      name: piece.name,
      rfid: piece.rfid,
      alreadySaved: savedRfids.has(piece.rfid)
    }))
  );

  const savedPieces = [];
  const skippedPieces = [];
  const failedPieces = [];

  for (const piece of pieces) {
    if (savedRfids.has(piece.rfid)) {
      skippedPieces.push(piece);
      continue;
    }

    try {
      const response = await fetch(`${API.stylist}/save/${encodeURIComponent(piece.rfid)}`, {
        method: "POST",
        headers: getAuthHeaders()
      });

      await assertAuthorizedResponse(response, `Unable to save ${piece.name}.`);

      savedRfids.add(piece.rfid);
      savedPieces.push(piece);
    } catch (error) {
      console.error(`Failed to save outfit piece: ${piece.name}`, {
        piece,
        error
      });

      failedPieces.push({
        ...piece,
        errorMessage: error.message || "Unable to save this piece."
      });
    }
  }

  if (failedPieces.length && !savedPieces.length && !skippedPieces.length) {
    throw new Error("Pixel could not save any pieces from this complete look.");
  }

  return {
    pieces,
    savedPieces,
    skippedPieces,
    failedPieces,
    totalCount: pieces.length
  };
}

async function saveMirrorMainLookIntent() {
  const fullOutfit = currentMirrorMainFullOutfit;

  if (!fullOutfit) {
    if (typeof saveToBag === "function" && currentLoadedItem) {
      await saveToBag();
      return true;
    }

    setStatus?.("Create a complete look before saving to bag.", "error");
    showToast?.("Create a complete look first.", "error");
    return false;
  }

  const saveButton = document.getElementById("mirrorMainSaveLookBtn");

  try {
    if (saveButton) {
      saveButton.disabled = true;
      saveButton.textContent = "Saving Look...";
      saveButton.classList.remove("is-saved");
    }

    const storeName =
      typeof getMirrorShowroomDisplayStore === "function"
        ? getMirrorShowroomDisplayStore()
        : "Current Store";

    const result = await saveMirrorMainOutfitPiecesToBag(fullOutfit);
    const lookSummary = buildMirrorMainOutfitShareText(fullOutfit);
    const savedLooks = safeParseJson(localStorage.getItem("pixelMirrorSavedLooks")) || [];

    const savedLook = {
      id: crypto.randomUUID(),
      savedAt: new Date().toISOString(),
      storeName,
      retailerKey: getSelectedRetailerKey(),
      storeCode: getSelectedStoreCode(),
      vibe: document.getElementById("vibeSelect")?.value || "Casual",
      lookSignature: getMirrorMainCurrentLookSignature(fullOutfit),
      anchorRfid: currentRfid || "",
      anchorName:
        getItemField(currentLoadedItem, "name", "itemName", "productName", "title") ||
        getItemField(lastScannedItem, "name", "itemName", "productName", "title") ||
        "Scanned Item",
      summary: lookSummary,
      savedPieceRfids: result.pieces.map(piece => piece.rfid),
      fullOutfit
    };

    localStorage.setItem(
      "pixelMirrorSavedLooks",
      JSON.stringify([savedLook, ...savedLooks].slice(0, 30))
    );

    if (typeof addTryOnTimelineEvent === "function") {
      addTryOnTimelineEvent(
        "save",
        "Saved complete look to bag",
        `${result.totalCount} outfit piece${result.totalCount === 1 ? "" : "s"} were saved to the shopper bag.`,
        [
          storeName,
          savedLook.vibe,
          savedLook.anchorRfid ? `RFID ${savedLook.anchorRfid}` : ""
        ]
      );
    }

    if (window.MirrorCustomerJourney?.saved) {
      window.MirrorCustomerJourney.saved(currentLoadedItem || lastScannedItem);
    }

    setMirrorMainLookSaveButtonState(true);
    updateMirrorMainBagCount?.();
    updateMirrorMainProductCard?.();

    await loadBag(false);

    if (
      document.getElementById("mirrorMainBagDrawer")?.classList.contains("is-active") &&
      typeof renderMirrorMainBagDrawer === "function"
    ) {
      await renderMirrorMainBagDrawer();
    }

    const savedCount = result.savedPieces.length;
    const skippedCount = result.skippedPieces.length;

    const failedCount = result.failedPieces.length;

    const message =
      savedCount > 0 && failedCount === 0
        ? `Complete look saved. ${savedCount} outfit piece${savedCount === 1 ? "" : "s"} added to bag.`
        : savedCount > 0 && failedCount > 0
          ? `Partial look saved. ${savedCount} piece${savedCount === 1 ? "" : "s"} added, ${failedCount} could not be saved.`
          : skippedCount > 0 && failedCount === 0
            ? "Complete look was already in the bag."
            : "Complete look save finished with warnings.";

    setStatus?.(message, "success");
    showToast?.(message, "success");

    speakPixelConcierge?.("save", {
      itemName: "Complete look",
      bagCount: savedRfids.size
    });

    return true;
  } catch (error) {
    console.error("Save complete look failed:", error);

    const message = error.message || "Unable to save this complete look.";

    setMirrorMainLookSaveButtonState(false);
    setStatus?.(message, "error");
    showToast?.(message, "error");

    speakPixelConcierge?.("saveError", {
      errorMessage: message
    });

    return false;
  }
}

function showMirrorMainOutfitError(message = "Unable to create this complete look.") {
  const showcase = document.getElementById("mirrorMainOutfitShowcase");
  const grid = document.getElementById("mirrorMainOutfitGrid");
  const title = document.getElementById("mirrorMainOutfitTitle");
  const score = document.getElementById("mirrorMainOutfitScore");
  const explain = document.getElementById("mirrorMainOutfitExplain");
  const saveButton = document.getElementById("mirrorMainSaveLookBtn");
  const anotherButton = document.getElementById("mirrorMainOutfitAnotherBtn");

  if (!showcase || !grid || !title || !score || !explain) {
    return false;
  }

  const cleanMessage =
    message ||
    "Pixel could not finish this complete look. Try scanning again or choose another item.";

  currentMirrorMainFullOutfit = null;

  showcase.classList.add("mirror-main-look-board", "is-active");
  showcase.classList.remove("is-building");
  showcase.setAttribute("aria-hidden", "false");

  document.body.classList.add("mirror-main-outfit-active");

  title.textContent = "Look Needs Attention";
  score.textContent = "!";
  explain.textContent = cleanMessage;

  grid.innerHTML = `
    <div class="mirror-main-outfit-empty">
      <strong>Pixel could not finish this complete look.</strong>
      <span>${escapeHtml(cleanMessage)}</span>
    </div>
  `;

  if (saveButton) {
    saveButton.textContent = "Try Again";
    saveButton.disabled = false;
    saveButton.classList.remove("is-saved");
    saveButton.onclick = () => {
      hideMirrorMainOutfitShowcase();

      if (typeof showMirrorMainOutfitLoading === "function") {
        showMirrorMainOutfitLoading(currentLoadedItem || lastScannedItem);
      }

      createFullLook();
    };
  }

  if (anotherButton) {
    anotherButton.textContent = "Scan Another Item";
  }

  return true;
}

function getMirrorMainCurrentLookSignature(fullOutfit = currentMirrorMainFullOutfit) {
  if (!fullOutfit || typeof fullOutfit !== "object") {
    return "";
  }

  return [
    fullOutfit.top,
    fullOutfit.bottom,
    fullOutfit.shoes,
    fullOutfit.outerwear
  ]
    .filter(Boolean)
    .map(item => {
      return (
        getItemField(item, "rfid", "itemRfid", "productRfid", "id") ||
        getItemField(item, "name", "itemName", "productName", "title") ||
        ""
      );
    })
    .filter(Boolean)
    .join("|")
    .toLowerCase();
}

function isMirrorMainCurrentLookSaved(fullOutfit = currentMirrorMainFullOutfit) {
  const signature = getMirrorMainCurrentLookSignature(fullOutfit);

  if (!signature) {
    return false;
  }

  const savedLooks = safeParseJson(localStorage.getItem("pixelMirrorSavedLooks")) || [];

  return savedLooks.some(savedLook => {
    return savedLook.lookSignature === signature;
  });
}

function setMirrorMainLookSaveButtonState(isSaved) {
  const saveButton = document.getElementById("mirrorMainSaveLookBtn");

  if (!saveButton) {
    return;
  }

  if (isSaved) {
    saveButton.textContent = "Complete Look Saved ✓";
    saveButton.disabled = true;
    saveButton.classList.add("is-saved");
    return;
  }

  saveButton.textContent = "Save Complete Look";
  saveButton.disabled = false;
  saveButton.classList.remove("is-saved");
}

function resetMirrorMainForAnotherScan() {
  currentRfid = "";
  currentLoadedItem = null;
  lastScannedItem = null;
  currentMirrorMainFullOutfit = null;

  const mainInput = document.getElementById("mirrorMainRfidInput");
  const hiddenInput = document.getElementById("rfidInput");

  if (mainInput) {
    mainInput.value = "";
  }

  if (hiddenInput) {
    hiddenInput.value = "";
  }

  hideMirrorMainOutfitShowcase();
  setMirrorMainProductVisible(false);
  updateMirrorMainProductCard();
  setMirrorMainScanningState(true);
  setMirrorMainLookSaveButtonState(false);

  if (window.MirrorCustomerJourney?.scan) {
    window.MirrorCustomerJourney.scan();
  }

  setStatus?.("Ready for the next RFID scan.", "ready");
  showToast?.("Ready for another scan.", "info");
  speakPixelConcierge?.("scanBlocked");

  window.setTimeout(() => {
    focusMirrorScanInput();
  }, 120);

  return true;
}

function bindMirrorMainExperienceEvents(main) {
  main.querySelector("#mirrorMainBackToLockBtn")?.addEventListener("click", () => {
    if (typeof toggleMirrorAssociateControl === "function") {
      toggleMirrorAssociateControl();
      return;
    }

    if (typeof showMirrorAssociateControl === "function") {
      showMirrorAssociateControl();
    }
  });

  main.querySelector("#mirrorMainExitBtn")?.addEventListener("click", () => {
    if (typeof toggleMirrorAssociateControl === "function") {
      toggleMirrorAssociateControl();
      return;
    }

    if (typeof showMirrorAssociateControl === "function") {
      showMirrorAssociateControl();
    }
  });

  main.querySelector("#mirrorMainScanRingBtn")?.addEventListener("click", handleMirrorMainStartScan);
  main.querySelector("#mirrorMainStartScanBtn")?.addEventListener("click", handleMirrorMainStartScan);

  function runMirrorMainRfidScan() {
    const mainInput = document.getElementById("mirrorMainRfidInput");
    const hiddenInput = document.getElementById("rfidInput");

    const rfid = normalizeMirrorRfid(mainInput?.value || "");

    if (mainInput) {
      mainInput.value = rfid;
    }

    if (!rfid) {
      handleMirrorMainStartScan();

      if (typeof setStatus === "function") {
        setStatus("Enter or scan an RFID tag first.", "error");
      }

      if (typeof showToast === "function") {
        showToast("Enter or scan an RFID tag first.", "error");
      }

      if (typeof speakPixelConcierge === "function") {
        speakPixelConcierge("scanBlocked");
      }

      focusMirrorScanInput();
      return;
    }

    if (hiddenInput) {
      hiddenInput.value = rfid;
    }

    if (typeof setMirrorMainScanningState === "function") {
      setMirrorMainScanningState(true);
    }

    if (typeof handleScan === "function") {
      handleScan();
    }
  }

  main.querySelector("#mirrorMainRunScanBtn")?.addEventListener("click", runMirrorMainRfidScan);

  main.querySelector("#mirrorMainRfidInput")?.addEventListener("keydown", event => {
    if (event.key === "Enter") {
      event.preventDefault();
      runMirrorMainRfidScan();
    }
  });

  main.querySelector("#mirrorMainResetProductBtn")?.addEventListener("click", () => {
    currentLoadedItem = null;
    lastScannedItem = null;
    currentRfid = "";
    currentMirrorMainFullOutfit = null;

    document.body.classList.remove("mirror-main-demo-product-active");

    setMirrorMainProductVisible(false);
    updateMirrorMainProductCard();
    setMirrorMainLookSaveButtonState(false);

    if (typeof setStatus === "function") {
      setStatus("Customer main product cleared.", "ready");
    }
  });

  main.querySelector("#mirrorMainCreateLookBtn")?.addEventListener("click", () => {
    const activeItem = currentLoadedItem || lastScannedItem || null;

    const activeRfid =
      currentRfid ||
      getItemField(activeItem, "rfid", "itemRfid", "productRfid", "id") ||
      "";

    if (!activeItem || !activeRfid) {
      handleMirrorMainStartScan();

      if (typeof setStatus === "function") {
        setStatus("Scan a real inventory item before creating a full outfit.", "error");
      }

      if (typeof showToast === "function") {
        showToast("Scan a real inventory item first.", "error");
      }

      return;
    }

    if (String(activeRfid).toUpperCase().startsWith("DEMO-")) {
      handleMirrorMainStartScan();

      if (typeof setStatus === "function") {
        setStatus("Demo product ignored. Scan a real inventory item to create a backend look.", "error");
      }

      if (typeof showToast === "function") {
        showToast("Demo item ignored. Scan a real product.", "error");
      }

      return;
    }

    if (window.MirrorCustomerJourney?.outfit) {
      window.MirrorCustomerJourney.outfit(activeItem);
    } else if (typeof setMirrorCustomerStage === "function") {
      setMirrorCustomerStage(MIRROR_CUSTOMER_STAGES.OUTFIT, {
        item: activeItem
      });
    }

    if (typeof resetAmbientIdleTimer === "function") {
      resetAmbientIdleTimer();
    }

    if (typeof showMirrorMainOutfitLoading === "function") {
      showMirrorMainOutfitLoading(activeItem);
    }

    if (typeof createFullLook === "function") {
      createFullLook();
    }
  });

  main.querySelector("#mirrorMainAddToLookBtn")?.addEventListener("click", async () => {
    if (!currentLoadedItem || !currentRfid) {
      setStatus?.("Scan a real item first before saving it to the bag.", "error");
      showToast?.("Scan a real item first.", "error");
      speakPixelConcierge?.("saveBlocked");
      focusMirrorScanInput?.();
      return;
    }

    if (typeof saveToBag === "function") {
      await saveToBag();
    }

    updateMirrorMainBagCount?.();
    updateMirrorMainProductCard?.();
  });

  main.querySelector("#mirrorMainOutfitCloseBtn")?.addEventListener("click", () => {
    hideMirrorMainOutfitShowcase();
  });

  main.querySelector("#mirrorMainOutfitAnotherBtn")?.addEventListener("click", () => {
    resetMirrorMainForAnotherScan();
  });

  main.querySelector("#mirrorMainSaveLookBtn")?.addEventListener("click", () => {
    saveMirrorMainLookIntent();
  });

  main.querySelector("#mirrorMainCheckoutBtn")?.addEventListener("click", () => {
    requestMirrorMainCheckoutIntent();
  });

  main.querySelector("#mirrorMainSeeMoreBtn")?.addEventListener("click", () => {
    hideMirrorMainExperience();
  });

  main.querySelector("#mirrorMainBagBtn")?.addEventListener("click", () => {
    openMirrorMainBagDrawer();
  });

  main.querySelector("#mirrorMainProfileBtn")?.addEventListener("click", () => {
    openMirrorMainProfileDrawer();
  });

  main.querySelector("#mirrorMainProfileDim")?.addEventListener("click", closeMirrorMainProfileDrawer);
  main.querySelector("#mirrorMainProfileCloseBtn")?.addEventListener("click", closeMirrorMainProfileDrawer);

  main.querySelector("#mirrorMainBagDim")?.addEventListener("click", closeMirrorMainBagDrawer);
  main.querySelector("#mirrorMainBagCloseBtn")?.addEventListener("click", closeMirrorMainBagDrawer);
  main.querySelector("#mirrorMainBagDrawerRefreshBtn")?.addEventListener("click", renderMirrorMainBagDrawer);

  main.querySelector("#mirrorMainBagDrawerScanBtn")?.addEventListener("click", () => {
    closeMirrorMainBagDrawer();
    resetMirrorMainForAnotherScan();
  });

  main.querySelector("#mirrorMainShowroomBtn")?.addEventListener("click", () => {
    openMirrorMainShowroomOverlay();
  });

  main.querySelector("#mirrorMainShowroomOverlay")?.addEventListener("click", () => {
    closeMirrorMainShowroomOverlay();
  });

  main.querySelector("#mirrorMainPixelCloseBtn")?.addEventListener("click", () => {
    const right = main.querySelector(".mirror-main-right");

    if (right) {
      right.style.display = "none";
    }
  });

  main.querySelector("#mirrorMainPixelOpenBtn")?.addEventListener("click", () => {
    const right = main.querySelector(".mirror-main-right");

    if (right) {
      right.style.display = "";
    }
  });

  main.addEventListener("click", event => {
    const checkoutButton = event.target?.closest?.("#mirrorMainCheckoutBtn");

    if (!checkoutButton) {
      return;
    }

    event.preventDefault();
    requestMirrorMainCheckoutIntent();
  });
}

function forceMirrorMainPageVisible() {
  document.documentElement.classList.remove("mirror-main-booting");
  document.documentElement.classList.add("mirror-main-ready");

  document.body.classList.add("mirror-page-visible");

  const mirrorPage = document.querySelector(".mirror-page");
  if (mirrorPage) {
    mirrorPage.style.opacity = "1";
    mirrorPage.style.visibility = "visible";
    mirrorPage.style.display = "block";
  }

  const main = document.getElementById("mirrorMainExperience");
  if (main) {
    main.style.opacity = "1";
    main.style.visibility = "visible";
  }
}

function showMirrorMainExperience() {
  forceMirrorMainPageVisible();

  let main = document.getElementById("mirrorMainExperience");

  try {
    main = buildMirrorMainExperience();
  } catch (error) {
    console.error("Failed to build Mirror Main Experience:", error);
    window.PixelMirrorBootError = error;
    forceMirrorMainPageVisible();
    return false;
  }

  try {
    if (typeof buildMirrorAssociateControl === "function") {
      buildMirrorAssociateControl();
    }
  } catch (error) {
    console.warn("Associate control failed to build, continuing mirror boot:", error);
  }

  try {
    if (typeof refreshMirrorRuntime === "function") {
      const runtime = refreshMirrorRuntime();

      if (runtime && typeof applyMirrorRuntimeToExperience === "function") {
        applyMirrorRuntimeToExperience(runtime);
      }
    }
  } catch (error) {
    console.warn("Mirror runtime failed to refresh, continuing mirror boot:", error);
  }

  try {
    if (typeof hideMirrorWelcomeScreen === "function") {
      hideMirrorWelcomeScreen(false);
    }

    if (typeof hideMirrorLockScreen === "function") {
      hideMirrorLockScreen(false);
    }
  } catch (error) {
    console.warn("Welcome/lock screen hide failed, continuing mirror boot:", error);
  }

  main.classList.remove("is-dismissing");
  main.classList.add("is-active");
  main.setAttribute("aria-hidden", "false");
  main.dataset.customerStage = currentMirrorCustomerStage || "landing";

  document.body.classList.add("mirror-main-active");
  forceMirrorMainPageVisible();

  try {
    updateMirrorMainProductCard();
    updateMirrorMainBagCount();
    renderMirrorMainRecentScans();
    renderMirrorMainTimeline();

    if (!currentLoadedItem && !document.body.classList.contains("mirror-main-demo-product-active")) {
      setMirrorMainProductVisible(false);
    }
  } catch (error) {
    console.warn("Mirror product/card update failed, continuing mirror boot:", error);
  }

  try {
    if (window.MirrorCustomerJourney?.sync) {
      window.MirrorCustomerJourney.sync();
    } else if (typeof syncMirrorCustomerJourney === "function") {
      syncMirrorCustomerJourney();
    }
  } catch (error) {
    console.warn("Customer journey sync failed, continuing mirror boot:", error);
  }

  try {
    if (typeof resetAmbientIdleTimer === "function") {
      resetAmbientIdleTimer();
    }
  } catch (error) {
    console.warn("Ambient idle timer failed, continuing mirror boot:", error);
  }

  try {
    if (typeof speakPixelConcierge === "function") {
      window.setTimeout(() => {
        speakPixelConcierge("ready");
      }, 400);
    }
  } catch (error) {
    console.warn("Pixel concierge failed, continuing mirror boot:", error);
  }

  return true;
}

function hideMirrorMainExperience(animate = true) {
  hideMirrorAssociateControl();

  const main = document.getElementById("mirrorMainExperience");
  if (!main) {
    return false;
  }

  closeMirrorMainProfileDrawer();
  closeMirrorMainShowroomOverlay();

  if (!animate) {
    main.classList.remove("is-active");
    main.classList.remove("is-dismissing");
    main.setAttribute("aria-hidden", "true");
    document.body.classList.remove("mirror-main-active");
    return true;
  }

  if (main.classList.contains("is-dismissing")) {
    return false;
  }

  main.classList.add("is-dismissing");

  window.setTimeout(() => {
    main.classList.remove("is-active");
    main.classList.remove("is-dismissing");
    main.setAttribute("aria-hidden", "true");
    document.body.classList.remove("mirror-main-active");

    if (typeof resetAmbientIdleTimer === "function") {
      resetAmbientIdleTimer();
    }
  }, 440);

  return true;
}

function setMirrorMainProductVisible(isVisible) {
  const idle = document.getElementById("mirrorMainScanIdle");
  const card = document.getElementById("mirrorMainProductCard");

  if (idle) {
    idle.style.display = isVisible ? "none" : "";
  }

  if (card) {
    card.classList.toggle("is-visible", !!isVisible);
  }
}

function updateMirrorMainProductCard() {
  const main = document.getElementById("mirrorMainExperience");

  if (!main) {
    return;
  }

  const product = getMirrorMainProduct();

  const image = document.getElementById("mirrorMainProductImage");
  const store = document.getElementById("mirrorMainProductStore");
  const name = document.getElementById("mirrorMainProductName");
  const price = document.getElementById("mirrorMainProductPrice");
  const meta = document.getElementById("mirrorMainProductMeta");

  if (image) {
    image.src = product.imageUrl;
    image.alt = product.name;
  }

  if (store) {
    store.textContent = product.store;
  }

  if (name) {
    name.textContent = product.name;
  }

  if (price) {
    price.textContent = product.price;
  }

  if (meta) {
    meta.textContent = product.meta;
  }

  const shouldShowProduct =
    !!currentLoadedItem ||
    document.body.classList.contains("mirror-main-demo-product-active");

  setMirrorMainProductVisible(shouldShowProduct);

  if (!shouldShowProduct || !currentLoadedItem) {
    setSaveButtonDefault(true);
  } else if (isCurrentItemSaved(currentLoadedItem)) {
    setSaveButtonSaved();
  } else {
    setSaveButtonDefault(false);
  }

  updateMirrorMainPixelMessages();
}

function updateMirrorMainPixelMessages() {
  const msgOne = document.getElementById("mirrorMainPixelMsgOne");
  const msgTwo = document.getElementById("mirrorMainPixelMsgTwo");
  const msgThree = document.getElementById("mirrorMainPixelMsgThree");

  if (!msgOne || !msgTwo || !msgThree) {
    return;
  }

  if (!currentLoadedItem && !document.body.classList.contains("mirror-main-demo-product-active")) {
    msgOne.textContent = "“Scan a tagged item and I’ll build a complete look from this store.”";
    msgTwo.textContent = "“I’ll check color, category, budget, profile signals, and availability.”";
    msgThree.textContent = "“Your recommendations will update as soon as an item appears.”";
    return;
  }

  const product = getMirrorMainProduct();
  const productName = product.name || "This item";
  const storeName = getMirrorShowroomDisplayStore();

  msgOne.textContent = `“${productName} is ready.”`;
  msgTwo.textContent = `“I’m curating ${storeName} pieces that complement the color, category, and styling direction.”`;
  msgThree.textContent = "“Create a full outfit when you’re ready, and I’ll complete the look.”";
}

function showMirrorMainDemoProduct() {
  handleMirrorMainStartScan();

  if (typeof setStatus === "function") {
    setStatus("Demo products are disabled. Scan or enter a real RFID item.", "ready");
  }

  if (typeof showToast === "function") {
    showToast("Demo products are disabled. Use a real RFID.", "info");
  }

  if (typeof speakPixelConcierge === "function") {
    speakPixelConcierge("scanBlocked");
  }

  return false;
}

function updateMirrorMainBagCount() {
  const count = document.getElementById("mirrorMainBagCount");

  if (count) {
    count.textContent = String(getMirrorMainBagCount());
  }
}
function getMirrorMainRecentScans() {
  const session = typeof readTryOnSession === "function" ? readTryOnSession() : null;
  const scans = Array.isArray(session?.scans) ? session.scans : [];

  return scans.slice(0, 3);
}

function renderMirrorMainRecentScans() {
  const container = document.getElementById("mirrorMainRecentScans");

  if (!container) {
    return;
  }

  const scans = getMirrorMainRecentScans();

  if (!scans.length) {
    container.innerHTML = `
      <span class="mirror-main-recent-label">Recent Scans</span>
    `;
    return;
  }

  container.innerHTML = `
    ${scans.slice(0, 2).map(scan => {
      const name = scan.name || "Recent scan";
      const imageUrl = safeImageUrl(
        scan.imageUrl,
        `https://placehold.co/160x160/f7f3ec/171411?text=${encodeURIComponent(name)}`
      );

      return `
        <div class="mirror-main-recent-thumb">
          <img
            src="${escapeMirrorShowroomHtml(imageUrl)}"
            alt="${escapeMirrorShowroomHtml(name)}"
            onerror="this.src='https://placehold.co/160x160/f7f3ec/171411?text=Scan';"
          >
        </div>
      `;
    }).join("")}

    <span class="mirror-main-recent-label">Recent Scans</span>
  `;
}

function renderMirrorMainTimeline() {
  const row = document.getElementById("mirrorMainTimelineRow");

  if (!row) {
    return;
  }

  const session = typeof readTryOnSession === "function" ? readTryOnSession() : null;
  const scans = Array.isArray(session?.scans) ? session.scans : [];

  if (!scans.length) {
    row.innerHTML = `
      <article class="mirror-main-timeline-item">
        <div>
          <p class="mirror-main-timeline-title">Ready for first scan</p>
          <p class="mirror-main-timeline-sub">Real inventory appears here</p>
        </div>
      </article>
    `;
    return;
  }

  row.innerHTML = scans.slice(0, 5).map(scan => {
    const name = scan.name || "Scanned item";
    const imageUrl = safeImageUrl(
      scan.imageUrl,
      `https://placehold.co/160x160/f7f3ec/171411?text=${encodeURIComponent(name)}`
    );

    return `
      <article class="mirror-main-timeline-item">
        <div class="mirror-main-timeline-thumb">
          <img
            src="${escapeMirrorShowroomHtml(imageUrl)}"
            alt="${escapeMirrorShowroomHtml(name)}"
            onerror="this.src='https://placehold.co/160x160/f7f3ec/171411?text=Scan';"
          >
        </div>

        <div>
          <p class="mirror-main-timeline-title">${escapeMirrorShowroomHtml(name)}</p>
          <p class="mirror-main-timeline-sub">
            Scanned · ${escapeMirrorShowroomHtml(scan.vibe || "Casual")}
          </p>
        </div>
      </article>
    `;
  }).join("");
}

function setMirrorMainScanningState(isScanning) {
  const main = document.getElementById("mirrorMainExperience");
  const scanCard = document.querySelector(".mirror-main-scan-card");
  const scanRing = document.getElementById("mirrorMainScanRingBtn");
  const scanCopy = document.querySelector(".mirror-main-scan-copy");
  const startButton = document.getElementById("mirrorMainStartScanBtn");

  if (main) {
    main.classList.toggle("is-scanning", !!isScanning);
    main.dataset.customerStage = isScanning
      ? "scanning"
      : currentMirrorCustomerStage || "landing";
  }

  if (scanCard) {
    scanCard.classList.remove("is-reading");
  }

  if (scanRing) {
    scanRing.setAttribute("aria-busy", String(!!isScanning));
    scanRing.innerHTML = `<span class="mirror-main-zap">✦</span>`;
  }

  if (scanCopy) {
    scanCopy.textContent = isScanning
      ? "Scan or enter an RFID tag to reveal the product."
      : "Hold any tagged item near the reader to instantly reveal details.";
  }

  if (startButton) {
    startButton.classList.remove("is-reading");
    startButton.textContent = isScanning ? "Scan Ready" : "✦ Start Scanning";
  }
}

function getMirrorMainOutfitPieceLabel(role) {
  const labels = {
    top: "Anchor",
    bottom: "Bottom",
    shoes: "Shoes",
    outerwear: "Layer"
  };

  return labels[String(role || "").toLowerCase()] || "Piece";
}

function getMirrorMainOutfitPieces(fullOutfit) {
  if (!fullOutfit || typeof fullOutfit !== "object") {
    return [];
  }

  return [
    ["top", fullOutfit.top],
    ["bottom", fullOutfit.bottom],
    ["shoes", fullOutfit.shoes],
    ["outerwear", fullOutfit.outerwear]
  ].filter(([, item]) => !!item);
}

function renderMirrorMainOutfitCard(role, item) {
  const name =
    getItemField(item, "name", "itemName", "productName", "title") ||
    getMirrorMainOutfitPieceLabel(role);

  const brand =
    getItemField(item, "brand", "retailer", "retailerName") ||
    getMirrorShowroomDisplayStore();

  const color = getItemField(item, "color") || "";
  const category = getItemField(item, "category") || "";
  const price = formatPrice(getItemField(item, "price"));
  const imageUrl = safeImageUrl(
    getItemField(item, "imageUrl", "image_url", "image", "photoUrl", "productImageUrl"),
    "https://placehold.co/400x500/f7f3ec/171411?text=Look"
  );

  return `
    <article class="mirror-main-outfit-piece">
      <div class="mirror-main-outfit-image">
        <img
          src="${escapeMirrorShowroomHtml(imageUrl)}"
          alt="${escapeMirrorShowroomHtml(name)}"
          onerror="this.src='https://placehold.co/400x500/f7f3ec/171411?text=Look';"
        >
      </div>

      <div class="mirror-main-outfit-body">
        <p>${escapeMirrorShowroomHtml(getMirrorMainOutfitPieceLabel(role))}</p>
        <strong>${escapeMirrorShowroomHtml(name)}</strong>
        <span>
          ${escapeMirrorShowroomHtml([brand, category, color].filter(Boolean).join(" · "))} · ${escapeMirrorShowroomHtml(price)}
        </span>
      </div>
    </article>
  `;
}

function showMirrorMainOutfitShowcase() {
  const showcase = document.getElementById("mirrorMainOutfitShowcase");
  if (!showcase) return false;

  showcase.classList.remove("is-building");
  showcase.classList.add("is-active");
  showcase.setAttribute("aria-hidden", "false");

  document.body.classList.add("mirror-main-outfit-active");

  if (window.MirrorCustomerJourney?.outfit) {
    window.MirrorCustomerJourney.outfit(currentLoadedItem || lastScannedItem);
  }

  return true;
}

function hideMirrorMainOutfitShowcase() {
  const showcase = document.getElementById("mirrorMainOutfitShowcase");
  if (!showcase) return false;

  showcase.classList.remove("is-active", "is-building");
  showcase.setAttribute("aria-hidden", "true");

  document.body.classList.remove("mirror-main-outfit-active");

  return true;
}

function getMirrorMainOutfitRoleItem(fullOutfit, role) {
  if (!fullOutfit || typeof fullOutfit !== "object") {
    return null;
  }

  return fullOutfit[role] || null;
}

function getMirrorMainOutfitItemName(item, fallback = "Add Item") {
  return (
    getItemField(item, "name", "itemName", "productName", "title") ||
    fallback
  );
}

function getMirrorMainOutfitItemPrice(item) {
  const price = getItemField(item, "price");

  return Number.isFinite(Number(price)) ? formatPrice(price) : "";
}

function isPlaceholderImageUrl(value) {
   const text = String(value || "").trim().toLowerCase();

   return (
     !text ||
     text.includes("placehold.co") ||
     text.includes("placeholder") ||
     text.includes("scanned+item") ||
     text.includes("product+image") ||
     text.includes("image+needed")
   );
 }

 function getMirrorMainOutfitItemImage(item, fallbackText = "Item") {
   const fallbackUrl =
     `https://placehold.co/600x760/f7f3ec/171411?text=${encodeURIComponent(fallbackText)}`;

   if (!item || typeof item !== "object") {
     return fallbackUrl;
   }

   const candidates = [
     getItemField(
       item,
       "imageUrl",
       "image_url",
       "image",
       "photoUrl",
       "productImageUrl",
       "primaryImage",
       "primaryImageUrl",
       "thumbnailUrl",
       "thumbnail",
       "mediaUrl",
       "imageSrc",
       "pictureUrl",
       "url"
     ),

     item.product?.imageUrl,
     item.product?.image_url,
     item.product?.image,
     item.product?.photoUrl,
     item.product?.productImageUrl,
     item.product?.primaryImage,
     item.product?.primaryImageUrl,
     item.product?.thumbnailUrl,
     item.product?.mediaUrl,
     item.product?.imageSrc,
     item.product?.pictureUrl,

     item.inventoryItem?.imageUrl,
     item.inventoryItem?.image_url,
     item.inventoryItem?.image,
     item.inventoryItem?.photoUrl,
     item.inventoryItem?.productImageUrl,
     item.inventoryItem?.primaryImage,
     item.inventoryItem?.primaryImageUrl,
     item.inventoryItem?.thumbnailUrl,

     item.merchantInventoryItem?.imageUrl,
     item.merchantInventoryItem?.image_url,
     item.merchantInventoryItem?.image,

     Array.isArray(item.images) ? item.images[0]?.url : "",
     Array.isArray(item.images) ? item.images[0]?.imageUrl : "",
     Array.isArray(item.images) ? item.images[0] : "",

     Array.isArray(item.media) ? item.media[0]?.url : "",
     Array.isArray(item.media) ? item.media[0]?.imageUrl : "",
     Array.isArray(item.media) ? item.media[0] : ""
   ]
     .map(value => String(value || "").trim())
     .filter(Boolean)
     .filter(value => !isPlaceholderImageUrl(value));

   if (!candidates.length) {
     return fallbackUrl;
   }

   return safeImageUrl(candidates[0], fallbackUrl);
 }

function renderMirrorMainOutfitRailItem(role, item, options = {}) {
  const isAddSlot = !item;
  const roleLabel = getMirrorMainOutfitPieceLabel(role);
  const name = isAddSlot
    ? options.emptyName || `Add ${roleLabel}`
    : getMirrorMainOutfitItemName(item, roleLabel);

  const price = isAddSlot ? "" : getMirrorMainOutfitItemPrice(item);
  const imageUrl = isAddSlot ? "" : getMirrorMainOutfitItemImage(item, name);

  return `
    <article class="mirror-main-look-rail-item ${isAddSlot ? "is-empty" : ""}">
      <div class="mirror-main-look-rail-thumb">
        ${
          isAddSlot
            ? `<span>＋</span>`
            : `
              <img
                src="${escapeMirrorShowroomHtml(imageUrl)}"
                alt="${escapeMirrorShowroomHtml(name)}"
                loading="lazy"
                onerror="this.src='https://placehold.co/180x180/f7f3ec/171411?text=Item';"
              >
            `
        }
      </div>

      <div class="mirror-main-look-rail-copy">
        <p>${escapeMirrorShowroomHtml(roleLabel)}</p>
        <strong>${escapeMirrorShowroomHtml(name)}</strong>
        ${price ? `<span>${escapeMirrorShowroomHtml(price)}</span>` : `<span>Recommended</span>`}
      </div>

      <span class="mirror-main-look-rail-dot" aria-hidden="true"></span>
    </article>
  `;
}

function showMirrorMainOutfitLoading(item = currentLoadedItem || lastScannedItem) {
  const showcase = document.getElementById("mirrorMainOutfitShowcase");
  const grid = document.getElementById("mirrorMainOutfitGrid");
  const title = document.getElementById("mirrorMainOutfitTitle");
  const score = document.getElementById("mirrorMainOutfitScore");
  const explain = document.getElementById("mirrorMainOutfitExplain");

  if (!showcase || !grid || !title || !score || !explain) {
    return false;
  }

  const productName =
    getMirrorMainOutfitItemName?.(item, "the scanned item") ||
    getItemField(item, "name", "itemName", "productName", "title") ||
    "the scanned item";

  showcase.classList.add("mirror-main-look-board", "is-building", "is-active");
  showcase.setAttribute("aria-hidden", "false");

  document.body.classList.add("mirror-main-outfit-active");

  title.textContent = "Building the Complete Look";
  score.textContent = "•••";

  grid.innerHTML = `
    <section class="mirror-main-look-canvas mirror-main-look-building">
      <div class="mirror-main-look-hero mirror-main-look-hero-building">
        <div class="mirror-main-look-building-orb">
          <span>✦</span>
        </div>

        <h3>Pixel is styling ${escapeMirrorShowroomHtml(productName)}.</h3>
        <p>Balancing color, silhouette, shopper profile, price, and store availability.</p>

        <div class="mirror-main-look-build-lines">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>

      <aside class="mirror-main-look-rail">
        <article class="mirror-main-look-note">
          <p>Pixel’s Process</p>
          <span>
            “I’m choosing pieces that complete the scanned item using live store inventory.”
          </span>
        </article>
      </aside>
    </section>
  `;

  explain.textContent = "Pixel is preparing a complete outfit recommendation.";

  return true;
}

function renderMirrorMainRealOutfitCanvas(fullOutfit) {
  const top = getMirrorMainOutfitRoleItem(fullOutfit, "top");
  const bottom = getMirrorMainOutfitRoleItem(fullOutfit, "bottom");
  const shoes = getMirrorMainOutfitRoleItem(fullOutfit, "shoes");
  const outerwear = getMirrorMainOutfitRoleItem(fullOutfit, "outerwear");

  const pieces = [
    ["outerwear", outerwear],
    ["top", top],
    ["bottom", bottom],
    ["shoes", shoes]
  ].filter(([, item]) => !!item);

  if (!pieces.length) {
    return `
      <div class="mirror-main-outfit-empty">
        No outfit images are available yet.
      </div>
    `;
  }

  return `
    <section class="mirror-main-real-look-canvas">
      <div class="mirror-main-real-look-stage">
        ${pieces.map(([role, item]) => {
          const label = getMirrorMainOutfitPieceLabel(role);
          const name = getMirrorMainOutfitItemName(item, label);
          const price = getMirrorMainOutfitItemPrice(item);
          const imageUrl = getMirrorMainOutfitItemImage(item, name);

          return `
            <article class="mirror-main-real-look-piece ${escapeMirrorShowroomHtml(role)}">
              <div class="mirror-main-real-look-image">
                <img
                  src="${escapeMirrorShowroomHtml(imageUrl)}"
                  alt="${escapeMirrorShowroomHtml(name)}"
                  loading="lazy"
                  onerror="this.src='https://placehold.co/600x760/f7f3ec/171411?text=Image+Needed';"
                >
              </div>

              <div class="mirror-main-real-look-copy">
                <p>${escapeMirrorShowroomHtml(label)}</p>
                <strong>${escapeMirrorShowroomHtml(name)}</strong>
                ${price ? `<span>${escapeMirrorShowroomHtml(price)}</span>` : ""}
              </div>
            </article>
          `;
        }).join("")}
      </div>

      <aside class="mirror-main-look-rail">
        ${renderMirrorMainOutfitRailItem("top", top)}
        ${renderMirrorMainOutfitRailItem("bottom", bottom)}
        ${renderMirrorMainOutfitRailItem("shoes", shoes)}
        ${renderMirrorMainOutfitRailItem("outerwear", outerwear, {
          emptyName: "Add Layer +"
        })}

        <article class="mirror-main-look-note">
          <p>Pixel’s Note</p>
          <span>
            “This look was assembled from live store inventory and real product imagery, balanced by color, category, price, and availability.”
          </span>
        </article>
      </aside>
    </section>
  `;
}

const PIXEL_CHECKOUT_INTENTS_KEY = "pixelMirrorCheckoutIntents";

function getMirrorMainCheckoutPieces(fullOutfit = currentMirrorMainFullOutfit) {
  if (!fullOutfit || typeof fullOutfit !== "object") {
    return [];
  }

  return getMirrorMainOutfitPieces(fullOutfit).map(([role, item]) => {
    const name =
      getItemField(item, "name", "itemName", "productName", "title") ||
      getMirrorMainOutfitPieceLabel(role);

    const rfid =
      getItemField(item, "rfid", "itemRfid", "productRfid", "id") ||
      "";

    const price = safeNumber(getItemField(item, "price"), 0);

    const category =
      getItemField(item, "category", "productType", "type") ||
      getMirrorMainOutfitPieceLabel(role);

    const imageUrl = getMirrorMainOutfitItemImage(item, name);

    return {
      role,
      name,
      rfid,
      price,
      category,
      imageUrl
    };
  });
}

function getPixelCheckoutIntents() {
  const stored = safeParseJson(localStorage.getItem(PIXEL_CHECKOUT_INTENTS_KEY));
  return Array.isArray(stored) ? stored : [];
}

function writePixelCheckoutIntents(intents) {
  localStorage.setItem(
    PIXEL_CHECKOUT_INTENTS_KEY,
    JSON.stringify(Array.isArray(intents) ? intents.slice(0, 50) : [])
  );
}

function buildMirrorCheckoutIntent(fullOutfit = currentMirrorMainFullOutfit) {
  if (!fullOutfit || typeof fullOutfit !== "object") {
    throw new Error("Create a complete look before requesting checkout.");
  }

  const pieces = getMirrorMainCheckoutPieces(fullOutfit);

  if (!pieces.length) {
    throw new Error("This look does not have checkout-ready pieces.");
  }

  const runtime =
    typeof getMirrorRuntime === "function"
      ? getMirrorRuntime()
      : window.MirrorRuntimeState || {};

  const anchorItem = currentLoadedItem || lastScannedItem || null;

  const anchorName =
    getItemField(anchorItem, "name", "itemName", "productName", "title") ||
    "Scanned Item";

  const totalValue = pieces.reduce((sum, piece) => {
    return sum + safeNumber(piece.price, 0);
  }, 0);

  const associateRecommendations =
    typeof getPixelAssociateRecommendations === "function"
      ? getPixelAssociateRecommendations()
      : window.PixelAssociateRecommendations || { recommendations: [] };

  return {
    id: crypto.randomUUID(),
    createdAt: new Date().toISOString(),
    status: "requested",
    source: "smart-mirror",
    retailerKey:
      runtime.retailerKey ||
      getSelectedRetailerKey?.() ||
      "",
    retailerConfigKey:
      runtime.retailerConfigKey ||
      "",
    storeCode:
      runtime.storeCode ||
      getSelectedStoreCode?.() ||
      "",
    storeName:
      runtime.storeName ||
      getMirrorShowroomDisplayStore?.() ||
      getMirrorStoreDisplayName?.() ||
      "Current Store",
    vibe:
      runtime.vibe ||
      document.getElementById("vibeSelect")?.value ||
      "Casual",
    anchorRfid:
      currentRfid ||
      getItemField(anchorItem, "rfid", "itemRfid", "productRfid", "id") ||
      "",
    anchorItem: anchorName,
    totalValue,
    itemCount: pieces.length,
    items: pieces,
    outfitScore:
      typeof getAdvancedOutfitScores === "function"
        ? getAdvancedOutfitScores(fullOutfit)?.overallScore || 0
        : safeNumber(fullOutfit.overallScore, 0),
    associateRecommendations:
      Array.isArray(associateRecommendations.recommendations)
        ? associateRecommendations.recommendations.slice(0, 6)
        : [],
    note:
      "Customer requested checkout assistance from the smart mirror complete-look experience."
  };
}

function setMirrorMainCheckoutButtonState(state = "ready") {
  const button = document.getElementById("mirrorMainCheckoutBtn");

  if (!button) {
    return;
  }

  button.classList.remove("is-saved");

  if (state === "loading") {
    button.disabled = true;
    button.textContent = "Sending Request...";
    return;
  }

  if (state === "sent") {
    button.disabled = true;
    button.textContent = "Checkout Requested ✓";
    button.classList.add("is-saved");
    return;
  }

  button.disabled = false;
  button.textContent = "Request Checkout";
}

function ensureMirrorMainCheckoutButton() {
  const actions = document.querySelector(".mirror-main-outfit-actions");
  const anotherButton = document.getElementById("mirrorMainOutfitAnotherBtn");

  if (!actions) {
    return null;
  }

  let checkoutButton = document.getElementById("mirrorMainCheckoutBtn");

  if (checkoutButton) {
    return checkoutButton;
  }

  checkoutButton = document.createElement("button");
  checkoutButton.className = "mirror-main-secondary";
  checkoutButton.type = "button";
  checkoutButton.id = "mirrorMainCheckoutBtn";
  checkoutButton.textContent = "Request Checkout";

  if (anotherButton) {
    actions.insertBefore(checkoutButton, anotherButton);
  } else {
    actions.appendChild(checkoutButton);
  }

  checkoutButton.addEventListener("click", requestMirrorMainCheckoutIntent);

  return checkoutButton;
}

async function requestMirrorMainCheckoutIntent() {
  const fullOutfit = currentMirrorMainFullOutfit;

  try {
    setMirrorMainCheckoutButtonState("loading");

    const intent = buildMirrorCheckoutIntent(fullOutfit);
    const intents = getPixelCheckoutIntents();

    writePixelCheckoutIntents([intent, ...intents]);

    window.PixelMirrorLatestCheckoutIntent = intent;

    console.group("Pixel Checkout Intent");
    console.table({
      id: intent.id,
      status: intent.status,
      storeName: intent.storeName,
      retailerKey: intent.retailerKey,
      storeCode: intent.storeCode,
      anchorItem: intent.anchorItem,
      itemCount: intent.itemCount,
      totalValue: formatPrice(intent.totalValue),
      outfitScore: intent.outfitScore
    });
    console.table(intent.items);
    console.log("Full checkout intent:", intent);
    console.groupEnd();

    if (typeof addTryOnTimelineEvent === "function") {
      addTryOnTimelineEvent(
        "checkout",
        "Checkout requested",
        `${intent.anchorItem} complete look was sent for associate checkout assistance.`,
        [
          intent.storeName,
          formatPrice(intent.totalValue),
          `${intent.itemCount} item${intent.itemCount === 1 ? "" : "s"}`
        ]
      );
    }

    if (typeof setStatus === "function") {
      setStatus("Checkout request sent to associate assistance.", "success");
    }

    if (typeof showToast === "function") {
      showToast("Checkout request sent.", "success");
    }

    if (typeof speakPixelConcierge === "function") {
      speakPixelConcierge("command", {
        commandName: "Checkout request sent"
      });
    }

    setMirrorMainCheckoutButtonState("sent");

    if (typeof updateMirrorAssociateControlStats === "function") {
      updateMirrorAssociateControlStats();
    }

    return intent;

  } catch (error) {
    console.error("Checkout intent failed:", error);

    const message =
      error.message ||
      "Unable to request checkout for this look.";

    setMirrorMainCheckoutButtonState("ready");

    if (typeof setStatus === "function") {
      setStatus(message, "error");
    }

    if (typeof showToast === "function") {
      showToast(message, "error");
    }

    if (typeof speakPixelConcierge === "function") {
      speakPixelConcierge("error", {
        errorMessage: message
      });
    }

    return null;
  }
}

function renderMirrorMainOutfitShowcase(fullOutfit) {
  const showcase = document.getElementById("mirrorMainOutfitShowcase");
  const grid = document.getElementById("mirrorMainOutfitGrid");
  const title = document.getElementById("mirrorMainOutfitTitle");
  const score = document.getElementById("mirrorMainOutfitScore");
  const explain = document.getElementById("mirrorMainOutfitExplain");
  const saveButton = document.getElementById("mirrorMainSaveLookBtn");
  const anotherButton = document.getElementById("mirrorMainOutfitAnotherBtn");

  if (!showcase || !grid || !title || !score || !explain) {
    return false;
  }

  console.log("REAL BACKEND FULL OUTFIT:", fullOutfit);

  console.table({
    topImage: getMirrorMainOutfitItemImage(fullOutfit?.top, "Top"),
    bottomImage: getMirrorMainOutfitItemImage(fullOutfit?.bottom, "Bottom"),
    shoesImage: getMirrorMainOutfitItemImage(fullOutfit?.shoes, "Shoes"),
    outerwearImage: getMirrorMainOutfitItemImage(fullOutfit?.outerwear, "Outerwear"),

    rawTopImage: fullOutfit?.top?.imageUrl,
    rawBottomImage: fullOutfit?.bottom?.imageUrl,
    rawShoesImage: fullOutfit?.shoes?.imageUrl,
    rawOuterwearImage: fullOutfit?.outerwear?.imageUrl
  });

  showcase.classList.add("mirror-main-look-board");

  if (!fullOutfit) {
    grid.innerHTML = `
      <div class="mirror-main-outfit-empty">
        Create a full outfit to see Pixel’s styling recommendation.
      </div>
    `;

    title.textContent = "The Complete Look";
    score.textContent = "0%";
    explain.textContent = "Pixel will explain how the look works once it is generated.";

    hideMirrorMainOutfitShowcase();
    return false;
  }

  const top = getMirrorMainOutfitRoleItem(fullOutfit, "top");
  const bottom = getMirrorMainOutfitRoleItem(fullOutfit, "bottom");
  const shoes = getMirrorMainOutfitRoleItem(fullOutfit, "shoes");
  const outerwear = getMirrorMainOutfitRoleItem(fullOutfit, "outerwear");

  const analysis =
    typeof getAdvancedOutfitScores === "function"
      ? getAdvancedOutfitScores(fullOutfit)
      : null;

  const outfitScore =
    analysis?.overallScore ||
    safeNumber(fullOutfit.overallScore) ||
    87;

  title.textContent = "The Complete Look";
  score.textContent = `${outfitScore}%`;

  setMirrorMainLookSaveButtonState(false);

  const checkoutButton = ensureMirrorMainCheckoutButton();

  if (checkoutButton) {
    setMirrorMainCheckoutButtonState("ready");
  }

  if (anotherButton) {
    anotherButton.textContent = "Scan Another Item";
  }

  currentMirrorMainFullOutfit = fullOutfit;

  grid.innerHTML = renderMirrorMainRealOutfitCanvas(fullOutfit);

  explain.textContent =
    fullOutfit.explanation ||
    "Pixel balanced the scanned item with matching store inventory, shopper signals, color, price, and availability.";

  showMirrorMainOutfitShowcase();

  if (typeof updateMirrorAssociateControlStats === "function") {
    updateMirrorAssociateControlStats();
  }

  return true;
}

function handleMirrorMainStartScan() {
  if (window.MirrorCustomerJourney?.scan) {
    window.MirrorCustomerJourney.scan();
  } else if (typeof setMirrorCustomerStage === "function") {
    setMirrorCustomerStage(MIRROR_CUSTOMER_STAGES.SCANNING);
  }

  setMirrorMainScanningState(true);

  if (typeof resetAmbientIdleTimer === "function") {
    resetAmbientIdleTimer();
  }

  window.setTimeout(() => {
    if (typeof focusMirrorScanInput === "function") {
      focusMirrorScanInput();
    } else if (typeof focusRfidInput === "function") {
      focusRfidInput();
    }

    if (typeof setStatus === "function") {
      setStatus("Reading RFID scan. Hold the item near the mirror reader.", "ready");
    }

    if (typeof speakPixelConcierge === "function") {
      speakPixelConcierge("scan");
    }
  }, 120);

  return true;
}

function openMirrorMainProfileDrawer() {
  document
    .getElementById("mirrorMainProfileDrawer")
    ?.classList.add("is-active");

  if (typeof speakPixelConcierge === "function") {
    speakPixelConcierge("profile");
  }
}

function closeMirrorMainProfileDrawer() {
  document
    .getElementById("mirrorMainProfileDrawer")
    ?.classList.remove("is-active");
}

function openMirrorMainBagDrawer() {
  const drawer = document.getElementById("mirrorMainBagDrawer");

  if (!drawer) {
    return false;
  }

  drawer.classList.add("is-active");
  drawer.setAttribute("aria-hidden", "false");

  renderMirrorMainBagDrawer();

  if (typeof speakPixelConcierge === "function") {
    speakPixelConcierge("bag", {
      bagCount: getMirrorMainBagCount()
    });
  }

  return true;
}

function closeMirrorMainBagDrawer() {
  const drawer = document.getElementById("mirrorMainBagDrawer");

  if (!drawer) {
    return false;
  }

  drawer.classList.remove("is-active");
  drawer.setAttribute("aria-hidden", "true");

  return true;
}

function getMirrorMainBagItemRfid(item) {
  return (
    item.rfid ||
    item.itemRfid ||
    item.productRfid ||
    item.product?.rfid ||
    item.inventoryItem?.rfid ||
    ""
  );
}

function getMirrorMainBagItemName(item) {
  return (
    item.itemName ||
    item.name ||
    item.productName ||
    item.title ||
    item.product?.name ||
    item.inventoryItem?.name ||
    "Saved Item"
  );
}

function getMirrorMainBagItemImage(item) {
  return safeImageUrl(
    item.imageUrl ||
      item.image_url ||
      item.image ||
      item.photoUrl ||
      item.productImageUrl ||
      item.product?.imageUrl ||
      item.product?.image ||
      item.inventoryItem?.imageUrl ||
      item.inventoryItem?.image,
    "https://placehold.co/180x180/f7f3ec/171411?text=Item"
  );
}

function renderMirrorMainBagDrawerEmpty(message = "Saved items will appear here after the customer taps Save to Bag.") {
  const content = document.getElementById("mirrorMainBagDrawerContent");

  renderMirrorMainBagIntentSummary([]);

  if (content) {
    content.innerHTML = `
      <div class="mirror-main-bag-empty">
        ${escapeHtml(message)}
      </div>
    `;
  }
}

async function renderMirrorMainBagDrawer() {
  const content = document.getElementById("mirrorMainBagDrawerContent");

  if (!content) {
    return false;
  }

  if (!getToken()) {
    renderMirrorMainBagDrawerEmpty("Login from the Merchant App to view the shopper bag.");
    return false;
  }

  content.innerHTML = `
    <div class="mirror-main-bag-loading">
      Refreshing shopper bag...
    </div>
  `;

  try {
    const response = await fetch(`${API.stylist}/bag`, {
      method: "GET",
      headers: getAuthHeaders({
        Accept: "application/json"
      })
    });

    await assertAuthorizedResponse(response, "Unable to load bag.");

    const bag = await response.json();
    const items = Array.isArray(bag.items) ? bag.items : [];

    savedRfids = new Set(
      items
        .map(getMirrorMainBagItemRfid)
        .filter(Boolean)
    );

    updateMirrorMainBagCount();
    renderMirrorMainBagIntentSummary(items);

    if (!items.length) {
      renderMirrorMainBagDrawerEmpty("Your style bag is empty. Save a scanned item to build customer intent.");
      return true;
    }

    content.innerHTML = items.map(item => {
      const id = item.id || "";
      const name = getMirrorMainBagItemName(item);
      const retailer = item.retailerName || item.retailer || getMirrorShowroomDisplayStore();
      const category = item.category || item.product?.category || item.inventoryItem?.category || "Item";
      const price = item.price || item.product?.price || item.inventoryItem?.price || "";
      const imageUrl = getMirrorMainBagItemImage(item);

      return `
        <article class="mirror-main-bag-row">
          <div class="mirror-main-bag-thumb">
            <img
              src="${escapeMirrorShowroomHtml(imageUrl)}"
              alt="${escapeMirrorShowroomHtml(name)}"
              loading="lazy"
              onerror="this.src='https://placehold.co/180x180/f7f3ec/171411?text=Item';"
            >
          </div>

          <div class="mirror-main-bag-copy">
            <p>${escapeMirrorShowroomHtml(retailer)}</p>
            <strong>${escapeMirrorShowroomHtml(name)}</strong>
            <span>
              ${escapeMirrorShowroomHtml(category)}
              ${price ? ` · ${escapeMirrorShowroomHtml(formatPrice(price))}` : ""}
            </span>
          </div>

          <button
            class="mirror-main-bag-remove"
            type="button"
            data-mirror-main-bag-remove-id="${escapeMirrorShowroomHtml(id)}"
            data-mirror-main-bag-remove-name="${escapeMirrorShowroomHtml(name)}"
          >
            Remove
          </button>
        </article>
      `;
    }).join("");

    content.querySelectorAll("[data-mirror-main-bag-remove-id]").forEach(button => {
      button.addEventListener("click", () => {
        removeMirrorMainBagItem(
          button.dataset.mirrorMainBagRemoveId || "",
          button.dataset.mirrorMainBagRemoveName || "This item"
        );
      });
    });

    return true;
  } catch (error) {
    console.error("Premium mirror bag drawer failed:", error);

    content.innerHTML = `
      <div class="mirror-main-bag-empty">
        Unable to load the shopper bag right now.
      </div>
    `;

    if (typeof showToast === "function") {
      showToast(error.message || "Unable to load bag.", "error");
    }

    if (typeof speakPixelConcierge === "function") {
      speakPixelConcierge("error", {
        errorMessage: error.message || "Unable to load bag."
      });
    }

    return false;
  }
}

async function removeMirrorMainBagItem(id, itemName = "This item") {
  if (!id) {
    showToast?.("Missing bag item id.", "error");
    return false;
  }

  try {
    requireToken();

    const response = await fetch(`${API.stylist}/bag/${encodeURIComponent(id)}`, {
      method: "DELETE",
      headers: getAuthHeaders()
    });

    await assertAuthorizedResponse(response, "Unable to remove item.");

    showToast?.(`${itemName} removed from bag.`, "success");

    if (typeof speakPixelConcierge === "function") {
      speakPixelConcierge("remove", {
        itemName,
        bagCount: Math.max(0, getMirrorMainBagCount() - 1)
      });
    }

    if (typeof addTryOnTimelineEvent === "function") {
      addTryOnTimelineEvent(
        "remove",
        "Removed item from premium mirror bag",
        `${itemName} was removed from the shopper’s style bag.`,
        [getMirrorShowroomDisplayStore()]
      );
    }

    await renderMirrorMainBagDrawer();

    return true;
  } catch (error) {
    console.error("Premium mirror bag remove failed:", error);

    showToast?.(error.message || "Unable to remove item.", "error");

    if (typeof speakPixelConcierge === "function") {
      speakPixelConcierge("removeError", {
        errorMessage: error.message || "Unable to remove item."
      });
    }

    return false;
  }
}

function openMirrorMainShowroomOverlay() {
  document
    .getElementById("mirrorMainShowroomOverlay")
    ?.classList.add("is-active");

  if (typeof speakPixelConcierge === "function") {
    speakPixelConcierge("cinematic");
  }
}

function closeMirrorMainShowroomOverlay() {
  document
    .getElementById("mirrorMainShowroomOverlay")
    ?.classList.remove("is-active");
}

/* =========================================================
   Universal Stylist — Mirror Runtime v1
   One smart mirror engine for every retailer/store
   Paste above Customer Main Experience
   ========================================================= */

const MIRROR_RUNTIME_STORAGE_KEY = "universalStylistMirrorRuntime";

const MIRROR_THEME_PRESETS = {
  default: {
    id: "default-premium",
    label: "Premium Retail",
    surface: "#f8fafc",
    ink: "#0f172a",
    muted: "#64748b",
    accent: "#4A90D9",
    accentSoft: "rgba(74, 144, 217, 0.16)",
    secondary: "#C3B8E8",
    success: "#7DBF8E",
    editorialEyebrow: "Today’s Editorial",
    editorialTitleTop: "The",
    editorialTitleStrong: "Modern",
    editorialTitleBottom: "Minimalist",
    editorialCopy: "Scan an item to unlock styling, outfit intelligence, and store-aware recommendations."
  },

  boutiqueMinimal: {
    id: "boutique-minimal",
    label: "Boutique Minimal",
    surface: "#f7f3ec",
    ink: "#171411",
    muted: "#756b5f",
    accent: "#9b7a4f",
    accentSoft: "rgba(155, 122, 79, 0.18)",
    secondary: "#d7c2a2",
    success: "#7DBF8E",
    editorialEyebrow: "Autumn — Winter Collection",
    editorialTitleTop: "Dressed in",
    editorialTitleStrong: "Silence.",
    editorialTitleBottom: "",
    editorialCopy: "Curated selections from this boutique, brought to your reflection."
  },

  departmentStore: {
    id: "department-store",
    label: "Department Store",
    surface: "#f8fafc",
    ink: "#0f172a",
    muted: "#64748b",
    accent: "#cc0033",
    accentSoft: "rgba(204, 0, 51, 0.12)",
    secondary: "#111827",
    success: "#7DBF8E",
    editorialEyebrow: "New Arrivals",
    editorialTitleTop: "Styled",
    editorialTitleStrong: "For",
    editorialTitleBottom: "You",
    editorialCopy: "Explore store inventory, complete looks, and personalized recommendations."
  }
};
function getMirrorRuntimeRetailerConfigKey(retailerKey = "", storeName = "", storeCode = "") {
  const key = String(retailerKey || "").trim().toUpperCase();
  const store = String(storeName || "").trim().toLowerCase();
  const code = String(storeCode || "").trim().toLowerCase();

  if (key && RETAILER_CONFIG?.getRetailerConfig?.(key)) {
    return key;
  }

  if (code && typeof RETAILER_CONFIG?.getStoreByCode === "function") {
    const storeMatch = RETAILER_CONFIG.getStoreByCode(storeCode);

    if (storeMatch?.retailerKey) {
      return storeMatch.retailerKey;
    }
  }

  if (
    key.startsWith("MCS") ||
    key.includes("NICKS") ||
    store.includes("nick") ||
    store.includes("boutique") ||
    code.includes("nick") ||
    code.includes("boutique")
  ) {
    return "NICKS001";
  }

  if (
    key.includes("KINGS") ||
    store.includes("king") ||
    code.includes("king")
  ) {
    return "KINGS001";
  }

  if (
    key.includes("MACY") ||
    store.includes("macy") ||
    code.includes("macy")
  ) {
    return "MACY001";
  }

  if (
    key.includes("ZARA") ||
    store.includes("zara") ||
    code.includes("zara")
  ) {
    return "ZARA001";
  }

  if (
    key.includes("NORD") ||
    store.includes("nordstrom") ||
    code.includes("nord")
  ) {
    return "NORD001";
  }

  if (
    key.includes("NIKE") ||
    store.includes("nike") ||
    code.includes("nike")
  ) {
    return "NIKE001";
  }

  if (
    key.includes("WALMART") ||
    store.includes("walmart") ||
    code.includes("walmart")
  ) {
    return "WALMART001";
  }

  if (
    key.includes("TARGET") ||
    store.includes("target") ||
    code.includes("target")
  ) {
    return "TARGET001";
  }

  return key || "";
}

function getMirrorRuntimeThemePreset(retailerKey = "", storeName = "", storeCode = "") {
  const configKey = getMirrorRuntimeRetailerConfigKey(retailerKey, storeName, storeCode);

  const retailerConfig =
    typeof RETAILER_CONFIG?.getRetailerMirrorConfig === "function"
      ? RETAILER_CONFIG.getRetailerMirrorConfig(configKey)
      : null;

  if (retailerConfig?.theme) {
    return {
      ...MIRROR_THEME_PRESETS.default,
      ...retailerConfig.theme
    };
  }

  const retailerTheme =
    typeof RETAILER_CONFIG?.getRetailerTheme === "function"
      ? RETAILER_CONFIG.getRetailerTheme(configKey)
      : null;

  if (retailerTheme) {
    return {
      ...MIRROR_THEME_PRESETS.default,
      ...retailerTheme
    };
  }

  const key = String(retailerKey || "").toUpperCase();
  const store = String(storeName || "").toLowerCase();
  const code = String(storeCode || "").toLowerCase();

  if (
    key.includes("MCS") ||
    key.includes("NICKS") ||
    store.includes("nick") ||
    store.includes("boutique") ||
    code.includes("nick") ||
    code.includes("boutique")
  ) {
    return MIRROR_THEME_PRESETS.boutiqueMinimal;
  }

  if (key.includes("MACY") || store.includes("macy") || code.includes("macy")) {
    return MIRROR_THEME_PRESETS.departmentStore;
  }

  return MIRROR_THEME_PRESETS.default;
}

function getMirrorRuntimeUrlContext() {
  const params = new URLSearchParams(window.location.search);

  return {
    retailerKey: params.get("retailer") || "",
    storeCode: params.get("storeCode") || "",
    storeName: params.get("storeName") || "",
    vibe: params.get("vibe") || ""
  };
}

function readMirrorRuntimeStorageContext() {
  const stored = safeParseJson(localStorage.getItem(MIRROR_RUNTIME_STORAGE_KEY)) || {};

  return {
    retailerKey:
      stored.retailerKey ||
      localStorage.getItem("currentRetailerKey") ||
      localStorage.getItem("retailerKey") ||
      "",
    storeCode:
      stored.storeCode ||
      localStorage.getItem("currentStoreCode") ||
      localStorage.getItem("storeCode") ||
      "",
    storeName:
      stored.storeName ||
      localStorage.getItem("currentStoreName") ||
      localStorage.getItem("storeName") ||
      "",
    vibe:
      stored.vibe ||
      localStorage.getItem("mirrorRuntimeVibe") ||
      ""
  };
}

function getMirrorRuntimeDomContext() {
  const retailerKey =
    document.getElementById("retailerSelect")?.value ||
    "";

  const storeSelect = document.getElementById("storeCodeSelect");
  const storeCode = storeSelect?.value || "";

  const storeName =
    storeSelect?.selectedOptions?.[0]?.textContent?.trim() ||
    "";

  const vibe =
    document.getElementById("vibeSelect")?.value ||
    "";

  return {
    retailerKey,
    storeCode,
    storeName,
    vibe
  };
}

function cleanMirrorRuntimeText(value, fallback = "") {
  const text = String(value || "").trim();
  return text || fallback;
}

function buildMirrorRuntime() {
  const urlContext = getMirrorRuntimeUrlContext();
  const storageContext = readMirrorRuntimeStorageContext();
  const domContext = getMirrorRuntimeDomContext();

  let retailerKey = cleanMirrorRuntimeText(
    urlContext.retailerKey ||
      domContext.retailerKey ||
      storageContext.retailerKey ||
      "MACY001"
  );

  const preliminaryStoreCode = cleanMirrorRuntimeText(
    urlContext.storeCode ||
      domContext.storeCode ||
      storageContext.storeCode ||
      ""
  );

  const preliminaryStoreName = cleanMirrorRuntimeText(
    urlContext.storeName ||
      domContext.storeName ||
      storageContext.storeName ||
      ""
  );

  if (!retailerKey && preliminaryStoreCode && typeof RETAILER_CONFIG?.getStoreByCode === "function") {
    const storeMatch = RETAILER_CONFIG.getStoreByCode(preliminaryStoreCode);
    retailerKey = storeMatch?.retailerKey || retailerKey;
  }

  const retailerConfigKey = getMirrorRuntimeRetailerConfigKey(
    retailerKey,
    preliminaryStoreName,
    preliminaryStoreCode
  );

  const retailerMirrorConfig =
    typeof RETAILER_CONFIG?.getRetailerMirrorConfig === "function"
      ? RETAILER_CONFIG.getRetailerMirrorConfig(retailerConfigKey)
      : null;

  const defaultStoreCode =
    retailerMirrorConfig?.defaultStoreCode ||
    (typeof RETAILER_CONFIG?.getDefaultStoreCode === "function"
      ? RETAILER_CONFIG.getDefaultStoreCode(retailerConfigKey)
      : "") ||
    "";

  const storeCode = cleanMirrorRuntimeText(
    preliminaryStoreCode ||
      defaultStoreCode ||
      ""
  );

  const configuredStoreName =
    typeof RETAILER_CONFIG?.getStoreName === "function"
      ? RETAILER_CONFIG.getStoreName(retailerConfigKey, storeCode)
      : "";

  const rawStoreName = cleanMirrorRuntimeText(
    preliminaryStoreName ||
      configuredStoreName ||
      storeCode ||
      retailerKey
  );

  const storeName = beautifyStoreName(rawStoreName);

  const vibe = cleanMirrorRuntimeText(
    urlContext.vibe ||
      domContext.vibe ||
      storageContext.vibe ||
      "Casual"
  );

  const retailerName =
    retailerMirrorConfig?.name ||
    RETAILER_CONFIG?.getRetailerName?.(retailerConfigKey) ||
    beautifyRetailerName(retailerKey);

  const brandLabel =
    retailerMirrorConfig?.brandLabel ||
    RETAILER_CONFIG?.getRetailerBrandLabel?.(retailerConfigKey) ||
    retailerName;

  const retailerType =
    retailerMirrorConfig?.type ||
    RETAILER_CONFIG?.getRetailerType?.(retailerConfigKey) ||
    "default";

  const theme = getMirrorRuntimeThemePreset(retailerKey, storeName, storeCode);

  return {
    id: `${retailerKey}:${storeCode || "NO_STORE"}`,
    retailerKey,
    retailerConfigKey,
    retailerName,
    brandLabel,
    retailerType,
    storeCode,
    storeName,
    vibe,
    theme,
    inventoryScope: storeCode ? "store-only" : "retailer-default",
    customerModeEnabled: true,
    associateModeEnabled: true,
    createdAt: new Date().toISOString()
  };
}

function persistMirrorRuntime(runtime) {
  if (!runtime) return;

  localStorage.setItem(
    MIRROR_RUNTIME_STORAGE_KEY,
    JSON.stringify({
      retailerKey: runtime.retailerKey,
      retailerName: runtime.retailerName,
      storeCode: runtime.storeCode,
      storeName: runtime.storeName,
      vibe: runtime.vibe,
      themeId: runtime.theme?.id || "default-premium",
      inventoryScope: runtime.inventoryScope,
      updatedAt: new Date().toISOString()
    })
  );

  localStorage.setItem("retailerKey", runtime.retailerKey);
  localStorage.setItem("currentRetailerKey", runtime.retailerKey);

  if (runtime.storeCode) {
    localStorage.setItem("storeCode", runtime.storeCode);
    localStorage.setItem("currentStoreCode", runtime.storeCode);
  }

  if (runtime.storeName) {
    localStorage.setItem("storeName", runtime.storeName);
    localStorage.setItem("currentStoreName", runtime.storeName);
  }

  localStorage.setItem("mirrorRuntimeVibe", runtime.vibe);
}

function applyMirrorRuntimeTheme(runtime) {
  if (!runtime?.theme) return;

  const root = document.documentElement;
  const theme = runtime.theme;

  root.style.setProperty("--mirror-runtime-surface", theme.surface);
  root.style.setProperty("--mirror-runtime-ink", theme.ink);
  root.style.setProperty("--mirror-runtime-muted", theme.muted);
  root.style.setProperty("--mirror-runtime-accent", theme.accent);
  root.style.setProperty("--mirror-runtime-accent-soft", theme.accentSoft);
  root.style.setProperty("--mirror-runtime-secondary", theme.secondary);
  root.style.setProperty("--mirror-runtime-success", theme.success);

  document.body.dataset.mirrorRetailer = runtime.retailerKey;
  document.body.dataset.mirrorStore = runtime.storeCode || "";
  document.body.dataset.mirrorTheme = theme.id;
}

function applyMirrorRuntimeToExperience(runtime) {
  if (!runtime) return;

  const storeLabels = document.querySelectorAll(".mirror-main-store span:last-child");
  storeLabels.forEach(label => {
    label.textContent = runtime.storeName;
  });

  const editorialEyebrow = document.querySelector(".mirror-main-eyebrow");
  if (editorialEyebrow) {
    editorialEyebrow.textContent = runtime.theme.editorialEyebrow;
  }

  const editorialTitle = document.querySelector(".mirror-main-title");
  if (editorialTitle) {
    const bottom = runtime.theme.editorialTitleBottom
      ? `<span>${escapeMirrorShowroomHtml(runtime.theme.editorialTitleBottom)}</span>`
      : "";

    editorialTitle.innerHTML = `
      <span>${escapeMirrorShowroomHtml(runtime.theme.editorialTitleTop)}</span>
      <em>${escapeMirrorShowroomHtml(runtime.theme.editorialTitleStrong)}</em>
      ${bottom}
    `;
  }

 const editorialCopy = document.querySelector(".mirror-main-editorial-copy");
 if (editorialCopy) {
   const copy = runtime.theme.editorialCopy || "";

   editorialCopy.textContent = copy.includes("this boutique")
     ? copy.replace("this boutique", runtime.storeName)
     : copy.includes("this store")
       ? copy.replace("this store", runtime.storeName)
       : copy;
 }

  const showroomTitle = document.querySelector(".mirror-main-showroom-content h2");
  if (showroomTitle) {
    showroomTitle.textContent = runtime.storeName;
  }

  if (typeof updateMirrorMainProductCard === "function") {
    updateMirrorMainProductCard();
  }

  if (typeof updateMirrorAssociateControlStats === "function") {
    updateMirrorAssociateControlStats();
  }
}

function refreshMirrorRuntime() {
  currentMirrorRuntime = buildMirrorRuntime();

  persistMirrorRuntime(currentMirrorRuntime);
  applyMirrorRuntimeTheme(currentMirrorRuntime);
  applyMirrorRuntimeToExperience(currentMirrorRuntime);

  window.MirrorRuntimeState = currentMirrorRuntime;

  return currentMirrorRuntime;
}

function getMirrorRuntime() {
  if (!currentMirrorRuntime) {
    return refreshMirrorRuntime();
  }

  return currentMirrorRuntime;
}

function exposeMirrorRuntimeTools() {
  window.MirrorRuntime = {
    get: getMirrorRuntime,

    refresh() {
      const runtime = refreshMirrorRuntime();
      console.table({
        retailerKey: runtime.retailerKey,
        retailerName: runtime.retailerName,
        storeCode: runtime.storeCode,
        storeName: runtime.storeName,
        vibe: runtime.vibe,
        theme: runtime.theme.id,
        inventoryScope: runtime.inventoryScope,
        customerModeEnabled: runtime.customerModeEnabled,
        associateModeEnabled: runtime.associateModeEnabled
      });

      return runtime;
    },

    status() {
      const runtime = getMirrorRuntime();

      return {
        id: runtime.id,
        retailerKey: runtime.retailerKey,
        retailerName: runtime.retailerName,
        storeCode: runtime.storeCode,
        storeName: runtime.storeName,
        vibe: runtime.vibe,
        theme: runtime.theme.id,
        inventoryScope: runtime.inventoryScope,
        customerModeEnabled: runtime.customerModeEnabled,
        associateModeEnabled: runtime.associateModeEnabled
      };
    },

    health() {
      const runtime = getMirrorRuntime();

      const report = {
        healthy: !!runtime.retailerKey && !!runtime.storeName,
        hasRetailerKey: !!runtime.retailerKey,
        hasStoreCode: !!runtime.storeCode,
        hasStoreName: !!runtime.storeName,
        hasTheme: !!runtime.theme?.id,
        customerModeEnabled: runtime.customerModeEnabled,
        associateModeEnabled: runtime.associateModeEnabled,
        inventoryScope: runtime.inventoryScope,
        runtime
      };

      console.table({
        healthy: report.healthy,
        retailerKey: runtime.retailerKey,
        storeCode: runtime.storeCode,
        storeName: runtime.storeName,
        theme: runtime.theme.id,
        inventoryScope: runtime.inventoryScope
      });

      return report;
    }
  };

  if (window.PixelMirrorDebug) {
    window.PixelMirrorDebug.runtime = window.MirrorRuntime.status;
    window.PixelMirrorDebug.runtimeHealth = window.MirrorRuntime.health;
    window.PixelMirrorDebug.refreshRuntime = window.MirrorRuntime.refresh;
  }

  console.log("MirrorRuntime ready. Try: MirrorRuntime.health()");
}

/* =========================================================
   Universal Stylist — Associate Control Drawer v1
   Vanilla conversion of UXCanvas AssociateControlOpen
   Paste above exposeMirrorMainExperienceTools()
   ========================================================= */

function getMirrorAssociateSessionStats() {
  const session =
    typeof readTryOnSession === "function"
      ? readTryOnSession()
      : {
          scans: [],
          looksCreated: 0,
          savesToBag: 0
        };

  const scans = Array.isArray(session.scans) ? session.scans : [];
  const totalInterest = scans.reduce((sum, scan) => {
    return sum + safeNumber(scan.price || 0);
  }, 0);

  return {
    scansCount: scans.length,
    looksCreated: safeNumber(session.looksCreated || 0),
    savesToBag: safeNumber(session.savesToBag || 0),
    interestValue: totalInterest,
    interestLabel: totalInterest > 0 ? formatPrice(totalInterest) : "$0.00",
    storeName: getMirrorShowroomDisplayStore()
  };
}

function getMirrorAssociateLatestCheckoutIntent() {
  const intents =
    typeof getPixelCheckoutIntents === "function"
      ? getPixelCheckoutIntents()
      : [];

  if (window.PixelMirrorLatestCheckoutIntent) {
    return window.PixelMirrorLatestCheckoutIntent;
  }

  return Array.isArray(intents) && intents.length ? intents[0] : null;
}

function writeMirrorAssociateCheckoutIntent(intent) {
  if (!intent || !intent.id || typeof getPixelCheckoutIntents !== "function") {
    return null;
  }

  const existingIntents = getPixelCheckoutIntents();
  const nextIntents = [
    intent,
    ...existingIntents.filter(existing => existing.id !== intent.id)
  ];

  if (typeof writePixelCheckoutIntents === "function") {
    writePixelCheckoutIntents(nextIntents);
  }

  window.PixelMirrorLatestCheckoutIntent = intent;

  return intent;
}

function updateMirrorAssociateCheckoutIntentStatus(status, label) {
  const intent = getMirrorAssociateLatestCheckoutIntent();

  if (!intent) {
    showToast?.("No checkout request is available yet.", "error");
    setStatus?.("No checkout request is available yet.", "error");
    return null;
  }

  const updatedIntent = {
    ...intent,
    status,
    associateStatus: status,
    associateStatusLabel: label,
    associateUpdatedAt: new Date().toISOString()
  };

  writeMirrorAssociateCheckoutIntent(updatedIntent);

  if (typeof addTryOnTimelineEvent === "function") {
    addTryOnTimelineEvent(
      "checkout",
      label,
      `${updatedIntent.anchorItem || "Customer look"} was updated by associate control.`,
      [
        updatedIntent.storeName || getMirrorShowroomDisplayStore?.() || "Current Store",
        formatPrice(updatedIntent.totalValue || 0),
        `${updatedIntent.itemCount || 0} item${updatedIntent.itemCount === 1 ? "" : "s"}`
      ]
    );
  }

  renderMirrorAssociateCustomerAssistance();

  const shell = document.getElementById("mirrorAssociateControl");
  const assistance = document.getElementById("mirrorAssociateCustomerAssistance");

  if (shell && assistance) {
    assistance.scrollIntoView({
      behavior: "smooth",
      block: "start"
    });
  }

  showToast?.(label, "success");
  setStatus?.(label, "success");

  speakPixelConcierge?.("command", {
    commandName: label
  });

  console.table({
    checkoutIntentId: updatedIntent.id,
    anchorItem: updatedIntent.anchorItem,
    status: updatedIntent.status,
    associateStatus: updatedIntent.associateStatus,
    associateStatusLabel: updatedIntent.associateStatusLabel,
    updatedAt: updatedIntent.associateUpdatedAt
  });

  return updatedIntent;
}

function clearMirrorAssociateCheckoutIntent() {
  const intent = getMirrorAssociateLatestCheckoutIntent();

  if (!intent) {
    showToast?.("No checkout request to clear.", "info");
    return [];
  }

  const existingIntents =
    typeof getPixelCheckoutIntents === "function"
      ? getPixelCheckoutIntents()
      : [];

  const nextIntents = existingIntents.filter(existing => existing.id !== intent.id);

  if (typeof writePixelCheckoutIntents === "function") {
    writePixelCheckoutIntents(nextIntents);
  }

  window.PixelMirrorLatestCheckoutIntent = nextIntents[0] || null;

  if (typeof addTryOnTimelineEvent === "function") {
    addTryOnTimelineEvent(
      "checkout",
      "Checkout request cleared",
      `${intent.anchorItem || "Customer look"} checkout request was cleared from associate control.`,
      [intent.storeName || getMirrorShowroomDisplayStore?.() || "Current Store"]
    );
  }

  renderMirrorAssociateCustomerAssistance();
  updateMirrorAssociateControlStats();

  showToast?.("Checkout request cleared.", "success");
  setStatus?.("Checkout request cleared.", "success");

  return nextIntents;
}

function getMirrorAssociateIntentStatusLabel(intent) {
  const status = String(intent?.associateStatus || intent?.status || "none").toLowerCase();

  if (status === "assisting") {
    return "Associate Assisting";
  }

  if (status === "ready-for-checkout") {
    return "Ready for Checkout";
  }

  if (status === "requested") {
    return "Customer Requested";
  }

  return "No Request";
}

function buildMirrorAssociateIntentItemsHtml(intent) {
  const items = Array.isArray(intent?.items) ? intent.items : [];

  if (!items.length) {
    return `
      <div class="mirror-associate-inline-empty">
        No checkout items are attached yet.
      </div>
    `;
  }

  return items.slice(0, 6).map(item => {
    const name = item.name || "Checkout Item";
    const role = item.role || item.category || "Item";
    const price = formatPrice(item.price || 0);
    const imageUrl = safeImageUrl(
      item.imageUrl,
      `https://placehold.co/120x120/f7f3ec/171411?text=${encodeURIComponent(name)}`
    );

    return `
      <article class="mirror-associate-activity-row">
        <img
          src="${escapeMirrorShowroomHtml(imageUrl)}"
          alt="${escapeMirrorShowroomHtml(name)}"
          onerror="this.src='https://placehold.co/120x120/f7f3ec/171411?text=Item';"
        >

        <div>
          <strong>${escapeMirrorShowroomHtml(name)}</strong>
          <span>${escapeMirrorShowroomHtml(role)} · ${escapeMirrorShowroomHtml(price)}</span>
        </div>
      </article>
    `;
  }).join("");
}

function buildMirrorAssociateRecommendationsHtml(intent) {
  const recommendations = Array.isArray(intent?.associateRecommendations)
    ? intent.associateRecommendations
    : [];

  if (!recommendations.length) {
    return `
      <div class="mirror-associate-inline-empty">
        Pixel recommendations will appear after a scan or complete look.
      </div>
    `;
  }

  return recommendations.slice(0, 4).map(recommendation => `
    <article class="mirror-associate-timeline-row">
      <strong>${escapeMirrorShowroomHtml(recommendation.title || "Associate recommendation")}</strong>
      <span>
        ${escapeMirrorShowroomHtml(recommendation.action || recommendation.detail || "Review customer context.")}
      </span>
    </article>
  `).join("");
}

function buildMirrorAssociateCustomerAssistanceHtml() {
  const intent = getMirrorAssociateLatestCheckoutIntent();

  if (!intent) {
    return `
      <section class="mirror-associate-actions" id="mirrorAssociateCustomerAssistance">
        <div class="mirror-associate-section-label">Customer Assistance</div>

        <div class="mirror-associate-inline-empty">
          No checkout request yet. When a customer taps Request Checkout, the latest look will appear here.
        </div>
      </section>
    `;
  }

  const rawStatus = String(intent.associateStatus || intent.status || "requested").toLowerCase();
  const statusLabel = getMirrorAssociateIntentStatusLabel(intent);

  const isRequested = rawStatus === "requested";
  const isAssisting = rawStatus === "assisting";
  const isReady = rawStatus === "ready-for-checkout";

  const storeName =
    intent.storeName ||
    getMirrorShowroomDisplayStore?.() ||
    "Current Store";

  const updatedText = intent.associateUpdatedAt
    ? `Updated ${formatSessionTime(intent.associateUpdatedAt)}`
    : `Created ${formatSessionTime(intent.createdAt)}`;

  return `
    <section class="mirror-associate-actions" id="mirrorAssociateCustomerAssistance">
      <div class="mirror-associate-section-label">Customer Assistance</div>

      <div class="mirror-associate-session">
        <div>
          <span>${escapeMirrorShowroomHtml(statusLabel)}</span>
          <strong>${escapeMirrorShowroomHtml(intent.anchorItem || "Complete Look")}</strong>
          <small>
            ${escapeMirrorShowroomHtml(storeName)}
            · ${escapeMirrorShowroomHtml(intent.vibe || "Casual")}
            · ${escapeMirrorShowroomHtml(updatedText)}
          </small>
        </div>

        <div class="mirror-associate-session-metrics">
          <div>
            <strong>${escapeMirrorShowroomHtml(formatPrice(intent.totalValue || 0))}</strong>
            <span>Total</span>
          </div>

          <div>
            <strong>${escapeMirrorShowroomHtml(String(intent.outfitScore || 0))}%</strong>
            <span>Score</span>
          </div>
        </div>
      </div>

      <div class="mirror-associate-inline-empty">
        ${
          isReady
            ? "Status: Look is prepared for checkout handoff."
            : isAssisting
              ? "Status: Associate is now helping this customer."
              : "Status: Customer requested checkout assistance."
        }
      </div>

      <section class="mirror-associate-inline-section">
        <h4>Checkout Items</h4>
        ${buildMirrorAssociateIntentItemsHtml(intent)}
      </section>

      <section class="mirror-associate-inline-section">
        <h4>Pixel Associate Recommendations</h4>
        ${buildMirrorAssociateRecommendationsHtml(intent)}
      </section>

      <div class="mirror-associate-actions">
        <button
          class="mirror-associate-action ${isAssisting ? "accent" : ""}"
          type="button"
          data-associate-intent-action="assisting"
          ${isAssisting ? "disabled" : ""}
        >
          <span class="mirror-associate-action-icon">${isAssisting ? "✓" : "○"}</span>
          <span>
            <strong>${isAssisting ? "Assisting Customer ✓" : "Mark as Assisting"}</strong>
            <small>
              ${isAssisting ? "This request is currently being handled" : "Associate is helping this customer"}
            </small>
          </span>
          <em>›</em>
        </button>

        <button
          class="mirror-associate-action ${isReady ? "accent" : ""}"
          type="button"
          data-associate-intent-action="ready"
          ${isReady ? "disabled" : ""}
        >
          <span class="mirror-associate-action-icon">${isReady ? "✓" : "◇"}</span>
          <span>
            <strong>${isReady ? "Ready for Checkout ✓" : "Ready for Checkout"}</strong>
            <small>
              ${isReady ? "Look is ready for payment handoff" : "Look is prepared for payment handoff"}
            </small>
          </span>
          <em>›</em>
        </button>

        <button class="mirror-associate-action" type="button" data-associate-intent-action="clear">
          <span class="mirror-associate-action-icon">×</span>
          <span>
            <strong>Clear Request</strong>
            <small>Remove this checkout intent from the queue</small>
          </span>
          <em>›</em>
        </button>
      </div>
    </section>
  `;
}

function renderMirrorAssociateCustomerAssistance() {
  const existing = document.getElementById("mirrorAssociateCustomerAssistance");

  if (!existing) {
    return false;
  }

  existing.outerHTML = buildMirrorAssociateCustomerAssistanceHtml();

  const section = document.getElementById("mirrorAssociateCustomerAssistance");

  section?.querySelectorAll("[data-associate-intent-action]").forEach(button => {
    button.addEventListener("click", () => {
      handleMirrorAssociateIntentAction(button.dataset.associateIntentAction || "");
    });
  });

  return true;
}

function handleMirrorAssociateIntentAction(action) {
  const normalizedAction = String(action || "").trim().toLowerCase();

  if (normalizedAction === "assisting") {
    return updateMirrorAssociateCheckoutIntentStatus(
      "assisting",
      "Associate assisting checkout request"
    );
  }

  if (normalizedAction === "ready") {
    return updateMirrorAssociateCheckoutIntentStatus(
      "ready-for-checkout",
      "Look ready for checkout"
    );
  }

  if (normalizedAction === "clear") {
    return clearMirrorAssociateCheckoutIntent();
  }

  return null;
}

function buildMirrorAssociateControl() {
  const existing = document.getElementById("mirrorAssociateControl");

  if (existing) {
    return existing;
  }

  const stats = getMirrorAssociateSessionStats();
  const storeName = escapeMirrorShowroomHtml(stats.storeName);

  const shell = document.createElement("section");
  shell.id = "mirrorAssociateControl";
  shell.className = "mirror-associate-control";
  shell.setAttribute("aria-label", "Associate control panel");

  shell.innerHTML = `
    <button
      class="mirror-associate-trigger"
      type="button"
      id="mirrorAssociateTriggerBtn"
      aria-label="Open associate controls"
      aria-expanded="false"
    >
      <span class="mirror-associate-trigger-icon">⚙</span>
      <span>Associate</span>
    </button>

    <button
      class="mirror-associate-backdrop"
      type="button"
      id="mirrorAssociateBackdrop"
      aria-label="Close associate controls"
    ></button>

    <aside class="mirror-associate-panel" aria-label="Associate Control">
      <div class="mirror-associate-panel-line" aria-hidden="true"></div>

      <header class="mirror-associate-header">
        <div>
          <div class="mirror-associate-kicker">
            <span class="mirror-associate-live-dot"></span>
            Universal Stylist
          </div>

          <h2>Associate Control</h2>

          <p>${storeName} · Mirror Station</p>
        </div>

        <button
          class="mirror-associate-close"
          type="button"
          id="mirrorAssociateCloseBtn"
          aria-label="Close associate controls"
        >
          ×
        </button>
      </header>

      <section class="mirror-associate-session">
        <div>
          <span>Current Session</span>
          <strong>Guest · Walk-in Customer</strong>
        </div>

        <div class="mirror-associate-session-metrics">
          <div>
            <strong id="mirrorAssociateScanCount">${stats.scansCount}</strong>
            <span>Scans</span>
          </div>

          <div>
            <strong id="mirrorAssociateInterestValue">${escapeMirrorShowroomHtml(stats.interestLabel)}</strong>
            <span>Interest</span>
          </div>
        </div>
      </section>

      ${buildMirrorAssociateCustomerAssistanceHtml()}

      <section class="mirror-associate-actions">
        <div class="mirror-associate-section-label">Quick Actions</div>

        <button class="mirror-associate-action accent" type="button" data-associate-action="associate-mirror">
          <span class="mirror-associate-action-icon">▣</span>
          <span>
            <strong>Continue in Associate Mirror</strong>
            <small>Switch to full associate view</small>
          </span>
          <em>›</em>
        </button>

        <button class="mirror-associate-action" type="button" data-associate-action="dashboard">
          <span class="mirror-associate-action-icon">◫</span>
          <span>
            <strong>Open Dashboard</strong>
            <small>Sales, sessions, and analytics</small>
          </span>
          <em>›</em>
        </button>

        <button class="mirror-associate-action" type="button" data-associate-action="inventory">
          <span class="mirror-associate-action-icon">▤</span>
          <span>
            <strong>Open Inventory</strong>
            <small>Browse and manage stock</small>
          </span>
          <em>›</em>
        </button>

        <button class="mirror-associate-action" type="button" data-associate-action="activity">
          <span class="mirror-associate-action-icon">⌁</span>
          <span>
            <strong>View Activity</strong>
            <small>Recent mirror session log</small>
          </span>
          <em>›</em>
        </button>

        <button class="mirror-associate-action" type="button" data-associate-action="debug">
          <span class="mirror-associate-action-icon">⌘</span>
          <span>
            <strong>Debug / Health Check</strong>
            <small>Run Pixel mirror diagnostics</small>
          </span>
          <em>›</em>
        </button>

        <button class="mirror-associate-action" type="button" data-associate-action="admin">
          <span class="mirror-associate-action-icon">◇</span>
          <span>
            <strong>Admin Controls</strong>
            <small>Settings and configuration</small>
          </span>
          <em>›</em>
        </button>
      </section>

      <footer class="mirror-associate-footer">
        <button
          class="mirror-associate-return"
          type="button"
          data-associate-action="customer-mirror"
        >
          ← Return to Customer Mirror
        </button>

        <p>Mirror will resume customer mode automatically.</p>
      </footer>
    </aside>
  `;

  document.body.appendChild(shell);
  bindMirrorAssociateControlEvents(shell);
  updateMirrorAssociateControlStats();

  return shell;
}

function updateMirrorAssociateControlStats() {
  const stats = getMirrorAssociateSessionStats();

  const scanCount = document.getElementById("mirrorAssociateScanCount");
  const interestValue = document.getElementById("mirrorAssociateInterestValue");

  if (scanCount) {
    scanCount.textContent = String(stats.scansCount);
  }

  if (interestValue) {
    interestValue.textContent = stats.interestLabel;
  }

  if (typeof renderMirrorAssociateCustomerAssistance === "function") {
    renderMirrorAssociateCustomerAssistance();
  }
}

function showMirrorAssociateControl() {
  const shell = buildMirrorAssociateControl();

  updateMirrorAssociateControlStats();

  shell.classList.add("is-open");
  document.body.classList.add("mirror-associate-control-open");

  const trigger = document.getElementById("mirrorAssociateTriggerBtn");
  trigger?.setAttribute("aria-expanded", "true");

  if (typeof speakPixelConcierge === "function") {
    speakPixelConcierge("command", {
      commandName: "Associate Control"
    });
  }

  return true;
}

function hideMirrorAssociateControl() {
  const shell = document.getElementById("mirrorAssociateControl");

  if (!shell) {
    return false;
  }

  shell.classList.remove("is-open");
  document.body.classList.remove("mirror-associate-control-open");

  const trigger = document.getElementById("mirrorAssociateTriggerBtn");
  trigger?.setAttribute("aria-expanded", "false");

  return true;
}

function toggleMirrorAssociateControl() {
  const shell = buildMirrorAssociateControl();

  if (shell.classList.contains("is-open")) {
    return hideMirrorAssociateControl();
  }

  return showMirrorAssociateControl();
}

function getMirrorAssociateRouteContext() {
  const runtime =
    typeof getMirrorRuntime === "function"
      ? getMirrorRuntime()
      : window.MirrorRuntimeState || {};

  return {
    retailerKey:
      runtime.retailerKey ||
      getSelectedRetailerKey?.() ||
      localStorage.getItem("currentRetailerKey") ||
      localStorage.getItem("retailerKey") ||
      "MACY001",

    storeCode:
      runtime.storeCode ||
      getSelectedStoreCode?.() ||
      localStorage.getItem("currentStoreCode") ||
      localStorage.getItem("storeCode") ||
      "",

    storeName:
      runtime.storeName ||
      getMirrorShowroomDisplayStore?.() ||
      getMirrorStoreDisplayName?.() ||
      localStorage.getItem("currentStoreName") ||
      localStorage.getItem("storeName") ||
      "Current Store",

    vibe:
      runtime.vibe ||
      document.getElementById("vibeSelect")?.value ||
      "Casual"
  };
}

function buildMirrorAssociateUrl(path, extraParams = {}) {
  const context = getMirrorAssociateRouteContext();
  const params = new URLSearchParams();

  params.set("retailer", context.retailerKey);

  if (context.storeCode) {
    params.set("storeCode", context.storeCode);
  }

  if (context.storeName) {
    params.set("storeName", context.storeName);
  }

  if (context.vibe) {
    params.set("vibe", context.vibe);
  }

  Object.entries(extraParams).forEach(([key, value]) => {
    const cleanValue = String(value ?? "").trim();

    if (cleanValue) {
      params.set(key, cleanValue);
    }
  });

  return `${path}?${params.toString()}`;
}

function navigateMirrorAssociateTo(path, label, extraParams = {}) {
  const url = buildMirrorAssociateUrl(path, extraParams);

  hideMirrorAssociateControl();

  if (typeof setStatus === "function") {
    setStatus(`${label} opened. Customer mirror session context preserved.`, "ready");
  }

  if (typeof showToast === "function") {
    showToast(`${label} opened.`, "info");
  }

  if (typeof speakPixelConcierge === "function") {
    speakPixelConcierge("command", {
      commandName: label
    });
  }

  window.location.href = url;
  return true;
}

function openAssociateMirrorMode(label = "Associate Mirror") {
  hideMirrorAssociateControl();

  if (typeof hideMirrorMainExperience === "function") {
    hideMirrorMainExperience();
  }

  document.body.classList.remove("mirror-main-active");

  window.requestAnimationFrame(() => {
    window.scrollTo({
      top: 0,
      behavior: "smooth"
    });
  });

  if (typeof setStatus === "function") {
    setStatus(`${label} opened. Customer mirror is paused.`, "ready");
  }

  if (typeof showToast === "function") {
    showToast(`${label} opened.`, "info");
  }

  if (typeof speakPixelConcierge === "function") {
    speakPixelConcierge("command", {
      commandName: label
    });
  }

  return true;
}

function buildMirrorAssociateActivityHtml() {
  const session =
    typeof readTryOnSession === "function"
      ? readTryOnSession()
      : {
          scans: [],
          timeline: [],
          looksCreated: 0,
          savesToBag: 0
        };

  const scans = Array.isArray(session.scans) ? session.scans : [];
  const timeline = Array.isArray(session.timeline) ? session.timeline : [];
  const storeName =
    session.storeName ||
    getMirrorShowroomDisplayStore?.() ||
    "Current Store";

  const totalInterest = scans.reduce((sum, scan) => {
    return sum + safeNumber(scan.price || 0);
  }, 0);

  const scanRows = scans.slice(0, 6).map(scan => {
    const name = scan.name || "Scanned Item";
    const category = scan.category || "Item";
    const vibe = scan.vibe || "Casual";
    const price = formatPrice(scan.price || 0);
    const imageUrl = safeImageUrl(
      scan.imageUrl,
      `https://placehold.co/120x120/f7f3ec/171411?text=${encodeURIComponent(name)}`
    );

    return `
      <article class="mirror-associate-activity-row">
        <img
          src="${escapeMirrorShowroomHtml(imageUrl)}"
          alt="${escapeMirrorShowroomHtml(name)}"
          onerror="this.src='https://placehold.co/120x120/f7f3ec/171411?text=Scan';"
        >

        <div>
          <strong>${escapeMirrorShowroomHtml(name)}</strong>
          <span>${escapeMirrorShowroomHtml(category)} · ${escapeMirrorShowroomHtml(vibe)} · ${escapeMirrorShowroomHtml(price)}</span>
        </div>
      </article>
    `;
  }).join("");

  const timelineRows = timeline.slice(0, 8).map(event => {
    return `
      <article class="mirror-associate-timeline-row">
        <strong>${escapeMirrorShowroomHtml(event.title || "Mirror event")}</strong>
        ${
          event.detail
            ? `<span>${escapeMirrorShowroomHtml(event.detail)}</span>`
            : ""
        }
      </article>
    `;
  }).join("");

  return `
    <div class="mirror-associate-inline-view">
      <header class="mirror-associate-inline-head">
        <div>
          <p>Associate Activity</p>
          <h3>${escapeMirrorShowroomHtml(storeName)}</h3>
        </div>

        <button class="mirror-associate-inline-close" type="button" id="mirrorAssociateInlineCloseBtn">
          ×
        </button>
      </header>

      <section class="mirror-associate-inline-stats">
        <div>
          <span>Scans</span>
          <strong>${scans.length}</strong>
        </div>

        <div>
          <span>Looks</span>
          <strong>${safeNumber(session.looksCreated || 0)}</strong>
        </div>

        <div>
          <span>Saves</span>
          <strong>${safeNumber(session.savesToBag || 0)}</strong>
        </div>

        <div>
          <span>Interest</span>
          <strong>${formatPrice(totalInterest)}</strong>
        </div>
      </section>

      <section class="mirror-associate-inline-section">
        <h4>Recent Scans</h4>
        ${
          scanRows ||
          `<div class="mirror-associate-inline-empty">No scans recorded yet.</div>`
        }
      </section>

      <section class="mirror-associate-inline-section">
        <h4>Session Timeline</h4>
        ${
          timelineRows ||
          `<div class="mirror-associate-inline-empty">No timeline activity yet.</div>`
        }
      </section>
    </div>
  `;
}

function showMirrorAssociateInlineActivity() {
  const shell = buildMirrorAssociateControl();
  const panel = shell.querySelector(".mirror-associate-panel");

  if (!panel) {
    return false;
  }

  panel.innerHTML = buildMirrorAssociateActivityHtml();

  shell.classList.add("is-open");
  document.body.classList.add("mirror-associate-control-open");

  panel.querySelector("#mirrorAssociateInlineCloseBtn")?.addEventListener("click", () => {
    shell.remove();
    buildMirrorAssociateControl();
    showMirrorAssociateControl();
  });

  if (typeof setStatus === "function") {
    setStatus("Associate activity view opened.", "ready");
  }

  if (typeof showToast === "function") {
    showToast("Activity view opened.", "info");
  }

  return true;
}

function runMirrorAssociateSafeDiagnostic(label, callback) {
  try {
    const result = typeof callback === "function" ? callback() : null;

    return {
      label,
      ok: true,
      result,
      error: ""
    };
  } catch (error) {
    console.warn(`Associate diagnostic failed: ${label}`, error);

    return {
      label,
      ok: false,
      result: null,
      error: error?.message || "Diagnostic failed."
    };
  }
}

function runMirrorAssociateDebugHealth() {
  const diagnostics = [
    runMirrorAssociateSafeDiagnostic("Pixel Mirror Debug", () => {
      return window.PixelMirrorDebug?.health?.() || null;
    }),

    runMirrorAssociateSafeDiagnostic("Main Experience", () => {
      return window.MirrorMainExperience?.health?.({
        autoBuild: true
      }) || null;
    }),

    runMirrorAssociateSafeDiagnostic("Runtime Context", () => {
      return window.MirrorRuntime?.health?.() || null;
    }),

    runMirrorAssociateSafeDiagnostic("Customer Journey", () => {
      return window.MirrorCustomerJourney?.health?.() || null;
    }),

    runMirrorAssociateSafeDiagnostic("Checkout Intent", () => {
      return typeof getMirrorAssociateLatestCheckoutIntent === "function"
        ? getMirrorAssociateLatestCheckoutIntent()
        : null;
    }),

    runMirrorAssociateSafeDiagnostic("Inventory Payload", () => {
      return window.MirrorAssociateInventoryPayload || null;
    })
  ];

  const mirrorDiagnostic = diagnostics.find(item => item.label === "Pixel Mirror Debug");
  const mainDiagnostic = diagnostics.find(item => item.label === "Main Experience");
  const runtimeDiagnostic = diagnostics.find(item => item.label === "Runtime Context");
  const journeyDiagnostic = diagnostics.find(item => item.label === "Customer Journey");
  const checkoutDiagnostic = diagnostics.find(item => item.label === "Checkout Intent");
  const inventoryDiagnostic = diagnostics.find(item => item.label === "Inventory Payload");

  const report = {
    checkedAt: new Date().toISOString(),
    healthy: diagnostics.every(item => item.ok),
    diagnostics,
    mirrorReport: mirrorDiagnostic?.result || null,
    mainReport: mainDiagnostic?.result || null,
    runtimeReport: runtimeDiagnostic?.result || null,
    customerJourneyReport: journeyDiagnostic?.result || null,
    checkoutIntent: checkoutDiagnostic?.result || null,
    inventoryPayload: inventoryDiagnostic?.result || null,
    storeName:
      getMirrorShowroomDisplayStore?.() ||
      getMirrorStoreDisplayName?.() ||
      "Current Store",
    customerStage:
      document.body.dataset.mirrorCustomerStage ||
      "unknown",
    associateDrawerOpen:
      !!document.getElementById("mirrorAssociateControl")?.classList.contains("is-open")
  };

  console.group(
    report.healthy
      ? "✅ Associate Debug / Health Check Passed"
      : "⚠️ Associate Debug / Health Check Needs Attention"
  );

  console.table(
    diagnostics.map(item => ({
      check: item.label,
      ok: item.ok,
      hasResult: !!item.result,
      error: item.error || ""
    }))
  );

  console.log("Full associate diagnostic report:", report);
  console.groupEnd();

  if (typeof showToast === "function") {
    showToast(
      report.healthy
        ? "Debug health check completed."
        : "Debug health check completed with warnings.",
      report.healthy ? "success" : "info"
    );
  }

  if (typeof setStatus === "function") {
    setStatus(
      report.healthy
        ? "Debug health check completed."
        : "Debug health check completed with warnings. See console.",
      report.healthy ? "success" : "ready"
    );
  }

  return report;
}

function buildMirrorAssociateDebugHealthHtml(report) {
  const checkedAt = report?.checkedAt
    ? formatSessionTime(report.checkedAt)
    : "Just now";

  const diagnostics = Array.isArray(report?.diagnostics)
    ? report.diagnostics
    : [];

  const healthy = diagnostics.length
    ? diagnostics.every(item => item.ok)
    : !!report?.healthy;

  const inventoryItems = Array.isArray(report?.inventoryPayload?.items)
    ? report.inventoryPayload.items.length
    : Array.isArray(window.MirrorAssociateInventoryPayload?.items)
      ? window.MirrorAssociateInventoryPayload.items.length
      : 0;

  const checkoutIntent = report?.checkoutIntent || null;

  const rowsHtml = diagnostics.length
    ? diagnostics.map(item => `
        <article class="mirror-associate-timeline-row">
          <strong>${item.ok ? "✓" : "○"} ${escapeMirrorShowroomHtml(item.label)}</strong>
          <span>
            ${
              item.ok
                ? item.result
                  ? "Diagnostic responded successfully."
                  : "Diagnostic available, no active data returned."
                : escapeMirrorShowroomHtml(item.error || "Diagnostic failed.")
            }
          </span>
        </article>
      `).join("")
    : `
      <article class="mirror-associate-timeline-row">
        <strong>○ No diagnostics available</strong>
        <span>The health check runner did not return diagnostic rows.</span>
      </article>
    `;

  return `
    <div class="mirror-associate-inline-view">
      <header class="mirror-associate-inline-head">
        <div>
          <p>Debug / Health Check</p>
          <h3>Mirror Diagnostics</h3>
        </div>

        <button
          class="mirror-associate-inline-close"
          type="button"
          id="mirrorAssociateInlineCloseBtn"
          aria-label="Close debug health check"
        >
          ×
        </button>
      </header>

      <section class="mirror-associate-inline-stats">
        <div>
          <span>Status</span>
          <strong>${healthy ? "Passed" : "Review"}</strong>
        </div>

        <div>
          <span>Time</span>
          <strong>${escapeMirrorShowroomHtml(checkedAt)}</strong>
        </div>

        <div>
          <span>Store</span>
          <strong>${escapeMirrorShowroomHtml(report?.storeName || getMirrorShowroomDisplayStore?.() || "Store")}</strong>
        </div>

        <div>
          <span>Mode</span>
          <strong>${escapeMirrorShowroomHtml(report?.customerStage || "Mirror")}</strong>
        </div>
      </section>

      <section class="mirror-associate-inline-section">
        <h4>Health Results</h4>
        ${rowsHtml}
      </section>

      <section class="mirror-associate-inline-section">
        <h4>Live Context</h4>

        <article class="mirror-associate-timeline-row">
          <strong>${checkoutIntent ? "✓ Checkout request available" : "○ No checkout request"}</strong>
          <span>
            ${
              checkoutIntent
                ? escapeMirrorShowroomHtml(`${checkoutIntent.anchorItem || "Complete Look"} · ${formatPrice(checkoutIntent.totalValue || 0)}`)
                : "No active checkout intent is currently queued."
            }
          </span>
        </article>

        <article class="mirror-associate-timeline-row">
          <strong>${inventoryItems ? "✓ Inventory payload available" : "○ Inventory not loaded"}</strong>
          <span>
            ${
              inventoryItems
                ? `${inventoryItems} inventory item${inventoryItems === 1 ? "" : "s"} available in the associate cache.`
                : "Open Associate Inventory to sync live backend inventory."
            }
          </span>
        </article>

        <article class="mirror-associate-timeline-row">
          <strong>Console report logged</strong>
          <span>Open the browser console to inspect the full diagnostic object.</span>
        </article>
      </section>
    </div>
  `;
}

function showMirrorAssociateInlineDebugHealth() {
  const shell = buildMirrorAssociateControl();
  const panel = shell.querySelector(".mirror-associate-panel");

  if (!panel) {
    return false;
  }

  shell.classList.add("is-open");
  document.body.classList.add("mirror-associate-control-open");

  panel.innerHTML = `
    <div class="mirror-associate-inline-view">
      <header class="mirror-associate-inline-head">
        <div>
          <p>Debug / Health Check</p>
          <h3>Running Diagnostics...</h3>
        </div>

        <button
          class="mirror-associate-inline-close"
          type="button"
          id="mirrorAssociateInlineCloseBtn"
          aria-label="Close debug health check"
        >
          ×
        </button>
      </header>

      <div class="mirror-associate-inline-empty">
        Pixel is checking mirror health, runtime context, customer journey, checkout intent, and inventory payload.
      </div>
    </div>
  `;

  const closeInlineDebug = () => {
    shell.remove();
    buildMirrorAssociateControl();
    showMirrorAssociateControl();
  };

  panel.querySelector("#mirrorAssociateInlineCloseBtn")?.addEventListener("click", closeInlineDebug);

  window.setTimeout(() => {
    const report = runMirrorAssociateDebugHealth();

    panel.innerHTML = buildMirrorAssociateDebugHealthHtml(report);

    panel.querySelector("#mirrorAssociateInlineCloseBtn")?.addEventListener("click", closeInlineDebug);

    if (typeof addTryOnTimelineEvent === "function") {
      addTryOnTimelineEvent(
        "debug",
        "Associate health check opened",
        "Associate opened the visible mirror diagnostic health panel.",
        [getMirrorShowroomDisplayStore?.() || "Current Store"]
      );
    }
  }, 80);

  return true;
}

function openMirrorAssociateAdminControls() {
  const report = runMirrorAssociateDebugHealth();

  if (typeof openShortcutHelp === "function") {
    openShortcutHelp();
  }

  if (typeof setStatus === "function") {
    setStatus("Admin controls opened. Shortcut/help panel is available.", "ready");
  }

  if (typeof showToast === "function") {
    showToast("Admin controls opened.", "info");
  }

  return report;
}

async function syncMirrorAssociateInventoryFromBackend() {
  const runtime =
    typeof getMirrorRuntime === "function"
      ? getMirrorRuntime()
      : window.MirrorRuntimeState || {};

  const retailerKey = String(
    runtime.retailerKey ||
      getSelectedRetailerKey?.() ||
      ""
  ).trim();

  const storeCode = String(
    runtime.storeCode ||
      getSelectedStoreCode?.() ||
      ""
  ).trim();

  if (!retailerKey || !storeCode) {
    console.warn("Associate inventory sync skipped: missing retailer/store context.", {
      retailerKey,
      storeCode
    });

    return {
      ok: false,
      source: "missing-context",
      items: []
    };
  }

  const params = new URLSearchParams({
    retailerKey,
    storeCode,
    page: "0",
    size: "50"
  });

  const endpoint = `/api/v1/macy-stylist/associate/inventory?${params.toString()}`;

  try {
    const response = await fetch(endpoint, {
      method: "GET",
      headers: getAuthHeaders({
        Accept: "application/json"
      }),
      credentials: "same-origin"
    });

    if (!response.ok) {
      const errorText = await response.text().catch(() => "");
      throw new Error(`Inventory sync failed ${response.status}: ${errorText || response.statusText}`);
    }

    const payload = await response.json();

    const rawItems = Array.isArray(payload?.items)
      ? payload.items
      : Array.isArray(payload)
        ? payload
        : [];

    const items = rawItems.map(item => {
      const rfid =
        item.rfid ||
        item.itemRfid ||
        item.productRfid ||
        item.sku ||
        item.id ||
        "";

      const name =
        item.itemName ||
        item.name ||
        item.productName ||
        item.title ||
        "Inventory Item";

      const stockValue =
        item.stockQuantity ??
        item.availableQuantity ??
        item.quantityAvailable ??
        item.onHand ??
        item.stock ??
        item.quantity ??
        item.inventory ??
        null;

      const stock =
        stockValue === null || stockValue === undefined || stockValue === ""
          ? null
          : Number(stockValue);

      return {
        ...item,
        id: item.id || rfid || crypto.randomUUID(),
        rfid,
        itemRfid: item.itemRfid || rfid,
        name,
        itemName: item.itemName || name,
        stock: Number.isFinite(stock) ? stock : null,
        stockQuantity: Number.isFinite(stock) ? stock : item.stockQuantity ?? null,
        retailerKey: item.retailerKey || retailerKey,
        retailerConfigKey: runtime.retailerConfigKey || "",
        storeCode: item.storeCode || storeCode,
        storeName:
          item.storeName ||
          runtime.storeName ||
          getMirrorShowroomDisplayStore?.() ||
          getMirrorStoreDisplayName?.() ||
          "",
        source: "backend-associate-inventory"
      };
    });

    const normalizedPayload = {
      source: "backend-associate-inventory",
      syncedAt: new Date().toISOString(),
      retailerKey,
      retailerConfigKey: runtime.retailerConfigKey || "",
      storeCode,
      storeName:
        runtime.storeName ||
        getMirrorShowroomDisplayStore?.() ||
        getMirrorStoreDisplayName?.() ||
        "",
      totalItems: Number(payload?.totalItems ?? items.length),
      page: Number(payload?.page ?? 0),
      size: Number(payload?.size ?? items.length),
      totalPages: Number(payload?.totalPages ?? 1),
      items
    };

    localStorage.setItem("merchantInventory", JSON.stringify(normalizedPayload));
    localStorage.setItem("mirrorAssociateInventoryPayload", JSON.stringify(normalizedPayload));

    window.MirrorAssociateInventoryPayload = normalizedPayload;

    console.groupCollapsed("Mirror Associate Inventory Synced");
    console.log("endpoint", endpoint);
    console.table({
      retailerKey,
      storeCode,
      storeName: normalizedPayload.storeName,
      totalItems: normalizedPayload.totalItems,
      storedItems: items.length
    });
    console.table(
      items.map(item => ({
        rfid: item.rfid,
        name: item.itemName || item.name,
        category: item.category,
        price: item.price,
        stock: item.stock,
        stockQuantity: item.stockQuantity,
        lowStock: item.lowStock,
        outOfStock: item.outOfStock,
        available: item.available,
        active: item.active,
        source: item.source
      }))
    );
    console.groupEnd();

    return {
      ok: true,
      source: "backend",
      items,
      payload: normalizedPayload
    };
  } catch (error) {
    console.warn("Backend associate inventory sync failed. Falling back to cached/local inventory.", error);

    return {
      ok: false,
      source: "backend-error",
      error,
      items: []
    };
  }
}

function getMirrorAssociateStoredInventory() {
  const runtime =
    typeof getMirrorRuntime === "function"
      ? getMirrorRuntime()
      : window.MirrorRuntimeState || {};

  const retailerKey = String(
    runtime.retailerKey ||
      getSelectedRetailerKey?.() ||
      ""
  ).toLowerCase();

  const retailerConfigKey = String(
    runtime.retailerConfigKey ||
      ""
  ).toLowerCase();

  const storeCode = String(
    runtime.storeCode ||
      getSelectedStoreCode?.() ||
      ""
  ).toLowerCase();

  const storeName = String(
    runtime.storeName ||
      getMirrorShowroomDisplayStore?.() ||
      getMirrorStoreDisplayName?.() ||
      ""
  ).toLowerCase();

  const inventoryStorageKeys = [
    "merchantInventory",
    "universalStylistInventory",
    "retailerInventory",
    "currentMerchantInventory",
    "inventoryItems",
    "storeInventory"
  ];

  const normalizeInventoryItem = (item, source = "inventory") => {
    if (!item || typeof item !== "object") {
      return null;
    }

    const name =
      item.itemName ||
      item.name ||
      item.productName ||
      item.title ||
      item.product?.name ||
      item.inventoryItem?.name ||
      "Inventory Item";

    const rfid =
      item.rfid ||
      item.itemRfid ||
      item.productRfid ||
      item.sku ||
      item.id ||
      item.product?.rfid ||
      item.inventoryItem?.rfid ||
      "";

    const knownStockValue =
      item.stock ??
      item.quantity ??
      item.inventory ??
      item.availableQuantity ??
      item.stockQuantity ??
      item.onHand ??
      item.product?.stock ??
      item.inventoryItem?.stock ??
      null;

    return {
      ...item,
      id: item.id || rfid || crypto.randomUUID(),
      name,
      itemName: item.itemName || name,
      rfid,
      itemRfid: item.itemRfid || rfid,

      /*
        Important:
        Do not invent stock for checkout/session/outfit fallback records.
        If the item does not carry real stock, keep it null so the UI says
        “Stock unknown” instead of falsely saying “1 low stock”.
      */
      stock: knownStockValue,

      retailerKey:
        item.retailerKey ||
        item.retailer ||
        item.retailerCode ||
        item.tenantKey ||
        runtime.retailerKey ||
        "",
      retailerConfigKey:
        item.retailerConfigKey ||
        runtime.retailerConfigKey ||
        "",
      storeCode:
        item.storeCode ||
        item.currentStoreCode ||
        item.locationCode ||
        runtime.storeCode ||
        "",
      storeName:
        item.storeName ||
        item.locationName ||
        runtime.storeName ||
        "",
      source
    };
  };

  const flattenInventoryPayload = payload => {
    if (!payload) {
      return [];
    }

    if (Array.isArray(payload)) {
      return payload;
    }

    if (Array.isArray(payload.items)) {
      return payload.items;
    }

    if (Array.isArray(payload.inventory)) {
      return payload.inventory;
    }

    if (Array.isArray(payload.products)) {
      return payload.products;
    }

    if (Array.isArray(payload.data)) {
      return payload.data;
    }

    return [];
  };

  const storageItems = inventoryStorageKeys.flatMap(key => {
    const payload = safeParseJson(localStorage.getItem(key));

    return flattenInventoryPayload(payload).map(item =>
      normalizeInventoryItem(item, key)
    );
  });

  const checkoutIntent =
    typeof getMirrorAssociateLatestCheckoutIntent === "function"
      ? getMirrorAssociateLatestCheckoutIntent()
      : null;

  const checkoutItems = Array.isArray(checkoutIntent?.items)
    ? checkoutIntent.items.map(item =>
        normalizeInventoryItem(
          {
            ...item,
            category: item.category || item.role || "Checkout Item"
          },
          "checkout-intent"
        )
      )
    : [];

  const outfitItems =
    currentMirrorMainFullOutfit && typeof getMirrorMainOutfitPieces === "function"
      ? getMirrorMainOutfitPieces(currentMirrorMainFullOutfit).map(([role, item]) =>
          normalizeInventoryItem(
            {
              ...item,
              category:
                getItemField(item, "category", "productType", "type") ||
                getMirrorMainOutfitPieceLabel(role)
            },
            "current-outfit"
          )
        )
      : [];

  const session =
    typeof readTryOnSession === "function"
      ? readTryOnSession()
      : null;

  const scanItems = Array.isArray(session?.scans)
    ? session.scans.map(scan =>
        normalizeInventoryItem(
          {
            ...scan
          },
          "session-scan"
        )
      )
    : [];

  const savedLooks = safeParseJson(localStorage.getItem("pixelMirrorSavedLooks")) || [];

  const savedLookItems = Array.isArray(savedLooks)
    ? savedLooks.flatMap(savedLook => {
        const outfit = savedLook.fullOutfit;

        if (
          !outfit ||
          typeof outfit !== "object" ||
          typeof getMirrorMainOutfitPieces !== "function"
        ) {
          return [];
        }

        return getMirrorMainOutfitPieces(outfit).map(([role, item]) =>
          normalizeInventoryItem(
            {
              ...item,
              category:
                getItemField(item, "category", "productType", "type") ||
                getMirrorMainOutfitPieceLabel(role)
            },
            "saved-look"
          )
        );
      })
    : [];

  const allItems = [
    ...storageItems,
    ...checkoutItems,
    ...outfitItems,
    ...scanItems,
    ...savedLookItems
  ].filter(Boolean);

  const filteredItems = allItems.filter(item => {
    const itemRetailerKey = String(
      item.retailerKey ||
        item.retailer ||
        item.retailerCode ||
        item.tenantKey ||
        ""
    ).toLowerCase();

    const itemRetailerConfigKey = String(
      item.retailerConfigKey ||
        ""
    ).toLowerCase();

    const itemStoreCode = String(
      item.storeCode ||
        item.currentStoreCode ||
        item.locationCode ||
        ""
    ).toLowerCase();

    const itemStoreName = String(
      item.storeName ||
        item.locationName ||
        ""
    ).toLowerCase();

    const matchesRetailer =
      !retailerKey ||
      !itemRetailerKey ||
      itemRetailerKey === retailerKey ||
      itemRetailerKey === retailerConfigKey ||
      itemRetailerConfigKey === retailerConfigKey ||
      itemRetailerConfigKey === retailerKey ||
      itemRetailerKey.includes("nicks") ||
      itemRetailerKey.includes("boutique");

    const matchesStore =
      !storeCode ||
      !itemStoreCode ||
      itemStoreCode === storeCode ||
      itemStoreName === storeName ||
      itemStoreName.includes(storeName) ||
      storeName.includes(itemStoreName);

    return matchesRetailer && matchesStore;
  });

  const unique = new Map();

  filteredItems.forEach(item => {
    const key = String(
      item.rfid ||
        item.itemRfid ||
        item.productRfid ||
        item.sku ||
        item.id ||
        item.name ||
        item.itemName ||
        crypto.randomUUID()
    ).toLowerCase();

    if (!unique.has(key)) {
      unique.set(key, item);
      return;
    }

    const existing = unique.get(key);

    const existingStock = getMirrorAssociateInventoryStock(existing);
    const incomingStock = getMirrorAssociateInventoryStock(item);

    /*
      Prefer real known inventory stock over unknown fallback records.
      This prevents checkout/session items from overwriting real app inventory.
    */
    const resolvedStock =
      incomingStock !== null
        ? incomingStock
        : existingStock !== null
          ? existingStock
          : null;

    unique.set(key, {
      ...existing,
      ...item,
      stock: resolvedStock,
      imageUrl:
        item.imageUrl ||
        item.image ||
        existing.imageUrl ||
        existing.image ||
        "",
      source:
        existing.source === item.source
          ? existing.source
          : `${existing.source}, ${item.source}`
    });
  });

  return Array.from(unique.values());
}

function getMirrorAssociateInventoryName(item) {
  return (
    item.itemName ||
    item.name ||
    item.productName ||
    item.title ||
    item.product?.name ||
    item.inventoryItem?.name ||
    "Inventory Item"
  );
}

function getMirrorAssociateInventoryRfid(item) {
  return (
    item.rfid ||
    item.itemRfid ||
    item.productRfid ||
    item.sku ||
    item.product?.rfid ||
    item.inventoryItem?.rfid ||
    ""
  );
}

function getMirrorAssociateInventoryCategory(item) {
  return (
    item.category ||
    item.productType ||
    item.type ||
    item.product?.category ||
    item.inventoryItem?.category ||
    "Item"
  );
}

function getMirrorAssociateInventoryPrice(item) {
  return safeNumber(
    item.price ||
      item.salePrice ||
      item.retailPrice ||
      item.product?.price ||
      item.inventoryItem?.price ||
      0,
    0
  );
}

function getMirrorAssociateInventoryStock(item) {
  if (!item || typeof item !== "object") {
    return null;
  }

  const stockValue =
    item.stockQuantity ??
    item.availableQuantity ??
    item.quantityAvailable ??
    item.onHand ??
    item.stock ??
    item.quantity ??
    item.inventory ??
    item.product?.stockQuantity ??
    item.product?.stock ??
    item.inventoryItem?.stockQuantity ??
    item.inventoryItem?.stock ??
    null;

  if (stockValue === null || stockValue === undefined || stockValue === "") {
    return null;
  }

  const stock = Number(stockValue);
  return Number.isFinite(stock) ? stock : null;
}

function getMirrorAssociateInventoryImage(item) {
  const name = getMirrorAssociateInventoryName(item);

  return safeImageUrl(
    item.imageUrl ||
      item.image_url ||
      item.image ||
      item.photoUrl ||
      item.productImageUrl ||
      item.primaryImageUrl ||
      item.thumbnailUrl ||
      item.product?.imageUrl ||
      item.product?.image ||
      item.inventoryItem?.imageUrl ||
      item.inventoryItem?.image,
    `https://placehold.co/140x140/f7f3ec/171411?text=${encodeURIComponent(name)}`
  );
}

function getMirrorAssociateInventoryStats(items = []) {
  const safeItems = Array.isArray(items) ? items : [];

  const totalValue = safeItems.reduce((sum, item) => {
    const price = getMirrorAssociateInventoryPrice(item);
    const stock = getMirrorAssociateInventoryStock(item);

    return sum + price * Math.max(stock || 1, 1);
  }, 0);

  const lowStockCount = safeItems.filter(item => {
    const stock = getMirrorAssociateInventoryStock(item);
    return stock !== null && stock > 0 && stock <= 2;
  }).length;

  const outOfStockCount = safeItems.filter(item => {
    const stock = getMirrorAssociateInventoryStock(item);
    return stock !== null && stock <= 0;
  }).length;

  const categories = [
    ...new Set(
      safeItems
        .map(getMirrorAssociateInventoryCategory)
        .map(category => String(category || "").trim())
        .filter(Boolean)
    )
  ];

  return {
    itemCount: safeItems.length,
    totalValue,
    lowStockCount,
    outOfStockCount,
    categoryCount: categories.length,
    categories
  };
}

function buildMirrorAssociateInventoryRowsHtml(items = []) {
  if (!items.length) {
    return `
      <div class="mirror-associate-inline-empty">
        No local inventory was found for this retailer/store yet.
      </div>
    `;
  }

  return items.slice(0, 24).map(item => {
    const name = getMirrorAssociateInventoryName(item);
    const rfid = getMirrorAssociateInventoryRfid(item);
    const category = getMirrorAssociateInventoryCategory(item);
    const price = getMirrorAssociateInventoryPrice(item);
    const stock = getMirrorAssociateInventoryStock(item);
    const imageUrl = getMirrorAssociateInventoryImage(item);

    const stockLabel =
      stock === null
        ? "Stock unknown"
        : stock <= 0
          ? "Out of stock"
          : stock <= 2
            ? `${stock} low stock`
            : `${stock} available`;

    return `
      <article class="mirror-associate-activity-row">
        <img
          src="${escapeMirrorShowroomHtml(imageUrl)}"
          alt="${escapeMirrorShowroomHtml(name)}"
          onerror="this.src='https://placehold.co/120x120/f7f3ec/171411?text=Item';"
        >

        <div>
          <strong>${escapeMirrorShowroomHtml(name)}</strong>
          <span>
            ${escapeMirrorShowroomHtml(category)}
            · ${escapeMirrorShowroomHtml(formatPrice(price))}
            · ${escapeMirrorShowroomHtml(stockLabel)}
          </span>
          ${
            rfid
              ? `<span>RFID ${escapeMirrorShowroomHtml(rfid)}</span>`
              : ""
          }
        </div>
      </article>
    `;
  }).join("");
}

function buildMirrorAssociateInventoryHtml() {
  const inventory = getMirrorAssociateStoredInventory();
  const stats = getMirrorAssociateInventoryStats(inventory);
  const storeName =
    getMirrorShowroomDisplayStore?.() ||
    getMirrorStoreDisplayName?.() ||
    "Current Store";

  return `
    <div class="mirror-associate-inline-view">
      <header class="mirror-associate-inline-head">
        <div>
          <p>Associate Inventory</p>
          <h3>${escapeMirrorShowroomHtml(storeName)}</h3>
        </div>

        <button class="mirror-associate-inline-close" type="button" id="mirrorAssociateInlineCloseBtn">
          ×
        </button>
      </header>

      <section class="mirror-associate-inline-stats">
        <div>
          <span>Items</span>
          <strong>${stats.itemCount}</strong>
        </div>

        <div>
          <span>Categories</span>
          <strong>${stats.categoryCount}</strong>
        </div>

        <div>
          <span>Low Stock</span>
          <strong>${stats.lowStockCount}</strong>
        </div>

        <div>
          <span>Value</span>
          <strong>${escapeMirrorShowroomHtml(formatPrice(stats.totalValue))}</strong>
        </div>
      </section>

      <section class="mirror-associate-inline-section">
        <h4>Store Inventory</h4>
        ${buildMirrorAssociateInventoryRowsHtml(inventory)}
      </section>

      <section class="mirror-associate-inline-section">
        <h4>Inventory Notes</h4>

        <article class="mirror-associate-timeline-row">
          <strong>Store-scoped inventory</strong>
          <span>
            This view syncs live backend inventory first, then falls back to cached mirror/session inventory only if the backend is unavailable.
          </span>
        </article>

        <article class="mirror-associate-timeline-row">
          <strong>Live inventory source</strong>
          <span>
            Stock counts come from /api/v1/macy-stylist/associate/inventory when available.
          </span>
        </article>
      </section>
    </div>
  `;
}

async function showMirrorAssociateInlineInventory() {
  const shell = buildMirrorAssociateControl();
  const panel = shell.querySelector(".mirror-associate-panel");

  if (!panel) {
    return false;
  }

  panel.innerHTML = `
    <div class="mirror-associate-inline-view">
      <header class="mirror-associate-inline-head">
        <div>
          <p>Associate Inventory</p>
          <h3>${escapeMirrorShowroomHtml(
            getMirrorShowroomDisplayStore?.() ||
              getMirrorStoreDisplayName?.() ||
              "Current Store"
          )}</h3>
        </div>

        <button class="mirror-associate-inline-close" type="button" id="mirrorAssociateInlineCloseBtn">
          ×
        </button>
      </header>

      <div class="mirror-associate-inline-empty">
        Syncing live inventory from Universal Stylist...
      </div>
    </div>
  `;

  shell.classList.add("is-open");
  document.body.classList.add("mirror-associate-control-open");

  panel.querySelector("#mirrorAssociateInlineCloseBtn")?.addEventListener("click", () => {
    shell.remove();
    buildMirrorAssociateControl();
    showMirrorAssociateControl();
  });

  const syncResult =
    typeof syncMirrorAssociateInventoryFromBackend === "function"
      ? await syncMirrorAssociateInventoryFromBackend()
      : { ok: false, source: "missing-sync", items: [] };

  panel.innerHTML = buildMirrorAssociateInventoryHtml();

  panel.querySelector("#mirrorAssociateInlineCloseBtn")?.addEventListener("click", () => {
    shell.remove();
    buildMirrorAssociateControl();
    showMirrorAssociateControl();
  });

  if (typeof addTryOnTimelineEvent === "function") {
    addTryOnTimelineEvent(
      "inventory",
      "Associate inventory opened",
      syncResult.ok
        ? "Associate viewed live backend store inventory from the mirror control panel."
        : "Associate viewed cached/local inventory because backend sync was unavailable.",
      [getMirrorShowroomDisplayStore?.() || "Current Store"]
    );
  }

  showToast?.(
    syncResult.ok
      ? "Live inventory synced."
      : "Inventory opened from cached data.",
    syncResult.ok ? "success" : "info"
  );

  setStatus?.(
    syncResult.ok
      ? "Associate inventory synced from backend."
      : "Associate inventory opened from cached data.",
    syncResult.ok ? "success" : "ready"
  );

  return true;
}

function handleMirrorAssociateAction(action) {
  const normalizedAction = String(action || "").trim().toLowerCase();

  if (!normalizedAction) {
    return false;
  }

  if (normalizedAction === "customer-mirror") {
    hideMirrorAssociateControl();

    if (typeof showMirrorMainExperience === "function") {
      showMirrorMainExperience();
    }

    if (typeof setStatus === "function") {
      setStatus("Returned to Customer Mirror.", "ready");
    }

    if (typeof showToast === "function") {
      showToast("Returned to Customer Mirror.", "success");
    }

    return true;
  }

  if (normalizedAction === "associate-mirror") {
    return openAssociateMirrorMode("Associate Mirror");
  }

  if (normalizedAction === "dashboard") {
    return navigateMirrorAssociateTo("/dashboard.html", "Associate Dashboard", {
      source: "mirror-associate-control"
    });
  }

  if (normalizedAction === "inventory") {
    return showMirrorAssociateInlineInventory();
  }

  if (normalizedAction === "activity") {
    return showMirrorAssociateInlineActivity();
  }

  if (normalizedAction === "debug") {
    return showMirrorAssociateInlineDebugHealth();
  }

  if (normalizedAction === "admin") {
    return openMirrorAssociateAdminControls();
  }

  return false;
}

function bindMirrorAssociateControlEvents(shell) {
  shell.querySelector("#mirrorAssociateTriggerBtn")?.addEventListener("click", toggleMirrorAssociateControl);
  shell.querySelector("#mirrorAssociateBackdrop")?.addEventListener("click", hideMirrorAssociateControl);
  shell.querySelector("#mirrorAssociateCloseBtn")?.addEventListener("click", hideMirrorAssociateControl);

  shell.querySelectorAll("[data-associate-action]").forEach(button => {
    button.addEventListener("click", () => {
      handleMirrorAssociateAction(button.dataset.associateAction || "");
    });
  });
}

function exposeMirrorMainExperienceTools() {
  window.MirrorMainExperience = {
    show: showMirrorMainExperience,
    hide: hideMirrorMainExperience,
    build: buildMirrorMainExperience,
    demoProduct: showMirrorMainDemoProduct,
    updateProduct: updateMirrorMainProductCard,
    updateBagCount: updateMirrorMainBagCount,
    openProfile: openMirrorMainProfileDrawer,
    closeProfile: closeMirrorMainProfileDrawer,
    openBag: openMirrorMainBagDrawer,
    closeBag: closeMirrorMainBagDrawer,
    openShowroom: openMirrorMainShowroomOverlay,
    closeShowroom: closeMirrorMainShowroomOverlay,
    openAssociateControl: showMirrorAssociateControl,
    closeAssociateControl: hideMirrorAssociateControl,
    toggleAssociateControl: toggleMirrorAssociateControl,
    openInventory: showMirrorAssociateInlineInventory,
    syncInventory: syncMirrorAssociateInventoryFromBackend,
    openDebugHealth: showMirrorAssociateInlineDebugHealth,

    status() {
      const main = document.getElementById("mirrorMainExperience");

      return {
        exists: !!main,
        active: !!main?.classList.contains("is-active"),
        productVisible: !!document
          .getElementById("mirrorMainProductCard")
          ?.classList.contains("is-visible"),
        demoProductActive: document.body.classList.contains("mirror-main-demo-product-active"),
        bagCount: getMirrorMainBagCount(),
        storeName: getMirrorShowroomDisplayStore()
      };
    },

    health({ autoBuild = false } = {}) {
      let main = document.getElementById("mirrorMainExperience");

      if (!main && autoBuild && typeof buildMirrorMainExperience === "function") {
        main = buildMirrorMainExperience();
      }

      const report = {
        healthy: !!main,
        exists: !!main,
        active: !!main?.classList.contains("is-active"),
        buildable: typeof buildMirrorMainExperience === "function",
        showFunctionReady: typeof showMirrorMainExperience === "function",
        hideFunctionReady: typeof hideMirrorMainExperience === "function",
        hasNav: !!document.querySelector(".mirror-main-nav"),
        hasScanCard: !!document.querySelector(".mirror-main-scan-card"),
        hasEditorial: !!document.querySelector(".mirror-main-editorial"),
        hasPixelCard: !!document.querySelector(".mirror-main-pixel-card"),
        hasTimeline: !!document.querySelector(".mirror-main-timeline"),
        productVisible: !!document
          .getElementById("mirrorMainProductCard")
          ?.classList.contains("is-visible"),
        demoProductActive: document.body.classList.contains("mirror-main-demo-product-active"),
        bagCount: getMirrorMainBagCount(),
        storeName: getMirrorShowroomDisplayStore()
      };

      console.table(report);
      return report;
    }
  };

  if (window.PixelMirrorDebug) {
    window.PixelMirrorDebug.showMainExperience = showMirrorMainExperience;
    window.PixelMirrorDebug.hideMainExperience = hideMirrorMainExperience;
    window.PixelMirrorDebug.buildMainExperience = buildMirrorMainExperience;
    window.PixelMirrorDebug.mainExperienceStatus = window.MirrorMainExperience.status;
    window.PixelMirrorDebug.mainExperienceHealth = window.MirrorMainExperience.health;
    window.PixelMirrorDebug.showAssociateControl = showMirrorAssociateControl;
    window.PixelMirrorDebug.hideAssociateControl = hideMirrorAssociateControl;
    window.PixelMirrorDebug.toggleAssociateControl = toggleMirrorAssociateControl;
    window.PixelMirrorDebug.openAssociateInventory = showMirrorAssociateInlineInventory;
    window.PixelMirrorDebug.syncAssociateInventory = syncMirrorAssociateInventoryFromBackend;
    window.PixelMirrorDebug.openAssociateDebugHealth = showMirrorAssociateInlineDebugHealth;
  }

  console.log("MirrorMainExperience tools ready. Try:");
  console.log("MirrorMainExperience.show()");
  console.log("MirrorMainExperience.health()");
  console.log("MirrorMainExperience.health({ autoBuild: true })");
}

document.addEventListener("DOMContentLoaded", () => {
  try {
    forceMirrorMainPageVisible();
    init();
    forceMirrorMainPageVisible();
  } catch (error) {
    forceMirrorMainPageVisible();

    console.error("Pixel mirror failed to initialize:", error);

    window.PixelMirrorBootError = error;

    const authStatus = document.getElementById("authStatus");
    if (authStatus) {
      authStatus.textContent = "Mirror setup needs attention";
    }

    const scanStatus = document.getElementById("scanStatus");
    if (scanStatus) {
      scanStatus.className = "status error";
      scanStatus.innerHTML = `
        <span class="dot error-dot"></span>
        <span>Pixel mirror could not finish setup. Check PixelMirrorBootError in the console.</span>
      `;
    }

    if (PIXEL_MIRROR_DEBUG_ENABLED && typeof exposePixelMirrorDebugTools === "function") {
      exposePixelMirrorDebugTools();
    }

    if (typeof exposeMirrorMainExperienceTools === "function") {
      exposeMirrorMainExperienceTools();
    }
  }

  window.setTimeout(() => {
    forceMirrorMainPageVisible();

    if (
      typeof showMirrorMainExperience === "function" &&
      !document.getElementById("mirrorMainExperience")?.classList.contains("is-active")
    ) {
      try {
        showMirrorMainExperience();
      } catch (error) {
        console.error("Final mirror boot failsafe failed:", error);
        window.PixelMirrorBootError = error;
      }
    }
  }, 750);
});