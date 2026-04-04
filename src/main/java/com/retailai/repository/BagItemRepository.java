package com.retailai.repository;

import com.retailai.model.BagItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BagItemRepository extends JpaRepository<BagItem, Long> {
}