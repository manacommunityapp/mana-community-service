package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsTournamentTimelineEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsTournamentTimelineEntryRepository extends JpaRepository<SportsTournamentTimelineEntry, Long> {

    List<SportsTournamentTimelineEntry> findByTournamentIdOrderBySortOrderAscIdAsc(Long tournamentId);
}
