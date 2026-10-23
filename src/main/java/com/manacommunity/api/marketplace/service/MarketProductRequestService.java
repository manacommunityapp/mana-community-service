package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.marketplace.dto.MarketProductRequestDto;
import com.manacommunity.api.marketplace.dto.MarketRequestOfferDto;
import com.manacommunity.api.marketplace.entity.MarketListing;
import com.manacommunity.api.marketplace.entity.MarketProductRequest;
import com.manacommunity.api.marketplace.entity.MarketRequestOffer;
import com.manacommunity.api.marketplace.repository.MarketListingRepository;
import com.manacommunity.api.marketplace.repository.MarketProductRequestRepository;
import com.manacommunity.api.marketplace.repository.MarketRequestOfferRepository;
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
public class MarketProductRequestService {

    private final MarketProductRequestRepository requestRepository;
    private final MarketRequestOfferRepository offerRepository;
    private final MarketListingRepository listingRepository;

    public Page<MarketProductRequestDto.Response> getActiveRequests(Long communityId, Pageable pageable) {
        return requestRepository.findByCommunityIdAndStatus(communityId, MarketProductRequest.RequestStatus.OPEN, pageable)
                .map(this::toResponse);
    }

    public List<MarketProductRequestDto.Response> getMyRequests(Long requesterId) {
        return requestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public MarketProductRequestDto.Response getById(Long id) {
        return requestRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product request not found with id: " + id));
    }

    @Transactional
    public MarketProductRequestDto.Response create(MarketProductRequestDto.Request req, AppUser requester, Community community) {
        MarketProductRequest entity = MarketProductRequest.builder()
                .requestTitle(req.getRequestTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .targetBudget(req.getTargetBudget())
                .neededByDate(req.getNeededByDate())
                .urgency(req.getUrgency() != null ? req.getUrgency() : MarketProductRequest.UrgencyLevel.MEDIUM)
                .requester(requester)
                .community(community)
                .status(MarketProductRequest.RequestStatus.OPEN)
                .sellerOffers(new ArrayList<>())
                .build();

        return toResponse(requestRepository.save(entity));
    }

    @Transactional
    public MarketRequestOfferDto.Response makeOffer(Long requestId, MarketRequestOfferDto.Request req, AppUser seller) {
        MarketProductRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));

        MarketListing listing = null;
        if (req.getListingId() != null) {
            listing = listingRepository.findById(req.getListingId()).orElse(null);
        }

        MarketRequestOffer offer = MarketRequestOffer.builder()
                .productRequest(request)
                .seller(seller)
                .listing(listing)
                .offeredPrice(req.getOfferedPrice())
                .message(req.getMessage())
                .status(MarketRequestOffer.OfferStatus.PENDING)
                .build();

        MarketRequestOffer saved = offerRepository.save(offer);

        return MarketRequestOfferDto.Response.builder()
                .id(saved.getId())
                .requestId(request.getId())
                .sellerId(seller.getId())
                .sellerName(seller.getFullName() != null ? seller.getFullName() : seller.getUsername())
                .listingId(listing != null ? listing.getId() : null)
                .offeredPrice(saved.getOfferedPrice())
                .message(saved.getMessage())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private MarketProductRequestDto.Response toResponse(MarketProductRequest r) {
        List<MarketRequestOfferDto.Response> offerDtos = r.getSellerOffers() != null
                ? r.getSellerOffers().stream().map(o -> MarketRequestOfferDto.Response.builder()
                .id(o.getId())
                .requestId(r.getId())
                .sellerId(o.getSeller().getId())
                .sellerName(o.getSeller().getFullName() != null ? o.getSeller().getFullName() : o.getSeller().getUsername())
                .listingId(o.getListing() != null ? o.getListing().getId() : null)
                .offeredPrice(o.getOfferedPrice())
                .message(o.getMessage())
                .status(o.getStatus())
                .createdAt(o.getCreatedAt())
                .build()).collect(Collectors.toList())
                : List.of();

        return MarketProductRequestDto.Response.builder()
                .id(r.getId())
                .requestTitle(r.getRequestTitle())
                .description(r.getDescription())
                .category(r.getCategory())
                .targetBudget(r.getTargetBudget())
                .neededByDate(r.getNeededByDate())
                .urgency(r.getUrgency())
                .status(r.getStatus())
                .requesterName(r.getRequester().getFullName() != null ? r.getRequester().getFullName() : r.getRequester().getUsername())
                .requesterId(r.getRequester().getId())
                .communityId(r.getCommunity() != null ? r.getCommunity().getId() : null)
                .offersCount(r.getSellerOffers() != null ? r.getSellerOffers().size() : 0)
                .offers(offerDtos)
                .createdAt(r.getCreatedAt())
                .build();
    }
}
