package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.marketplace.dto.MarketWishlistResponse;
import com.manacommunity.api.marketplace.entity.MarketListing;
import com.manacommunity.api.marketplace.entity.MarketListingImage;
import com.manacommunity.api.marketplace.entity.MarketWishlist;
import com.manacommunity.api.marketplace.repository.MarketListingRepository;
import com.manacommunity.api.marketplace.repository.MarketWishlistRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketWishlistService {

    private final MarketWishlistRepository wishlistRepository;
    private final MarketListingRepository listingRepository;

    public List<MarketWishlistResponse> getWishlist(Long userId) {
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public boolean isWishlisted(Long userId, Long listingId) {
        return wishlistRepository.existsByUserIdAndListingId(userId, listingId);
    }

    @Transactional
    public void toggleWishlist(Long listingId, AppUser user) {
        if (wishlistRepository.existsByUserIdAndListingId(user.getId(), listingId)) {
            wishlistRepository.deleteByUserIdAndListingId(user.getId(), listingId);
        } else {
            MarketListing listing = listingRepository.findById(listingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));
            MarketWishlist w = MarketWishlist.builder()
                    .user(user)
                    .listing(listing)
                    .build();
            wishlistRepository.save(w);
        }
    }

    private MarketWishlistResponse toResponse(MarketWishlist w) {
        MarketListing l = w.getListing();
        String img = (l.getImages() != null && !l.getImages().isEmpty()) ? l.getImages().get(0).getUrl() : null;

        return MarketWishlistResponse.builder()
                .id(w.getId())
                .listingId(l.getId())
                .listingTitle(l.getTitle())
                .listingPrice(l.getPrice())
                .listingCategory(l.getCategory())
                .listingStatus(l.getStatus())
                .listingImageUrl(img)
                .sellerName(l.getSeller() != null ? l.getSeller().getFullName() : null)
                .sellerId(l.getSeller() != null ? l.getSeller().getId() : null)
                .addedAt(w.getCreatedAt())
                .build();
    }
}
