package com.manacommunity.api.food.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.food.entity.*;
import com.manacommunity.api.food.repository.*;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodOrderService {

    private final FoodOrderRepository orderRepo;
    private final FoodOrderItemRepository orderItemRepo;
    private final FoodOrderTrackingRepository trackingRepo;
    private final FoodOrderRatingRepository ratingRepo;
    private final FoodGroupOrderRepository groupOrderRepo;
    private final FoodGroupOrderParticipantRepository participantRepo;
    private final FoodOrderRefundRepository refundRepo;

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> placeOrder(Map<String, Object> request, AppUser user, Community community) {
        String orderNumber = "ORD-" + System.currentTimeMillis() + "-" + String.format("%04d", new Random().nextInt(10000));

        FoodOrder order = FoodOrder.builder()
                .orderNumber(orderNumber)
                .user(user)
                .status(FoodOrder.OrderStatus.PLACED)
                .paymentStatus(FoodOrder.PaymentStatus.PENDING)
                .placedAt(LocalDateTime.now())
                .community(community)
                .build();

        if (request.containsKey("providerType")) {
            order.setProviderType(FoodOrder.ProviderType.valueOf((String) request.get("providerType")));
        }
        if (request.containsKey("providerId")) {
            order.setProviderId(Long.valueOf(request.get("providerId").toString()));
        }
        if (request.containsKey("orderType")) {
            order.setOrderType(FoodOrder.OrderType.valueOf((String) request.get("orderType")));
        }
        if (request.containsKey("subtotal")) {
            order.setSubtotal(new BigDecimal(request.get("subtotal").toString()));
        }
        if (request.containsKey("tax")) {
            order.setTax(new BigDecimal(request.get("tax").toString()));
        }
        if (request.containsKey("deliveryFee")) {
            order.setDeliveryFee(new BigDecimal(request.get("deliveryFee").toString()));
        }
        if (request.containsKey("discount")) {
            order.setDiscount(new BigDecimal(request.get("discount").toString()));
        }
        if (request.containsKey("totalAmount")) {
            order.setTotalAmount(new BigDecimal(request.get("totalAmount").toString()));
        }
        if (request.containsKey("paymentMethod")) {
            order.setPaymentMethod((String) request.get("paymentMethod"));
        }
        if (request.containsKey("deliveryAddress")) {
            order.setDeliveryAddress((String) request.get("deliveryAddress"));
        }
        if (request.containsKey("deliveryLatitude")) {
            order.setDeliveryLatitude(new BigDecimal(request.get("deliveryLatitude").toString()));
        }
        if (request.containsKey("deliveryLongitude")) {
            order.setDeliveryLongitude(new BigDecimal(request.get("deliveryLongitude").toString()));
        }
        if (request.containsKey("deliveryInstructions")) {
            order.setDeliveryInstructions((String) request.get("deliveryInstructions"));
        }
        if (request.containsKey("scheduledFor")) {
            order.setScheduledFor(LocalDateTime.parse((String) request.get("scheduledFor")));
        }
        if (request.containsKey("isGift")) {
            order.setIsGift((Boolean) request.get("isGift"));
        }
        if (request.containsKey("giftMessage")) {
            order.setGiftMessage((String) request.get("giftMessage"));
        }
        if (request.containsKey("isGroupOrder")) {
            order.setIsGroupOrder((Boolean) request.get("isGroupOrder"));
        }
        if (request.containsKey("groupOrderId")) {
            order.setGroupOrderId(Long.valueOf(request.get("groupOrderId").toString()));
        }

        FoodOrder savedOrder = orderRepo.save(order);

        if (request.containsKey("items")) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
            for (Map<String, Object> itemReq : items) {
                FoodOrderItem orderItem = FoodOrderItem.builder()
                        .order(savedOrder)
                        .itemId(itemReq.containsKey("itemId") ? Long.valueOf(itemReq.get("itemId").toString()) : null)
                        .itemName((String) itemReq.get("itemName"))
                        .quantity(itemReq.containsKey("quantity") ? (Integer) itemReq.get("quantity") : 1)
                        .unitPrice(itemReq.containsKey("unitPrice") ? new BigDecimal(itemReq.get("unitPrice").toString()) : null)
                        .totalPrice(itemReq.containsKey("totalPrice") ? new BigDecimal(itemReq.get("totalPrice").toString()) : null)
                        .variantName((String) itemReq.get("variantName"))
                        .specialInstructions((String) itemReq.get("specialInstructions"))
                        .isVeg(itemReq.containsKey("isVeg") ? (Boolean) itemReq.get("isVeg") : null)
                        .build();
                orderItemRepo.save(orderItem);
            }
        }

        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getOrders(Long userId, Long communityId, Pageable pageable) {
        return orderRepo.findByUserIdAndCommunityId(userId, communityId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOrderById(Long id, Long communityId) {
        FoodOrder order = orderRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOrderByNumber(String orderNumber) {
        FoodOrder order = orderRepo.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getProviderOrders(String providerType, Long providerId, Long communityId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            FoodOrder.OrderStatus orderStatus = FoodOrder.OrderStatus.valueOf(status);
            return orderRepo.findByStatusAndCommunityId(orderStatus, communityId, pageable)
                    .map(this::toResponse);
        }
        FoodOrder.ProviderType type = FoodOrder.ProviderType.valueOf(providerType);
        return orderRepo.findByProviderTypeAndProviderIdAndCommunityId(type, providerId, communityId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public Map<String, Object> updateOrderStatus(Long orderId, String status, Long communityId) {
        FoodOrder order = orderRepo.findByIdAndCommunityId(orderId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        FoodOrder.OrderStatus newStatus = FoodOrder.OrderStatus.valueOf(status);
        order.setStatus(newStatus);

        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case CONFIRMED:
                order.setConfirmedAt(now);
                break;
            case PREPARING:
                order.setPreparingAt(now);
                break;
            case READY:
                order.setReadyAt(now);
                break;
            case DELIVERED:
                order.setDeliveredAt(now);
                order.setActualDelivery(now);
                break;
            case CANCELLED:
                order.setCancelledAt(now);
                break;
            default:
                break;
        }

        FoodOrder saved = orderRepo.save(order);
        return toResponse(saved);
    }

    @Transactional
    public Map<String, Object> cancelOrder(Long orderId, String reason, Long communityId) {
        FoodOrder order = orderRepo.findByIdAndCommunityId(orderId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        order.setStatus(FoodOrder.OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason);

        FoodOrder saved = orderRepo.save(order);
        return toResponse(saved);
    }

    @Transactional
    public Map<String, Object> rateOrder(Long orderId, Map<String, Object> rating, AppUser user) {
        FoodOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        FoodOrderRating orderRating = FoodOrderRating.builder()
                .order(order)
                .user(user)
                .overallRating(rating.containsKey("overallRating") ? (Integer) rating.get("overallRating") : null)
                .foodRating(rating.containsKey("foodRating") ? (Integer) rating.get("foodRating") : null)
                .deliveryRating(rating.containsKey("deliveryRating") ? (Integer) rating.get("deliveryRating") : null)
                .packagingRating(rating.containsKey("packagingRating") ? (Integer) rating.get("packagingRating") : null)
                .reviewText((String) rating.get("reviewText"))
                .images((String) rating.get("images"))
                .community(order.getCommunity())
                .build();

        FoodOrderRating saved = ratingRepo.save(orderRating);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("orderId", saved.getOrder().getId());
        map.put("userId", saved.getUser().getId());
        map.put("overallRating", saved.getOverallRating());
        map.put("foodRating", saved.getFoodRating());
        map.put("deliveryRating", saved.getDeliveryRating());
        map.put("packagingRating", saved.getPackagingRating());
        map.put("reviewText", saved.getReviewText());
        map.put("images", saved.getImages());
        map.put("createdAt", saved.getCreatedAt());
        map.put("updatedAt", saved.getUpdatedAt());
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOrderTracking(Long orderId) {
        List<FoodOrderTracking> trackings = trackingRepo.findByOrderIdOrderByTimestampDesc(orderId);
        return trackings.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("orderId", t.getOrder().getId());
            map.put("status", t.getStatus());
            map.put("latitude", t.getLatitude());
            map.put("longitude", t.getLongitude());
            map.put("notes", t.getNotes());
            map.put("timestamp", t.getTimestamp());
            map.put("createdAt", t.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createGroupOrder(Map<String, Object> request, AppUser user, Community community) {
        String joinCode = UUID.randomUUID().toString().substring(0, 8);

        FoodGroupOrder groupOrder = FoodGroupOrder.builder()
                .createdBy(user)
                .title((String) request.get("title"))
                .status(FoodGroupOrder.GroupOrderStatus.OPEN)
                .joinCode(joinCode)
                .community(community)
                .build();

        if (request.containsKey("providerType")) {
            groupOrder.setProviderType((String) request.get("providerType"));
        }
        if (request.containsKey("providerId")) {
            groupOrder.setProviderId(Long.valueOf(request.get("providerId").toString()));
        }
        if (request.containsKey("expiresAt")) {
            groupOrder.setExpiresAt(LocalDateTime.parse((String) request.get("expiresAt")));
        }
        if (request.containsKey("maxParticipants")) {
            groupOrder.setMaxParticipants((Integer) request.get("maxParticipants"));
        }
        if (request.containsKey("splitType")) {
            groupOrder.setSplitType(FoodGroupOrder.SplitType.valueOf((String) request.get("splitType")));
        }

        FoodGroupOrder saved = groupOrderRepo.save(groupOrder);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("createdById", saved.getCreatedBy().getId());
        map.put("title", saved.getTitle());
        map.put("status", saved.getStatus() != null ? saved.getStatus().name() : null);
        map.put("providerType", saved.getProviderType());
        map.put("providerId", saved.getProviderId());
        map.put("joinCode", saved.getJoinCode());
        map.put("expiresAt", saved.getExpiresAt());
        map.put("maxParticipants", saved.getMaxParticipants());
        map.put("splitType", saved.getSplitType() != null ? saved.getSplitType().name() : null);
        map.put("communityId", saved.getCommunity().getId());
        map.put("createdAt", saved.getCreatedAt());
        map.put("updatedAt", saved.getUpdatedAt());
        return map;
    }

    @Transactional
    public Map<String, Object> joinGroupOrder(String joinCode, AppUser user) {
        FoodGroupOrder groupOrder = groupOrderRepo.findByJoinCode(joinCode)
                .orElseThrow(() -> new ResourceNotFoundException("GroupOrder", "joinCode", joinCode));

        FoodGroupOrderParticipant participant = FoodGroupOrderParticipant.builder()
                .groupOrder(groupOrder)
                .user(user)
                .status(FoodGroupOrderParticipant.ParticipantStatus.JOINED)
                .build();

        FoodGroupOrderParticipant saved = participantRepo.save(participant);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("groupOrderId", saved.getGroupOrder().getId());
        map.put("userId", saved.getUser().getId());
        map.put("status", saved.getStatus() != null ? saved.getStatus().name() : null);
        map.put("individualTotal", saved.getIndividualTotal());
        map.put("createdAt", saved.getCreatedAt());
        map.put("updatedAt", saved.getUpdatedAt());
        return map;
    }

    @Transactional
    public Map<String, Object> requestRefund(Long orderId, Map<String, Object> request, Long communityId) {
        FoodOrder order = orderRepo.findByIdAndCommunityId(orderId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        FoodOrderRefund refund = FoodOrderRefund.builder()
                .order(order)
                .amount(request.containsKey("amount") ? new BigDecimal(request.get("amount").toString()) : order.getTotalAmount())
                .reason((String) request.get("reason"))
                .status(FoodOrderRefund.RefundStatus.REQUESTED)
                .refundMethod((String) request.get("refundMethod"))
                .community(order.getCommunity())
                .build();

        FoodOrderRefund saved = refundRepo.save(refund);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("orderId", saved.getOrder().getId());
        map.put("amount", saved.getAmount());
        map.put("reason", saved.getReason());
        map.put("status", saved.getStatus() != null ? saved.getStatus().name() : null);
        map.put("refundMethod", saved.getRefundMethod());
        map.put("transactionRef", saved.getTransactionRef());
        map.put("communityId", saved.getCommunity().getId());
        map.put("createdAt", saved.getCreatedAt());
        map.put("updatedAt", saved.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toResponse(FoodOrder order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("orderNumber", order.getOrderNumber());
        map.put("userId", order.getUser().getId());
        map.put("providerType", order.getProviderType() != null ? order.getProviderType().name() : null);
        map.put("providerId", order.getProviderId());
        map.put("orderType", order.getOrderType() != null ? order.getOrderType().name() : null);
        map.put("status", order.getStatus() != null ? order.getStatus().name() : null);
        map.put("subtotal", order.getSubtotal());
        map.put("tax", order.getTax());
        map.put("deliveryFee", order.getDeliveryFee());
        map.put("discount", order.getDiscount());
        map.put("totalAmount", order.getTotalAmount());
        map.put("paymentStatus", order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        map.put("paymentMethod", order.getPaymentMethod());
        map.put("deliveryAddress", order.getDeliveryAddress());
        map.put("deliveryLatitude", order.getDeliveryLatitude());
        map.put("deliveryLongitude", order.getDeliveryLongitude());
        map.put("deliveryInstructions", order.getDeliveryInstructions());
        map.put("estimatedDelivery", order.getEstimatedDelivery());
        map.put("actualDelivery", order.getActualDelivery());
        map.put("placedAt", order.getPlacedAt());
        map.put("confirmedAt", order.getConfirmedAt());
        map.put("preparingAt", order.getPreparingAt());
        map.put("readyAt", order.getReadyAt());
        map.put("deliveredAt", order.getDeliveredAt());
        map.put("cancelledAt", order.getCancelledAt());
        map.put("cancellationReason", order.getCancellationReason());
        map.put("isGroupOrder", order.getIsGroupOrder());
        map.put("groupOrderId", order.getGroupOrderId());
        map.put("scheduledFor", order.getScheduledFor());
        map.put("isGift", order.getIsGift());
        map.put("giftMessage", order.getGiftMessage());
        map.put("communityId", order.getCommunity() != null ? order.getCommunity().getId() : null);
        map.put("createdAt", order.getCreatedAt());
        map.put("updatedAt", order.getUpdatedAt());

        List<FoodOrderItem> items = orderItemRepo.findByOrderId(order.getId());
        map.put("items", items.stream().map(i -> {
            Map<String, Object> im = new HashMap<>();
            im.put("id", i.getId());
            im.put("itemId", i.getItemId());
            im.put("itemName", i.getItemName());
            im.put("quantity", i.getQuantity());
            im.put("unitPrice", i.getUnitPrice());
            im.put("totalPrice", i.getTotalPrice());
            im.put("variantName", i.getVariantName());
            im.put("specialInstructions", i.getSpecialInstructions());
            im.put("isVeg", i.getIsVeg());
            im.put("createdAt", i.getCreatedAt());
            return im;
        }).collect(Collectors.toList()));

        return map;
    }
}
