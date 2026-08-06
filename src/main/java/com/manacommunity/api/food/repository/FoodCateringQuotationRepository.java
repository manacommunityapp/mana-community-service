package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCateringQuotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodCateringQuotationRepository extends JpaRepository<FoodCateringQuotation, Long> {

    List<FoodCateringQuotation> findByRequestId(Long requestId);

    Optional<FoodCateringQuotation> findByRequestIdAndCatererId(Long requestId, Long catererId);
}
