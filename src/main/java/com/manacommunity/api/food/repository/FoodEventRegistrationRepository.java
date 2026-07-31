package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodEventRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodEventRegistrationRepository extends JpaRepository<FoodEventRegistration, Long> {

    Page<FoodEventRegistration> findByEventId(Long eventId, Pageable pageable);

    Optional<FoodEventRegistration> findByEventIdAndUserId(Long eventId, Long userId);

    long countByEventIdAndStatus(Long eventId, String status);

    Optional<FoodEventRegistration> findByIdAndCommunityId(Long id, Long communityId);
}
