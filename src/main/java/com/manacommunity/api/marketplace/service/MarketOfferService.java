package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.marketplace.dto.MarketOfferRequest;
import com.manacommunity.api.marketplace.dto.MarketOfferResponse;
import com.manacommunity.api.marketplace.entity.MarketListing;
import com.manacommunity.api.marketplace.entity.MarketOffer;
import com.manacommunity.api.marketplace.repository.MarketListingRepository;
import com.manacommunity.api.marketplace.repository.MarketOfferRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketOfferService {

    private final MarketOfferRepository offerRepository;
    private final MarketListingRepository listingRepository;

    public List<MarketOfferResponse> getOffersForListing(Long listingId, Long currentUserId) {
        MarketListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));

        if (!listing.getSeller().getId().equals(currentUserId)) {
            throw new UnauthorizedActionException("Only the seller can view all offers for this listing");
        }

        return offerRepository.findByListingIdOrderByCreatedAtDesc(listingId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<MarketOfferResponse> getReceivedOffers(Long sellerId) {
        return offerRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<MarketOfferResponse> getSentOffers(Long buyerId) {
        return offerRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MarketOfferResponse makeOffer(Long listingId, MarketOfferRequest req, AppUser buyer) {
        MarketListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));

        if (listing.getSeller().getId().equals(buyer.getId())) {
            throw new InvalidInputException("You cannot make an offer on your own listing");
        }

        MarketOffer offer = MarketOffer.builder()
                .listing(listing)
                .buyer(buyer)
                .seller(listing.getSeller())
                .originalPrice(listing.getPrice())
                .offeredPrice(req.getOfferedPrice())
                .status(MarketOffer.OfferStatus.PENDING)
                .message(req.getMessage())
                .build();

        return toResponse(offerRepository.save(offer));
    }

    @Transactional
    public MarketOfferResponse respondToOffer(Long offerId, String action, BigDecimal counterPrice, String counterMsg, Long currentUserId) {
        MarketOffer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + offerId));

        if (!offer.getSeller().getId().equals(currentUserId) && !offer.getBuyer().getId().equals(currentUserId)) {
            throw new UnauthorizedActionException("Unauthorized to respond to this offer");
        }

        switch (action.toUpperCase()) {
            case "ACCEPT" -> offer.setStatus(MarketOffer.OfferStatus.ACCEPTED);
            case "DECLINE" -> offer.setStatus(MarketOffer.OfferStatus.DECLINED);
            case "COUNTER" -> {
                if (counterPrice == null || counterPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new InvalidInputException("Valid counter price is required");
                }
                offer.setCounterPrice(counterPrice);
                offer.setCounterMessage(counterMsg);
                offer.setStatus(MarketOffer.OfferStatus.COUNTERED);
            }
            default -> throw new InvalidInputException("Unknown offer action: " + action);
        }

        return toResponse(offerRepository.save(offer));
    }

    private MarketOfferResponse toResponse(MarketOffer o) {
        return MarketOfferResponse.builder()
                .id(o.getId())
                .listingId(o.getListing().getId())
                .listingTitle(o.getListing().getTitle())
                .originalPrice(o.getOriginalPrice())
                .offeredPrice(o.getOfferedPrice())
                .counterPrice(o.getCounterPrice())
                .status(o.getStatus())
                .message(o.getMessage())
                .counterMessage(o.getCounterMessage())
                .buyerName(o.getBuyer().getFullName() != null ? o.getBuyer().getFullName() : o.getBuyer().getUsername())
                .buyerId(o.getBuyer().getId())
                .sellerName(o.getSeller().getFullName() != null ? o.getSeller().getFullName() : o.getSeller().getUsername())
                .sellerId(o.getSeller().getId())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
