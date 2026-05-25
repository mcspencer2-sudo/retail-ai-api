package com.retailai.service;

import com.retailai.model.Product;
import com.retailai.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DemoInventorySeedService {

    private static final String DEFAULT_RETAILER_KEY = "MACY001";
    private static final String DEFAULT_RETAILER_NAME = "Macy's";
    private static final String DEFAULT_STORE_CODE = "MACY-NYC-01";
    private static final String DEFAULT_STORE_NAME = "Herald Square";

    private final ProductRepository productRepository;

    public DemoInventorySeedService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public int seedDemoInventory() {
        return seedDemoInventory(
                DEFAULT_RETAILER_KEY,
                DEFAULT_RETAILER_NAME,
                DEFAULT_STORE_CODE,
                DEFAULT_STORE_NAME
        );
    }

    public int seedDemoInventory(
            String retailerKey,
            String retailerName,
            String storeCode,
            String storeName
    ) {
        String safeRetailerKey = required(retailerKey, "Retailer key is required");
        String safeRetailerName = defaultIfBlank(retailerName, safeRetailerKey);
        String safeStoreCode = required(storeCode, "Store code is required");
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

    public int clearDemoInventory() {
        List<String> demoRfids = demoRfids();
        int removed = 0;

        for (String rfid : demoRfids) {
            if (productRepository.existsById(rfid)) {
                productRepository.deleteById(rfid);
                removed++;
            }
        }

        return removed;
    }

    public int clearDemoInventory(
            String retailerKey,
            String storeCode
    ) {
        String safeRetailerKey = required(retailerKey, "Retailer key is required");
        String safeStoreCode = required(storeCode, "Store code is required");

        int removed = 0;

        for (String rfid : demoRfids()) {
            Product product = productRepository.findById(rfid).orElse(null);

            if (product == null) {
                continue;
            }

            boolean sameRetailer = safeRetailerKey.equalsIgnoreCase(safe(product.getRetailerKey()));
            boolean sameStore = safeStoreCode.equalsIgnoreCase(safe(product.getStoreCode()));

            if (sameRetailer && sameStore) {
                productRepository.deleteById(rfid);
                removed++;
            }
        }

        return removed;
    }

    private List<String> demoRfids() {
        return List.of(
                "RFID1001", "RFID1002", "RFID1003", "RFID1004",
                "RFID2001", "RFID2002", "RFID2003", "RFID2004",
                "RFID2005", "RFID2006", "RFID2007", "RFID2008",
                "RFID2009", "RFID2010", "RFID2011", "RFID2012",
                "RFID2013", "RFID2014", "RFID2015", "RFID2016",
                "RFID3001", "RFID3002", "RFID3003", "RFID3004",
                "RFID4001", "RFID4002", "RFID4003", "RFID4004",
                "RFID5001", "RFID5002", "RFID5003", "RFID5004",
                "RFID6001", "RFID6002", "RFID6003", "RFID6004"
        );
    }

    private List<Product> demoProducts(
            String retailerKey,
            String retailerName,
            String storeCode,
            String storeName
    ) {
        return demoTemplates()
                .stream()
                .map(template -> product(
                        template.rfid(),
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
                ))
                .toList();
    }

    private List<DemoProductTemplate> demoTemplates() {
        return List.of(
                template("RFID1001", "Oxford Shirt", "Polo Ralph Lauren", "Tops", "Blue", 80.00, "/images/products/oxford-shirt.jpg", 12, "M", "Classic Fit", "Cotton", "Unisex", "Spring/Fall", "Casual, Smart Casual, Office", "preppy, classic, polished, versatile", "Solid"),
                template("RFID1002", "Slim Chino", "Polo Ralph Lauren", "Bottoms", "Khaki", 95.00, "/images/products/slim-chino.jpg", 8, "32x32", "Slim Fit", "Cotton Twill", "Unisex", "Spring/Fall", "Casual, Office, Smart Casual", "clean, tailored, preppy, neutral", "Solid"),
                template("RFID1003", "Leather Sneaker", "Cole Haan", "Shoes", "White", 140.00, "/images/products/leather-sneaker.jpg", 6, "10", "Standard", "Leather", "Unisex", "All Season", "Casual, Smart Casual, Travel", "minimal, clean, versatile, modern", "Solid"),
                template("RFID1004", "Wool Blazer", "Calvin Klein", "Outerwear", "Navy", 220.00, "/images/products/wool-blazer.jpg", 4, "40R", "Tailored Fit", "Wool Blend", "Unisex", "Fall/Winter", "Formal, Office, Date Night", "sharp, tailored, elevated, classic", "Solid"),

                template("RFID2001", "Cropped Poplin Shirt", "Zara", "Tops", "White", 49.90, "/images/products/cropped-poplin-shirt.png", 14, "S", "Cropped Fit", "Cotton Poplin", "Women", "Spring/Summer", "Casual, Date Night, Brunch", "minimal, crisp, modern, feminine", "Solid"),
                template("RFID2002", "Relaxed Cotton Tee", "Zara", "Tops", "Cream", 29.90, "/images/products/relaxed-cotton-tee.png", 18, "M", "Relaxed Fit", "Cotton", "Unisex", "Spring/Summer", "Casual, Streetwear, Everyday", "soft, relaxed, neutral, minimal", "Solid"),
                template("RFID2003", "Ribbed Knit Top", "Zara", "Tops", "Black", 39.90, "/images/products/ribbed-knit-top.png", 10, "S", "Slim Fit", "Ribbed Knit", "Women", "All Season", "Date Night, Casual, Smart Casual", "sleek, fitted, modern, versatile", "Ribbed"),
                template("RFID2004", "Silky Button Blouse", "Zara", "Tops", "Champagne", 59.90, "/images/products/silky-button-blouse.png", 7, "M", "Regular Fit", "Satin Blend", "Women", "Spring/Fall", "Date Night, Formal, Office", "elegant, soft, polished, elevated", "Solid"),
                template("RFID2005", "Wide Leg Trouser", "Zara", "Bottoms", "Beige", 69.90, "/images/products/wide-leg-trouser.png", 11, "M", "Wide Leg", "Polyester Blend", "Women", "Spring/Fall", "Office, Smart Casual, Luxury", "tailored, relaxed, neutral, elevated", "Solid"),
                template("RFID2006", "Straight Leg Denim", "Zara", "Bottoms", "Blue", 59.90, "/images/products/straight-leg-denim.png", 16, "30x32", "Straight Fit", "Denim", "Unisex", "All Season", "Casual, Streetwear, Everyday", "classic, relaxed, versatile, denim", "Solid"),
                template("RFID2007", "Pleated Tailored Pant", "Zara", "Bottoms", "Charcoal", 79.90, "/images/products/pleated-tailored-pant.png", 8, "M", "Tailored Fit", "Wool Blend", "Unisex", "Fall/Winter", "Office, Formal, Luxury", "tailored, refined, structured, elevated", "Solid"),
                template("RFID2008", "Satin Midi Skirt", "Zara", "Bottoms", "Olive", 55.90, "/images/products/satin-midi-skirt.png", 6, "S", "Midi Fit", "Satin", "Women", "Spring/Fall", "Date Night, Brunch, Smart Casual", "soft, elegant, feminine, fluid", "Solid"),
                template("RFID2009", "Minimal Leather Sneaker", "Zara", "Shoes", "White", 89.90, "/images/products/minimal-leather-sneaker.png", 12, "10", "Standard", "Leather", "Unisex", "All Season", "Casual, Smart Casual, Travel", "minimal, clean, modern, versatile", "Solid"),
                template("RFID2010", "Chunky Sole Loafer", "Zara", "Shoes", "Black", 99.90, "/images/products/chunky-sole-loafer.png", 9, "9", "Standard", "Faux Leather", "Unisex", "Fall/Winter", "Office, Streetwear, Date Night", "bold, polished, modern, chunky", "Solid"),
                template("RFID2011", "Strappy Heeled Sandal", "Zara", "Shoes", "Tan", 79.90, "/images/products/strappy-heeled-sandal.png", 5, "8", "Standard", "Faux Leather", "Women", "Spring/Summer", "Date Night, Formal, Vacation", "elegant, feminine, warm, elevated", "Solid"),
                template("RFID2012", "Retro Runner Sneaker", "Zara", "Shoes", "Grey", 95.90, "/images/products/retro-runner-sneaker.jpg", 8, "10", "Athletic", "Mesh/Suede", "Unisex", "All Season", "Streetwear, Casual, Travel", "retro, sporty, casual, urban", "Mixed"),
                template("RFID2013", "Structured Overshirt", "Zara", "Outerwear", "Stone", 79.90, "/images/products/structured-overshirt.png", 7, "M", "Relaxed Fit", "Cotton Blend", "Unisex", "Spring/Fall", "Casual, Streetwear, Layering", "structured, neutral, casual, modern", "Solid"),
                template("RFID2014", "Cropped Moto Jacket", "Zara", "Outerwear", "Black", 119.00, "/images/products/cropped-moto-jacket.png", 4, "S", "Cropped Fit", "Faux Leather", "Women", "Fall/Winter", "Date Night, Streetwear, Casual", "edgy, bold, fitted, modern", "Solid"),
                template("RFID2015", "Longline Trench Coat", "Zara", "Outerwear", "Camel", 139.00, "/images/products/longline-trench-coat.png", 5, "M", "Longline Fit", "Cotton Blend", "Unisex", "Spring/Fall", "Office, Smart Casual, Luxury", "classic, elevated, timeless, neutral", "Solid"),
                template("RFID2016", "Soft Knit Cardigan", "Zara", "Outerwear", "Grey", 69.90, "/images/products/soft-knit-cardigan.png", 9, "M", "Relaxed Fit", "Knit Blend", "Unisex", "Fall/Winter", "Casual, Lounge, Smart Casual", "soft, cozy, relaxed, neutral", "Knit"),

                template("RFID3001", "Dri-FIT Tee", "Nike", "Tops", "Black", 35.00, "/images/products/nike-white-tee.jpg", 20, "M", "Athletic Fit", "Dri-FIT Polyester", "Unisex", "All Season", "Athletic, Casual, Streetwear", "sporty, performance, minimal, active", "Solid"),
                template("RFID3002", "Tech Fleece Joggers", "Nike", "Bottoms", "Gray", 110.00, "/images/products/nike-black-cargo.jpg", 11, "M", "Tapered Fit", "Fleece", "Unisex", "Fall/Winter", "Athletic, Streetwear, Casual", "sporty, relaxed, technical, modern", "Solid"),
                template("RFID3003", "Air Max Sneakers", "Nike", "Shoes", "White", 115.00, "/images/products/air-max-sneakers.jpg", 9, "10", "Athletic", "Mesh/Leather", "Unisex", "All Season", "Athletic, Streetwear, Casual", "sporty, iconic, clean, streetwear", "Mixed"),
                template("RFID3004", "Bomber Jacket", "Nike", "Outerwear", "Olive", 130.00, "/images/products/nike-shell-jacket.jpg", 6, "M", "Regular Fit", "Nylon", "Unisex", "Fall/Winter", "Streetwear, Casual, Athletic", "sporty, urban, lightweight, functional", "Solid"),

                template("RFID4001", "Cashmere Crewneck", "Theory", "Tops", "Camel", 185.00, "/images/products/cashmere-crewneck.jpg", 7, "M", "Regular Fit", "Cashmere", "Unisex", "Fall/Winter", "Luxury, Smart Casual, Office", "soft, premium, minimal, refined", "Solid"),
                template("RFID4002", "Tailored Trousers", "BOSS", "Bottoms", "Charcoal", 165.00, "/images/products/tailored-trousers.jpg", 6, "32x32", "Tailored Fit", "Wool Blend", "Unisex", "Fall/Winter", "Office, Formal, Luxury", "tailored, polished, premium, structured", "Solid"),
                template("RFID4003", "Suede Chelsea Boot", "To Boot New York", "Shoes", "Brown", 298.00, "/images/products/suede-chelsea-boot.jpg", 5, "10", "Standard", "Suede", "Unisex", "Fall/Winter", "Date Night, Formal, Smart Casual", "premium, refined, classic, elevated", "Solid"),
                template("RFID4004", "Topcoat", "Vince", "Outerwear", "Black", 395.00, "/images/products/topcoat-vince.jpg", 3, "M", "Longline Fit", "Wool Blend", "Unisex", "Fall/Winter", "Luxury, Formal, Office", "premium, sleek, timeless, refined", "Solid"),

                template("RFID5001", "Relaxed Fit Hoodie", "Free Assembly", "Tops", "Gray", 28.00, "/images/products/walmart-relaxed-fit-hoodie.jpg", 18, "M", "Relaxed Fit", "Cotton Fleece", "Unisex", "Fall/Winter", "Casual, Streetwear, Lounge", "comfortable, relaxed, affordable, casual", "Solid"),
                template("RFID5002", "Athletic Jogger", "George", "Bottoms", "Dark Blue", 32.00, "/images/products/walmart-athletic-jogger.jpg", 16, "M", "Athletic Fit", "Cotton Blend", "Unisex", "All Season", "Casual, Athletic, Lounge", "comfortable, sporty, relaxed, practical", "Solid"),
                template("RFID5003", "Court Sneaker", "Time and Tru", "Shoes", "White", 24.00, "/images/products/walmart-court-sneaker.jpg", 20, "10", "Standard", "Synthetic Leather", "Unisex", "All Season", "Casual, Everyday, Travel", "clean, affordable, simple, versatile", "Solid"),
                template("RFID5004", "Quilted Puffer", "Free Assembly", "Outerwear", "Olive", 54.00, "/images/products/walmart-quilted-puffer.jpg", 9, "M", "Regular Fit", "Nylon", "Unisex", "Fall/Winter", "Casual, Outdoor, Streetwear", "warm, practical, casual, utility", "Quilted"),

                template("RFID6001", "Ribbed Slim Fit Tee", "A New Day", "Tops", "Cream", 18.00, "/images/products/target-ribbed-slim-fit-tee.jpg", 22, "S", "Slim Fit", "Ribbed Cotton", "Women", "Spring/Summer", "Casual, Brunch, Everyday", "soft, fitted, minimal, feminine", "Ribbed"),
                template("RFID6002", "High Rise Straight Jean", "Universal Thread", "Bottoms", "Light Blue", 36.00, "/images/products/target-high-rise-straight-jeans.jpg", 14, "28", "Straight Fit", "Denim", "Women", "All Season", "Casual, Everyday, Brunch", "classic, denim, relaxed, wearable", "Solid"),
                template("RFID6003", "Platform Sneaker", "A New Day", "Shoes", "White", 32.00, "/images/products/target-platform-sneaker.jpg", 15, "8", "Platform", "Synthetic Leather", "Women", "All Season", "Casual, Brunch, Streetwear", "clean, playful, modern, casual", "Solid"),
                template("RFID6004", "Utility Shirt Jacket", "A New Day", "Outerwear", "Tan", 65.00, "/images/products/target-utility-shirt-jacket.jpg", 7, "M", "Relaxed Fit", "Cotton Twill", "Women", "Spring/Fall", "Casual, Layering, Smart Casual", "utility, casual, neutral, practical", "Solid")
        );
    }

    private DemoProductTemplate template(
            String rfid,
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
                rfid,
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
        Product product = productRepository.findById(rfid).orElseGet(Product::new);

        product.setRfid(rfid);
        product.setItemName(itemName);
        product.setBrand(brand);
        product.setCategory(category);
        product.setColor(color);
        product.setPrice(price);
        product.setImageUrl(imageUrl);
        product.setStockQuantity(stockQuantity);

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
            String rfid,
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