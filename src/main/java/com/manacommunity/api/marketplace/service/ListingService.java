package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.marketplace.dto.ListingRequest;
import com.manacommunity.api.marketplace.dto.ListingResponse;
import com.manacommunity.api.marketplace.entity.Listing;
import com.manacommunity.api.marketplace.entity.ListingImage;
import com.manacommunity.api.marketplace.repository.ListingRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepo;

    @Transactional(readOnly = true)
    public List<ListingResponse> getCommunityListings(Long communityId, String category) {
        List<Listing> listings = (category == null || category.isBlank() || "All".equalsIgnoreCase(category))
                ? listingRepo.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, Listing.ListingStatus.ACTIVE)
                : listingRepo.findByCommunityIdAndCategoryAndStatusOrderByCreatedAtDesc(communityId, category, Listing.ListingStatus.ACTIVE);
        return listings.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> searchListings(Long communityId, String query) {
        return listingRepo.searchByCommunity(communityId, query, Listing.ListingStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> getMyListings(Long sellerId) {
        return listingRepo.findBySellerIdAndStatusNotOrderByCreatedAtDesc(sellerId, Listing.ListingStatus.DELETED)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ListingResponse getById(Long id) {
        return toResponse(listingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + id)));
    }

    @Transactional
    public ListingResponse create(ListingRequest req, AppUser seller, Community community) {
        Listing listing = Listing.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .price(req.getPrice())
                .priceUnit(req.getPriceUnit())
                .category(req.getCategory())
                .location(req.getLocation())
                .transactionMode(parseEnum(Listing.TransactionMode.class, req.getTransactionMode()))
                .visibility(parseEnum(Listing.ListingVisibility.class, req.getVisibility()))
                .seller(seller)
                .community(community)
                .images(new ArrayList<>())
                .build();

        if (req.getImageUrls() != null) {
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                listing.getImages().add(ListingImage.builder()
                        .listing(listing)
                        .url(req.getImageUrls().get(i))
                        .sortOrder(i)
                        .build());
            }
        }

        return toResponse(listingRepo.save(listing));
    }

    @Transactional
    public ListingResponse update(Long id, ListingRequest req, Long sellerId) {
        Listing listing = listingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + id));
        if (!listing.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("You can only edit your own listings");
        }

        listing.setTitle(req.getTitle());
        listing.setDescription(req.getDescription());
        listing.setPrice(req.getPrice());
        listing.setPriceUnit(req.getPriceUnit());
        listing.setCategory(req.getCategory());
        listing.setLocation(req.getLocation());
        if (req.getTransactionMode() != null) {
            listing.setTransactionMode(parseEnum(Listing.TransactionMode.class, req.getTransactionMode()));
        }
        if (req.getVisibility() != null) {
            listing.setVisibility(parseEnum(Listing.ListingVisibility.class, req.getVisibility()));
        }

        if (req.getImageUrls() != null) {
            listing.getImages().clear();
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                listing.getImages().add(ListingImage.builder()
                        .listing(listing)
                        .url(req.getImageUrls().get(i))
                        .sortOrder(i)
                        .build());
            }
        }

        return toResponse(listingRepo.save(listing));
    }

    @Transactional
    public void updateStatus(Long id, String status, Long sellerId) {
        Listing listing = listingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + id));
        if (!listing.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("You can only modify your own listings");
        }
        listing.setStatus(Listing.ListingStatus.valueOf(status));
        listingRepo.save(listing);
    }

    @Transactional
    public void adminDelete(Long id) {
        Listing listing = listingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + id));
        listing.setStatus(Listing.ListingStatus.DELETED);
        listingRepo.save(listing);
    }

    private ListingResponse toResponse(Listing l) {
        AppUser s = l.getSeller();
        return ListingResponse.builder()
                .id(l.getId())
                .title(l.getTitle())
                .description(l.getDescription())
                .price(l.getPrice())
                .priceUnit(l.getPriceUnit())
                .category(l.getCategory())
                .status(l.getStatus().name())
                .transactionMode(l.getTransactionMode() != null ? l.getTransactionMode().name() : null)
                .visibility(l.getVisibility() != null ? l.getVisibility().name() : null)
                .location(l.getLocation())
                .imageUrls(l.getImages() != null
                        ? l.getImages().stream().map(ListingImage::getUrl).toList()
                        : List.of())
                .seller(ListingResponse.SellerRef.builder()
                        .id(s.getId())
                        .fullName(s.getFullName())
                        .verified("APPROVED".equalsIgnoreCase(s.getKycStatus()))
                        .build())
                .communityId(l.getCommunity() != null ? l.getCommunity().getId() : null)
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
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
