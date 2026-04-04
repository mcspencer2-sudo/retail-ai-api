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

            // Macy's
            productRepository.save(new Product(
                    "RFID1001",
                    "Macy's",
                    "Oxford Shirt",
                    "Tops",
                    "https://images.unsplash.com/photo-1603252109303-2751441dd157?auto=format&fit=crop&w=900&q=80",
                    65.00
            ));

            productRepository.save(new Product(
                    "RFID1002",
                    "Macy's",
                    "Slim Fit Jeans",
                    "Bottoms",
                    "https://images.unsplash.com/photo-1541099649105-f69ad21f3246?auto=format&fit=crop&w=900&q=80",
                    80.00
            ));

            // Zara
            productRepository.save(new Product(
                    "RFID2001",
                    "Zara",
                    "Oversized Trench Coat",
                    "Outerwear",
                    "https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=900&q=80",
                    120.00
            ));

            productRepository.save(new Product(
                    "RFID2002",
                    "Zara",
                    "Slim Fit Blazer",
                    "Formal",
                    "https://images.unsplash.com/photo-1593030761757-71fae45fa0e7?auto=format&fit=crop&w=900&q=80",
                    110.00
            ));

            // Nike
            productRepository.save(new Product(
                    "RFID3001",
                    "Nike",
                    "Streetwear Hoodie",
                    "Tops",
                    "https://images.unsplash.com/photo-1578681994506-b8f463449011?auto=format&fit=crop&w=900&q=80",
                    95.00
            ));

            productRepository.save(new Product(
                    "RFID3002",
                    "Nike",
                    "Air Max Sneakers",
                    "Shoes",
                    "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80",
                    130.00
            ));

            // Nordstrom
            productRepository.save(new Product(
                    "RFID4001",
                    "Nordstrom",
                    "Leather Chelsea Boots",
                    "Shoes",
                    "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80",
                    150.00
            ));

            productRepository.save(new Product(
                    "RFID4002",
                    "Nordstrom",
                    "Wool Coat",
                    "Outerwear",
                    "https://images.unsplash.com/photo-1544441893-675973e31985?auto=format&fit=crop&w=900&q=80",
                    180.00
            ));

            System.out.println("✅ Product seed data loaded successfully.");
        } else {
            System.out.println("ℹ️ Products already exist. Skipping seed load.");
        }
    }
}