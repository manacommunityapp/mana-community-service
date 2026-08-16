package com.manacommunity.api.service.sample.data;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.service.impl.CommunityRoleInitializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CommunitySeeder — Handles seeding default communities (GENERAL and Lakshmi's Emperia).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunitySeeder {

    private final CommunityRepository communityRepo;
    private final CommunityRoleInitializer communityRoleInitializer;

    @Transactional
    public void defaultSeed() {
        log.info("Seeding default communities...");
        Community general = getOrCreateCommunity(
                "GENERAL", "GENERAL", "Hyderabad", "Telangana", "Miaypur", "GENERAL", "GENERAL");
        log.info("✓ Communities seeded: GENERAL (id={})", general.getId());
    }

    @Transactional
    public void seed() {
        log.info("Seeding Lakshmi's Emperia community...");
        Community le = getOrCreateCommunity(
                "Lakshmi's Emperia", "APARTMENT", "Hyderabad", "Telangana", "Miaypur", "Gated Community", "LE-MY-HYD");
        log.info("✓ Communities seeded: Lakshmi's Emperia (id={})", le.getId());
    }

    public Community getGeneralCommunity() {
        return communityRepo.findByInviteCode("GENERAL")
                .orElseThrow(() -> new IllegalStateException("GENERAL community has not been seeded yet."));
    }

    public Community getLeCommunity() {
        return communityRepo.findByInviteCode("LE-MY-HYD")
                .orElseThrow(() -> new IllegalStateException("Lakshmi's Emperia community has not been seeded yet."));
    }

    private Community getOrCreateCommunity(String name, String type, String city,
                                           String state, String area, String subtype, String inviteCode) {
        return communityRepo.findByInviteCode(inviteCode).orElseGet(() -> {
            Community c = Community.builder()
                    .name(name).type(type).city(city).state(state)
                    .area(area).subtype(subtype).inviteCode(inviteCode)
                    .build();
            Community saved = communityRepo.save(c);
            communityRoleInitializer.initializeCommunityRoles(saved);
            return saved;
        });
    }
}
