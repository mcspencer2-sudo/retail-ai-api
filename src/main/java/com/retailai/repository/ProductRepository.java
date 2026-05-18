package com.retailai.repository;

import com.retailai.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByRetailerKey(String retailerKey);

    List<Product> findByStoreCode(String storeCode);

    List<Product> findByRetailerKeyAndStoreCode(String retailerKey, String storeCode);

    List<Product> findByRetailerKeyAndAvailableTrue(String retailerKey);

    List<Product> findByStoreCodeAndAvailableTrue(String storeCode);

    List<Product> findByRetailerKeyAndStoreCodeAndAvailableTrue(String retailerKey, String storeCode);

    Optional<Product> findByRfidAndRetailerKey(String rfid, String retailerKey);

    Optional<Product> findByRfidAndRetailerKeyAndStoreCode(String rfid, String retailerKey, String storeCode);


    List<Product> findByRetailerKeyAndActiveTrueAndAvailableTrueAndStockQuantityGreaterThan(
            String retailerKey,
            Integer stockQuantity
    );

    List<Product> findByStoreCodeAndActiveTrueAndAvailableTrueAndStockQuantityGreaterThan(
            String storeCode,
            Integer stockQuantity
    );

    List<Product> findByRetailerKeyAndStoreCodeAndActiveTrueAndAvailableTrueAndStockQuantityGreaterThan(
            String retailerKey,
            String storeCode,
            Integer stockQuantity
    );

    List<Product> findByRetailerKeyAndCategoryIgnoreCaseAndActiveTrueAndAvailableTrueAndStockQuantityGreaterThan(
            String retailerKey,
            String category,
            Integer stockQuantity
    );

    List<Product> findByStoreCodeAndCategoryIgnoreCaseAndActiveTrueAndAvailableTrueAndStockQuantityGreaterThan(
            String storeCode,
            String category,
            Integer stockQuantity
    );

    List<Product> findByRetailerKeyAndStoreCodeAndCategoryIgnoreCaseAndActiveTrueAndAvailableTrueAndStockQuantityGreaterThan(
            String retailerKey,
            String storeCode,
            String category,
            Integer stockQuantity
    );

    List<Product> findByCategoryIgnoreCaseAndActiveTrueAndAvailableTrueAndStockQuantityGreaterThan(
            String category,
            Integer stockQuantity
    );

    List<Product> findByRetailerKeyAndBrandIgnoreCaseAndActiveTrueAndAvailableTrueAndStockQuantityGreaterThan(
            String retailerKey,
            String brand,
            Integer stockQuantity
    );

    List<Product> findByStoreCodeAndBrandIgnoreCaseAndActiveTrueAndAvailableTrueAndStockQuantityGreaterThan(
            String storeCode,
            String brand,
            Integer stockQuantity
    );

    List<Product> findByRetailerKeyAndStoreCodeAndBrandIgnoreCaseAndActiveTrueAndAvailableTrueAndStockQuantityGreaterThan(
            String retailerKey,
            String storeCode,
            String brand,
            Integer stockQuantity
    );
}