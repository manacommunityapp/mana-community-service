package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsMatchPostponement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsMatchPostponementRepository extends JpaRepository<SportsMatchPostponement, Long> {

    List<SportsMatchPostponement> findByMatchIdOrderByCreatedAtDesc(Long matchId);
}
