package com.manacommunity.api.repository;

import com.manacommunity.api.model.SportsTournamentAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsTournamentAnnouncementRepository extends JpaRepository<SportsTournamentAnnouncement, Long> {

    List<SportsTournamentAnnouncement> findByTournamentIdOrderBySortOrderAscIdAsc(Long tournamentId);
}
