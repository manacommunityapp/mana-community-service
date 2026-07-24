package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodGroupOrderParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodGroupOrderParticipantRepository extends JpaRepository<FoodGroupOrderParticipant, Long> {

    List<FoodGroupOrderParticipant> findByGroupOrderId(Long groupOrderId);
}
