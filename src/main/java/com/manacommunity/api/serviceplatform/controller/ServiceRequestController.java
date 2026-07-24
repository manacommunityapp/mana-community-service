// serviceplatform/controller/ServiceRequestController.java
package com.manacommunity.api.serviceplatform.controller;

import com.manacommunity.api.serviceplatform.dto.request.CreateServiceRequestDto;
import com.manacommunity.api.serviceplatform.dto.response.ServiceRequestResponse;
import com.manacommunity.api.serviceplatform.dto.response.ServiceSearchResult;
import com.manacommunity.api.serviceplatform.service.ServiceRequestService;
import com.manacommunity.api.serviceplatform.service.ServiceSearchService;
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
@RequestMapping("/api/service-platform")
@RequiredArgsConstructor
public class ServiceRequestController {

    private final ServiceRequestService requestService;
    private final ServiceSearchService searchService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<Page<ServiceSearchResult>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long domainId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(searchService.search(communityId, q, domainId, categoryId, page, size));
    }

    @PostMapping("/requests")
    @PreAuthorize("hasAuthority('Create Service Request')")
    public ResponseEntity<ServiceRequestResponse> createRequest(
            @Valid @RequestBody CreateServiceRequestDto request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(requestService.createRequest(request, user));
    }

    @GetMapping("/requests/my")
    @PreAuthorize("hasAuthority('View Service Requests')")
    public ResponseEntity<Page<ServiceRequestResponse>> myRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(requestService.listMyRequests(principal.getId(), page, size));
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('View Service Requests')")
    public ResponseEntity<ServiceRequestResponse> getRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(requestService.getRequest(id, communityId));
    }

    @PatchMapping("/requests/{id}/cancel")
    @PreAuthorize("hasAuthority('Create Service Request')")
    public ResponseEntity<ServiceRequestResponse> cancelRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(requestService.cancelRequest(id, principal.getId(), reason));
    }

    @PatchMapping("/requests/{id}/submit")
    @PreAuthorize("hasAuthority('Create Service Request')")
    public ResponseEntity<ServiceRequestResponse> submitRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(requestService.submitRequest(id, principal.getId()));
    }
}
