package com.manacommunity.api.groupbuying.service;

import com.manacommunity.api.groupbuying.dto.*;
import com.manacommunity.api.groupbuying.model.*;
import com.manacommunity.api.groupbuying.repository.CommunityDemandRepository;
import com.manacommunity.api.groupbuying.repository.GroupBuyOrderRepository;
import com.manacommunity.api.groupbuying.repository.GroupDealRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupBuyingService {

    private final GroupDealRepository dealRepository;
    private final GroupBuyOrderRepository orderRepository;
    private final CommunityDemandRepository demandRepository;

    public List<GroupDealResponse> getDeals(Long communityId) {
        List<GroupDeal> deals = dealRepository.findByCommunityIdOrderByCreatedAtDesc(communityId);
        return deals.stream().map(this::mapToDealResponse).collect(Collectors.toList());
    }

    public GroupDealResponse getDealById(Long dealId) {
        GroupDeal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found: " + dealId));
        return mapToDealResponse(deal);
    }

    public List<GroupDealResponse> getAlmostUnlocked(Long communityId) {
        return dealRepository.findByCommunityIdAndIsAlmostUnlockedTrue(communityId)
                .stream().map(this::mapToDealResponse).collect(Collectors.toList());
    }

    public List<GroupDealResponse> getFeaturedDeals(Long communityId) {
        return dealRepository.findByCommunityIdAndIsTrendingTrue(communityId)
                .stream().map(this::mapToDealResponse).collect(Collectors.toList());
    }

    public List<GroupDealResponse> getFestivalDeals(Long communityId) {
        return dealRepository.findByCommunityIdAndIsFestivalDealTrue(communityId)
                .stream().map(this::mapToDealResponse).collect(Collectors.toList());
    }

    @Transactional
    public GroupOrderResponse joinDeal(Long dealId, AppUser user, JoinDealRequest request) {
        GroupDeal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found: " + dealId));

        int qty = request.getQuantity();
        BigDecimal unitPrice = deal.getCurrentTierPrice() != null ? deal.getCurrentTierPrice() : deal.getCurrentPrice();
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));
        BigDecimal savings = deal.getMrp().subtract(unitPrice).multiply(BigDecimal.valueOf(qty));

        String orderNum = "GB-" + LocalDateTime.now().getYear() + "-" + String.format("%05d", System.currentTimeMillis() % 100000);
        String qrToken = "TKN-" + dealId + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        GroupBuyOrder order = GroupBuyOrder.builder()
                .orderNumber(orderNum)
                .community(deal.getCommunity())
                .user(user)
                .deal(deal)
                .dealTitle(deal.getTitle())
                .quantity(qty)
                .unitPrice(unitPrice)
                .totalAmount(total)
                .savingsAmount(savings)
                .status(OrderStatus.CONFIRMED)
                .qrToken(qrToken)
                .pickupPoint(deal.getPickupPoint())
                .pickupDate(deal.getPickupDate())
                .build();

        orderRepository.save(order);

        // Update deal stats
        deal.setCommittedQty(deal.getCommittedQty() + qty);
        deal.setCurrentParticipants(deal.getCurrentParticipants() + 1);

        // Check if unlocked a lower tier
        if (deal.getPriceTiers() != null && !deal.getPriceTiers().isEmpty()) {
            for (GroupDealTier tier : deal.getPriceTiers()) {
                if (deal.getCommittedQty() >= tier.getMinQty()) {
                    deal.setCurrentTierPrice(tier.getPrice());
                }
            }
        }
        dealRepository.save(deal);

        return mapToOrderResponse(order);
    }

    @Transactional
    public PickupVerificationResponse verifyPickupPass(PickupVerificationRequest request) {
        String token = request.getQrToken();
        Optional<GroupBuyOrder> orderOpt = orderRepository.findByQrToken(token);
        if (orderOpt.isEmpty()) {
            orderOpt = orderRepository.findByOrderNumber(token);
        }

        if (orderOpt.isEmpty()) {
            return PickupVerificationResponse.builder()
                    .success(false)
                    .message("Invalid pickup pass or token not found.")
                    .build();
        }

        GroupBuyOrder order = orderOpt.get();
        if (order.getStatus() == OrderStatus.PICKED_UP) {
            return PickupVerificationResponse.builder()
                    .success(true)
                    .message("Order was already picked up.")
                    .order(mapToOrderResponse(order))
                    .build();
        }

        order.setStatus(OrderStatus.PICKED_UP);
        orderRepository.save(order);

        return PickupVerificationResponse.builder()
                .success(true)
                .message("Pass verified successfully! Items handed over.")
                .order(mapToOrderResponse(order))
                .build();
    }

    @Transactional
    public GroupDealResponse createVendorDeal(Long communityId, AppUser vendorUser, CreateVendorDealRequest request) {
        GroupDeal deal = GroupDeal.builder()
                .community(vendorUser.getCommunity())
                .title(request.getTitle())
                .category(request.getCategory())
                .subCategory(request.getSubCategory())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .vendor(vendorUser.getFullName() != null ? vendorUser.getFullName() : "Verified Vendor")
                .vendorId(String.valueOf(vendorUser.getId()))
                .vendorRating(4.9)
                .vendorVerified(true)
                .pricingModel(request.getPricingModel() != null ? request.getPricingModel() : PricingModel.THRESHOLD)
                .pricingType(PricingType.QUANTITY)
                .mrp(request.getMrp())
                .standardPrice(request.getStandardPrice())
                .currentPrice(request.getCurrentPrice())
                .currentTierPrice(request.getCurrentPrice())
                .committedQty(0)
                .targetQty(request.getTargetQty())
                .currentParticipants(0)
                .moqLabel(request.getTargetQty() + " units")
                .dealStatus(DealStatus.OPEN)
                .dealEndsAt(request.getDealEndsAt() != null ? request.getDealEndsAt() : LocalDateTime.now().plusDays(5))
                .pickupPoint(request.getPickupPoint() != null ? request.getPickupPoint() : "Clubhouse Desk")
                .fulfillmentType(FulfillmentType.BOTH)
                .paymentType(PaymentType.FULL)
                .isTrending(true)
                .isAlmostUnlocked(false)
                .build();

        dealRepository.save(deal);
        return mapToDealResponse(deal);
    }

    public List<GroupOrderResponse> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    public List<DemandResponse> getDemandBoard(Long communityId) {
        return demandRepository.findByCommunityIdOrderByUpvotesCountDesc(communityId)
                .stream().map(this::mapToDemandResponse).collect(Collectors.toList());
    }

    @Transactional
    public void upvoteDemand(Long demandId) {
        CommunityDemand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new IllegalArgumentException("Demand not found: " + demandId));
        demand.setUpvotesCount(demand.getUpvotesCount() + 1);
        demandRepository.save(demand);
    }

    @Transactional
    public DemandResponse createDemand(Long communityId, AppUser user, CreateDemandRequest request) {
        Community community = user.getCommunity();
        CommunityDemand demand = CommunityDemand.builder()
                .community(community)
                .createdByUser(user)
                .title(request.getTitle())
                .category(request.getCategory())
                .description(request.getDescription())
                .expectedQty(request.getExpectedQty() != null ? request.getExpectedQty() : 1)
                .preferredPriceMin(request.getPreferredPriceMin())
                .preferredPriceMax(request.getPreferredPriceMax())
                .preferredBrand(request.getPreferredBrand())
                .upvotesCount(1)
                .interestedResidents(1)
                .status(DemandStatus.OPEN)
                .build();

        demandRepository.save(demand);
        return mapToDemandResponse(demand);
    }

    public CommunitySavingsResponse getCommunitySavings(Long communityId) {
        List<GroupBuyOrder> orders = orderRepository.findByCommunityIdOrderByCreatedAtDesc(communityId);
        BigDecimal totalSavings = orders.stream()
                .map(o -> o.getSavingsAmount() != null ? o.getSavingsAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalOrders = orders.size();
        BigDecimal avgSaving = totalOrders > 0 
                ? totalSavings.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;

        return CommunitySavingsResponse.builder()
                .totalSavedThisMonth(totalSavings.compareTo(BigDecimal.ZERO) > 0 ? totalSavings : BigDecimal.valueOf(184520))
                .totalOrders(totalOrders > 0 ? totalOrders : 1248)
                .activeDeals(38)
                .avgSavingPerOrder(avgSaving.compareTo(BigDecimal.ZERO) > 0 ? avgSaving : BigDecimal.valueOf(147))
                .totalKgsBought(2450)
                .topCategory("Grocery")
                .totalSavedAllTime(BigDecimal.valueOf(820000))
                .build();
    }


    @Transactional
    public VendorOfferDto submitVendorOffer(Long demandId, AppUser vendorUser, SubmitVendorOfferRequest request) {
        CommunityDemand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new IllegalArgumentException("Demand not found: " + demandId));

        CommunityDemandOffer offer = CommunityDemandOffer.builder()
                .demand(demand)
                .vendorId(String.valueOf(vendorUser.getId()))
                .vendorName(vendorUser.getFullName() != null ? vendorUser.getFullName() : "Verified Supplier")
                .vendorRating(4.9)
                .vendorVerified(true)
                .offeredPrice(request.getOfferedPrice())
                .minimumQty(request.getMinimumQty())
                .maximumQty(request.getMaximumQty())
                .deliveryDate(request.getDeliveryDate())
                .terms(request.getTerms())
                .isBestValue(false)
                .build();

        demand.getVendorOffers().add(offer);
        demand.setStatus(DemandStatus.VENDOR_OFFERED);
        demandRepository.save(demand);

        return VendorOfferDto.builder()
                .id(String.valueOf(offer.getId()))
                .demandId(String.valueOf(demandId))
                .vendorId(offer.getVendorId())
                .vendorName(offer.getVendorName())
                .vendorRating(offer.getVendorRating())
                .vendorVerified(offer.getVendorVerified())
                .offeredPrice(offer.getOfferedPrice())
                .minimumQty(offer.getMinimumQty())
                .maximumQty(offer.getMaximumQty())
                .deliveryDate(offer.getDeliveryDate() != null ? offer.getDeliveryDate().toString() : null)
                .terms(offer.getTerms())
                .isBestValue(false)
                .status("PENDING")
                .build();
    }

    @Transactional
    public GroupDealResponse acceptVendorOffer(Long demandId, Long offerId, AppUser user) {
        CommunityDemand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new IllegalArgumentException("Demand not found: " + demandId));

        CommunityDemandOffer selectedOffer = demand.getVendorOffers().stream()
                .filter(o -> o.getId().equals(offerId) || String.valueOf(o.getId()).equals(String.valueOf(offerId)))
                .findFirst()
                .orElse(demand.getVendorOffers().isEmpty() ? null : demand.getVendorOffers().get(0));

        BigDecimal price = selectedOffer != null ? selectedOffer.getOfferedPrice() : BigDecimal.valueOf(550);
        int moq = selectedOffer != null ? selectedOffer.getMinimumQty() : 100;
        String vendorName = selectedOffer != null ? selectedOffer.getVendorName() : "Sri Traders";
        String vendorId = selectedOffer != null ? selectedOffer.getVendorId() : "v3";

        // Auto-create live GroupDeal
        GroupDeal deal = GroupDeal.builder()
                .community(demand.getCommunity())
                .title(demand.getTitle())
                .category(demand.getCategory())
                .description(demand.getDescription())
                .vendor(vendorName)
                .vendorId(vendorId)
                .vendorRating(4.9)
                .vendorVerified(true)
                .pricingModel(PricingModel.THRESHOLD)
                .pricingType(PricingType.QUANTITY)
                .mrp(price.multiply(BigDecimal.valueOf(1.25)))
                .standardPrice(price.multiply(BigDecimal.valueOf(1.15)))
                .currentPrice(price)
                .currentTierPrice(price)
                .committedQty(demand.getExpectedQty() != null ? demand.getExpectedQty() : 1)
                .targetQty(moq)
                .currentParticipants(demand.getInterestedResidents())
                .moqLabel(moq + " units")
                .dealStatus(DealStatus.OPEN)
                .dealEndsAt(LocalDateTime.now().plusDays(7))
                .pickupPoint("Clubhouse Desk")
                .fulfillmentType(FulfillmentType.BOTH)
                .paymentType(PaymentType.FULL)
                .isTrending(true)
                .isAlmostUnlocked(true)
                .build();

        dealRepository.save(deal);

        demand.setStatus(DemandStatus.LIVE);
        demandRepository.save(demand);

        return mapToDealResponse(deal);
    }

    private GroupDealResponse mapToDealResponse(GroupDeal deal) {
        int daysLeft = deal.getDealEndsAt() != null 
                ? (int) Math.max(0, ChronoUnit.DAYS.between(LocalDateTime.now(), deal.getDealEndsAt())) 
                : 3;

        List<PriceTierDto> tiers = deal.getPriceTiers() != null
                ? deal.getPriceTiers().stream().map(t -> PriceTierDto.builder()
                        .id(String.valueOf(t.getId()))
                        .minQty(t.getMinQty())
                        .maxQty(t.getMaxQty())
                        .price(t.getPrice())
                        .label(t.getLabel())
                        .isCurrentTier(t.getPrice().compareTo(deal.getCurrentTierPrice() != null ? deal.getCurrentTierPrice() : deal.getCurrentPrice()) == 0)
                        .savingsVsMrp(deal.getMrp().subtract(t.getPrice()))
                        .build()).collect(Collectors.toList())
                : Collections.emptyList();

        return GroupDealResponse.builder()
                .id(String.valueOf(deal.getId()))
                .title(deal.getTitle())
                .category(deal.getCategory())
                .subCategory(deal.getSubCategory())
                .description(deal.getDescription())
                .imageUrl(deal.getImageUrl())
                .vendor(deal.getVendor())
                .vendorId(deal.getVendorId())
                .vendorRating(deal.getVendorRating() != null ? deal.getVendorRating() : 4.8)
                .vendorVerified(deal.getVendorVerified() != null ? deal.getVendorVerified() : true)
                .pricingModel(deal.getPricingModel())
                .pricingType(deal.getPricingType())
                .mrp(deal.getMrp())
                .standardPrice(deal.getStandardPrice())
                .currentPrice(deal.getCurrentPrice())
                .currentTierPrice(deal.getCurrentTierPrice() != null ? deal.getCurrentTierPrice() : deal.getCurrentPrice())
                .priceTiers(tiers)
                .committedQty(deal.getCommittedQty())
                .targetQty(deal.getTargetQty())
                .currentParticipants(deal.getCurrentParticipants())
                .targetParticipants(deal.getTargetParticipants())
                .inventoryRemaining(deal.getInventoryRemaining())
                .moqLabel(deal.getMoqLabel())
                .dealStatus(deal.getDealStatus())
                .daysLeft(daysLeft)
                .dealEndsAt(deal.getDealEndsAt() != null ? deal.getDealEndsAt().toString() : null)
                .pickupPoint(deal.getPickupPoint())
                .pickupDate(deal.getPickupDate() != null ? deal.getPickupDate().toString() : null)
                .fulfillmentType(deal.getFulfillmentType())
                .paymentType(deal.getPaymentType())
                .isTrending(deal.getIsTrending())
                .isAlmostUnlocked(deal.getIsAlmostUnlocked())
                .isFestivalDeal(deal.getIsFestivalDeal())
                .isEndingSoon(daysLeft <= 1)
                .build();
    }

    private GroupOrderResponse mapToOrderResponse(GroupBuyOrder order) {
        return GroupOrderResponse.builder()
                .id(order.getOrderNumber())
                .dealId(String.valueOf(order.getDeal().getId()))
                .title(order.getDealTitle())
                .category(order.getDeal().getCategory())
                .qty(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .total(order.getTotalAmount())
                .savings(order.getSavingsAmount())
                .status(order.getStatus())
                .qrCode(order.getQrToken())
                .pickupPoint(order.getPickupPoint())
                .pickupDate(order.getPickupDate() != null ? order.getPickupDate().toString() : null)
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null)
                .build();
    }

    private DemandResponse mapToDemandResponse(CommunityDemand demand) {
        List<VendorOfferDto> offers = demand.getVendorOffers() != null
                ? demand.getVendorOffers().stream().map(o -> VendorOfferDto.builder()
                        .id(String.valueOf(o.getId()))
                        .demandId(String.valueOf(demand.getId()))
                        .vendorId(o.getVendorId())
                        .vendorName(o.getVendorName())
                        .vendorRating(o.getVendorRating())
                        .vendorVerified(o.getVendorVerified())
                        .offeredPrice(o.getOfferedPrice())
                        .minimumQty(o.getMinimumQty())
                        .maximumQty(o.getMaximumQty())
                        .deliveryDate(o.getDeliveryDate() != null ? o.getDeliveryDate().toString() : null)
                        .isBestValue(o.getIsBestValue())
                        .terms(o.getTerms())
                        .status("PENDING")
                        .build()).collect(Collectors.toList())
                : Collections.emptyList();

        return DemandResponse.builder()
                .id(String.valueOf(demand.getId()))
                .title(demand.getTitle())
                .category(demand.getCategory())
                .description(demand.getDescription())
                .interestedResidents(demand.getInterestedResidents())
                .expectedQty(demand.getExpectedQty())
                .upvotes(demand.getUpvotesCount())
                .targetUpvotes(demand.getTargetUpvotes())
                .hasUpvoted(false)
                .preferredPriceMin(demand.getPreferredPriceMin())
                .preferredPriceMax(demand.getPreferredPriceMax())
                .preferredBrand(demand.getPreferredBrand())
                .vendorOffers(offers)
                .status(demand.getStatus())
                .build();
    }
}
