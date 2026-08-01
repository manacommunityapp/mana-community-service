package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodRestaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FoodRestaurantRepository extends JpaRepository<FoodRestaurant, Long> {

    Page<FoodRestaurant> findByCommunityId(Long communityId, Pageable pageable);

    Page<FoodRestaurant> findByCommunityIdAndStatus(Long communityId, FoodRestaurant.RestaurantStatus status, Pageable pageable);

    Optional<FoodRestaurant> findByIdAndCommunityId(Long id, Long communityId);

    List<FoodRestaurant> findByOwnerIdAndCommunityId(Long ownerId, Long communityId);

    @Query("SELECT r FROM FoodRestaurant r WHERE r.community.id = :communityId AND r.status = 'APPROVED' " +
           "AND (LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<FoodRestaurant> searchByCommunity(@Param("communityId") Long communityId, @Param("query") String query, Pageable pageable);

    long countByCommunityIdAndStatus(Long communityId, FoodRestaurant.RestaurantStatus status);

    List<FoodRestaurant> findByCommunityIdAndFeatured(Long communityId, Boolean featured);
}
