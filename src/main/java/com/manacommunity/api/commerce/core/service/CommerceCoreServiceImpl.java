package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.dto.*;
import com.manacommunity.api.commerce.core.model.*;
import com.manacommunity.api.commerce.core.repository.*;
import com.manacommunity.api.finance.personal.service.PersonalFinanceService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommerceCoreServiceImpl implements CommerceCoreService {

    private final CommerceOrderRepository orderRepository;
    private final CommercePaymentRepository paymentRepository;
    private final CommerceHandoverPassRepository handoverPassRepository;
    private final CommerceReviewRepository reviewRepository;
    private final CommerceDisputeRepository disputeRepository;
    private final CommerceSettlementRepository settlementRepository;
    private final CommerceProductRepository productRepository;
    private final CommerceRefundRepository refundRepository;
    private final CommerceRiskEngine riskEngine;
    private final PersonalFinanceService personalFinanceService;

    @Override
    @Transactional(readOnly = true)
    public List<CommerceProduct> getProducts(CommerceChannel channel) {
        if (channel != null) {
            return productRepository.findByChannelAndIsActiveTrue(channel);
        }
        return productRepository.findByIsActiveTrue();
    }

    @Override
    @Transactional
    public CommerceOrderDto checkout(AppUser buyer, CommerceCheckoutRequest request) {
        var risk = riskEngine.assessRisk(buyer, request);
        if (!risk.isAllowed()) {
            throw new IllegalStateException("Order rejected by Commerce Risk Engine: " + risk.decision());
        }

        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String handoverOtp = String.format("%06d", new Random().nextInt(900000) + 100000);

        BigDecimal subtotal = BigDecimal.ZERO;
        List<CommerceOrderItem> orderItems = new ArrayList<>();

        for (var itemReq : request.getItems()) {
            BigDecimal price = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : BigDecimal.ZERO;
            int qty = itemReq.getQuantity() != null ? itemReq.getQuantity() : 1;
            BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(qty));
            subtotal = subtotal.add(itemTotal);

            String img = itemReq.getImageUrl() != null ? itemReq.getImageUrl() : itemReq.getThumbnailUrl();

            orderItems.add(CommerceOrderItem.builder()
                    .sku(itemReq.getSku())
                    .title(itemReq.getTitle())
                    .unitPrice(price)
                    .quantity(qty)
                    .totalPrice(itemTotal)
                    .imageUrl(img)
                    .build());
        }

        BigDecimal discount = subtotal.multiply(BigDecimal.valueOf(0.05));
        BigDecimal deliveryFee = "DELIVERY".equalsIgnoreCase(request.getFulfillmentType()) ? BigDecimal.valueOf(30) : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.subtract(discount).add(deliveryFee);

        String vendorOrSeller = request.getVendorName() != null ? request.getVendorName() : request.getSellerName();
        String pickupOrDeliverySlot = request.getPickupSlot() != null ? request.getPickupSlot() : request.getDeliverySlot();

        CommerceOrder order = CommerceOrder.builder()
                .orderNumber(orderNumber)
                .channel(request.getChannel() != null ? request.getChannel() : CommerceChannel.MARKETPLACE)
                .buyer(buyer)
                .vendorName(vendorOrSeller)
                .status(CommerceOrderStatus.CONFIRMED)
                .fulfillmentType(request.getFulfillmentType() != null ? request.getFulfillmentType() : "CLUBHOUSE_PICKUP")
                .deliveryAddress(request.getDeliveryAddress())
                .pickupSlot(pickupOrDeliverySlot)
                .subtotalAmount(subtotal)
                .taxAmount(BigDecimal.ZERO)
                .deliveryFee(deliveryFee)
                .discountAmount(discount)
                .totalAmount(totalAmount)
                .handoverOtp(handoverOtp)
                .qrToken("QR-MANA-" + orderNumber)
                .items(orderItems)
                .community(buyer.getCommunity())
                .build();

        orderItems.forEach(i -> i.setOrder(order));
        CommerceOrder savedOrder = orderRepository.save(order);

        // Payment record
        CommercePayment payment = CommercePayment.builder()
                .paymentRef("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .order(savedOrder)
                .amount(totalAmount)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "UPI")
                .status("CAPTURED")
                .build();
        paymentRepository.save(payment);

        // Handover Pass record
        CommerceHandoverPass pass = CommerceHandoverPass.builder()
                .order(savedOrder)
                .passCode("PASS-" + orderNumber)
                .qrPayload("QR-MANA-" + orderNumber)
                .otp(handoverOtp)
                .fulfillmentType(savedOrder.getFulfillmentType())
                .pickupPoint(savedOrder.getDeliveryAddress() != null ? savedOrder.getDeliveryAddress() : "Clubhouse Main Desk")
                .status("ACTIVE")
                .build();
        handoverPassRepository.save(pass);

        return toDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommerceOrderDto> getMyOrders(AppUser buyer) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyer.getId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CommerceOrderDto getOrderByNumber(String orderNumber) {
        CommerceOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));
        return toDto(order);
    }

    @Override
    @Transactional
    public HandoverVerificationResponse verifyHandover(AppUser verifier, HandoverVerificationRequest request) {
        String queryNumber = request.getOrderNumber();
        String enteredPin = request.getEnteredOtp() != null ? request.getEnteredOtp() : request.getTokenOrOtp();

        CommerceOrder order = null;
        if (queryNumber != null) {
            order = orderRepository.findByOrderNumber(queryNumber).orElse(null);
        }

        if (order == null && enteredPin != null) {
            var passOpt = handoverPassRepository.findByOtp(enteredPin);
            if (passOpt.isPresent()) {
                order = passOpt.get().getOrder();
            }
        }

        if (order == null) {
            return HandoverVerificationResponse.builder()
                    .verified(false)
                    .message("Order not found for handover verification.")
                    .build();
        }

        boolean matched = (order.getHandoverOtp() != null && order.getHandoverOtp().equals(enteredPin))
                || (order.getQrToken() != null && order.getQrToken().equalsIgnoreCase(enteredPin));

        if (matched) {
            order.setStatus(CommerceOrderStatus.COMPLETED);
            orderRepository.save(order);

            return HandoverVerificationResponse.builder()
                    .verified(true)
                    .orderNumber(order.getOrderNumber())
                    .channel(order.getChannel() != null ? order.getChannel().name() : "MARKETPLACE")
                    .buyerName(order.getBuyer() != null ? order.getBuyer().getFullName() : "Resident")
                    .buyerFlat(order.getBuyer() != null ? order.getBuyer().getFlatNo() : "")
                    .totalAmount(order.getTotalAmount())
                    .status(order.getStatus().name())
                    .message("Handover authenticated! Payment released from escrow to seller.")
                    .build();
        }

        return HandoverVerificationResponse.builder()
                .verified(false)
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .message("Invalid 6-digit OTP verification code.")
                .build();
    }

    @Override
    @Transactional
    public CommerceReviewDto submitReview(AppUser reviewer, CommerceReviewDto reviewDto) {
        CommerceOrder order = orderRepository.findById(reviewDto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + reviewDto.getOrderId()));

        CommerceReview review = CommerceReview.builder()
                .order(order)
                .user(reviewer)
                .channel(order.getChannel())
                .targetType("ORDER")
                .targetId(String.valueOf(order.getId()))
                .rating(reviewDto.getRating())
                .comment(reviewDto.getComment())
                .build();

        CommerceReview saved = reviewRepository.save(review);
        return CommerceReviewDto.builder()
                .id(saved.getId())
                .orderId(order.getId())
                .rating(saved.getRating())
                .comment(saved.getComment())
                .build();
    }

    @Override
    @Transactional
    public CommerceDisputeDto raiseDispute(AppUser buyer, CommerceDisputeDto disputeDto) {
        CommerceOrder order = orderRepository.findById(disputeDto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + disputeDto.getOrderId()));

        CommerceDispute dispute = CommerceDispute.builder()
                .disputeCode("DISP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .order(order)
                .user(buyer)
                .channel(order.getChannel())
                .reason(disputeDto.getReason())
                .description(disputeDto.getDescription() != null ? disputeDto.getDescription() : disputeDto.getReason())
                .requestedResolution("REFUND")
                .status("SUBMITTED")
                .build();

        CommerceDispute saved = disputeRepository.save(dispute);
        order.setStatus(CommerceOrderStatus.DISPUTED);
        orderRepository.save(order);

        return CommerceDisputeDto.builder()
                .id(saved.getId())
                .disputeCode(saved.getDisputeCode())
                .orderId(order.getId())
                .reason(saved.getReason())
                .status(saved.getStatus())
                .build();
    }

    @Override
    @Transactional
    public CommerceRefund processRefund(AppUser user, String orderNumber, String reason) {
        CommerceOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));

        String refundNumber = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        CommerceRefund refund = CommerceRefund.builder()
                .refundNumber(refundNumber)
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .amount(order.getTotalAmount())
                .reason(reason)
                .status("PROCESSED")
                .bankReference("UPI-REF-" + System.currentTimeMillis())
                .build();

        CommerceRefund saved = refundRepository.save(refund);
        order.setStatus(CommerceOrderStatus.REFUNDED);
        orderRepository.save(order);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommerceSettlementDto> getMySettlements(AppUser seller) {
        return settlementRepository.findBySeller_Id(seller.getId()).stream()
                .map(s -> CommerceSettlementDto.builder()
                        .id(s.getId())
                        .settlementNumber(s.getSettlementNumber())
                        .vendorId(s.getVendorId())
                        .sellerId(seller.getId())
                        .communityId(s.getCommunity() != null ? s.getCommunity().getId() : null)
                        .grossAmount(s.getGrossAmount())
                        .platformFee(s.getPlatformFee())
                        .deductions(s.getDeductions())
                        .netPayout(s.getNetPayout())
                        .status(s.getStatus())
                        .payoutUtr(s.getPayoutUtr())
                        .payoutDate(s.getPayoutDate() != null ? s.getPayoutDate().toString() : null)
                        .build())
                .collect(Collectors.toList());
    }

    private CommerceOrderDto toDto(CommerceOrder order) {
        List<CommerceOrderItemDto> itemDtos = order.getItems() != null ? order.getItems().stream()
                .map(i -> CommerceOrderItemDto.builder()
                        .sku(i.getSku())
                        .title(i.getTitle())
                        .unitPrice(i.getUnitPrice())
                        .quantity(i.getQuantity())
                        .totalPrice(i.getTotalPrice())
                        .imageUrl(i.getImageUrl())
                        .thumbnailUrl(i.getImageUrl())
                        .build())
                .collect(Collectors.toList()) : Collections.emptyList();

        return CommerceOrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .channel(order.getChannel())
                .buyerId(order.getBuyer() != null ? order.getBuyer().getId() : null)
                .buyerName(order.getBuyer() != null ? order.getBuyer().getFullName() : null)
                .buyerFlat(order.getBuyer() != null ? order.getBuyer().getFlatNo() : null)
                .vendorName(order.getVendorName())
                .sellerName(order.getVendorName())
                .status(order.getStatus())
                .fulfillmentType(order.getFulfillmentType())
                .deliveryAddress(order.getDeliveryAddress())
                .pickupSlot(order.getPickupSlot())
                .subtotalAmount(order.getSubtotalAmount())
                .deliveryFee(order.getDeliveryFee())
                .discountAmount(order.getDiscountAmount())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .handoverOtp(order.getHandoverOtp())
                .qrToken(order.getQrToken())
                .items(itemDtos)
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : Instant.now().toString())
                .build();
    }
}
