package com.manacommunity.api.groupbuying.service;

import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.model.CommerceOrderStatus;
import com.manacommunity.api.commerce.core.model.CommercePayment;
import com.manacommunity.api.commerce.core.repository.CommerceOrderRepository;
import com.manacommunity.api.commerce.core.repository.CommercePaymentRepository;
import com.manacommunity.api.groupbuying.model.*;
import com.manacommunity.api.groupbuying.repository.GroupBuyOrderRepository;
import com.manacommunity.api.groupbuying.repository.GroupDealRepository;
import com.manacommunity.api.transactioncore.adapter.GroupBuyingTransactionAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupBuyPaymentConfirmationService {

    private final GroupDealRepository dealRepository;
    private final GroupBuyOrderRepository orderRepository;
    private final CommerceOrderRepository commerceOrderRepository;
    private final CommercePaymentRepository commercePaymentRepository;
    @Autowired(required = false)
    private GroupBuyingTransactionAdapter transactionAdapter;

    @Transactional
    public void confirmPayment(Long commerceOrderId, String cfOrderId, String cfPaymentId, String paymentMethod, BigDecimal amount) {
        GroupBuyOrder order = orderRepository.findByCommerceOrderId(commerceOrderId)
                .orElse(null);

        if (order == null) {
            log.warn("confirmPayment: No GroupBuyOrder found for commerceOrderId={}", commerceOrderId);
            return;
        }

        if (order.getStatus() == OrderStatus.CONFIRMED && "PAID".equals(order.getPaymentStatus())) {
            log.info("confirmPayment: Order {} is already CONFIRMED and PAID, skipping.", order.getOrderNumber());
            return;
        }

        Long dealId = order.getDeal() != null ? order.getDeal().getId() : null;
        if (dealId == null) {
            log.warn("confirmPayment: Order {} has no associated deal", order.getOrderNumber());
            return;
        }

        GroupDeal deal = dealRepository.findByIdForUpdate(dealId)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found: " + dealId));

        int qty = order.getQuantity();

        // 1. Update deal reservations and committed quantities
        if (deal.getReservedQty() != null && deal.getReservedQty() > 0) {
            deal.setReservedQty(Math.max(0, deal.getReservedQty() - qty));
        }
        deal.setCommittedQty(deal.getCommittedQty() + qty);
        deal.setCurrentParticipants(deal.getCurrentParticipants() + 1);
        if (deal.getInventoryRemaining() != null) {
            deal.setInventoryRemaining(Math.max(0, deal.getInventoryRemaining() - qty));
        }

        BigDecimal prevTierPrice = deal.getCurrentTierPrice() != null ? deal.getCurrentTierPrice() : deal.getCurrentPrice();
        BigDecimal newTierPrice = prevTierPrice;

        if (deal.getPriceTiers() != null && !deal.getPriceTiers().isEmpty()) {
            for (GroupDealTier tier : deal.getPriceTiers()) {
                if (deal.getCommittedQty() >= tier.getMinQty()) {
                    newTierPrice = tier.getPrice();
                    deal.setCurrentTierPrice(tier.getPrice());
                }
            }
        }

        // Check target unlock
        if (deal.getTargetQty() != null && deal.getCommittedQty() >= deal.getTargetQty() && deal.getDealStatus() == DealStatus.OPEN) {
            deal.setDealStatus(DealStatus.TARGET_REACHED);
            deal.setPriceLockedAt(LocalDateTime.now());
            log.info("★ Target unlocked for group deal '{}' (Committed: {}/{})", deal.getTitle(), deal.getCommittedQty(), deal.getTargetQty());
        }

        dealRepository.save(deal);

        // 2. Update GroupBuyOrder
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus("PAID");
        order.setPaymentMethod(paymentMethod != null ? paymentMethod : "CASHFREE");
        order.setTransactionId(cfPaymentId != null ? cfPaymentId : (cfOrderId != null ? cfOrderId : "TXN-" + System.currentTimeMillis()));
        order.setPaymentTimestamp(LocalDateTime.now());
        order.setReservedUntil(null);
        order.setEscrowHoldAmount(order.getTotalAmount());
        orderRepository.save(order);

        // 3. Update CommerceOrder
        CommerceOrder co = order.getCommerceOrder();
        if (co == null && commerceOrderId != null) {
            co = commerceOrderRepository.findById(commerceOrderId).orElse(null);
        }
        if (co != null) {
            co.setStatus(CommerceOrderStatus.CONFIRMED);
            co.setPaymentStatus("PAID");
            co.setPaymentMethod(paymentMethod != null ? paymentMethod : "CASHFREE");
            co.setHandoverOtp(order.getDeliveryOtp());
            co.setQrToken(order.getQrToken());
            commerceOrderRepository.save(co);

            // Record CommercePayment
            try {
                CommercePayment payment = CommercePayment.builder()
                        .order(co)
                        .paymentRef(cfPaymentId != null ? cfPaymentId : "CF-" + order.getOrderNumber() + "-" + System.currentTimeMillis())
                        .gateway("CASHFREE")
                        .paymentMethod(paymentMethod != null ? paymentMethod : "UPI")
                        .amount(amount != null ? amount : order.getTotalAmount())
                        .status("CAPTURED")
                        .capturedAt(LocalDateTime.now())
                        .build();
                commercePaymentRepository.save(payment);
            } catch (Exception e) {
                log.warn("Failed to record CommercePayment for order {}: {}", order.getOrderNumber(), e.getMessage());
            }
        }

        // 4. Trigger retroactive tier refunds if unlocked
        if (newTierPrice.compareTo(prevTierPrice) < 0) {
            processTierPriceDropRefunds(deal, prevTierPrice, newTierPrice);
        }

        log.info("✓ Payment confirmed for Group Order {} (Deal: '{}', Qty: {}, Amount: ₹{})",
                order.getOrderNumber(), deal.getTitle(), qty, order.getTotalAmount());
    }

    private void processTierPriceDropRefunds(GroupDeal deal, BigDecimal oldPrice, BigDecimal newTierPrice) {
        List<GroupBuyOrder> priorOrders = orderRepository.findByDealId(deal.getId());
        BigDecimal priceDiff = oldPrice.subtract(newTierPrice);
        if (priceDiff.compareTo(BigDecimal.ZERO) <= 0) return;

        int refundedCount = 0;
        BigDecimal totalRefunded = BigDecimal.ZERO;

        for (GroupBuyOrder ord : priorOrders) {
            if (ord.getStatus() != OrderStatus.CANCELLED && ord.getStatus() != OrderStatus.REFUNDED && "PAID".equals(ord.getPaymentStatus())) {
                BigDecimal orderRefund = priceDiff.multiply(BigDecimal.valueOf(ord.getQuantity()));
                BigDecimal currentTierRefund = ord.getTierPriceRefundAmount() != null ? ord.getTierPriceRefundAmount() : BigDecimal.ZERO;
                ord.setTierPriceRefundAmount(currentTierRefund.add(orderRefund));

                BigDecimal currentSavings = ord.getSavingsAmount() != null ? ord.getSavingsAmount() : BigDecimal.ZERO;
                ord.setSavingsAmount(currentSavings.add(orderRefund));
                ord.setUnitPrice(newTierPrice);
                ord.setTotalAmount(newTierPrice.multiply(BigDecimal.valueOf(ord.getQuantity())));
                orderRepository.save(ord);

                if (ord.getCommerceOrder() != null) {
                    CommerceOrder co = ord.getCommerceOrder();
                    co.setTotalAmount(ord.getTotalAmount());
                    co.setSavingsAmount(ord.getSavingsAmount());
                    commerceOrderRepository.save(co);
                }

                if (transactionAdapter != null && ord.getTransactionId() != null) {
                    try {
                        transactionAdapter.refundTierSavingsDelta(ord.getTransactionId(), orderRefund, deal.getTitle());
                    } catch (Exception e) {
                        log.warn("Tier savings auto-refund via Transaction Core warning: {}", e.getMessage());
                    }
                }

                refundedCount++;
                totalRefunded = totalRefunded.add(orderRefund);
            }
        }
        log.info("★ Tier unlocked for deal '{}' (New Price: ₹{}) — Auto-refunded ₹{} across {} orders.",
                deal.getTitle(), newTierPrice, totalRefunded, refundedCount);
    }
}
