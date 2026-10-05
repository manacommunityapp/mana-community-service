package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.dto.*;
import com.manacommunity.api.commerce.core.model.*;
import com.manacommunity.api.commerce.core.repository.*;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommerceCoreService {

    private final CommerceOrderRepository orderRepository;
    private final CommerceOrderItemRepository orderItemRepository;
    private final CommercePaymentRepository paymentRepository;
    private final CommerceHandoverPassRepository handoverPassRepository;
    private final CommerceReviewRepository reviewRepository;
    private final CommerceDisputeRepository disputeRepository;
    private final CommerceSettlementRepository settlementRepository;
    private final AppUserRepository userRepository;

    @Transactional
    public CommerceOrderDto checkout(AppUser buyer, CommerceCheckoutRequest request) {
        Community community = buyer.getCommunity();
        String orderNumber = "ORD-" + buyer.getCommunity().getId() + "-" + System.currentTimeMillis() % 1000000;
        String otp = String.format("%04d", (int) (Math.random() * 9000 + 1000));
        String qrToken = "TKN-" + request.getChannel() + "-" + System.currentTimeMillis() + "-" + otp;

        BigDecimal subtotal = request.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal delivery = request.getDeliveryFee() != null ? request.getDeliveryFee() : BigDecimal.ZERO;
        BigDecimal total = subtotal.subtract(discount).add(delivery);

        AppUser seller = null;
        if (request.getSellerId() != null) {
            seller = userRepository.findById(request.getSellerId()).orElse(null);
        }

        CommerceOrder order = CommerceOrder.builder()
                .orderNumber(orderNumber)
                .channel(request.getChannel())
                .buyer(buyer)
                .seller(seller)
                .vendorId(request.getVendorId())
                .vendorName(request.getVendorName() != null ? request.getVendorName() : "Community Partner")
                .community(community)
                .channelReferenceId(request.getChannelReferenceId())
                .status(CommerceOrderStatus.CONFIRMED)
                .subtotalAmount(subtotal)
                .discountAmount(discount)
                .deliveryFee(delivery)
                .totalAmount(total)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "UPI")
                .paymentStatus("CAPTURED")
                .fulfillmentType(request.getFulfillmentType() != null ? request.getFulfillmentType() : "CLUBHOUSE_PICKUP")
                .deliveryAddress(request.getDeliveryAddress())
                .pickupPoint(request.getPickupPoint() != null ? request.getPickupPoint() : "Clubhouse Desk")
                .pickupSlot(request.getPickupSlot())
                .handoverOtp(otp)
                .qrToken(qrToken)
                .isEscrowLocked(true)
                .build();

        CommerceOrder savedOrder = orderRepository.save(order);

        List<CommerceOrderItem> items = request.getItems().stream().map(dto -> CommerceOrderItem.builder()
                .order(savedOrder)
                .productId(dto.getProductId())
                .variantId(dto.getVariantId())
                .sku(dto.getSku())
                .title(dto.getTitle())
                .packSize(dto.getPackSize())
                .unitPrice(dto.getUnitPrice())
                .quantity(dto.getQuantity())
                .totalPrice(dto.getUnitPrice().multiply(BigDecimal.valueOf(dto.getQuantity())))
                .imageUrl(dto.getImageUrl())
                .build()).collect(Collectors.toList());

        orderItemRepository.saveAll(items);
        savedOrder.setItems(items);

        // Create Handover Pass
        CommerceHandoverPass pass = CommerceHandoverPass.builder()
                .order(savedOrder)
                .passCode("PASS-" + savedOrder.getId())
                .qrPayload(qrToken)
                .otp(otp)
                .fulfillmentType(savedOrder.getFulfillmentType())
                .pickupPoint(savedOrder.getPickupPoint())
                .status("ACTIVE")
                .build();
        handoverPassRepository.save(pass);

        // Record Initial Payment
        CommercePayment payment = CommercePayment.builder()
                .order(savedOrder)
                .paymentRef("PAY-" + System.currentTimeMillis())
                .gateway("RAZORPAY")
                .paymentMethod(savedOrder.getPaymentMethod())
                .amount(total)
                .currency("INR")
                .status("CAPTURED")
                .build();
        paymentRepository.save(payment);

        return mapToOrderDto(savedOrder);
    }

    @Transactional
    public HandoverVerificationResponse verifyHandover(AppUser guardOrCoordinator, HandoverVerificationRequest request) {
        String token = request.getTokenOrOtp().trim();

        CommerceOrder order = orderRepository.findByQrToken(token)
                .or(() -> orderRepository.findByOrderNumber(token))
                .orElse(null);

        if (order == null) {
            CommerceHandoverPass pass = handoverPassRepository.findByOtp(token).orElse(null);
            if (pass != null) {
                order = pass.getOrder();
            }
        }

        if (order == null) {
            return HandoverVerificationResponse.builder()
                    .verified(false)
                    .message("Invalid QR code or OTP token. Order not found.")
                    .build();
        }

        if (order.getStatus() == CommerceOrderStatus.COMPLETED || order.getStatus() == CommerceOrderStatus.DELIVERED) {
            return HandoverVerificationResponse.builder()
                    .verified(false)
                    .orderNumber(order.getOrderNumber())
                    .status(order.getStatus().name())
                    .message("Order has already been handed over and completed.")
                    .build();
        }

        order.setStatus(CommerceOrderStatus.COMPLETED);
        orderRepository.save(order);

        Optional<CommerceHandoverPass> passOpt = handoverPassRepository.findByOrderId(order.getId());
        passOpt.ifPresent(p -> {
            p.setStatus("VERIFIED");
            p.setVerifiedBy(guardOrCoordinator);
            p.setVerifiedAt(Instant.now());
            handoverPassRepository.save(p);
        });

        int totalQty = order.getItems().stream().mapToInt(CommerceOrderItem::getQuantity).sum();
        String summary = order.getItems().stream().map(i -> i.getTitle() + " x" + i.getQuantity()).collect(Collectors.joining(", "));

        return HandoverVerificationResponse.builder()
                .verified(true)
                .orderNumber(order.getOrderNumber())
                .channel(order.getChannel().name())
                .buyerName(order.getBuyer().getFullName())
                .buyerFlat(order.getBuyer().getFlatNo())
                .itemSummary(summary)
                .totalQuantity(totalQty)
                .totalAmount(order.getTotalAmount())
                .status("COMPLETED")
                .message("Handover verified successfully! Package released to resident.")
                .build();
    }

    public List<CommerceOrderDto> getMyOrders(AppUser user) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToOrderDto)
                .collect(Collectors.toList());
    }

    public CommerceOrderDto getOrderByNumber(String orderNumber) {
        CommerceOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));
        return mapToOrderDto(order);
    }

    @Transactional
    public CommerceReviewDto submitReview(AppUser user, CommerceReviewDto dto) {
        CommerceOrder order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + dto.getOrderId()));

        CommerceReview review = CommerceReview.builder()
                .order(order)
                .user(user)
                .channel(order.getChannel())
                .targetType(dto.getTargetType() != null ? dto.getTargetType() : "VENDOR")
                .targetId(dto.getTargetId() != null ? dto.getTargetId() : order.getVendorId())
                .rating(dto.getRating())
                .qualityScore(dto.getQualityScore())
                .onTimeScore(dto.getOnTimeScore())
                .comment(dto.getComment())
                .build();

        CommerceReview saved = reviewRepository.save(review);
        return mapToReviewDto(saved);
    }

    @Transactional
    public CommerceDisputeDto raiseDispute(AppUser user, CommerceDisputeDto dto) {
        CommerceOrder order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + dto.getOrderId()));

        String dispCode = "DISP-" + order.getChannel() + "-" + System.currentTimeMillis() % 100000;

        CommerceDispute dispute = CommerceDispute.builder()
                .disputeCode(dispCode)
                .order(order)
                .user(user)
                .channel(order.getChannel())
                .reason(dto.getReason())
                .description(dto.getDescription())
                .requestedResolution(dto.getRequestedResolution())
                .claimAmount(dto.getClaimAmount() != null ? dto.getClaimAmount() : order.getTotalAmount())
                .status("SUBMITTED")
                .build();

        order.setStatus(CommerceOrderStatus.DISPUTED);
        orderRepository.save(order);

        CommerceDispute saved = disputeRepository.save(dispute);
        return mapToDisputeDto(saved);
    }

    public List<CommerceSettlementDto> getVendorSettlements(String vendorId) {
        return settlementRepository.findByVendorIdOrderByCycleEndDateDesc(vendorId).stream()
                .map(this::mapToSettlementDto)
                .collect(Collectors.toList());
    }

    private CommerceOrderDto mapToOrderDto(CommerceOrder order) {
        List<CommerceOrderItemDto> itemDtos = order.getItems() != null ? order.getItems().stream().map(i -> CommerceOrderItemDto.builder()
                .productId(i.getProductId())
                .variantId(i.getVariantId())
                .sku(i.getSku())
                .title(i.getTitle())
                .packSize(i.getPackSize())
                .unitPrice(i.getUnitPrice())
                .quantity(i.getQuantity())
                .totalPrice(i.getTotalPrice())
                .imageUrl(i.getImageUrl())
                .build()).collect(Collectors.toList()) : Collections.emptyList();

        return CommerceOrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .channel(order.getChannel())
                .buyerId(order.getBuyer().getId())
                .buyerName(order.getBuyer().getFullName())
                .buyerFlat(order.getBuyer().getFlatNo())
                .sellerId(order.getSeller() != null ? order.getSeller().getId() : null)
                .vendorId(order.getVendorId())
                .vendorName(order.getVendorName())
                .communityId(order.getCommunity().getId())
                .status(order.getStatus())
                .subtotalAmount(order.getSubtotalAmount())
                .discountAmount(order.getDiscountAmount())
                .deliveryFee(order.getDeliveryFee())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .savingsAmount(order.getSavingsAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .fulfillmentType(order.getFulfillmentType())
                .deliveryAddress(order.getDeliveryAddress())
                .pickupPoint(order.getPickupPoint())
                .pickupSlot(order.getPickupSlot())
                .handoverOtp(order.getHandoverOtp())
                .qrToken(order.getQrToken())
                .isEscrowLocked(order.getIsEscrowLocked())
                .items(itemDtos)
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null)
                .build();
    }

    private CommerceReviewDto mapToReviewDto(CommerceReview review) {
        return CommerceReviewDto.builder()
                .id(review.getId())
                .orderId(review.getOrder().getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFullName())
                .channel(review.getChannel())
                .targetType(review.getTargetType())
                .targetId(review.getTargetId())
                .rating(review.getRating())
                .qualityScore(review.getQualityScore())
                .onTimeScore(review.getOnTimeScore())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt() != null ? review.getCreatedAt().toString() : null)
                .build();
    }

    private CommerceDisputeDto mapToDisputeDto(CommerceDispute dispute) {
        return CommerceDisputeDto.builder()
                .id(dispute.getId())
                .disputeCode(dispute.getDisputeCode())
                .orderId(dispute.getOrder().getId())
                .orderNumber(dispute.getOrder().getOrderNumber())
                .userId(dispute.getUser().getId())
                .userName(dispute.getUser().getFullName())
                .channel(dispute.getChannel())
                .reason(dispute.getReason())
                .description(dispute.getDescription())
                .requestedResolution(dispute.getRequestedResolution())
                .claimAmount(dispute.getClaimAmount())
                .status(dispute.getStatus())
                .vendorResponse(dispute.getVendorResponse())
                .resolvedAt(dispute.getResolvedAt() != null ? dispute.getResolvedAt().toString() : null)
                .createdAt(dispute.getCreatedAt() != null ? dispute.getCreatedAt().toString() : null)
                .build();
    }

    private CommerceSettlementDto mapToSettlementDto(CommerceSettlement settlement) {
        return CommerceSettlementDto.builder()
                .id(settlement.getId())
                .settlementNumber(settlement.getSettlementNumber())
                .vendorId(settlement.getVendorId())
                .sellerId(settlement.getSeller() != null ? settlement.getSeller().getId() : null)
                .communityId(settlement.getCommunity().getId())
                .cycleStartDate(settlement.getCycleStartDate().toString())
                .cycleEndDate(settlement.getCycleEndDate().toString())
                .totalOrdersCount(settlement.getTotalOrdersCount())
                .grossAmount(settlement.getGrossAmount())
                .platformFee(settlement.getPlatformFee())
                .deductions(settlement.getDeductions())
                .netPayout(settlement.getNetPayout())
                .status(settlement.getStatus())
                .payoutUtr(settlement.getPayoutUtr())
                .payoutDate(settlement.getPayoutDate() != null ? settlement.getPayoutDate().toString() : null)
                .build();
    }
}