package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.marketplace.dto.MarketDonationRequest;
import com.manacommunity.api.marketplace.dto.MarketDonationResponse;
import com.manacommunity.api.marketplace.entity.MarketDonation;
import com.manacommunity.api.marketplace.repository.MarketDonationRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketDonationService {

    private final MarketDonationRepository donationRepository;

    public Page<MarketDonationResponse> getCommunityDonations(Long communityId, MarketDonation.SharingMode mode, Pageable pageable) {
        if (mode != null) {
            return donationRepository.findByCommunityIdAndSharingModeAndStatus(communityId, mode, MarketDonation.DonationStatus.AVAILABLE, pageable)
                    .map(this::toResponse);
        }
        return donationRepository.findByCommunityIdAndStatus(communityId, MarketDonation.DonationStatus.AVAILABLE, pageable)
                .map(this::toResponse);
    }

    public List<MarketDonationResponse> getMyDonations(Long donorId) {
        return donationRepository.findByDonorIdOrderByCreatedAtDesc(donorId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public MarketDonationResponse getById(Long id) {
        return donationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Donation item not found with id: " + id));
    }

    @Transactional
    public MarketDonationResponse create(MarketDonationRequest req, AppUser donor, Community community) {
        MarketDonation donation = MarketDonation.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .condition(req.getCondition() != null ? req.getCondition() : MarketDonation.ItemCondition.GOOD)
                .imageUrl(req.getImageUrl())
                .sharingMode(req.getSharingMode() != null ? req.getSharingMode() : MarketDonation.SharingMode.GIVEAWAY)
                .barterPreferredItem(req.getBarterPreferredItem())
                .donor(donor)
                .community(community)
                .status(MarketDonation.DonationStatus.AVAILABLE)
                .build();

        return toResponse(donationRepository.save(donation));
    }

    @Transactional
    public MarketDonationResponse claim(Long id, AppUser user) {
        MarketDonation donation = donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + id));

        if (donation.getStatus() != MarketDonation.DonationStatus.AVAILABLE) {
            throw new InvalidInputException("Item is no longer available");
        }

        if (donation.getDonor().getId().equals(user.getId())) {
            throw new InvalidInputException("You cannot claim your own item");
        }

        donation.setStatus(MarketDonation.DonationStatus.CLAIMED);
        donation.setClaimedBy(user);

        return toResponse(donationRepository.save(donation));
    }

    @Transactional
    public MarketDonationResponse markDonated(Long id, Long currentUserId) {
        MarketDonation donation = donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + id));

        if (!donation.getDonor().getId().equals(currentUserId)) {
            throw new UnauthorizedActionException("Only donor can mark the item as completed");
        }

        donation.setStatus(MarketDonation.DonationStatus.DONATED);
        return toResponse(donationRepository.save(donation));
    }

    private MarketDonationResponse toResponse(MarketDonation d) {
        return MarketDonationResponse.builder()
                .id(d.getId())
                .title(d.getTitle())
                .description(d.getDescription())
                .category(d.getCategory())
                .condition(d.getCondition())
                .imageUrl(d.getImageUrl())
                .sharingMode(d.getSharingMode())
                .barterPreferredItem(d.getBarterPreferredItem())
                .status(d.getStatus())
                .donorName(d.getDonor().getFullName() != null ? d.getDonor().getFullName() : d.getDonor().getUsername())
                .donorId(d.getDonor().getId())
                .communityId(d.getCommunity() != null ? d.getCommunity().getId() : null)
                .claimedByName(d.getClaimedBy() != null ? d.getClaimedBy().getFullName() : null)
                .claimedById(d.getClaimedBy() != null ? d.getClaimedBy().getId() : null)
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
