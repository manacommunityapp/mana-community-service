package com.manacommunity.api.service.sample.data;

import com.manacommunity.api.model.SportsCourt;
import com.manacommunity.api.model.Venue;
import com.manacommunity.api.repository.SportsCourtRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SportsCourtDataSeeder — dedicated seeder for the {@code court} table. Adds courts
 * to the {@link VenueDataSeeder} arena (idempotent by court name per venue).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsCourtDataSeeder {

    private final SportsCourtRepository courtRepo;
    private final VenueDataSeeder venueDataSeeder;

    @Transactional
    public void seed() {
        log.info("Seeding court table sample data...");
        Venue arena = venueDataSeeder.getOrCreateArena();

        List<SportsCourt> courts = List.of(
                SportsCourt.builder().name("Arena SportsCourt A").color("#3b82f6").build(),
                SportsCourt.builder().name("Arena SportsCourt B").color("#f59e0b").build(),
                SportsCourt.builder().name("Arena SportsCourt C").color("#10b981").build()
        );

        List<SportsCourt> existing = courtRepo.findByVenueId(arena.getId());
        int created = 0;
        for (SportsCourt c : courts) {
            boolean present = existing.stream().anyMatch(e -> e.getName().equalsIgnoreCase(c.getName()));
            if (!present) {
                c.setVenue(arena);
                courtRepo.save(c);
                created++;
            }
        }
        log.info("✓ SportsCourt table seeded: {} new court(s) for {}", created, arena.getName());
    }
}
