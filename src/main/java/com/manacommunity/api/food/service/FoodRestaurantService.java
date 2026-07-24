package com.manacommunity.api.food.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.food.entity.FoodRestaurant;
import com.manacommunity.api.food.repository.FoodRestaurantRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FoodRestaurantService {

    private final FoodRestaurantRepository restaurantRepo;

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getRestaurants(Long communityId, String status, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return restaurantRepo.searchByCommunity(communityId, search, pageable)
                    .map(this::toResponse);
        }
        if (status != null && !status.isBlank()) {
            FoodRestaurant.RestaurantStatus restaurantStatus = FoodRestaurant.RestaurantStatus.valueOf(status);
            return restaurantRepo.findByCommunityIdAndStatus(communityId, restaurantStatus, pageable)
                    .map(this::toResponse);
        }
        return restaurantRepo.findByCommunityId(communityId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRestaurantById(Long id, Long communityId) {
        FoodRestaurant restaurant = restaurantRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
        return toResponse(restaurant);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMyRestaurant(Long userId, Long communityId) {
        List<FoodRestaurant> restaurants = restaurantRepo.findByOwnerIdAndCommunityId(userId, communityId);
        if (restaurants.isEmpty()) {
            throw new ResourceNotFoundException("Restaurant", "ownerId", userId.toString());
        }
        return toResponse(restaurants.get(0));
    }

    @Transactional
    public Map<String, Object> createRestaurant(Map<String, Object> request, AppUser user, Community community) {
        String name = (String) request.get("name");
        String slug = name.toLowerCase().replaceAll("\\s+", "-");

        FoodRestaurant restaurant = FoodRestaurant.builder()
                .name(name)
                .slug(slug)
                .description((String) request.get("description"))
                .cuisineTypes((String) request.get("cuisineTypes"))
                .address((String) request.get("address"))
                .phone((String) request.get("phone"))
                .email((String) request.get("email"))
                .logoUrl((String) request.get("logoUrl"))
                .coverImageUrl((String) request.get("coverImageUrl"))
                .fssaiLicense((String) request.get("fssaiLicense"))
                .gstNumber((String) request.get("gstNumber"))
                .owner(user)
                .community(community)
                .build();

        if (request.containsKey("latitude")) {
            restaurant.setLatitude(new BigDecimal(request.get("latitude").toString()));
        }
        if (request.containsKey("longitude")) {
            restaurant.setLongitude(new BigDecimal(request.get("longitude").toString()));
        }
        if (request.containsKey("deliveryEnabled")) {
            restaurant.setDeliveryEnabled((Boolean) request.get("deliveryEnabled"));
        }
        if (request.containsKey("takeawayEnabled")) {
            restaurant.setTakeawayEnabled((Boolean) request.get("takeawayEnabled"));
        }
        if (request.containsKey("dineInEnabled")) {
            restaurant.setDineInEnabled((Boolean) request.get("dineInEnabled"));
        }
        if (request.containsKey("minOrderAmount")) {
            restaurant.setMinOrderAmount(new BigDecimal(request.get("minOrderAmount").toString()));
        }
        if (request.containsKey("avgDeliveryTime")) {
            restaurant.setAvgDeliveryTime((Integer) request.get("avgDeliveryTime"));
        }
        if (request.containsKey("openingTime")) {
            restaurant.setOpeningTime(LocalTime.parse((String) request.get("openingTime")));
        }
        if (request.containsKey("closingTime")) {
            restaurant.setClosingTime(LocalTime.parse((String) request.get("closingTime")));
        }

        FoodRestaurant saved = restaurantRepo.save(restaurant);
        return toResponse(saved);
    }

    @Transactional
    public Map<String, Object> updateRestaurant(Long id, Map<String, Object> request, Long communityId) {
        FoodRestaurant restaurant = restaurantRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));

        if (request.containsKey("name")) {
            restaurant.setName((String) request.get("name"));
            restaurant.setSlug(((String) request.get("name")).toLowerCase().replaceAll("\\s+", "-"));
        }
        if (request.containsKey("description")) {
            restaurant.setDescription((String) request.get("description"));
        }
        if (request.containsKey("cuisineTypes")) {
            restaurant.setCuisineTypes((String) request.get("cuisineTypes"));
        }
        if (request.containsKey("address")) {
            restaurant.setAddress((String) request.get("address"));
        }
        if (request.containsKey("phone")) {
            restaurant.setPhone((String) request.get("phone"));
        }
        if (request.containsKey("email")) {
            restaurant.setEmail((String) request.get("email"));
        }
        if (request.containsKey("logoUrl")) {
            restaurant.setLogoUrl((String) request.get("logoUrl"));
        }
        if (request.containsKey("coverImageUrl")) {
            restaurant.setCoverImageUrl((String) request.get("coverImageUrl"));
        }
        if (request.containsKey("fssaiLicense")) {
            restaurant.setFssaiLicense((String) request.get("fssaiLicense"));
        }
        if (request.containsKey("gstNumber")) {
            restaurant.setGstNumber((String) request.get("gstNumber"));
        }
        if (request.containsKey("latitude")) {
            restaurant.setLatitude(new BigDecimal(request.get("latitude").toString()));
        }
        if (request.containsKey("longitude")) {
            restaurant.setLongitude(new BigDecimal(request.get("longitude").toString()));
        }
        if (request.containsKey("deliveryEnabled")) {
            restaurant.setDeliveryEnabled((Boolean) request.get("deliveryEnabled"));
        }
        if (request.containsKey("takeawayEnabled")) {
            restaurant.setTakeawayEnabled((Boolean) request.get("takeawayEnabled"));
        }
        if (request.containsKey("dineInEnabled")) {
            restaurant.setDineInEnabled((Boolean) request.get("dineInEnabled"));
        }
        if (request.containsKey("minOrderAmount")) {
            restaurant.setMinOrderAmount(new BigDecimal(request.get("minOrderAmount").toString()));
        }
        if (request.containsKey("avgDeliveryTime")) {
            restaurant.setAvgDeliveryTime((Integer) request.get("avgDeliveryTime"));
        }
        if (request.containsKey("openingTime")) {
            restaurant.setOpeningTime(LocalTime.parse((String) request.get("openingTime")));
        }
        if (request.containsKey("closingTime")) {
            restaurant.setClosingTime(LocalTime.parse((String) request.get("closingTime")));
        }

        FoodRestaurant saved = restaurantRepo.save(restaurant);
        return toResponse(saved);
    }

    @Transactional
    public Map<String, Object> updateStatus(Long id, String status, Long communityId) {
        FoodRestaurant restaurant = restaurantRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
        restaurant.setStatus(FoodRestaurant.RestaurantStatus.valueOf(status));
        FoodRestaurant saved = restaurantRepo.save(restaurant);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFeaturedRestaurants(Long communityId) {
        List<FoodRestaurant> restaurants = restaurantRepo.findByCommunityIdAndFeatured(communityId, true);
        return restaurants.stream().map(this::toResponse).toList();
    }

    private Map<String, Object> toResponse(FoodRestaurant r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("name", r.getName());
        map.put("slug", r.getSlug());
        map.put("description", r.getDescription());
        map.put("cuisineTypes", r.getCuisineTypes());
        map.put("address", r.getAddress());
        map.put("latitude", r.getLatitude());
        map.put("longitude", r.getLongitude());
        map.put("phone", r.getPhone());
        map.put("email", r.getEmail());
        map.put("logoUrl", r.getLogoUrl());
        map.put("coverImageUrl", r.getCoverImageUrl());
        map.put("fssaiLicense", r.getFssaiLicense());
        map.put("gstNumber", r.getGstNumber());
        map.put("status", r.getStatus() != null ? r.getStatus().name() : null);
        map.put("rating", r.getRating());
        map.put("totalRatings", r.getTotalRatings());
        map.put("commissionRate", r.getCommissionRate());
        map.put("openingTime", r.getOpeningTime());
        map.put("closingTime", r.getClosingTime());
        map.put("deliveryEnabled", r.getDeliveryEnabled());
        map.put("takeawayEnabled", r.getTakeawayEnabled());
        map.put("dineInEnabled", r.getDineInEnabled());
        map.put("minOrderAmount", r.getMinOrderAmount());
        map.put("avgDeliveryTime", r.getAvgDeliveryTime());
        map.put("featured", r.getFeatured());
        map.put("verified", r.getVerified());
        map.put("ownerId", r.getOwner().getId());
        map.put("communityId", r.getCommunity().getId());
        map.put("createdAt", r.getCreatedAt());
        map.put("updatedAt", r.getUpdatedAt());
        return map;
    }
}
