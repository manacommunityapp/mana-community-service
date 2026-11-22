package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.kyc.service.KycIdNumberProtector;
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

    /** JSON field names the client must send in place of the retired raw PAN. */
    private static final String EXPECTED_PAN_FIELDS = "'panNumberHash' and 'panNumberLast4'";

    private final MarketSellerRepository sellerRepository;

    /** Same protector resident KYC uses: validates the client digest and peppers it. */
    private final KycIdNumberProtector idNumberProtector;

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

        // Refuse an old client that still transmits the raw PAN. Hashing it here instead
        // would mean the number had already crossed the wire and been logged by every proxy
        // on the way in — the thing the change removes.
        idNumberProtector.rejectRawIdNumber(request.getPanNumber(), "panNumber", EXPECTED_PAN_FIELDS);

        profile.setFssaiLicenseNumber(request.getFssaiLicenseNumber());
        profile.setGstin(request.getGstin());

        // PAN: peppered HMAC of the client digest plus a display fragment. Never the PAN,
        // and never the bare client digest — a PAN has only ~10^9 plausible candidates for
        // a given name pattern, so a dump of plain SHA-256 digests is a dump of the PANs.
        // See KycIdNumberProtector.
        profile.setPanNumberHash(
                idNumberProtector.protectForStorage(request.getPanNumberHash(), "panNumberHash"));
        profile.setPanNumberLast4(
                idNumberProtector.requireLast4(request.getPanNumberLast4(), "panNumberLast4"));

        // Bank account: encrypted at rest by the JPA converter on the entity, so the value
        // assigned here is plaintext by design — a payout needs the real digits. The last 4
        // are split out in the clear so no read path has to decrypt just to mask.
        String account = requireBankAccountNumber(request.getBankAccountNumber());
        profile.setBankAccountNumber(account);
        profile.setBankAccountLast4(last4(account));
        profile.setBankIfscCode(request.getBankIfscCode());
        profile.setBankAccountHolderName(request.getBankAccountHolderName());
        profile.setKycStatus(MarketSellerProfile.KycStatus.SUBMITTED);
        profile.setKycSubmittedAt(LocalDateTime.now());
        profile.setKycRejectionReason(null);

        return toResponse(sellerRepository.save(profile), true);
    }

    /** Normalises and shape-checks the settlement account number. */
    private String requireBankAccountNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidInputException("'bankAccountNumber' is required for settlements.");
        }
        String normalised = raw.replaceAll("[\\s-]", "");
        if (!normalised.matches("^[0-9]{6,20}$")) {
            throw new InvalidInputException("'bankAccountNumber' must be 6-20 digits.");
        }
        return normalised;
    }

    private String last4(String value) {
        if (value == null) return null;
        return value.length() <= 4 ? value : value.substring(value.length() - 4);
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

        return toResponse(sellerRepository.save(profile), true);
    }

    @Transactional(readOnly = true)
    public SellerProfileResponse getMyProfile(AppUser user) {
        MarketSellerProfile profile = sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No seller profile found for current user"));
        return toResponse(profile, true);
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
        // Admin-only review queue: the masked fragments are what makes a decision possible.
        return sellerRepository.findByCommunityIdAndKycStatus(communityId, MarketSellerProfile.KycStatus.SUBMITTED, pageable)
                .map(p -> toResponse(p, true));
    }

    @Transactional
    public SellerProfileResponse toggleStoreOpen(AppUser user, boolean isOpen) {
        MarketSellerProfile profile = sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No seller profile found for current user"));
        profile.setIsOpen(isOpen);
        return toResponse(sellerRepository.save(profile));
    }

    /** Public-facing projection: no KYC identifiers at all. */
    private SellerProfileResponse toResponse(MarketSellerProfile p) {
        return toResponse(p, false);
    }

    /**
     * @param includeKycFragments true only for the seller's own profile and the admin review
     *        paths. The public seller and community-listing endpoints are callable by every
     *        authenticated resident, so GSTIN, the masked PAN and the masked settlement
     *        account must not travel on them. Nothing here can emit a full PAN (only a
     *        peppered digest is stored) or a full account number.
     */
    private SellerProfileResponse toResponse(MarketSellerProfile p, boolean includeKycFragments) {
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
                .gstin(includeKycFragments ? p.getGstin() : null)
                .panNumberLast4(includeKycFragments ? p.getPanNumberLast4() : null)
                .bankAccountLast4(includeKycFragments ? p.getBankAccountLast4() : null)
                .bankIfscCode(includeKycFragments ? p.getBankIfscCode() : null)
                .bankAccountHolderName(includeKycFragments ? p.getBankAccountHolderName() : null)
                .isActive(p.getIsActive())
                .isOpen(p.getIsOpen())
                .rating(p.getRating())
                .totalReviews(p.getTotalReviews())
                .totalOrdersCompleted(p.getTotalOrdersCompleted())
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null)
                .build();
    }
}
