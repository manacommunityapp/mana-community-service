package com.manacommunity.api.food.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.food.entity.*;
import com.manacommunity.api.food.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodMenuService {

    private final FoodMenuCategoryRepository categoryRepo;
    private final FoodMenuItemRepository menuItemRepo;
    private final FoodMenuItemVariantRepository variantRepo;
    private final FoodMenuItemAddonRepository addonRepo;
    private final FoodMenuItemComboRepository comboRepo;
    private final FoodRestaurantRepository restaurantRepo;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCategories(Long restaurantId) {
        List<FoodMenuCategory> categories = categoryRepo.findByRestaurantIdAndActiveOrderBySortOrder(restaurantId, true);
        return categories.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("restaurantId", c.getRestaurant().getId());
            map.put("name", c.getName());
            map.put("slug", c.getSlug());
            map.put("description", c.getDescription());
            map.put("imageUrl", c.getImageUrl());
            map.put("sortOrder", c.getSortOrder());
            map.put("active", c.getActive());
            map.put("parentId", c.getParent() != null ? c.getParent().getId() : null);
            map.put("communityId", c.getCommunity().getId());
            map.put("createdAt", c.getCreatedAt());
            map.put("updatedAt", c.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createCategory(Map<String, Object> request, Long restaurantId, Long communityId) {
        FoodRestaurant restaurant = restaurantRepo.findByIdAndCommunityId(restaurantId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));

        String name = (String) request.get("name");
        FoodMenuCategory category = FoodMenuCategory.builder()
                .restaurant(restaurant)
                .name(name)
                .slug(name.toLowerCase().replaceAll("\\s+", "-"))
                .description((String) request.get("description"))
                .imageUrl((String) request.get("imageUrl"))
                .sortOrder(request.containsKey("sortOrder") ? (Integer) request.get("sortOrder") : 0)
                .active(true)
                .community(restaurant.getCommunity())
                .build();

        if (request.containsKey("parentId")) {
            Long parentId = Long.valueOf(request.get("parentId").toString());
            FoodMenuCategory parent = categoryRepo.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", parentId));
            category.setParent(parent);
        }

        FoodMenuCategory saved = categoryRepo.save(category);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("restaurantId", saved.getRestaurant().getId());
        map.put("name", saved.getName());
        map.put("slug", saved.getSlug());
        map.put("description", saved.getDescription());
        map.put("imageUrl", saved.getImageUrl());
        map.put("sortOrder", saved.getSortOrder());
        map.put("active", saved.getActive());
        map.put("parentId", saved.getParent() != null ? saved.getParent().getId() : null);
        map.put("communityId", saved.getCommunity().getId());
        map.put("createdAt", saved.getCreatedAt());
        map.put("updatedAt", saved.getUpdatedAt());
        return map;
    }

    @Transactional
    public Map<String, Object> updateCategory(Long categoryId, Map<String, Object> request) {
        FoodMenuCategory category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", categoryId));

        if (request.containsKey("name")) {
            category.setName((String) request.get("name"));
            category.setSlug(((String) request.get("name")).toLowerCase().replaceAll("\\s+", "-"));
        }
        if (request.containsKey("description")) {
            category.setDescription((String) request.get("description"));
        }
        if (request.containsKey("imageUrl")) {
            category.setImageUrl((String) request.get("imageUrl"));
        }
        if (request.containsKey("sortOrder")) {
            category.setSortOrder((Integer) request.get("sortOrder"));
        }
        if (request.containsKey("active")) {
            category.setActive((Boolean) request.get("active"));
        }

        FoodMenuCategory saved = categoryRepo.save(category);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("restaurantId", saved.getRestaurant().getId());
        map.put("name", saved.getName());
        map.put("slug", saved.getSlug());
        map.put("description", saved.getDescription());
        map.put("imageUrl", saved.getImageUrl());
        map.put("sortOrder", saved.getSortOrder());
        map.put("active", saved.getActive());
        map.put("parentId", saved.getParent() != null ? saved.getParent().getId() : null);
        map.put("communityId", saved.getCommunity().getId());
        map.put("createdAt", saved.getCreatedAt());
        map.put("updatedAt", saved.getUpdatedAt());
        return map;
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        if (!categoryRepo.existsById(categoryId)) {
            throw new ResourceNotFoundException("MenuCategory", categoryId);
        }
        categoryRepo.deleteById(categoryId);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getMenuItems(Long restaurantId, Long categoryId, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return menuItemRepo.searchByRestaurant(restaurantId, search, pageable)
                    .map(this::toResponse);
        }
        if (categoryId != null) {
            List<FoodMenuItem> items = menuItemRepo.findByCategoryId(categoryId);
            return new org.springframework.data.domain.PageImpl<>(
                    items.stream().map(this::toResponse).collect(Collectors.toList()),
                    pageable,
                    items.size()
            );
        }
        return menuItemRepo.findByRestaurantId(restaurantId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMenuItem(Long itemId) {
        FoodMenuItem item = menuItemRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", itemId));
        return toResponse(item);
    }

    @Transactional
    public Map<String, Object> createMenuItem(Map<String, Object> request, Long restaurantId, Long communityId) {
        FoodRestaurant restaurant = restaurantRepo.findByIdAndCommunityId(restaurantId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));

        Long categoryId = Long.valueOf(request.get("categoryId").toString());
        FoodMenuCategory category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", categoryId));

        String name = (String) request.get("name");
        FoodMenuItem item = FoodMenuItem.builder()
                .category(category)
                .restaurant(restaurant)
                .name(name)
                .slug(name.toLowerCase().replaceAll("\\s+", "-"))
                .description((String) request.get("description"))
                .imageUrl((String) request.get("imageUrl"))
                .price(new BigDecimal(request.get("price").toString()))
                .community(restaurant.getCommunity())
                .build();

        if (request.containsKey("discountedPrice")) {
            item.setDiscountedPrice(new BigDecimal(request.get("discountedPrice").toString()));
        }
        if (request.containsKey("isVeg")) {
            item.setIsVeg((Boolean) request.get("isVeg"));
        }
        if (request.containsKey("isVegan")) {
            item.setIsVegan((Boolean) request.get("isVegan"));
        }
        if (request.containsKey("isJain")) {
            item.setIsJain((Boolean) request.get("isJain"));
        }
        if (request.containsKey("spiceLevel")) {
            item.setSpiceLevel((Integer) request.get("spiceLevel"));
        }
        if (request.containsKey("calories")) {
            item.setCalories((Integer) request.get("calories"));
        }
        if (request.containsKey("protein")) {
            item.setProtein(new BigDecimal(request.get("protein").toString()));
        }
        if (request.containsKey("carbs")) {
            item.setCarbs(new BigDecimal(request.get("carbs").toString()));
        }
        if (request.containsKey("fat")) {
            item.setFat(new BigDecimal(request.get("fat").toString()));
        }
        if (request.containsKey("fiber")) {
            item.setFiber(new BigDecimal(request.get("fiber").toString()));
        }
        if (request.containsKey("preparationTime")) {
            item.setPreparationTime((Integer) request.get("preparationTime"));
        }
        if (request.containsKey("sortOrder")) {
            item.setSortOrder((Integer) request.get("sortOrder"));
        }
        if (request.containsKey("tags")) {
            item.setTags((String) request.get("tags"));
        }

        FoodMenuItem saved = menuItemRepo.save(item);
        return toResponse(saved);
    }

    @Transactional
    public Map<String, Object> updateMenuItem(Long itemId, Map<String, Object> request) {
        FoodMenuItem item = menuItemRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", itemId));

        if (request.containsKey("name")) {
            item.setName((String) request.get("name"));
            item.setSlug(((String) request.get("name")).toLowerCase().replaceAll("\\s+", "-"));
        }
        if (request.containsKey("description")) {
            item.setDescription((String) request.get("description"));
        }
        if (request.containsKey("imageUrl")) {
            item.setImageUrl((String) request.get("imageUrl"));
        }
        if (request.containsKey("price")) {
            item.setPrice(new BigDecimal(request.get("price").toString()));
        }
        if (request.containsKey("discountedPrice")) {
            item.setDiscountedPrice(new BigDecimal(request.get("discountedPrice").toString()));
        }
        if (request.containsKey("isVeg")) {
            item.setIsVeg((Boolean) request.get("isVeg"));
        }
        if (request.containsKey("isVegan")) {
            item.setIsVegan((Boolean) request.get("isVegan"));
        }
        if (request.containsKey("isJain")) {
            item.setIsJain((Boolean) request.get("isJain"));
        }
        if (request.containsKey("spiceLevel")) {
            item.setSpiceLevel((Integer) request.get("spiceLevel"));
        }
        if (request.containsKey("calories")) {
            item.setCalories((Integer) request.get("calories"));
        }
        if (request.containsKey("protein")) {
            item.setProtein(new BigDecimal(request.get("protein").toString()));
        }
        if (request.containsKey("carbs")) {
            item.setCarbs(new BigDecimal(request.get("carbs").toString()));
        }
        if (request.containsKey("fat")) {
            item.setFat(new BigDecimal(request.get("fat").toString()));
        }
        if (request.containsKey("fiber")) {
            item.setFiber(new BigDecimal(request.get("fiber").toString()));
        }
        if (request.containsKey("preparationTime")) {
            item.setPreparationTime((Integer) request.get("preparationTime"));
        }
        if (request.containsKey("sortOrder")) {
            item.setSortOrder((Integer) request.get("sortOrder"));
        }
        if (request.containsKey("tags")) {
            item.setTags((String) request.get("tags"));
        }
        if (request.containsKey("categoryId")) {
            Long categoryId = Long.valueOf(request.get("categoryId").toString());
            FoodMenuCategory category = categoryRepo.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", categoryId));
            item.setCategory(category);
        }

        FoodMenuItem saved = menuItemRepo.save(item);
        return toResponse(saved);
    }

    @Transactional
    public Map<String, Object> toggleAvailability(Long itemId, Boolean available) {
        FoodMenuItem item = menuItemRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", itemId));
        item.setIsAvailable(available);
        FoodMenuItem saved = menuItemRepo.save(item);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCombos(Long restaurantId) {
        List<FoodMenuItemCombo> combos = comboRepo.findByRestaurantIdAndActive(restaurantId, true);
        return combos.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getName());
            map.put("restaurantId", c.getRestaurant().getId());
            map.put("description", c.getDescription());
            map.put("imageUrl", c.getImageUrl());
            map.put("comboPrice", c.getComboPrice());
            map.put("originalPrice", c.getOriginalPrice());
            map.put("active", c.getActive());
            map.put("communityId", c.getCommunity().getId());
            map.put("createdAt", c.getCreatedAt());
            map.put("updatedAt", c.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> toResponse(FoodMenuItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getId());
        map.put("categoryId", item.getCategory().getId());
        map.put("restaurantId", item.getRestaurant().getId());
        map.put("name", item.getName());
        map.put("slug", item.getSlug());
        map.put("description", item.getDescription());
        map.put("imageUrl", item.getImageUrl());
        map.put("price", item.getPrice());
        map.put("discountedPrice", item.getDiscountedPrice());
        map.put("isVeg", item.getIsVeg());
        map.put("isVegan", item.getIsVegan());
        map.put("isJain", item.getIsJain());
        map.put("spiceLevel", item.getSpiceLevel());
        map.put("calories", item.getCalories());
        map.put("protein", item.getProtein());
        map.put("carbs", item.getCarbs());
        map.put("fat", item.getFat());
        map.put("fiber", item.getFiber());
        map.put("preparationTime", item.getPreparationTime());
        map.put("isAvailable", item.getIsAvailable());
        map.put("isFeatured", item.getIsFeatured());
        map.put("isBestseller", item.getIsBestseller());
        map.put("sortOrder", item.getSortOrder());
        map.put("tags", item.getTags());
        map.put("communityId", item.getCommunity().getId());
        map.put("createdAt", item.getCreatedAt());
        map.put("updatedAt", item.getUpdatedAt());

        List<FoodMenuItemVariant> variants = variantRepo.findByItemId(item.getId());
        map.put("variants", variants.stream().map(v -> {
            Map<String, Object> vm = new HashMap<>();
            vm.put("id", v.getId());
            vm.put("variantName", v.getVariantName());
            vm.put("price", v.getPrice());
            vm.put("isDefault", v.getIsDefault());
            vm.put("createdAt", v.getCreatedAt());
            vm.put("updatedAt", v.getUpdatedAt());
            return vm;
        }).collect(Collectors.toList()));

        List<FoodMenuItemAddon> addons = addonRepo.findByItemId(item.getId());
        map.put("addons", addons.stream().map(a -> {
            Map<String, Object> am = new HashMap<>();
            am.put("id", a.getId());
            am.put("addonGroupName", a.getAddonGroupName());
            am.put("addonName", a.getAddonName());
            am.put("price", a.getPrice());
            am.put("isDefault", a.getIsDefault());
            am.put("maxQuantity", a.getMaxQuantity());
            am.put("createdAt", a.getCreatedAt());
            am.put("updatedAt", a.getUpdatedAt());
            return am;
        }).collect(Collectors.toList()));

        return map;
    }
}
