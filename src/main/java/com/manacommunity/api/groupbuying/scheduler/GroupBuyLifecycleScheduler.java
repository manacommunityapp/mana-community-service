package com.manacommunity.api.groupbuying.scheduler;

import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.model.CommerceOrderStatus;
import com.manacommunity.api.commerce.core.repository.CommerceOrderRepository;
import com.manacommunity.api.groupbuying.model.*;
import com.manacommunity.api.groupbuying.repository.GroupBuyOrderRepository;
import com.manacommunity.api.groupbuying.repository.GroupDealRepository;
import com.manacommunity.api.payments.service.CashfreePaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupBuyLifecycleScheduler {

    private final GroupDealRepository dealRepository;
    private final GroupBuyOrderRepository orderRepository;
    private final CommerceOrderRepository commerceOrderRepository;
    private final ObjectProvider<CashfreePaymentService> cashfreePaymentService;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void runLifecycleJobs() {
        releaseExpiredReservations();
        processEndedDeals();
    }

    private void releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<GroupBuyOrder> expiredOrders = orderRepository.findByStatusAndReservedUntilBefore(
                OrderStatus.PENDING_PAYMENT, now);

        if (expiredOrders.isEmpty()) return;

        log.info("GroupBuyLifecycleScheduler: Releasing {} expired order reservation(s)", expiredOrders.size());

        for (GroupBuyOrder ord : expiredOrders) {
            try {
                ord.setStatus(OrderStatus.CANCELLED);
                ord.setPaymentStatus("EXPIRED");
                ord.setRefundReason("Payment window expired (15m hold released)");
                orderRepository.save(ord);

                if (ord.getCommerceOrder() != null) {
                    CommerceOrder co = ord.getCommerceOrder();
                    co.setStatus(CommerceOrderStatus.CANCELLED);
                    co.setPaymentStatus("EXPIRED");
                    co.setCancellationReason("Payment window expired (15m hold released)");
                    commerceOrderRepository.save(co);
                }

                if (ord.getDeal() != null) {
                    dealRepository.findByIdForUpdate(ord.getDeal().getId()).ifPresent(deal -> {
                        int currentRes = deal.getReservedQty() != null ? deal.getReservedQty() : 0;
                        deal.setReservedQty(Math.max(0, currentRes - ord.getQuantity()));
                        dealRepository.save(deal);
                    });
                }
            } catch (Exception e) {
                log.error("Failed to release expired reservation for order {}: {}", ord.getOrderNumber(), e.getMessage());
            }
        }
    }

    private void processEndedDeals() {
        LocalDateTime now = LocalDateTime.now();
        List<GroupDeal> endedDeals = dealRepository.findByDealStatusInAndDealEndsAtBefore(
                List.of(DealStatus.OPEN), now);

        for (GroupDeal deal : endedDeals) {
            try {
                dealRepository.findByIdForUpdate(deal.getId()).ifPresent(lockedDeal -> {
                    if (lockedDeal.getDealStatus() != DealStatus.OPEN) return;

                    int committed = lockedDeal.getCommittedQty() != null ? lockedDeal.getCommittedQty() : 0;
                    int target = lockedDeal.getTargetQty() != null ? lockedDeal.getTargetQty() : 1;

                    if (committed >= target) {
                        lockedDeal.setDealStatus(DealStatus.TARGET_REACHED);
                        lockedDeal.setPriceLockedAt(now);
                        dealRepository.save(lockedDeal);
                        log.info("GroupBuyLifecycleScheduler: Deal '{}' TARGET_REACHED ({}/{}).",
                                lockedDeal.getTitle(), committed, target);
                    } else {
                        if (lockedDeal.getPricingModel() == PricingModel.THRESHOLD) {
                            lockedDeal.setDealStatus(DealStatus.TARGET_NOT_REACHED);
                            dealRepository.save(lockedDeal);
                            log.warn("GroupBuyLifecycleScheduler: Deal '{}' TARGET_NOT_REACHED ({}/{}). Initiating refunds.",
                                    lockedDeal.getTitle(), committed, target);

                            refundUnsuccessfulDealOrders(lockedDeal);
                        } else {
                            lockedDeal.setDealStatus(DealStatus.ORDER_PLACED);
                            dealRepository.save(lockedDeal);
                            log.info("GroupBuyLifecycleScheduler: Guaranteed deal '{}' marked ORDER_PLACED ({}/{}).",
                                    lockedDeal.getTitle(), committed, target);
                        }
                    }
                });
            } catch (Exception e) {
                log.error("Failed to process lifecycle for deal {}: {}", deal.getId(), e.getMessage());
            }
        }
    }

    private void refundUnsuccessfulDealOrders(GroupDeal deal) {
        List<GroupBuyOrder> orders = orderRepository.findByDealId(deal.getId());
        CashfreePaymentService cfService = cashfreePaymentService.getIfAvailable();

        for (GroupBuyOrder ord : orders) {
            if (ord.getStatus() == OrderStatus.CONFIRMED && "PAID".equals(ord.getPaymentStatus())) {
                ord.setStatus(OrderStatus.REFUNDED);
                ord.setPaymentStatus("REFUNDED");
                ord.setRefundAmount(ord.getTotalAmount());
                ord.setRefundReason("Deal target not reached by cutoff time");
                ord.setRefundTimestamp(LocalDateTime.now());
                ord.setEscrowHoldAmount(java.math.BigDecimal.ZERO);
                orderRepository.save(ord);

                if (ord.getCommerceOrder() != null) {
                    CommerceOrder co = ord.getCommerceOrder();
                    co.setStatus(CommerceOrderStatus.REFUNDED);
                    co.setPaymentStatus("REFUNDED");
                    co.setCancellationReason("Deal target not reached by cutoff time");
                    commerceOrderRepository.save(co);

                    if (cfService != null && ord.getCommerceOrder().getOrderNumber() != null) {
                        try {
                            cfService.initiateRefund(co.getOrderNumber(), ord.getTotalAmount(), "Campaign target not reached");
                        } catch (Exception e) {
                            log.warn("Auto-refund call to Cashfree for order {} skipped/failed: {}", co.getOrderNumber(), e.getMessage());
                        }
                    }
                }
            }
        }
        deal.setDealStatus(DealStatus.REFUNDED);
        dealRepository.save(deal);
    }
}
