package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.marketplace.dto.DonationRequest;
import com.manacommunity.api.marketplace.dto.DonationResponse;
import com.manacommunity.api.marketplace.entity.Donation;
import com.manacommunity.api.marketplace.repository.DonationRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DonationService {

    private final DonationRepository donationRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<DonationResponse> getCommunityDonations(Long communityId, Pageable pageable) {
        return donationRepo.findByCommunityIdAndStatusOrderByCreatedAtDesc(
                communityId, Donation.DonationStatus.AVAILABLE, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<DonationResponse> getMyDonations(Long donorId) {
        return donationRepo.findByDonorIdOrderByCreatedAtDesc(donorId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public DonationResponse create(DonationRequest req, AppUser donor, Community community) {
        Donation donation = Donation.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .condition(parseEnum(Donation.ItemCondition.class, req.getCondition()))
                .imageUrl(req.getImageUrl())
                .donor(donor)
                .community(community)
                .build();

        Donation saved = donationRepo.save(donation);
        auditService.record(AuditAction.DONATION_CREATED, AuditModule.MARKETPLACE,
                "Donation", String.valueOf(saved.getId()));
        return toResponse(saved);
    }

    @Transactional
    public DonationResponse claimDonation(Long id, AppUser claimer) {
        Donation donation = donationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donation", id));
        if (donation.getStatus() != Donation.DonationStatus.AVAILABLE) {
            throw new InvalidInputException("Donation is no longer available");
        }
        if (claimer.getId().equals(donation.getDonor().getId())) {
            throw new InvalidInputException("You cannot claim your own donation");
        }

        donation.setClaimedBy(claimer);
        donation.setStatus(Donation.DonationStatus.CLAIMED);
        Donation saved = donationRepo.save(donation);
        auditService.record(AuditAction.DONATION_CLAIMED, AuditModule.MARKETPLACE,
                "Donation", String.valueOf(saved.getId()));
        return toResponse(saved);
    }

    @Transactional
    public void deleteDonation(Long id, Long donorId) {
        Donation donation = donationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donation", id));
        if (!donation.getDonor().getId().equals(donorId)) {
            throw new UnauthorizedActionException("You can only delete your own donations");
        }

        donation.setStatus(Donation.DonationStatus.DONATED);
        donationRepo.save(donation);
        auditService.record(AuditAction.DONATION_DELETED, AuditModule.MARKETPLACE,
                "Donation", String.valueOf(id));
    }

    private DonationResponse toResponse(Donation d) {
        AppUser donor = d.getDonor();
        return DonationResponse.builder()
                .id(d.getId())
                .title(d.getTitle())
                .description(d.getDescription())
                .category(d.getCategory())
                .condition(d.getCondition() != null ? d.getCondition().name() : null)
                .status(d.getStatus().name())
                .imageUrl(d.getImageUrl())
                .donor(DonationResponse.DonorRef.builder()
                        .id(donor.getId())
                        .fullName(donor.getFullName())
                        .build())
                .communityId(d.getCommunity() != null ? d.getCommunity().getId() : null)
                .claimedByName(d.getClaimedBy() != null ? d.getClaimedBy().getFullName() : null)
                .createdAt(d.getCreatedAt())
                .build();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
