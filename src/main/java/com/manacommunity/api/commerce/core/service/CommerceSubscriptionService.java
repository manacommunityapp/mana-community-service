package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.dto.CommerceSubscriptionDto;
import com.manacommunity.api.commerce.core.dto.CommerceSubscriptionRequest;
import com.manacommunity.api.commerce.core.model.*;
import com.manacommunity.api.commerce.core.repository.CommerceOrderItemRepository;
import com.manacommunity.api.commerce.core.repository.CommerceOrderRepository;
import com.manacommunity.api.commerce.core.repository.CommerceProductRepository;
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
import java.security.SecureRandom;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommerceSubscriptionService {

    /**
     * Handover OTPs are a bearer credential for physically collecting goods, so
     * they MUST come from a CSPRNG. java.util.Random is a predictable 48-bit LCG.
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CommerceSubscriptionRepository subscriptionRepository;
    private final CommerceOrderRepository orderRepository;
    private final CommerceOrderItemRepository orderItemRepository;
    private final CommerceProductRepository productRepository;
    private final AppUserRepository userRepository;

    // ── Customer operations ───────────────────────────────────────────────────

    /**
     * Creates a recurring subscription.
     *
     * <p>The price is resolved from the product catalogue server-side;
     * {@code req.getUnitPrice()} is ignored. A tampered price here would not just
     * underpay once, it would set the price for every future delivery, so the same
     * rule as the one-off checkout in {@code CommerceCoreServiceImpl#checkout}
     * applies: {@code discountPrice ?? basePrice}, and a product that cannot be
     * resolved or has no usable price is rejected.
     */
    @Transactional
    public CommerceSubscriptionDto createSubscription(AppUser customer, CommerceSubscriptionRequest req) {
        LocalDate startDate = req.getStartDate() != null
                ? LocalDate.parse(req.getStartDate()) : LocalDate.now();

        AppUser seller = req.getSellerId() != null
                ? userRepository.findById(req.getSellerId()).orElse(null) : null;

        int quantity = req.getQuantity() != null ? req.getQuantity() : 1;
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        CommerceProduct product = resolveProduct(req.getProductId(), req.getSku());
        // Enforce catalog price server-side; ignore client-tampered unitPrice
        BigDecimal unitPrice = catalogPrice(product);
        if (req.getUnitPrice() != null && req.getUnitPrice().compareTo(unitPrice) != 0) {
            log.warn("Ignoring client unitPrice ₹{} for subscription on SKU {}; catalog price is ₹{} (userId={})",
                    req.getUnitPrice(), product.getSku(), unitPrice, customer.getId());
        }

        CommerceSubscription sub = CommerceSubscription.builder()
                .subscriptionCode("SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .community(customer.getCommunity())
                .customerUser(customer)
                .productId(product.getId())
                .productName(product.getTitle() != null ? product.getTitle()
                        : (req.getProductName() != null ? req.getProductName() : "Subscription Item"))
                .sku(product.getSku())
                .seller(seller)
                .sellerName(req.getSellerName() != null ? req.getSellerName()
                        : (seller != null ? seller.getFullName() : null))
                .unitPrice(unitPrice)
                .quantity(quantity)
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

    /**
     * Generates one order for a due subscription.
     *
     * <p>Pricing rule: each run uses the price <strong>captured when the subscription
     * was created</strong> ({@code sub.getUnitPrice()}), not the current catalogue
     * price. A recurring debit is a standing mandate the resident agreed to at a known
     * figure; re-reading the catalogue would let a seller raise what is auto-charged
     * every week without the resident consenting, and this model has no agreed price
     * cap or price-change notification to hang that on. A price rise therefore applies
     * only to new subscriptions; residents can cancel and re-subscribe at the new price.
     *
     * <p>The captured price is still re-validated here, because rows created before
     * the price was enforced server-side may carry a client-supplied figure: a
     * non-positive price aborts the run (no free orders), and drift from the current
     * catalogue price is logged so it can be audited.
     */
    private void createOrderFromSubscription(CommerceSubscription sub) {
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(900000) + 100000);

        BigDecimal unitPrice = sub.getUnitPrice();
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Subscription " + sub.getSubscriptionCode()
                    + " has no usable unit price (₹" + unitPrice + "); refusing to generate an order.");
        }
        logCatalogDrift(sub, unitPrice);
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

    /**
     * Resolves the catalogue product for a subscription request, by SKU first and then
     * by id — the same order {@code CommerceCoreServiceImpl#checkout} uses.
     */
    private CommerceProduct resolveProduct(Long productId, String sku) {
        CommerceProduct product = null;
        if (sku != null && !sku.isBlank()) {
            product = productRepository.findBySku(sku).orElse(null);
        }
        if (product == null && productId != null) {
            product = productRepository.findById(productId).orElse(null);
        }
        if (product == null) {
            throw new IllegalArgumentException(
                    "Product not found in catalog for subscription (sku=" + sku + ", productId=" + productId + ").");
        }
        if (!product.isActive()) {
            throw new IllegalStateException(
                    "Product is no longer active or available: " + product.getTitle());
        }
        return product;
    }

    /** Same rule as the one-off checkout: discountPrice when set, else basePrice. */
    private BigDecimal catalogPrice(CommerceProduct product) {
        BigDecimal unitPrice = product.getDiscountPrice() != null
                ? product.getDiscountPrice() : product.getBasePrice();
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "Invalid product price configuration for SKU: " + product.getSku());
        }
        return unitPrice;
    }

    /**
     * Logs when a subscription's captured price no longer matches the catalogue.
     * Expected after a legitimate price change; a captured price far BELOW the
     * catalogue on an old row may be a pre-enforcement, client-set price.
     */
    private void logCatalogDrift(CommerceSubscription sub, BigDecimal capturedPrice) {
        try {
            CommerceProduct product = null;
            if (sub.getSku() != null && !sub.getSku().isBlank()) {
                product = productRepository.findBySku(sub.getSku()).orElse(null);
            }
            if (product == null && sub.getProductId() != null) {
                product = productRepository.findById(sub.getProductId()).orElse(null);
            }
            if (product == null) {
                log.warn("Subscription {} references a product no longer in the catalog (sku={}, productId={}); "
                                + "charging the captured price ₹{}",
                        sub.getSubscriptionCode(), sub.getSku(), sub.getProductId(), capturedPrice);
                return;
            }
            BigDecimal current = product.getDiscountPrice() != null
                    ? product.getDiscountPrice() : product.getBasePrice();
            if (current != null && current.compareTo(capturedPrice) != 0) {
                log.warn("Subscription {} price drift: captured ₹{} vs catalog ₹{} for SKU {} — "
                                + "charging the captured price (mandate amount).",
                        sub.getSubscriptionCode(), capturedPrice, current, product.getSku());
            }
        } catch (Exception e) {
            log.warn("Could not compare subscription {} price against the catalog: {}",
                    sub.getSubscriptionCode(), e.getMessage());
        }
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
