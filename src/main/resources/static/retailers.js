(function () {
  const DEFAULT_THEME = {
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
    editorialCopy:
      "Scan an item to unlock styling, outfit intelligence, and store-aware recommendations."
  };

  const RETAILERS = {
    MACY001: {
      name: "Macy's",
      brandLabel: "Macy's",
      type: "department-store",
      defaultStoreCode: "MACY-NYC-01",
      stores: [
        { code: "MACY-NYC-01", label: "Herald Square" },
        { code: "MACY-BK-02", label: "Brooklyn" },
        { code: "MACY-DEMO", label: "Macy's Demo Store" }
      ],
      theme: {
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
        editorialCopy:
          "Explore Macy's inventory, complete looks, and personalized recommendations."
      }
    },

    NICKS001: {
      name: "Nicks Boutique",
      brandLabel: "Nicks Boutique",
      type: "boutique",
      defaultStoreCode: "NICKS-DEMO",
      stores: [
        { code: "NICKS-DEMO", label: "Nicks Boutique" },
        { code: "NICKS-SOHO-01", label: "Nicks SoHo" }
      ],
      theme: {
        id: "boutique-minimal",
        label: "Boutique Minimal",
        surface: "#f7f3ec",
        ink: "#171411",
        muted: "#756b5f",
        accent: "#9b7a4f",
        accentSoft: "rgba(155, 122, 79, 0.18)",
        secondary: "#d7c2a2",
        success: "#7DBF8E",
        editorialEyebrow: "The Quiet Edit",
        editorialTitleTop: "Dressed in",
        editorialTitleStrong: "Silence.",
        editorialTitleBottom: "",
        editorialCopy:
          "Curated selections from Nicks Boutique, brought to your reflection."
      }
    },

    KINGS001: {
      name: "Kings Boutique",
      brandLabel: "Kings Boutique",
      type: "boutique",
      defaultStoreCode: "KINGS-DEMO",
      stores: [
        { code: "KINGS-DEMO", label: "Kings Boutique Demo Store" },
        { code: "KINGS-ATL-01", label: "Kings Atlanta" }
      ],
      theme: {
        id: "boutique-luxe",
        label: "Boutique Luxe",
        surface: "#f7f0e7",
        ink: "#18120d",
        muted: "#7a6a5a",
        accent: "#8a623d",
        accentSoft: "rgba(138, 98, 61, 0.18)",
        secondary: "#c9ad86",
        success: "#7DBF8E",
        editorialEyebrow: "Private Collection",
        editorialTitleTop: "Styled",
        editorialTitleStrong: "With",
        editorialTitleBottom: "Intention",
        editorialCopy:
          "Premium boutique pieces curated into complete looks from Kings Boutique."
      }
    },

    ZARA001: {
      name: "Zara",
      brandLabel: "Zara",
      type: "fast-fashion",
      defaultStoreCode: "ZARA-SOHO-01",
      stores: [
        { code: "ZARA-SOHO-01", label: "SoHo" },
        { code: "ZARA-5TH-02", label: "5th Avenue" }
      ],
      theme: {
        id: "minimal-fashion",
        label: "Minimal Fashion",
        surface: "#f6f6f3",
        ink: "#101010",
        muted: "#6d6d68",
        accent: "#111111",
        accentSoft: "rgba(17, 17, 17, 0.10)",
        secondary: "#b8b8ae",
        success: "#7DBF8E",
        editorialEyebrow: "Current Edit",
        editorialTitleTop: "Sharp",
        editorialTitleStrong: "Modern",
        editorialTitleBottom: "Lines",
        editorialCopy:
          "Fast, modern outfit creation from current Zara store inventory."
      }
    },

    NORD001: {
      name: "Nordstrom",
      brandLabel: "Nordstrom",
      type: "department-store",
      defaultStoreCode: "NORD-NYC-01",
      stores: [
        { code: "NORD-NYC-01", label: "57th Street" },
        { code: "NORD-WTC-02", label: "World Trade Center" }
      ],
      theme: {
        id: "quiet-luxury",
        label: "Quiet Luxury",
        surface: "#f5f3ef",
        ink: "#1f2933",
        muted: "#68737d",
        accent: "#2f4858",
        accentSoft: "rgba(47, 72, 88, 0.14)",
        secondary: "#b9a88f",
        success: "#7DBF8E",
        editorialEyebrow: "Designer Styling",
        editorialTitleTop: "Quiet",
        editorialTitleStrong: "Luxury",
        editorialTitleBottom: "Now",
        editorialCopy:
          "Elevated styling recommendations from Nordstrom store inventory."
      }
    },

    NIKE001: {
      name: "Nike",
      brandLabel: "Nike",
      type: "sport",
      defaultStoreCode: "NIKE-NYC-01",
      stores: [
        { code: "NIKE-NYC-01", label: "Nike NYC" },
        { code: "NIKE-SOHO-02", label: "Nike SoHo" }
      ],
      theme: {
        id: "sport-performance",
        label: "Sport Performance",
        surface: "#f7f8fa",
        ink: "#0b0b0b",
        muted: "#5f6670",
        accent: "#111111",
        accentSoft: "rgba(17, 17, 17, 0.12)",
        secondary: "#ff6a00",
        success: "#7DBF8E",
        editorialEyebrow: "Performance Edit",
        editorialTitleTop: "Move",
        editorialTitleStrong: "With",
        editorialTitleBottom: "Purpose",
        editorialCopy:
          "Performance-ready looks styled from Nike store inventory."
      }
    },

    WALMART001: {
      name: "Walmart",
      brandLabel: "Walmart",
      type: "mass-retail",
      defaultStoreCode: "WALMART-NYC-01",
      stores: [
        { code: "WALMART-NYC-01", label: "Walmart NYC" },
        { code: "WALMART-NJ-02", label: "Walmart New Jersey" }
      ],
      theme: {
        id: "accessible-retail",
        label: "Accessible Retail",
        surface: "#f8fbff",
        ink: "#0f172a",
        muted: "#64748b",
        accent: "#0071ce",
        accentSoft: "rgba(0, 113, 206, 0.14)",
        secondary: "#ffc220",
        success: "#7DBF8E",
        editorialEyebrow: "Everyday Value",
        editorialTitleTop: "Ready",
        editorialTitleStrong: "For",
        editorialTitleBottom: "Today",
        editorialCopy:
          "Affordable, store-aware styling from Walmart inventory."
      }
    },

    TARGET001: {
      name: "Target",
      brandLabel: "Target",
      type: "mass-retail",
      defaultStoreCode: "TARGET-NYC-01",
      stores: [
        { code: "TARGET-NYC-01", label: "Target NYC" },
        { code: "TARGET-BK-02", label: "Target Brooklyn" }
      ],
      theme: {
        id: "clean-red-retail",
        label: "Clean Red Retail",
        surface: "#fffafa",
        ink: "#171717",
        muted: "#6f6f6f",
        accent: "#cc0000",
        accentSoft: "rgba(204, 0, 0, 0.12)",
        secondary: "#111827",
        success: "#7DBF8E",
        editorialEyebrow: "Style Run",
        editorialTitleTop: "Find",
        editorialTitleStrong: "Your",
        editorialTitleBottom: "Look",
        editorialCopy:
          "Clean, accessible outfit recommendations from Target store inventory."
      }
    }
  };

  function cloneTheme(theme) {
    return {
      ...DEFAULT_THEME,
      ...(theme && typeof theme === "object" ? theme : {})
    };
  }

  function getRetailerConfig(retailerKey) {
    return RETAILERS[retailerKey] || null;
  }

  function getRetailerLabels() {
    return Object.fromEntries(
      Object.entries(RETAILERS).map(([key, retailer]) => [key, retailer.name])
    );
  }

  function getStoreOptions() {
    return Object.fromEntries(
      Object.entries(RETAILERS).map(([key, retailer]) => [key, retailer.stores])
    );
  }

  function getRetailerName(retailerKey) {
    return RETAILERS[retailerKey]?.name || retailerKey || "Unknown Retailer";
  }

  function getRetailerBrandLabel(retailerKey) {
    return RETAILERS[retailerKey]?.brandLabel || getRetailerName(retailerKey);
  }

  function getRetailerType(retailerKey) {
    return RETAILERS[retailerKey]?.type || "default";
  }

  function getRetailerTheme(retailerKey) {
    return cloneTheme(RETAILERS[retailerKey]?.theme);
  }

  function getRetailerMirrorConfig(retailerKey) {
    const retailer = getRetailerConfig(retailerKey);

    if (!retailer) {
      return {
        key: retailerKey || "",
        name: retailerKey || "Unknown Retailer",
        brandLabel: retailerKey || "Unknown Retailer",
        type: "default",
        defaultStoreCode: "",
        stores: [],
        theme: cloneTheme(DEFAULT_THEME)
      };
    }

    return {
      key: retailerKey,
      name: retailer.name,
      brandLabel: retailer.brandLabel || retailer.name,
      type: retailer.type || "default",
      defaultStoreCode: retailer.defaultStoreCode || retailer.stores?.[0]?.code || "",
      stores: Array.isArray(retailer.stores) ? retailer.stores : [],
      theme: cloneTheme(retailer.theme)
    };
  }

  function getRetailerKeyFromName(name) {
    const normalizedName = String(name || "").trim().toLowerCase();

    return (
      Object.entries(RETAILERS).find(([, retailer]) => {
        return retailer.name.toLowerCase() === normalizedName;
      })?.[0] || ""
    );
  }

  function getRetailerEntries() {
    return Object.entries(RETAILERS).map(([key, retailer]) => ({
      key,
      name: retailer.name,
      brandLabel: retailer.brandLabel || retailer.name,
      type: retailer.type || "default",
      defaultStoreCode: retailer.defaultStoreCode || retailer.stores?.[0]?.code || "",
      stores: retailer.stores,
      theme: cloneTheme(retailer.theme)
    }));
  }

  function getStoresForRetailer(retailerKey) {
    return RETAILERS[retailerKey]?.stores || [];
  }

  function getDefaultStoreCode(retailerKey) {
    const retailer = RETAILERS[retailerKey];

    return retailer?.defaultStoreCode || retailer?.stores?.[0]?.code || "";
  }

  function getStoreName(retailerKey, storeCode) {
    const store = getStoresForRetailer(retailerKey).find(store => store.code === storeCode);
    return store?.label || storeCode || "Unknown Store";
  }

  function getStoreByCode(storeCode) {
    const normalizedCode = String(storeCode || "").trim();

    for (const [retailerKey, retailer] of Object.entries(RETAILERS)) {
      const store = retailer.stores.find(store => store.code === normalizedCode);

      if (store) {
        return {
          ...store,
          retailerKey,
          retailerName: retailer.name,
          retailerType: retailer.type || "default",
          theme: cloneTheme(retailer.theme)
        };
      }
    }

    return null;
  }

  function buildRetailerOptionsHtml(selectedValue = "", includeAllOption = false) {
    const allOption = includeAllOption ? `<option value="">All Retailers</option>` : "";

    const retailerOptions = getRetailerEntries()
      .map(retailer => {
        const selected = retailer.key === selectedValue ? " selected" : "";

        return `<option value="${retailer.key}"${selected}>${retailer.name}</option>`;
      })
      .join("");

    return `${allOption}${retailerOptions}`;
  }

  window.UniversalStylistRetailers = {
    RETAILERS,
    DEFAULT_THEME,

    getRetailerConfig,
    getRetailerLabels,
    getStoreOptions,
    getRetailerName,
    getRetailerBrandLabel,
    getRetailerType,
    getRetailerTheme,
    getRetailerMirrorConfig,
    getRetailerKeyFromName,
    getRetailerEntries,
    getStoresForRetailer,
    getDefaultStoreCode,
    getStoreName,
    getStoreByCode,
    buildRetailerOptionsHtml
  };
})();