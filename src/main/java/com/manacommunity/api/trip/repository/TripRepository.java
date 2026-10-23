package com.manacommunity.api.trip.repository;

import com.manacommunity.api.trip.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, String> {
    List<Trip> findByCategoryIgnoreCaseOrderByDepartureDateAsc(String category);
    List<Trip> findAllByOrderByCreatedAtDesc();
}