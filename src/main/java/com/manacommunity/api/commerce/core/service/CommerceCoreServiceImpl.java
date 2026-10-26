package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.dto.*;
import com.manacommunity.api.commerce.core.model.*;
import com.manacommunity.api.commerce.core.repository.*;
import com.manacommunity.api.finance.dto.FinanceDocumentDto;
import com.manacommunity.api.finance.personal.service.PersonalFinanceService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private final CommerceInvoiceService invoiceService;
    private final CommerceInventoryService inventoryService;
    private final CommerceLoyaltyService loyaltyService;
    private final CommerceNotificationService notificationService;
    private final AppUserRepository userRepository;
    private final CommerceCommissionService commissionService;

    private static final Map<String, Integer> failedOtpAttempts = new ConcurrentHashMap<>();
    private static final Map<String, Long> lockoutTimestamps = new ConcurrentHashMap<>();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000L;

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

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Checkout must include at least one item.");
        }

        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String handoverOtp = String.format("%06d", new Random().nextInt(900000) + 100000);

        BigDecimal subtotal = BigDecimal.ZERO;
        List<CommerceOrderItem> orderItems = new ArrayList<>();
        AppUser seller = null;

        for (var itemReq : request.getItems()) {
            int qty = itemReq.getQuantity() != null ? itemReq.getQuantity() : 1;
            if (qty <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero for item: " + itemReq.getSku());
            }

            CommerceProduct product = null;
            if (itemReq.getSku() != null && !itemReq.getSku().isBlank()) {
                product = productRepository.findBySku(itemReq.getSku()).orElse(null);
            }
            if (product == null && itemReq.getProductId() != null) {
                try {
                    Long pId = Long.parseLong(itemReq.getProductId());
                    product = productRepository.findById(pId).orElse(null);
                } catch (NumberFormatException ignored) {}
            }

            if (product == null) {
                throw new IllegalArgumentException("Product not found in catalog for SKU: " + itemReq.getSku());
            }

            if (!product.isActive()) {
                throw new IllegalStateException("Product is no longer active or available: " + product.getTitle());
            }

            // Enforce catalog price server-side; ignore client-tampered unitPrice
            BigDecimal unitPrice = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getBasePrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Invalid product price configuration for SKU: " + product.getSku());
            }

            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            subtotal = subtotal.add(itemTotal);

            String img = product.getThumbnailUrl() != null ? product.getThumbnailUrl()
                    : (itemReq.getImageUrl() != null ? itemReq.getImageUrl() : itemReq.getThumbnailUrl());
            String title = product.getTitle() != null ? product.getTitle() : itemReq.getTitle();

            orderItems.add(CommerceOrderItem.builder()
                    .sku(product.getSku())
                    .title(title)
                    .unitPrice(unitPrice)
                    .quantity(qty)
                    .totalPrice(itemTotal)
                    .imageUrl(img)
                    .build());

            if (seller == null && product.getSellerId() != null) {
                seller = userRepository.findById(product.getSellerId()).orElse(null);
            }
        }

        if (seller == null && request.getSellerId() != null) {
            seller = userRepository.findById(request.getSellerId()).orElse(null);
        }

        // Server-side calculated discount and delivery fee (never trust client-sent amounts)
        BigDecimal discount = subtotal.multiply(BigDecimal.valueOf(0.05));
        BigDecimal deliveryFee = "DELIVERY".equalsIgnoreCase(request.getFulfillmentType()) ? BigDecimal.valueOf(30) : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.subtract(discount).add(deliveryFee);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        String vendorOrSeller = request.getVendorName() != null ? request.getVendorName() : request.getSellerName();
        if (vendorOrSeller == null && seller != null) {
            vendorOrSeller = seller.getFullName();
        }
        String pickupOrDeliverySlot = request.getPickupSlot() != null ? request.getPickupSlot() : request.getDeliverySlot();

        CommerceOrder order = CommerceOrder.builder()
                .orderNumber(orderNumber)
                .channel(request.getChannel() != null ? request.getChannel() : CommerceChannel.MARKETPLACE)
                .buyer(buyer)
                .seller(seller)
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

        // Phase 8: commit stock for confirmed orders
        try {
            inventoryService.commitStock(savedOrder);
        } catch (Exception e) {
            log.warn("Inventory commit failed for order {}: {}", orderNumber, e.getMessage());
        }
        // Phase 12: notify buyer + vendor
        notificationService.notifyOrderConfirmed(savedOrder);
        notificationService.notifyVendorNewOrder(savedOrder);

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
    public CommerceOrderDto getOrderByNumber(AppUser user, String orderNumber) {
        CommerceOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));

        boolean isBuyer = order.getBuyer() != null && order.getBuyer().getId().equals(user.getId());
        boolean isSeller = order.getSeller() != null && order.getSeller().getId().equals(user.getId());
        boolean isAdmin = isAdmin(user);

        if (!isBuyer && !isSeller && !isAdmin) {
            throw new AccessDeniedException("Access denied to order " + orderNumber);
        }

        CommerceOrderDto dto = toDto(order);
        // Only buyer is entitled to see the handover OTP in clear text; sellers must scan QR or enter code at handover
        if (!isBuyer) {
            dto.setHandoverOtp(null);
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public CommerceOrderDto getOrderByNumber(String orderNumber) {
        CommerceOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));
        CommerceOrderDto dto = toDto(order);
        dto.setHandoverOtp(null); // Never leak OTP anonymously
        return dto;
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

        // Authorization check: Buyer cannot self-verify handover
        if (order.getBuyer() != null && order.getBuyer().getId().equals(verifier.getId())) {
            throw new IllegalStateException("Buyer cannot verify their own order handover.");
        }

        boolean isSeller = (order.getSeller() != null && order.getSeller().getId().equals(verifier.getId()))
                || (order.getVendorName() != null && order.getVendorName().equalsIgnoreCase(verifier.getFullName()));
        boolean isAdmin = isAdmin(verifier);

        if (!isSeller && !isAdmin) {
            throw new AccessDeniedException("Only the seller or authorized community staff can verify order handover.");
        }

        // Brute-force rate limiting check
        Long lockedUntil = lockoutTimestamps.get(order.getOrderNumber());
        if (lockedUntil != null) {
            if (System.currentTimeMillis() < lockedUntil) {
                long remainingMins = Math.max(1, (lockedUntil - System.currentTimeMillis()) / 60000);
                return HandoverVerificationResponse.builder()
                        .verified(false)
                        .orderNumber(order.getOrderNumber())
                        .status(order.getStatus().name())
                        .message("Too many failed OTP attempts. Handover verification is locked for " + remainingMins + " more minute(s).")
                        .build();
            } else {
                lockoutTimestamps.remove(order.getOrderNumber());
                failedOtpAttempts.remove(order.getOrderNumber());
            }
        }

        boolean matched = (order.getHandoverOtp() != null && order.getHandoverOtp().equals(enteredPin))
                || (order.getQrToken() != null && order.getQrToken().equalsIgnoreCase(enteredPin));

        if (matched) {
            failedOtpAttempts.remove(order.getOrderNumber());
            lockoutTimestamps.remove(order.getOrderNumber());

            order.setStatus(CommerceOrderStatus.COMPLETED);
            orderRepository.save(order);
            // Auto-generate GST invoice on completion
            try { invoiceService.generateInvoiceForOrder(order); }
            catch (Exception ex) { log.warn("Invoice generation failed for order {}: {}", order.getOrderNumber(), ex.getMessage()); }
            // Phase 10: award loyalty points on completion
            try { loyaltyService.awardPointsForOrder(order); }
            catch (Exception ex) { log.warn("Loyalty award failed for order {}: {}", order.getOrderNumber(), ex.getMessage()); }
            // Phase 12: notify buyer on completion
            notificationService.notifyOrderCompleted(order);

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

        int attempts = failedOtpAttempts.getOrDefault(order.getOrderNumber(), 0) + 1;
        failedOtpAttempts.put(order.getOrderNumber(), attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            lockoutTimestamps.put(order.getOrderNumber(), System.currentTimeMillis() + LOCKOUT_DURATION_MS);
            return HandoverVerificationResponse.builder()
                    .verified(false)
                    .orderNumber(order.getOrderNumber())
                    .status(order.getStatus().name())
                    .message("Too many failed OTP attempts. Handover verification locked for 15 minutes.")
                    .build();
        }

        return HandoverVerificationResponse.builder()
                .verified(false)
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .message("Invalid 6-digit OTP verification code. " + (MAX_FAILED_ATTEMPTS - attempts) + " attempt(s) remaining.")
                .build();
    }

    @Override
    @Transactional
    public CommerceReviewDto submitReview(AppUser reviewer, CommerceReviewDto reviewDto) {
        if (reviewDto.getOrderId() == null) {
            throw new IllegalArgumentException("Order ID is required to submit a review.");
        }
        CommerceOrder order = orderRepository.findById(reviewDto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + reviewDto.getOrderId()));

        if (order.getBuyer() == null || !order.getBuyer().getId().equals(reviewer.getId())) {
            throw new AccessDeniedException("Only the buyer who placed the order can submit a review.");
        }

        if (order.getStatus() != CommerceOrderStatus.COMPLETED) {
            throw new IllegalStateException("Reviews can only be submitted for completed orders.");
        }

        if (reviewRepository.existsByOrderIdAndUserId(order.getId(), reviewer.getId())) {
            throw new IllegalStateException("You have already reviewed this order.");
        }

        if (reviewDto.getRating() == null || reviewDto.getRating() < 1 || reviewDto.getRating() > 5) {
            throw new IllegalArgumentException("Review rating must be between 1 and 5 stars.");
        }

        CommerceReview review = CommerceReview.builder()
                .order(order)
                .user(reviewer)
                .channel(order.getChannel())
                .targetType("ORDER")
                .targetId(String.valueOf(order.getId()))
                .rating(reviewDto.getRating())
                .qualityScore(reviewDto.getQualityScore())
                .onTimeScore(reviewDto.getOnTimeScore())
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
        if (disputeDto.getOrderId() == null) {
            throw new IllegalArgumentException("Order ID is required to raise a dispute.");
        }
        CommerceOrder order = orderRepository.findById(disputeDto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + disputeDto.getOrderId()));

        if (order.getBuyer() == null || !order.getBuyer().getId().equals(buyer.getId())) {
            throw new AccessDeniedException("Only the buyer who placed the order can raise a dispute.");
        }

        if (order.getStatus() == CommerceOrderStatus.DISPUTED) {
            throw new IllegalStateException("A dispute is already active for this order.");
        }
        if (order.getStatus() == CommerceOrderStatus.REFUNDED || order.getStatus() == CommerceOrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot dispute an order that has already been refunded or cancelled.");
        }

        if (disputeDto.getReason() == null || disputeDto.getReason().isBlank()) {
            throw new IllegalArgumentException("Dispute reason is required.");
        }

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

        boolean isBuyer = order.getBuyer() != null && order.getBuyer().getId().equals(user.getId());
        boolean isAdmin = isAdmin(user);
        if (!isBuyer && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to refund order " + orderNumber);
        }

        if (order.getStatus() == CommerceOrderStatus.REFUNDED || order.getStatus() == CommerceOrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already refunded or cancelled.");
        }

        String refundNumber = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        CommerceRefund refund = CommerceRefund.builder()
                .refundNumber(refundNumber)
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .amount(order.getTotalAmount())
                .reason(reason != null ? reason : "Order refund requested")
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
                .map(this::toSettlementDto)
                .collect(Collectors.toList());
    }

    // ── Phase 4: order lifecycle transitions ──────────────────────────────────

    @Override
    @Transactional
    public CommerceOrderDto cancelOrder(AppUser user, String orderNumber, String reason) {
        CommerceOrder order = requireOrder(orderNumber);

        boolean isBuyer = order.getBuyer() != null && order.getBuyer().getId().equals(user.getId());
        if (!isBuyer && !isAdmin(user)) {
            throw new AccessDeniedException("Only the buyer or an admin can cancel this order.");
        }

        EnumSet<CommerceOrderStatus> cancellable = EnumSet.of(
                CommerceOrderStatus.PENDING, CommerceOrderStatus.CONFIRMED, CommerceOrderStatus.PROCESSING);
        if (!cancellable.contains(order.getStatus())) {
            throw new IllegalStateException("Order cannot be cancelled in status: " + order.getStatus());
        }

        // Phase 8: restore stock on cancellation
        try {
            inventoryService.releaseStock(order);
        } catch (Exception e) {
            log.warn("Inventory release failed for order {}: {}", orderNumber, e.getMessage());
        }

        order.setStatus(CommerceOrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        CommerceOrder saved = orderRepository.save(order);
        notificationService.notifyOrderCancelled(saved);
        return toDto(saved);
    }

    @Override
    @Transactional
    public CommerceOrderDto processOrder(AppUser vendor, String orderNumber, String vendorNote) {
        CommerceOrder order = requireOrder(orderNumber);
        requireVendorOrAdmin(vendor, order);

        if (order.getStatus() != CommerceOrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order must be CONFIRMED before processing. Current: " + order.getStatus());
        }

        order.setStatus(CommerceOrderStatus.PROCESSING);
        if (vendorNote != null && !vendorNote.isBlank()) order.setVendorNotes(vendorNote);
        CommerceOrder saved = orderRepository.save(order);
        notificationService.notifyOrderProcessing(saved);
        return toDto(saved);
    }

    @Override
    @Transactional
    public CommerceOrderDto markReadyForPickup(AppUser vendor, String orderNumber) {
        CommerceOrder order = requireOrder(orderNumber);
        requireVendorOrAdmin(vendor, order);

        if (order.getStatus() != CommerceOrderStatus.PROCESSING) {
            throw new IllegalStateException("Order must be PROCESSING to mark ready. Current: " + order.getStatus());
        }

        order.setStatus(CommerceOrderStatus.READY_FOR_PICKUP);
        CommerceOrder saved = orderRepository.save(order);
        notificationService.notifyOrderReady(saved);
        return toDto(saved);
    }

    @Override
    @Transactional
    public CommerceOrderDto markOutForDelivery(AppUser vendor, String orderNumber, String estimatedDeliveryDate) {
        CommerceOrder order = requireOrder(orderNumber);
        requireVendorOrAdmin(vendor, order);

        if (order.getStatus() != CommerceOrderStatus.READY_FOR_PICKUP) {
            throw new IllegalStateException("Order must be READY_FOR_PICKUP to dispatch. Current: " + order.getStatus());
        }

        order.setStatus(CommerceOrderStatus.OUT_FOR_DELIVERY);
        if (estimatedDeliveryDate != null) {
            try {
                order.setEstimatedDeliveryDate(LocalDate.parse(estimatedDeliveryDate));
            } catch (Exception ignored) {}
        }
        CommerceOrder saved = orderRepository.save(order);
        notificationService.notifyOrderOutForDelivery(saved);
        return toDto(saved);
    }

    @Override
    @Transactional
    public CommerceOrderDto markDelivered(AppUser user, String orderNumber) {
        CommerceOrder order = requireOrder(orderNumber);
        requireVendorOrAdmin(user, order);

        EnumSet<CommerceOrderStatus> deliverable = EnumSet.of(
                CommerceOrderStatus.OUT_FOR_DELIVERY, CommerceOrderStatus.READY_FOR_PICKUP);
        if (!deliverable.contains(order.getStatus())) {
            throw new IllegalStateException("Cannot mark as delivered from status: " + order.getStatus());
        }

        order.setStatus(CommerceOrderStatus.DELIVERED);
        order.setActualDeliveryAt(LocalDateTime.now());
        CommerceOrder saved = orderRepository.save(order);
        notificationService.notifyOrderDelivered(saved);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommerceOrderDto> getVendorOrders(AppUser vendor, CommerceOrderStatus status) {
        String vendorIdStr = String.valueOf(vendor.getId());
        List<CommerceOrder> orders = status != null
                ? orderRepository.findByVendorIdAndStatusOrderByCreatedAtDesc(vendorIdStr, status)
                : orderRepository.findByVendorIdOrderByCreatedAtDesc(vendorIdStr);

        if (orders.isEmpty()) {
            // also look by sellerId
            orders = status != null
                    ? orderRepository.findBySellerIdAndStatusOrderByCreatedAtDesc(vendor.getId(), status)
                    : orderRepository.findBySellerIdOrderByCreatedAtDesc(vendor.getId());
        }

        return orders.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommerceOrderDto> getAdminOrders(AppUser admin, CommerceOrderStatus status, Pageable pageable) {
        if (!isAdmin(admin)) {
            throw new AccessDeniedException("Only admins can view all community orders.");
        }
        Long communityId = admin.getCommunity().getId();
        Page<CommerceOrder> page = status != null
                ? orderRepository.findByCommunityIdAndStatusPaged(communityId, status, pageable)
                : orderRepository.findByCommunityId(communityId, pageable);
        return page.map(this::toDto);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private CommerceOrder requireOrder(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));
    }

    private void requireVendorOrAdmin(AppUser user, CommerceOrder order) {
        boolean isSeller = (order.getSeller() != null && order.getSeller().getId().equals(user.getId()))
                || (order.getVendorId() != null && order.getVendorId().equals(String.valueOf(user.getId())));
        if (!isSeller && !isAdmin(user)) {
            throw new AccessDeniedException("Only the vendor or an admin can perform this action.");
        }
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
                .notes(order.getNotes())
                .vendorNotes(order.getVendorNotes())
                .estimatedDeliveryDate(order.getEstimatedDeliveryDate() != null ? order.getEstimatedDeliveryDate().toString() : null)
                .actualDeliveryAt(order.getActualDeliveryAt() != null ? order.getActualDeliveryAt().toString() : null)
                .cancellationReason(order.getCancellationReason())
                .invoiceDocumentId(order.getInvoiceDocumentId())
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : Instant.now().toString())
                .updatedAt(order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<FinanceDocumentDto> getOrderInvoice(AppUser user, String orderNumber) {
        CommerceOrder order = requireOrder(orderNumber);
        if (order.getBuyer() != null && !order.getBuyer().getId().equals(user.getId()) && !isAdmin(user)) {
            throw new AccessDeniedException("Not authorised to view this invoice");
        }
        return invoiceService.findInvoiceForOrder(order);
    }

    // ── Phase 7: Settlement & Payouts ─────────────────────────────────────────

    @Override
    @Transactional
    public CommerceSettlementDto runSettlementCycle(AppUser admin, com.manacommunity.api.commerce.core.dto.CommerceSettlementRunRequest req) {
        if (!isAdmin(admin)) throw new AccessDeniedException("Only admins can run settlement cycles");

        Instant start = java.time.LocalDate.parse(req.getCycleStart()).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        Instant end   = java.time.LocalDate.parse(req.getCycleEnd()).plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();

        List<CommerceOrder> orders;
        if (req.getSellerId() != null) {
            orders = orderRepository.findCompletedOrdersForSellerInCycle(req.getSellerId(), start, end);
        } else if (req.getVendorId() != null) {
            orders = orderRepository.findCompletedOrdersForVendorInCycle(req.getVendorId(), start, end);
        } else {
            throw new IllegalArgumentException("Either sellerId or vendorId must be provided");
        }

        BigDecimal gross = orders.stream()
                .map(CommerceOrder::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Use commission engine: request override > vendor-specific rule > category rule > community default > 2.5%
        BigDecimal fee;
        if (req.getPlatformFeePercent() != null) {
            fee = gross.multiply(BigDecimal.valueOf(req.getPlatformFeePercent() / 100.0))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        } else {
            fee = commissionService.computeFeeAmount(gross, admin.getCommunity().getId(), req.getSellerId(), null);
        }
        BigDecimal net = gross.subtract(fee);

        String settlementNum = "STL-" + java.time.LocalDate.parse(req.getCycleEnd()).getYear()
                + "-" + System.currentTimeMillis();

        var community = admin.getCommunity();
        AppUser seller = req.getSellerId() != null
                ? userRepository.findById(req.getSellerId()).orElse(null) : null;

        var settlement = CommerceSettlement.builder()
                .settlementNumber(settlementNum)
                .vendorId(req.getVendorId())
                .seller(seller)
                .community(community)
                .cycleStartDate(start)
                .cycleEndDate(end)
                .totalOrdersCount(orders.size())
                .grossAmount(gross)
                .platformFee(fee)
                .deductions(BigDecimal.ZERO)
                .netPayout(net)
                .status("PENDING")
                .adminNote(req.getAdminNote())
                .build();

        var saved = settlementRepository.save(settlement);
        return toSettlementDto(saved);
    }

    @Override
    @Transactional
    public CommerceSettlementDto markSettlementPaid(AppUser admin, String settlementNumber,
            com.manacommunity.api.commerce.core.dto.CommerceSettlementMarkPaidRequest req) {
        if (!isAdmin(admin)) throw new AccessDeniedException("Only admins can mark settlements as paid");

        var settlement = settlementRepository.findBySettlementNumber(settlementNumber)
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + settlementNumber));

        if (!"PENDING".equals(settlement.getStatus())) {
            throw new IllegalStateException("Settlement is not in PENDING state: " + settlement.getStatus());
        }

        settlement.setPayoutUtr(req.getPayoutUtr());
        settlement.setPayoutDate(Instant.now());
        settlement.setProcessedAt(Instant.now());
        settlement.setStatus("PAID");
        if (req.getAdminNote() != null) settlement.setAdminNote(req.getAdminNote());

        return toSettlementDto(settlementRepository.save(settlement));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommerceSettlementDto> getAdminSettlements(AppUser admin, String status, Pageable pageable) {
        if (!isAdmin(admin)) throw new AccessDeniedException("Only admins can view all settlements");
        Long communityId = admin.getCommunity().getId();
        Page<CommerceSettlement> page = (status != null && !status.isBlank())
                ? settlementRepository.findByCommunityIdAndStatusPaged(communityId, status.toUpperCase(), pageable)
                : settlementRepository.findByCommunityIdPaged(communityId, pageable);
        return page.map(this::toSettlementDto);
    }

    private CommerceSettlementDto toSettlementDto(CommerceSettlement s) {
        return CommerceSettlementDto.builder()
                .id(s.getId())
                .settlementNumber(s.getSettlementNumber())
                .vendorId(s.getVendorId())
                .sellerId(s.getSeller() != null ? s.getSeller().getId() : null)
                .communityId(s.getCommunity() != null ? s.getCommunity().getId() : null)
                .cycleStartDate(s.getCycleStartDate() != null ? s.getCycleStartDate().toString() : null)
                .cycleEndDate(s.getCycleEndDate() != null ? s.getCycleEndDate().toString() : null)
                .totalOrdersCount(s.getTotalOrdersCount())
                .grossAmount(s.getGrossAmount())
                .platformFee(s.getPlatformFee())
                .deductions(s.getDeductions())
                .netPayout(s.getNetPayout())
                .status(s.getStatus())
                .payoutUtr(s.getPayoutUtr())
                .payoutDate(s.getPayoutDate() != null ? s.getPayoutDate().toString() : null)
                .build();
    }

    private boolean isAdmin(AppUser user) {
        if (user == null) return false;
        if (user.getRole() != null && user.getRole().toUpperCase().contains("ADMIN")) {
            return true;
        }
        if (user.getUserRoles() != null) {
            return user.getUserRoles().stream()
                    .anyMatch(r -> r.getName() != null && r.getName().toUpperCase().contains("ADMIN"));
        }
        return false;
    }

    // ── Phase 10: Loyalty & Rewards ──────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CommerceLoyaltyDto getMyLoyalty(AppUser user) {
        return loyaltyService.getMyLoyalty(user);
    }

    @Override
    @Transactional
    public CommerceLoyaltyDto redeemLoyaltyPoints(AppUser user, long points) {
        loyaltyService.redeemPoints(user, points);
        return loyaltyService.getMyLoyalty(user);
    }

    // ── Phase 15: Dispute Resolution ─────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<CommerceDisputeDto> getAdminDisputes(AppUser admin, String status, Pageable pageable) {
        Long communityId = admin.getCommunity().getId();
        Page<CommerceDispute> page = (status != null && !status.isBlank())
                ? disputeRepository.findByCommunityIdAndStatusPaged(communityId, status.toUpperCase(), pageable)
                : disputeRepository.findByCommunityIdPaged(communityId, pageable);
        return page.map(this::toDisputeDto);
    }

    @Override
    @Transactional
    public CommerceDisputeDto resolveDispute(AppUser admin, String disputeCode, String resolution, String adminNote) {
        CommerceDispute dispute = disputeRepository.findByDisputeCode(disputeCode)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeCode));

        Long communityId = admin.getCommunity().getId();
        if (!dispute.getOrder().getCommunity().getId().equals(communityId)) {
            throw new AccessDeniedException("Dispute does not belong to your community.");
        }

        dispute.setResolution(resolution);
        dispute.setAdminNote(adminNote);
        dispute.setResolvedBy(admin);
        dispute.setResolvedAt(Instant.now());
        dispute.setStatus("RESOLVED");

        if ("REFUND".equalsIgnoreCase(resolution)) {
            try {
                processRefund(admin, dispute.getOrder().getOrderNumber(), "Admin resolved dispute: " + disputeCode);
            } catch (Exception ignored) {}
        }

        CommerceDispute saved = disputeRepository.save(dispute);
        return toDisputeDto(saved);
    }

    private CommerceDisputeDto toDisputeDto(CommerceDispute d) {
        return CommerceDisputeDto.builder()
                .id(d.getId())
                .disputeCode(d.getDisputeCode())
                .orderId(d.getOrder().getId())
                .orderNumber(d.getOrder().getOrderNumber())
                .userId(d.getUser().getId())
                .userName(d.getUser().getFullName())
                .channel(d.getChannel())
                .reason(d.getReason())
                .description(d.getDescription())
                .requestedResolution(d.getRequestedResolution())
                .claimAmount(d.getClaimAmount())
                .status(d.getStatus())
                .vendorResponse(d.getVendorResponse())
                .resolvedAt(d.getResolvedAt() != null ? d.getResolvedAt().toString() : null)
                .createdAt(d.getCreatedAt() != null ? d.getCreatedAt().toString() : null)
                .resolution(d.getResolution())
                .adminNote(d.getAdminNote())
                .resolvedByName(d.getResolvedBy() != null ? d.getResolvedBy().getFullName() : null)
                .build();
    }

    // ── Phase 14: Vendor Ratings ──────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getVendorRatings(AppUser admin) {
        Long communityId = admin.getCommunity().getId();
        List<Object[]> rows = reviewRepository.vendorRatingSummaryByCommunity(communityId);
        return rows.stream().map(r -> Map.<String, Object>of(
                "vendorId", r[0] != null ? r[0].toString() : "",
                "averageRating", r[1] != null ? ((Number) r[1]).doubleValue() : 0.0,
                "reviewCount", r[2] != null ? ((Number) r[2]).longValue() : 0L
        )).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getVendorRating(AppUser user, String targetType, String targetId) {
        Object[] row = reviewRepository.aggregateRating(targetType, targetId);
        if (row == null || row[0] == null) {
            return Map.of("targetType", targetType, "targetId", targetId,
                    "averageRating", 0.0, "reviewCount", 0L, "avgQuality", 0.0, "avgOnTime", 0.0);
        }
        return Map.of(
                "targetType", targetType,
                "targetId", targetId,
                "averageRating", row[0] != null ? ((Number) row[0]).doubleValue() : 0.0,
                "reviewCount", row[1] != null ? ((Number) row[1]).longValue() : 0L,
                "avgQuality", row[2] != null ? ((Number) row[2]).doubleValue() : 0.0,
                "avgOnTime", row[3] != null ? ((Number) row[3]).doubleValue() : 0.0
        );
    }
}
