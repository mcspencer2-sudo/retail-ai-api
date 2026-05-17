package com.retailai.controller;

import com.retailai.dto.SavedLookDTO;
import com.retailai.dto.SavedLookRequestDTO;
import com.retailai.service.SavedLookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/macy-stylist/saved-looks")
public class SavedLookController {

    private final SavedLookService savedLookService;

    public SavedLookController(SavedLookService savedLookService) {
        this.savedLookService = savedLookService;
    }

    @PostMapping
    public ResponseEntity<?> saveLook(@RequestBody SavedLookRequestDTO request) {
        try {
            SavedLookDTO savedLook = savedLookService.saveLook(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedLook);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getSavedLooks() {
        try {
            List<SavedLookDTO> looks = savedLookService.getSavedLooks();
            return ResponseEntity.ok(looks);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSavedLookById(@PathVariable Long id) {
        try {
            SavedLookDTO look = savedLookService.getSavedLookById(id);
            return ResponseEntity.ok(look);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSavedLook(@PathVariable Long id) {
        try {
            savedLookService.deleteSavedLook(id);
            return ResponseEntity.ok("Saved look removed.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> clearSavedLooks() {
        try {
            savedLookService.clearSavedLooks();
            return ResponseEntity.ok("All saved looks cleared.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}