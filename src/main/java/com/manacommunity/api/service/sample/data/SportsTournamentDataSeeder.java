package com.manacommunity.api.service.sample.data;

import com.manacommunity.api.model.SportsTournament;
import com.manacommunity.api.repository.SportsTournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * SportsTournamentDataSeeder — dedicated seeder for the {@code tournament} table.
 * Checks if test tournament already exists; if found, deletes and recreates it fresh.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsTournamentDataSeeder {

    public static final String TOURNAMENT_NAME = "LE 2026 Winter Cup";

    private final SportsTournamentRepository tournamentRepo;
    private final com.manacommunity.api.repository.CommunityRepository communityRepo;

    @Transactional
    public void seed() {
        SportsTournament existing = tournamentRepo.findAll().stream()
                .filter(t -> TOURNAMENT_NAME.equalsIgnoreCase(t.getName()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            tournamentRepo.delete(existing);
            tournamentRepo.flush();
            log.info("✓ Cleaned existing tournament: {}", TOURNAMENT_NAME);
        }
    }
}
