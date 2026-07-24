package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodWalletRepository extends JpaRepository<FoodWallet, Long> {

    Optional<FoodWallet> findByUserIdAndCommunityId(Long userId, Long communityId);
}
