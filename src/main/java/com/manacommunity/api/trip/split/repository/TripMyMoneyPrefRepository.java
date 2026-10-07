package com.manacommunity.api.trip.split.repository;

import com.manacommunity.api.trip.split.entity.TripMyMoneyPref;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripMyMoneyPrefRepository extends JpaRepository<TripMyMoneyPref, Long> {

    Optional<TripMyMoneyPref> findByTripIdAndUserId(String tripId, Long userId);

    List<TripMyMoneyPref> findByTripId(String tripId);
}
