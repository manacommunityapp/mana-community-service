package com.manacommunity.api.platform.tenant.provisioning;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Multi-tenant onboarding and isolation diagnostics controller.
 *
 * <p>Supports both {@code /api/community/provision} and {@code /api/communities/provision}
 * to onboard a new society in a single transactional API call.</p>
 */
@RestController
@RequestMapping({"/api/communities", "/api/community"})
@RequiredArgsConstructor
public class CommunityProvisioningController {

    private final CommunityProvisioningService provisioningService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PostMapping("/provision")
    public ResponseEntity<CommunityProvisionResponse> provisionCommunity(
            @Valid @RequestBody CommunityProvisionRequest request
    ) {
        CommunityProvisionResponse response = provisioningService.provisionCommunity(request);
        HttpStatus status = "PROVISIONED".equals(response.getStatus()) ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @GetMapping("/{id}/isolation-status")
    public ResponseEntity<Map<String, Object>> getIsolationStatus(@PathVariable Long id) {
        return ResponseEntity.ok(provisioningService.getIsolationStatus(id));
    }
}
