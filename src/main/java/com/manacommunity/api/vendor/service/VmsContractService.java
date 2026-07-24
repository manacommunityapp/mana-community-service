package com.manacommunity.api.vendor.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.vendor.dto.ContractRequest;
import com.manacommunity.api.vendor.dto.ContractResponse;
import com.manacommunity.api.vendor.entity.VmsContract;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.repository.VmsContractRepository;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VmsContractService {

    private final VmsContractRepository contractRepo;
    private final VmsVendorRepository vendorRepo;

    @Transactional(readOnly = true)
    public Page<ContractResponse> getCommunityContracts(Long communityId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return contractRepo.findByCommunityIdAndStatus(communityId, VmsContract.ContractStatus.valueOf(status), pageable)
                    .map(this::toResponse);
        }
        return contractRepo.findByCommunityId(communityId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ContractResponse> getVendorContracts(Long vendorId, Pageable pageable) {
        return contractRepo.findByVendorId(vendorId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ContractResponse getById(Long id, Long communityId) {
        return toResponse(contractRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract", id)));
    }

    @Transactional
    public ContractResponse create(ContractRequest req, AppUser createdBy, Community community) {
        VmsVendor vendor = vendorRepo.findById(req.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", req.getVendorId()));

        String contractNumber = "CT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        VmsContract contract = VmsContract.builder()
                .contractNumber(contractNumber)
                .vendor(vendor)
                .title(req.getTitle())
                .description(req.getDescription())
                .contractType(VmsContract.ContractType.valueOf(req.getContractType()))
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .totalValue(req.getTotalValue())
                .paymentTerms(req.getPaymentTerms())
                .autoRenew(req.getAutoRenew() != null ? req.getAutoRenew() : false)
                .renewalNoticeDays(req.getRenewalNoticeDays() != null ? req.getRenewalNoticeDays() : 30)
                .createdBy(createdBy)
                .community(community)
                .build();

        return toResponse(contractRepo.save(contract));
    }

    @Transactional
    public ContractResponse updateStatus(Long id, String status, Long communityId) {
        VmsContract contract = contractRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract", id));
        contract.setStatus(VmsContract.ContractStatus.valueOf(status));
        return toResponse(contractRepo.save(contract));
    }

    private ContractResponse toResponse(VmsContract c) {
        return ContractResponse.builder()
                .id(c.getId())
                .contractNumber(c.getContractNumber())
                .vendor(ContractResponse.VendorRef.builder()
                        .id(c.getVendor().getId())
                        .businessName(c.getVendor().getBusinessName())
                        .build())
                .title(c.getTitle())
                .description(c.getDescription())
                .contractType(c.getContractType() != null ? c.getContractType().name() : null)
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .totalValue(c.getTotalValue())
                .paymentTerms(c.getPaymentTerms())
                .autoRenew(c.getAutoRenew())
                .renewalNoticeDays(c.getRenewalNoticeDays())
                .communityId(c.getCommunity() != null ? c.getCommunity().getId() : null)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
