package com.manacommunity.api.service.sample.data;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.SportsPlayerCategory;
import com.manacommunity.api.repository.SportsPlayerCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * SportsPlayerCategorySeeder — Seeds baseline player categories linked to communities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsPlayerCategorySeeder {

    private final SportsPlayerCategoryRepository playerCategoryRepo;
    private final CommunitySeeder communitySeeder;

    @Transactional
    public void defaultSeed() {
        log.info("Seeding player categories...");
        Community generalCommunity = communitySeeder.getGeneralCommunity();

        // ── 1. Cricket Brackets (3 categories) ──────────────────────────
        getOrCreatePlayerCategory("Cricket Kids (Under 8)",   "KIDS",    "ALL",     4,   7, generalCommunity, "Cricket (Combined Boys & Girls)");
        getOrCreatePlayerCategory("Cricket Youth (8 - 18)",   "OPEN",    "ALL",     8,  18, generalCommunity, "Cricket (Combined Boys & Girls)");
        getOrCreatePlayerCategory("Cricket Men (Above 18)",   "MENS",    "MALE",   18, 100, generalCommunity, "Cricket (Men Only)");

        // ── 2. Badminton Brackets (6 categories - strictly separate) ─────
        getOrCreatePlayerCategory("Badminton Boys (< 12)",     "BOYS",    "MALE",    4,  11, generalCommunity, "Badminton");
        getOrCreatePlayerCategory("Badminton Girls (< 12)",    "GIRLS",   "FEMALE",  4,  11, generalCommunity, "Badminton");
        getOrCreatePlayerCategory("Badminton Boys (12 - 18)",  "BOYS",    "MALE",   12,  18, generalCommunity, "Badminton");
        getOrCreatePlayerCategory("Badminton Girls (12 - 18)", "GIRLS",   "FEMALE", 12,  18, generalCommunity, "Badminton");
        getOrCreatePlayerCategory("Badminton Men (18+)",       "MENS",    "MALE",   18, 100, generalCommunity, "Badminton");
        getOrCreatePlayerCategory("Badminton Women (18+)",     "WOMENS",  "FEMALE", 18, 100, generalCommunity, "Badminton");

        // ── 3. Chess, Carroms, TT, Basketball, Skating (< 15) ────────────
        getOrCreatePlayerCategory("Boys Under 15 (< 15)",      "BOYS",    "MALE",    4,  14, generalCommunity, "Chess, Carroms, TT, Basketball, Skating");
        getOrCreatePlayerCategory("Girls Under 15 (< 15)",     "GIRLS",   "FEMALE",  4,  14, generalCommunity, "Chess, Carroms, TT, Basketball, Skating");

        // ── 4. Chess, Carroms, TT, Basketball (15+) ───────────────────────
        getOrCreatePlayerCategory("Men Above 15 (15+)",        "MENS",    "MALE",   15, 100, generalCommunity, "Chess, Carroms, TT, Basketball");
        getOrCreatePlayerCategory("Women Above 15 (15+)",      "WOMENS",  "FEMALE", 15, 100, generalCommunity, "Chess, Carroms, TT, Basketball");

        // ── 5. Volleyball (Adult Men Only) (1 category) ──────────────────
        getOrCreatePlayerCategory("Volleyball Men (18+)",      "MENS",    "MALE",   18, 100, generalCommunity, "Volleyball (Adult Men Only)");

        log.info("✓ Player categories seeded successfully (14 standard categories).");
    }

    @Transactional
    public void seed() {
        defaultSeed();
    }

    public SportsPlayerCategory getCategoryByName(String name) {
        return playerCategoryRepo.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("SportsPlayerCategory " + name + " has not been seeded yet."));
    }

    public Set<SportsPlayerCategory> getSummerCupCategories() {
        SportsPlayerCategory cricketYouth = getCategoryByName("Cricket Youth (8 - 18)");
        SportsPlayerCategory cricketMen = getCategoryByName("Cricket Men (Above 18)");
        return Set.of(cricketYouth, cricketMen);
    }

    private SportsPlayerCategory getOrCreatePlayerCategory(String name, String categoryType,
                                                     String gender, int minAge, int maxAge,
                                                     Community community, String description) {
        return playerCategoryRepo.findAll().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseGet(() -> playerCategoryRepo.save(SportsPlayerCategory.builder()
                        .name(name)
                        .category_type(categoryType)
                        .gender(gender)
                        .minAge(minAge)
                        .maxAge(maxAge)
                        .community(community)
                        .type("DEFAULT")
                        .description(description != null ? description : "Sample " + name + " category")
                        .build()));
    }
}
