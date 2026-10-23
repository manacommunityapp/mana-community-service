package com.manacommunity.api.service;

import com.manacommunity.api.dto.GroceryItemResponse;
import com.manacommunity.api.dto.GroceryOrderRequest;
import com.manacommunity.api.dto.GroceryOrderResponse;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface GroceryService {

    List<GroceryItemResponse> getAvailableItems(Long communityId);

    GroceryItemResponse getItem(Long id, Long communityId);

    List<GroceryOrderResponse> getUserOrders(Long userId);

    GroceryOrderResponse getOrder(Long id, Long userId);

    GroceryOrderResponse createOrder(GroceryOrderRequest request, AppUser user);

    GroceryOrderResponse cancelOrder(Long id, Long userId);
}
