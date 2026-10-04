package com.manacommunity.api.sports.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.model.SportsCricHeroesProfile;
import com.manacommunity.api.sports.model.SportsAuctionPlayer;
import com.manacommunity.api.sports.model.SportsAuctionTeam;
import com.manacommunity.api.sports.repository.SportsCricHeroesProfileRepository;
import com.manacommunity.api.sports.repository.SportsAuctionPlayerRepository;
import com.manacommunity.api.sports.repository.SportsAuctionTeamRepository;
import com.manacommunity.api.sports.service.SportsCricHeroesService;
import com.manacommunity.api.sports.service.SportsCricHeroesRatingEngine;
import com.manacommunity.api.sports.service.SportsCricHeroesScraperService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsCricHeroesServiceImpl implements SportsCricHeroesService {

    private final SportsCricHeroesProfileRepository profileRepo;
    private final SportsAuctionPlayerRepository playerRepo;
    private final SportsAuctionTeamRepository teamRepo;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SportsCricHeroesRatingEngine ratingEngine;
    private final SportsCricHeroesScraperService scraperService;


    private static final String CRICHEROES_API_BASE = "https://cricheroes.com/api/v1";

    @Override
    @Transactional
    public SportsCricHeroesLinkResponse linkProfile(SportsCricHeroesLinkRequest request, AppUser loggedInUser) {
        SportsAuctionPlayer player = playerRepo.findById(request.getPlayerId())
                .orElseThrow(() -> new ResourceNotFoundException("AuctionPlayer", request.getPlayerId()));

        if (profileRepo.existsByPlayerId(player.getId())) {
            throw new ManaCommunityException(
                    "Player already has a linked CricHeroes profile. Unlink first to change.",
                    HttpStatus.CONFLICT, "DUPLICATE_CRICHEROES_LINK");
        }

        String url = request.getCricHeroesUrl().trim();
        String cricheroesId = extractCricHeroesId(url);

        SportsCricHeroesProfile.FormatScope scope = SportsCricHeroesProfile.FormatScope.OVERALL;
        if (request.getFormatScope() != null) {
            try {
                scope = SportsCricHeroesProfile.FormatScope.valueOf(request.getFormatScope());
            } catch (IllegalArgumentException ignored) {}
        }

        SportsCricHeroesProfileResponse profileData = fetchProfileFromCricHeroes(cricheroesId);

        SportsCricHeroesProfile profile = SportsCricHeroesProfile.builder()
                .player(player)
                .community(player.getCommunity())
                .cricheroesId(cricheroesId)
                .shareUrl(url)
                .resolvedUrl(CRICHEROES_API_BASE + "/player/" + cricheroesId)
                .formatScope(scope)
                .bioJson(toJson(profileData.getBio()))
                .battingJson(toJson(profileData.getBatting()))
                .bowlingJson(toJson(profileData.getBowling()))
                .fieldingJson(toJson(profileData.getFielding()))
                .recentFormJson(toJson(profileData.getRecentForm()))
                .mvpPoints(profileData.getMvpPoints())
                .build();

        SportsPlayerRatingResponse rating = ratingEngine.computeRating(profileData);
        profile.setRatingOverall(rating.getOverall());
        profile.setRatingTier(rating.getTier());
        profile.setRatingBadges(String.join(",", rating.getBadges()));
        profile.setSuggestedBasePrice(rating.getSuggestedBasePrice());

        profileRepo.save(profile);

        player.setCricheroesId(cricheroesId);
        player.setCricheroesUrl(url);
        playerRepo.save(player);

        log.info("Linked CricHeroes profile {} to player {}", cricheroesId, player.getId());

        return SportsCricHeroesLinkResponse.builder()
                .playerId(player.getId())
                .cricheroesId(cricheroesId)
                .resolvedUrl(profile.getResolvedUrl())
                .profile(profileData)
                .linkedAt(profile.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void unlinkProfile(Long playerId, AppUser loggedInUser) {
        if (!profileRepo.existsByPlayerId(playerId)) {
            throw new ResourceNotFoundException("CricHeroesProfile", "playerId", String.valueOf(playerId));
        }
        profileRepo.deleteByPlayerId(playerId);

        playerRepo.findById(playerId).ifPresent(player -> {
            player.setCricheroesId(null);
            player.setCricheroesUrl(null);
            playerRepo.save(player);
        });

        log.info("Unlinked CricHeroes profile from player {}", playerId);
    }

    @Override
    @Transactional(readOnly = true)
    public SportsCricHeroesProfileResponse getProfile(Long playerId) {
        SportsCricHeroesProfile profile = profileRepo.findByPlayerId(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("CricHeroesProfile", "playerId", String.valueOf(playerId)));
        return toResponse(profile);
    }

    @Override
    @Transactional
    public SportsCricHeroesProfileResponse refreshProfile(Long playerId, AppUser loggedInUser) {
        SportsCricHeroesProfile profile = profileRepo.findByPlayerId(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("CricHeroesProfile", "playerId", String.valueOf(playerId)));

        SportsCricHeroesProfileResponse freshData = fetchProfileFromCricHeroes(profile.getCricheroesId());

        profile.setBioJson(toJson(freshData.getBio()));
        profile.setBattingJson(toJson(freshData.getBatting()));
        profile.setBowlingJson(toJson(freshData.getBowling()));
        profile.setFieldingJson(toJson(freshData.getFielding()));
        profile.setRecentFormJson(toJson(freshData.getRecentForm()));
        profile.setMvpPoints(freshData.getMvpPoints());
        profile.setSyncedAt(LocalDateTime.now());

        SportsPlayerRatingResponse rating = ratingEngine.computeRating(freshData);
        profile.setRatingOverall(rating.getOverall());
        profile.setRatingTier(rating.getTier());
        profile.setRatingBadges(String.join(",", rating.getBadges()));
        profile.setSuggestedBasePrice(rating.getSuggestedBasePrice());

        profileRepo.save(profile);
        log.info("Refreshed CricHeroes profile for player {}", playerId);
        return freshData;
    }

    @Override
    public SportsCricHeroesProfileResponse previewProfile(String url) {
        String cricheroesId = extractCricHeroesId(url.trim());
        return fetchProfileFromCricHeroes(cricheroesId);
    }

    @Override
    @Transactional(readOnly = true)
    public SportsPlayerRatingResponse getPlayerRating(Long playerId) {
        SportsCricHeroesProfile profile = profileRepo.findByPlayerId(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("CricHeroesProfile", "playerId", String.valueOf(playerId)));

        if (profile.getRatingOverall() != null) {
            List<String> badges = profile.getRatingBadges() != null && !profile.getRatingBadges().isBlank()
                    ? List.of(profile.getRatingBadges().split(","))
                    : List.of();
            return SportsPlayerRatingResponse.builder()
                    .overall(profile.getRatingOverall())
                    .tier(profile.getRatingTier())
                    .stars(tierToStars(profile.getRatingTier()))
                    .badges(badges)
                    .suggestedBasePrice(profile.getSuggestedBasePrice() != null ? profile.getSuggestedBasePrice() : 0)
                    .breakdown(Map.of())
                    .build();
        }

        SportsCricHeroesProfileResponse data = toResponse(profile);
        return ratingEngine.computeRating(data);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, SportsCricHeroesProfileResponse> getLinkedProfiles(Long configId) {
        return profileRepo.findByConfigId(configId).stream()
                .collect(Collectors.toMap(
                        p -> p.getPlayer().getId(),
                        this::toResponse
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public SportsTeamCompositionResponse getTeamComposition(Long configId, Long teamId) {
        SportsAuctionTeam team = teamRepo.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("AuctionTeam", teamId));

        List<SportsAuctionPlayer> teamPlayers = playerRepo.findByConfigId(configId).stream()
                .filter(p -> p.getAssignedTeam() != null && p.getAssignedTeam().getId().equals(teamId))
                .toList();

        int batters = 0, bowlers = 0, allRounders = 0, keepers = 0;
        int leftHand = 0, rightHand = 0, pacers = 0, spinners = 0;
        long totalRuns = 0;
        int totalWickets = 0;
        double sumBatAvg = 0, sumBowlEcon = 0, sumSR = 0;
        int batAvgCount = 0, bowlEconCount = 0, srCount = 0;

        for (SportsAuctionPlayer player : teamPlayers) {
            SportsCricHeroesProfile chProfile = profileRepo.findByPlayerId(player.getId()).orElse(null);
            if (chProfile == null) continue;

            SportsCricHeroesProfileResponse.Bio bio = fromJson(chProfile.getBioJson(), SportsCricHeroesProfileResponse.Bio.class);
            SportsCricHeroesProfileResponse.Batting bat = fromJson(chProfile.getBattingJson(), SportsCricHeroesProfileResponse.Batting.class);
            SportsCricHeroesProfileResponse.Bowling bowl = fromJson(chProfile.getBowlingJson(), SportsCricHeroesProfileResponse.Bowling.class);

            if (bio != null) {
                String role = bio.getPrimaryRole();
                if (role != null) {
                    switch (role) {
                        case "Batter" -> batters++;
                        case "Bowler" -> bowlers++;
                        case "All-Rounder" -> allRounders++;
                        case "Wicket Keeper" -> keepers++;
                    }
                }
                if ("Left Hand Bat".equals(bio.getBattingStyle())) leftHand++;
                else rightHand++;

                String bowlStyle = bio.getBowlingStyle();
                if (bowlStyle != null) {
                    String lower = bowlStyle.toLowerCase();
                    if (lower.contains("fast") || lower.contains("medium") || lower.contains("pace")) pacers++;
                    else if (lower.contains("spin") || lower.contains("off") || lower.contains("leg") || lower.contains("slow")) spinners++;
                }
            }
            if (bat != null) {
                totalRuns += bat.getRuns();
                if (bat.getAverage() > 0) { sumBatAvg += bat.getAverage(); batAvgCount++; }
                if (bat.getStrikeRate() > 0) { sumSR += bat.getStrikeRate(); srCount++; }
            }
            if (bowl != null) {
                totalWickets += bowl.getWickets();
                if (bowl.getEconomy() > 0) { sumBowlEcon += bowl.getEconomy(); bowlEconCount++; }
            }
        }

        long spent = team.getSpent() != null ? team.getSpent() : 0;
        long budget = team.getTotalBudget() != null ? team.getTotalBudget() : 0;

        return SportsTeamCompositionResponse.builder()
                .teamId(teamId)
                .totalPlayers(teamPlayers.size())
                .batters(batters)
                .bowlers(bowlers)
                .allRounders(allRounders)
                .wicketKeepers(keepers)
                .leftHandBats(leftHand)
                .rightHandBats(rightHand)
                .pacers(pacers)
                .spinners(spinners)
                .totalRuns(totalRuns)
                .totalWickets(totalWickets)
                .avgBattingAverage(batAvgCount > 0 ? Math.round(sumBatAvg / batAvgCount * 10.0) / 10.0 : 0)
                .avgBowlingEconomy(bowlEconCount > 0 ? Math.round(sumBowlEcon / bowlEconCount * 10.0) / 10.0 : 0)
                .avgStrikeRate(srCount > 0 ? Math.round(sumSR / srCount * 10.0) / 10.0 : 0)
                .budgetSpent(spent)
                .budgetRemaining(budget - spent)
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private SportsCricHeroesProfileResponse fetchProfileFromCricHeroes(String cricheroesId) {
        try {
            String url = CRICHEROES_API_BASE + "/player/" + cricheroesId + "/profile";
            JsonNode root = restTemplate.getForObject(url, JsonNode.class);
            if (root == null) {
                throw new ManaCommunityException("Failed to fetch CricHeroes profile", HttpStatus.BAD_GATEWAY, "CRICHEROES_FETCH_FAILED");
            }
            return mapCricHeroesResponse(root, cricheroesId);
        } catch (ManaCommunityException e) {
            throw e;
        } catch (Exception e) {
            log.warn("CricHeroes API call failed for id={}: {}", cricheroesId, e.getMessage());
            throw new ManaCommunityException(
                    "Unable to reach CricHeroes. Please try again later.",
                    HttpStatus.BAD_GATEWAY, "CRICHEROES_UNAVAILABLE");
        }
    }

    private SportsCricHeroesProfileResponse mapCricHeroesResponse(JsonNode root, String cricheroesId) {
        JsonNode data = root.has("data") ? root.get("data") : root;

        SportsCricHeroesProfileResponse.Bio bio = SportsCricHeroesProfileResponse.Bio.builder()
                .fullName(textOrDefault(data, "name", "Unknown"))
                .avatarUrl(textOrNull(data, "profile_image"))
                .battingStyle(textOrDefault(data, "batting_style", "Right Hand Bat"))
                .bowlingStyle(textOrDefault(data, "bowling_style", ""))
                .primaryRole(textOrDefault(data, "player_type", "Batter"))
                .build();

        JsonNode batNode = data.path("batting");
        SportsCricHeroesProfileResponse.Batting batting = SportsCricHeroesProfileResponse.Batting.builder()
                .matches(intVal(batNode, "matches"))
                .innings(intVal(batNode, "innings"))
                .runs(intVal(batNode, "runs"))
                .highestScore(textOrDefault(batNode, "highest_score", "0"))
                .average(doubleVal(batNode, "average"))
                .strikeRate(doubleVal(batNode, "strike_rate"))
                .fifties(intVal(batNode, "fifties"))
                .hundreds(intVal(batNode, "hundreds"))
                .fours(intVal(batNode, "fours"))
                .sixes(intVal(batNode, "sixes"))
                .build();

        JsonNode bowlNode = data.path("bowling");
        SportsCricHeroesProfileResponse.Bowling bowling = SportsCricHeroesProfileResponse.Bowling.builder()
                .matches(intVal(bowlNode, "matches"))
                .innings(intVal(bowlNode, "innings"))
                .overs(doubleVal(bowlNode, "overs"))
                .wickets(intVal(bowlNode, "wickets"))
                .economy(doubleVal(bowlNode, "economy"))
                .average(doubleVal(bowlNode, "average"))
                .strikeRate(doubleVal(bowlNode, "strike_rate"))
                .bestFigures(textOrDefault(bowlNode, "best_figures", "0/0"))
                .maidens(intVal(bowlNode, "maidens"))
                .threeWickets(intVal(bowlNode, "three_wickets"))
                .fiveWickets(intVal(bowlNode, "five_wickets"))
                .build();

        JsonNode fieldNode = data.path("fielding");
        SportsCricHeroesProfileResponse.Fielding fielding = SportsCricHeroesProfileResponse.Fielding.builder()
                .catches(intVal(fieldNode, "catches"))
                .stumpings(intVal(fieldNode, "stumpings"))
                .runOuts(intVal(fieldNode, "run_outs"))
                .build();

        List<SportsCricHeroesProfileResponse.RecentInning> recentForm = new ArrayList<>();
        JsonNode recentNode = data.path("recent_innings");
        if (recentNode.isArray()) {
            for (JsonNode inn : recentNode) {
                recentForm.add(SportsCricHeroesProfileResponse.RecentInning.builder()
                        .matchDate(textOrNull(inn, "match_date"))
                        .runs(intValOrNull(inn, "runs"))
                        .balls(intValOrNull(inn, "balls"))
                        .wickets(intValOrNull(inn, "wickets"))
                        .runsConceded(intValOrNull(inn, "runs_conceded"))
                        .opponent(textOrNull(inn, "opponent"))
                        .format(textOrNull(inn, "format"))
                        .build());
            }
        }

        return SportsCricHeroesProfileResponse.builder()
                .cricheroesId(cricheroesId)
                .shareUrl("https://cricheroes.com/player-profile/" + cricheroesId)
                .formatScope("OVERALL")
                .bio(bio)
                .batting(batting)
                .bowling(bowling)
                .fielding(fielding)
                .recentForm(recentForm)
                .build();
    }

    /** Maps a stored tier string to its star count for the cached-rating fast path. */
    private static int tierToStars(String tier) {
        if (tier == null) return 1;
        return switch (tier.toUpperCase()) {
            case "LEGEND"   -> 5;
            case "ICON"     -> 4;
            case "PLATINUM" -> 3;
            case "GOLD"     -> 2;
            default         -> 1; // SILVER, BRONZE
        };
    }

    private String extractCricHeroesId(String url) {
        if (url == null || url.isBlank()) {
            throw new ManaCommunityException("CricHeroes URL is required", HttpStatus.BAD_REQUEST, "INVALID_CRICHEROES_URL");
        }
        // Handle formats: https://cricheroes.com/player-profile/123456 or https://chshare.link/player/123456
        String cleaned = url.replaceAll("[?#].*$", "").replaceAll("/$", "");
        String[] parts = cleaned.split("/");
        String lastSegment = parts[parts.length - 1];
        if (lastSegment.isBlank() || lastSegment.length() > 100) {
            throw new ManaCommunityException("Invalid CricHeroes URL format", HttpStatus.BAD_REQUEST, "INVALID_CRICHEROES_URL");
        }
        return lastSegment;
    }

    private SportsCricHeroesProfileResponse toResponse(SportsCricHeroesProfile profile) {
        return SportsCricHeroesProfileResponse.builder()
                .cricheroesId(profile.getCricheroesId())
                .shareUrl(profile.getShareUrl())
                .resolvedUrl(profile.getResolvedUrl())
                .verifiedAt(profile.getVerifiedAt())
                .formatScope(profile.getFormatScope().name())
                .bio(fromJson(profile.getBioJson(), SportsCricHeroesProfileResponse.Bio.class))
                .batting(fromJson(profile.getBattingJson(), SportsCricHeroesProfileResponse.Batting.class))
                .bowling(fromJson(profile.getBowlingJson(), SportsCricHeroesProfileResponse.Bowling.class))
                .fielding(fromJson(profile.getFieldingJson(), SportsCricHeroesProfileResponse.Fielding.class))
                .recentForm(fromJsonList(profile.getRecentFormJson()))
                .mvpPoints(profile.getMvpPoints())
                .rating(profile.getRatingOverall())
                .build();
    }

    private String toJson(Object obj) {
        if (obj == null) return "{}";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("JSON serialization failed: {}", e.getMessage());
            return "{}";
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.warn("JSON deserialization failed for {}: {}", type.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private List<SportsCricHeroesProfileResponse.RecentInning> fromJsonList(String json) {
        if (json == null || json.isBlank() || "[]".equals(json)) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("JSON list deserialization failed: {}", e.getMessage());
            return List.of();
        }
    }

    private static String textOrDefault(JsonNode node, String field, String defaultVal) {
        JsonNode child = node.path(field);
        return child.isMissingNode() || child.isNull() ? defaultVal : child.asText(defaultVal);
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode child = node.path(field);
        return child.isMissingNode() || child.isNull() ? null : child.asText();
    }

    private static int intVal(JsonNode node, String field) {
        return node.path(field).asInt(0);
    }

    private static Integer intValOrNull(JsonNode node, String field) {
        JsonNode child = node.path(field);
        return child.isMissingNode() || child.isNull() ? null : child.asInt(0);
    }

    private static double doubleVal(JsonNode node, String field) {
        return node.path(field).asDouble(0.0);
    }
}
