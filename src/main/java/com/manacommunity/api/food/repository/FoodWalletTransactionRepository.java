package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodWalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodWalletTransactionRepository extends JpaRepository<FoodWalletTransaction, Long> {

    Page<FoodWalletTransaction> findByWalletId(Long walletId, Pageable pageable);
}
