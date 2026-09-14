package com.manacommunity.api.service.sample.data;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.SportsMeta;
import com.manacommunity.api.model.karate.SportsKarateBelt;
import com.manacommunity.api.repository.karate.SportsKarateBeltRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds the default 9 Karate belt levels for a community.
 * Idempotent — skips belts that already exist at the given rank.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsKarateBeltSeeder {

    private final SportsKarateBeltRepository beltRepo;
    private final SportsMetaSeeder sportsMetaSeeder;
    private final CommunitySeeder communitySeeder;

    @Transactional
    public void seed() {
        Community community = communitySeeder.getLeCommunity();
        SportsMeta karate = sportsMetaSeeder.getOrCreateSport("Karate", "🥋");
        seedForCommunity(community, karate);
    }

    @Transactional
    public void seedForCommunity(Community community, SportsMeta sport) {
        record BeltDef(String name, String hex, int rank, int minClasses, int minMonths, String desc) {}

        List<BeltDef> defaults = List.of(
                new BeltDef("White Belt",  "#FFFFFF", 1,   0,  0, "Starting rank — no prior experience required."),
                new BeltDef("Yellow Belt", "#FACC15", 2,  30,  2, "Basic stances, blocks and punches mastered."),
                new BeltDef("Orange Belt", "#F97316", 3,  60,  4, "Introduction to basic kata."),
                new BeltDef("Green Belt",  "#22C55E", 4,  90,  6, "Intermediate techniques and sparring introduced."),
                new BeltDef("Blue Belt",   "#3B82F6", 5, 120,  9, "Advanced kata and combination attacks."),
                new BeltDef("Purple Belt", "#8B5CF6", 6, 160, 12, "Controlled sparring and self-defence applications."),
                new BeltDef("Red Belt",    "#EF4444", 7, 210, 16, "Pre-black belt — high-level techniques and teaching."),
                new BeltDef("Brown Belt",  "#92400E", 8, 270, 21, "Near-mastery — refining all previous skills."),
                new BeltDef("Black Belt",  "#1C1C1C", 9, 360, 36, "Mastery of fundamental techniques; teaching role.")
        );

        int created = 0;
        for (BeltDef def : defaults) {
            if (!beltRepo.existsByCommunityIdAndRank(community.getId(), def.rank())) {
                beltRepo.save(SportsKarateBelt.builder()
                        .community(community).sport(sport)
                        .name(def.name()).colorHex(def.hex()).rank(def.rank())
                        .minClassesRequired(def.minClasses()).minMonthsRequired(def.minMonths())
                        .description(def.desc()).active(true)
                        .build());
                created++;
            }
        }
        log.info("✓ SportsKarateBeltSeeder: {} new belt(s) seeded for community '{}'",
                created, community.getName());
    }
}
