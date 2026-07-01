package com.manacommunity.api.controller;

import com.manacommunity.api.model.ProcurementStatus;
import com.manacommunity.api.model.PurchaseRequest;
import com.manacommunity.api.repository.PurchaseRequestRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procurement/requests")
public class ProcurementController {

    private final PurchaseRequestRepository purchaseRequestRepository;

    public ProcurementController(PurchaseRequestRepository purchaseRequestRepository) {
        this.purchaseRequestRepository = purchaseRequestRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<PurchaseRequest>> getRequests() {
        return ResponseEntity.ok(purchaseRequestRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<PurchaseRequest> getRequest(@PathVariable Long id) {
        return purchaseRequestRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<PurchaseRequest> createRequest(@RequestBody PurchaseRequest request) {
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
            @RequestParam(required = false) String poNumber) {

        return purchaseRequestRepository.findById(id)
                .map(request -> {
                    request.setStatus(status);
                    if (approvalNotes != null) {
                        request.setApprovalNotes(approvalNotes);
                    }
                    if (poNumber != null) {
                        request.setPurchaseOrderNumber(poNumber);
                    }
                    return ResponseEntity.ok(purchaseRequestRepository.save(request));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
