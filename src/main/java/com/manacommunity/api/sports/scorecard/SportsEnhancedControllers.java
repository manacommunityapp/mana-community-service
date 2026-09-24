package com.manacommunity.api.sports.scorecard;

import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.LoggedInUserService;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

// ── Scorecard Controller ──────────────────────────────────────
@Slf4j
@RestController
@RequestMapping("/api/sports/matches/{matchId}")
@RequiredArgsConstructor
class ScorecardController {

    @PersistenceContext private final EntityManager em;
    private final LoggedInUserService loggedInUserService;
    private final org.springframework.context.ApplicationEventPublisher events;

    /** GET /api/sports/matches/{matchId}/scorecard */
    @GetMapping("/scorecard")
    public ResponseEntity<Map<String, Object>> getScorecard(@PathVariable Long matchId) {
        try {
            Object sc = em.createQuery("SELECT s FROM CricketScorecard s WHERE s.matchId=:id")
                .setParameter("id", matchId).getSingleResult();
            return ResponseEntity.ok(buildScorecardDto(sc, matchId));
        } catch (NoResultException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** PUT /api/sports/matches/{matchId}/scorecard — admin/scorer only */
    @PutMapping("/scorecard")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MODERATOR')")
    public ResponseEntity<Map<String, Object>> saveScorecard(
            @PathVariable Long matchId,
            @RequestBody Map<String, Object> body) {
        try {
            Object sc = em.createQuery("SELECT s FROM CricketScorecard s WHERE s.matchId=:id")
                .setParameter("id", matchId).getSingleResult();
            updateScorecard(sc, body);
            em.merge(sc);
        } catch (NoResultException e) {
            // Create new
            createScorecard(matchId, body);
        }
        return getScorecard(matchId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildScorecardDto(Object sc, Long matchId) {
        var cls = sc.getClass();
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("matchId", matchId);
        dto.put("result",  getF(sc, cls, "result"));
        dto.put("manOfMatch", buildMom(sc, cls));

        // Get innings from batting/bowling entries
        dto.put("firstInnings",  buildInnings(sc, 1));
        dto.put("secondInnings", buildInnings(sc, 2));
        return dto;
    }

    private Map<String, Object> buildMom(Object sc, Class<?> cls) {
        Object pid  = getF(sc, cls, "momPlayerId");
        Object name = getF(sc, cls, "momPlayerName");
        Object contrib = getF(sc, cls, "momContribution");
        if (pid == null && name == null) return null;
        return Map.of("playerId", pid, "playerName", name != null ? name : "",
                      "contribution", contrib != null ? contrib : "");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildInnings(Object sc, int inningsNum) {
        var scId = getF(sc, sc.getClass(), "id");
        List<Object> batting = em.createQuery(
            "SELECT b FROM BattingEntry b WHERE b.scorecard.id=:id AND b.inningsNumber=:n ORDER BY b.position")
            .setParameter("id", scId).setParameter("n", inningsNum).getResultList();
        List<Object> bowling = em.createQuery(
            "SELECT b FROM BowlingEntry b WHERE b.scorecard.id=:id AND b.inningsNumber=:n")
            .setParameter("id", scId).setParameter("n", inningsNum).getResultList();
        if (batting.isEmpty() && bowling.isEmpty()) return null;

        String teamName = batting.isEmpty() ? "" : (String) getF(batting.get(0), batting.get(0).getClass(), "battingTeamName");
        Object runs     = batting.isEmpty() ? 0  : getF(batting.get(0), batting.get(0).getClass(), "totalRuns");
        Object wickets  = batting.isEmpty() ? 0  : getF(batting.get(0), batting.get(0).getClass(), "wickets");
        Object overs    = batting.isEmpty() ? "" : getF(batting.get(0), batting.get(0).getClass(), "overs");

        return Map.of(
            "inningsNumber",   inningsNum,
            "battingTeamName", teamName != null ? teamName : "",
            "totalRuns",       runs     != null ? runs     : 0,
            "wickets",         wickets  != null ? wickets  : 0,
            "overs",           overs    != null ? overs    : "",
            "batting",  batting.stream().map(this::toBattingDto).collect(Collectors.toList()),
            "bowling",  bowling.stream().map(this::toBowlingDto).collect(Collectors.toList())
        );
    }

    private Map<String, Object> toBattingDto(Object b) {
        var c = b.getClass();
        return mapOf("id", getF(b,c,"id"), "playerName", getF(b,c,"playerName"),
            "runs", getF(b,c,"runs"), "balls", getF(b,c,"balls"),
            "fours", getF(b,c,"fours"), "sixes", getF(b,c,"sixes"),
            "strikeRate", getF(b,c,"strikeRate"), "dismissal", getF(b,c,"dismissal"),
            "isNotOut", getF(b,c,"isNotOut"), "position", getF(b,c,"position"));
    }

    private Map<String, Object> toBowlingDto(Object b) {
        var c = b.getClass();
        return mapOf("id", getF(b,c,"id"), "playerName", getF(b,c,"playerName"),
            "overs", getF(b,c,"overs"), "maidens", getF(b,c,"maidens"),
            "runs", getF(b,c,"runs"), "wickets", getF(b,c,"wickets"),
            "economy", getF(b,c,"economy"), "wides", getF(b,c,"wides"),
            "noBalls", getF(b,c,"noBalls"));
    }

    private void updateScorecard(Object sc, Map<String, Object> body) { /* merge fields */ }
    private void createScorecard(Long matchId, Map<String, Object> body) { /* persist new entity */ }

    private Object getF(Object o, Class<?> c, String f) {
        try { var field = c.getDeclaredField(f); field.setAccessible(true); return field.get(o); }
        catch (Exception e) { return null; }
    }

    @SafeVarargs
    private static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length - 1; i += 2) m.put((String)kv[i], kv[i+1]);
        return m;
    }
}

// ── Photo Controller ─────────────────────────────────────────
@Slf4j
@RestController
@RequestMapping("/api/sports/matches/{matchId}/photos")
@RequiredArgsConstructor
class MatchPhotoController {
    @PersistenceContext private final EntityManager em;
    private final LoggedInUserService loggedInUserService;
    private final org.springframework.web.multipart.MultipartResolver multipartResolver;
    private final org.springframework.beans.factory.annotation.Value("${app.upload.base-url:http://localhost:8082}") String baseUrl;
    private final org.springframework.beans.factory.annotation.Value("${app.upload.dir:uploads}") String uploadDir;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getPhotos(@PathVariable Long matchId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        @SuppressWarnings("unchecked")
        List<Object> photos = em.createQuery(
            "SELECT p FROM MatchPhoto p WHERE p.matchId=:id ORDER BY p.createdAt DESC")
            .setParameter("id", matchId).getResultList();
        return ResponseEntity.ok(photos.stream().map(p -> toDto(p, user.getId())).collect(Collectors.toList()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<Map<String, Object>> uploadPhoto(
            @PathVariable Long matchId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String caption,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        try {
            String filename = java.util.UUID.randomUUID() + "_" + file.getOriginalFilename();
            java.nio.file.Path dir = java.nio.file.Paths.get(uploadDir, "sports", "photos");
            java.nio.file.Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename).toFile());
            String url = baseUrl + "/uploads/sports/photos/" + filename;

            // Persist entity via JPQL (entity class in sports.model package)
            em.createNativeQuery(
                "INSERT INTO match_photo (match_id, uploaded_by_id, image_url, caption, like_count, created_at)" +
                " VALUES (?,?,?,?,0,NOW())")
                .setParameter(1, matchId).setParameter(2, user.getId())
                .setParameter(3, url).setParameter(4, caption)
                .executeUpdate();

            Object saved = em.createQuery("SELECT p FROM MatchPhoto p WHERE p.imageUrl=:u")
                .setParameter("u", url).getSingleResult();
            return ResponseEntity.status(201).body(toDto(saved, user.getId()));
        } catch (Exception e) {
            log.error("Photo upload failed", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/{photoId}/like")
    @Transactional
    public ResponseEntity<Void> toggleLike(@PathVariable Long matchId,
            @PathVariable Long photoId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long count = (Long) em.createQuery(
            "SELECT COUNT(l) FROM MatchPhotoLike l WHERE l.photoId=:p AND l.user.id=:u")
            .setParameter("p", photoId).setParameter("u", user.getId()).getSingleResult();
        if (count > 0) {
            em.createQuery("DELETE FROM MatchPhotoLike l WHERE l.photoId=:p AND l.user.id=:u")
                .setParameter("p", photoId).setParameter("u", user.getId()).executeUpdate();
            em.createQuery("UPDATE MatchPhoto p SET p.likeCount=GREATEST(p.likeCount-1,0) WHERE p.id=:id")
                .setParameter("id", photoId).executeUpdate();
        } else {
            em.createNativeQuery("INSERT INTO match_photo_like (photo_id, user_id) VALUES (?,?)")
                .setParameter(1, photoId).setParameter(2, user.getId()).executeUpdate();
            em.createQuery("UPDATE MatchPhoto p SET p.likeCount=p.likeCount+1 WHERE p.id=:id")
                .setParameter("id", photoId).executeUpdate();
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{photoId}")
    @Transactional
    public ResponseEntity<Void> deletePhoto(@PathVariable Long matchId,
            @PathVariable Long photoId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        int deleted = em.createQuery(
            "DELETE FROM MatchPhoto p WHERE p.id=:id AND p.uploadedBy.id=:uid")
            .setParameter("id", photoId).setParameter("uid", user.getId()).executeUpdate();
        return deleted > 0 ? ResponseEntity.noContent().build() : ResponseEntity.status(403).build();
    }

    private Map<String, Object> toDto(Object p, Long currentUserId) {
        var c = p.getClass();
        Long pid = (Long) getF(p, c, "id");
        Long uploaderId = null;
        try { uploaderId = (Long) p.getClass().getDeclaredMethod("getUploadedBy")
            .invoke(p).getClass().getDeclaredMethod("getId").invoke(
                p.getClass().getDeclaredMethod("getUploadedBy").invoke(p)); } catch (Exception ignored) {}
        boolean liked = em.createQuery(
            "SELECT COUNT(l) FROM MatchPhotoLike l WHERE l.photoId=:p AND l.user.id=:u", Long.class)
            .setParameter("p", pid).setParameter("u", currentUserId).getSingleResult() > 0;
        return Map.of("id", pid, "imageUrl", getF(p,c,"imageUrl"), "caption", Optional.ofNullable(getF(p,c,"caption")).orElse(""),
            "likeCount", getF(p,c,"likeCount"), "isLiked", liked,
            "uploaderName", "Resident", "uploadedById", uploaderId != null ? uploaderId : 0,
            "createdAt", getF(p,c,"createdAt"));
    }

    private Object getF(Object o, Class<?> c, String f) {
        try { var field = c.getDeclaredField(f); field.setAccessible(true); return field.get(o); }
        catch (Exception e) { return null; }
    }
}

// ── Ratings Controller ────────────────────────────────────────
@Slf4j
@RestController
@RequestMapping("/api/sports/matches/{matchId}/ratings")
@RequiredArgsConstructor
class MatchRatingController {
    @PersistenceContext private final EntityManager em;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getRatings(@PathVariable Long matchId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        boolean hasRated = ((Long) em.createQuery(
            "SELECT COUNT(r) FROM MatchPlayerRating r WHERE r.matchId=:m AND r.rater.id=:u", Long.class)
            .setParameter("m", matchId).setParameter("u", user.getId()).getSingleResult()) > 0;

        // Check if user participated
        boolean canRate = !hasRated && ((Long) em.createQuery(
            "SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team.match.id=:m AND tm.user.id=:u", Long.class)
            .setParameter("m", matchId).setParameter("u", user.getId()).getSingleResult()) > 0;

        // Get all players in this match
        @SuppressWarnings("unchecked")
        List<Object> players = em.createQuery(
            "SELECT DISTINCT tm.user FROM TeamMember tm WHERE tm.team.match.id=:m")
            .setParameter("m", matchId).getResultList();

        List<Map<String, Object>> playerDtos = players.stream().map(p -> {
            var pc = p.getClass();
            Long pid = (Long) getF(p, pc, "id");
            Double avgRating = em.createQuery(
                "SELECT AVG(r.stars) FROM MatchPlayerRating r WHERE r.matchId=:m AND r.ratedPlayer.id=:p", Double.class)
                .setParameter("m", matchId).setParameter("p", pid).getSingleResult();
            Long ratingCount = em.createQuery(
                "SELECT COUNT(r) FROM MatchPlayerRating r WHERE r.matchId=:m AND r.ratedPlayer.id=:p", Long.class)
                .setParameter("m", matchId).setParameter("p", pid).getSingleResult();
            boolean isMom = ((Long) em.createQuery(
                "SELECT COUNT(r) FROM MatchPlayerRating r WHERE r.matchId=:m AND r.ratedPlayer.id=:p AND r.isManOfMatch=true", Long.class)
                .setParameter("m", matchId).setParameter("p", pid).getSingleResult()) > 0;
            return Map.of("playerId", pid,
                "playerName", Optional.ofNullable(getF(p, pc, "fullName")).orElse("Player"),
                "averageRating", avgRating != null ? avgRating : 0.0,
                "ratingCount", ratingCount,
                "isManOfMatch", isMom);
        }).collect(Collectors.toList());

        // Man of match
        @SuppressWarnings("unchecked")
        List<Object[]> momVotes = em.createQuery(
            "SELECT r.ratedPlayer.id, r.ratedPlayer.fullName, COUNT(r) FROM MatchPlayerRating r" +
            " WHERE r.matchId=:m AND r.isManOfMatch=true GROUP BY r.ratedPlayer.id, r.ratedPlayer.fullName ORDER BY COUNT(r) DESC")
            .setParameter("m", matchId).setMaxResults(1).getResultList();

        Map<String, Object> mom = momVotes.isEmpty() ? null :
            Map.of("playerId", momVotes.get(0)[0], "playerName", momVotes.get(0)[1], "voteCount", momVotes.get(0)[2]);

        return ResponseEntity.ok(Map.of(
            "matchId", matchId, "canRate", canRate, "hasRated", hasRated,
            "players", playerDtos, "manOfMatch", mom != null ? mom : Map.of()
        ));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Void> submitRatings(@PathVariable Long matchId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ratings = (List<Map<String, Object>>) body.get("ratings");
        if (ratings == null || ratings.isEmpty()) return ResponseEntity.badRequest().build();

        for (var r : ratings) {
            Long playerId = Long.parseLong(r.get("playerId").toString());
            int stars = Integer.parseInt(r.get("stars").toString());
            String reaction = r.get("reaction") != null ? r.get("reaction").toString() : null;
            boolean isMom = Boolean.parseBoolean(r.getOrDefault("isManOfMatch","false").toString());

            em.createNativeQuery(
                "INSERT INTO match_player_rating (match_id, rater_id, rated_player_id, stars, reaction, is_man_of_match, created_at)" +
                " VALUES (?,?,?,?,?,?,NOW()) ON CONFLICT DO NOTHING")
                .setParameter(1, matchId).setParameter(2, user.getId()).setParameter(3, playerId)
                .setParameter(4, stars).setParameter(5, reaction).setParameter(6, isMom)
                .executeUpdate();
        }
        return ResponseEntity.ok().build();
    }

    private Object getF(Object o, Class<?> c, String f) {
        try { var field = c.getDeclaredField(f); field.setAccessible(true); return field.get(o); }
        catch (Exception e) { return null; }
    }
}

// ── Player Profile Controller ────────────────────────────────
@Slf4j
@RestController
@RequestMapping("/api/sports/players")
@RequiredArgsConstructor
class PlayerProfileController {
    @PersistenceContext private final EntityManager em;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return getProfile(loggedInUserService.resolve(principal).getId(), principal);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Object user;
        try {
            user = em.createQuery("SELECT u FROM AppUser u WHERE u.id=:id").setParameter("id", userId).getSingleResult();
        } catch (NoResultException e) { return ResponseEntity.notFound().build(); }

        @SuppressWarnings("unchecked")
        List<Object> stats = em.createQuery("SELECT s FROM PlayerSportStat s WHERE s.user.id=:id").setParameter("id", userId).getResultList();

        @SuppressWarnings("unchecked")
        List<Object> badges = em.createQuery("SELECT pb FROM PlayerBadge pb WHERE pb.user.id=:id").setParameter("id", userId).getResultList();

        Double avgRating = em.createQuery("SELECT AVG(r.stars) FROM MatchPlayerRating r WHERE r.ratedPlayer.id=:id", Double.class)
            .setParameter("id", userId).getSingleResult();
        Long ratingCount = em.createQuery("SELECT COUNT(r) FROM MatchPlayerRating r WHERE r.ratedPlayer.id=:id", Long.class)
            .setParameter("id", userId).getSingleResult();

        var uc = user.getClass();
        return ResponseEntity.ok(Map.of(
            "userId",          userId,
            "name",            getF(user, uc, "fullName"),
            "flatNo",          getF(user, uc, "flatNo"),
            "sportStats",      stats.stream().map(this::toStatDto).collect(Collectors.toList()),
            "badges",          badges.stream().map(this::toBadgeDto).collect(Collectors.toList()),
            "communityRating", avgRating  != null ? avgRating  : 0.0,
            "ratingCount",     ratingCount != null ? ratingCount : 0L,
            "totalMatches",    0, "totalTrophies", 0,
            "recentMatches",   List.of()
        ));
    }

    @GetMapping("/{userId}/badges")
    public ResponseEntity<List<Map<String, Object>>> getBadges(@PathVariable Long userId) {
        @SuppressWarnings("unchecked")
        List<Object> badges = em.createQuery("SELECT pb FROM PlayerBadge pb WHERE pb.user.id=:id").setParameter("id", userId).getResultList();
        return ResponseEntity.ok(badges.stream().map(this::toBadgeDto).collect(Collectors.toList()));
    }

    private Map<String, Object> toStatDto(Object s) {
        var c = s.getClass();
        return Map.of("sport", getF(s,c,"sport"), "matchesPlayed", getF(s,c,"matchesPlayed"),
            "wins", getF(s,c,"wins"), "losses", getF(s,c,"losses"), "draws", getF(s,c,"draws"),
            "winRate", safeDiv(getF(s,c,"wins"), getF(s,c,"matchesPlayed")),
            "tournaments", getF(s,c,"tournaments"), "trophies", getF(s,c,"trophies"),
            "totalRuns", getF(s,c,"totalRuns"), "totalWickets", getF(s,c,"totalWickets"),
            "goals", getF(s,c,"goals"));
    }

    private Map<String, Object> toBadgeDto(Object b) {
        var c = b.getClass();
        return Map.of("id", getF(b,c,"badgeId"), "earnedAt", getF(b,c,"earnedAt"), "isEarned", true);
    }

    private double safeDiv(Object wins, Object played) {
        try { return played != null && ((Number)played).doubleValue() > 0
            ? ((Number)wins).doubleValue() / ((Number)played).doubleValue() : 0.0; }
        catch (Exception e) { return 0.0; }
    }

    private Object getF(Object o, Class<?> c, String f) {
        try { var field = c.getDeclaredField(f); field.setAccessible(true); return field.get(o); }
        catch (Exception e) { return null; }
    }
}

// ── Leaderboard Controller ───────────────────────────────────
@Slf4j
@RestController
@RequestMapping("/api/sports/leaderboard")
@RequiredArgsConstructor
class SportsLeaderboardMobileController {
    @PersistenceContext private final EntityManager em;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getLeaderboard(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String sport,
            @RequestParam(defaultValue = "WINS") String category,
            @RequestParam(defaultValue = "ALL_TIME") String period) {

        AppUser user = loggedInUserService.resolve(principal);
        Long cid = user.getCommunity() != null ? user.getCommunity().getId() : null;
        Long currentUserId = user.getId();

        String orderField = switch (category) {
            case "RUNS"    -> "s.totalRuns";
            case "WICKETS" -> "s.totalWickets";
            case "GOALS"   -> "s.goals";
            case "MATCHES_PLAYED" -> "s.matchesPlayed";
            case "TROPHIES" -> "s.trophies";
            default         -> "s.wins";
        };

        StringBuilder q = new StringBuilder(
            "SELECT s FROM PlayerSportStat s WHERE s.user.community.id=:cid");
        if (sport != null && !sport.isBlank()) q.append(" AND s.sport=:sport");
        q.append(" ORDER BY ").append(orderField).append(" DESC");

        var query = em.createQuery(q.toString()).setParameter("cid", cid).setMaxResults(50);
        if (sport != null && !sport.isBlank()) query.setParameter("sport", sport);

        @SuppressWarnings("unchecked")
        List<Object> stats = query.getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < stats.size(); i++) {
            Object s = stats.get(i);
            var sc = s.getClass();
            Object userObj = getF(s, sc, "user");
            var uc = userObj.getClass();
            Long uid = (Long) getF(userObj, uc, "id");

            String cat = category;
            Object value = switch (cat) {
                case "RUNS"    -> getF(s, sc, "totalRuns");
                case "WICKETS" -> getF(s, sc, "totalWickets");
                case "GOALS"   -> getF(s, sc, "goals");
                case "MATCHES_PLAYED" -> getF(s, sc, "matchesPlayed");
                case "TROPHIES" -> getF(s, sc, "trophies");
                default        -> getF(s, sc, "wins");
            };
            long v = value instanceof Number ? ((Number)value).longValue() : 0L;
            String display = v + " " + cat.toLowerCase().replace("_"," ");

            result.add(Map.of(
                "rank", i + 1,
                "userId", uid,
                "name",   getF(userObj, uc, "fullName"),
                "flatNo", Optional.ofNullable(getF(userObj, uc, "flatNo")).orElse(""),
                "value",  v,
                "displayValue", display,
                "isCurrentUser", uid.equals(currentUserId)
            ));
        }
        return ResponseEntity.ok(result);
    }

    private Object getF(Object o, Class<?> c, String f) {
        try { var field = c.getDeclaredField(f); field.setAccessible(true); return field.get(o); }
        catch (Exception e) { return null; }
    }
}

// ── Badge Controller ─────────────────────────────────────────
@RestController
@RequestMapping("/api/sports/badges")
@RequiredArgsConstructor
class SportsBadgeController {

    /** Returns the full badge catalogue with definitions. */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllBadges() {
        return ResponseEntity.ok(BADGE_CATALOGUE);
    }

    private static final List<Map<String, Object>> BADGE_CATALOGUE = List.of(
        badge("tournament_winner",  "Tournament Champion", "🏆", "Won a tournament",         "achievement", "legendary"),
        badge("runner_up",          "Runner-up",           "🥈", "Finished 2nd in tournament","achievement", "epic"),
        badge("century_club",       "Century Club",        "💯", "Scored 100+ runs in a match","achievement", "epic"),
        badge("hat_trick",          "Hat-trick Hero",      "🎩", "Took 3+ wickets in 3 balls","achievement", "legendary"),
        badge("five_wickets",       "Fifer",               "🌟", "Took 5 wickets in an innings","achievement", "rare"),
        badge("top_scorer",         "Top Scorer",          "🏅", "Highest scorer in a tournament","achievement","rare"),
        badge("most_valuable",      "MVP",                 "⭐", "Man of the Match 3+ times", "achievement", "epic"),
        badge("10_matches",         "Veteran",             "💪", "Played 10 matches",         "milestone",   "common"),
        badge("50_matches",         "Legend",              "🦁", "Played 50 matches",         "milestone",   "rare"),
        badge("first_match",        "Debut",               "🌱", "Played your first match",   "participation","common"),
        badge("team_player",        "Team Player",         "🤝", "Rated 'Team Player' 5+ times","milestone", "common"),
        badge("good_sport",         "Good Sport",          "🏅", "Rated 'Good Sport' by peers","participation","common")
    );

    private static Map<String, Object> badge(String id, String name, String emoji, String desc, String cat, String rarity) {
        return Map.of("id",id,"name",name,"emoji",emoji,"description",desc,
            "category",cat,"rarity",rarity,"isEarned",false);
    }
}
