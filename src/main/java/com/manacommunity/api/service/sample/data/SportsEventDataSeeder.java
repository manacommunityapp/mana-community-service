package com.manacommunity.api.service.sample.data;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.SportsEventRegistrationRepository;
import com.manacommunity.api.repository.SportsEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * SportsEventDataSeeder — dedicated seeder for the {@code sports_event} table.
 * Checks if test event already exists; if found, deletes and recreates it fresh.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsEventDataSeeder {

    public static final String EVENT_NAME = "Winter Football League";

    private final SportsEventRepository sportsEventRepo;
    private final SportsEventRegistrationRepository regRepo;
    private final SportsMetaSeeder sportsMetaSeeder;
    private final CommunitySeeder communitySeeder;
    private final VenueDataSeeder venueDataSeeder;
    private final UserSeeder userSeeder;
    private final SportsPlayerCategorySeeder playerCategorySeeder;

    @Transactional
    public void seed() {
        log.info("Seeding sports_event table sample data (delete & recreate if exists)...");
        getOrCreateWinterLeague();
        log.info("✓ Sports event table seeded: {}", EVENT_NAME);
    }

    /** Find-or-recreate the Winter Football League event (deletes existing first). */
    public SportsEvent getOrCreateWinterLeague() {
        SportsMeta football = sportsMetaSeeder.getOrCreateSport("Football", "⚽");
        Community community = communitySeeder.getLeCommunity();
        Venue arena = venueDataSeeder.getOrCreateArena();
        AppUser createdBy = userSeeder.getRamesh();
        SportsPlayerCategory mensOpen = playerCategorySeeder.getCategoryByName("Men Above 15 (15+)");

        SportsEvent existing = sportsEventRepo.findAll().stream()
                .filter(e -> EVENT_NAME.equalsIgnoreCase(e.getName()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            List<SportsEventRegistration> regs = regRepo.findByEventId(existing.getId());
            if (!regs.isEmpty()) {
                regRepo.deleteAll(regs);
                regRepo.flush();
            }
            sportsEventRepo.delete(existing);
            sportsEventRepo.flush();
            log.info("✓ Cleaned existing event: {}", EVENT_NAME);
        }

        return sportsEventRepo.save(SportsEvent.builder()
                .name(EVENT_NAME)
                .active(true)
                .sport(football)
                .community(community)
                .venue(arena)
                .createdBy(createdBy)
                .format(List.of("TEAM"))
                .tournamentType(SportsEvent.TournamentType.ROUND_ROBIN)
                .registrationDateStart(LocalDate.of(2026, 11, 1))
                .registrationDateEnd(LocalDate.of(2026, 11, 10))
                .eventDateStart(LocalDate.of(2026, 12, 1))
                .eventDateEnd(LocalDate.of(2026, 12, 15))
                .maxParticipants(32)
                .categories(Set.of(mensOpen))
                .minAge(15)
                .maxAge(100)
                .gender("MALE")
                .playersBorn(LocalDate.of(1900, 1, 1))
                .status(SportsEvent.EventStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }
}
