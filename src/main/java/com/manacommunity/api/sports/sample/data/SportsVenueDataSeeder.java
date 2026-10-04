package com.manacommunity.api.sports.sample.data;
import com.manacommunity.api.service.sample.data.CommunitySeeder;


import com.manacommunity.api.model.Community;
import com.manacommunity.api.sports.model.SportsVenue;
import com.manacommunity.api.sports.repository.SportsVenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SportsVenueDataSeeder — dedicated seeder for the {@code venue} table. Seeds a
 * distinct venue (idempotent by name) so it coexists with {@link SportsVenueSeeder}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsVenueDataSeeder {

    public static final String VENUE_NAME = "Community Sports Arena";

    private final SportsVenueRepository venueRepo;
    private final CommunitySeeder communitySeeder;

    @Transactional
    public void seed() {
        log.info("Seeding venue table sample data...");
        getOrCreateArena();
        log.info("✓ Venue table seeded: {}", VENUE_NAME);
    }

    /** Find-or-create the arena venue (idempotent by name). */
    public SportsVenue getOrCreateArena() {
        Community community = communitySeeder.getLeCommunity();
        return venueRepo.findAll().stream()
                .filter(v -> VENUE_NAME.equalsIgnoreCase(v.getName()))
                .findFirst()
                .orElseGet(() -> venueRepo.save(SportsVenue.builder()
                        .name(VENUE_NAME)
                        .venueType("COMMUNITY")
                        .venueCategory("SPORTS_VENUE")
                        .address("Near Central Park")
                        .area("Gachibowli")
                        .city("Hyderabad")
                        .pinCode("500032")
                        .capacity(500)
                        .community(community)
                        .openingTime("06:00 AM")
                        .closingTime("10:00 PM")
                        .contactName("Arena Desk")
                        .contactNumber("9000000001")
                        .contactEmail("arena@community.com")
                        .build()));
    }
}
