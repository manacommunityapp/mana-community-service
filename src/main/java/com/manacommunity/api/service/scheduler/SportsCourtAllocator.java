package com.manacommunity.api.service.scheduler;

import com.manacommunity.api.model.SportsCourt;
import com.manacommunity.api.model.scheduler.SportsTournamentConfig;
import com.manacommunity.api.repository.SportsCourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Allocates courts to generated matches, distributing them round-robin across
 * the config venue's courts so every court is used (mirrors the UI's
 * court-distribution behaviour). Returns null when the venue has no courts.
 */
@Service
@RequiredArgsConstructor
public class SportsCourtAllocator {

    private final SportsCourtRepository courtRepo;

    /** Courts available at the config's venue (empty if no venue / no courts). */
    public List<SportsCourt> courtsFor(SportsTournamentConfig config) {
        if (config == null || config.getVenue() == null) return List.of();
        return courtRepo.findByVenueId(config.getVenue().getId());
    }

    /** Round-robin pick from a pre-fetched court list by match index. */
    public SportsCourt pick(List<SportsCourt> courts, int index) {
        if (courts == null || courts.isEmpty()) return null;
        return courts.get(Math.floorMod(index, courts.size()));
    }

    /** Convenience: allocate a court for the given match index (queries per call). */
    public SportsCourt allocate(SportsTournamentConfig config, int index) {
        return pick(courtsFor(config), index);
    }
}
