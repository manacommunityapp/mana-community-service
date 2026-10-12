package com.manacommunity.api.vendor.commerce.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.vendor.commerce.dto.VmsVendorCommerceProfileRequest;
import com.manacommunity.api.vendor.commerce.dto.VmsVendorCommerceProfileResponse;
import com.manacommunity.api.vendor.commerce.model.VmsVendorCommerceProfile;
import com.manacommunity.api.vendor.commerce.model.VmsVendorCommerceProfile.CommerceVendorStatus;
import com.manacommunity.api.vendor.commerce.repository.VmsVendorCommerceProfileRepository;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VmsVendorCommerceProfileService {

    private final VmsVendorCommerceProfileRepository profileRepo;
    private final VmsVendorRepository vendorRepo;

    @Transactional(readOnly = true)
    public VmsVendorCommerceProfileResponse getProfile(Long vendorId, Long communityId) {
        VmsVendor vendor = vendorRepo.findByIdAndCommunityId(vendorId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", vendorId));
        VmsVendorCommerceProfile profile = profileRepo.findByVendorId(vendor.getId())
                .orElseGet(() -> buildDefault(vendor));
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public VmsVendorCommerceProfileResponse getMyProfile(Long userId, Long communityId) {
        VmsVendor vendor = vendorRepo.findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor profile not found for user"));
        VmsVendorCommerceProfile profile = profileRepo.findByVendorId(vendor.getId())
                .orElseGet(() -> buildDefault(vendor));
        return toResponse(profile);
    }

    @Transactional
    public VmsVendorCommerceProfileResponse upsertProfile(Long vendorId, Long communityId,
                                                           VmsVendorCommerceProfileRequest req) {
        VmsVendor vendor = vendorRepo.findByIdAndCommunityId(vendorId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", vendorId));

        VmsVendorCommerceProfile profile = profileRepo.findByVendorId(vendor.getId())
                .orElseGet(() -> VmsVendorCommerceProfile.builder().vendor(vendor).build());

        applyRequest(profile, req);
        return toResponse(profileRepo.save(profile));
    }

    @Transactional
    public VmsVendorCommerceProfileResponse submitForVerification(Long vendorId, Long communityId) {
        VmsVendor vendor = vendorRepo.findByIdAndCommunityId(vendorId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", vendorId));
        VmsVendorCommerceProfile profile = profileRepo.findByVendorId(vendor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Commerce profile not found — save a profile first"));

        if (profile.getCommerceStatus() != CommerceVendorStatus.DRAFT &&
            profile.getCommerceStatus() != CommerceVendorStatus.REJECTED) {
            throw new IllegalStateException("Only DRAFT or REJECTED profiles can be submitted for verification");
        }

        profile.setCommerceStatus(CommerceVendorStatus.PENDING_VERIFICATION);
        profile.setSubmittedAt(LocalDateTime.now());
        return toResponse(profileRepo.save(profile));
    }

    @Transactional
    public VmsVendorCommerceProfileResponse verifyProfile(Long vendorId, Long communityId, AppUser admin) {
        VmsVendorCommerceProfile profile = getProfileForVendor(vendorId, communityId);
        profile.setCommerceStatus(CommerceVendorStatus.VERIFIED);
        profile.setVerifiedAt(LocalDateTime.now());
        profile.setVerifiedByUserId(admin.getId());
        profile.setRejectionReason(null);
        profile.setCommunityVisible(true);
        profile.setStorefrontEnabled(true);
        return toResponse(profileRepo.save(profile));
    }

    @Transactional
    public VmsVendorCommerceProfileResponse rejectProfile(Long vendorId, Long communityId,
                                                           String reason, AppUser admin) {
        VmsVendorCommerceProfile profile = getProfileForVendor(vendorId, communityId);
        profile.setCommerceStatus(CommerceVendorStatus.REJECTED);
        profile.setRejectionReason(reason);
        profile.setVerifiedByUserId(admin.getId());
        profile.setCommunityVisible(false);
        return toResponse(profileRepo.save(profile));
    }

    @Transactional
    public VmsVendorCommerceProfileResponse suspendProfile(Long vendorId, Long communityId) {
        VmsVendorCommerceProfile profile = getProfileForVendor(vendorId, communityId);
        profile.setCommerceStatus(CommerceVendorStatus.SUSPENDED);
        profile.setCommunityVisible(false);
        return toResponse(profileRepo.save(profile));
    }

    @Transactional(readOnly = true)
    public Page<VmsVendorCommerceProfileResponse> listByCommunity(Long communityId,
                                                                   String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return profileRepo.findByCommunityIdAndStatus(communityId,
                    CommerceVendorStatus.valueOf(status), pageable).map(this::toResponse);
        }
        return profileRepo.findByCommunityId(communityId, pageable).map(this::toResponse);
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private VmsVendorCommerceProfile getProfileForVendor(Long vendorId, Long communityId) {
        VmsVendor vendor = vendorRepo.findByIdAndCommunityId(vendorId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", vendorId));
        return profileRepo.findByVendorId(vendor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Commerce profile", vendorId));
    }

    private void applyRequest(VmsVendorCommerceProfile p, VmsVendorCommerceProfileRequest req) {
        if (req.getUpiEnabled() != null)              p.setUpiEnabled(req.getUpiEnabled());
        if (req.getUpiId() != null)                   p.setUpiId(req.getUpiId());
        if (req.getDeliveryAvailable() != null)        p.setDeliveryAvailable(req.getDeliveryAvailable());
        if (req.getDeliveryRadiusKm() != null)         p.setDeliveryRadiusKm(req.getDeliveryRadiusKm());
        if (req.getAvgDeliveryTimeMinutes() != null)   p.setAvgDeliveryTimeMinutes(req.getAvgDeliveryTimeMinutes());
        if (req.getMinOrderAmount() != null)           p.setMinOrderAmount(req.getMinOrderAmount());
        if (req.getPickupAvailable() != null)          p.setPickupAvailable(req.getPickupAvailable());
        if (req.getPickupAddress() != null)            p.setPickupAddress(req.getPickupAddress());
        if (req.getStorefrontEnabled() != null)        p.setStorefrontEnabled(req.getStorefrontEnabled());
        if (req.getStorefrontDescription() != null)    p.setStorefrontDescription(req.getStorefrontDescription());
        if (req.getStorefrontBannerUrl() != null)      p.setStorefrontBannerUrl(req.getStorefrontBannerUrl());
        if (req.getCommunityVisible() != null)         p.setCommunityVisible(req.getCommunityVisible());
    }

    private VmsVendorCommerceProfile buildDefault(VmsVendor vendor) {
        return VmsVendorCommerceProfile.builder().vendor(vendor).build();
    }

    private VmsVendorCommerceProfileResponse toResponse(VmsVendorCommerceProfile p) {
        return VmsVendorCommerceProfileResponse.builder()
                .id(p.getId())
                .vendorId(p.getVendor() != null ? p.getVendor().getId() : null)
                .vendorName(p.getVendor() != null ? p.getVendor().getBusinessName() : null)
                .commerceStatus(p.getCommerceStatus() != null ? p.getCommerceStatus().name() : null)
                .upiEnabled(p.getUpiEnabled())
                .upiId(p.getUpiId())
                .deliveryAvailable(p.getDeliveryAvailable())
                .deliveryRadiusKm(p.getDeliveryRadiusKm())
                .avgDeliveryTimeMinutes(p.getAvgDeliveryTimeMinutes())
                .minOrderAmount(p.getMinOrderAmount())
                .pickupAvailable(p.getPickupAvailable())
                .pickupAddress(p.getPickupAddress())
                .storefrontEnabled(p.getStorefrontEnabled())
                .storefrontDescription(p.getStorefrontDescription())
                .storefrontBannerUrl(p.getStorefrontBannerUrl())
                .communityVisible(p.getCommunityVisible())
                .verifiedAt(p.getVerifiedAt())
                .rejectionReason(p.getRejectionReason())
                .submittedAt(p.getSubmittedAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
