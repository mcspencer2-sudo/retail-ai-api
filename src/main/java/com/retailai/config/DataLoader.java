package com.retailai.config;

import com.retailai.model.Product;
import com.retailai.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final ProductRepository productRepository;

    public DataLoader(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            productRepository.save(buildProduct(
                    "RFID1001",
                    "Macy's",
                    "MACY001",
                    "MACY-NYC-01",
                    "Herald Square",
                    "Oxford Shirt",
                    "Polo Ralph Lauren",
                    "Tops",
                    "Blue",
                    "/images/products/oxford-shirt.jpg",
                    80.00,
                    12
            ));

            productRepository.save(buildProduct(
                    "RFID2001",
                    "Zara",
                    "ZARA001",
                    "ZARA-SOHO-01",
                    "SoHo",
                    "Slim Fit Trousers",
                    "Zara",
                    "Bottoms",
                    "Black",
                    "/images/products/slim-fit-trousers.jpg",
                    65.00,
                    10
            ));

            productRepository.save(buildProduct(
                    "RFID3001",
                    "Nike",
                    "NIKE001",
                    "NIKE-NYC-01",
                    "Nike NYC",
                    "Air Max Sneakers",
                    "Nike",
                    "Shoes",
                    "White",
                    "/images/products/air-max-sneakers.jpg",
                    140.00,
                    8
            ));

            productRepository.save(buildProduct(
                    "RFID4001",
                    "Nordstrom",
                    "NORD001",
                    "NORD-NYC-01",
                    "57th Street",
                    "Slim Fit Trousers",
                    "Theory",
                    "Bottoms",
                    "Charcoal",
                    "/images/products/slim-fit-trousers.jpg",
                    110.00,
                    9
            ));

            productRepository.save(buildProduct(
                    "RFID5001",
                    "Nike",
                    "NIKE001",
                    "NIKE-SOHO-02",
                    "Nike SoHo",
                    "Streetwear Hoodie",
                    "Nike",
                    "Tops",
                    "Black",
                    "/images/products/streetwear-hoodie.jpg",
                    95.00,
                    7
            ));

            productRepository.save(buildProduct(
                    "RFID6001",
                    "Macy's",
                    "MACY001",
                    "MACY-BK-02",
                    "Brooklyn",
                    "Slim Fit Jeans",
                    "Levi's",
                    "Bottoms",
                    "Dark Blue",
                    "/images/products/slim-fit-jeans.jpg",
                    75.00,
                    11
            ));

            productRepository.save(buildProduct(
                    "RFID7001",
                    "Zara",
                    "ZARA001",
                    "ZARA-5TH-02",
                    "5th Avenue",
                    "Oversized Trench Coat",
                    "Zara",
                    "Outerwear",
                    "Beige",
                    "/images/products/oversized-trench-coat.jpg",
                    120.00,
                    6
            ));

            productRepository.save(buildProduct(
                    "RFID8001",
                    "Nordstrom",
                    "NORD001",
                    "NORD-WTC-02",
                    "World Trade Center",
                    "Chelsea Boots",
                    "Steve Madden",
                    "Shoes",
                    "Brown",
                    "/images/products/chelsea-boots.jpg",
                    140.00,
                    5
            ));
        }
    }

    private Product buildProduct(
            String rfid,
            String retailerName,
            String retailerKey,
            String storeCode,
            String storeName,
            String itemName,
            String brand,
            String category,
            String color,
            String imageUrl,
            double price,
            int stockQuantity
    ) {
        Product product = new Product();
        product.setRfid(rfid);
        product.setRetailerName(retailerName);
        product.setRetailerKey(retailerKey);
        product.setStoreCode(storeCode);
        product.setStoreName(storeName);
        product.setItemName(itemName);
        product.setBrand(brand);
        product.setCategory(category);
        product.setColor(color);
        product.setImageUrl(imageUrl);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        product.setAvailable(true);
        product.setActive(true);
        return product;
    }
}