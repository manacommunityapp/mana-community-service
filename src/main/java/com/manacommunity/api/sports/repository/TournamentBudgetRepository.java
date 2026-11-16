package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.TournamentBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TournamentBudgetRepository extends JpaRepository<TournamentBudget, Long> {

    Optional<TournamentBudget> findByTournamentId(Long tournamentId);

    List<TournamentBudget> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    @Query("SELECT b FROM TournamentBudget b JOIN FETCH b.lineItems WHERE b.tournament.id = :tournamentId")
    Optional<TournamentBudget> findByTournamentIdWithLineItems(@Param("tournamentId") Long tournamentId);

    boolean existsByTournamentId(Long tournamentId);
}
