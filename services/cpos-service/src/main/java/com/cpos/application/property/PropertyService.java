package com.cpos.application.property;

import com.cpos.domain.property.model.Property;
import com.cpos.domain.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Property Master Service — Core business logic for property digital twins.
 * Handles property lifecycle: creation, status transitions, search, analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyService {

    private final PropertyRepository propertyRepository;

    @Transactional
    public Property createProperty(Property property) {
        // Generate unique permanent property code
        property.setPropertyCode(generatePropertyCode(property));
        property.setPropertyStatus("ACTIVE");
        property.setOccupancyStatus("VACANT");
        property.setConstructionStatus("READY");
        Property saved = propertyRepository.save(property);
        log.info("Created property with code: {}", saved.getPropertyCode());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Property> getCommunityProperties(UUID tenantId, UUID communityId, String type, String status, Pageable pageable) {
        return propertyRepository.searchProperties(tenantId, communityId, type, status, pageable);
    }

    @Transactional(readOnly = true)
    public Property getByCode(String propertyCode) {
        return propertyRepository.findByPropertyCodeAndIsDeletedFalse(propertyCode)
                .orElseThrow(() -> new RuntimeException("Property not found: " + propertyCode));
    }

    @Transactional(readOnly = true)
    public List<Property> getVacantProperties(UUID tenantId) {
        return propertyRepository.findByTenantIdAndOccupancyStatusAndIsDeletedFalse(tenantId, "VACANT");
    }

    @Transactional(readOnly = true)
    public List<Property> getListedForSale(UUID tenantId) {
        return propertyRepository.findByTenantIdAndPropertyStatusAndIsDeletedFalse(tenantId, "LISTED_FOR_SALE");
    }

    @Transactional
    public Property updateOccupancyStatus(UUID id, String newStatus) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found"));
        property.setOccupancyStatus(newStatus);
        return propertyRepository.save(property);
    }

    @Transactional
    public Property updateMarketValue(UUID id, java.math.BigDecimal value) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found"));
        property.setCurrentMarketValue(value);
        return propertyRepository.save(property);
    }

    @Transactional
    public void deleteProperty(UUID id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found"));
        property.setIsDeleted(true);
        propertyRepository.save(property);
    }

    private String generatePropertyCode(Property property) {
        String communityShort = property.getCommunityId().toString().substring(0, 8).toUpperCase();
        String unit = property.getUnitNumber().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return "CPOS-" + communityShort + "-" + unit;
    }
}
