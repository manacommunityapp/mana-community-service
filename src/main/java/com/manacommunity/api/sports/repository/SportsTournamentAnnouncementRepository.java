package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsTournamentAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsTournamentAnnouncementRepository extends JpaRepository<SportsTournamentAnnouncement, Long> {

    List<SportsTournamentAnnouncement> findByTournamentIdOrderBySortOrderAscIdAsc(Long tournamentId);
}
