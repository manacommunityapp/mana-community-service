package com.manacommunity.api.repository;

import com.manacommunity.api.model.TournamentAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentAnnouncementRepository extends JpaRepository<TournamentAnnouncement, Long> {

    List<TournamentAnnouncement> findByTournamentIdOrderBySortOrderAscIdAsc(Long tournamentId);
}
