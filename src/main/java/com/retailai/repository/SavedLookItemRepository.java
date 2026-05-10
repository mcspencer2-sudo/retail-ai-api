package com.retailai.repository;

import com.retailai.model.SavedLookItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedLookItemRepository extends JpaRepository<SavedLookItem, Long> {
}