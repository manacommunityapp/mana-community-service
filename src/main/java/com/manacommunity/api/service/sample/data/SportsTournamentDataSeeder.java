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
        log.info("Seeding tournament table sample data (delete & recreate if exists)...");

        SportsTournament existing = tournamentRepo.findAll().stream()
                .filter(t -> TOURNAMENT_NAME.equalsIgnoreCase(t.getName()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            tournamentRepo.delete(existing);
            tournamentRepo.flush();
            log.info("✓ Cleaned existing tournament: {}", TOURNAMENT_NAME);
        }

        SportsTournament tournament = tournamentRepo.save(SportsTournament.builder()
                .name(TOURNAMENT_NAME)
                .description("LE 2026 Winter Cup")
                .eventDateStart(LocalDate.of(2026, 12, 1))
                .eventDateEnd(LocalDate.of(2026, 12, 15))
                .registrationDateStart(LocalDate.of(2026, 11, 1))
                .registrationDateEnd(LocalDate.of(2026, 11, 10))
                .maxParticipants(32)
                .contactNumber("9000000002")
                .contactEmail("wintercup@community.com")
                .allowAdminChat(false)
                .startTime("09:00 AM")
                .dueTime("06:00 PM")
                .otherContacts("[]")
                .bannerImage("")
                .registrationStatus(SportsTournament.EventStatus.DRAFT)
                .sportsEvents(new ArrayList<>())
                .sponsors(new ArrayList<>())
                .community(communityRepo.findById(2L).orElse(null))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        log.info("✓ SportsTournament table seeded: {} (id={})", TOURNAMENT_NAME, tournament.getId());
    }
}
