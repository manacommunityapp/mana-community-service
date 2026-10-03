package com.manacommunity.api.sports.service;

import com.manacommunity.api.sports.dto.SportsCricHeroesProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jsoup-based HTML scraper for CricHeroes public player profile pages.
 *
 * Used as a fallback when the CricHeroes REST API is unavailable or when
 * performing a "preview" before officially linking a profile.
 *
 * All parsing is best-effort: missing fields fall back to sensible defaults
 * rather than throwing exceptions, to keep the link/preview flow resilient.
 */
@Slf4j
@Service
public class SportsCricHeroesScraperService {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private static final Pattern PLAYER_ID_PATTERN =
            Pattern.compile("/player-profile/(\\d+)|/player/(\\d+)");

    private static final int CONNECT_TIMEOUT_MS = 12_000;
    private static final int FETCH_TIMEOUT_MS   = 15_000;

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Resolves short share URLs (e.g. https://chshare.link/player/…)
     * to the canonical CricHeroes player-profile URL by following redirects.
     */
    public String resolveCanonicalUrl(String inputUrl) throws IOException {
        if (inputUrl == null || inputUrl.isBlank()) {
            throw new IllegalArgumentException("CricHeroes URL cannot be empty");
        }
        Connection.Response response = Jsoup.connect(inputUrl.trim())
                .userAgent(USER_AGENT)
                .followRedirects(true)
                .timeout(CONNECT_TIMEOUT_MS)
                .execute();
        return response.url().toExternalForm();
    }

    /**
     * Extracts the numeric CricHeroes player ID from a canonical profile URL.
     * Falls back to a hash-based pseudo-ID if no match is found.
     */
    public String extractPlayerId(String url) {
        if (url == null) return null;
        Matcher m = PLAYER_ID_PATTERN.matcher(url);
        if (m.find()) {
            return m.group(1) != null ? m.group(1) : m.group(2);
        }
        return "CH-" + Math.abs(url.hashCode());
    }

    /**
     * Scrapes the public player profile page and returns a populated
     * {@link SportsCricHeroesProfileResponse}.
     *
     * @param rawUrl      The URL pasted by the admin (may be a short share link)
     * @param formatScope "LEATHER", "TENNIS", or "OVERALL"
     * @throws IOException if the page cannot be fetched
     */
    public SportsCricHeroesProfileResponse fetchProfile(String rawUrl, String formatScope) throws IOException {
        String canonicalUrl = resolveCanonicalUrl(rawUrl);
        String chId         = extractPlayerId(canonicalUrl);

        log.info("Scraping CricHeroes profile: chId={} url={}", chId, canonicalUrl);

        Document doc = Jsoup.connect(canonicalUrl)
                .userAgent(USER_AGENT)
                .timeout(FETCH_TIMEOUT_MS)
                .get();

        // ── Bio ──────────────────────────────────────────────────────────
        String name = doc.select("h1.player-name, .profile-name, .user-name").text();
        if (name.isBlank()) {
            name = doc.title()
                    .replaceAll("(?i)- CricHeroes.*", "")
                    .replaceAll("(?i)Player Profile.*", "")
                    .trim();
        }
        if (name.isBlank()) name = "CricHeroes Player";

        String role      = doc.select(".player-role, .playing-role").text();
        if (role.isBlank()) role = "All-Rounder";

        String avatar    = doc.select(".player-image img, .profile-avatar img, img[alt*='profile']").attr("src");
        String batStyle  = doc.select(":containsOwn(Batting Style) + *").text();
        String bowlStyle = doc.select(":containsOwn(Bowling Style) + *").text();

        SportsCricHeroesProfileResponse.Bio bio = SportsCricHeroesProfileResponse.Bio.builder()
                .fullName(name)
                .avatarUrl(avatar.isBlank() ? null : avatar)
                .primaryRole(role)
                .battingStyle(batStyle.isBlank()  ? "Right Hand Bat"   : batStyle)
                .bowlingStyle(bowlStyle.isBlank() ? "Right-arm Medium" : bowlStyle)
                .build();

        return SportsCricHeroesProfileResponse.builder()
                .cricheroesId(chId != null ? chId : "CH-" + Math.abs(canonicalUrl.hashCode()))
                .shareUrl(canonicalUrl)
                .resolvedUrl(canonicalUrl)
                .verifiedAt(LocalDateTime.now())
                .formatScope(formatScope != null ? formatScope : "OVERALL")
                .bio(bio)
                .batting(parseBatting(doc))
                .bowling(parseBowling(doc))
                .fielding(parseFielding(doc))
                .recentForm(parseRecentForm(doc))
                .build();
    }

    // ── Private parsers ─────────────────────────────────────────────────────

    private SportsCricHeroesProfileResponse.Batting parseBatting(Document doc) {
        int innings  = extractInt(doc, "Innings", 0);
        int runs     = extractInt(doc, "Runs", 0);
        int matches  = extractInt(doc, "Matches", Math.max(innings, 1));
        double avg   = extractDouble(doc, "Average",     innings > 0 ? (double) runs / innings : 0.0);
        double sr    = extractDouble(doc, "Strike Rate", 100.0);
        String hs    = doc.select(":containsOwn(Highest Score) + *, :containsOwn(HS) + *").text();
        if (hs.isBlank()) hs = "-";

        return SportsCricHeroesProfileResponse.Batting.builder()
                .matches(matches)
                .innings(innings)
                .runs(runs)
                .highestScore(hs)
                .average(Math.round(avg * 100.0) / 100.0)
                .strikeRate(Math.round(sr * 100.0) / 100.0)
                .fifties(extractInt(doc, "50s", 0))
                .hundreds(extractInt(doc, "100s", 0))
                .fours(extractInt(doc, "4s", 0))
                .sixes(extractInt(doc, "6s", 0))
                .build();
    }

    private SportsCricHeroesProfileResponse.Bowling parseBowling(Document doc) {
        int matches    = extractInt(doc, "Matches", 0);
        int innings    = extractInt(doc, "Innings", matches);
        int wickets    = extractInt(doc, "Wickets", 0);
        double econ    = extractDouble(doc, "Economy",     8.0);
        double avg     = extractDouble(doc, "Bowling Avg", 20.0);
        double overs   = extractDouble(doc, "Overs",       0.0);
        String best    = doc.select(":containsOwn(Best Bowling) + *, :containsOwn(BBI) + *").text();
        if (best.isBlank()) best = "-";

        return SportsCricHeroesProfileResponse.Bowling.builder()
                .matches(matches)
                .innings(innings)
                .overs(overs)
                .wickets(wickets)
                .economy(Math.round(econ * 100.0) / 100.0)
                .average(Math.round(avg * 100.0) / 100.0)
                .strikeRate(wickets > 0 ? Math.round((overs * 6.0 / wickets) * 100.0) / 100.0 : 0.0)
                .bestFigures(best)
                .maidens(extractInt(doc, "Maidens", 0))
                .threeWickets(extractInt(doc, "3W", 0))
                .fiveWickets(extractInt(doc, "5W", 0))
                .build();
    }

    private SportsCricHeroesProfileResponse.Fielding parseFielding(Document doc) {
        return SportsCricHeroesProfileResponse.Fielding.builder()
                .catches(extractInt(doc, "Catches", 0))
                .stumpings(extractInt(doc, "Stumpings", 0))
                .runOuts(extractInt(doc, "Run Outs", 0))
                .build();
    }

    private List<SportsCricHeroesProfileResponse.RecentInning> parseRecentForm(Document doc) {
        // CricHeroes recent-form is rendered via JS; return empty list as graceful fallback.
        // If they ever expose a static table, parse it here.
        return new ArrayList<>();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private int extractInt(Document doc, String label, int defaultVal) {
        try {
            Element el = doc.select(":containsOwn(" + label + ") + *").first();
            if (el != null) return Integer.parseInt(el.text().replaceAll("[^0-9]", ""));
        } catch (Exception ignored) {}
        return defaultVal;
    }

    private double extractDouble(Document doc, String label, double defaultVal) {
        try {
            Element el = doc.select(":containsOwn(" + label + ") + *").first();
            if (el != null) return Double.parseDouble(el.text().replaceAll("[^0-9.]", ""));
        } catch (Exception ignored) {}
        return defaultVal;
    }
}
