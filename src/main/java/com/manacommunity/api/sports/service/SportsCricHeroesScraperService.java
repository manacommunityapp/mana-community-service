package com.manacommunity.api.sports.service;

import com.manacommunity.api.sports.dto.SportsCricHeroesProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
    private static final int MAX_REDIRECT_HOPS  = 5;

    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "cricheroes.com",
            "www.cricheroes.com",
            "chshare.link",
            "www.chshare.link"
    );

    @FunctionalInterface
    public interface DnsLookup {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    // ── SSRF Defense & Validation ────────────────────────────────────────────

    /**
     * Validates that the provided URL is a safe, external CricHeroes HTTPS URL
     * and does not resolve to private, loopback, or cloud metadata IP addresses.
     *
     * @param inputUrl The raw URL string to validate
     * @return Normalized URI
     * @throws IllegalArgumentException if the URL fails any security check
     */
    public static URI validateSafeUrl(String inputUrl) {
        return validateSafeUrl(inputUrl, InetAddress::getAllByName);
    }

    /**
     * Overload supporting custom DNS lookup for testability and IP pinning.
     */
    public static URI validateSafeUrl(String inputUrl, DnsLookup dnsLookup) {
        if (inputUrl == null || inputUrl.isBlank()) {
            throw new IllegalArgumentException("CricHeroes URL cannot be empty");
        }

        URI uri;
        try {
            uri = URI.create(inputUrl.trim()).normalize();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL syntax: " + e.getMessage(), e);
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Only HTTPS URLs are permitted");
        }

        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("URL must not contain user credentials");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL host cannot be empty");
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        boolean isWhitelisted = ALLOWED_HOSTS.contains(normalizedHost)
                || normalizedHost.endsWith(".cricheroes.com")
                || normalizedHost.endsWith(".chshare.link");

        if (!isWhitelisted) {
            throw new IllegalArgumentException("Host '" + host + "' is not permitted");
        }

        if (uri.getPort() != -1 && uri.getPort() != 443) {
            throw new IllegalArgumentException("Only standard HTTPS port (443) is permitted");
        }

        // SSRF IP checks (DNS resolution against loopback, RFC 1918 private, and RFC 3927 cloud metadata)
        try {
            InetAddress[] addresses = dnsLookup.resolve(normalizedHost);
            if (addresses == null || addresses.length == 0) {
                throw new IllegalArgumentException("Unable to resolve host: " + host);
            }
            for (InetAddress addr : addresses) {
                if (isRestrictedIp(addr)) {
                    log.warn("Blocked SSRF attempt targeting restricted IP {} for host {}", addr.getHostAddress(), host);
                    throw new IllegalArgumentException("Target host resolves to a restricted internal IP address");
                }
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Host resolution failed for: " + host, e);
        }

        return uri;
    }

    /**
     * Checks if the IP address belongs to loopback, site-local private, link-local metadata,
     * or other reserved address ranges.
     */
    public static boolean isRestrictedIp(InetAddress addr) {
        if (addr == null) return true;
        if (addr.isLoopbackAddress()
                || addr.isSiteLocalAddress()
                || addr.isLinkLocalAddress()
                || addr.isAnyLocalAddress()
                || addr.isMulticastAddress()) {
            return true;
        }

        String ip = addr.getHostAddress();
        return ip.startsWith("169.254.")    // Cloud metadata (AWS, Azure, GCP 169.254.169.254)
                || ip.startsWith("127.")      // IPv4 Loopback
                || ip.startsWith("0.")        // Current network
                || ip.startsWith("10.")       // Class A Private
                || ip.startsWith("192.168.")  // Class C Private
                || ip.startsWith("172.16.")   // Class B Private
                || ip.startsWith("172.17.")
                || ip.startsWith("172.18.")
                || ip.startsWith("172.19.")
                || ip.startsWith("172.20.")
                || ip.startsWith("172.21.")
                || ip.startsWith("172.22.")
                || ip.startsWith("172.23.")
                || ip.startsWith("172.24.")
                || ip.startsWith("172.25.")
                || ip.startsWith("172.26.")
                || ip.startsWith("172.27.")
                || ip.startsWith("172.28.")
                || ip.startsWith("172.29.")
                || ip.startsWith("172.30.")
                || ip.startsWith("172.31.")
                || ip.equals("255.255.255.255");
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Resolves short share URLs (e.g. https://chshare.link/player/…)
     * to the canonical CricHeroes player-profile URL by following redirects safely.
     */
    public String resolveCanonicalUrl(String inputUrl) throws IOException {
        URI currentUri = validateSafeUrl(inputUrl);
        String currentUrl = currentUri.toASCIIString();

        int hops = 0;
        while (hops < MAX_REDIRECT_HOPS) {
            Connection.Response response = Jsoup.connect(currentUrl)
                    .userAgent(USER_AGENT)
                    .followRedirects(false)
                    .timeout(CONNECT_TIMEOUT_MS)
                    .execute();

            int statusCode = response.statusCode();
            if (statusCode >= 300 && statusCode < 400) {
                String location = response.header("Location");
                if (location == null || location.isBlank()) {
                    break;
                }
                URI nextUri = currentUri.resolve(location).normalize();
                currentUri = validateSafeUrl(nextUri.toASCIIString());
                currentUrl = currentUri.toASCIIString();
                hops++;
            } else {
                return response.url().toExternalForm();
            }
        }
        return currentUrl;
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
        validateSafeUrl(canonicalUrl);
        String chId         = extractPlayerId(canonicalUrl);

        log.info("Scraping CricHeroes profile: chId={} url={}", chId, canonicalUrl);

        Connection.Response response = Jsoup.connect(canonicalUrl)
                .userAgent(USER_AGENT)
                .followRedirects(false)
                .timeout(FETCH_TIMEOUT_MS)
                .execute();

        Document doc = response.parse();

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
