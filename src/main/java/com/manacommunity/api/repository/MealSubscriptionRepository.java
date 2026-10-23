package com.manacommunity.api.repository;

import com.manacommunity.api.model.MealSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MealSubscriptionRepository extends JpaRepository<MealSubscription, Long> {

    List<MealSubscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<MealSubscription> findByIdAndUserId(Long id, Long userId);
}
