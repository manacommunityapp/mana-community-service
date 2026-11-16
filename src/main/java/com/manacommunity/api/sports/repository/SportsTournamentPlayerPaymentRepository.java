package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsTournamentPlayerPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SportsTournamentPlayerPaymentRepository extends JpaRepository<SportsTournamentPlayerPayment, Long> {
    List<SportsTournamentPlayerPayment> findByTournamentId(Long tournamentId);
    Optional<SportsTournamentPlayerPayment> findByTournamentIdAndPlayerKey(Long tournamentId, String playerKey);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM SportsTournamentPlayerPayment p WHERE p.tournament.id = :tournamentId")
    BigDecimal sumAmountByTournament(@Param("tournamentId") Long tournamentId);
}
