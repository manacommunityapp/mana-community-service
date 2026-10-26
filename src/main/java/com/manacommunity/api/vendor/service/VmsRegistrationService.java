package com.manacommunity.api.vendor.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.RoleRepository;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.entity.VmsVendorBranch;
import com.manacommunity.api.vendor.repository.VmsVendorBranchRepository;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VmsRegistrationService {

    private final VmsVendorRegistrationRepository registrationRepo;
    private final VmsVendorCategoryRepository categoryRepo;
    private final VmsVendorRepository vendorRepo;
    private final VmsVendorBranchRepository branchRepo;
    private final RoleRepository roleRepo;

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

    @Transactional(readOnly = true)
    public List<VendorRegistrationResponse> getMyRegistrations(Long communityId, Long applicantUserId) {
        return registrationRepo.findByCommunityIdAndApplicantUserIdOrderByCreatedAtDesc(communityId, applicantUserId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public VendorRegistrationResponse submit(VendorRegistrationRequest req, AppUser user, Community community) {
        VmsVendorCategory category = categoryRepo.findByIdAndCommunityId(req.getCategoryId(), community.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", req.getCategoryId()));

        if (registrationRepo.existsByCommunityIdAndApplicantUserIdAndStatusIn(community.getId(), user.getId(),
                List.of(VmsVendorRegistration.RegistrationStatus.PENDING,
                        VmsVendorRegistration.RegistrationStatus.APPROVED,
                        VmsVendorRegistration.RegistrationStatus.CONVERTED))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already have an active vendor application in this community");
        }

        VmsVendorRegistration reg = VmsVendorRegistration.builder()
                .businessName(req.getBusinessName())
                .category(category)
                .businessType(normalizeBusinessType(req.getBusinessType()))
                .contactName(req.getContactName())
                .email(req.getContactEmail())
                .phone(req.getContactPhone())
                .description(req.getDescription())
                .address(req.getAddress())
                .city(req.getCity())
                .state(req.getState())
                .pincode(req.getPincode())
                .gstNumber(req.getGstNumber())
                .panNumber(req.getPanNumber())
                .applicantUser(user)
                .notes(req.getDescription())
                .community(community)
                .build();

        return toResponse(registrationRepo.save(reg));
    }

    @Transactional
    public VendorRegistrationResponse approve(Long id, Long communityId, AppUser reviewer) {
        VmsVendorRegistration reg = registrationRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", id));
        if (reg.getStatus() == VmsVendorRegistration.RegistrationStatus.CONVERTED) return toResponse(reg);
        if (reg.getStatus() != VmsVendorRegistration.RegistrationStatus.PENDING
                && reg.getStatus() != VmsVendorRegistration.RegistrationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending applications can be approved");
        }

        VmsVendor vendor = reg.getVendor();
        if (vendor == null && reg.getApplicantUser() != null) {
            vendor = vendorRepo.findByUserIdAndCommunityId(reg.getApplicantUser().getId(), communityId).orElse(null);
        }
        if (vendor == null) {
            vendor = VmsVendor.builder()
                    .businessName(reg.getBusinessName())
                    .businessType(toVendorBusinessType(reg.getBusinessType()))
                    .description(reg.getDescription())
                    .gstNumber(reg.getGstNumber())
                    .pan(reg.getPanNumber())
                    .category(reg.getCategory())
                    .user(reg.getApplicantUser())
                    .community(reg.getCommunity())
                    .status(VmsVendor.VendorStatus.APPROVED)
                    .verified(false)
                    .build();
            vendor = vendorRepo.save(vendor);
            if (reg.getAddress() != null || reg.getCity() != null || reg.getState() != null
                    || reg.getPincode() != null || reg.getPhone() != null || reg.getEmail() != null) {
                branchRepo.save(VmsVendorBranch.builder()
                        .vendor(vendor)
                        .name(reg.getBusinessName())
                        .address(reg.getAddress())
                        .city(reg.getCity())
                        .state(reg.getState())
                        .pincode(reg.getPincode())
                        .phone(reg.getPhone())
                        .email(reg.getEmail())
                        .isHeadOffice(true)
                        .active(true)
                        .build());
            }
        }
        vendor.setStatus(VmsVendor.VendorStatus.APPROVED);
        vendor.setApprovedByUser(reviewer);
        vendor.setApprovedAt(LocalDateTime.now());
        if (reg.getApplicantUser() != null) grantVendorRole(reg.getApplicantUser(), communityId);
        reg.setVendor(vendor);
        reg.setStatus(VmsVendorRegistration.RegistrationStatus.CONVERTED);
        reg.setReviewedBy(reviewer);
        reg.setReviewedAt(LocalDateTime.now());
        reg.setRejectionReason(null);
        return toResponse(registrationRepo.save(reg));
    }

    @Transactional
    public VendorRegistrationResponse reject(Long id, Long communityId, String reason, AppUser reviewer) {
        VmsVendorRegistration reg = registrationRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", id));
        if (reg.getStatus() != VmsVendorRegistration.RegistrationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending applications can be rejected");
        }
        reg.setStatus(VmsVendorRegistration.RegistrationStatus.REJECTED);
        reg.setRejectionReason(reason);
        reg.setReviewedBy(reviewer);
        reg.setReviewedAt(LocalDateTime.now());
        return toResponse(registrationRepo.save(reg));
    }

    private void grantVendorRole(AppUser user, Long communityId) {
        Role role = roleRepo.findByNameIgnoreCaseAndCommunityId("VENDOR", communityId)
                .or(() -> roleRepo.findByNameIgnoreCaseAndCommunityIdIsNull("VENDOR"))
                .orElseThrow(() -> new IllegalStateException("VENDOR role is not configured"));
        if (user.getUserRoles() == null) user.setUserRoles(new java.util.LinkedHashSet<>());
        user.getUserRoles().removeIf(existing -> "VENDOR".equalsIgnoreCase(existing.getName())
                && !java.util.Objects.equals(existing.getId(), role.getId()));
        if (user.getUserRoles().stream().noneMatch(existing -> java.util.Objects.equals(existing.getId(), role.getId()))) {
            user.getUserRoles().add(role);
        }
        user.syncRoleString();
    }

    private String normalizeBusinessType(String businessType) {
        if (businessType == null || businessType.isBlank()) return VmsVendor.BusinessType.INDIVIDUAL.name();
        try {
            return VmsVendor.BusinessType.valueOf(businessType.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported business type");
        }
    }

    private VmsVendor.BusinessType toVendorBusinessType(String businessType) {
        return VmsVendor.BusinessType.valueOf(normalizeBusinessType(businessType));
    }

    private VendorRegistrationResponse toResponse(VmsVendorRegistration r) {
        return VendorRegistrationResponse.builder()
                .id(r.getId())
                .businessName(r.getBusinessName())
                .categoryId(r.getCategory() != null ? r.getCategory().getId() : null)
                .categoryName(r.getCategory() != null ? r.getCategory().getName() : null)
                .businessType(r.getBusinessType())
                .contactName(r.getContactName())
                .contactEmail(r.getEmail())
                .contactPhone(r.getPhone())
                .description(r.getDescription())
                .address(r.getAddress())
                .city(r.getCity())
                .state(r.getState())
                .pincode(r.getPincode())
                .gstNumber(r.getGstNumber())
                .panNumber(r.getPanNumber())
                .applicantUserId(r.getApplicantUser() != null ? r.getApplicantUser().getId() : null)
                .applicantUserName(r.getApplicantUser() != null ? r.getApplicantUser().getFullName() : null)
                .applicantUserEmail(r.getApplicantUser() != null ? r.getApplicantUser().getEmail() : null)
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .rejectionReason(r.getRejectionReason())
                .vendorId(r.getVendor() != null ? r.getVendor().getId() : null)
                .communityId(r.getCommunity() != null ? r.getCommunity().getId() : null)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .reviewedAt(r.getReviewedAt())
                .reviewedById(r.getReviewedBy() != null ? r.getReviewedBy().getId() : null)
                .reviewedByName(r.getReviewedBy() != null ? r.getReviewedBy().getFullName() : null)
                .build();
    }
}
