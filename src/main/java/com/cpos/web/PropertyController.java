package com.cpos.web;

import com.cpos.application.property.PropertyService;
import com.cpos.domain.common.CommunityUuidBridge;
import com.cpos.domain.property.model.Property;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Property Master Controller â€” REST API for Property Digital Twin operations.
 * Base path: /api/v1/cpos/properties
 *
 * <p>Access control:</p>
 * <ul>
 *   <li>SUPER_ADMIN â€” full cross-community access across all tenants.</li>
 *   <li>ADMIN / COMMUNITY_ADMIN â€” scoped strictly to their own community via {@link CommunityUuidBridge}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/cpos/properties")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMMUNITY_ADMIN')")
public class PropertyController {

    private final PropertyService propertyService;

    private static boolean isSuperAdmin(UserPrincipal p) {
        if (p == null || p.getAuthorities() == null) return false;
        return p.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    private static void assertCommunityAccess(UserPrincipal principal, UUID requestedCommunityId) {
        if (isSuperAdmin(principal)) return;
        if (principal == null || principal.getCommunityId() == null) {
            throw new AccessDeniedException("User is not associated with any community");
        }
        if (!CommunityUuidBridge.matches(principal.getCommunityId(), requestedCommunityId)) {
            throw new AccessDeniedException("Cross-community access denied");
        }
    }

    private void assertPropertyAccess(UserPrincipal principal, UUID propertyId) {
        if (isSuperAdmin(principal)) return;
        Property prop = propertyService.getById(propertyId);
        assertCommunityAccess(principal, prop.getCommunityId());
    }

    @PostMapping
    public ResponseEntity<Property> createProperty(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Property property) {
        if (!isSuperAdmin(principal)) {
            if (property.getCommunityId() == null) {
                property.setCommunityId(CommunityUuidBridge.toUuid(principal.getCommunityId()));
            } else {
                assertCommunityAccess(principal, property.getCommunityId());
            }
            if (property.getTenantId() == null) {
                property.setTenantId(CommunityUuidBridge.toUuid(principal.getCommunityId()));
            }
        }
        return ResponseEntity.ok(propertyService.createProperty(property));
    }

    @GetMapping("/community/{communityId}")
    public ResponseEntity<Page<Property>> getCommunityProperties(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID communityId,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        assertCommunityAccess(principal, communityId);

        UUID effectiveTenantId = isSuperAdmin(principal) ? tenantId : null;

        return ResponseEntity.ok(propertyService.getCommunityProperties(
                effectiveTenantId, communityId, type, status,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/code/{propertyCode}")
    public ResponseEntity<Property> getByCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String propertyCode) {
        Property property = propertyService.getByCode(propertyCode);
        assertCommunityAccess(principal, property.getCommunityId());
        return ResponseEntity.ok(property);
    }

    @GetMapping("/vacant")
    public ResponseEntity<List<Property>> getVacant(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID tenantId) {
        UUID effectiveTenant = isSuperAdmin(principal) ? tenantId : CommunityUuidBridge.toUuid(principal.getCommunityId());
        if (effectiveTenant == null) {
            throw new AccessDeniedException("Tenant ID required");
        }
        return ResponseEntity.ok(propertyService.getVacantProperties(effectiveTenant));
    }

    @GetMapping("/for-sale")
    public ResponseEntity<List<Property>> getForSale(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID tenantId) {
        UUID effectiveTenant = isSuperAdmin(principal) ? tenantId : CommunityUuidBridge.toUuid(principal.getCommunityId());
        if (effectiveTenant == null) {
            throw new AccessDeniedException("Tenant ID required");
        }
        return ResponseEntity.ok(propertyService.getListedForSale(effectiveTenant));
    }

    @PatchMapping("/{id}/occupancy-status")
    public ResponseEntity<Property> updateOccupancyStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam String status) {
        assertPropertyAccess(principal, id);
        return ResponseEntity.ok(propertyService.updateOccupancyStatus(id, status));
    }

    @PatchMapping("/{id}/market-value")
    public ResponseEntity<Property> updateMarketValue(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam BigDecimal value) {
        assertPropertyAccess(principal, id);
        return ResponseEntity.ok(propertyService.updateMarketValue(id, value));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProperty(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        assertPropertyAccess(principal, id);
        propertyService.deleteProperty(id);
        return ResponseEntity.noContent().build();
    }
}
