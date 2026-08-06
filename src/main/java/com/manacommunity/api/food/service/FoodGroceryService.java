package com.manacommunity.api.food.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.food.entity.*;
import com.manacommunity.api.food.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodGroceryService {

    private final FoodGroceryStoreRepository storeRepo;
    private final FoodGroceryCategoryRepository categoryRepo;
    private final FoodGroceryProductRepository productRepo;
    private final FoodGroceryOrderRepository orderRepo;
    private final FoodGroceryDeliverySlotRepository slotRepo;
    private final FoodGroceryWishlistRepository wishlistRepo;

    // ---- Store ----

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getStores(Long communityId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            return storeRepo.findByCommunityIdAndStatus(communityId,
                    FoodGroceryStore.StoreStatus.valueOf(status), pageable).map(this::toStoreResponse);
        }
        return storeRepo.findByCommunityId(communityId, pageable).map(this::toStoreResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStoreById(Long communityId, Long id) {
        FoodGroceryStore store = storeRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("GroceryStore", id));
        return toStoreResponse(store);
    }

    @Transactional
    public Map<String, Object> createStore(Long communityId, Map<String, Object> request, AppUser user) {
        Community community = user.getCommunity();
        FoodGroceryStore store = FoodGroceryStore.builder()
                .name((String) request.get("name"))
                .slug((String) request.get("slug"))
                .description((String) request.get("description"))
                .address((String) request.get("address"))
                .logoUrl((String) request.get("logoUrl"))
                .coverImageUrl((String) request.get("coverImageUrl"))
                .storeType(request.get("storeType") != null ?
                        FoodGroceryStore.StoreType.valueOf((String) request.get("storeType")) : null)
                .deliveryEnabled(request.get("deliveryEnabled") != null ?
                        (Boolean) request.get("deliveryEnabled") : true)
                .minOrder(request.get("minOrder") != null ?
                        new BigDecimal(request.get("minOrder").toString()) : null)
                .deliveryFee(request.get("deliveryFee") != null ?
                        new BigDecimal(request.get("deliveryFee").toString()) : null)
                .owner(user)
                .community(community)
                .build();

        return toStoreResponse(storeRepo.save(store));
    }

    @Transactional
    public Map<String, Object> updateStore(Long communityId, Long id, Map<String, Object> request) {
        FoodGroceryStore store = storeRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("GroceryStore", id));

        if (request.containsKey("name")) store.setName((String) request.get("name"));
        if (request.containsKey("slug")) store.setSlug((String) request.get("slug"));
        if (request.containsKey("description")) store.setDescription((String) request.get("description"));
        if (request.containsKey("address")) store.setAddress((String) request.get("address"));
        if (request.containsKey("logoUrl")) store.setLogoUrl((String) request.get("logoUrl"));
        if (request.containsKey("coverImageUrl")) store.setCoverImageUrl((String) request.get("coverImageUrl"));
        if (request.containsKey("storeType"))
            store.setStoreType(FoodGroceryStore.StoreType.valueOf((String) request.get("storeType")));
        if (request.containsKey("status"))
            store.setStatus(FoodGroceryStore.StoreStatus.valueOf((String) request.get("status")));
        if (request.containsKey("deliveryEnabled"))
            store.setDeliveryEnabled((Boolean) request.get("deliveryEnabled"));
        if (request.containsKey("minOrder"))
            store.setMinOrder(new BigDecimal(request.get("minOrder").toString()));
        if (request.containsKey("deliveryFee"))
            store.setDeliveryFee(new BigDecimal(request.get("deliveryFee").toString()));

        return toStoreResponse(storeRepo.save(store));
    }

    // ---- Category ----

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCategories(Long communityId, Long storeId) {
        return categoryRepo.findByStoreIdAndActiveOrderBySortOrder(storeId, true)
                .stream().map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("storeId", c.getStore() != null ? c.getStore().getId() : null);
                    map.put("name", c.getName());
                    map.put("slug", c.getSlug());
                    map.put("icon", c.getIcon());
                    map.put("parentId", c.getParent() != null ? c.getParent().getId() : null);
                    map.put("sortOrder", c.getSortOrder());
                    map.put("active", c.getActive());
                    map.put("createdAt", c.getCreatedAt());
                    return map;
                }).collect(Collectors.toList());
    }

    // ---- Product ----

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getProducts(Long communityId, Long storeId, Long categoryId, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return productRepo.searchByStore(storeId, search, pageable).map(this::toProductResponse);
        }
        if (categoryId != null) {
            return productRepo.findByCategoryIdAndActive(categoryId, true, pageable).map(this::toProductResponse);
        }
        return productRepo.findByStoreIdAndActive(storeId, true, pageable).map(this::toProductResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProductById(Long communityId, Long id) {
        FoodGroceryProduct product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GroceryProduct", id));
        return toProductResponse(product);
    }

    @Transactional
    public Map<String, Object> createProduct(Long communityId, Map<String, Object> request) {
        Long storeId = Long.valueOf(request.get("storeId").toString());
        FoodGroceryStore store = storeRepo.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("GroceryStore", storeId));

        Long categoryId = Long.valueOf(request.get("categoryId").toString());
        FoodGroceryCategory category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("GroceryCategory", categoryId));

        Community community = new Community();
        community.setId(communityId);

        FoodGroceryProduct product = FoodGroceryProduct.builder()
                .store(store)
                .category(category)
                .name((String) request.get("name"))
                .slug((String) request.get("slug"))
                .description((String) request.get("description"))
                .imageUrl((String) request.get("imageUrl"))
                .images((String) request.get("images"))
                .brand((String) request.get("brand"))
                .unit((String) request.get("unit"))
                .unitValue(request.get("unitValue") != null ?
                        new BigDecimal(request.get("unitValue").toString()) : null)
                .price(request.get("price") != null ?
                        new BigDecimal(request.get("price").toString()) : null)
                .discountedPrice(request.get("discountedPrice") != null ?
                        new BigDecimal(request.get("discountedPrice").toString()) : null)
                .stock(request.get("stock") != null ?
                        Integer.valueOf(request.get("stock").toString()) : null)
                .lowStockThreshold(request.get("lowStockThreshold") != null ?
                        Integer.valueOf(request.get("lowStockThreshold").toString()) : null)
                .community(community)
                .build();

        return toProductResponse(productRepo.save(product));
    }

    @Transactional
    public Map<String, Object> updateProduct(Long communityId, Long id, Map<String, Object> request) {
        FoodGroceryProduct product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GroceryProduct", id));

        if (request.containsKey("name")) product.setName((String) request.get("name"));
        if (request.containsKey("slug")) product.setSlug((String) request.get("slug"));
        if (request.containsKey("description")) product.setDescription((String) request.get("description"));
        if (request.containsKey("imageUrl")) product.setImageUrl((String) request.get("imageUrl"));
        if (request.containsKey("images")) product.setImages((String) request.get("images"));
        if (request.containsKey("brand")) product.setBrand((String) request.get("brand"));
        if (request.containsKey("unit")) product.setUnit((String) request.get("unit"));
        if (request.containsKey("unitValue"))
            product.setUnitValue(new BigDecimal(request.get("unitValue").toString()));
        if (request.containsKey("price"))
            product.setPrice(new BigDecimal(request.get("price").toString()));
        if (request.containsKey("discountedPrice"))
            product.setDiscountedPrice(new BigDecimal(request.get("discountedPrice").toString()));
        if (request.containsKey("stock"))
            product.setStock(Integer.valueOf(request.get("stock").toString()));
        if (request.containsKey("lowStockThreshold"))
            product.setLowStockThreshold(Integer.valueOf(request.get("lowStockThreshold").toString()));
        if (request.containsKey("active"))
            product.setActive((Boolean) request.get("active"));

        return toProductResponse(productRepo.save(product));
    }

    // ---- Order ----

    @Transactional
    public Map<String, Object> placeOrder(Long communityId, Map<String, Object> request, AppUser user) {
        Community community = user.getCommunity();
        Long storeId = Long.valueOf(request.get("storeId").toString());
        FoodGroceryStore store = storeRepo.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("GroceryStore", storeId));

        String orderNumber = "GRO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        FoodGroceryOrder order = FoodGroceryOrder.builder()
                .user(user)
                .store(store)
                .orderNumber(orderNumber)
                .status(FoodGroceryOrder.GroceryOrderStatus.PLACED)
                .subtotal(request.get("subtotal") != null ?
                        new BigDecimal(request.get("subtotal").toString()) : BigDecimal.ZERO)
                .tax(request.get("tax") != null ?
                        new BigDecimal(request.get("tax").toString()) : BigDecimal.ZERO)
                .deliveryFee(request.get("deliveryFee") != null ?
                        new BigDecimal(request.get("deliveryFee").toString()) : BigDecimal.ZERO)
                .discount(request.get("discount") != null ?
                        new BigDecimal(request.get("discount").toString()) : BigDecimal.ZERO)
                .totalAmount(request.get("totalAmount") != null ?
                        new BigDecimal(request.get("totalAmount").toString()) : BigDecimal.ZERO)
                .deliveryAddress((String) request.get("deliveryAddress"))
                .deliverySlot(request.get("deliverySlot") != null ?
                        LocalDateTime.parse((String) request.get("deliverySlot")) : null)
                .paymentMethod((String) request.get("paymentMethod"))
                .paymentStatus(FoodGroceryOrder.PaymentStatus.PENDING)
                .community(community)
                .build();

        return toOrderResponse(orderRepo.save(order));
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getMyOrders(Long communityId, Long userId, Pageable pageable) {
        return orderRepo.findByUserIdAndCommunityId(userId, communityId, pageable)
                .map(this::toOrderResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOrderById(Long communityId, Long id) {
        FoodGroceryOrder order = orderRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("GroceryOrder", id));
        return toOrderResponse(order);
    }

    @Transactional
    public Map<String, Object> updateStatus(Long communityId, Long id, String status) {
        FoodGroceryOrder order = orderRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("GroceryOrder", id));
        order.setStatus(FoodGroceryOrder.GroceryOrderStatus.valueOf(status));
        if ("DELIVERED".equals(status)) {
            order.setDeliveredAt(LocalDateTime.now());
        }
        return toOrderResponse(orderRepo.save(order));
    }

    // ---- Delivery Slots ----

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDeliverySlots(Long communityId, Long storeId, LocalDate date) {
        return slotRepo.findByStoreIdAndDate(storeId, date)
                .stream().map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", s.getId());
                    map.put("storeId", s.getStore() != null ? s.getStore().getId() : null);
                    map.put("date", s.getDate());
                    map.put("startTime", s.getStartTime());
                    map.put("endTime", s.getEndTime());
                    map.put("capacity", s.getCapacity());
                    map.put("booked", s.getBooked());
                    map.put("slotType", s.getSlotType() != null ? s.getSlotType().name() : null);
                    map.put("available", s.getCapacity() != null && s.getBooked() != null ?
                            s.getCapacity() - s.getBooked() : null);
                    map.put("createdAt", s.getCreatedAt());
                    return map;
                }).collect(Collectors.toList());
    }

    // ---- Wishlist ----

    @Transactional
    public Map<String, Object> addToWishlist(Long communityId, Long userId, Map<String, Object> request) {
        Long productId = Long.valueOf(request.get("productId").toString());
        FoodGroceryProduct product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("GroceryProduct", productId));

        AppUser user = new AppUser();
        user.setId(userId);
        Community community = new Community();
        community.setId(communityId);

        FoodGroceryWishlist wishlist = FoodGroceryWishlist.builder()
                .user(user)
                .product(product)
                .addedAt(LocalDateTime.now())
                .community(community)
                .build();
        wishlist = wishlistRepo.save(wishlist);

        Map<String, Object> map = new HashMap<>();
        map.put("id", wishlist.getId());
        map.put("userId", userId);
        map.put("productId", productId);
        map.put("addedAt", wishlist.getAddedAt());
        map.put("createdAt", wishlist.getCreatedAt());
        return map;
    }

    @Transactional
    public void removeFromWishlist(Long communityId, Long userId, Long productId) {
        wishlistRepo.deleteByUserIdAndProductId(userId, productId);
    }

    // ---- Private mappers ----

    private Map<String, Object> toStoreResponse(FoodGroceryStore s) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", s.getId());
        map.put("name", s.getName());
        map.put("slug", s.getSlug());
        map.put("description", s.getDescription());
        map.put("address", s.getAddress());
        map.put("logoUrl", s.getLogoUrl());
        map.put("coverImageUrl", s.getCoverImageUrl());
        map.put("storeType", s.getStoreType() != null ? s.getStoreType().name() : null);
        map.put("status", s.getStatus() != null ? s.getStatus().name() : null);
        map.put("rating", s.getRating());
        map.put("deliveryEnabled", s.getDeliveryEnabled());
        map.put("minOrder", s.getMinOrder());
        map.put("deliveryFee", s.getDeliveryFee());
        map.put("ownerId", s.getOwner() != null ? s.getOwner().getId() : null);
        map.put("communityId", s.getCommunity() != null ? s.getCommunity().getId() : null);
        map.put("createdAt", s.getCreatedAt());
        map.put("updatedAt", s.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toProductResponse(FoodGroceryProduct p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("storeId", p.getStore() != null ? p.getStore().getId() : null);
        map.put("categoryId", p.getCategory() != null ? p.getCategory().getId() : null);
        map.put("name", p.getName());
        map.put("slug", p.getSlug());
        map.put("description", p.getDescription());
        map.put("imageUrl", p.getImageUrl());
        map.put("images", p.getImages());
        map.put("brand", p.getBrand());
        map.put("unit", p.getUnit());
        map.put("unitValue", p.getUnitValue());
        map.put("price", p.getPrice());
        map.put("discountedPrice", p.getDiscountedPrice());
        map.put("stock", p.getStock());
        map.put("lowStockThreshold", p.getLowStockThreshold());
        map.put("active", p.getActive());
        map.put("communityId", p.getCommunity() != null ? p.getCommunity().getId() : null);
        map.put("createdAt", p.getCreatedAt());
        map.put("updatedAt", p.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toOrderResponse(FoodGroceryOrder o) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", o.getId());
        map.put("userId", o.getUser() != null ? o.getUser().getId() : null);
        map.put("storeId", o.getStore() != null ? o.getStore().getId() : null);
        map.put("orderNumber", o.getOrderNumber());
        map.put("status", o.getStatus() != null ? o.getStatus().name() : null);
        map.put("subtotal", o.getSubtotal());
        map.put("tax", o.getTax());
        map.put("deliveryFee", o.getDeliveryFee());
        map.put("discount", o.getDiscount());
        map.put("totalAmount", o.getTotalAmount());
        map.put("deliveryAddress", o.getDeliveryAddress());
        map.put("deliverySlot", o.getDeliverySlot());
        map.put("deliveredAt", o.getDeliveredAt());
        map.put("paymentStatus", o.getPaymentStatus() != null ? o.getPaymentStatus().name() : null);
        map.put("paymentMethod", o.getPaymentMethod());
        map.put("communityId", o.getCommunity() != null ? o.getCommunity().getId() : null);
        map.put("createdAt", o.getCreatedAt());
        map.put("updatedAt", o.getUpdatedAt());
        return map;
    }
}
