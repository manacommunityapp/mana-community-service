package com.manacommunity.api.food.service;

import com.manacommunity.api.food.entity.FoodRestaurantAnalytics;
import com.manacommunity.api.food.repository.FoodHomeChefRepository;
import com.manacommunity.api.food.repository.FoodOrderRepository;
import com.manacommunity.api.food.repository.FoodRestaurantAnalyticsRepository;
import com.manacommunity.api.food.repository.FoodRestaurantRepository;
import com.manacommunity.api.food.repository.FoodSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodAnalyticsService {

    private final FoodRestaurantAnalyticsRepository restaurantAnalyticsRepo;
    private final FoodRestaurantRepository restaurantRepo;
    private final FoodHomeChefRepository homeChefRepo;
    private final FoodOrderRepository orderRepo;
    private final FoodSubscriptionRepository subscriptionRepo;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRestaurantAnalytics(Long communityId, Long restaurantId, LocalDate startDate, LocalDate endDate) {
        List<FoodRestaurantAnalytics> analytics = restaurantAnalyticsRepo
                .findByRestaurantIdAndDateBetween(restaurantId, startDate, endDate);

        return analytics.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("restaurantId", a.getRestaurant().getId());
            map.put("date", a.getDate());
            map.put("totalOrders", a.getTotalOrders());
            map.put("totalRevenue", a.getTotalRevenue());
            map.put("avgOrderValue", a.getAvgOrderValue());
            map.put("newCustomers", a.getNewCustomers());
            map.put("repeatCustomers", a.getRepeatCustomers());
            map.put("avgPreparationTime", a.getAvgPreparationTime());
            map.put("avgDeliveryTime", a.getAvgDeliveryTime());
            map.put("cancellationRate", a.getCancellationRate());
            map.put("ratingAvg", a.getRatingAvg());
            map.put("communityId", a.getCommunity() != null ? a.getCommunity().getId() : null);
            map.put("createdAt", a.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getHomeChefAnalytics(Long communityId, Long chefId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("communityId", communityId);
        summary.put("chefId", chefId);
        summary.put("startDate", startDate);
        summary.put("endDate", endDate);
        summary.put("totalOrders", 0);
        summary.put("totalRevenue", BigDecimal.ZERO);
        summary.put("avgOrderValue", BigDecimal.ZERO);
        summary.put("avgRating", BigDecimal.ZERO);
        return summary;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCommunityTrends(Long communityId, int months) {
        List<Map<String, Object>> trends = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            Map<String, Object> month = new HashMap<>();
            month.put("month", monthStart.getMonth().name());
            month.put("year", monthStart.getYear());
            month.put("totalOrders", 0);
            month.put("totalRevenue", BigDecimal.ZERO);
            month.put("topCategory", "N/A");
            trends.add(month);
        }

        return trends;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFoodWaste(Long communityId, int months) {
        List<Map<String, Object>> wasteData = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            Map<String, Object> month = new HashMap<>();
            month.put("month", monthStart.getMonth().name());
            month.put("year", monthStart.getYear());
            month.put("wasteKg", BigDecimal.ZERO);
            month.put("wasteCost", BigDecimal.ZERO);
            month.put("topWasteCategory", "N/A");
            wasteData.add(month);
        }

        return wasteData;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRevenue(Long communityId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("communityId", communityId);
        summary.put("startDate", startDate);
        summary.put("endDate", endDate);
        summary.put("totalRevenue", BigDecimal.ZERO);
        summary.put("restaurantRevenue", BigDecimal.ZERO);
        summary.put("homeChefRevenue", BigDecimal.ZERO);
        summary.put("groceryRevenue", BigDecimal.ZERO);
        summary.put("subscriptionRevenue", BigDecimal.ZERO);
        summary.put("growthPercentage", BigDecimal.ZERO);
        return summary;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats(Long communityId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRestaurants", restaurantRepo.countByCommunityIdAndStatus(communityId,
                com.manacommunity.api.food.entity.FoodRestaurant.RestaurantStatus.APPROVED));
        stats.put("totalHomeChefs", homeChefRepo.count());
        stats.put("totalOrders", orderRepo.count());
        stats.put("totalSubscriptions", subscriptionRepo.count());
        return stats;
    }
}
