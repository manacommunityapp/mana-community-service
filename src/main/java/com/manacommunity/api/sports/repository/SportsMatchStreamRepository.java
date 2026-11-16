package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsMatchStream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SportsMatchStreamRepository extends JpaRepository<SportsMatchStream, Long> {
    Optional<SportsMatchStream> findByMatchId(Long matchId);
}
