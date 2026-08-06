package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodDiningReservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FoodDiningReservationRepository extends JpaRepository<FoodDiningReservation, Long> {

    Page<FoodDiningReservation> findByUserIdAndCommunityId(Long userId, Long communityId, Pageable pageable);

    List<FoodDiningReservation> findByRestaurantIdAndDateAndStatus(Long restaurantId, LocalDate date, FoodDiningReservation.ReservationStatus status);

    Optional<FoodDiningReservation> findByIdAndCommunityId(Long id, Long communityId);
}
