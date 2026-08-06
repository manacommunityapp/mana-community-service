package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodGroceryWishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodGroceryWishlistRepository extends JpaRepository<FoodGroceryWishlist, Long> {

    List<FoodGroceryWishlist> findByUserIdAndCommunityId(Long userId, Long communityId);

    void deleteByUserIdAndProductId(Long userId, Long productId);
}
