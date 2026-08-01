package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCateringRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodCateringRequestRepository extends JpaRepository<FoodCateringRequest, Long> {

    Page<FoodCateringRequest> findByUserIdAndCommunityId(Long userId, Long communityId, Pageable pageable);

    Page<FoodCateringRequest> findByCommunityIdAndStatus(Long communityId, String status, Pageable pageable);

    Optional<FoodCateringRequest> findByIdAndCommunityId(Long id, Long communityId);
}
