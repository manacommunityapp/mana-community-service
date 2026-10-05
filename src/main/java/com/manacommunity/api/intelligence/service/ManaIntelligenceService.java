package com.manacommunity.api.intelligence.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.intelligence.dto.*;
import com.manacommunity.api.intelligence.model.GraphProfile;
import com.manacommunity.api.intelligence.model.ProfileVisibility;
import com.manacommunity.api.intelligence.model.RecommendationType;
import com.manacommunity.api.intelligence.repository.GraphProfileRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ManaIntelligenceService {

    private final GraphProfileRepository profileRepo;
    private final AppUserRepository userRepo;
    private final PrivacyEnforcementFilter privacyFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 1. GET PERSONALIZED FEED
     * Community Graph -> User Context -> Recommendation Engine -> Privacy Filter -> Ranking -> Personalized Feed
     */
    @Transactional(readOnly = true)
    public List<RecommendationCardDto> getPersonalizedFeed(AppUser currentUser) {
        List<RecommendationCardDto> feed = new ArrayList<>();
        Long communityId = currentUser.getCommunity() != null ? currentUser.getCommunity().getId() : 1L;

        // Fetch candidate profiles
        List<GraphProfile> candidates = profileRepo.findDiscoverableProfiles(communityId);

        for (GraphProfile target : candidates) {
            PrivacyEnforcementFilter.AccessLevel access = privacyFilter.evaluateAccess(currentUser, target);
            if (access == PrivacyEnforcementFilter.AccessLevel.DENIED) {
                continue; // Server-Enforced Privacy filter out
            }

            // Exclude self from neighbor discovery recommendations
            if (target.getUser() != null && target.getUser().getId().equals(currentUser.getId())) {
                continue;
            }

            List<String> professions = parseJsonList(target.getProfessionsJson());
            List<String> skills = parseJsonList(target.getSkillsJson());
            List<String> tags = new ArrayList<>();
            tags.addAll(professions);
            tags.addAll(skills);

            int baseScore = 75;
            if (Boolean.TRUE.equals(target.getIsVerified())) baseScore += 10;
            if (target.getTower() != null && currentUser.getTower() != null && target.getTower().equalsIgnoreCase(currentUser.getTower())) {
                baseScore += 10; // Same tower boost
            }

            String professionTitle = !professions.isEmpty() ? professions.get(0) : "Resident";
            String towerFlat = (privacyFilter.isFlatVisible(currentUser, target) && target.getFlatNo() != null)
                    ? target.getTower() + "-" + target.getFlatNo() + " • "
                    : (target.getTower() != null ? target.getTower() + " • " : "");

            feed.add(RecommendationCardDto.builder()
                    .id("rec-p-" + target.getId())
                    .type(RecommendationType.PERSON)
                    .title(target.getDisplayName())
                    .subtitle(towerFlat + professionTitle)
                    .description(target.getBio() != null ? target.getBio() : "Active member in the community.")
                    .score(Math.min(99, baseScore))
                    .tags(tags.stream().limit(3).collect(Collectors.toList()))
                    .actionLabel("Connect")
                    .actionPath("/discover")
                    .imagePlaceholderColor("#6366f1")
                    .build());
        }

        // Add Multi-domain Community Context cards (Sports, Events, Trips, Food, Services)
        feed.add(RecommendationCardDto.builder()
                .id("rec-sport-01")
                .type(RecommendationType.SPORT)
                .title("Badminton Mixed Doubles — Weekend")
                .subtitle("Sports • Court A")
                .description("2 slots open for intermediate mixed doubles tournament prep.")
                .score(92)
                .tags(List.of("Badminton", "Court A", "Saturday"))
                .actionLabel("Join Court")
                .actionPath("/sports")
                .imagePlaceholderColor("#f59e0b")
                .build());

        feed.add(RecommendationCardDto.builder()
                .id("rec-event-01")
                .type(RecommendationType.EVENT)
                .title("Community Festival Celebration & Dinner")
                .subtitle("Event • Clubhouse")
                .description("Annual community gathering with live cultural performances and dinner.")
                .score(88)
                .tags(List.of("Festival", "Cultural", "Dinner"))
                .actionLabel("RSVP")
                .actionPath("/events")
                .imagePlaceholderColor("#f97316")
                .build());

        feed.add(RecommendationCardDto.builder()
                .id("rec-trip-01")
                .type(RecommendationType.TRIP)
                .title("Coorg Coffee Trail Carpool")
                .subtitle("Trip • Weekend Outing")
                .description("Weekend road trip organised by community members. 2 carpool seats available.")
                .score(85)
                .tags(List.of("Outing", "Nature", "Carpool"))
                .actionLabel("Join Trip")
                .actionPath("/trips")
                .imagePlaceholderColor("#16a34a")
                .build());

        // Sort descending by score
        feed.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        return feed;
    }

    /**
     * 2. SEARCH DISCOVER PROFILES
     * Server-Enforced Privacy & Masked DTOs
     */
    @Transactional(readOnly = true)
    public List<CommunityProfileDto> searchDiscoverProfiles(AppUser requester, String query, String towerFilter, String skillFilter) {
        Long communityId = requester.getCommunity() != null ? requester.getCommunity().getId() : 1L;
        List<GraphProfile> allProfiles = profileRepo.findDiscoverableProfiles(communityId);

        String q = query != null ? query.trim().toLowerCase() : "";
        List<CommunityProfileDto> results = new ArrayList<>();

        for (GraphProfile target : allProfiles) {
            // Privacy Enforcement check
            PrivacyEnforcementFilter.AccessLevel access = privacyFilter.evaluateAccess(requester, target);
            if (access == PrivacyEnforcementFilter.AccessLevel.DENIED) {
                continue; // Strictly filtered at server level
            }

            List<String> professions = parseJsonList(target.getProfessionsJson());
            List<String> skills = parseJsonList(target.getSkillsJson());
            List<String> interests = parseJsonList(target.getInterestsJson());
            List<String> sports = parseJsonList(target.getSportsJson());

            // Apply search query filter
            if (!q.isEmpty()) {
                boolean matches = target.getDisplayName().toLowerCase().contains(q)
                        || professions.stream().anyMatch(p -> p.toLowerCase().contains(q))
                        || skills.stream().anyMatch(s -> s.toLowerCase().contains(q))
                        || interests.stream().anyMatch(i -> i.toLowerCase().contains(q))
                        || sports.stream().anyMatch(sp -> sp.toLowerCase().contains(q))
                        || (target.getBio() != null && target.getBio().toLowerCase().contains(q));

                if (!matches) continue;
            }

            // Tower filter
            if (towerFilter != null && !towerFilter.isEmpty() && !towerFilter.equalsIgnoreCase("ALL")) {
                if (target.getTower() == null || !target.getTower().equalsIgnoreCase(towerFilter)) {
                    continue;
                }
            }

            // Skill filter
            if (skillFilter != null && !skillFilter.isEmpty() && !skillFilter.equalsIgnoreCase("ALL")) {
                boolean hasSkill = skills.stream().anyMatch(s -> s.equalsIgnoreCase(skillFilter));
                if (!hasSkill) continue;
            }

            // Apply PII Masking
            String maskedPhone = privacyFilter.isPhoneVisible(requester, target)
                    ? (target.getUser() != null ? target.getUser().getPhone() : null)
                    : maskPhone(target.getUser() != null ? target.getUser().getPhone() : null);

            String maskedEmail = privacyFilter.isEmailVisible(requester, target)
                    ? (target.getUser() != null ? target.getUser().getEmail() : null)
                    : maskEmail(target.getUser() != null ? target.getUser().getEmail() : null);

            String flatDisplay = privacyFilter.isFlatVisible(requester, target) ? target.getFlatNo() : "•••";

            results.add(CommunityProfileDto.builder()
                    .id("p-" + target.getId())
                    .userId(target.getUser() != null ? target.getUser().getId() : null)
                    .name(target.getDisplayName())
                    .tower(target.getTower())
                    .flatNumber(flatDisplay)
                    .bio(target.getBio())
                    .professions(professions)
                    .skills(skills)
                    .interests(interests)
                    .sports(sports)
                    .availabilityHours(target.getAvailabilityHours())
                    .visibility(target.getVisibility())
                    .isVerified(Boolean.TRUE.equals(target.getIsVerified()))
                    .phone(maskedPhone)
                    .email(maskedEmail)
                    .matchScore(90)
                    .mutualConnections(2)
                    .build());
        }

        return results;
    }

    /**
     * 3. OMNISEARCH
     * Unified 10-domain search with Server-Enforced Privacy
     */
    @Transactional(readOnly = true)
    public OmniSearchResponseDto omniSearch(AppUser requester, String query, int limit) {
        String q = query != null ? query.trim().toLowerCase() : "";
        List<CommunityProfileDto> matchedProfiles = searchDiscoverProfiles(requester, q, null, null);

        List<OmniSearchItemDto> peopleItems = matchedProfiles.stream().limit(limit).map(p -> OmniSearchItemDto.builder()
                .id(p.getId())
                .domain("PEOPLE")
                .title(p.getName())
                .subtitle(p.getTower() + "-" + p.getFlatNumber() + " • " + (!p.getProfessions().isEmpty() ? p.getProfessions().get(0) : "Resident"))
                .description("Skills: " + String.join(", ", p.getSkills()))
                .badge(p.isVerified() ? "Verified Resident" : "Resident")
                .deepLink("/discover?id=" + p.getId())
                .relevanceScore(0.92)
                .tags(p.getSkills())
                .build()).collect(Collectors.toList());

        Map<String, Integer> domainCounts = new LinkedHashMap<>();
        domainCounts.put("PEOPLE", peopleItems.size());
        domainCounts.put("EVENTS", 0);
        domainCounts.put("SPORTS", 0);
        domainCounts.put("JOBS", 0);
        domainCounts.put("BUSINESSES", 0);
        domainCounts.put("SERVICES", 0);
        domainCounts.put("MARKETPLACE", 0);
        domainCounts.put("FOOD", 0);
        domainCounts.put("DOCUMENTS", 0);
        domainCounts.put("VENDORS", 0);

        return OmniSearchResponseDto.builder()
                .query(query)
                .totalResults(peopleItems.size())
                .domainCounts(domainCounts)
                .people(peopleItems)
                .events(Collections.emptyList())
                .sports(Collections.emptyList())
                .jobs(Collections.emptyList())
                .businesses(Collections.emptyList())
                .services(Collections.emptyList())
                .marketplace(Collections.emptyList())
                .food(Collections.emptyList())
                .documents(Collections.emptyList())
                .vendors(Collections.emptyList())
                .build();
    }

    /**
     * 4. UPDATE DISCOVER VISIBILITY
     */
    @Transactional
    public CommunityProfileDto updateVisibility(AppUser user, UpdateVisibilityRequest req) {
        GraphProfile profile = profileRepo.findByUserId(user.getId()).orElseGet(() -> {
            GraphProfile p = GraphProfile.builder()
                    .user(user)
                    .community(user.getCommunity())
                    .displayName(user.getFullName())
                    .tower(user.getTower())
                    .flatNo(user.getFlatNo())
                    .build();
            return p;
        });

        profile.setVisibility(req.getVisibility());
        if (req.getBio() != null) profile.setBio(req.getBio());
        if (req.getProfessions() != null) profile.setProfessionsJson(writeJson(req.getProfessions()));
        if (req.getSkills() != null) profile.setSkillsJson(writeJson(req.getSkills()));
        if (req.getInterests() != null) profile.setInterestsJson(writeJson(req.getInterests()));
        if (req.getSports() != null) profile.setSportsJson(writeJson(req.getSports()));
        if (req.getAvailabilityHours() != null) profile.setAvailabilityHours(req.getAvailabilityHours());

        profile = profileRepo.save(profile);

        return CommunityProfileDto.builder()
                .id("p-" + profile.getId())
                .userId(user.getId())
                .name(profile.getDisplayName())
                .tower(profile.getTower())
                .flatNumber(profile.getFlatNo())
                .bio(profile.getBio())
                .professions(parseJsonList(profile.getProfessionsJson()))
                .skills(parseJsonList(profile.getSkillsJson()))
                .interests(parseJsonList(profile.getInterestsJson()))
                .sports(parseJsonList(profile.getSportsJson()))
                .visibility(profile.getVisibility())
                .build();
    }

    /**
     * 5. GET TOP SKILLS
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopSkills(AppUser requester) {
        Long communityId = requester.getCommunity() != null ? requester.getCommunity().getId() : 1L;
        List<GraphProfile> profiles = profileRepo.findDiscoverableProfiles(communityId);

        Map<String, Integer> counts = new HashMap<>();
        for (GraphProfile p : profiles) {
            if (privacyFilter.evaluateAccess(requester, p) != PrivacyEnforcementFilter.AccessLevel.DENIED) {
                List<String> skills = parseJsonList(p.getSkillsJson());
                for (String s : skills) {
                    counts.put(s, counts.getOrDefault(s, 0) + 1);
                }
            }
        }

        return counts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("skill", e.getKey());
                    map.put("count", e.getValue());
                    return map;
                }).collect(Collectors.toList());
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "••••••";
        return phone.substring(0, 3) + "••••" + phone.substring(phone.length() - 3);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "•••@•••";
        int at = email.indexOf("@");
        String prefix = email.substring(0, Math.min(2, at));
        return prefix + "•••" + email.substring(at);
    }
}