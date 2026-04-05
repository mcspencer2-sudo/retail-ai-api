package com.retailai.service;

import com.retailai.model.BagItem;
import com.retailai.repository.BagItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BagService {

    private final BagItemRepository repository;

    public BagService(BagItemRepository repository) {
        this.repository = repository;
    }

    public BagItem save(BagItem item) {
        return repository.save(item);
    }

    public List<BagItem> getAll() {
        return repository.findAll();
    }
}