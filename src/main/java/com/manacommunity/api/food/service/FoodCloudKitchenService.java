package com.manacommunity.api.food.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.food.entity.FoodCloudKitchen;
import com.manacommunity.api.food.entity.FoodCloudKitchenAnalytics;
import com.manacommunity.api.food.entity.FoodCloudKitchenBrand;
import com.manacommunity.api.food.entity.FoodCloudKitchenSlot;
import com.manacommunity.api.food.repository.FoodCloudKitchenBrandRepository;
import com.manacommunity.api.food.repository.FoodCloudKitchenRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodCloudKitchenService {

    private final FoodCloudKitchenRepository kitchenRepo;
    private final FoodCloudKitchenBrandRepository brandRepo;

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> list(Long communityId, Pageable pageable) {
        return kitchenRepo.findByCommunityId(communityId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getById(Long communityId, Long id) {
        FoodCloudKitchen kitchen = kitchenRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("CloudKitchen", id));
        return toResponse(kitchen);
    }

    @Transactional
    public Map<String, Object> create(Long communityId, Map<String, Object> request, AppUser user) {
        Community community = user.getCommunity();
        FoodCloudKitchen kitchen = FoodCloudKitchen.builder()
                .name((String) request.get("name"))
                .description((String) request.get("description"))
                .address((String) request.get("address"))
                .owner(user)
                .licenseNumber((String) request.get("licenseNumber"))
                .community(community)
                .build();

        if (request.containsKey("latitude")) {
            kitchen.setLatitude(new BigDecimal(request.get("latitude").toString()));
        }
        if (request.containsKey("longitude")) {
            kitchen.setLongitude(new BigDecimal(request.get("longitude").toString()));
        }
        if (request.containsKey("capacity")) {
            kitchen.setCapacity((Integer) request.get("capacity"));
        }
        if (request.containsKey("kitchenType")) {
            kitchen.setKitchenType(FoodCloudKitchen.KitchenType.valueOf((String) request.get("kitchenType")));
        }
        if (request.containsKey("rent")) {
            kitchen.setRent(new BigDecimal(request.get("rent").toString()));
        }

        FoodCloudKitchen saved = kitchenRepo.save(kitchen);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBrands(Long communityId, Long kitchenId) {
        kitchenRepo.findByIdAndCommunityId(kitchenId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("CloudKitchen", kitchenId));
        List<FoodCloudKitchenBrand> brands = brandRepo.findByKitchenId(kitchenId);
        return brands.stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("kitchenId", b.getKitchen().getId());
            map.put("brandName", b.getBrandName());
            map.put("slug", b.getSlug());
            map.put("description", b.getDescription());
            map.put("logoUrl", b.getLogoUrl());
            map.put("cuisineType", b.getCuisineType());
            map.put("status", b.getStatus() != null ? b.getStatus().name() : null);
            map.put("rating", b.getRating());
            map.put("communityId", b.getCommunity() != null ? b.getCommunity().getId() : null);
            map.put("createdAt", b.getCreatedAt());
            map.put("updatedAt", b.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createBrand(Long communityId, Long kitchenId, Map<String, Object> request) {
        FoodCloudKitchen kitchen = kitchenRepo.findByIdAndCommunityId(kitchenId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("CloudKitchen", kitchenId));

        String brandName = (String) request.get("brandName");
        String slug = brandName.toLowerCase().replaceAll("\\s+", "-");

        FoodCloudKitchenBrand brand = FoodCloudKitchenBrand.builder()
                .kitchen(kitchen)
                .brandName(brandName)
                .slug(slug)
                .description((String) request.get("description"))
                .logoUrl((String) request.get("logoUrl"))
                .cuisineType((String) request.get("cuisineType"))
                .status(FoodCloudKitchenBrand.BrandStatus.ACTIVE)
                .community(kitchen.getCommunity())
                .build();

        FoodCloudKitchenBrand saved = brandRepo.save(brand);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("kitchenId", saved.getKitchen().getId());
        map.put("brandName", saved.getBrandName());
        map.put("slug", saved.getSlug());
        map.put("description", saved.getDescription());
        map.put("logoUrl", saved.getLogoUrl());
        map.put("cuisineType", saved.getCuisineType());
        map.put("status", saved.getStatus() != null ? saved.getStatus().name() : null);
        map.put("rating", saved.getRating());
        map.put("communityId", saved.getCommunity() != null ? saved.getCommunity().getId() : null);
        map.put("createdAt", saved.getCreatedAt());
        map.put("updatedAt", saved.getUpdatedAt());
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSlots(Long communityId, Long kitchenId) {
        // TODO: Implement when FoodCloudKitchenSlotRepository is available
        return new ArrayList<>();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAnalytics(Long communityId, Long kitchenId, LocalDate startDate, LocalDate endDate) {
        // TODO: Implement when FoodCloudKitchenAnalyticsRepository is available
        return new ArrayList<>();
    }

    private Map<String, Object> toResponse(FoodCloudKitchen kitchen) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", kitchen.getId());
        map.put("name", kitchen.getName());
        map.put("description", kitchen.getDescription());
        map.put("address", kitchen.getAddress());
        map.put("latitude", kitchen.getLatitude());
        map.put("longitude", kitchen.getLongitude());
        map.put("ownerId", kitchen.getOwner().getId());
        map.put("capacity", kitchen.getCapacity());
        map.put("status", kitchen.getStatus() != null ? kitchen.getStatus().name() : null);
        map.put("licenseNumber", kitchen.getLicenseNumber());
        map.put("kitchenType", kitchen.getKitchenType() != null ? kitchen.getKitchenType().name() : null);
        map.put("rent", kitchen.getRent());
        map.put("communityId", kitchen.getCommunity() != null ? kitchen.getCommunity().getId() : null);
        map.put("createdAt", kitchen.getCreatedAt());
        map.put("updatedAt", kitchen.getUpdatedAt());
        return map;
    }
}
