package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodResidentProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FoodResidentProfileRepository extends JpaRepository<FoodResidentProfile, Long> {

    Optional<FoodResidentProfile> findByUserIdAndCommunityId(Long userId, Long communityId);

    List<FoodResidentProfile> findByUserId(Long userId);

    Page<FoodResidentProfile> findByCommunityId(Long communityId, Pageable pageable);
}
