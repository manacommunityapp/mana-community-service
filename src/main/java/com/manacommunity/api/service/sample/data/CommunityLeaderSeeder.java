package com.manacommunity.api.service.sample.data;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.CommunityLeader;
import com.manacommunity.api.repository.CommunityLeaderRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityLeaderSeeder {

    private final CommunityLeaderRepository leaderRepo;
    private final CommunitySeeder communitySeeder;
    private final UserSeeder userSeeder;

    @Transactional
    public void seed() {
        log.info("Seeding community directory leaders...");
        Community le = communitySeeder.getLeCommunity();

        // Executive committee
        seedLeader(le, userSeeder.getSunil(),    "President",        null, 1);
        seedLeader(le, userSeeder.getSandeep(),  "Vice President",   null, 2);
        seedLeader(le, userSeeder.getRamesh(),   "Secretary",        null, 3);
        seedLeader(le, userSeeder.getMady(),     "Treasurer",        null, 4);

        // Directors
        seedLeader(le, userSeeder.getUserByEmail("chethan@gmail.com"),    "Sports Director",    null, 5);
        seedLeader(le, userSeeder.getUserByEmail("priya.patel@gmail.com"), "Cultural Head",      null, 6);
        seedLeader(le, userSeeder.getUserByEmail("amit.kumar@gmail.com"),  "Maintenance Head",   null, 7);
        seedLeader(le, userSeeder.getUserByEmail("vikram.singh@gmail.com"),"Security Head",      null, 8);
        seedLeader(le, userSeeder.getUserByEmail("sneha.reddy@gmail.com"), "Grievance Officer",  null, 9);

        // Sports Committee
        seedLeader(le, userSeeder.getUserByEmail("rajat.bhatia@gmail.com"), "Member", "Sports Committee", 10);
        seedLeader(le, userSeeder.getUserByEmail("kavita.menon@gmail.com"), "Member", "Sports Committee", 11);
        seedLeader(le, userSeeder.getUserByEmail("arjun.kapoor@gmail.com"), "Member", "Sports Committee", 12);

        // Cultural Committee
        seedLeader(le, userSeeder.getUserByEmail("ananya.desai@gmail.com"), "Member", "Cultural Committee", 13);
        seedLeader(le, userSeeder.getUserByEmail("neha.gupta@gmail.com"),   "Member", "Cultural Committee", 14);
        seedLeader(le, userSeeder.getUserByEmail("divya.rao@gmail.com"),    "Member", "Cultural Committee", 15);

        // Maintenance Committee
        seedLeader(le, userSeeder.getUserByEmail("rohit.verma@gmail.com"),  "Member", "Maintenance Committee", 16);
        seedLeader(le, userSeeder.getUserByEmail("deepak.pillai@gmail.com"),"Member", "Maintenance Committee", 17);

        log.info("✓ Community directory leaders seeded successfully ({} entries).",
                leaderRepo.findByCommunityIdAndIsActiveTrueOrderByDisplayOrderAsc(le.getId()).size());
    }

    private void seedLeader(Community community, AppUser user, String designation,
                            String committee, int displayOrder) {
        if (leaderRepo.existsByCommunityIdAndUserIdAndDesignation(
                community.getId(), user.getId(), designation)) {
            return;
        }
        leaderRepo.save(CommunityLeader.builder()
                .community(community)
                .user(user)
                .designation(designation)
                .committee(committee)
                .contactPhone(user.getPhone())
                .contactEmail(user.getEmail())
                .displayOrder(displayOrder)
                .build());
    }
}
