// serviceplatform/controller/ServiceOfferingController.java
package com.manacommunity.api.serviceplatform.controller;

import com.manacommunity.api.serviceplatform.dto.request.CreateOfferingRequest;
import com.manacommunity.api.serviceplatform.dto.response.ServiceOfferingResponse;
import com.manacommunity.api.serviceplatform.service.ServiceProviderService;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-platform/providers/me/offerings")
@RequiredArgsConstructor
public class ServiceOfferingController {

    private final ServiceProviderService providerService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServiceOfferingResponse>> listMyOfferings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(providerService.listMyOfferings(principal.getId()));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceOfferingResponse> createOffering(
            @Valid @RequestBody CreateOfferingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerService.createOffering(principal.getId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceOfferingResponse> updateOffering(
            @PathVariable Long id,
            @Valid @RequestBody CreateOfferingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(providerService.updateOffering(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteOffering(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        providerService.deleteOffering(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}