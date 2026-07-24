package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCloudKitchenBrand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodCloudKitchenBrandRepository extends JpaRepository<FoodCloudKitchenBrand, Long> {

    List<FoodCloudKitchenBrand> findByKitchenId(Long kitchenId);

    List<FoodCloudKitchenBrand> findByKitchenIdAndStatus(Long kitchenId, FoodCloudKitchenBrand.BrandStatus status);
}
