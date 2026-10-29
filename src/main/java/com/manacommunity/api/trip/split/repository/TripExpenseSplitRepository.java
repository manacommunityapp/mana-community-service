package com.manacommunity.api.trip.split.repository;

import com.manacommunity.api.trip.split.entity.TripExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TripExpenseSplitRepository extends JpaRepository<TripExpenseSplit, Long> {

    List<TripExpenseSplit> findByExpenseId(Long expenseId);

    List<TripExpenseSplit> findByExpenseIdIn(Collection<Long> expenseIds);

    void deleteByExpenseId(Long expenseId);
}
