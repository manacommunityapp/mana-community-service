package com.manacommunity.api.trip.split.repository;

import com.manacommunity.api.trip.split.entity.TripExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TripExpenseRepository extends JpaRepository<TripExpense, Long> {

    List<TripExpense> findByTripIdOrderByExpenseDateDescIdDesc(String tripId);

    List<TripExpense> findByTripIdAndStatusIn(String tripId, Collection<String> statuses);
}
