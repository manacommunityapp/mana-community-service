package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsMatchOfficial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsMatchOfficialRepository extends JpaRepository<SportsMatchOfficial, Long> {
    List<SportsMatchOfficial> findByMatchId(Long matchId);
    Optional<SportsMatchOfficial> findByMatchIdAndUserId(Long matchId, Long userId);
    boolean existsByMatchIdAndUserId(Long matchId, Long userId);
    boolean existsByMatchIdAndUserIdAndRole(Long matchId, Long userId, String role);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SportsMatchOfficial o WHERE o.match.id IN " +
           "(SELECT m.id FROM SportsTournamentMatch m WHERE m.config.id = :configId)")
    void deleteByConfigId(@Param("configId") Long configId);
}
