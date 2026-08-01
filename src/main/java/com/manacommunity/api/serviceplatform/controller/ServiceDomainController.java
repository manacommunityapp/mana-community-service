package com.manacommunity.api.serviceplatform.controller;

import com.manacommunity.api.serviceplatform.dto.request.CreateServiceDomainRequest;
import com.manacommunity.api.serviceplatform.dto.response.ServiceDomainResponse;
import com.manacommunity.api.serviceplatform.dto.response.ServiceProviderResponse;
import com.manacommunity.api.serviceplatform.dto.response.ServiceRequestResponse;
import com.manacommunity.api.serviceplatform.dto.response.WorkOrderResponse;
import com.manacommunity.api.serviceplatform.dto.request.AssignProviderRequest;
import com.manacommunity.api.serviceplatform.service.ServiceCatalogService;
import com.manacommunity.api.serviceplatform.service.ServiceProviderService;
import com.manacommunity.api.serviceplatform.service.ServiceRequestService;
import com.manacommunity.api.serviceplatform.service.WorkOrderService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-platform")
@RequiredArgsConstructor
public class ServiceDomainController {

    private final ServiceCatalogService catalogService;
    private final ServiceProviderService providerService;
    private final ServiceRequestService requestService;
    private final WorkOrderService workOrderService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/domains")
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<List<ServiceDomainResponse>> listDomains(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(catalogService.listDomains(communityId));
    }

    @GetMapping("/domains/{id}")
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<ServiceDomainResponse> getDomain(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getDomain(id));
    }

    @PostMapping("/domains")
    @PreAuthorize("hasAuthority('Manage Service Catalog')")
    public ResponseEntity<ServiceDomainResponse> createDomain(
            @Valid @RequestBody CreateServiceDomainRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogService.createDomain(request, communityId));
    }

    @PutMapping("/domains/{id}")
    @PreAuthorize("hasAuthority('Manage Service Catalog')")
    public ResponseEntity<ServiceDomainResponse> updateDomain(
            @PathVariable Long id,
            @Valid @RequestBody CreateServiceDomainRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(catalogService.updateDomain(id, request, communityId));
    }

    @DeleteMapping("/domains/{id}")
    @PreAuthorize("hasAuthority('Manage Service Catalog')")
    public ResponseEntity<Void> deleteDomain(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        catalogService.deleteDomain(id, communityId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/providers")
    @PreAuthorize("hasAuthority('Manage Service Providers')")
    public ResponseEntity<Page<ServiceProviderResponse>> listProviders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(providerService.listProviders(communityId, status, page, size));
    }

    @PatchMapping("/admin/providers/{id}/verify")
    @PreAuthorize("hasAuthority('Manage Service Providers')")
    public ResponseEntity<ServiceProviderResponse> verifyProvider(
            @PathVariable Long id,
            @RequestParam String action) {
        return ResponseEntity.ok(providerService.verifyProvider(id, action));
    }

    @GetMapping("/admin/requests")
    @PreAuthorize("hasAuthority('Manage Service Requests')")
    public ResponseEntity<Page<ServiceRequestResponse>> listAllRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(requestService.listAllRequests(communityId, status, page, size));
    }

    @PatchMapping("/admin/requests/{id}/assign")
    @PreAuthorize("hasAuthority('Manage Service Requests')")
    public ResponseEntity<ServiceRequestResponse> assignProvider(
            @PathVariable Long id,
            @Valid @RequestBody AssignProviderRequest request) {
        return ResponseEntity.ok(requestService.assignProvider(id, request));
    }

    @GetMapping("/admin/work-orders")
    @PreAuthorize("hasAuthority('Manage Work Orders')")
    public ResponseEntity<Page<WorkOrderResponse>> listAllWorkOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(workOrderService.listAllWorkOrders(communityId, status, page, size));
    }
}
