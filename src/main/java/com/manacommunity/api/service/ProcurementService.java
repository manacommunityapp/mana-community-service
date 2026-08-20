package com.manacommunity.api.service;

import com.manacommunity.api.dto.PurchaseRequestDto;
import com.manacommunity.api.model.ProcurementStatus;
import com.manacommunity.api.model.PurchaseRequest;
import com.manacommunity.api.model.Vendor;
import com.manacommunity.api.repository.PurchaseRequestRepository;
import com.manacommunity.api.repository.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProcurementService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final VendorRepository vendorRepository;

    public ProcurementService(PurchaseRequestRepository purchaseRequestRepository,
                              VendorRepository vendorRepository) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.vendorRepository = vendorRepository;
    }

    public List<PurchaseRequest> getRequests(Long communityId, ProcurementStatus status) {
        return status == null
                ? purchaseRequestRepository.findByCommunityIdOrderByCreatedAtDesc(communityId)
                : purchaseRequestRepository.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, status);
    }

    public Optional<PurchaseRequest> getRequest(Long id, Long communityId) {
        return purchaseRequestRepository.findByIdAndCommunityId(id, communityId);
    }

    @Transactional
    public PurchaseRequest createRequest(PurchaseRequestDto dto, Long communityId) {
        PurchaseRequest request = PurchaseRequest.builder()
                .communityId(communityId)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .estimatedAmount(dto.getEstimatedAmount())
                .requestedBy(dto.getRequestedBy())
                .neededBy(dto.getNeededBy())
                .status(ProcurementStatus.REQUESTED)
                .build();
        attachVendor(request, dto.getSelectedVendorId());
        return purchaseRequestRepository.save(request);
    }

    @Transactional
    public Optional<PurchaseRequest> updateStatus(Long id, Long communityId, ProcurementStatus status, PurchaseRequestDto dto) {
        return purchaseRequestRepository.findByIdAndCommunityId(id, communityId).map(request -> {
            request.setStatus(status);
            if (dto != null) {
                request.setApprovalNotes(dto.getApprovalNotes());
                request.setPurchaseOrderNumber(dto.getPurchaseOrderNumber());
                attachVendor(request, dto.getSelectedVendorId());
            }
            return purchaseRequestRepository.save(request);
        });
    }

    private void attachVendor(PurchaseRequest request, Long selectedVendorId) {
        if (selectedVendorId == null) {
            return;
        }
        Vendor vendor = vendorRepository.findById(selectedVendorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + selectedVendorId));
        request.setSelectedVendor(vendor);
    }
}
