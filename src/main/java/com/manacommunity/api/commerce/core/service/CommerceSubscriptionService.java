package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.dto.CommerceSubscriptionDto;
import com.manacommunity.api.commerce.core.dto.CommerceSubscriptionRequest;
import com.manacommunity.api.commerce.core.model.*;
import com.manacommunity.api.commerce.core.repository.CommerceOrderItemRepository;
import com.manacommunity.api.commerce.core.repository.CommerceOrderRepository;
import com.manacommunity.api.commerce.core.repository.CommerceSubscriptionRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommerceSubscriptionService {

    private final CommerceSubscriptionRepository subscriptionRepository;
    private final CommerceOrderRepository orderRepository;
    private final CommerceOrderItemRepository orderItemRepository;
    private final AppUserRepository userRepository;

    // ── Customer operations ───────────────────────────────────────────────────

    @Transactional
    public CommerceSubscriptionDto createSubscription(AppUser customer, CommerceSubscriptionRequest req) {
        LocalDate startDate = req.getStartDate() != null
                ? LocalDate.parse(req.getStartDate()) : LocalDate.now();

        AppUser seller = req.getSellerId() != null
                ? userRepository.findById(req.getSellerId()).orElse(null) : null;

        CommerceSubscription sub = CommerceSubscription.builder()
                .subscriptionCode("SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .community(customer.getCommunity())
                .customerUser(customer)
                .productId(req.getProductId())
                .productName(req.getProductName() != null ? req.getProductName() : "Subscription Item")
                .sku(req.getSku())
                .seller(seller)
                .sellerName(req.getSellerName() != null ? req.getSellerName()
                        : (seller != null ? seller.getFullName() : null))
                .unitPrice(req.getUnitPrice() != null ? req.getUnitPrice() : BigDecimal.ZERO)
                .quantity(req.getQuantity() != null ? req.getQuantity() : 1)
                .channel(req.getChannel() != null ? req.getChannel() : "MARKETPLACE")
                .frequency(req.getFrequency() != null ? req.getFrequency().toUpperCase() : "WEEKLY")
                .status("ACTIVE")
                .nextRunDate(startDate)
                .totalCycles(req.getTotalCycles())
                .deliveryAddress(req.getDeliveryAddress())
                .notes(req.getNotes())
                .build();

        return toDto(subscriptionRepository.save(sub));
    }

    @Transactional(readOnly = true)
    public List<CommerceSubscriptionDto> getMySubscriptions(AppUser customer) {
        return subscriptionRepository.findByCustomerUserIdOrderByCreatedAtDesc(customer.getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public CommerceSubscriptionDto pauseSubscription(AppUser customer, String code) {
        CommerceSubscription sub = findAndAuthorize(customer, code);
        if (!"ACTIVE".equals(sub.getStatus())) throw new IllegalStateException("Subscription is not active.");
        sub.setStatus("PAUSED");
        sub.setUpdatedAt(LocalDateTime.now());
        return toDto(subscriptionRepository.save(sub));
    }

    @Transactional
    public CommerceSubscriptionDto resumeSubscription(AppUser customer, String code) {
        CommerceSubscription sub = findAndAuthorize(customer, code);
        if (!"PAUSED".equals(sub.getStatus())) throw new IllegalStateException("Subscription is not paused.");
        sub.setStatus("ACTIVE");
        sub.setUpdatedAt(LocalDateTime.now());
        return toDto(subscriptionRepository.save(sub));
    }

    @Transactional
    public CommerceSubscriptionDto cancelSubscription(AppUser customer, String code) {
        CommerceSubscription sub = findAndAuthorize(customer, code);
        if ("CANCELLED".equals(sub.getStatus())) throw new IllegalStateException("Already cancelled.");
        sub.setStatus("CANCELLED");
        sub.setUpdatedAt(LocalDateTime.now());
        return toDto(subscriptionRepository.save(sub));
    }

    // ── Admin operations ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CommerceSubscriptionDto> getAdminSubscriptions(AppUser admin) {
        return subscriptionRepository.findByCommunityIdOrderByCreatedAtDesc(admin.getCommunity().getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ── Scheduled auto-order creation ─────────────────────────────────────────

    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void processSubscriptions() {
        LocalDate today = LocalDate.now();
        List<CommerceSubscription> due = subscriptionRepository
                .findByStatusAndNextRunDateLessThanEqual("ACTIVE", today);

        log.info("Processing {} due subscriptions for {}", due.size(), today);

        for (CommerceSubscription sub : due) {
            try {
                createOrderFromSubscription(sub);
                sub.setLastRunDate(today);
                sub.setCompletedCycles(sub.getCompletedCycles() + 1);
                sub.setNextRunDate(advanceDate(today, sub.getFrequency()));
                if (sub.getTotalCycles() != null && sub.getCompletedCycles() >= sub.getTotalCycles()) {
                    sub.setStatus("CANCELLED");
                }
                sub.setUpdatedAt(LocalDateTime.now());
                subscriptionRepository.save(sub);
            } catch (Exception e) {
                log.error("Failed to process subscription {}: {}", sub.getSubscriptionCode(), e.getMessage());
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void createOrderFromSubscription(CommerceSubscription sub) {
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String otp = String.format("%06d", new Random().nextInt(900000) + 100000);

        BigDecimal unitPrice = sub.getUnitPrice();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(sub.getQuantity()));

        CommerceOrderItem item = CommerceOrderItem.builder()
                .productId(sub.getProductId() != null ? String.valueOf(sub.getProductId()) : null)
                .sku(sub.getSku())
                .title(sub.getProductName())
                .unitPrice(unitPrice)
                .quantity(sub.getQuantity())
                .totalPrice(subtotal)
                .build();

        CommerceOrder order = CommerceOrder.builder()
                .orderNumber(orderNumber)
                .channel(parseChannel(sub.getChannel()))
                .buyer(sub.getCustomerUser())
                .seller(sub.getSeller())
                .community(sub.getCommunity())
                .subtotalAmount(subtotal)
                .totalAmount(subtotal)
                .status(CommerceOrderStatus.CONFIRMED)
                .paymentMethod("SUBSCRIPTION")
                .paymentStatus("PENDING")
                .fulfillmentType("HOME_DELIVERY")
                .deliveryAddress(sub.getDeliveryAddress())
                .notes("Auto-generated from subscription " + sub.getSubscriptionCode())
                .handoverOtp(otp)
                .qrToken("QR-MANA-" + orderNumber)
                .build();

        CommerceOrder savedOrder = orderRepository.save(order);
        item.setOrder(savedOrder);
        orderItemRepository.save(item);
    }

    private LocalDate advanceDate(LocalDate from, String frequency) {
        return switch (frequency.toUpperCase()) {
            case "DAILY"       -> from.plusDays(1);
            case "FORTNIGHTLY" -> from.plusWeeks(2);
            case "MONTHLY"     -> from.plusMonths(1);
            default            -> from.plusWeeks(1); // WEEKLY
        };
    }

    private CommerceChannel parseChannel(String ch) {
        if (ch == null) return CommerceChannel.MARKETPLACE;
        try { return CommerceChannel.valueOf(ch.toUpperCase()); }
        catch (IllegalArgumentException e) { return CommerceChannel.MARKETPLACE; }
    }

    private CommerceSubscription findAndAuthorize(AppUser user, String code) {
        CommerceSubscription sub = subscriptionRepository.findBySubscriptionCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + code));
        if (!sub.getCustomerUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Not your subscription.");
        }
        return sub;
    }

    private CommerceSubscriptionDto toDto(CommerceSubscription s) {
        BigDecimal total = s.getUnitPrice().multiply(BigDecimal.valueOf(s.getQuantity()));
        return CommerceSubscriptionDto.builder()
                .id(s.getId())
                .subscriptionCode(s.getSubscriptionCode())
                .customerId(s.getCustomerUser().getId())
                .customerName(s.getCustomerUser().getFullName())
                .productId(s.getProductId())
                .productName(s.getProductName())
                .sku(s.getSku())
                .sellerId(s.getSeller() != null ? s.getSeller().getId() : null)
                .sellerName(s.getSellerName())
                .unitPrice(s.getUnitPrice())
                .quantity(s.getQuantity())
                .totalPerCycle(total)
                .channel(s.getChannel())
                .frequency(s.getFrequency())
                .status(s.getStatus())
                .nextRunDate(s.getNextRunDate() != null ? s.getNextRunDate().toString() : null)
                .lastRunDate(s.getLastRunDate() != null ? s.getLastRunDate().toString() : null)
                .totalCycles(s.getTotalCycles())
                .completedCycles(s.getCompletedCycles())
                .deliveryAddress(s.getDeliveryAddress())
                .notes(s.getNotes())
                .createdAt(s.getCreatedAt() != null ? s.getCreatedAt().toString() : null)
                .build();
    }
}
