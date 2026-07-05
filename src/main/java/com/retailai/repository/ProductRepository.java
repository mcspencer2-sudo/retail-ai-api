package com.retailai.repository;

import com.retailai.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByRetailerKey(String retailerKey);

    List<Product> findByStoreCode(String storeCode);

    List<Product> findByRetailerKeyAndStoreCode(
            String retailerKey,
            String storeCode
    );

    List<Product> findByRetailerKeyAndAvailableTrue(String retailerKey);

    List<Product> findByStoreCodeAndAvailableTrue(String storeCode);

    List<Product> findByRetailerKeyAndStoreCodeAndAvailableTrue(
            String retailerKey,
            String storeCode
    );

    Optional<Product> findByRfidAndRetailerKey(
            String rfid,
            String retailerKey
    );

    Optional<Product> findByRfidAndRetailerKeyAndStoreCode(
            String rfid,
            String retailerKey,
            String storeCode
    );

    boolean existsByRfidAndRetailerKeyAndStoreCode(
            String rfid,
            String retailerKey,
            String storeCode
    );

    List<Product> findAllByRfidAndRetailerKeyAndStoreCode(
            String rfid,
            String retailerKey,
            String storeCode
    );

    List<Product> findByRfidInAndRetailerKeyAndStoreCode(
            Collection<String> rfids,
            String retailerKey,
            String storeCode
    );

    List<Product> findByRfidInAndRetailerKeyAndStoreCodeAndActiveTrueAndAvailableTrueAndStockQuantityGreaterThan(
            Collection<String> rfids,
            String retailerKey,
            String storeCode,
            Integer stockQuantity
    );

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

    long countByRetailerKeyAndStoreCodeAndStockQuantityLessThanEqual(
            String retailerKey,
            String storeCode,
            Integer stockQuantity
    );

    long countByRetailerKeyAndStoreCodeAndStockQuantityLessThanEqualAndStockQuantityGreaterThan(
            String retailerKey,
            String storeCode,
            Integer maxStockQuantity,
            Integer minStockQuantity
    );

    long countByRetailerKeyAndStoreCodeAndStockQuantityLessThanEqualAndActiveTrue(
            String retailerKey,
            String storeCode,
            Integer stockQuantity
    );

    long countByRetailerKeyAndStoreCodeAndStockQuantityLessThanEqualAndAvailableTrue(
            String retailerKey,
            String storeCode,
            Integer stockQuantity
    );

    long countByRetailerKeyAndStoreCodeAndActiveTrueAndAvailableTrueAndStockQuantityLessThanEqual(
            String retailerKey,
            String storeCode,
            Integer stockQuantity
    );

    long countByRetailerKeyAndStoreCodeAndActiveTrueAndAvailableTrueAndStockQuantityLessThanEqualAndStockQuantityGreaterThan(
            String retailerKey,
            String storeCode,
            Integer maxStockQuantity,
            Integer minStockQuantity
    );

    List<Product> findTop10ByRetailerKeyAndStoreCodeAndStockQuantityLessThanEqualOrderByStockQuantityAsc(
            String retailerKey,
            String storeCode,
            Integer stockQuantity
    );

    List<Product> findTop10ByRetailerKeyAndStoreCodeAndActiveTrueAndAvailableTrueAndStockQuantityLessThanEqualOrderByStockQuantityAsc(
            String retailerKey,
            String storeCode,
            Integer stockQuantity
    );

    List<Product> findByRetailerKeyAndStoreCodeAndStockQuantityLessThanEqualOrderByStockQuantityAsc(
            String retailerKey,
            String storeCode,
            Integer stockQuantity
    );

    List<Product> findByRetailerKeyAndStoreCodeAndActiveTrueAndAvailableTrueAndStockQuantityLessThanEqualOrderByStockQuantityAsc(
            String retailerKey,
            String storeCode,
            Integer stockQuantity
    );
}