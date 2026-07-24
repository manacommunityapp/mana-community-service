package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodLoyaltyTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodLoyaltyTransactionRepository extends JpaRepository<FoodLoyaltyTransaction, Long> {

    Page<FoodLoyaltyTransaction> findByMemberId(Long memberId, Pageable pageable);
}
