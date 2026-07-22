package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.DuplicateResourceException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.marketplace.dto.WishlistResponse;
import com.manacommunity.api.marketplace.entity.Listing;
import com.manacommunity.api.marketplace.entity.ListingImage;
import com.manacommunity.api.marketplace.entity.Wishlist;
import com.manacommunity.api.marketplace.repository.ListingRepository;
import com.manacommunity.api.marketplace.repository.WishlistRepository;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepo;
    private final ListingRepository listingRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<WishlistResponse> getMyWishlist(Long userId) {
        return wishlistRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public WishlistResponse addToWishlist(Long userId, Long listingId, AppUser user) {
        Listing listing = listingRepo.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        if (wishlistRepo.existsByUserIdAndListingId(userId, listingId)) {
            throw new DuplicateResourceException("Wishlist item", "listing", String.valueOf(listingId));
        }

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .listing(listing)
                .build();

        Wishlist saved = wishlistRepo.save(wishlist);
        auditService.record(AuditAction.PRODUCT_UPDATED, AuditModule.MARKETPLACE,
                "Wishlist", String.valueOf(saved.getId()));
        return toResponse(saved);
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long listingId) {
        wishlistRepo.deleteByUserIdAndListingId(userId, listingId);
        auditService.record(AuditAction.PRODUCT_UPDATED, AuditModule.MARKETPLACE,
                "Wishlist", userId + ":" + listingId);
    }

    @Transactional(readOnly = true)
    public boolean isWishlisted(Long userId, Long listingId) {
        return wishlistRepo.existsByUserIdAndListingId(userId, listingId);
    }

    @Transactional(readOnly = true)
    public long getWishlistCount(Long listingId) {
        return wishlistRepo.countByListingId(listingId);
    }

    private WishlistResponse toResponse(Wishlist w) {
        Listing l = w.getListing();
        String imageUrl = (l.getImages() != null && !l.getImages().isEmpty())
                ? l.getImages().get(0).getUrl()
                : null;

        return WishlistResponse.builder()
                .id(w.getId())
                .listingId(l.getId())
                .listingTitle(l.getTitle())
                .listingPrice(l.getPrice())
                .listingCategory(l.getCategory())
                .listingStatus(l.getStatus().name())
                .listingImageUrl(imageUrl)
                .sellerName(l.getSeller().getFullName())
                .addedAt(w.getCreatedAt())
                .build();
    }
}
