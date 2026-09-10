package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.marketplace.dto.MarketListingRequest;
import com.manacommunity.api.marketplace.dto.MarketListingResponse;
import com.manacommunity.api.marketplace.entity.MarketListing;
import com.manacommunity.api.marketplace.entity.MarketListingImage;
import com.manacommunity.api.marketplace.repository.MarketListingImageRepository;
import com.manacommunity.api.marketplace.repository.MarketListingRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketListingService {

    private final MarketListingRepository listingRepository;
    private final MarketListingImageRepository imageRepository;

    public Page<MarketListingResponse> getCommunityListings(Long communityId, String category, Pageable pageable) {
        if (category != null && !category.isBlank()) {
            return listingRepository.findByCommunityIdAndCategoryAndStatus(communityId, category, MarketListing.ListingStatus.ACTIVE, pageable)
                    .map(this::toResponse);
        }
        return listingRepository.findByCommunityIdAndStatus(communityId, MarketListing.ListingStatus.ACTIVE, pageable)
                .map(this::toResponse);
    }

    public Page<MarketListingResponse> searchListings(Long communityId, String query, Pageable pageable) {
        return listingRepository.searchActiveListings(communityId, query, pageable).map(this::toResponse);
    }

    public List<MarketListingResponse> getMyListings(Long sellerId) {
        return listingRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public MarketListingResponse getById(Long id) {
        return listingRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + id));
    }

    @Transactional
    public MarketListingResponse create(MarketListingRequest req, AppUser seller, Community community) {
        MarketListing listing = MarketListing.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .price(req.getPrice())
                .priceUnit(req.getPriceUnit() != null ? req.getPriceUnit() : "INR")
                .category(req.getCategory())
                .condition(req.getCondition())
                .warranty(req.getWarranty())
                .transactionMode(req.getTransactionMode() != null ? req.getTransactionMode() : MarketListing.TransactionMode.IN_APP_PAYMENT)
                .visibility(req.getVisibility() != null ? req.getVisibility() : MarketListing.ListingVisibility.COMMUNITY)
                .location(req.getLocation())
                .seller(seller)
                .community(community)
                .status(MarketListing.ListingStatus.ACTIVE)
                .images(new ArrayList<>())
                .build();

        MarketListing saved = listingRepository.save(listing);

        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
            int order = 0;
            for (String url : req.getImageUrls()) {
                MarketListingImage img = MarketListingImage.builder()
                        .listing(saved)
                        .url(url)
                        .sortOrder(order++)
                        .build();
                saved.getImages().add(imageRepository.save(img));
            }
        }

        return toResponse(saved);
    }

    @Transactional
    public MarketListingResponse update(Long id, MarketListingRequest req, Long currentUserId) {
        MarketListing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + id));

        if (!listing.getSeller().getId().equals(currentUserId)) {
            throw new UnauthorizedActionException("You can only edit your own listings");
        }

        listing.setTitle(req.getTitle());
        listing.setDescription(req.getDescription());
        listing.setPrice(req.getPrice());
        if (req.getPriceUnit() != null) listing.setPriceUnit(req.getPriceUnit());
        listing.setCategory(req.getCategory());
        if (req.getCondition() != null) listing.setCondition(req.getCondition());
        listing.setWarranty(req.getWarranty());
        if (req.getTransactionMode() != null) listing.setTransactionMode(req.getTransactionMode());
        if (req.getVisibility() != null) listing.setVisibility(req.getVisibility());
        listing.setLocation(req.getLocation());

        if (req.getImageUrls() != null) {
            imageRepository.deleteByListingId(listing.getId());
            listing.getImages().clear();
            int order = 0;
            for (String url : req.getImageUrls()) {
                MarketListingImage img = MarketListingImage.builder()
                        .listing(listing)
                        .url(url)
                        .sortOrder(order++)
                        .build();
                listing.getImages().add(imageRepository.save(img));
            }
        }

        return toResponse(listingRepository.save(listing));
    }

    @Transactional
    public void updateStatus(Long id, String statusStr, Long currentUserId) {
        MarketListing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + id));

        if (!listing.getSeller().getId().equals(currentUserId)) {
            throw new UnauthorizedActionException("You can only change status of your own listings");
        }

        MarketListing.ListingStatus newStatus = MarketListing.ListingStatus.valueOf(statusStr.toUpperCase());
        listing.setStatus(newStatus);
        listingRepository.save(listing);
    }

    @Transactional
    public void adminDelete(Long id) {
        MarketListing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + id));
        listing.setStatus(MarketListing.ListingStatus.DELETED);
        listingRepository.save(listing);
    }

    public MarketListingResponse toResponse(MarketListing l) {
        List<String> imageUrls = l.getImages() != null
                ? l.getImages().stream().map(MarketListingImage::getUrl).collect(Collectors.toList())
                : List.of();

        MarketListingResponse.SellerSummary sellerSummary = null;
        if (l.getSeller() != null) {
            sellerSummary = MarketListingResponse.SellerSummary.builder()
                    .id(l.getSeller().getId())
                    .fullName(l.getSeller().getFullName() != null ? l.getSeller().getFullName() : l.getSeller().getUsername())
                    .verified(true)
                    .apartmentNumber(l.getSeller().getFlatNo())
                    .tower(l.getSeller().getTower())
                    .build();
        }

        return MarketListingResponse.builder()
                .id(l.getId())
                .title(l.getTitle())
                .description(l.getDescription())
                .price(l.getPrice())
                .priceUnit(l.getPriceUnit())
                .category(l.getCategory())
                .condition(l.getCondition())
                .warranty(l.getWarranty())
                .status(l.getStatus())
                .transactionMode(l.getTransactionMode())
                .visibility(l.getVisibility())
                .location(l.getLocation())
                .imageUrls(imageUrls)
                .seller(sellerSummary)
                .communityId(l.getCommunity() != null ? l.getCommunity().getId() : null)
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }
}
