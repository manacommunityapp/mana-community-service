package com.manacommunity.api.trip.split.repository;

import com.manacommunity.api.trip.split.entity.TripBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripBudgetRepository extends JpaRepository<TripBudget, String> {
}
