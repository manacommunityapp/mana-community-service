package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodHomeChef;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FoodHomeChefRepository extends JpaRepository<FoodHomeChef, Long> {

    Page<FoodHomeChef> findByCommunityId(Long communityId, Pageable pageable);

    Page<FoodHomeChef> findByCommunityIdAndStatus(Long communityId, FoodHomeChef.ChefStatus status, Pageable pageable);

    Optional<FoodHomeChef> findByIdAndCommunityId(Long id, Long communityId);

    Optional<FoodHomeChef> findByUserIdAndCommunityId(Long userId, Long communityId);

    @Query("SELECT c FROM FoodHomeChef c WHERE c.community.id = :communityId AND c.status = 'APPROVED' " +
           "AND (LOWER(c.kitchenName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<FoodHomeChef> searchByCommunity(@Param("communityId") Long communityId, @Param("query") String query, Pageable pageable);
}
