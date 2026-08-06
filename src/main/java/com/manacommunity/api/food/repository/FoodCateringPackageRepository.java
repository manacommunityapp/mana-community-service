package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCateringPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodCateringPackageRepository extends JpaRepository<FoodCateringPackage, Long> {

    List<FoodCateringPackage> findByCatererIdAndActive(Long catererId, Boolean active);
}
