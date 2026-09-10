package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.marketplace.dto.MarketHandoverPassDto;
import com.manacommunity.api.marketplace.dto.MarketOrderRequest;
import com.manacommunity.api.marketplace.dto.MarketOrderResponse;
import com.manacommunity.api.marketplace.entity.MarketListing;
import com.manacommunity.api.marketplace.entity.MarketOrder;
import com.manacommunity.api.marketplace.entity.MarketOrderItem;
import com.manacommunity.api.marketplace.repository.MarketListingRepository;
import com.manacommunity.api.marketplace.repository.MarketOrderItemRepository;
import com.manacommunity.api.marketplace.repository.MarketOrderRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketOrderService {

    private final MarketOrderRepository orderRepository;
    private final MarketOrderItemRepository orderItemRepository;
    private final MarketListingRepository listingRepository;
    private final AppUserRepository userRepository;
    private final MarketHandoverSecurityService handoverSecurityService;

    public Page<MarketOrderResponse> getMyPurchases(Long buyerId, Pageable pageable) {
        return orderRepository.findByBuyerId(buyerId, pageable).map(this::toResponse);
    }

    public Page<MarketOrderResponse> getMySales(Long sellerId, Pageable pageable) {
        return orderRepository.findBySellerId(sellerId, pageable).map(this::toResponse);
    }

    public MarketOrderResponse getById(Long id) {
        return orderRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    @Transactional
    public MarketOrderResponse createOrder(MarketOrderRequest req, AppUser buyer, Community community) {
        AppUser seller = userRepository.findById(req.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + req.getSellerId()));

        String orderNumber = "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        BigDecimal subtotal = BigDecimal.ZERO;

        MarketOrder order = MarketOrder.builder()
                .orderNumber(orderNumber)
                .buyer(buyer)
                .seller(seller)
                .community(community)
                .status(MarketOrder.OrderStatus.CONFIRMED)
                .subtotalAmount(BigDecimal.ZERO)
                .discountAmount(req.getCouponCode() != null ? BigDecimal.valueOf(50) : BigDecimal.ZERO)
                .deliveryFee(req.getDeliveryFee() != null ? req.getDeliveryFee() : BigDecimal.ZERO)
                .totalAmount(req.getTotalAmount())
                .couponCode(req.getCouponCode())
                .paymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : MarketOrder.PaymentMethod.UPI)
                .paymentStatus(MarketOrder.PaymentStatus.PAID)
                .deliveryMode(req.getDeliveryMode() != null ? req.getDeliveryMode() : MarketOrder.DeliveryMode.DOORSTEP)
                .deliveryAddress(req.getDeliveryAddress())
                .notes(req.getNotes())
                .items(new ArrayList<>())
                .build();

        MarketOrder savedOrder = orderRepository.save(order);

        for (MarketOrderRequest.OrderItemRequest itemReq : req.getItems()) {
            MarketListing listing = listingRepository.findById(itemReq.getListingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + itemReq.getListingId()));

            BigDecimal total = itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(total);

            MarketOrderItem item = MarketOrderItem.builder()
                    .order(savedOrder)
                    .listing(listing)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .totalPrice(total)
                    .build();

            savedOrder.getItems().add(orderItemRepository.save(item));
        }

        savedOrder.setSubtotalAmount(subtotal);
        savedOrder = orderRepository.save(savedOrder);

        // Generate Security Gate Handover Pass
        MarketHandoverPassDto passDto = handoverSecurityService.generatePassForOrder(savedOrder);

        MarketOrderResponse response = toResponse(savedOrder);
        response.setPass(passDto);
        return response;
    }

    @Transactional
    public MarketOrderResponse updateStatus(Long orderId, String statusStr) {
        MarketOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        MarketOrder.OrderStatus status = MarketOrder.OrderStatus.valueOf(statusStr.toUpperCase());
        order.setStatus(status);

        return toResponse(orderRepository.save(order));
    }

    public MarketOrderResponse toResponse(MarketOrder o) {
        List<MarketOrderResponse.OrderItemResponse> itemResponses = o.getItems() != null
                ? o.getItems().stream().map(i -> {
            String img = (i.getListing().getImages() != null && !i.getListing().getImages().isEmpty())
                    ? i.getListing().getImages().get(0).getUrl() : null;
            return MarketOrderResponse.OrderItemResponse.builder()
                    .id(i.getId())
                    .listingId(i.getListing().getId())
                    .listingTitle(i.getListing().getTitle())
                    .listingCategory(i.getListing().getCategory())
                    .imageUrl(img)
                    .quantity(i.getQuantity())
                    .unitPrice(i.getUnitPrice())
                    .totalPrice(i.getTotalPrice())
                    .build();
        }).collect(Collectors.toList())
                : List.of();

        return MarketOrderResponse.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .buyerId(o.getBuyer().getId())
                .buyerName(o.getBuyer().getFullName() != null ? o.getBuyer().getFullName() : o.getBuyer().getUsername())
                .sellerId(o.getSeller().getId())
                .sellerName(o.getSeller().getFullName() != null ? o.getSeller().getFullName() : o.getSeller().getUsername())
                .communityId(o.getCommunity() != null ? o.getCommunity().getId() : null)
                .status(o.getStatus())
                .subtotalAmount(o.getSubtotalAmount())
                .discountAmount(o.getDiscountAmount())
                .deliveryFee(o.getDeliveryFee())
                .totalAmount(o.getTotalAmount())
                .couponCode(o.getCouponCode())
                .paymentMethod(o.getPaymentMethod())
                .paymentStatus(o.getPaymentStatus())
                .deliveryMode(o.getDeliveryMode())
                .deliveryAddress(o.getDeliveryAddress())
                .notes(o.getNotes())
                .items(itemResponses)
                .pass(handoverSecurityService.toDto(o.getHandoverPass()))
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
