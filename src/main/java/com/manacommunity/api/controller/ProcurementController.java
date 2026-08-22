package com.manacommunity.api.controller;

import com.manacommunity.api.model.ProcurementStatus;
import com.manacommunity.api.model.PurchaseRequest;
import com.manacommunity.api.repository.PurchaseRequestRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procurement/requests")
public class ProcurementController {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final LoggedInUserService loggedInUserService;

    public ProcurementController(PurchaseRequestRepository purchaseRequestRepository,
                                 LoggedInUserService loggedInUserService) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.loggedInUserService = loggedInUserService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<PurchaseRequest>> getRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.ok(purchaseRequestRepository.findByCommunityIdOrderByCreatedAtDesc(communityId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<PurchaseRequest> getRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return purchaseRequestRepository.findByIdAndCommunityId(id, communityId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<PurchaseRequest> createRequest(
            @RequestBody PurchaseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        request.setCommunityId(communityId);
        if (request.getStatus() == null) {
            request.setStatus(ProcurementStatus.REQUESTED);
        }
        return ResponseEntity.ok(purchaseRequestRepository.save(request));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<PurchaseRequest> updateStatus(
            @PathVariable Long id,
            @RequestParam ProcurementStatus status,
            @RequestParam(required = false) String approvalNotes,
            @RequestParam(required = false) String poNumber,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = resolveCommunityId(principal);
        return purchaseRequestRepository.findByIdAndCommunityId(id, communityId)
                .map(pr -> {
                    pr.setStatus(status);
                    if (approvalNotes != null) pr.setApprovalNotes(approvalNotes);
                    if (poNumber != null) pr.setPurchaseOrderNumber(poNumber);
                    return ResponseEntity.ok(purchaseRequestRepository.save(pr));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Long resolveCommunityId(UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return user.getCommunity() != null ? user.getCommunity().getId() : null;
    }
}
