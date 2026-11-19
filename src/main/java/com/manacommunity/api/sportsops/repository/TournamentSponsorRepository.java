package com.manacommunity.api.sportsops.repository;

import com.manacommunity.api.sportsops.entity.TournamentSponsor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TournamentSponsorRepository extends JpaRepository<TournamentSponsor, Long> {
    List<TournamentSponsor> findByTournamentId(Long tournamentId);
    List<TournamentSponsor> findByCommunityIdAndTournamentId(Long communityId, Long tournamentId);
}
