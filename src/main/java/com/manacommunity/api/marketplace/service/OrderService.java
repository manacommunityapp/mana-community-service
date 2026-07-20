package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.marketplace.dto.OrderRequest;
import com.manacommunity.api.marketplace.dto.OrderResponse;
import com.manacommunity.api.marketplace.entity.Listing;
import com.manacommunity.api.marketplace.entity.ListingImage;
import com.manacommunity.api.marketplace.entity.MarketplaceOrder;
import com.manacommunity.api.marketplace.entity.OrderItem;
import com.manacommunity.api.marketplace.repository.ListingRepository;
import com.manacommunity.api.marketplace.repository.MarketplaceOrderRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final MarketplaceOrderRepository orderRepo;
    private final ListingRepository listingRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(Long buyerId, Pageable pageable) {
        return orderRepo.findByBuyerIdOrderByCreatedAtDesc(buyerId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getSellerOrders(Long sellerId, Pageable pageable) {
        return orderRepo.findBySellerIdOrderByCreatedAtDesc(sellerId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        return toResponse(orderRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id)));
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest req, AppUser buyer, Community community) {
        Listing listing = listingRepo.findById(req.getListingId())
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + req.getListingId()));

        if (listing.getStatus() != Listing.ListingStatus.ACTIVE) {
            throw new IllegalArgumentException("Listing is not active: " + req.getListingId());
        }

        if (listing.getSeller().getId().equals(buyer.getId())) {
            throw new IllegalArgumentException("You cannot order your own listing");
        }

        String orderNumber = "ORD-" + System.currentTimeMillis();
        int quantity = Math.max(req.getQuantity(), 1);
        BigDecimal totalAmount = listing.getPrice().multiply(BigDecimal.valueOf(quantity));

        MarketplaceOrder order = MarketplaceOrder.builder()
                .orderNumber(orderNumber)
                .buyer(buyer)
                .seller(listing.getSeller())
                .community(community)
                .totalAmount(totalAmount)
                .notes(req.getNotes())
                .deliveryAddress(req.getDeliveryAddress())
                .items(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .listing(listing)
                .quantity(quantity)
                .unitPrice(listing.getPrice())
                .build();
        order.getItems().add(item);

        MarketplaceOrder saved = orderRepo.save(order);
        auditService.record(AuditAction.PRODUCT_CREATED, AuditModule.MARKETPLACE,
                "Order", String.valueOf(saved.getId()));
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, String status, Long userId) {
        MarketplaceOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!order.getBuyer().getId().equals(userId) && !order.getSeller().getId().equals(userId)) {
            throw new IllegalArgumentException("You are not authorized to update this order");
        }

        MarketplaceOrder.OrderStatus currentStatus = order.getStatus();
        if (currentStatus == MarketplaceOrder.OrderStatus.COMPLETED
                || currentStatus == MarketplaceOrder.OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot update order in " + currentStatus + " status");
        }

        String oldStatus = currentStatus.name();
        MarketplaceOrder.OrderStatus newStatus = MarketplaceOrder.OrderStatus.valueOf(status);
        order.setStatus(newStatus);

        MarketplaceOrder saved = orderRepo.save(order);
        auditService.record(AuditAction.PRODUCT_UPDATED, AuditModule.MARKETPLACE,
                "Order", String.valueOf(saved.getId()), oldStatus, newStatus.name());
        return toResponse(saved);
    }

    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        MarketplaceOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!order.getBuyer().getId().equals(userId) && !order.getSeller().getId().equals(userId)) {
            throw new IllegalArgumentException("You are not authorized to cancel this order");
        }

        MarketplaceOrder.OrderStatus currentStatus = order.getStatus();
        if (currentStatus != MarketplaceOrder.OrderStatus.PENDING
                && currentStatus != MarketplaceOrder.OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("Order can only be cancelled when PENDING or CONFIRMED");
        }

        String oldStatus = currentStatus.name();
        order.setStatus(MarketplaceOrder.OrderStatus.CANCELLED);
        orderRepo.save(order);
        auditService.record(AuditAction.PRODUCT_UPDATED, AuditModule.MARKETPLACE,
                "Order", String.valueOf(orderId), oldStatus, "CANCELLED");
    }

    private OrderResponse toResponse(MarketplaceOrder o) {
        AppUser b = o.getBuyer();
        AppUser s = o.getSeller();
        return OrderResponse.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .buyer(OrderResponse.OrderBuyerRef.builder()
                        .id(b.getId())
                        .fullName(b.getFullName())
                        .build())
                .seller(OrderResponse.OrderSellerRef.builder()
                        .id(s.getId())
                        .fullName(s.getFullName())
                        .verified("APPROVED".equalsIgnoreCase(s.getKycStatus()))
                        .build())
                .status(o.getStatus().name())
                .totalAmount(o.getTotalAmount())
                .notes(o.getNotes())
                .deliveryAddress(o.getDeliveryAddress())
                .items(o.getItems() != null
                        ? o.getItems().stream().map(this::toItemResponse).toList()
                        : List.of())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }

    private OrderResponse.OrderItemResponse toItemResponse(OrderItem item) {
        Listing listing = item.getListing();
        String imageUrl = null;
        if (listing.getImages() != null && !listing.getImages().isEmpty()) {
            imageUrl = listing.getImages().stream()
                    .sorted((a, bImg) -> Integer.compare(a.getSortOrder(), bImg.getSortOrder()))
                    .map(ListingImage::getUrl)
                    .findFirst()
                    .orElse(null);
        }
        return OrderResponse.OrderItemResponse.builder()
                .id(item.getId())
                .listingId(listing.getId())
                .listingTitle(listing.getTitle())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .imageUrl(imageUrl)
                .build();
    }
}
