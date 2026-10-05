package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.marketplace.dto.MarketSellerDtos.*;
import com.manacommunity.api.marketplace.entity.MarketSellerProfile;
import com.manacommunity.api.marketplace.repository.MarketSellerRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketSellerService {

    private final MarketSellerRepository sellerRepository;

    @Transactional
    public SellerProfileResponse registerSeller(AppUser user, SellerRegisterRequest request) {
        return sellerRepository.findByUserId(user.getId())
                .map(existing -> {
                    existing.setBusinessName(request.getBusinessName());
                    existing.setSellerType(request.getSellerType());
                    existing.setStoreDescription(request.getStoreDescription());
                    existing.setContactPhone(request.getContactPhone() != null ? request.getContactPhone() : user.getPhone());
                    existing.setContactEmail(request.getContactEmail() != null ? request.getContactEmail() : user.getEmail());
                    existing.setFlatNumber(request.getFlatNumber());
                    return toResponse(sellerRepository.save(existing));
                })
                .orElseGet(() -> {
                    MarketSellerProfile profile = MarketSellerProfile.builder()
                            .user(user)
                            .businessName(request.getBusinessName())
                            .sellerType(request.getSellerType())
                            .storeDescription(request.getStoreDescription())
                            .contactPhone(request.getContactPhone() != null ? request.getContactPhone() : user.getPhone())
                            .contactEmail(request.getContactEmail() != null ? request.getContactEmail() : user.getEmail())
                            .flatNumber(request.getFlatNumber())
                            .community(user.getCommunity())
                            .kycStatus(MarketSellerProfile.KycStatus.UNVERIFIED)
                            .isActive(true)
                            .isOpen(true)
                            .rating(BigDecimal.valueOf(5.00))
                            .totalReviews(0)
                            .totalOrdersCompleted(0)
                            .build();
                    return toResponse(sellerRepository.save(profile));
                });
    }

    @Transactional
    public SellerProfileResponse submitKyc(AppUser user, SellerKycSubmitRequest request) {
        MarketSellerProfile profile = sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found for user: " + user.getId()));

        profile.setFssaiLicenseNumber(request.getFssaiLicenseNumber());
        profile.setGstin(request.getGstin());
        profile.setPanNumber(request.getPanNumber());
        profile.setBankAccountNumber(request.getBankAccountNumber());
        profile.setBankIfscCode(request.getBankIfscCode());
        profile.setBankAccountHolderName(request.getBankAccountHolderName());
        profile.setKycStatus(MarketSellerProfile.KycStatus.SUBMITTED);
        profile.setKycSubmittedAt(LocalDateTime.now());
        profile.setKycRejectionReason(null);

        return toResponse(sellerRepository.save(profile));
    }

    @Transactional
    public SellerProfileResponse processKycDecision(Long sellerId, KycDecisionRequest request) {
        MarketSellerProfile profile = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found: " + sellerId));

        profile.setKycStatus(request.getDecision());
        if (request.getDecision() == MarketSellerProfile.KycStatus.VERIFIED) {
            profile.setKycVerifiedAt(LocalDateTime.now());
            profile.setKycRejectionReason(null);
        } else if (request.getDecision() == MarketSellerProfile.KycStatus.REJECTED) {
            profile.setKycRejectionReason(request.getRejectionReason());
        }

        return toResponse(sellerRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public SellerProfileResponse getMyProfile(AppUser user) {
        MarketSellerProfile profile = sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No seller profile found for current user"));
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public SellerProfileResponse getSellerById(Long sellerId) {
        MarketSellerProfile profile = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found: " + sellerId));
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public Page<SellerProfileResponse> getCommunitySellers(Long communityId, Pageable pageable) {
        return sellerRepository.findByCommunityIdAndIsActiveTrue(communityId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SellerProfileResponse> getPendingKycSellers(Long communityId, Pageable pageable) {
        return sellerRepository.findByCommunityIdAndKycStatus(communityId, MarketSellerProfile.KycStatus.SUBMITTED, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public SellerProfileResponse toggleStoreOpen(AppUser user, boolean isOpen) {
        MarketSellerProfile profile = sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No seller profile found for current user"));
        profile.setIsOpen(isOpen);
        return toResponse(sellerRepository.save(profile));
    }

    private SellerProfileResponse toResponse(MarketSellerProfile p) {
        return SellerProfileResponse.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .sellerName(p.getUser().getFullName() != null ? p.getUser().getFullName() : p.getBusinessName())
                .businessName(p.getBusinessName())
                .sellerType(p.getSellerType())
                .storeDescription(p.getStoreDescription())
                .contactPhone(p.getContactPhone())
                .contactEmail(p.getContactEmail())
                .flatNumber(p.getFlatNumber())
                .kycStatus(p.getKycStatus())
                .kycRejectionReason(p.getKycRejectionReason())
                .isActive(p.getIsActive())
                .isOpen(p.getIsOpen())
                .rating(p.getRating())
                .totalReviews(p.getTotalReviews())
                .totalOrdersCompleted(p.getTotalOrdersCompleted())
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null)
                .build();
    }
}
