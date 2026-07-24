package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodResidentAllergy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodResidentAllergyRepository extends JpaRepository<FoodResidentAllergy, Long> {

    List<FoodResidentAllergy> findByProfileId(Long profileId);
}
