package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodLoyaltyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodLoyaltyMemberRepository extends JpaRepository<FoodLoyaltyMember, Long> {

    Optional<FoodLoyaltyMember> findByProgramIdAndUserId(Long programId, Long userId);

    List<FoodLoyaltyMember> findByUserIdAndCommunityId(Long userId, Long communityId);
}
