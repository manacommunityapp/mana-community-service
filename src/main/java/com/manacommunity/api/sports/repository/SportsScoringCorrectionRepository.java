package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsScoringCorrection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SportsScoringCorrectionRepository extends JpaRepository<SportsScoringCorrection, Long> {
    List<SportsScoringCorrection> findByMatchIdOrderByCreatedAtDesc(Long matchId);
}
