package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.service.ExpoPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sends push notifications for commerce order lifecycle events.
 * All methods are @Async and swallow exceptions — push failures must never
 * block or roll back the primary order operation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommerceNotificationService {

    private static final String CHANNEL = "default";

    private final ExpoPushService pushService;

    @Async
    public void notifyOrderConfirmed(CommerceOrder order) {
        if (order.getBuyer() == null) return;
        try {
            pushService.sendToUser(
                    order.getBuyer().getId(),
                    "Order Confirmed ✅",
                    "Your order " + order.getOrderNumber() + " has been confirmed.",
                    Map.of("type", "COMMERCE_ORDER", "orderNumber", order.getOrderNumber(), "status", "CONFIRMED"),
                    CHANNEL);
        } catch (Exception e) {
            log.warn("Push failed for order confirmed {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    @Async
    public void notifyPaymentReceived(CommerceOrder order) {
        if (order.getBuyer() == null) return;
        try {
            pushService.sendToUser(
                    order.getBuyer().getId(),
                    "Payment Received 💳",
                    "Payment for order " + order.getOrderNumber() + " has been received.",
                    Map.of("type", "COMMERCE_ORDER", "orderNumber", order.getOrderNumber(), "status", "PAYMENT_AUTHORIZED"),
                    CHANNEL);
        } catch (Exception e) {
            log.warn("Push failed for payment received {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    @Async
    public void notifyOrderProcessing(CommerceOrder order) {
        if (order.getBuyer() == null) return;
        try {
            pushService.sendToUser(
                    order.getBuyer().getId(),
                    "Order Being Prepared 🛒",
                    "The vendor is preparing your order " + order.getOrderNumber() + ".",
                    Map.of("type", "COMMERCE_ORDER", "orderNumber", order.getOrderNumber(), "status", "PROCESSING"),
                    CHANNEL);
        } catch (Exception e) {
            log.warn("Push failed for order processing {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    @Async
    public void notifyOrderReady(CommerceOrder order) {
        if (order.getBuyer() == null) return;
        try {
            pushService.sendToUser(
                    order.getBuyer().getId(),
                    "Order Ready for Pickup 📦",
                    "Your order " + order.getOrderNumber() + " is ready. Please collect it.",
                    Map.of("type", "COMMERCE_ORDER", "orderNumber", order.getOrderNumber(), "status", "READY_FOR_PICKUP"),
                    CHANNEL);
        } catch (Exception e) {
            log.warn("Push failed for order ready {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    @Async
    public void notifyOrderOutForDelivery(CommerceOrder order) {
        if (order.getBuyer() == null) return;
        try {
            String eta = order.getEstimatedDeliveryDate() != null
                    ? " · ETA: " + order.getEstimatedDeliveryDate() : "";
            pushService.sendToUser(
                    order.getBuyer().getId(),
                    "Out for Delivery 🚚",
                    "Your order " + order.getOrderNumber() + " is on the way!" + eta,
                    Map.of("type", "COMMERCE_ORDER", "orderNumber", order.getOrderNumber(), "status", "OUT_FOR_DELIVERY"),
                    CHANNEL);
        } catch (Exception e) {
            log.warn("Push failed for order out for delivery {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    @Async
    public void notifyOrderDelivered(CommerceOrder order) {
        if (order.getBuyer() == null) return;
        try {
            pushService.sendToUser(
                    order.getBuyer().getId(),
                    "Order Delivered 🎉",
                    "Your order " + order.getOrderNumber() + " has been delivered. Please verify with OTP.",
                    Map.of("type", "COMMERCE_ORDER", "orderNumber", order.getOrderNumber(), "status", "DELIVERED"),
                    CHANNEL);
        } catch (Exception e) {
            log.warn("Push failed for order delivered {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    @Async
    public void notifyOrderCompleted(CommerceOrder order) {
        if (order.getBuyer() == null) return;
        try {
            pushService.sendToUser(
                    order.getBuyer().getId(),
                    "Order Completed ⭐",
                    "Order " + order.getOrderNumber() + " is complete. Rate your experience!",
                    Map.of("type", "COMMERCE_ORDER", "orderNumber", order.getOrderNumber(), "status", "COMPLETED"),
                    CHANNEL);
        } catch (Exception e) {
            log.warn("Push failed for order completed {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    @Async
    public void notifyOrderCancelled(CommerceOrder order) {
        if (order.getBuyer() == null) return;
        try {
            pushService.sendToUser(
                    order.getBuyer().getId(),
                    "Order Cancelled ❌",
                    "Your order " + order.getOrderNumber() + " has been cancelled.",
                    Map.of("type", "COMMERCE_ORDER", "orderNumber", order.getOrderNumber(), "status", "CANCELLED"),
                    CHANNEL);
        } catch (Exception e) {
            log.warn("Push failed for order cancelled {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    /** Notify the vendor/seller when a new order arrives. */
    @Async
    public void notifyVendorNewOrder(CommerceOrder order) {
        if (order.getSeller() == null) return;
        try {
            pushService.sendToUser(
                    order.getSeller().getId(),
                    "New Order Received 🛍️",
                    "Order " + order.getOrderNumber() + " from "
                            + (order.getBuyer() != null ? order.getBuyer().getFullName() : "a customer"),
                    Map.of("type", "COMMERCE_VENDOR_ORDER", "orderNumber", order.getOrderNumber()),
                    CHANNEL);
        } catch (Exception e) {
            log.warn("Push failed for vendor new order {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }
}
