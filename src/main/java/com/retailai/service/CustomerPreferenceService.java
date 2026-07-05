package com.retailai.service;

import com.retailai.customer.CustomerPreference;
import com.retailai.dto.CustomerPreferenceRequest;
import com.retailai.repository.CustomerPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CustomerPreferenceService {

    private final CustomerPreferenceRepository customerPreferenceRepository;

    public CustomerPreferenceService(CustomerPreferenceRepository customerPreferenceRepository) {
        this.customerPreferenceRepository = customerPreferenceRepository;
    }

    @Transactional(readOnly = true)
    public CustomerPreference getPreferences(String userId, String email, String storeCode) {
        String safeUserId = clean(userId);
        String safeEmail = clean(email);
        String safeStoreCode = clean(storeCode);

        if (!safeUserId.isBlank() && !safeStoreCode.isBlank()) {
            return customerPreferenceRepository
                    .findByUserIdAndStoreCode(safeUserId, safeStoreCode)
                    .orElseGet(() -> buildDefaultPreference(safeUserId, safeEmail, safeStoreCode));
        }

        if (!safeEmail.isBlank() && !safeStoreCode.isBlank()) {
            return customerPreferenceRepository
                    .findByEmailAndStoreCode(safeEmail, safeStoreCode)
                    .orElseGet(() -> buildDefaultPreference(safeUserId, safeEmail, safeStoreCode));
        }

        return buildDefaultPreference(safeUserId, safeEmail, safeStoreCode);
    }

    @Transactional
    public CustomerPreference savePreferences(
            String userId,
            String email,
            String storeCode,
            CustomerPreferenceRequest request
    ) {
        String safeUserId = clean(userId);
        String safeEmail = clean(email);
        String safeStoreCode = clean(storeCode);

        if (safeUserId.isBlank() && safeEmail.isBlank()) {
            throw new IllegalArgumentException("User context is missing.");
        }

        if (safeStoreCode.isBlank()) {
            throw new IllegalArgumentException("Store context is missing.");
        }

        validateBudget(request);

        CustomerPreference preference = customerPreferenceRepository
                .findByUserIdAndStoreCode(safeUserId, safeStoreCode)
                .or(() -> customerPreferenceRepository.findByEmailAndStoreCode(safeEmail, safeStoreCode))
                .orElseGet(() -> {
                    CustomerPreference created = new CustomerPreference();
                    created.setUserId(safeUserId);
                    created.setEmail(safeEmail);
                    created.setStoreCode(safeStoreCode);
                    created.setCreatedAt(LocalDateTime.now());
                    return created;
                });

        preference.setUserId(safeUserId);
        preference.setEmail(safeEmail);
        preference.setStoreCode(safeStoreCode);

        preference.setSizeTop(clean(request.getSizeTop()));
        preference.setSizeBottom(clean(request.getSizeBottom()));
        preference.setShoeSize(clean(request.getShoeSize()));

        preference.setBudgetMin(request.getBudgetMin());
        preference.setBudgetMax(request.getBudgetMax());

        preference.setFavoriteColors(clean(request.getFavoriteColors()));
        preference.setAvoidedColors(clean(request.getAvoidedColors()));

        preference.setFitPreference(cleanOrDefault(request.getFitPreference(), "Regular"));
        preference.setGenderStyle(cleanOrDefault(request.getGenderStyle(), "Any"));
        preference.setPreferredMaterials(clean(request.getPreferredMaterials()));
        preference.setOccasionPriority(cleanOrDefault(request.getOccasionPriority(), "Everyday"));
        preference.setStyleKeywords(clean(request.getStyleKeywords()));
        preference.setDislikedStyles(clean(request.getDislikedStyles()));
        preference.setNotes(clean(request.getNotes()));

        preference.setUpdatedAt(LocalDateTime.now());

        return customerPreferenceRepository.save(preference);
    }

    @Transactional
    public void resetPreferences(String userId, String email, String storeCode) {
        String safeUserId = clean(userId);
        String safeStoreCode = clean(storeCode);

        if (!safeUserId.isBlank() && !safeStoreCode.isBlank()) {
            customerPreferenceRepository.deleteByUserIdAndStoreCode(safeUserId, safeStoreCode);
        }
    }

    private CustomerPreference buildDefaultPreference(String userId, String email, String storeCode) {
        CustomerPreference preference = new CustomerPreference();

        preference.setUserId(clean(userId));
        preference.setEmail(clean(email));
        preference.setStoreCode(clean(storeCode));

        preference.setFitPreference("Regular");
        preference.setGenderStyle("Any");
        preference.setOccasionPriority("Everyday");

        return preference;
    }

    private void validateBudget(CustomerPreferenceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Preference request is missing.");
        }

        Double min = request.getBudgetMin();
        Double max = request.getBudgetMax();

        if (min != null && min < 0) {
            throw new IllegalArgumentException("Budget minimum cannot be negative.");
        }

        if (max != null && max < 0) {
            throw new IllegalArgumentException("Budget maximum cannot be negative.");
        }

        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException("Budget minimum cannot be greater than budget maximum.");
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanOrDefault(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? fallback : cleaned;
    }
}