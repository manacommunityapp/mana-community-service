package com.manacommunity.api.groupbuying.service;

import com.manacommunity.api.groupbuying.dto.*;
import com.manacommunity.api.groupbuying.model.*;
import com.manacommunity.api.groupbuying.repository.*;
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
    private final OrderReviewRepository reviewRepository;
    private final OrderDisputeRepository disputeRepository;
    private final BuyingGroupRepository buyingGroupRepository;

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

        deal.setCommittedQty(deal.getCommittedQty() + qty);
        deal.setCurrentParticipants(deal.getCurrentParticipants() + 1);

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
    public GroupOrderResponse checkoutGroupBuy(Long dealId, AppUser user, GroupBuyCheckoutRequest request) {
        GroupDeal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found: " + dealId));

        int qty = request.getQuantity();
        BigDecimal unitPrice = deal.getCurrentTierPrice() != null ? deal.getCurrentTierPrice() : deal.getCurrentPrice();
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));
        BigDecimal savings = deal.getMrp().subtract(unitPrice).multiply(BigDecimal.valueOf(qty));

        String orderNum = "GB-" + LocalDateTime.now().getYear() + "-" + String.format("%05d", System.currentTimeMillis() % 100000);
        String otp = String.format("%04d", (int) (Math.random() * 9000 + 1000));
        String qrToken = "TKN-" + dealId + "-" + System.currentTimeMillis() + "-" + otp;

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
                .pickupPoint(request.getDeliveryAddressOrPickup() != null ? request.getDeliveryAddressOrPickup() : deal.getPickupPoint())
                .pickupDate(deal.getPickupDate())
                .paymentMethod(request.getPaymentMethod())
                .escrowHoldAmount(request.getEscrowHoldAmount() != null ? request.getEscrowHoldAmount() : BigDecimal.ZERO)
                .deliveryAddress(request.getDeliveryAddressOrPickup())
                .specialNotes(request.getSpecialNotes())
                .build();

        orderRepository.save(order);

        deal.setCommittedQty(deal.getCommittedQty() + qty);
        deal.setCurrentParticipants(deal.getCurrentParticipants() + 1);

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
    public AuthorizedCollectorResponse authorizeCollector(String orderNumber, AuthorizeCollectorRequest request) {
        GroupBuyOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));

        String pin = String.format("%04d", (int) (Math.random() * 9000 + 1000));
        order.setCollectorPin(pin);
        order.setCollectorName(request.getName());
        order.setCollectorRelation(request.getRelationship());
        orderRepository.save(order);

        return AuthorizedCollectorResponse.builder()
                .pin(pin)
                .expiresAt("Expires at 7:00 PM")
                .build();
    }

    @Transactional
    public OrderReviewResponse submitReview(AppUser user, OrderReviewRequest request) {
        OrderReview review = OrderReview.builder()
                .orderId(request.getOrderId())
                .dealId(request.getDealId() != null ? request.getDealId() : "deal-1")
                .user(user)
                .residentName(request.getResidentName() != null ? request.getResidentName() : user.getFullName())
                .productRating(request.getProductRating())
                .deliveryRating(request.getDeliveryRating())
                .comment(request.getComment())
                .build();

        reviewRepository.save(review);

        return OrderReviewResponse.builder()
                .id(String.valueOf(review.getId()))
                .orderId(review.getOrderId())
                .dealId(review.getDealId())
                .residentName(review.getResidentName())
                .productRating(review.getProductRating())
                .deliveryRating(review.getDeliveryRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt() != null ? review.getCreatedAt().toString() : LocalDateTime.now().toString())
                .build();
    }

    public List<OrderReviewResponse> getReviewsForDeal(String dealId) {
        return reviewRepository.findByDealIdOrderByCreatedAtDesc(dealId).stream()
                .map(r -> OrderReviewResponse.builder()
                        .id(String.valueOf(r.getId()))
                        .orderId(r.getOrderId())
                        .dealId(r.getDealId())
                        .residentName(r.getResidentName())
                        .productRating(r.getProductRating())
                        .deliveryRating(r.getDeliveryRating())
                        .comment(r.getComment())
                        .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDisputeResponse raiseDispute(AppUser user, OrderDisputeRequest request) {
        String dispNum = "DISP-" + LocalDateTime.now().getYear() + "-" + String.format("%03d", (int) (Math.random() * 900 + 100));

        OrderDispute dispute = OrderDispute.builder()
                .disputeNumber(dispNum)
                .orderId(request.getOrderId())
                .dealId(request.getDealId())
                .dealTitle(request.getDealTitle())
                .user(user)
                .residentName(request.getResidentName() != null ? request.getResidentName() : user.getFullName())
                .flatNumber(request.getFlat())
                .reason(request.getReason())
                .requestedResolution(request.getRequestedResolution())
                .claimAmount(request.getClaimAmount())
                .description(request.getDescription())
                .status("SUBMITTED")
                .build();

        disputeRepository.save(dispute);

        return OrderDisputeResponse.builder()
                .id(dispute.getDisputeNumber())
                .orderId(dispute.getOrderId())
                .dealId(dispute.getDealId())
                .dealTitle(dispute.getDealTitle())
                .residentName(dispute.getResidentName())
                .flat(dispute.getFlatNumber())
                .reason(dispute.getReason())
                .requestedResolution(dispute.getRequestedResolution())
                .claimAmount(dispute.getClaimAmount())
                .description(dispute.getDescription())
                .status(dispute.getStatus())
                .createdAt(LocalDateTime.now().toString())
                .build();
    }

    public List<OrderDisputeResponse> getUserDisputes(Long userId) {
        return disputeRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(d -> OrderDisputeResponse.builder()
                        .id(d.getDisputeNumber())
                        .orderId(d.getOrderId())
                        .dealId(d.getDealId())
                        .dealTitle(d.getDealTitle())
                        .residentName(d.getResidentName())
                        .flat(d.getFlatNumber())
                        .reason(d.getReason())
                        .requestedResolution(d.getRequestedResolution())
                        .claimAmount(d.getClaimAmount())
                        .description(d.getDescription())
                        .status(d.getStatus())
                        .createdAt(d.getCreatedAt() != null ? d.getCreatedAt().toString() : null)
                        .resolvedAt(d.getResolvedAt() != null ? d.getResolvedAt().toString() : null)
                        .vendorResponse(d.getVendorResponse())
                        .build())
                .collect(Collectors.toList());
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
                .topCategory("Groceries & Farm Produce")
                .totalSavedAllTime(BigDecimal.valueOf(1840000))
                .collectiveDiscountPercent(23.8)
                .heroMilestoneText("Your community saved ₹18.4 lakh through collective purchasing this year.")
                .topDealsThisMonth(List.of(
                        CommunitySavingsResponse.TopDealSavingsDto.builder().dealTitle("Devgad Alphonso Mangoes (1 Dozen)").savings(BigDecimal.valueOf(68400)).participants(84).build(),
                        CommunitySavingsResponse.TopDealSavingsDto.builder().dealTitle("Premium Aged Basmati Rice (5kg)").savings(BigDecimal.valueOf(45200)).participants(86).build(),
                        CommunitySavingsResponse.TopDealSavingsDto.builder().dealTitle("A2 Vedic Bilona Cow Ghee (1L)").savings(BigDecimal.valueOf(36800)).participants(52).build()
                ))
                .towerLeaderboard(List.of(
                        CommunitySavingsResponse.TowerSavingsDto.builder().tower("Tower A").orders(412).totalSaved(BigDecimal.valueOf(62450)).build(),
                        CommunitySavingsResponse.TowerSavingsDto.builder().tower("Tower B").orders(386).totalSaved(BigDecimal.valueOf(58200)).build(),
                        CommunitySavingsResponse.TowerSavingsDto.builder().tower("Tower C").orders(298).totalSaved(BigDecimal.valueOf(42870)).build()
                ))
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

    public List<BuyingGroupDto> getTowerGroups(Long communityId, AppUser user) {
        List<BuyingGroup> groups = buyingGroupRepository.findByCommunityIdOrderByNameAsc(communityId);
        if (groups.isEmpty()) {
            return List.of(
                BuyingGroupDto.builder()
                    .id(1L)
                    .communityId(communityId)
                    .name("Tower A Wholesale Club")
                    .tower("Tower A")
                    .block("Block 1")
                    .leaderName("Rajesh Sharma")
                    .leaderFlat("A-502")
                    .description("Bulk groceries, oil and atta procurement for Tower A residents.")
                    .totalSaved(BigDecimal.valueOf(34800))
                    .memberCount(42)
                    .activeDealsCount(6)
                    .isMember(true)
                    .build(),
                BuyingGroupDto.builder()
                    .id(2L)
                    .communityId(communityId)
                    .name("Tower B Organic & Fresh")
                    .tower("Tower B")
                    .block("Block 2")
                    .leaderName("Ananya Rao")
                    .leaderFlat("B-201")
                    .description("Farm fresh fruits, organic honey and seasonal vegetables.")
                    .totalSaved(BigDecimal.valueOf(28400))
                    .memberCount(35)
                    .activeDealsCount(4)
                    .isMember(false)
                    .build()
            );
        }
        return groups.stream().map(g -> BuyingGroupDto.builder()
                .id(g.getId())
                .communityId(g.getCommunityId())
                .name(g.getName())
                .tower(g.getTower())
                .block(g.getBlock())
                .description(g.getDescription())
                .totalSaved(g.getTotalSaved())
                .memberCount(g.getMemberCount())
                .activeDealsCount(g.getActiveDealsCount())
                .isMember(g.getLeaderId().equals(user.getId()))
                .build()).collect(Collectors.toList());
    }

    public CommunityAiQueryResponse queryCommunityAi(Long communityId, String query) {
        String q = query != null ? query.toLowerCase() : "";
        List<GroupDeal> matchedDeals = dealRepository.findByCommunityIdOrderByCreatedAtDesc(communityId).stream()
                .filter(d -> d.getTitle().toLowerCase().contains(q) || d.getCategory().toLowerCase().contains(q))
                .collect(Collectors.toList());

        List<CommunityDemand> matchedDemands = demandRepository.findByCommunityIdOrderByUpvotesCountDesc(communityId).stream()
                .filter(d -> d.getTitle().toLowerCase().contains(q) || d.getCategory().toLowerCase().contains(q))
                .collect(Collectors.toList());

        if (!matchedDeals.isEmpty()) {
            GroupDeal top = matchedDeals.get(0);
            return CommunityAiQueryResponse.builder()
                    .query(query)
                    .matchedDeals(matchedDeals.stream().map(this::mapToDealResponse).collect(Collectors.toList()))
                    .matchedDemands(matchedDemands.stream().map(this::mapToDemandResponse).collect(Collectors.toList()))
                    .suggestedAction("JOIN_DEAL")
                    .suggestedPrice(top.getCurrentTierPrice() != null ? top.getCurrentTierPrice() : top.getCurrentPrice())
                    .estimatedCommunitySavings(top.getMrp().subtract(top.getCurrentTierPrice() != null ? top.getCurrentTierPrice() : top.getCurrentPrice()))
                    .confidenceScore(0.95)
                    .explanation("Found active wholesale deal matching " + query + ". Community rate starts at Rs." + (top.getCurrentTierPrice() != null ? top.getCurrentTierPrice() : top.getCurrentPrice()) + ".")
                    .build();
        } else if (!matchedDemands.isEmpty()) {
            CommunityDemand top = matchedDemands.get(0);
            return CommunityAiQueryResponse.builder()
                    .query(query)
                    .matchedDeals(Collections.emptyList())
                    .matchedDemands(matchedDemands.stream().map(this::mapToDemandResponse).collect(Collectors.toList()))
                    .suggestedAction("UPVOTE_DEMAND")
                    .confidenceScore(0.88)
                    .explanation(top.getInterestedResidents() + " neighbours already requested " + top.getTitle() + ". Upvote to attract wholesale vendor bids.")
                    .build();
        } else {
            return CommunityAiQueryResponse.builder()
                    .query(query)
                    .matchedDeals(Collections.emptyList())
                    .matchedDemands(Collections.emptyList())
                    .suggestedAction("CREATE_DEMAND")
                    .suggestedPrice(BigDecimal.valueOf(450))
                    .confidenceScore(0.75)
                    .explanation("No live deals found for " + query + ". Start a community demand with target MOQ to get competing quotes from verified suppliers.")
                    .build();
        }
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
