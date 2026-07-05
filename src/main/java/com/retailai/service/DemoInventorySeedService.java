package com.retailai.service;

import com.retailai.model.Product;
import com.retailai.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DemoInventorySeedService {

    private static final String DEFAULT_RETAILER_KEY = "MCS003";
    private static final String DEFAULT_RETAILER_NAME = "Universal Stylist Demo";
    private static final String DEFAULT_STORE_CODE = "MCS003-DEMO-STORE";
    private static final String DEFAULT_STORE_NAME = "Universal Stylist Demo Store";

    private final ProductRepository productRepository;

    public DemoInventorySeedService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public int seedDemoInventory() {
        return seedDemoInventory(
                DEFAULT_RETAILER_KEY,
                DEFAULT_RETAILER_NAME,
                DEFAULT_STORE_CODE,
                DEFAULT_STORE_NAME
        );
    }

    @Transactional
    public int seedDemoInventory(
            String retailerKey,
            String retailerName,
            String storeCode,
            String storeName
    ) {
        String safeRetailerKey = required(retailerKey, "Retailer key is required").toUpperCase();
        String safeRetailerName = defaultIfBlank(retailerName, safeRetailerKey);
        String safeStoreCode = required(storeCode, "Store code is required").toUpperCase();
        String safeStoreName = defaultIfBlank(storeName, safeStoreCode);

        List<Product> products = demoProducts(
                safeRetailerKey,
                safeRetailerName,
                safeStoreCode,
                safeStoreName
        );

        productRepository.saveAll(products);

        return products.size();
    }

    @Transactional
    public int clearDemoInventory() {
        return clearDemoInventory(
                DEFAULT_RETAILER_KEY,
                DEFAULT_STORE_CODE
        );
    }

    @Transactional
    public int clearDemoInventory(
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = required(retailerKey, "Retailer key is required").toUpperCase();
        String safeStoreCode = required(storeCode, "Store code is required").toUpperCase();

        List<Product> productsToRemove = productRepository.findByRetailerKeyAndStoreCode(
                        safeRetailerKey,
                        safeStoreCode
                )
                .stream()
                .filter(product -> isDemoProductForStore(product, safeRetailerKey, safeStoreCode))
                .toList();

        productRepository.deleteAll(productsToRemove);

        return productsToRemove.size();
    }

    private boolean isDemoProductForStore(
            Product product,
            String retailerKey,
            String storeCode
    ) {
        if (product == null) {
            return false;
        }

        String productRetailerKey = safe(product.getRetailerKey()).toUpperCase();
        String productStoreCode = safe(product.getStoreCode()).toUpperCase();
        String productRfid = safe(product.getRfid()).toUpperCase();
        String productImageUrl = safe(product.getImageUrl()).toLowerCase();

        boolean sameRetailer = retailerKey.equals(productRetailerKey);
        boolean sameStore = storeCode.equals(productStoreCode);

        if (!sameRetailer || !sameStore) {
            return false;
        }

        boolean currentDemoRfid =
                productRfid.startsWith(retailerKey + "-TOP-")
                        || productRfid.startsWith(retailerKey + "-BOT-")
                        || productRfid.startsWith(retailerKey + "-SHOE-")
                        || productRfid.startsWith(retailerKey + "-OUT-");

        boolean legacyDemoRfid =
                productRfid.matches("^RFID\\d+$")
                        || productRfid.matches("^RFID-[A-Z]-\\d+$")
                        || productRfid.startsWith("RFID-A-")
                        || productRfid.startsWith("RFID-B-");

        boolean localDemoImage =
                productImageUrl.startsWith("/images/products/")
                        || productImageUrl.contains("/images/products/");

        boolean knownDemoTemplate = isKnownDemoTemplate(product);

        return currentDemoRfid || legacyDemoRfid || localDemoImage || knownDemoTemplate;
    }

    private boolean isKnownDemoTemplate(Product product) {
        if (product == null) {
            return false;
        }

        String name = safe(product.getItemName()).toLowerCase();
        String brand = safe(product.getBrand()).toLowerCase();

        if (name.isBlank()) {
            return false;
        }

        return switch (name) {
            case "oxford shirt",
                 "slim chino",
                 "leather sneaker",
                 "wool blazer",
                 "cropped poplin shirt",
                 "relaxed cotton tee",
                 "ribbed knit top",
                 "silky button blouse",
                 "wide leg trouser",
                 "straight leg denim",
                 "pleated tailored pant",
                 "satin midi skirt",
                 "minimal leather sneaker",
                 "chunky sole loafer",
                 "strappy heeled sandal",
                 "retro runner sneaker",
                 "structured overshirt",
                 "cropped moto jacket",
                 "longline trench coat",
                 "soft knit cardigan",
                 "dri-fit tee",
                 "tech fleece joggers",
                 "air max sneakers",
                 "bomber jacket",
                 "cashmere crewneck",
                 "tailored trousers",
                 "suede chelsea boot",
                 "topcoat",
                 "relaxed fit hoodie",
                 "athletic jogger",
                 "court sneaker",
                 "quilted puffer",
                 "ribbed slim fit tee",
                 "high rise straight jean",
                 "platform sneaker",
                 "utility shirt jacket" -> true;
            default -> false;
        };
    }

    private List<Product> demoProducts(
            String retailerKey,
            String retailerName,
            String storeCode,
            String storeName
    ) {
        Map<String, Integer> categoryCounters = new LinkedHashMap<>();
        List<Product> products = new ArrayList<>();

        for (DemoProductTemplate template : demoTemplates()) {
            String generatedRfid = buildStoreSpecificRfid(
                    retailerKey,
                    template.category(),
                    categoryCounters
            );

            Product product = product(
                    generatedRfid,
                    template.itemName(),
                    template.brand(),
                    template.category(),
                    template.color(),
                    template.price(),
                    template.imageUrl(),
                    template.stockQuantity(),
                    retailerKey,
                    retailerName,
                    storeCode,
                    storeName,
                    template.size(),
                    template.fit(),
                    template.material(),
                    template.gender(),
                    template.season(),
                    template.occasion(),
                    template.styleTags(),
                    template.pattern()
            );

            products.add(product);
        }

        return products;
    }

    private String buildStoreSpecificRfid(
            String retailerKey,
            String category,
            Map<String, Integer> categoryCounters
    ) {
        String safeRetailerKey = required(retailerKey, "Retailer key is required").toUpperCase();
        String categoryCode = categoryCode(category);

        int nextNumber = categoryCounters.getOrDefault(categoryCode, 0) + 1;
        categoryCounters.put(categoryCode, nextNumber);

        return safeRetailerKey + "-" + categoryCode + "-" + String.format("%03d", nextNumber);
    }

    private String categoryCode(String category) {
        String normalized = safe(category).toLowerCase();

        if (normalized.equals("tops") || normalized.equals("top")) {
            return "TOP";
        }

        if (normalized.equals("bottoms") || normalized.equals("bottom")) {
            return "BOT";
        }

        if (normalized.equals("shoes") || normalized.equals("shoe")) {
            return "SHOE";
        }

        if (normalized.equals("outerwear")) {
            return "OUT";
        }

        return "ITEM";
    }

    private List<DemoProductTemplate> demoTemplates() {
        return List.of(
                template("Oxford Shirt", "Polo Ralph Lauren", "Tops", "Blue", 80.00, "/images/products/oxford-shirt.jpg", 12, "M", "Classic Fit", "Cotton", "Unisex", "Spring/Fall", "Casual, Smart Casual, Office", "preppy, classic, polished, versatile", "Solid"),
                template("Slim Chino", "Polo Ralph Lauren", "Bottoms", "Khaki", 95.00, "/images/products/slim-chino.jpg", 8, "32x32", "Slim Fit", "Cotton Twill", "Unisex", "Spring/Fall", "Casual, Office, Smart Casual", "clean, tailored, preppy, neutral", "Solid"),
                template("Leather Sneaker", "Cole Haan", "Shoes", "White", 140.00, "/images/products/leather-sneaker.jpg", 6, "10", "Standard", "Leather", "Unisex", "All Season", "Casual, Smart Casual, Travel", "minimal, clean, versatile, modern", "Solid"),
                template("Wool Blazer", "Calvin Klein", "Outerwear", "Navy", 220.00, "/images/products/wool-blazer.jpg", 4, "40R", "Tailored Fit", "Wool Blend", "Unisex", "Fall/Winter", "Formal, Office, Date Night", "sharp, tailored, elevated, classic", "Solid"),

                template("Cropped Poplin Shirt", "Zara", "Tops", "White", 49.90, "/images/products/cropped-poplin-shirt.png", 14, "S", "Cropped Fit", "Cotton Poplin", "Women", "Spring/Summer", "Casual, Date Night, Brunch", "minimal, crisp, modern, feminine", "Solid"),
                template("Relaxed Cotton Tee", "Zara", "Tops", "Cream", 29.90, "/images/products/relaxed-cotton-tee.png", 18, "M", "Relaxed Fit", "Cotton", "Unisex", "Spring/Summer", "Casual, Streetwear, Everyday", "soft, relaxed, neutral, minimal", "Solid"),
                template("Ribbed Knit Top", "Zara", "Tops", "Black", 39.90, "/images/products/ribbed-knit-top.png", 10, "S", "Slim Fit", "Ribbed Knit", "Women", "All Season", "Date Night, Casual, Smart Casual", "sleek, fitted, modern, versatile", "Ribbed"),
                template("Silky Button Blouse", "Zara", "Tops", "Champagne", 59.90, "/images/products/silky-button-blouse.png", 7, "M", "Regular Fit", "Satin Blend", "Women", "Spring/Fall", "Date Night, Formal, Office", "elegant, soft, polished, elevated", "Solid"),
                template("Wide Leg Trouser", "Zara", "Bottoms", "Beige", 69.90, "/images/products/wide-leg-trouser.png", 11, "M", "Wide Leg", "Polyester Blend", "Women", "Spring/Fall", "Office, Smart Casual, Luxury", "tailored, relaxed, neutral, elevated", "Solid"),
                template("Straight Leg Denim", "Zara", "Bottoms", "Blue", 59.90, "/images/products/straight-leg-denim.png", 16, "30x32", "Straight Fit", "Denim", "Unisex", "All Season", "Casual, Streetwear, Everyday", "classic, relaxed, versatile, denim", "Solid"),
                template("Pleated Tailored Pant", "Zara", "Bottoms", "Charcoal", 79.90, "/images/products/pleated-tailored-pant.png", 8, "M", "Tailored Fit", "Wool Blend", "Unisex", "Fall/Winter", "Office, Formal, Luxury", "tailored, refined, structured, elevated", "Solid"),
                template("Satin Midi Skirt", "Zara", "Bottoms", "Olive", 55.90, "/images/products/satin-midi-skirt.png", 6, "S", "Midi Fit", "Satin", "Women", "Spring/Fall", "Date Night, Brunch, Smart Casual", "soft, elegant, feminine, fluid", "Solid"),
                template("Minimal Leather Sneaker", "Zara", "Shoes", "White", 89.90, "/images/products/minimal-leather-sneaker.png", 12, "10", "Standard", "Leather", "Unisex", "All Season", "Casual, Smart Casual, Travel", "minimal, clean, modern, versatile", "Solid"),
                template("Chunky Sole Loafer", "Zara", "Shoes", "Black", 99.90, "/images/products/chunky-sole-loafer.png", 9, "9", "Standard", "Faux Leather", "Unisex", "Fall/Winter", "Office, Streetwear, Date Night", "bold, polished, modern, chunky", "Solid"),
                template("Strappy Heeled Sandal", "Zara", "Shoes", "Tan", 79.90, "/images/products/strappy-heeled-sandal.png", 5, "8", "Standard", "Faux Leather", "Women", "Spring/Summer", "Date Night, Formal, Vacation", "elegant, feminine, warm, elevated", "Solid"),
                template("Retro Runner Sneaker", "Zara", "Shoes", "Grey", 95.90, "/images/products/retro-runner-sneaker.jpg", 8, "10", "Athletic", "Mesh/Suede", "Unisex", "All Season", "Streetwear, Casual, Travel", "retro, sporty, casual, urban", "Mixed"),
                template("Structured Overshirt", "Zara", "Outerwear", "Stone", 79.90, "/images/products/structured-overshirt.png", 7, "M", "Relaxed Fit", "Cotton Blend", "Unisex", "Spring/Fall", "Casual, Streetwear, Layering", "structured, neutral, casual, modern", "Solid"),
                template("Cropped Moto Jacket", "Zara", "Outerwear", "Black", 119.00, "/images/products/cropped-moto-jacket.png", 4, "S", "Cropped Fit", "Faux Leather", "Women", "Fall/Winter", "Date Night, Streetwear, Casual", "edgy, bold, fitted, modern", "Solid"),
                template("Longline Trench Coat", "Zara", "Outerwear", "Camel", 139.00, "/images/products/longline-trench-coat.png", 5, "M", "Longline Fit", "Cotton Blend", "Unisex", "Spring/Fall", "Office, Smart Casual, Luxury", "classic, elevated, timeless, neutral", "Solid"),
                template("Soft Knit Cardigan", "Zara", "Outerwear", "Grey", 69.90, "/images/products/soft-knit-cardigan.png", 9, "M", "Relaxed Fit", "Knit Blend", "Unisex", "Fall/Winter", "Casual, Lounge, Smart Casual", "soft, cozy, relaxed, neutral", "Knit"),

                template("Dri-FIT Tee", "Nike", "Tops", "Black", 35.00, "/images/products/nike-white-tee.jpg", 20, "M", "Athletic Fit", "Dri-FIT Polyester", "Unisex", "All Season", "Athletic, Casual, Streetwear", "sporty, performance, minimal, active", "Solid"),
                template("Tech Fleece Joggers", "Nike", "Bottoms", "Gray", 110.00, "/images/products/nike-black-cargo.jpg", 11, "M", "Tapered Fit", "Fleece", "Unisex", "Fall/Winter", "Athletic, Streetwear, Casual", "sporty, relaxed, technical, modern", "Solid"),
                template("Air Max Sneakers", "Nike", "Shoes", "White", 115.00, "/images/products/air-max-sneakers.jpg", 9, "10", "Athletic", "Mesh/Leather", "Unisex", "All Season", "Athletic, Streetwear, Casual", "sporty, iconic, clean, streetwear", "Mixed"),
                template("Bomber Jacket", "Nike", "Outerwear", "Olive", 130.00, "/images/products/nike-shell-jacket.jpg", 6, "M", "Regular Fit", "Nylon", "Unisex", "Fall/Winter", "Streetwear, Casual, Athletic", "sporty, urban, lightweight, functional", "Solid"),

                template("Cashmere Crewneck", "Theory", "Tops", "Camel", 185.00, "/images/products/cashmere-crewneck.jpg", 7, "M", "Regular Fit", "Cashmere", "Unisex", "Fall/Winter", "Luxury, Smart Casual, Office", "soft, premium, minimal, refined", "Solid"),
                template("Tailored Trousers", "BOSS", "Bottoms", "Charcoal", 165.00, "/images/products/tailored-trousers.jpg", 6, "32x32", "Tailored Fit", "Wool Blend", "Unisex", "Fall/Winter", "Office, Formal, Luxury", "tailored, polished, premium, structured", "Solid"),
                template("Suede Chelsea Boot", "To Boot New York", "Shoes", "Brown", 298.00, "/images/products/suede-chelsea-boot.jpg", 5, "10", "Standard", "Suede", "Unisex", "Fall/Winter", "Date Night, Formal, Smart Casual", "premium, refined, classic, elevated", "Solid"),
                template("Topcoat", "Vince", "Outerwear", "Black", 395.00, "/images/products/topcoat-vince.jpg", 3, "M", "Longline Fit", "Wool Blend", "Unisex", "Fall/Winter", "Luxury, Formal, Office", "premium, sleek, timeless, refined", "Solid"),

                template("Relaxed Fit Hoodie", "Free Assembly", "Tops", "Gray", 28.00, "/images/products/walmart-relaxed-fit-hoodie.jpg", 18, "M", "Relaxed Fit", "Cotton Fleece", "Unisex", "Fall/Winter", "Casual, Streetwear, Lounge", "comfortable, relaxed, affordable, casual", "Solid"),
                template("Athletic Jogger", "George", "Bottoms", "Dark Blue", 32.00, "/images/products/walmart-athletic-jogger.jpg", 16, "M", "Athletic Fit", "Cotton Blend", "Unisex", "All Season", "Casual, Athletic, Lounge", "comfortable, sporty, relaxed, practical", "Solid"),
                template("Court Sneaker", "Time and Tru", "Shoes", "White", 24.00, "/images/products/walmart-court-sneaker.jpg", 20, "10", "Standard", "Synthetic Leather", "Unisex", "All Season", "Casual, Everyday, Travel", "clean, affordable, simple, versatile", "Solid"),
                template("Quilted Puffer", "Free Assembly", "Outerwear", "Olive", 54.00, "/images/products/walmart-quilted-puffer.jpg", 9, "M", "Regular Fit", "Nylon", "Unisex", "Fall/Winter", "Casual, Outdoor, Streetwear", "warm, practical, casual, utility", "Quilted"),

                template("Ribbed Slim Fit Tee", "A New Day", "Tops", "Cream", 18.00, "/images/products/target-ribbed-slim-fit-tee.jpg", 22, "S", "Slim Fit", "Ribbed Cotton", "Women", "Spring/Summer", "Casual, Brunch, Everyday", "soft, fitted, minimal, feminine", "Ribbed"),
                template("High Rise Straight Jean", "Universal Thread", "Bottoms", "Light Blue", 36.00, "/images/products/target-high-rise-straight-jeans.jpg", 14, "28", "Straight Fit", "Denim", "Women", "All Season", "Casual, Everyday, Brunch", "classic, denim, relaxed, wearable", "Solid"),
                template("Platform Sneaker", "A New Day", "Shoes", "White", 32.00, "/images/products/target-platform-sneaker.jpg", 15, "8", "Platform", "Synthetic Leather", "Women", "All Season", "Casual, Brunch, Streetwear", "clean, playful, modern, casual", "Solid"),
                template("Utility Shirt Jacket", "A New Day", "Outerwear", "Tan", 65.00, "/images/products/target-utility-shirt-jacket.jpg", 7, "M", "Relaxed Fit", "Cotton Twill", "Women", "Spring/Fall", "Casual, Layering, Smart Casual", "utility, casual, neutral, practical", "Solid")
        );
    }

    private DemoProductTemplate template(
            String itemName,
            String brand,
            String category,
            String color,
            Double price,
            String imageUrl,
            Integer stockQuantity,
            String size,
            String fit,
            String material,
            String gender,
            String season,
            String occasion,
            String styleTags,
            String pattern
    ) {
        return new DemoProductTemplate(
                itemName,
                brand,
                category,
                color,
                price,
                imageUrl,
                stockQuantity,
                size,
                fit,
                material,
                gender,
                season,
                occasion,
                styleTags,
                pattern
        );
    }

    private Product product(
            String rfid,
            String itemName,
            String brand,
            String category,
            String color,
            Double price,
            String imageUrl,
            Integer stockQuantity,
            String retailerKey,
            String retailerName,
            String storeCode,
            String storeName,
            String size,
            String fit,
            String material,
            String gender,
            String season,
            String occasion,
            String styleTags,
            String pattern
    ) {
        Product product = productRepository
                .findByRfidAndRetailerKeyAndStoreCode(rfid, retailerKey, storeCode)
                .orElseGet(() -> productRepository.findById(rfid).orElseGet(Product::new));

        product.setRfid(rfid);
        product.setItemName(itemName);
        product.setBrand(brand);
        product.setCategory(category);
        product.setColor(color);
        product.setPrice(price);
        product.setImageUrl(imageUrl);
        product.setStockQuantity(stockQuantity == null ? 0 : stockQuantity);

        product.setRetailerKey(retailerKey);
        product.setRetailerName(retailerName);
        product.setStoreCode(storeCode);
        product.setStoreName(storeName);

        product.setSize(size);
        product.setFit(fit);
        product.setMaterial(material);
        product.setGender(gender);
        product.setSeason(season);
        product.setOccasion(occasion);
        product.setStyleTags(styleTags);
        product.setPattern(pattern);

        product.setActive(true);
        product.setInStoreOnly(false);
        product.setAvailable(stockQuantity != null && stockQuantity > 0);

        return product;
    }

    private String required(String value, String message) {
        String safeValue = safe(value);

        if (safeValue.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return safeValue;
    }

    private String defaultIfBlank(String value, String fallback) {
        String safeValue = safe(value);
        return safeValue.isBlank() ? safe(fallback) : safeValue;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record DemoProductTemplate(
            String itemName,
            String brand,
            String category,
            String color,
            Double price,
            String imageUrl,
            Integer stockQuantity,
            String size,
            String fit,
            String material,
            String gender,
            String season,
            String occasion,
            String styleTags,
            String pattern
    ) {
    }
}