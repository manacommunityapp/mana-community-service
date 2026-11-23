package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsMatchPostponement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SportsMatchPostponementRepository extends JpaRepository<SportsMatchPostponement, Long> {

    List<SportsMatchPostponement> findByMatchIdOrderByCreatedAtDesc(Long matchId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SportsMatchPostponement p WHERE p.match.id IN " +
           "(SELECT m.id FROM SportsTournamentMatch m WHERE m.config.id = :configId)")
    void deleteByConfigId(@Param("configId") Long configId);
}
