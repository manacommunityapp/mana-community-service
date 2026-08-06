package com.manacommunity.api.repository;

import com.manacommunity.api.model.TournamentTimelineEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentTimelineEntryRepository extends JpaRepository<TournamentTimelineEntry, Long> {

    List<TournamentTimelineEntry> findByTournamentIdOrderBySortOrderAscIdAsc(Long tournamentId);
}
