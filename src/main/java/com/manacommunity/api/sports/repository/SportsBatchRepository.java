package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SportsBatchRepository extends JpaRepository<SportsBatch, Long> {

    List<SportsBatch> findByIsActiveTrue();

    List<SportsBatch> findByVenueIdAndIsActiveTrue(Long venueId);

    List<SportsBatch> findByCoachIdAndIsActiveTrue(Long coachId);
}
