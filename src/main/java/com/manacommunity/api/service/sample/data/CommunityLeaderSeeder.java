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
        seedLeader(le, userSeeder.getOrCreateUser("chethan@gmail.com", "Chethan Reddy", "ROLE_SPORTS_ADMIN", le),    "Sports Director",    null, 5);
//        seedLeader(le, userSeeder.getOrCreateUser("priya.patel@gmail.com", "Priya Patel", "ROLE_MEMBER", le), "Cultural Head",      null, 6);
//        seedLeader(le, userSeeder.getOrCreateUser("amit.kumar@gmail.com", "Amit Kumar", "ROLE_MEMBER", le),  "Maintenance Head",   null, 7);
//        seedLeader(le, userSeeder.getOrCreateUser("vikram.singh@gmail.com", "Vikram Singh", "ROLE_MEMBER", le), "Security Head",      null, 8);
//        seedLeader(le, userSeeder.getOrCreateUser("sneha.reddy@gmail.com", "Sneha Reddy", "ROLE_MEMBER", le), "Grievance Officer",  null, 9);

        // Sports Committee
        //seedLeader(le, userSeeder.getOrCreateUser("rajat.bhatia@gmail.com", "Rajat Bhatia", "ROLE_MEMBER", le), "Member", "Sports Committee", 10);
        //seedLeader(le, userSeeder.getOrCreateUser("kavita.menon@gmail.com", "Kavita Menon", "ROLE_MEMBER", le), "Member", "Sports Committee", 11);
        //seedLeader(le, userSeeder.getOrCreateUser("arjun.kapoor@gmail.com", "Arjun Kapoor", "ROLE_MEMBER", le), "Member", "Sports Committee", 12);

        // Cultural Committee
        //seedLeader(le, userSeeder.getOrCreateUser("ananya.desai@gmail.com", "Ananya Desai", "ROLE_MEMBER", le), "Member", "Cultural Committee", 13);
        //seedLeader(le, userSeeder.getOrCreateUser("neha.gupta@gmail.com", "Neha Gupta", "ROLE_MEMBER", le),   "Member", "Cultural Committee", 14);
        //seedLeader(le, userSeeder.getOrCreateUser("divya.rao@gmail.com", "Divya Rao", "ROLE_MEMBER", le),    "Member", "Cultural Committee", 15);

        // Maintenance Committee
        //seedLeader(le, userSeeder.getOrCreateUser("rohit.verma@gmail.com", "Rohit Verma", "ROLE_MEMBER", le),  "Member", "Maintenance Committee", 16);
        //seedLeader(le, userSeeder.getOrCreateUser("deepak.pillai@gmail.com", "Deepak Pillai", "ROLE_MEMBER", le), "Member", "Maintenance Committee", 17);

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
