package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.MealRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MealRegistrationRepository extends JpaRepository<MealRegistration, Long> {

    List<MealRegistration> findByEventIdAndUserId(Long eventId, Long userId);

    void deleteByEventIdAndUserId(Long eventId, Long userId);

    @Query("SELECT m FROM MealRegistration m WHERE m.event.id = :eventId ORDER BY m.mealDate, m.mealType")
    List<MealRegistration> findByEventIdOrdered(@Param("eventId") Long eventId);
}
