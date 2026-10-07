package com.manacommunity.api.trip.split.repository;

import com.manacommunity.api.trip.split.entity.TripExpenseParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripExpenseParticipantRepository extends JpaRepository<TripExpenseParticipant, Long> {

    List<TripExpenseParticipant> findByExpenseId(Long expenseId);

    void deleteByExpenseId(Long expenseId);
}
