package com.manacommunity.api.vendor.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.vendor.dto.VendorRegistrationRequest;
import com.manacommunity.api.vendor.dto.VendorRegistrationResponse;
import com.manacommunity.api.vendor.entity.VmsVendorCategory;
import com.manacommunity.api.vendor.entity.VmsVendorRegistration;
import com.manacommunity.api.vendor.repository.VmsVendorCategoryRepository;
import com.manacommunity.api.vendor.repository.VmsVendorRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VmsRegistrationService {

    private final VmsVendorRegistrationRepository registrationRepo;
    private final VmsVendorCategoryRepository categoryRepo;

    @Transactional(readOnly = true)
    public Page<VendorRegistrationResponse> getRegistrations(Long communityId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return registrationRepo.findByCommunityIdAndStatus(communityId, status, pageable)
                    .map(this::toResponse);
        }
        return registrationRepo.findByCommunityId(communityId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public VendorRegistrationResponse getById(Long id, Long communityId) {
        return toResponse(registrationRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", id)));
    }

    @Transactional
    public VendorRegistrationResponse submit(VendorRegistrationRequest req, AppUser user, Community community) {
        VmsVendorCategory category = categoryRepo.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", req.getCategoryId()));

        VmsVendorRegistration reg = VmsVendorRegistration.builder()
                .businessName(req.getBusinessName())
                .category(category)
                .contactName(req.getContactName())
                .email(req.getContactEmail())
                .phone(req.getContactPhone())
                .notes(req.getDescription())
                .community(community)
                .build();

        return toResponse(registrationRepo.save(reg));
    }

    @Transactional
    public VendorRegistrationResponse approve(Long id, Long communityId, AppUser reviewer) {
        VmsVendorRegistration reg = registrationRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", id));
        reg.setStatus(VmsVendorRegistration.RegistrationStatus.APPROVED);
        return toResponse(registrationRepo.save(reg));
    }

    @Transactional
    public VendorRegistrationResponse reject(Long id, Long communityId, String reason, AppUser reviewer) {
        VmsVendorRegistration reg = registrationRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", id));
        reg.setStatus(VmsVendorRegistration.RegistrationStatus.REJECTED);
        reg.setNotes(reason);
        return toResponse(registrationRepo.save(reg));
    }

    private VendorRegistrationResponse toResponse(VmsVendorRegistration r) {
        return VendorRegistrationResponse.builder()
                .id(r.getId())
                .businessName(r.getBusinessName())
                .categoryName(r.getCategory() != null ? r.getCategory().getName() : null)
                .contactName(r.getContactName())
                .contactEmail(r.getEmail())
                .contactPhone(r.getPhone())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .rejectionReason(r.getNotes())
                .communityId(r.getCommunity() != null ? r.getCommunity().getId() : null)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
