package com.manacommunity.api.food.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.food.entity.FoodPantryAlert;
import com.manacommunity.api.food.entity.FoodPantryItem;
import com.manacommunity.api.food.entity.FoodPantryShoppingList;
import com.manacommunity.api.food.entity.FoodPantryShoppingListItem;
import com.manacommunity.api.food.repository.FoodPantryAlertRepository;
import com.manacommunity.api.food.repository.FoodPantryItemRepository;
import com.manacommunity.api.food.repository.FoodPantryShoppingListItemRepository;
import com.manacommunity.api.food.repository.FoodPantryShoppingListRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodPantryService {

    private final FoodPantryItemRepository pantryItemRepo;
    private final FoodPantryAlertRepository pantryAlertRepo;
    private final FoodPantryShoppingListRepository shoppingListRepo;
    private final FoodPantryShoppingListItemRepository shoppingListItemRepo;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getItems(Long communityId, Long userId) {
        List<FoodPantryItem> items = pantryItemRepo.findByUserIdAndCommunityId(userId, communityId);
        return items.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> addItem(Long communityId, Map<String, Object> request, AppUser user) {
        Community community = user.getCommunity();
        FoodPantryItem item = FoodPantryItem.builder()
                .user(user)
                .itemName((String) request.get("itemName"))
                .category((String) request.get("category"))
                .unit((String) request.get("unit"))
                .barcode((String) request.get("barcode"))
                .imageUrl((String) request.get("imageUrl"))
                .status(FoodPantryItem.PantryItemStatus.AVAILABLE)
                .community(community)
                .build();

        if (request.containsKey("quantity")) {
            item.setQuantity(new BigDecimal(request.get("quantity").toString()));
        }
        if (request.containsKey("purchaseDate")) {
            item.setPurchaseDate(LocalDate.parse((String) request.get("purchaseDate")));
        }
        if (request.containsKey("expiryDate")) {
            item.setExpiryDate(LocalDate.parse((String) request.get("expiryDate")));
        }
        if (request.containsKey("storageLocation")) {
            item.setStorageLocation(FoodPantryItem.StorageLocation.valueOf((String) request.get("storageLocation")));
        }

        FoodPantryItem saved = pantryItemRepo.save(item);
        return toResponse(saved);
    }

    @Transactional
    public Map<String, Object> updateItem(Long communityId, Long id, Map<String, Object> request) {
        FoodPantryItem item = pantryItemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PantryItem", id));

        if (request.containsKey("itemName")) {
            item.setItemName((String) request.get("itemName"));
        }
        if (request.containsKey("category")) {
            item.setCategory((String) request.get("category"));
        }
        if (request.containsKey("quantity")) {
            item.setQuantity(new BigDecimal(request.get("quantity").toString()));
        }
        if (request.containsKey("unit")) {
            item.setUnit((String) request.get("unit"));
        }
        if (request.containsKey("purchaseDate")) {
            item.setPurchaseDate(LocalDate.parse((String) request.get("purchaseDate")));
        }
        if (request.containsKey("expiryDate")) {
            item.setExpiryDate(LocalDate.parse((String) request.get("expiryDate")));
        }
        if (request.containsKey("barcode")) {
            item.setBarcode((String) request.get("barcode"));
        }
        if (request.containsKey("imageUrl")) {
            item.setImageUrl((String) request.get("imageUrl"));
        }
        if (request.containsKey("storageLocation")) {
            item.setStorageLocation(FoodPantryItem.StorageLocation.valueOf((String) request.get("storageLocation")));
        }
        if (request.containsKey("status")) {
            item.setStatus(FoodPantryItem.PantryItemStatus.valueOf((String) request.get("status")));
        }

        FoodPantryItem saved = pantryItemRepo.save(item);
        return toResponse(saved);
    }

    @Transactional
    public Map<String, Object> consume(Long communityId, Long id, BigDecimal quantityUsed, String usedFor, AppUser user) {
        FoodPantryItem item = pantryItemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PantryItem", id));

        BigDecimal remaining = item.getQuantity().subtract(quantityUsed);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            item.setQuantity(BigDecimal.ZERO);
            item.setStatus(FoodPantryItem.PantryItemStatus.CONSUMED);
        } else {
            item.setQuantity(remaining);
            if (remaining.compareTo(new BigDecimal("1")) <= 0) {
                item.setStatus(FoodPantryItem.PantryItemStatus.LOW);
            }
        }

        FoodPantryItem saved = pantryItemRepo.save(item);
        Map<String, Object> response = toResponse(saved);
        response.put("quantityUsed", quantityUsed);
        response.put("usedFor", usedFor);
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getExpiring(Long communityId, Long userId, int daysAhead) {
        LocalDate expiryThreshold = LocalDate.now().plusDays(daysAhead);
        List<FoodPantryItem> items = pantryItemRepo.findByUserIdAndExpiryDateBefore(userId, expiryThreshold);
        return items.stream()
                .filter(i -> i.getExpiryDate() != null && !i.getExpiryDate().isBefore(LocalDate.now()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLists(Long communityId, Long userId) {
        List<FoodPantryShoppingList> lists = shoppingListRepo.findByUserIdAndCommunityId(userId, communityId);
        return lists.stream().map(sl -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", sl.getId());
            map.put("name", sl.getName());
            map.put("status", sl.getStatus() != null ? sl.getStatus().name() : null);
            map.put("userId", sl.getUser().getId());
            map.put("communityId", sl.getCommunity() != null ? sl.getCommunity().getId() : null);
            map.put("createdAt", sl.getCreatedAt());
            map.put("updatedAt", sl.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> create(Long communityId, String name, AppUser user) {
        Community community = user.getCommunity();
        FoodPantryShoppingList list = FoodPantryShoppingList.builder()
                .name(name)
                .user(user)
                .status(FoodPantryShoppingList.ShoppingListStatus.ACTIVE)
                .community(community)
                .build();

        FoodPantryShoppingList saved = shoppingListRepo.save(list);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("name", saved.getName());
        map.put("status", saved.getStatus() != null ? saved.getStatus().name() : null);
        map.put("userId", saved.getUser().getId());
        map.put("communityId", saved.getCommunity() != null ? saved.getCommunity().getId() : null);
        map.put("createdAt", saved.getCreatedAt());
        map.put("updatedAt", saved.getUpdatedAt());
        return map;
    }

    @Transactional
    public Map<String, Object> addItem(Long communityId, Long listId, Map<String, Object> request) {
        FoodPantryShoppingList list = shoppingListRepo.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("ShoppingList", listId));

        BigDecimal quantity = null;
        if (request.get("quantity") != null) {
            quantity = new BigDecimal(request.get("quantity").toString());
        }
        BigDecimal estimatedPrice = null;
        if (request.get("estimatedPrice") != null) {
            estimatedPrice = new BigDecimal(request.get("estimatedPrice").toString());
        }

        FoodPantryShoppingListItem item = FoodPantryShoppingListItem.builder()
                .list(list)
                .itemName((String) request.get("itemName"))
                .category((String) request.get("category"))
                .quantity(quantity)
                .unit((String) request.get("unit"))
                .estimatedPrice(estimatedPrice)
                .isPurchased(false)
                .build();
        FoodPantryShoppingListItem saved = shoppingListItemRepo.save(item);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("listId", listId);
        map.put("itemName", saved.getItemName());
        map.put("category", saved.getCategory());
        map.put("quantity", saved.getQuantity());
        map.put("unit", saved.getUnit());
        map.put("estimatedPrice", saved.getEstimatedPrice());
        map.put("isPurchased", saved.getIsPurchased());
        map.put("createdAt", saved.getCreatedAt());
        return map;
    }

    @Transactional
    public Map<String, Object> markPurchased(Long communityId, Long listItemId, BigDecimal purchasedPrice) {
        FoodPantryShoppingListItem item = shoppingListItemRepo.findById(listItemId)
                .orElseThrow(() -> new ResourceNotFoundException("ShoppingListItem", listItemId));
        item.setIsPurchased(true);
        if (purchasedPrice != null) {
            item.setPurchasedPrice(purchasedPrice);
        }
        FoodPantryShoppingListItem saved = shoppingListItemRepo.save(item);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("listId", saved.getList().getId());
        map.put("itemName", saved.getItemName());
        map.put("isPurchased", saved.getIsPurchased());
        map.put("purchasedPrice", saved.getPurchasedPrice());
        map.put("updatedAt", saved.getUpdatedAt());
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAlerts(Long communityId, Long userId) {
        List<FoodPantryAlert> alerts = pantryAlertRepo.findByUserIdAndCommunityId(userId, communityId);
        return alerts.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("userId", a.getUser().getId());
            map.put("pantryItemId", a.getPantryItem() != null ? a.getPantryItem().getId() : null);
            map.put("alertType", a.getAlertType() != null ? a.getAlertType().name() : null);
            map.put("message", a.getMessage());
            map.put("isRead", a.getIsRead());
            map.put("communityId", a.getCommunity() != null ? a.getCommunity().getId() : null);
            map.put("createdAt", a.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> markRead(Long communityId, Long alertId) {
        FoodPantryAlert alert = pantryAlertRepo.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("PantryAlert", alertId));
        alert.setIsRead(true);
        FoodPantryAlert saved = pantryAlertRepo.save(alert);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("userId", saved.getUser().getId());
        map.put("pantryItemId", saved.getPantryItem() != null ? saved.getPantryItem().getId() : null);
        map.put("alertType", saved.getAlertType() != null ? saved.getAlertType().name() : null);
        map.put("message", saved.getMessage());
        map.put("isRead", saved.getIsRead());
        map.put("communityId", saved.getCommunity() != null ? saved.getCommunity().getId() : null);
        map.put("createdAt", saved.getCreatedAt());
        return map;
    }

    private Map<String, Object> toResponse(FoodPantryItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getId());
        map.put("userId", item.getUser().getId());
        map.put("itemName", item.getItemName());
        map.put("category", item.getCategory());
        map.put("quantity", item.getQuantity());
        map.put("unit", item.getUnit());
        map.put("purchaseDate", item.getPurchaseDate());
        map.put("expiryDate", item.getExpiryDate());
        map.put("barcode", item.getBarcode());
        map.put("imageUrl", item.getImageUrl());
        map.put("storageLocation", item.getStorageLocation() != null ? item.getStorageLocation().name() : null);
        map.put("status", item.getStatus() != null ? item.getStatus().name() : null);
        map.put("communityId", item.getCommunity() != null ? item.getCommunity().getId() : null);
        map.put("createdAt", item.getCreatedAt());
        map.put("updatedAt", item.getUpdatedAt());

        if (item.getExpiryDate() != null) {
            long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), item.getExpiryDate());
            map.put("daysUntilExpiry", daysUntilExpiry);
        }

        return map;
    }
}
