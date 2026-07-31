package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodHomeChefPayout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodHomeChefPayoutRepository extends JpaRepository<FoodHomeChefPayout, Long> {

    List<FoodHomeChefPayout> findByChefIdAndStatus(Long chefId, FoodHomeChefPayout.PayoutStatus status);

    Page<FoodHomeChefPayout> findByChefId(Long chefId, Pageable pageable);
}
