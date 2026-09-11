package com.manacommunity.api.repository;

import com.manacommunity.api.model.SportsTournamentTimelineEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsTournamentTimelineEntryRepository extends JpaRepository<SportsTournamentTimelineEntry, Long> {

    List<SportsTournamentTimelineEntry> findByTournamentIdOrderBySortOrderAscIdAsc(Long tournamentId);
}
