// serviceplatform/controller/ServiceProviderController.java
package com.manacommunity.api.serviceplatform.controller;

import com.manacommunity.api.serviceplatform.dto.request.RegisterProviderRequest;
import com.manacommunity.api.serviceplatform.dto.request.UpdateWorkOrderStatusRequest;
import com.manacommunity.api.serviceplatform.dto.response.ServiceProviderResponse;
import com.manacommunity.api.serviceplatform.dto.response.ServiceRequestResponse;
import com.manacommunity.api.serviceplatform.dto.response.WorkOrderResponse;
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

@RestController
@RequestMapping("/api/service-platform/providers")
@RequiredArgsConstructor
public class ServiceProviderController {

    private final ServiceProviderService providerService;
    private final ServiceRequestService requestService;
    private final WorkOrderService workOrderService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceProviderResponse> register(
            @Valid @RequestBody RegisterProviderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerService.register(request, user));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceProviderResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(providerService.getProfile(principal.getId()));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceProviderResponse> updateMyProfile(
            @Valid @RequestBody RegisterProviderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(providerService.updateProfile(principal.getId(), request));
    }

    @GetMapping("/me/requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ServiceRequestResponse>> myRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(requestService.listRequestsForProvider(principal.getId(), page, size));
    }

    @PatchMapping("/me/requests/{id}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceRequestResponse> acceptRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(requestService.acceptRequest(id, principal.getId()));
    }

    @PatchMapping("/me/requests/{id}/decline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceRequestResponse> declineRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(requestService.declineRequest(id, principal.getId()));
    }

    @GetMapping("/me/work-orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<WorkOrderResponse>> myWorkOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.listProviderWorkOrders(principal.getId(), page, size));
    }

    @PatchMapping("/me/work-orders/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkOrderResponse> updateWorkOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkOrderStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.updateStatus(id, principal.getId(), request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Service Providers')")
    public ResponseEntity<ServiceProviderResponse> getProvider(@PathVariable Long id) {
        return ResponseEntity.ok(providerService.getProfile(id));
    }
}