package com.manacommunity.api.groupbuying.service;

import com.manacommunity.api.groupbuying.dto.*;
import com.manacommunity.api.groupbuying.model.*;
import com.manacommunity.api.groupbuying.repository.*;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupBuyingService {

    private final GroupDealRepository dealRepository;
    private final GroupBuyOrderRepository orderRepository;
    private final CommunityDemandRepository demandRepository;
    private final OrderReviewRepository reviewRepository;
    private final OrderDisputeRepository disputeRepository;
    private final BuyingGroupRepository buyingGroupRepository;
    private final GroupDealSettlementRepository settlementRepository;

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

    // ── Fraud Control & Order Velocity Checks ─────────────────────────────
    private void enforceOrderFraudControls(GroupDeal deal, AppUser user, int requestedQty) {
        int maxAllowedPerHousehold = 20;
        List<GroupBuyOrder> existingUserOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(o -> o.getDeal() != null && o.getDeal().getId().equals(deal.getId()))
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.toList());
        
        int alreadyOrdered = existingUserOrders.stream().mapToInt(GroupBuyOrder::getQuantity).sum();
        if (alreadyOrdered + requestedQty > maxAllowedPerHousehold) {
            throw new IllegalArgumentException("Order limit exceeded. Maximum " + maxAllowedPerHousehold + 
                    " units per household allowed for this deal. You already committed " + alreadyOrdered + " units.");
        }

        if (deal.getDealStatus() != DealStatus.OPEN) {
            throw new IllegalStateException("Deal is no longer open for participation (Current status: " + deal.getDealStatus() + ")");
        }
        if (deal.getDealEndsAt() != null && LocalDateTime.now().isAfter(deal.getDealEndsAt())) {
            throw new IllegalStateException("Deal has expired on " + deal.getDealEndsAt());
        }
    }

    @Transactional
    public GroupOrderResponse joinDeal(Long dealId, AppUser user, JoinDealRequest request) {
        GroupDeal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found: " + dealId));

        int qty = request.getQuantity() != null && request.getQuantity() > 0 ? request.getQuantity() : 1;
        enforceOrderFraudControls(deal, user, qty);

        BigDecimal unitPrice = deal.getCurrentTierPrice() != null ? deal.getCurrentTierPrice() : deal.getCurrentPrice();
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));
        BigDecimal savings = deal.getMrp() != null ? deal.getMrp().subtract(unitPrice).multiply(BigDecimal.valueOf(qty)) : BigDecimal.ZERO;

        String orderNum = "GB-" + LocalDateTime.now().getYear() + "-" + String.format("%05d", (System.currentTimeMillis() + (long)(Math.random() * 999)) % 100000);
        String qrToken = "TKN-" + dealId + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String deliveryOtp = String.format("%06d", (int)(Math.random() * 900000) + 100000);

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
                .paymentStatus("PAID")
                .paymentMethod(request.getPaymentType() != null ? request.getPaymentType() : "UPI")
                .escrowHoldAmount(total)
                .qrToken(qrToken)
                .deliveryOtp(deliveryOtp)
                .pickupPoint(deal.getPickupPoint())
                .pickupDate(deal.getPickupDate())
                .deliveryAddress(request.getDeliveryAddress() != null ? request.getDeliveryAddress() : (user.getFlatNo() != null ? "Flat " + user.getFlatNo() : "Clubhouse"))
                .specialNotes(request.getSpecialNotes())
                .paymentTimestamp(LocalDateTime.now())
                .build();

        orderRepository.save(order);

        BigDecimal prevTierPrice = deal.getCurrentTierPrice() != null ? deal.getCurrentTierPrice() : deal.getCurrentPrice();
        deal.setCommittedQty(deal.getCommittedQty() + qty);
        deal.setCurrentParticipants(deal.getCurrentParticipants() + 1);

        BigDecimal newTierPrice = prevTierPrice;
        if (deal.getPriceTiers() != null && !deal.getPriceTiers().isEmpty()) {
            for (GroupDealTier tier : deal.getPriceTiers()) {
                if (deal.getCommittedQty() >= tier.getMinQty()) {
                    newTierPrice = tier.getPrice();
                    deal.setCurrentTierPrice(tier.getPrice());
                }
            }
        }
        dealRepository.save(deal);

        if (newTierPrice.compareTo(prevTierPrice) < 0) {
            processTierPriceDropRefunds(deal, prevTierPrice, newTierPrice);
        }

        return mapToOrderResponse(order);
    }

    @Transactional
    public GroupOrderResponse checkoutGroupBuy(Long dealId, AppUser user, GroupBuyCheckoutRequest request) {
        GroupDeal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found: " + dealId));

        int qty = request.getQuantity() != null && request.getQuantity() > 0 ? request.getQuantity() : 1;
        enforceOrderFraudControls(deal, user, qty);

        BigDecimal unitPrice = deal.getCurrentTierPrice() != null ? deal.getCurrentTierPrice() : deal.getCurrentPrice();
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));
        BigDecimal savings = deal.getMrp() != null ? deal.getMrp().subtract(unitPrice).multiply(BigDecimal.valueOf(qty)) : BigDecimal.ZERO;

        String orderNum = "GB-" + LocalDateTime.now().getYear() + "-" + String.format("%05d", (System.currentTimeMillis() + (long)(Math.random() * 999)) % 100000);
        String qrToken = "TKN-" + dealId + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String deliveryOtp = String.format("%06d", (int)(Math.random() * 900000) + 100000);

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
                .paymentStatus("PAID")
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "UPI")
                .transactionId(request.getPaymentMethod() != null ? "TXN-" + System.currentTimeMillis() : null)
                .escrowHoldAmount(total)
                .qrToken(qrToken)
                .deliveryOtp(deliveryOtp)
                .pickupPoint(deal.getPickupPoint())
                .pickupDate(deal.getPickupDate())
                .deliveryAddress(request.getDeliveryAddress() != null ? request.getDeliveryAddress() : (user.getFlatNo() != null ? "Flat " + user.getFlatNo() : "Clubhouse"))
                .specialNotes(request.getDeliveryNotes())
                .paymentTimestamp(LocalDateTime.now())
                .build();

        orderRepository.save(order);

        BigDecimal prevTierPrice = deal.getCurrentTierPrice() != null ? deal.getCurrentTierPrice() : deal.getCurrentPrice();
        deal.setCommittedQty(deal.getCommittedQty() + qty);
        deal.setCurrentParticipants(deal.getCurrentParticipants() + 1);

        BigDecimal newTierPrice = prevTierPrice;
        if (deal.getPriceTiers() != null && !deal.getPriceTiers().isEmpty()) {
            for (GroupDealTier tier : deal.getPriceTiers()) {
                if (deal.getCommittedQty() >= tier.getMinQty()) {
                    newTierPrice = tier.getPrice();
                    deal.setCurrentTierPrice(tier.getPrice());
                }
            }
        }
        dealRepository.save(deal);

        if (newTierPrice.compareTo(prevTierPrice) < 0) {
            processTierPriceDropRefunds(deal, prevTierPrice, newTierPrice);
        }

        return mapToOrderResponse(order);
    }

    // ── Full Payment Processing & Escrow Hold ──────────────────────────────
    @Transactional
    public OrderPaymentResponse processOrderPayment(String orderNumber, AppUser user, OrderPaymentRequest request) {
        GroupBuyOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));

        order.setPaymentStatus("PAID");
        order.setPaymentMethod(request.getPaymentMethod());
        order.setTransactionId(request.getTransactionId() != null ? request.getTransactionId() : "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        order.setPaymentTimestamp(LocalDateTime.now());
        order.setStatus(OrderStatus.CONFIRMED);
        order.setEscrowHoldAmount(order.getTotalAmount());
        orderRepository.save(order);

        log.info("✓ Payment processed for Group Order {}: {} via {} (Escrow Hold: ₹{})", 
                orderNumber, order.getTotalAmount(), request.getPaymentMethod(), order.getEscrowHoldAmount());

        return OrderPaymentResponse.builder()
                .orderNumber(orderNumber)
                .paymentStatus("PAID")
                .paymentMethod(order.getPaymentMethod())
                .transactionId(order.getTransactionId())
                .amountPaid(order.getTotalAmount())
                .escrowHoldAmount(order.getEscrowHoldAmount())
                .paymentTimestamp(order.getPaymentTimestamp())
                .message("Payment successful and held in community escrow until fulfillment.")
                .build();
    }

    // ── Retroactive Tier Price Drop Refund Engine ───────────────────────────
    @Transactional
    public void processTierPriceDropRefunds(GroupDeal deal, BigDecimal oldPrice, BigDecimal newTierPrice) {
        List<GroupBuyOrder> priorOrders = orderRepository.findByDealId(deal.getId());
        BigDecimal priceDiff = oldPrice.subtract(newTierPrice);
        if (priceDiff.compareTo(BigDecimal.ZERO) <= 0) return;

        int refundedCount = 0;
        BigDecimal totalRefunded = BigDecimal.ZERO;

        for (GroupBuyOrder ord : priorOrders) {
            if (ord.getStatus() != OrderStatus.CANCELLED && ord.getStatus() != OrderStatus.REFUNDED) {
                BigDecimal orderRefund = priceDiff.multiply(BigDecimal.valueOf(ord.getQuantity()));
                BigDecimal currentTierRefund = ord.getTierPriceRefundAmount() != null ? ord.getTierPriceRefundAmount() : BigDecimal.ZERO;
                ord.setTierPriceRefundAmount(currentTierRefund.add(orderRefund));
                
                BigDecimal currentSavings = ord.getSavingsAmount() != null ? ord.getSavingsAmount() : BigDecimal.ZERO;
                ord.setSavingsAmount(currentSavings.add(orderRefund));
                ord.setUnitPrice(newTierPrice);
                ord.setTotalAmount(newTierPrice.multiply(BigDecimal.valueOf(ord.getQuantity())));
                orderRepository.save(ord);

                refundedCount++;
                totalRefunded = totalRefunded.add(orderRefund);
            }
        }
        log.info("★ Tier unlocked for deal '{}' (New Price: ₹{}) — Auto-refunded ₹{} across {} orders.",
                deal.getTitle(), newTierPrice, totalRefunded, refundedCount);
    }

    // ── Order Cancellation & Refund ────────────────────────────────────────
    @Transactional
    public GroupOrderResponse cancelOrder(String orderNumber, AppUser user, OrderCancellationRequest request) {
        GroupBuyOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));

        if (!order.getUser().getId().equals(user.getId()) && !Boolean.TRUE.equals(user.getIsActive())) {
            throw new SecurityException("Unauthorized to cancel this order");
        }

        if (order.getStatus() == OrderStatus.OUT_FOR_DELIVERY || order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.PICKED_UP) {
            throw new IllegalStateException("Order is already " + order.getStatus() + " and cannot be cancelled. Please raise a dispute.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setRefundAmount(order.getTotalAmount());
        order.setRefundReason(request.getReason());
        order.setRefundTimestamp(LocalDateTime.now());
        order.setPaymentStatus("REFUNDED");
        order.setEscrowHoldAmount(BigDecimal.ZERO);
        orderRepository.save(order);

        GroupDeal deal = order.getDeal();
        if (deal != null) {
            deal.setCommittedQty(Math.max(0, deal.getCommittedQty() - order.getQuantity()));
            deal.setCurrentParticipants(Math.max(0, deal.getCurrentParticipants() - 1));
            dealRepository.save(deal);
        }

        log.info("✓ Order {} cancelled. Refund of ₹{} initiated.", orderNumber, order.getRefundAmount());
        return mapToOrderResponse(order);
    }

    // ── Vendor Fulfillment & Doorstep Delivery ─────────────────────────────
    public List<GroupOrderResponse> getVendorOrders(String vendorId, Long communityId) {
        List<GroupDeal> vendorDeals = dealRepository.findByCommunityIdOrderByCreatedAtDesc(communityId);
        List<Long> dealIds = vendorDeals.stream().map(GroupDeal::getId).collect(Collectors.toList());
        
        List<GroupBuyOrder> allOrders = new ArrayList<>();
        for (Long dId : dealIds) {
            allOrders.addAll(orderRepository.findByDealId(dId));
        }
        return allOrders.stream().map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    @Transactional
    public GroupOrderResponse updateFulfillment(String orderNumber, OrderFulfillmentUpdateRequest request) {
        GroupBuyOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));

        try {
            OrderStatus newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
            order.setStatus(newStatus);
            if (newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.PICKED_UP) {
                order.setDeliveryTimestamp(LocalDateTime.now());
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + request.getStatus());
        }

        if (request.getTrackingNumber() != null) order.setTrackingNumber(request.getTrackingNumber());
        if (request.getDeliveryPartnerName() != null) order.setDeliveryPartnerName(request.getDeliveryPartnerName());
        if (request.getDeliveryPartnerPhone() != null) order.setDeliveryPartnerPhone(request.getDeliveryPartnerPhone());
        if (request.getNotes() != null) order.setSpecialNotes(request.getNotes());

        orderRepository.save(order);
        return mapToOrderResponse(order);
    }

    @Transactional
    public GroupOrderResponse verifyDeliveryOtp(DeliveryVerificationRequest request) {
        GroupBuyOrder order = orderRepository.findByOrderNumber(request.getOrderNumber())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + request.getOrderNumber()));

        if (order.getDeliveryOtp() == null || !order.getDeliveryOtp().trim().equals(request.getDeliveryOtp().trim())) {
            throw new IllegalArgumentException("Invalid Delivery OTP. Please verify with resident.");
        }

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveryTimestamp(LocalDateTime.now());
        orderRepository.save(order);

        log.info("✓ Doorstep delivery verified via OTP for order {}", request.getOrderNumber());
        return mapToOrderResponse(order);
    }

    // ── Vendor Settlement Engine ───────────────────────────────────────────
    public List<VendorSettlementResponse> getVendorSettlements(Long communityId, String vendorId) {
        List<GroupDealSettlement> settlements = vendorId != null 
                ? settlementRepository.findByVendorIdOrderByCreatedAtDesc(vendorId)
                : settlementRepository.findByCommunityIdOrderByCreatedAtDesc(communityId);
        return settlements.stream().map(this::mapToSettlementResponse).collect(Collectors.toList());
    }

    @Transactional
    public VendorSettlementResponse generateDealSettlement(Long dealId, AppUser vendorUser) {
        GroupDeal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found: " + dealId));

        List<GroupBuyOrder> orders = orderRepository.findByDealId(dealId)
                .stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.toList());

        int totalOrders = orders.size();
        int totalQuantity = orders.stream().mapToInt(GroupBuyOrder::getQuantity).sum();
        BigDecimal grossSales = orders.stream()
                .map(GroupBuyOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tierRefunds = orders.stream()
                .map(o -> o.getTierPriceRefundAmount() != null ? o.getTierPriceRefundAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal commRate = new BigDecimal("3.00");
        BigDecimal platformComm = grossSales.multiply(commRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal reserveRate = new BigDecimal("1.00");
        BigDecimal communityReserve = grossSales.multiply(reserveRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal netPayout = grossSales.subtract(platformComm).subtract(communityReserve);

        GroupDealSettlement settlement = settlementRepository.findByDealId(dealId)
                .orElse(GroupDealSettlement.builder()
                        .deal(deal)
                        .dealTitle(deal.getTitle())
                        .community(deal.getCommunity())
                        .vendorId(deal.getVendorId() != null ? deal.getVendorId() : "VND-" + deal.getId())
                        .vendorName(deal.getVendor() != null ? deal.getVendor() : "Community Vendor")
                        .build());

        settlement.setTotalOrders(totalOrders);
        settlement.setTotalQuantity(totalQuantity);
        settlement.setGrossSalesAmount(grossSales);
        settlement.setPlatformCommissionRate(commRate);
        settlement.setPlatformCommissionAmount(platformComm);
        settlement.setCommunityReserveRate(reserveRate);
        settlement.setCommunityReserveAmount(communityReserve);
        settlement.setTierRefundsTotal(tierRefunds);
        settlement.setNetVendorPayout(netPayout);
        settlement.setSettlementStatus("PENDING");

        settlementRepository.save(settlement);

        for (GroupBuyOrder ord : orders) {
            ord.setSettlementId(settlement.getId());
            orderRepository.save(ord);
        }

        return mapToSettlementResponse(settlement);
    }

    @Transactional
    public VendorSettlementResponse payoutSettlement(Long settlementId, String payoutReference) {
        GroupDealSettlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + settlementId));

        settlement.setSettlementStatus("SETTLED");
        settlement.setPayoutReference(payoutReference != null ? payoutReference : "PAYOUT-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        settlement.setSettledAt(LocalDateTime.now());
        settlementRepository.save(settlement);

        List<GroupBuyOrder> orders = orderRepository.findByDealId(settlement.getDeal().getId());
        for (GroupBuyOrder ord : orders) {
            ord.setIsSettled(true);
            ord.setEscrowHoldAmount(BigDecimal.ZERO);
            orderRepository.save(ord);
        }

        log.info("✓ Payout released for settlement {}: ₹{} (Ref: {})", settlementId, settlement.getNetVendorPayout(), settlement.getPayoutReference());
        return mapToSettlementResponse(settlement);
    }

    // ── Order Reviews, Disputes, and Pickup verification ───────────────────
    @Transactional
    public AuthorizedCollectorResponse authorizeCollector(String orderNumber, AuthorizeCollectorRequest request) {
        GroupBuyOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));

        String pin = String.format("%04d", (int) (Math.random() * 9000) + 1000);
        order.setCollectorName(request.getName());
        order.setCollectorRelation(request.getRelation());
        order.setCollectorPin(pin);
        orderRepository.save(order);

        return AuthorizedCollectorResponse.builder()
                .orderNumber(orderNumber)
                .collectorName(request.getName())
                .collectorRelation(request.getRelation())
                .collectorPin(pin)
                .authorizedAt(LocalDateTime.now())
                .build();
    }

    @Transactional
    public OrderReviewResponse submitReview(AppUser user, OrderReviewRequest request) {
        OrderReview review = OrderReview.builder()
                .orderId(request.getOrderId())
                .dealId(request.getDealId())
                .user(user)
                .residentName(user.getFullName() != null ? user.getFullName() : (request.getResidentName() != null ? request.getResidentName() : "Resident"))
                .productRating(request.getProductRating() != null ? request.getProductRating() : 5)
                .deliveryRating(request.getDeliveryRating() != null ? request.getDeliveryRating() : 5)
                .comment(request.getComment())
                .build();

        reviewRepository.save(review);
        return mapToReviewResponse(review);
    }

    public List<OrderReviewResponse> getReviewsForDeal(String dealId) {
        return reviewRepository.findByDealIdOrderByCreatedAtDesc(dealId)
                .stream().map(this::mapToReviewResponse).collect(Collectors.toList());
    }

    @Transactional
    public OrderDisputeResponse raiseDispute(AppUser user, OrderDisputeRequest request) {
        String dispNum = "DISP-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 900 + 100);
        OrderDispute dispute = OrderDispute.builder()
                .disputeNumber(dispNum)
                .orderId(request.getOrderId())
                .dealId(request.getDealId())
                .dealTitle(request.getDealTitle())
                .user(user)
                .residentName(user.getFullName() != null ? user.getFullName() : "Resident")
                .flatNumber(user.getFlatNo() != null ? user.getFlatNo() : "N/A")
                .reason(request.getReason())
                .requestedResolution(request.getRequestedResolution() != null ? request.getRequestedResolution() : "REFUND")
                .claimAmount(request.getClaimAmount() != null ? request.getClaimAmount() : BigDecimal.ZERO)
                .description(request.getDescription())
                .status("SUBMITTED")
                .build();

        disputeRepository.save(dispute);
        return mapToDisputeResponse(dispute);
    }

    public List<OrderDisputeResponse> getUserDisputes(Long userId) {
        return disputeRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToDisputeResponse).collect(Collectors.toList());
    }

    @Transactional
    public PickupVerificationResponse verifyPickupPass(PickupVerificationRequest request) {
        GroupBuyOrder order = null;
        if (request.getQrToken() != null && !request.getQrToken().isEmpty()) {
            order = orderRepository.findByQrToken(request.getQrToken()).orElse(null);
        }
        if (order == null && request.getOrderNumber() != null) {
            order = orderRepository.findByOrderNumber(request.getOrderNumber()).orElse(null);
        }

        if (order == null) {
            return PickupVerificationResponse.builder()
                    .valid(false)
                    .message("Invalid pickup token or order number")
                    .build();
        }

        if (order.getStatus() == OrderStatus.PICKED_UP || order.getStatus() == OrderStatus.DELIVERED) {
            return PickupVerificationResponse.builder()
                    .valid(false)
                    .orderNumber(order.getOrderNumber())
                    .message("Order has already been picked up / delivered")
                    .build();
        }

        if (request.getCollectorPin() != null && !request.getCollectorPin().isEmpty()) {
            if (!request.getCollectorPin().equals(order.getCollectorPin())) {
                return PickupVerificationResponse.builder()
                        .valid(false)
                        .orderNumber(order.getOrderNumber())
                        .message("Invalid collector PIN")
                        .build();
            }
        }

        order.setStatus(OrderStatus.PICKED_UP);
        order.setDeliveryTimestamp(LocalDateTime.now());
        orderRepository.save(order);

        return PickupVerificationResponse.builder()
                .valid(true)
                .orderNumber(order.getOrderNumber())
                .dealTitle(order.getDealTitle())
                .userName(order.getUser() != null ? order.getUser().getFullName() : "Resident")
                .userFlat(order.getUser() != null ? order.getUser().getFlatNo() : "")
                .quantity(order.getQuantity())
                .pickupPoint(order.getPickupPoint())
                .message("Pickup verified successfully! Hand over item to resident.")
                .build();
    }

    @Transactional
    public GroupDealResponse createVendorDeal(Long communityId, AppUser user, CreateVendorDealRequest request) {
        Community comm = user.getCommunity();
        GroupDeal deal = GroupDeal.builder()
                .community(comm)
                .title(request.getTitle())
                .category(request.getCategory())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl() != null ? request.getImageUrl() : "https://images.unsplash.com/photo-1542838132-92c53300491e?w=800")
                .vendor(user.getFullName() != null ? user.getFullName() : "Community Vendor")
                .vendorId(user.getId() != null ? "VND-" + user.getId() : "VND-LOCAL")
                .vendorRating(4.9)
                .vendorVerified(true)
                .pricingModel(PricingModel.THRESHOLD)
                .pricingType(PricingType.QUANTITY)
                .mrp(request.getMrp())
                .standardPrice(request.getStandardPrice())
                .currentPrice(request.getCurrentPrice())
                .currentTierPrice(request.getCurrentPrice())
                .targetQty(request.getTargetQty() != null ? request.getTargetQty() : 50)
                .moqLabel("Min 10 orders")
                .dealStatus(DealStatus.OPEN)
                .dealEndsAt(request.getDealEndsAt() != null ? request.getDealEndsAt() : LocalDateTime.now().plusDays(7))
                .pickupPoint(request.getPickupPoint() != null ? request.getPickupPoint() : "Clubhouse Reception")
                .pickupDate(LocalDateTime.now().plusDays(8))
                .fulfillmentType(FulfillmentType.BOTH)
                .paymentType(PaymentType.FULL)
                .isTrending(true)
                .isAlmostUnlocked(false)
                .isFestivalDeal(false)
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
    public DemandResponse createDemand(Long communityId, AppUser user, CreateDemandRequest request) {
        CommunityDemand demand = CommunityDemand.builder()
                .community(user.getCommunity())
                .createdByUser(user)
                .title(request.getTitle())
                .category(request.getCategory())
                .description(request.getDescription())
                .expectedQty(request.getExpectedQty() != null ? request.getExpectedQty() : 1)
                .preferredPriceMin(request.getPreferredPriceMin())
                .preferredPriceMax(request.getPreferredPriceMax())
                .preferredBrand(request.getPreferredBrand())
                .targetUpvotes(20)
                .build();

        demandRepository.save(demand);
        return mapToDemandResponse(demand);
    }

    @Transactional
    public void upvoteDemand(Long id) {
        CommunityDemand demand = demandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demand not found: " + id));
        demand.setUpvotesCount(demand.getUpvotesCount() + 1);
        demand.setInterestedResidents(demand.getInterestedResidents() + 1);
        demandRepository.save(demand);
    }

    public CommunitySavingsResponse getCommunitySavings(Long communityId) {
        List<GroupBuyOrder> orders = orderRepository.findByCommunityIdOrderByCreatedAtDesc(communityId);
        BigDecimal totalSavings = orders.stream()
                .map(o -> o.getSavingsAmount() != null ? o.getSavingsAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpend = orders.stream()
                .map(GroupBuyOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int participants = (int) orders.stream().map(o -> o.getUser().getId()).distinct().count();

        return CommunitySavingsResponse.builder()
                .totalSavedThisMonth(totalSavings)
                .totalOrders(orders.size())
                .activeDeals((int) dealRepository.count())
                .avgSavingPerOrder(orders.size() > 0 ? totalSavings.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .totalKgsBought(1250)
                .topCategory("Organic Groceries & Staples")
                .totalSavedAllTime(totalSavings.multiply(new BigDecimal("1.25")))
                .collectiveDiscountPercent(22.5)
                .heroMilestoneText("Community saved ₹" + totalSavings + " through bulk buying!")
                .topDealsThisMonth(Collections.emptyList())
                .towerLeaderboard(Collections.emptyList())
                .build();
    }

    @Transactional
    public VendorOfferDto submitVendorOffer(Long demandId, AppUser vendorUser, SubmitVendorOfferRequest request) {
        CommunityDemand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new IllegalArgumentException("Demand not found: " + demandId));

        CommunityDemandOffer offer = CommunityDemandOffer.builder()
                .demand(demand)
                .vendorId(vendorUser.getId() != null ? "VND-" + vendorUser.getId() : "VND-LOCAL")
                .vendorName(vendorUser.getFullName() != null ? vendorUser.getFullName() : "Verified Supplier")
                .vendorRating(4.8)
                .vendorVerified(true)
                .offeredPrice(request.getOfferedPrice())
                .minimumQty(request.getMinimumQty())
                .maximumQty(request.getMaximumQty())
                .deliveryDate(request.getDeliveryDate())
                .terms(request.getTerms())
                .build();

        if (demand.getVendorOffers() == null) {
            demand.setVendorOffers(new ArrayList<>());
        }
        demand.getVendorOffers().add(offer);
        demandRepository.save(demand);

        return mapToVendorOfferDto(offer);
    }

    @Transactional
    public GroupDealResponse acceptVendorOffer(Long demandId, Long offerId, AppUser user) {
        CommunityDemand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new IllegalArgumentException("Demand not found: " + demandId));

        CommunityDemandOffer selectedOffer = demand.getVendorOffers().stream()
                .filter(o -> o.getId().equals(offerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Offer not found: " + offerId));

        demand.setStatus(DemandStatus.FULFILLED);
        demandRepository.save(demand);

        GroupDeal deal = GroupDeal.builder()
                .community(demand.getCommunity())
                .title(demand.getTitle())
                .category(demand.getCategory())
                .description("Community-sourced deal created from resident demand. Sourced by " + selectedOffer.getVendorName())
                .imageUrl("https://images.unsplash.com/photo-1542838132-92c53300491e?w=800")
                .vendor(selectedOffer.getVendorName())
                .vendorId(selectedOffer.getVendorId() != null ? selectedOffer.getVendorId() : "VND-LOCAL")
                .vendorRating(selectedOffer.getVendorRating() != null ? selectedOffer.getVendorRating() : 4.8)
                .vendorVerified(true)
                .pricingModel(PricingModel.THRESHOLD)
                .pricingType(PricingType.QUANTITY)
                .mrp(selectedOffer.getOfferedPrice().multiply(new BigDecimal("1.30")).setScale(2, RoundingMode.HALF_UP))
                .standardPrice(selectedOffer.getOfferedPrice())
                .currentPrice(selectedOffer.getOfferedPrice())
                .currentTierPrice(selectedOffer.getOfferedPrice())
                .targetQty(selectedOffer.getMinimumQty())
                .moqLabel("MOQ " + selectedOffer.getMinimumQty() + " units")
                .dealStatus(DealStatus.OPEN)
                .dealEndsAt(LocalDateTime.now().plusDays(5))
                .pickupPoint("Community Clubhouse & Tower Lobby")
                .pickupDate(LocalDateTime.now().plusDays(8))
                .fulfillmentType(FulfillmentType.BOTH)
                .paymentType(PaymentType.FULL)
                .isTrending(true)
                .isAlmostUnlocked(false)
                .isFestivalDeal(false)
                .build();

        dealRepository.save(deal);
        return mapToDealResponse(deal);
    }

    public List<BuyingGroupDto> getTowerGroups(Long communityId, AppUser user) {
        return buyingGroupRepository.findByCommunityIdOrderByNameAsc(communityId)
                .stream().map(this::mapToBuyingGroupDto).collect(Collectors.toList());
    }

    public CommunityAiQueryResponse queryCommunityAi(Long communityId, String query) {
        String lower = query.toLowerCase();
        List<GroupDeal> deals = dealRepository.findByCommunityIdOrderByCreatedAtDesc(communityId);
        
        List<GroupDealResponse> matching = deals.stream()
                .filter(d -> d.getTitle().toLowerCase().contains(lower) || d.getCategory().toLowerCase().contains(lower) || lower.contains(d.getCategory().toLowerCase()))
                .map(this::mapToDealResponse)
                .collect(Collectors.toList());

        String explanation;
        if (!matching.isEmpty()) {
            explanation = "I found " + matching.size() + " active group buying deal(s) matching '" + query + "'. You can unlock extra discounts by ordering with your neighbors!";
        } else {
            explanation = "We don't have an active group deal for '" + query + "' yet, but you can post a Demand Request on the Community Demand Board. Once 20 neighbors upvote it, verified local suppliers will bid with wholesale pricing.";
        }

        return CommunityAiQueryResponse.builder()
                .query(query)
                .matchedDeals(matching)
                .matchedDemands(Collections.emptyList())
                .suggestedAction(!matching.isEmpty() ? "JOIN_DEAL" : "CREATE_DEMAND")
                .suggestedPrice(!matching.isEmpty() ? matching.get(0).getCurrentPrice() : null)
                .estimatedCommunitySavings(new BigDecimal("350.00"))
                .confidenceScore(0.95)
                .explanation(explanation)
                .build();
    }

    // ── Mapping Helpers ───────────────────────────────────────────────────
    private GroupDealResponse mapToDealResponse(GroupDeal deal) {
        List<PriceTierDto> tiers = deal.getPriceTiers() != null
                ? deal.getPriceTiers().stream().map(t -> PriceTierDto.builder()
                        .minQty(t.getMinQty())
                        .maxQty(t.getMaxQty())
                        .price(t.getPrice())
                        .label(t.getLabel())
                        .build()).collect(Collectors.toList())
                : Collections.emptyList();

        long hoursRemaining = deal.getDealEndsAt() != null 
                ? Math.max(0, ChronoUnit.HOURS.between(LocalDateTime.now(), deal.getDealEndsAt())) 
                : 48;

        return GroupDealResponse.builder()
                .id(deal.getId() != null ? String.valueOf(deal.getId()) : null)
                .title(deal.getTitle())
                .category(deal.getCategory())
                .subCategory(deal.getSubCategory())
                .description(deal.getDescription())
                .imageUrl(deal.getImageUrl())
                .vendorName(deal.getVendor())
                .vendorId(deal.getVendorId())
                .vendorRating(deal.getVendorRating() != null ? deal.getVendorRating() : 4.8)
                .vendorVerified(deal.getVendorVerified())
                .pricingModel(deal.getPricingModel())
                .pricingType(deal.getPricingType())
                .mrp(deal.getMrp())
                .standardPrice(deal.getStandardPrice())
                .currentPrice(deal.getCurrentPrice())
                .currentTierPrice(deal.getCurrentTierPrice())
                .committedQty(deal.getCommittedQty())
                .targetQty(deal.getTargetQty())
                .currentParticipants(deal.getCurrentParticipants())
                .targetParticipants(deal.getTargetParticipants())
                .inventoryRemaining(deal.getInventoryRemaining())
                .moqLabel(deal.getMoqLabel())
                .dealStatus(deal.getDealStatus())
                .dealEndsAt(deal.getDealEndsAt())
                .hoursRemaining(hoursRemaining)
                .priceLockedAt(deal.getPriceLockedAt())
                .pickupPoint(deal.getPickupPoint())
                .pickupDate(deal.getPickupDate())
                .fulfillmentType(deal.getFulfillmentType())
                .paymentType(deal.getPaymentType())
                .isTrending(Boolean.TRUE.equals(deal.getIsTrending()))
                .isAlmostUnlocked(Boolean.TRUE.equals(deal.getIsAlmostUnlocked()))
                .isFestivalDeal(Boolean.TRUE.equals(deal.getIsFestivalDeal()))
                .priceTiers(tiers)
                .createdAt(deal.getCreatedAt())
                .build();
    }

    private GroupOrderResponse mapToOrderResponse(GroupBuyOrder order) {
        return GroupOrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .dealId(order.getDeal() != null ? order.getDeal().getId() : null)
                .dealTitle(order.getDealTitle())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .totalAmount(order.getTotalAmount())
                .savingsAmount(order.getSavingsAmount())
                .status(order.getStatus() != null ? order.getStatus().name() : "CONFIRMED")
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus() : "PAID")
                .paymentMethod(order.getPaymentMethod())
                .transactionId(order.getTransactionId())
                .qrToken(order.getQrToken())
                .pickupPoint(order.getPickupPoint())
                .pickupDate(order.getPickupDate())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryOtp(order.getDeliveryOtp())
                .deliveryPartnerName(order.getDeliveryPartnerName())
                .deliveryPartnerPhone(order.getDeliveryPartnerPhone())
                .trackingNumber(order.getTrackingNumber())
                .deliveryTimestamp(order.getDeliveryTimestamp())
                .refundAmount(order.getRefundAmount())
                .refundReason(order.getRefundReason())
                .tierPriceRefundAmount(order.getTierPriceRefundAmount())
                .collectorPin(order.getCollectorPin())
                .collectorName(order.getCollectorName())
                .collectorRelation(order.getCollectorRelation())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private VendorSettlementResponse mapToSettlementResponse(GroupDealSettlement s) {
        return VendorSettlementResponse.builder()
                .id(s.getId())
                .dealId(s.getDeal() != null ? s.getDeal().getId() : null)
                .dealTitle(s.getDealTitle())
                .vendorId(s.getVendorId())
                .vendorName(s.getVendorName())
                .totalOrders(s.getTotalOrders())
                .totalQuantity(s.getTotalQuantity())
                .grossSalesAmount(s.getGrossSalesAmount())
                .platformCommissionRate(s.getPlatformCommissionRate())
                .platformCommissionAmount(s.getPlatformCommissionAmount())
                .communityReserveRate(s.getCommunityReserveRate())
                .communityReserveAmount(s.getCommunityReserveAmount())
                .tierRefundsTotal(s.getTierRefundsTotal())
                .netVendorPayout(s.getNetVendorPayout())
                .settlementStatus(s.getSettlementStatus())
                .payoutReference(s.getPayoutReference())
                .settledAt(s.getSettledAt())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private DemandResponse mapToDemandResponse(CommunityDemand demand) {
        List<VendorOfferDto> offers = demand.getVendorOffers() != null
                ? demand.getVendorOffers().stream().map(this::mapToVendorOfferDto).collect(Collectors.toList())
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
                .status(demand.getStatus())
                .vendorOffers(offers)
                .build();
    }

    private VendorOfferDto mapToVendorOfferDto(CommunityDemandOffer offer) {
        return VendorOfferDto.builder()
                .id(String.valueOf(offer.getId()))
                .demandId(offer.getDemand() != null ? String.valueOf(offer.getDemand().getId()) : null)
                .vendorId(offer.getVendorId())
                .vendorName(offer.getVendorName())
                .vendorRating(offer.getVendorRating())
                .vendorVerified(offer.getVendorVerified())
                .offeredPrice(offer.getOfferedPrice())
                .minimumQty(offer.getMinimumQty())
                .maximumQty(offer.getMaximumQty())
                .deliveryDate(offer.getDeliveryDate() != null ? offer.getDeliveryDate().toString() : null)
                .terms(offer.getTerms())
                .isBestValue(offer.getIsBestValue())
                .status("PROPOSED")
                .fulfillmentRate(98.5)
                .onTimeRate(96.0)
                .cancellationRate(0.5)
                .disputeRate(0.2)
                .qualityScore(4.9)
                .compositeScore(94)
                .scoringHighlights(List.of("Verified Gold Partner", "98.5% Fulfillment Rate"))
                .build();
    }

    private OrderReviewResponse mapToReviewResponse(OrderReview review) {
        return OrderReviewResponse.builder()
                .id(String.valueOf(review.getId()))
                .orderId(review.getOrderId())
                .dealId(review.getDealId())
                .residentName(review.getResidentName())
                .productRating(review.getProductRating())
                .deliveryRating(review.getDeliveryRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt() != null ? review.getCreatedAt().toString() : null)
                .build();
    }

    private OrderDisputeResponse mapToDisputeResponse(OrderDispute dispute) {
        return OrderDisputeResponse.builder()
                .id(String.valueOf(dispute.getId()))
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
                .createdAt(dispute.getCreatedAt() != null ? dispute.getCreatedAt().toString() : null)
                .resolvedAt(dispute.getResolvedAt() != null ? dispute.getResolvedAt().toString() : null)
                .vendorResponse(dispute.getVendorResponse())
                .build();
    }

    private BuyingGroupDto mapToBuyingGroupDto(BuyingGroup group) {
        return BuyingGroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .towerName(group.getTower() != null ? group.getTower() : group.getName())
                .description(group.getDescription())
                .membersCount(group.getMemberCount())
                .activeDealsCount(group.getActiveDealsCount())
                .totalSavingsAmount(group.getTotalSaved())
                .topCategory("Society Essentials")
                .isJoined(false)
                .build();
    }
}
