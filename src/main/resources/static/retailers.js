(function () {
  const RETAILERS = {
    MACY001: {
      name: "Macy's",
      stores: [
        { code: "MACY-NYC-01", label: "Herald Square" },
        { code: "MACY-BK-02", label: "Brooklyn" }
      ]
    },

    ZARA001: {
      name: "Zara",
      stores: [
        { code: "ZARA-SOHO-01", label: "SoHo" },
        { code: "ZARA-5TH-02", label: "5th Avenue" }
      ]
    },

    NORD001: {
      name: "Nordstrom",
      stores: [
        { code: "NORD-NYC-01", label: "57th Street" },
        { code: "NORD-WTC-02", label: "World Trade Center" }
      ]
    },

    NIKE001: {
      name: "Nike",
      stores: [
        { code: "NIKE-NYC-01", label: "Nike NYC" },
        { code: "NIKE-SOHO-02", label: "Nike SoHo" }
      ]
    },

    WALMART001: {
      name: "Walmart",
      stores: [
        { code: "WALMART-NYC-01", label: "Walmart NYC" },
        { code: "WALMART-NJ-02", label: "Walmart New Jersey" }
      ]
    },

    TARGET001: {
      name: "Target",
      stores: [
        { code: "TARGET-NYC-01", label: "Target NYC" },
        { code: "TARGET-BK-02", label: "Target Brooklyn" }
      ]
    }
  };

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
      stores: retailer.stores
    }));
  }

  function getStoresForRetailer(retailerKey) {
    return RETAILERS[retailerKey]?.stores || [];
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
           retailerName: retailer.name
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
    getRetailerLabels,
    getStoreOptions,
    getRetailerName,
    getRetailerKeyFromName,
    getRetailerEntries,
    getStoresForRetailer,
    getStoreName,
    getStoreByCode,
    buildRetailerOptionsHtml
  };
})();