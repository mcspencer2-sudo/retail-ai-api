package com.retailai.repository;

import com.retailai.customer.CustomerPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerPreferenceRepository extends JpaRepository<CustomerPreference, Long> {

    Optional<CustomerPreference> findByUserIdAndStoreCode(String userId, String storeCode);

    Optional<CustomerPreference> findByEmailAndStoreCode(String email, String storeCode);

    boolean existsByUserIdAndStoreCode(String userId, String storeCode);

    void deleteByUserIdAndStoreCode(String userId, String storeCode);
}