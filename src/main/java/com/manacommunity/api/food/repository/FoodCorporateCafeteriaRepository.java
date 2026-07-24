package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCorporateCafeteria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodCorporateCafeteriaRepository extends JpaRepository<FoodCorporateCafeteria, Long> {

    List<FoodCorporateCafeteria> findByAccountIdAndStatus(Long accountId, String status);

    Optional<FoodCorporateCafeteria> findByIdAndCommunityId(Long id, Long communityId);
}
