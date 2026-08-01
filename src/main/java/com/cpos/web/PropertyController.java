package com.cpos.web;

import com.cpos.application.property.PropertyService;
import com.cpos.domain.property.model.Property;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Property Master Controller — REST API for Property Digital Twin operations.
 * Base path: /api/v1/cpos/properties
 */
@RestController
@RequestMapping("/api/v1/cpos/properties")
@RequiredArgsConstructor
@CrossOrigin
public class PropertyController {

    private final PropertyService propertyService;

    @PostMapping
    public ResponseEntity<Property> createProperty(@RequestBody Property property) {
        return ResponseEntity.ok(propertyService.createProperty(property));
    }

    @GetMapping("/community/{communityId}")
    public ResponseEntity<Page<Property>> getCommunityProperties(
            @PathVariable UUID communityId,
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(propertyService.getCommunityProperties(
                tenantId, communityId, type, status,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/code/{propertyCode}")
    public ResponseEntity<Property> getByCode(@PathVariable String propertyCode) {
        return ResponseEntity.ok(propertyService.getByCode(propertyCode));
    }

    @GetMapping("/vacant")
    public ResponseEntity<List<Property>> getVacant(@RequestParam UUID tenantId) {
        return ResponseEntity.ok(propertyService.getVacantProperties(tenantId));
    }

    @GetMapping("/for-sale")
    public ResponseEntity<List<Property>> getForSale(@RequestParam UUID tenantId) {
        return ResponseEntity.ok(propertyService.getListedForSale(tenantId));
    }

    @PatchMapping("/{id}/occupancy-status")
    public ResponseEntity<Property> updateOccupancyStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        return ResponseEntity.ok(propertyService.updateOccupancyStatus(id, status));
    }

    @PatchMapping("/{id}/market-value")
    public ResponseEntity<Property> updateMarketValue(
            @PathVariable UUID id,
            @RequestParam BigDecimal value) {
        return ResponseEntity.ok(propertyService.updateMarketValue(id, value));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProperty(@PathVariable UUID id) {
        propertyService.deleteProperty(id);
        return ResponseEntity.noContent().build();
    }
}
