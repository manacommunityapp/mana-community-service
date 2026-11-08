package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsMatchOfficial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsMatchOfficialRepository extends JpaRepository<SportsMatchOfficial, Long> {
    List<SportsMatchOfficial> findByMatchId(Long matchId);
    Optional<SportsMatchOfficial> findByMatchIdAndUserId(Long matchId, Long userId);
    boolean existsByMatchIdAndUserId(Long matchId, Long userId);
    boolean existsByMatchIdAndUserIdAndRole(Long matchId, Long userId, String role);
}
