package com.retailai.controller;

import com.retailai.dto.SavedLookDTO;
import com.retailai.dto.SavedLookRequestDTO;
import com.retailai.service.SavedLookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/macy-stylist/saved-looks")
public class SavedLookController {

    private static final Logger log = LoggerFactory.getLogger(SavedLookController.class);

    private static final HttpStatusCode UNPROCESSABLE_ENTITY = HttpStatusCode.valueOf(422);

    private final SavedLookService savedLookService;

    public SavedLookController(SavedLookService savedLookService) {
        this.savedLookService = savedLookService;
    }

    @PostMapping
    public ResponseEntity<?> saveLook(@RequestBody SavedLookRequestDTO request) {
        try {
            SavedLookDTO savedLook = savedLookService.saveLook(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedLook);
        } catch (SecurityException e) {
            return unauthorized("Saved look authentication failed.", e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (IllegalStateException e) {
            return unprocessable(e);
        } catch (RuntimeException e) {
            return serverError("Saved look save failed.", e);
        }
    }

    @GetMapping
    public ResponseEntity<?> getSavedLooks() {
        try {
            List<SavedLookDTO> looks = savedLookService.getSavedLooks();
            return ResponseEntity.ok(looks);
        } catch (SecurityException e) {
            return unauthorized("Saved look authentication failed.", e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (RuntimeException e) {
            return serverError("Saved look load failed.", e);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSavedLookById(@PathVariable Long id) {
        try {
            SavedLookDTO look = savedLookService.getSavedLookById(id);
            return ResponseEntity.ok(look);
        } catch (SecurityException e) {
            return unauthorized("Saved look authentication failed.", e);
        } catch (IllegalArgumentException e) {
            return notFound(e);
        } catch (RuntimeException e) {
            return serverError("Saved look detail load failed.", e);
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateSavedLook(
            @PathVariable Long id,
            @RequestBody SavedLookRequestDTO request
    ) {
        try {
            SavedLookDTO updatedLook = savedLookService.updateSavedLook(id, request);
            return ResponseEntity.ok(updatedLook);
        } catch (SecurityException e) {
            return unauthorized("Saved look authentication failed.", e);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (IllegalStateException e) {
            return unprocessable(e);
        } catch (RuntimeException e) {
            return serverError("Saved look update failed.", e);
        }
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<?> checkSavedLookAvailability(@PathVariable Long id) {
        try {
            Map<String, Object> availability = savedLookService.checkSavedLookAvailability(id);
            return ResponseEntity.ok(availability);
        } catch (SecurityException e) {
            return unauthorized("Saved look authentication failed.", e);
        } catch (IllegalArgumentException e) {
            return notFound(e);
        } catch (IllegalStateException e) {
            return unprocessable(e);
        } catch (RuntimeException e) {
            return serverError("Saved look availability check failed.", e);
        }
    }

    @PostMapping("/{id}/regenerate")
    public ResponseEntity<?> regenerateSavedLook(@PathVariable Long id) {
        try {
            SavedLookDTO regeneratedLook = savedLookService.regenerateSavedLook(id);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("mode", "SAFE_REHYDRATE");
            body.put("message", "Saved look reopened as a fresh styling workspace.");
            body.put("look", regeneratedLook);

            return ResponseEntity.ok(body);
        } catch (SecurityException e) {
            return unauthorized("Saved look authentication failed.", e);
        } catch (IllegalArgumentException e) {
            return notFound(e);
        } catch (IllegalStateException e) {
            return unprocessable(e);
        } catch (RuntimeException e) {
            return serverError("Saved look regeneration failed.", e);
        }
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<?> createPublicShareToken(@PathVariable Long id) {
        try {
            Map<String, Object> share = savedLookService.createPublicShareToken(id);
            return ResponseEntity.ok(share);
        } catch (SecurityException e) {
            return unauthorized("Saved look authentication failed.", e);
        } catch (IllegalArgumentException e) {
            return notFound(e);
        } catch (IllegalStateException e) {
            return unprocessable(e);
        } catch (RuntimeException e) {
            return serverError("Saved look share link creation failed.", e);
        }
    }

    @DeleteMapping("/{id}/share")
    public ResponseEntity<?> disablePublicShare(@PathVariable Long id) {
        try {
            Map<String, Object> share = savedLookService.disablePublicShare(id);
            return ResponseEntity.ok(share);
        } catch (SecurityException e) {
            return unauthorized("Saved look authentication failed.", e);
        } catch (IllegalArgumentException e) {
            return notFound(e);
        } catch (IllegalStateException e) {
            return unprocessable(e);
        } catch (RuntimeException e) {
            return serverError("Saved look share link disable failed.", e);
        }
    }

    @GetMapping("/shared/{shareToken}")
    public ResponseEntity<?> getPublicSharedLook(@PathVariable String shareToken) {
        try {
            SavedLookDTO sharedLook = savedLookService.getPublicSharedLook(shareToken);
            return ResponseEntity.ok(sharedLook);
        } catch (IllegalArgumentException e) {
            return notFound(e);
        } catch (IllegalStateException e) {
            return unprocessable(e);
        } catch (RuntimeException e) {
            return serverError("Shared saved look load failed.", e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSavedLook(@PathVariable Long id) {
        try {
            savedLookService.deleteSavedLook(id);
            return ResponseEntity.ok(messageBody("Saved look removed."));
        } catch (SecurityException e) {
            return unauthorized("Saved look authentication failed.", e);
        } catch (IllegalArgumentException e) {
            return notFound(e);
        } catch (RuntimeException e) {
            return serverError("Saved look delete failed.", e);
        }
    }

    @DeleteMapping
    public ResponseEntity<?> clearSavedLooks() {
        try {
            savedLookService.clearSavedLooks();
            return ResponseEntity.ok(messageBody("All saved looks cleared."));
        } catch (SecurityException e) {
            return unauthorized("Saved look authentication failed.", e);
        } catch (RuntimeException e) {
            return serverError("Saved looks clear failed.", e);
        }
    }

    private ResponseEntity<Map<String, Object>> badRequest(Exception e) {
        return ResponseEntity.badRequest().body(errorBody(safeMessage(e)));
    }

    private ResponseEntity<Map<String, Object>> notFound(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(safeMessage(e)));
    }

    private ResponseEntity<Map<String, Object>> unauthorized(String message, Exception e) {
        log.warn("{} {}", message, safeMessage(e));

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody(message + " " + safeMessage(e)));
    }

    private ResponseEntity<Map<String, Object>> unprocessable(Exception e) {
        return ResponseEntity.status(UNPROCESSABLE_ENTITY).body(errorBody(safeMessage(e)));
    }

    private ResponseEntity<Map<String, Object>> serverError(String message, Exception e) {
        log.error(message, e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(message + " " + safeMessage(e)));
    }

    private Map<String, Object> messageBody(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message == null || message.isBlank() ? "Request completed." : message);
        return body;
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message == null || message.isBlank() ? "Request failed." : message);
        return body;
    }

    private String safeMessage(Exception e) {
        if (e == null || e.getMessage() == null || e.getMessage().isBlank()) {
            return "Request failed.";
        }

        return e.getMessage().trim();
    }
}