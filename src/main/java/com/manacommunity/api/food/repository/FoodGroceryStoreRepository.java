package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodGroceryStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodGroceryStoreRepository extends JpaRepository<FoodGroceryStore, Long> {

    Page<FoodGroceryStore> findByCommunityId(Long communityId, Pageable pageable);

    Page<FoodGroceryStore> findByCommunityIdAndStatus(Long communityId, FoodGroceryStore.StoreStatus status, Pageable pageable);

    Optional<FoodGroceryStore> findByIdAndCommunityId(Long id, Long communityId);

    List<FoodGroceryStore> findByOwnerIdAndCommunityId(Long ownerId, Long communityId);
}
