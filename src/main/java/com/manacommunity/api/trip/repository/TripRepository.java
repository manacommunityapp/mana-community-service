package com.manacommunity.api.trip.repository;

import com.manacommunity.api.trip.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByCommunityIdOrderByStartDateDesc(Long communityId);

    List<Trip> findByCommunityIdAndStatusOrderByStartDateAsc(Long communityId, Trip.TripStatus status);

    List<Trip> findByOrganizerIdOrderByStartDateDesc(Long organizerId);
}
