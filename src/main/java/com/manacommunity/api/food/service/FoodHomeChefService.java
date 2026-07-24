package com.manacommunity.api.food.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.food.entity.FoodHomeChef;
import com.manacommunity.api.food.entity.FoodHomeChefMenu;
import com.manacommunity.api.food.entity.FoodHomeChefPayout;
import com.manacommunity.api.food.entity.FoodHomeChefReview;
import com.manacommunity.api.food.repository.FoodHomeChefMenuRepository;
import com.manacommunity.api.food.repository.FoodHomeChefPayoutRepository;
import com.manacommunity.api.food.repository.FoodHomeChefRepository;
import com.manacommunity.api.food.repository.FoodHomeChefReviewRepository;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FoodHomeChefService {

    private final FoodHomeChefRepository chefRepo;
    private final FoodHomeChefMenuRepository menuRepo;
    private final FoodHomeChefReviewRepository reviewRepo;
    private final FoodHomeChefPayoutRepository payoutRepo;

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getHomeChefs(Long communityId, String status, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return chefRepo.searchByCommunity(communityId, search, pageable)
                    .map(this::toResponse);
        }
        if (status != null && !status.isBlank()) {
            FoodHomeChef.ChefStatus chefStatus = FoodHomeChef.ChefStatus.valueOf(status);
            return chefRepo.findByCommunityIdAndStatus(communityId, chefStatus, pageable)
                    .map(this::toResponse);
        }
        return chefRepo.findByCommunityId(communityId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getHomeChefById(Long id, Long communityId) {
        FoodHomeChef chef = chefRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("HomeChef", id));
        return toResponse(chef);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMyProfile(Long userId, Long communityId) {
        FoodHomeChef chef = chefRepo.findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("HomeChef", "userId", userId.toString()));
        return toResponse(chef);
    }

    @Transactional
    public Map<String, Object> registerHomeChef(Map<String, Object> request, AppUser user, Community community) {
        FoodHomeChef chef = FoodHomeChef.builder()
                .user(user)
                .kitchenName((String) request.get("kitchenName"))
                .description((String) request.get("description"))
                .speciality((String) request.get("speciality"))
                .cuisineTypes((String) request.get("cuisineTypes"))
                .fssaiLicense((String) request.get("fssaiLicense"))
                .profileImageUrl((String) request.get("profileImageUrl"))
                .coverImageUrl((String) request.get("coverImageUrl"))
                .community(community)
                .build();

        if (request.containsKey("maxOrdersPerDay")) {
            chef.setMaxOrdersPerDay((Integer) request.get("maxOrdersPerDay"));
        }
        if (request.containsKey("commissionRate")) {
            chef.setCommissionRate(new BigDecimal(request.get("commissionRate").toString()));
        }

        FoodHomeChef saved = chefRepo.save(chef);
        return toResponse(saved);
    }

    @Transactional
    public Map<String, Object> updateHomeChef(Long id, Map<String, Object> request, Long communityId) {
        FoodHomeChef chef = chefRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("HomeChef", id));

        if (request.containsKey("kitchenName")) {
            chef.setKitchenName((String) request.get("kitchenName"));
        }
        if (request.containsKey("description")) {
            chef.setDescription((String) request.get("description"));
        }
        if (request.containsKey("speciality")) {
            chef.setSpeciality((String) request.get("speciality"));
        }
        if (request.containsKey("cuisineTypes")) {
            chef.setCuisineTypes((String) request.get("cuisineTypes"));
        }
        if (request.containsKey("fssaiLicense")) {
            chef.setFssaiLicense((String) request.get("fssaiLicense"));
        }
        if (request.containsKey("profileImageUrl")) {
            chef.setProfileImageUrl((String) request.get("profileImageUrl"));
        }
        if (request.containsKey("coverImageUrl")) {
            chef.setCoverImageUrl((String) request.get("coverImageUrl"));
        }
        if (request.containsKey("maxOrdersPerDay")) {
            chef.setMaxOrdersPerDay((Integer) request.get("maxOrdersPerDay"));
        }
        if (request.containsKey("commissionRate")) {
            chef.setCommissionRate(new BigDecimal(request.get("commissionRate").toString()));
        }
        if (request.containsKey("status")) {
            chef.setStatus(FoodHomeChef.ChefStatus.valueOf((String) request.get("status")));
        }
        if (request.containsKey("availabilityStatus")) {
            chef.setAvailabilityStatus(FoodHomeChef.AvailabilityStatus.valueOf((String) request.get("availabilityStatus")));
        }

        FoodHomeChef saved = chefRepo.save(chef);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getMenu(Long chefId, Pageable pageable) {
        return menuRepo.findByChefId(chefId, pageable).map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("chefId", m.getChef().getId());
            map.put("name", m.getName());
            map.put("description", m.getDescription());
            map.put("imageUrl", m.getImageUrl());
            map.put("price", m.getPrice());
            map.put("category", m.getCategory());
            map.put("isVeg", m.getIsVeg());
            map.put("calories", m.getCalories());
            map.put("protein", m.getProtein());
            map.put("preparationTime", m.getPreparationTime());
            map.put("availableDays", m.getAvailableDays());
            map.put("orderBeforeTime", m.getOrderBeforeTime());
            map.put("maxQuantity", m.getMaxQuantity());
            map.put("sortOrder", m.getSortOrder());
            map.put("active", m.getActive());
            map.put("communityId", m.getCommunity().getId());
            map.put("createdAt", m.getCreatedAt());
            map.put("updatedAt", m.getUpdatedAt());
            return map;
        });
    }

    @Transactional
    public Map<String, Object> addMenuItem(Long chefId, Map<String, Object> request, Long communityId) {
        FoodHomeChef chef = chefRepo.findByIdAndCommunityId(chefId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("HomeChef", chefId));

        FoodHomeChefMenu menu = FoodHomeChefMenu.builder()
                .chef(chef)
                .name((String) request.get("name"))
                .description((String) request.get("description"))
                .imageUrl((String) request.get("imageUrl"))
                .price(new BigDecimal(request.get("price").toString()))
                .category((String) request.get("category"))
                .community(chef.getCommunity())
                .build();

        if (request.containsKey("isVeg")) {
            menu.setIsVeg((Boolean) request.get("isVeg"));
        }
        if (request.containsKey("calories")) {
            menu.setCalories((Integer) request.get("calories"));
        }
        if (request.containsKey("protein")) {
            menu.setProtein(new BigDecimal(request.get("protein").toString()));
        }
        if (request.containsKey("preparationTime")) {
            menu.setPreparationTime((Integer) request.get("preparationTime"));
        }
        if (request.containsKey("availableDays")) {
            menu.setAvailableDays((String) request.get("availableDays"));
        }
        if (request.containsKey("orderBeforeTime")) {
            menu.setOrderBeforeTime(LocalTime.parse((String) request.get("orderBeforeTime")));
        }
        if (request.containsKey("maxQuantity")) {
            menu.setMaxQuantity((Integer) request.get("maxQuantity"));
        }
        if (request.containsKey("sortOrder")) {
            menu.setSortOrder((Integer) request.get("sortOrder"));
        }

        FoodHomeChefMenu saved = menuRepo.save(menu);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("chefId", saved.getChef().getId());
        map.put("name", saved.getName());
        map.put("description", saved.getDescription());
        map.put("imageUrl", saved.getImageUrl());
        map.put("price", saved.getPrice());
        map.put("category", saved.getCategory());
        map.put("isVeg", saved.getIsVeg());
        map.put("calories", saved.getCalories());
        map.put("protein", saved.getProtein());
        map.put("preparationTime", saved.getPreparationTime());
        map.put("availableDays", saved.getAvailableDays());
        map.put("orderBeforeTime", saved.getOrderBeforeTime());
        map.put("maxQuantity", saved.getMaxQuantity());
        map.put("sortOrder", saved.getSortOrder());
        map.put("active", saved.getActive());
        map.put("communityId", saved.getCommunity().getId());
        map.put("createdAt", saved.getCreatedAt());
        map.put("updatedAt", saved.getUpdatedAt());
        return map;
    }

    @Transactional
    public Map<String, Object> updateMenuItem(Long menuId, Map<String, Object> request) {
        FoodHomeChefMenu menu = menuRepo.findById(menuId)
                .orElseThrow(() -> new ResourceNotFoundException("HomeChefMenu", menuId));

        if (request.containsKey("name")) {
            menu.setName((String) request.get("name"));
        }
        if (request.containsKey("description")) {
            menu.setDescription((String) request.get("description"));
        }
        if (request.containsKey("imageUrl")) {
            menu.setImageUrl((String) request.get("imageUrl"));
        }
        if (request.containsKey("price")) {
            menu.setPrice(new BigDecimal(request.get("price").toString()));
        }
        if (request.containsKey("category")) {
            menu.setCategory((String) request.get("category"));
        }
        if (request.containsKey("isVeg")) {
            menu.setIsVeg((Boolean) request.get("isVeg"));
        }
        if (request.containsKey("calories")) {
            menu.setCalories((Integer) request.get("calories"));
        }
        if (request.containsKey("protein")) {
            menu.setProtein(new BigDecimal(request.get("protein").toString()));
        }
        if (request.containsKey("preparationTime")) {
            menu.setPreparationTime((Integer) request.get("preparationTime"));
        }
        if (request.containsKey("availableDays")) {
            menu.setAvailableDays((String) request.get("availableDays"));
        }
        if (request.containsKey("orderBeforeTime")) {
            menu.setOrderBeforeTime(LocalTime.parse((String) request.get("orderBeforeTime")));
        }
        if (request.containsKey("maxQuantity")) {
            menu.setMaxQuantity((Integer) request.get("maxQuantity"));
        }
        if (request.containsKey("sortOrder")) {
            menu.setSortOrder((Integer) request.get("sortOrder"));
        }
        if (request.containsKey("active")) {
            menu.setActive((Boolean) request.get("active"));
        }

        FoodHomeChefMenu saved = menuRepo.save(menu);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("chefId", saved.getChef().getId());
        map.put("name", saved.getName());
        map.put("description", saved.getDescription());
        map.put("imageUrl", saved.getImageUrl());
        map.put("price", saved.getPrice());
        map.put("category", saved.getCategory());
        map.put("isVeg", saved.getIsVeg());
        map.put("calories", saved.getCalories());
        map.put("protein", saved.getProtein());
        map.put("preparationTime", saved.getPreparationTime());
        map.put("availableDays", saved.getAvailableDays());
        map.put("orderBeforeTime", saved.getOrderBeforeTime());
        map.put("maxQuantity", saved.getMaxQuantity());
        map.put("sortOrder", saved.getSortOrder());
        map.put("active", saved.getActive());
        map.put("communityId", saved.getCommunity().getId());
        map.put("createdAt", saved.getCreatedAt());
        map.put("updatedAt", saved.getUpdatedAt());
        return map;
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getReviews(Long chefId, Pageable pageable) {
        return reviewRepo.findByChefId(chefId, pageable).map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("chefId", r.getChef().getId());
            map.put("userId", r.getUser().getId());
            map.put("orderId", r.getOrderId());
            map.put("rating", r.getRating());
            map.put("reviewText", r.getReviewText());
            map.put("tasteRating", r.getTasteRating());
            map.put("hygieneRating", r.getHygieneRating());
            map.put("packagingRating", r.getPackagingRating());
            map.put("valueRating", r.getValueRating());
            map.put("images", r.getImages());
            map.put("communityId", r.getCommunity().getId());
            map.put("createdAt", r.getCreatedAt());
            return map;
        });
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getPayouts(Long chefId, Pageable pageable) {
        return payoutRepo.findByChefId(chefId, pageable).map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("chefId", p.getChef().getId());
            map.put("amount", p.getAmount());
            map.put("periodStart", p.getPeriodStart());
            map.put("periodEnd", p.getPeriodEnd());
            map.put("status", p.getStatus() != null ? p.getStatus().name() : null);
            map.put("transactionRef", p.getTransactionRef());
            map.put("paidAt", p.getPaidAt());
            map.put("createdAt", p.getCreatedAt());
            map.put("updatedAt", p.getUpdatedAt());
            return map;
        });
    }

    private Map<String, Object> toResponse(FoodHomeChef chef) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", chef.getId());
        map.put("userId", chef.getUser().getId());
        map.put("kitchenName", chef.getKitchenName());
        map.put("description", chef.getDescription());
        map.put("speciality", chef.getSpeciality());
        map.put("cuisineTypes", chef.getCuisineTypes());
        map.put("fssaiLicense", chef.getFssaiLicense());
        map.put("status", chef.getStatus() != null ? chef.getStatus().name() : null);
        map.put("verificationStatus", chef.getVerificationStatus());
        map.put("maxOrdersPerDay", chef.getMaxOrdersPerDay());
        map.put("rating", chef.getRating());
        map.put("totalRatings", chef.getTotalRatings());
        map.put("totalOrders", chef.getTotalOrders());
        map.put("revenueTotal", chef.getRevenueTotal());
        map.put("commissionRate", chef.getCommissionRate());
        map.put("profileImageUrl", chef.getProfileImageUrl());
        map.put("coverImageUrl", chef.getCoverImageUrl());
        map.put("availabilityStatus", chef.getAvailabilityStatus() != null ? chef.getAvailabilityStatus().name() : null);
        map.put("communityId", chef.getCommunity().getId());
        map.put("createdAt", chef.getCreatedAt());
        map.put("updatedAt", chef.getUpdatedAt());
        return map;
    }
}
