package com.manacommunity.api.marketplace.cluster.service.impl;

import com.manacommunity.api.marketplace.cluster.dto.MarketClusterDtos.*;
import com.manacommunity.api.marketplace.cluster.entity.MarketClusterListing;
import com.manacommunity.api.marketplace.cluster.repository.MarketClusterListingRepository;
import com.manacommunity.api.marketplace.cluster.service.HyperlocalMarketService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class HyperlocalMarketServiceImpl implements HyperlocalMarketService {

    private final MarketClusterListingRepository listingRepo;

    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private static final Pattern PROHIBITED_KEYWORDS = Pattern.compile(
            "\\b(gun|firearm|weapon|cocaine|drugs|narcotics|crypto|bitcoin|usdt|telegram me|whatsapp me)\\b",
            Pattern.CASE_INSENSITIVE);

    @Override
    @Transactional
    public RankedListingResponse createListing(CreateClusterListingRequest req, AppUser seller, Community community) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Rate-limiting check: max 5 listings per seller per 24 hours
        long countRecent = listingRepo.countBySeller_IdAndCreatedAtAfter(seller.getId(), now.minusHours(24));
        if (countRecent >= 5) {
            throw new IllegalStateException("Daily listing limit reached (maximum 5 listings per 24h)");
        }

        // 2. Pre-ingestion spam & safety moderation check
        String combinedText = (req.getTitle() + " " + req.getDescription()).toLowerCase();
        boolean hasProhibitedKeywords = PROHIBITED_KEYWORDS.matcher(combinedText).find();

        String moderationStatus = hasProhibitedKeywords ? "QUARANTINED" : "ACTIVE";
        BigDecimal fraudRiskScore = hasProhibitedKeywords ? new BigDecimal("0.95") : BigDecimal.ZERO;

        int durationDays = req.getDurationDays() > 0 ? Math.min(req.getDurationDays(), 30) : 7;
        LocalDateTime expiresAt = now.plusDays(durationDays);

        String s2Cell = computeS2CellIdL13(req.getLatitude().doubleValue(), req.getLongitude().doubleValue());

        MarketClusterListing listing = MarketClusterListing.builder()
                .community(community)
                .seller(seller)
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory() != null ? req.getCategory().toUpperCase() : "GENERAL")
                .price(req.getPrice())
                .visibilityScope(req.getVisibilityScope() != null ? req.getVisibilityScope() : "CLUSTER_3KM")
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .s2CellIdL13(s2Cell)
                .sellerTrustScore(new BigDecimal("1.00"))
                .isSellerVerifiedResident(true)
                .moderationStatus(moderationStatus)
                .fraudRiskScore(fraudRiskScore)
                .reportCount(0)
                .expiresAt(expiresAt)
                .createdAt(now)
                .build();

        MarketClusterListing saved = listingRepo.save(listing);

        return RankedListingResponse.builder()
                .id(saved.getId())
                .communityId(community.getId())
                .communityName(community.getName())
                .title(saved.getTitle())
                .description(saved.getDescription())
                .category(saved.getCategory())
                .price(saved.getPrice())
                .visibilityScope(saved.getVisibilityScope())
                .distanceMeters(BigDecimal.ZERO)
                .isSameSociety(true)
                .rankingScore(1.0)
                .moderationStatus(saved.getModerationStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RankedListingResponse> getRankedHyperlocalFeed(HyperlocalFeedQuery query) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Compute S2 cluster coverage for the user's location
        List<String> s2ClusterCells = getS2ClusterNeighborTokens(
                query.getUserLatitude().doubleValue(), query.getUserLongitude().doubleValue());

        // 2. Fetch candidate listings within own society OR matching 3km S2 cluster
        List<MarketClusterListing> candidates = listingRepo.findActiveHyperlocalCandidates(
                query.getUserCommunityId(), s2ClusterCells, now);

        List<RankedListingResponse> rankedResults = new ArrayList<>();

        for (MarketClusterListing item : candidates) {
            if (query.getCategory() != null && !query.getCategory().isBlank()
                    && !query.getCategory().equalsIgnoreCase(item.getCategory())) {
                continue;
            }

            double distMeters = calculateHaversineMeters(
                    query.getUserLatitude().doubleValue(), query.getUserLongitude().doubleValue(),
                    item.getLatitude().doubleValue(), item.getLongitude().doubleValue());

            boolean isSameSociety = item.getCommunity().getId().equals(query.getUserCommunityId());

            // If item is outside own society, enforce max 3 km radius cutoff
            if (!isSameSociety && distMeters > 3000.0) {
                continue;
            }

            // Compute Multi-Objective Ranking Components:
            // 1. Spatial Score: 1.0 if same society, else exponential decay exp(-d / 1.5km)
            double spatialScore = isSameSociety ? 1.0 : Math.exp(-distMeters / 1500.0);

            // 2. Category Freshness Decay:
            //    HOME_FOOD: tau = 4 hours, CARPOOL: tau = 12 hours, Others: tau = 168 hours (7 days)
            double tauHours = switch (item.getCategory().toUpperCase()) {
                case "HOME_FOOD", "FOOD" -> 4.0;
                case "CARPOOL", "RIDES" -> 12.0;
                default -> 168.0;
            };
            long ageMinutes = Duration.between(item.getCreatedAt(), now).toMinutes();
            double ageHours = Math.max(0, ageMinutes / 60.0);
            double freshnessScore = Math.exp(-ageHours / tauHours);

            // 3. Trust Score:
            double trustScore = item.getSellerTrustScore().doubleValue();
            if (Boolean.TRUE.equals(item.getIsSellerVerifiedResident())) {
                trustScore = Math.min(1.0, trustScore * 1.15);
            }

            // 4. Penalty: user reports + fraud score
            double penalty = (item.getReportCount() * 0.25) + (item.getFraudRiskScore().doubleValue() * 0.50);

            // Final Composite Ranking Score
            double finalScore = (0.35 * spatialScore) + (0.35 * freshnessScore) + (0.30 * trustScore) - penalty;

            rankedResults.add(RankedListingResponse.builder()
                    .id(item.getId())
                    .communityId(item.getCommunity().getId())
                    .communityName(item.getCommunity().getName())
                    .title(item.getTitle())
                    .description(item.getDescription())
                    .category(item.getCategory())
                    .price(item.getPrice())
                    .visibilityScope(item.getVisibilityScope())
                    .distanceMeters(BigDecimal.valueOf(distMeters).setScale(1, RoundingMode.HALF_UP))
                    .isSameSociety(isSameSociety)
                    .rankingScore(finalScore)
                    .spatialScore(spatialScore)
                    .freshnessScore(freshnessScore)
                    .trustScore(trustScore)
                    .moderationStatus(item.getModerationStatus())
                    .createdAt(item.getCreatedAt())
                    .build());
        }

        // Sort descending by ranking score
        rankedResults.sort((a, b) -> Double.compare(b.getRankingScore(), a.getRankingScore()));

        int limit = query.getLimit() > 0 ? Math.min(query.getLimit(), 100) : 50;
        return rankedResults.stream().limit(limit).toList();
    }

    @Override
    public String computeS2CellIdL13(double latitude, double longitude) {
        // Discretized Hilbert Curve Level 13 projection mapping lat/lon to a hex token
        int maxCoord = (1 << 13) - 1; // 8191
        int x = (int) Math.round(((longitude + 180.0) / 360.0) * maxCoord);
        int y = (int) Math.round(((latitude + 90.0) / 180.0) * maxCoord);
        x = Math.max(0, Math.min(maxCoord, x));
        y = Math.max(0, Math.min(maxCoord, y));

        long d = xy2d(13, x, y);
        return Long.toHexString(d);
    }

    @Override
    public List<String> getS2ClusterNeighborTokens(double latitude, double longitude) {
        // Generates the center cell + 8 surrounding cells covering the 3km radius at Level 13
        int maxCoord = (1 << 13) - 1;
        int cx = (int) Math.round(((longitude + 180.0) / 360.0) * maxCoord);
        int cy = (int) Math.round(((latitude + 90.0) / 180.0) * maxCoord);

        Set<String> tokens = new HashSet<>();
        // Delta offset -1, 0, +1 in grid coordinates corresponds to ~1.2km steps
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                int nx = Math.max(0, Math.min(maxCoord, cx + dx));
                int ny = Math.max(0, Math.min(maxCoord, cy + dy));
                tokens.add(Long.toHexString(xy2d(13, nx, ny)));
            }
        }
        return new ArrayList<>(tokens);
    }

    // Hilbert 2D curve coordinate interleaving
    private long xy2d(int n, int x, int y) {
        long d = 0;
        for (int s = (1 << (n - 1)); s > 0; s /= 2) {
            int rx = (x & s) > 0 ? 1 : 0;
            int ry = (y & s) > 0 ? 1 : 0;
            d += (long) s * s * ((3 * rx) ^ ry);
            if (ry == 0) {
                if (rx == 1) {
                    x = (1 << n) - 1 - x;
                    y = (1 << n) - 1 - y;
                }
                int t = x;
                x = y;
                y = t;
            }
        }
        return d;
    }

    private double calculateHaversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
