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
                .orElseThrow(() -> new ResourceNotFoundException("Vendor profile not found"));
        return toFullResponse(vendor);
    }

    @Transactional
    public VendorResponse createVendor(VendorRequest req, AppUser user, Community community) {
        VmsVendorCategory category = categoryRepo.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", req.getCategoryId()));

        VmsVendor vendor = VmsVendor.builder()
                .businessName(req.getBusinessName())
                .tradeName(req.getTradeName())
                .category(category)
                .businessType(req.getBusinessType() != null ? VmsVendor.BusinessType.valueOf(req.getBusinessType()) : VmsVendor.BusinessType.INDIVIDUAL)
                .description(req.getDescription())
                .email(req.getEmail())
                .phone(req.getPhone())
                .alternatePhone(req.getAlternatePhone())
                .website(req.getWebsite())
                .address(req.getAddress())
                .city(req.getCity())
                .state(req.getState())
                .pincode(req.getPincode())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .gstNumber(req.getGstNumber())
                .panNumber(req.getPanNumber())
                .logoUrl(req.getLogoUrl())
                .bannerUrl(req.getBannerUrl())
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
                        .aadhaarNumber(o.getAadhaarNumber())
                        .panNumber(o.getPanNumber())
                        .ownershipPercent(o.getOwnershipPercent())
                        .build());
            }
        }

        return toResponse(saved);
    }

    @Transactional
    public VendorResponse updateVendor(Long id, VendorRequest req, Long communityId) {
        VmsVendor vendor = vendorRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", id));

        vendor.setBusinessName(req.getBusinessName());
        vendor.setTradeName(req.getTradeName());
        vendor.setDescription(req.getDescription());
        vendor.setEmail(req.getEmail());
        vendor.setPhone(req.getPhone());
        vendor.setAlternatePhone(req.getAlternatePhone());
        vendor.setWebsite(req.getWebsite());
        vendor.setAddress(req.getAddress());
        vendor.setCity(req.getCity());
        vendor.setState(req.getState());
        vendor.setPincode(req.getPincode());
        vendor.setLatitude(req.getLatitude());
        vendor.setLongitude(req.getLongitude());
        vendor.setGstNumber(req.getGstNumber());
        vendor.setPanNumber(req.getPanNumber());
        vendor.setLogoUrl(req.getLogoUrl());
        vendor.setBannerUrl(req.getBannerUrl());
        if (req.getCommissionRate() != null) vendor.setCommissionRate(req.getCommissionRate());
        if (req.getCategoryId() != null) {
            VmsVendorCategory category = categoryRepo.findById(req.getCategoryId())
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
                .tradeName(v.getTradeName())
                .category(VendorResponse.CategoryRef.builder()
                        .id(v.getCategory().getId())
                        .name(v.getCategory().getName())
                        .icon(v.getCategory().getIcon())
                        .build())
                .businessType(v.getBusinessType() != null ? v.getBusinessType().name() : null)
                .status(v.getStatus().name())
                .description(v.getDescription())
                .email(v.getEmail())
                .phone(v.getPhone())
                .logoUrl(v.getLogoUrl())
                .avgRating(v.getAvgRating())
                .totalRatings(v.getTotalRatings())
                .isVerified(v.getIsVerified())
                .communityId(v.getCommunity() != null ? v.getCommunity().getId() : null)
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }

    private VendorResponse toFullResponse(VmsVendor v) {
        List<VmsVendorOwner> owners = ownerRepo.findByVendorId(v.getId());
        List<VmsVendorBranch> branches = branchRepo.findByVendorIdAndIsActiveTrue(v.getId());

        VendorResponse resp = toResponse(v);
        resp.setAlternatePhone(v.getAlternatePhone());
        resp.setWebsite(v.getWebsite());
        resp.setAddress(v.getAddress());
        resp.setCity(v.getCity());
        resp.setState(v.getState());
        resp.setPincode(v.getPincode());
        resp.setLatitude(v.getLatitude());
        resp.setLongitude(v.getLongitude());
        resp.setGstNumber(v.getGstNumber());
        resp.setPanNumber(v.getPanNumber());
        resp.setBannerUrl(v.getBannerUrl());
        resp.setCommissionRate(v.getCommissionRate());

        resp.setOwners(owners.stream().map(o -> VendorOwnerResponse.builder()
                .id(o.getId())
                .name(o.getName())
                .designation(o.getDesignation())
                .email(o.getEmail())
                .phone(o.getPhone())
                .ownershipPercent(o.getOwnershipPercent())
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
                .isActive(b.getIsActive())
                .build()).toList());

        return resp;
    }
}
