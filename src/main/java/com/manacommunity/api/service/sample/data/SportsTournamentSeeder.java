package com.manacommunity.api.service.sample.data;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.SportsEvent;
import com.manacommunity.api.model.SportsTournament;
import com.manacommunity.api.repository.SportsEventRepository;
import com.manacommunity.api.repository.SportsTournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsTournamentSeeder {

    public static final String TOURNAMENT_NAME = "LE 2026 Season Fest";

    private final SportsTournamentRepository tournamentRepo;
    private final SportsEventRepository sportsEventRepo;
    private final CommunitySeeder communitySeeder;

    @Transactional
    public void seed() {
        log.info("Seeding tournament sample data with all sports sub-events (delete & recreate if exists)...");

        Community leCommunity = communitySeeder.getLeCommunity();

        // 1. Find and disassociate existing tournament if present (clean current & legacy)
        List<String> namesToClean = List.of(TOURNAMENT_NAME, "LE 2026 Summer Champ", "LE 2026 Winter Cup");
        for (String name : namesToClean) {
            tournamentRepo.findAll().stream()
                    .filter(t -> t.getName() != null && t.getName().equalsIgnoreCase(name))
                    .toList()
                    .forEach(existing -> {
                        List<SportsEvent> linkedEvents = sportsEventRepo.findByTournamentId(existing.getId());
                        for (SportsEvent e : linkedEvents) {
                            e.setTournament(null);
                            sportsEventRepo.save(e);
                        }
                        sportsEventRepo.flush();
                        tournamentRepo.delete(existing);
                        tournamentRepo.flush();
                        log.info("✓ Cleaned existing tournament: {}", name);
                    });
        }

        // 2. Create parent tournament fresh
        SportsTournament tournament = tournamentRepo.save(SportsTournament.builder()
                .name(TOURNAMENT_NAME)
                .description("Lakshmi's Emperia Annual Sports Season Fest 2026 featuring Cricket, Badminton, Chess, Carroms, Table Tennis, and Volleyball.")
                .eventDateStart(LocalDate.of(2026, 10, 2))
                .eventDateEnd(LocalDate.of(2026, 12, 31))
                .registrationDateStart(LocalDate.of(2026, 9, 17))
                .registrationDateEnd(LocalDate.of(2026, 9, 19))
                .maxParticipants(500)
                .contactName("Ramesh Korlakunta")
                .contactNumber("8801357225")
                .contactEmail("kskreddy1989@gmail.com")
                .allowAdminChat(true)
                .startTime("08:00 AM")
                .dueTime("08:00 PM")
                .otherContacts("[]")
                .bannerImage("")
                .registrationStatus(SportsTournament.EventStatus.REGISTRATION_OPEN)
                .sportsEvents(new ArrayList<>())
                .sponsors(new ArrayList<>())
                .community(leCommunity)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        // 3. Link all configured sub-events to this tournament
        List<String> eventNames = List.of(
                "Annual 2026 Cricket Cup",
                "Badminton - Singles & Doubles",
                "Chess Championship",
                "Carroms - Singles and Doubles",
                "Table Tennis - Singles and Doubles",
                "Volleyball Premier League"
        );

        for (String eventName : eventNames) {
            sportsEventRepo.findAll().stream()
                    .filter(e -> e.getName() != null && e.getName().equalsIgnoreCase(eventName))
                    .findFirst()
                    .ifPresent(event -> {
                        event.setTournament(tournament);
                        sportsEventRepo.save(event);
                        if (!tournament.getSportsEvents().contains(event)) {
                            tournament.getSportsEvents().add(event);
                        }
                        log.info("✓ Linked sub-event '{}' (categories={}) to tournament '{}'",
                                event.getName(),
                                event.getCategories() != null ? event.getCategories().size() : 0,
                                tournament.getName());
                    });
        }

        tournamentRepo.save(tournament);
        log.info("✓ SportsTournament seeded: {} with {} sub-events (id={})",
                tournament.getName(), tournament.getSportsEvents().size(), tournament.getId());
    }
}
