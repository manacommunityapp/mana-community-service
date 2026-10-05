package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.GroceryItem;
import com.manacommunity.api.model.GroceryOrder;
import com.manacommunity.api.model.GroceryOrder.GroceryOrderStatus;
import com.manacommunity.api.model.GroceryOrderItem;
import com.manacommunity.api.repository.GroceryItemRepository;
import com.manacommunity.api.repository.GroceryOrderRepository;
import com.manacommunity.api.service.GroceryService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroceryServiceImpl implements GroceryService {

    private final GroceryItemRepository itemRepository;
    private final GroceryOrderRepository orderRepository;

    @Override
    public List<GroceryItemResponse> getAvailableItems(Long communityId) {
        return itemRepository.findByCommunityIdAndAvailableTrueOrderByNameAsc(communityId)
                .stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    public GroceryItemResponse getItem(Long id, Long communityId) {
        GroceryItem item = itemRepository.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("GroceryItem", "id", id.toString()));
        return toItemResponse(item);
    }

    @Override
    public List<GroceryOrderResponse> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    public GroceryOrderResponse getOrder(Long id, Long userId) {
        GroceryOrder order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("GroceryOrder", "id", id.toString()));
        return toOrderResponse(order);
    }

    @Override
    @Transactional
    public GroceryOrderResponse createOrder(GroceryOrderRequest request, AppUser user) {
        Long communityId = user.getCommunity().getId();

        GroceryOrder order = GroceryOrder.builder()
                .user(user)
                .community(user.getCommunity())
                .deliveryAddress(request.getDeliveryAddress())
                .status(GroceryOrderStatus.PLACED)
                .items(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (GroceryOrderItemRequest itemReq : request.getItems()) {
            GroceryItem groceryItem = itemRepository.findByIdAndCommunityId(itemReq.getGroceryItemId(), communityId)
                    .orElseThrow(() -> new ResourceNotFoundException("GroceryItem", "id", itemReq.getGroceryItemId().toString()));

            BigDecimal subtotal = groceryItem.getPricePerUnit().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            GroceryOrderItem orderItem = GroceryOrderItem.builder()
                    .order(order)
                    .groceryItem(groceryItem)
                    .quantity(itemReq.getQuantity())
                    .subtotal(subtotal)
                    .build();

            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);
        return toOrderResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public GroceryOrderResponse cancelOrder(Long id, Long userId) {
        GroceryOrder order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("GroceryOrder", "id", id.toString()));

        if (order.getStatus() == GroceryOrderStatus.DELIVERED || order.getStatus() == GroceryOrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel an order that is already " + order.getStatus().name().toLowerCase() + ".");
        }
        order.setStatus(GroceryOrderStatus.CANCELLED);
        return toOrderResponse(orderRepository.save(order));
    }

    private GroceryItemResponse toItemResponse(GroceryItem item) {
        return GroceryItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .category(item.getCategory())
                .unit(item.getUnit())
                .pricePerUnit(item.getPricePerUnit())
                .available(item.getAvailable())
                .imageUrl(item.getImageUrl())
                .vendorName(item.getVendorName())
                .createdAt(item.getCreatedAt())
                .build();
    }

    private GroceryOrderResponse toOrderResponse(GroceryOrder order) {
        List<GroceryOrderItemResponse> items = order.getItems().stream()
                .map(oi -> GroceryOrderItemResponse.builder()
                        .id(oi.getId())
                        .groceryItemId(oi.getGroceryItem().getId())
                        .groceryItemName(oi.getGroceryItem().getName())
                        .quantity(oi.getQuantity())
                        .subtotal(oi.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return GroceryOrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .userName(order.getUser().getFullName())
                .items(items)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .deliveryAddress(order.getDeliveryAddress())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
