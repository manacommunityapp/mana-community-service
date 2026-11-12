package com.manacommunity.api.vendor.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.vendor.dto.*;
import com.manacommunity.api.vendor.entity.*;
import com.manacommunity.api.vendor.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VmsVendorManagementService {

    private final VmsVendorRepository vendorRepo;
    private final VmsVendorCategoryRepository categoryRepo;
    private final VmsVendorOwnerRepository ownerRepo;
    private final VmsVendorBranchRepository branchRepo;
    private final VmsVendorPerformanceRepository performanceRepo;

    @Transactional(readOnly = true)
    public Page<VendorResponse> getVendors(Long communityId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return vendorRepo.findByCommunityIdAndStatus(communityId, VmsVendor.VendorStatus.valueOf(status), pageable)
                    .map(this::toResponse);
        }
        return vendorRepo.findByCommunityId(communityId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<VendorResponse> searchVendors(Long communityId, String query, Pageable pageable) {
        return vendorRepo.searchByCommunity(communityId, query, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public VendorResponse getVendorById(Long id, Long communityId) {
        VmsVendor vendor = vendorRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", id));
        return toFullResponse(vendor);
    }

    @Transactional(readOnly = true)
    public VendorResponse getMyVendorProfile(Long userId, Long communityId) {
        VmsVendor vendor = vendorRepo.findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "userId", userId.toString()));
        return toFullResponse(vendor);
    }

    @Transactional
    public VendorResponse createVendor(VendorRequest req, AppUser user, Community community) {
        VmsVendorCategory category = categoryRepo.findByIdAndCommunityId(req.getCategoryId(), community.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", req.getCategoryId()));

        VmsVendor vendor = VmsVendor.builder()
                .businessName(req.getBusinessName())
                .category(category)
                .businessType(req.getBusinessType() != null ? VmsVendor.BusinessType.valueOf(req.getBusinessType()) : VmsVendor.BusinessType.INDIVIDUAL)
                .description(req.getDescription())
                .website(req.getWebsite())
                .gstNumber(req.getGstNumber())
                .pan(req.getPanNumber())
                .logoUrl(req.getLogoUrl())
                .coverImageUrl(req.getBannerUrl())
                .commissionRate(req.getCommissionRate())
                .user(user)
                .community(community)
                .build();

        VmsVendor saved = vendorRepo.save(vendor);

        if (req.getOwners() != null) {
            for (VendorOwnerRequest o : req.getOwners()) {
                ownerRepo.save(VmsVendorOwner.builder()
                        .vendor(saved)
                        .name(o.getName())
                        .designation(o.getDesignation())
                        .email(o.getEmail())
                        .phone(o.getPhone())
                        .aadhaar(o.getAadhaarNumber())
                        .pan(o.getPanNumber())
                        .build());
            }
        }

        return toResponse(saved);
    }

    @Transactional
    public VendorResponse updateMyVendorProfile(VendorRequest req, AppUser user) {
        VmsVendor vendor = vendorRepo.findByUserIdAndCommunityId(user.getId(), user.getCommunity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "userId", user.getId().toString()));
        return updateVendor(vendor.getId(), req, user.getCommunity().getId());
    }

    public VendorResponse updateVendor(Long id, VendorRequest req, Long communityId) {
        VmsVendor vendor = vendorRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", id));

        vendor.setBusinessName(req.getBusinessName());
        vendor.setDescription(req.getDescription());
        vendor.setWebsite(req.getWebsite());
        vendor.setGstNumber(req.getGstNumber());
        vendor.setPan(req.getPanNumber());
        vendor.setLogoUrl(req.getLogoUrl());
        vendor.setCoverImageUrl(req.getBannerUrl());
        if (req.getBusinessType() != null && !req.getBusinessType().isBlank()) {
            vendor.setBusinessType(VmsVendor.BusinessType.valueOf(req.getBusinessType().toUpperCase(java.util.Locale.ROOT)));
        }
        VmsVendorBranch primaryBranch = branchRepo.findByVendorIdAndActiveTrue(vendor.getId()).stream()
                .filter(branch -> Boolean.TRUE.equals(branch.getIsHeadOffice())).findFirst()
                .orElseGet(() -> VmsVendorBranch.builder().vendor(vendor).isHeadOffice(true).active(true)
                        .name(req.getTradeName() != null && !req.getTradeName().isBlank() ? req.getTradeName() : req.getBusinessName()).build());
        if (req.getTradeName() != null && !req.getTradeName().isBlank()) primaryBranch.setName(req.getTradeName());
        else if (primaryBranch.getName() == null || primaryBranch.getName().isBlank()) primaryBranch.setName(req.getBusinessName());
        if (req.getAddress() != null) primaryBranch.setAddress(req.getAddress());
        if (req.getCity() != null) primaryBranch.setCity(req.getCity());
        if (req.getState() != null) primaryBranch.setState(req.getState());
        if (req.getPincode() != null) primaryBranch.setPincode(req.getPincode());
        if (req.getLatitude() != null) primaryBranch.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) primaryBranch.setLongitude(req.getLongitude());
        if (req.getPhone() != null) primaryBranch.setPhone(req.getPhone());
        if (req.getEmail() != null) primaryBranch.setEmail(req.getEmail());
        branchRepo.save(primaryBranch);
        if (req.getCommissionRate() != null) vendor.setCommissionRate(req.getCommissionRate());
        if (req.getCategoryId() != null) {
            VmsVendorCategory category = categoryRepo.findByIdAndCommunityId(req.getCategoryId(), communityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category", req.getCategoryId()));
            vendor.setCategory(category);
        }

        return toResponse(vendorRepo.save(vendor));
    }

    @Transactional
    public void updateVendorStatus(Long id, String status, Long communityId) {
        VmsVendor vendor = vendorRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", id));
        vendor.setStatus(VmsVendor.VendorStatus.valueOf(status));
        vendorRepo.save(vendor);
    }

    private VendorResponse toResponse(VmsVendor v) {
        return VendorResponse.builder()
                .id(v.getId())
                .businessName(v.getBusinessName())
                .category(VendorResponse.CategoryRef.builder()
                        .id(v.getCategory().getId())
                        .name(v.getCategory().getName())
                        .icon(v.getCategory().getIcon())
                        .build())
                .businessType(v.getBusinessType() != null ? v.getBusinessType().name() : null)
                .status(v.getStatus().name())
                .description(v.getDescription())
                .email(v.getUser() != null ? v.getUser().getEmail() : null)
                .phone(v.getUser() != null ? v.getUser().getPhone() : null)
                .logoUrl(v.getLogoUrl())
                .avgRating(v.getAverageRating())
                .totalRatings(v.getTotalRatings())
                .isVerified(v.getVerified())
                .communityId(v.getCommunity() != null ? v.getCommunity().getId() : null)
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }

    private VendorResponse toFullResponse(VmsVendor v) {
        List<VmsVendorOwner> owners = ownerRepo.findByVendorId(v.getId());
        List<VmsVendorBranch> branches = branchRepo.findByVendorIdAndActiveTrue(v.getId());

        VendorResponse resp = toResponse(v);
        resp.setWebsite(v.getWebsite());
        branches.stream().filter(b -> Boolean.TRUE.equals(b.getIsHeadOffice())).findFirst().ifPresent(branch -> {
            resp.setAddress(branch.getAddress());
            resp.setCity(branch.getCity());
            resp.setState(branch.getState());
            resp.setPincode(branch.getPincode());
            resp.setLatitude(branch.getLatitude());
            resp.setLongitude(branch.getLongitude());
            if (branch.getPhone() != null && !branch.getPhone().isBlank()) resp.setPhone(branch.getPhone());
            if (branch.getEmail() != null && !branch.getEmail().isBlank()) resp.setEmail(branch.getEmail());
        });
        resp.setGstNumber(v.getGstNumber());
        resp.setPanNumber(v.getPan());
        resp.setBannerUrl(v.getCoverImageUrl());
        resp.setCommissionRate(v.getCommissionRate());

        resp.setOwners(owners.stream().map(o -> VendorOwnerResponse.builder()
                .id(o.getId())
                .name(o.getName())
                .designation(o.getDesignation())
                .email(o.getEmail())
                .phone(o.getPhone())
                .build()).toList());

        resp.setBranches(branches.stream().map(b -> VendorBranchResponse.builder()
                .id(b.getId())
                .name(b.getName())
                .address(b.getAddress())
                .city(b.getCity())
                .state(b.getState())
                .pincode(b.getPincode())
                .latitude(b.getLatitude())
                .longitude(b.getLongitude())
                .phone(b.getPhone())
                .email(b.getEmail())
                .isActive(b.getActive())
                .build()).toList());

        return resp;
    }
}
