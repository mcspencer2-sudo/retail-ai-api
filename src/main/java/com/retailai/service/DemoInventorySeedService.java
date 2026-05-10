package com.retailai.service;

import com.retailai.model.Product;
import com.retailai.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DemoInventorySeedService {

    private final ProductRepository productRepository;

    public DemoInventorySeedService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public int seedDemoInventory() {
        List<Product> products = demoProducts();

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

    private List<String> demoRfids() {
        return List.of(
                "RFID1001", "RFID1002", "RFID1003", "RFID1004",

                "RFID2001", "RFID2002", "RFID2003", "RFID2004",
                "RFID2005", "RFID2006", "RFID2007", "RFID2008",
                "RFID2009", "RFID2010", "RFID2011", "RFID2012",
                "RFID2013", "RFID2014", "RFID2015", "RFID2016",

                "RFID3001", "RFID3002", "RFID3003", "RFID3004",

                "RFID4001", "RFID4002", "RFID4003", "RFID4004"
        );
    }

    private List<Product> demoProducts() {
        return List.of(
                product("RFID1001", "Oxford Shirt", "Polo Ralph Lauren", "Tops", "Blue", 80.00, "/images/products/oxford-shirt.jpg", 12, "MACY001", "Macy's", "MACY-NYC-01", "Herald Square"),
                product("RFID1002", "Slim Chino", "Polo Ralph Lauren", "Bottoms", "Khaki", 95.00, "/images/products/slim-chino.jpg", 8, "MACY001", "Macy's", "MACY-NYC-01", "Herald Square"),
                product("RFID1003", "Leather Sneaker", "Cole Haan", "Shoes", "White", 140.00, "/images/products/leather-sneaker.jpg", 6, "MACY001", "Macy's", "MACY-NYC-01", "Herald Square"),
                product("RFID1004", "Wool Blazer", "Calvin Klein", "Outerwear", "Navy", 220.00, "/images/products/wool-blazer.jpg", 4, "MACY001", "Macy's", "MACY-NYC-01", "Herald Square"),

                product("RFID2001", "Cropped Poplin Shirt", "Zara", "Tops", "White", 49.90, "/images/products/cropped-poplin-shirt.png", 14, "ZARA001", "Zara", "ZARA-SOHO-01", "SoHo"),
                product("RFID2002", "Relaxed Cotton Tee", "Zara", "Tops", "Cream", 29.90, "/images/products/relaxed-cotton-tee.png", 18, "ZARA001", "Zara", "ZARA-SOHO-01", "SoHo"),
                product("RFID2003", "Ribbed Knit Top", "Zara", "Tops", "Black", 39.90, "/images/products/ribbed-knit-top.png", 10, "ZARA001", "Zara", "ZARA-SOHO-01", "SoHo"),
                product("RFID2004", "Silky Button Blouse", "Zara", "Tops", "Champagne", 59.90, "/images/products/silky-button-blouse.png", 7, "ZARA001", "Zara", "ZARA-SOHO-01", "SoHo"),
                product("RFID2005", "Wide Leg Trouser", "Zara", "Bottoms", "Beige", 69.90, "/images/products/wide-leg-trouser.png", 11, "ZARA001", "Zara", "ZARA-SOHO-01", "SoHo"),
                product("RFID2006", "Straight Leg Denim", "Zara", "Bottoms", "Blue", 59.90, "/images/products/straight-leg-denim.png", 16, "ZARA001", "Zara", "ZARA-SOHO-01", "SoHo"),
                product("RFID2007", "Pleated Tailored Pant", "Zara", "Bottoms", "Charcoal", 79.90, "/images/products/pleated-tailored-pant.png", 8, "ZARA001", "Zara", "ZARA-SOHO-01", "SoHo"),
                product("RFID2008", "Satin Midi Skirt", "Zara", "Bottoms", "Olive", 55.90, "/images/products/satin-midi-skirt.png", 6, "ZARA001", "Zara", "ZARA-SOHO-01", "SoHo"),
                product("RFID2009", "Minimal Leather Sneaker", "Zara", "Shoes", "White", 89.90, "/images/products/minimal-leather-sneaker.png", 12, "ZARA001", "Zara", "ZARA-SOHO-01", "SoHo"),
                product("RFID2010", "Chunky Sole Loafer", "Zara", "Shoes", "Black", 99.90, "/images/products/chunky-sole-loafer.png", 9, "ZARA001", "Zara", "ZARA-SOHO-01", "SoHo"),
                product("RFID2011", "Strappy Heeled Sandal", "Zara", "Shoes", "Tan", 79.90, "/images/products/strappy-heeled-sandal.png", 5, "ZARA001", "Zara", "ZARA-SOHO-01", "SoHo"),
                product("RFID2012", "Retro Runner Sneaker", "Zara", "Shoes", "Grey", 95.90, "/images/products/retro-runner-sneaker.jpg", 8, "ZARA001", "Zara", "ZARA-SOHO-01", "SoHo"),
                product("RFID2013", "Structured Overshirt", "Zara", "Outerwear", "Stone", 79.90, "/images/products/structured-overshirt.png", 7, "ZARA001", "Zara", "ZARA-SOHO-01", "SoHo"),
                product("RFID2014", "Cropped Moto Jacket", "Zara", "Outerwear", "Black", 119.00, "/images/products/cropped-moto-jacket.png", 4, "ZARA001", "Zara", "ZARA-SOHO-01", "SoHo"),
                product("RFID2015", "Longline Trench Coat", "Zara", "Outerwear", "Camel", 139.00, "/images/products/longline-trench-coat.png", 5, "ZARA001", "Zara", "ZARA-SOHO-01", "SoHo"),
                product("RFID2016", "Soft Knit Cardigan", "Zara", "Outerwear", "Grey", 69.90, "/images/products/soft-knit-cardigan.png", 9, "ZARA001", "Zara", "ZARA-SOHO-01", "SoHo"),

                product("RFID3001", "Dri-FIT Tee", "Nike", "Tops", "Black", 35.00, "/images/products/nike-white-tee.jpg", 20, "NIKE001", "Nike", "NIKE-NYC-01", "Nike NYC"),
                product("RFID3002", "Tech Fleece Joggers", "Nike", "Bottoms", "Gray", 110.00, "/images/products/nike-black-cargo.jpg", 11, "NIKE001", "Nike", "NIKE-NYC-01", "Nike NYC"),
                product("RFID3003", "Air Max Sneakers", "Nike", "Shoes", "White", 115.00, "/images/products/air-max-sneakers.jpg", 9, "NIKE001", "Nike", "NIKE-NYC-01", "Nike NYC"),
                product("RFID3004", "Bomber Jacket", "Nike", "Outerwear", "Olive", 130.00, "/images/products/nike-shell-jacket.jpg", 6, "NIKE001", "Nike", "NIKE-NYC-01", "Nike NYC"),

                product("RFID4001", "Cashmere Crewneck", "Theory", "Tops", "Camel", 185.00, "/images/products/cashmere-crewneck.jpg", 7, "NORD001", "Nordstrom", "NORD-NYC-01", "57th Street"),
                product("RFID4002", "Tailored Trousers", "BOSS", "Bottoms", "Charcoal", 165.00, "/images/products/tailored-trousers.jpg", 6, "NORD001", "Nordstrom", "NORD-NYC-01", "57th Street"),
                product("RFID4003", "Suede Chelsea Boot", "To Boot New York", "Shoes", "Brown", 298.00, "/images/products/suede-chelsea-boot.jpg", 5, "NORD001", "Nordstrom", "NORD-NYC-01", "57th Street"),
                product("RFID4004", "Topcoat", "Vince", "Outerwear", "Black", 395.00, "/images/products/topcoat-vince.jpg", 3, "NORD001", "Nordstrom", "NORD-NYC-01", "57th Street")
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
            String storeName
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
        product.setActive(true);
        product.setAvailable(stockQuantity != null && stockQuantity > 0);

        return product;
    }
}