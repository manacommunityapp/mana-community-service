package com.manacommunity.api.fulfillment.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.fulfillment.dto.ListingDtos.*;
import com.manacommunity.api.fulfillment.entity.CommunitySellerListing;
import com.manacommunity.api.fulfillment.repository.CommunitySellerListingRepository;
import com.manacommunity.api.marketplace.entity.MarketSellerProfile;
import com.manacommunity.api.marketplace.repository.MarketSellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommunitySellerListingService {

    private final CommunitySellerListingRepository listingRepo;
    private final MarketSellerRepository sellerRepo;

    @Transactional
    public ListingResponse addListing(Long ownerUserId, CreateListingRequest req) {
        MarketSellerProfile seller = sellerRepo.findByUserId(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found. Register as a seller first."));
        CommunitySellerListing listing = CommunitySellerListing.builder()
                .seller(seller)
                .communityId(seller.getCommunity() != null ? seller.getCommunity().getId() : null)
                .title(req.title())
                .description(req.description())
                .category(req.category())
                .price(req.price())
                .priceUnit(req.priceUnit())
                .isAvailable(true)
                .slotBased(req.slotBased() != null && req.slotBased())
                .build();
        return toResponse(listingRepo.save(listing));
    }

    @Transactional
    public ListingResponse toggleAvailability(Long listingId, Long ownerUserId) {
        CommunitySellerListing listing = listingRepo.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        if (!listing.getSeller().getUser().getId().equals(ownerUserId)) {
            throw new IllegalArgumentException("Not the listing owner");
        }
        listing.setIsAvailable(!listing.getIsAvailable());
        return toResponse(listingRepo.save(listing));
    }

    @Transactional(readOnly = true)
    public Page<ListingResponse> getListingsByCommunity(Long communityId, String category, Pageable pageable) {
        Page<CommunitySellerListing> page = (category != null && !category.isBlank())
                ? listingRepo.findByCommunityIdAndCategoryAndIsAvailableTrue(communityId, category, pageable)
                : listingRepo.findByCommunityIdAndIsAvailableTrue(communityId, pageable);
        return page.map(this::toResponse);
    }

    private ListingResponse toResponse(CommunitySellerListing l) {
        return new ListingResponse(l.getId(),
                l.getSeller().getId(),
                l.getSeller().getBusinessName(),
                l.getCommunityId(),
                l.getTitle(), l.getDescription(), l.getCategory(),
                l.getPrice(), l.getPriceUnit(),
                l.getIsAvailable(), l.getSlotBased(),
                l.getCreatedAt());
    }
}
