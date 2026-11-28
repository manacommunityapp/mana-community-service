package com.manacommunity.api.marketplace.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates hyperlocal classifieds for society/3 km geo-fence visibility,
 * image payload compression bounds, and profanity/phishing URL moderation.
 */
@Service
public class ClassifiedsModerationEngine {

    public static final double NEARBY_RADIUS_KM = 3.0;
    public static final int MAX_COMPRESSED_IMAGE_KB = 1024;
    public static final int MAX_RAW_UPLOAD_KB = 15 * 1024;

    private static final Set<String> PROFANITY_TERMS = Set.of(
            "scam", "fraudster", "bastard", "idiot", "shit", "fuck", "bitch", "asshole"
    );

    private static final Set<String> SUSPICIOUS_DOMAINS = Set.of(
            "bit.ly", "tinyurl.com", "t.co", "rb.gy", "is.gd", "cutt.ly"
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)\\b(?:https?://|www\\.)([^\\s/$.?#].[^\\s]*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern IP_HOST_PATTERN = Pattern.compile("^\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d+)?$");

    public enum GeoVisibilityScope {
        SOCIETY_ONLY,
        NEARBY_3KM
    }

    public record ClassifiedSubmission(
            Long sellerCommunityId,
            double sellerLat,
            double sellerLon,
            GeoVisibilityScope geoScope,
            String title,
            String description,
            int rawImageSizeKb
    ) {}

    public record ModerationDecision(
            boolean allowed,
            List<String> blockedReasons,
            int compressedImageSizeKb,
            boolean imageCompressed
    ) {}

    public ModerationDecision moderateSubmission(ClassifiedSubmission submission) {
        List<String> reasons = new ArrayList<>();
        String combined = ((submission.title() != null ? submission.title() : "") + " "
                + (submission.description() != null ? submission.description() : "")).trim();
        String lower = combined.toLowerCase(Locale.ROOT);

        for (String term : PROFANITY_TERMS) {
            if (lower.matches(".*\\b" + Pattern.quote(term) + "\\b.*")) {
                reasons.add("PROFANITY_DETECTED:" + term);
            }
        }

        Matcher urlMatcher = URL_PATTERN.matcher(combined);
        while (urlMatcher.find()) {
            String fullUrl = urlMatcher.group(0).toLowerCase(Locale.ROOT);
            String hostAndPath = urlMatcher.group(1).toLowerCase(Locale.ROOT);
            String host = hostAndPath.split("[/?#]", 2)[0];
            if (SUSPICIOUS_DOMAINS.contains(host)
                    || host.endsWith(".xyz")
                    || host.endsWith(".tk")
                    || host.endsWith(".top")
                    || host.contains("xn--")
                    || IP_HOST_PATTERN.matcher(host).matches()
                    || fullUrl.contains("/claim")
                    || fullUrl.contains("/verify-kyc")
                    || fullUrl.contains("fr33")) {
                reasons.add("PHISHING_OR_UNSAFE_URL:" + host);
            }
        }

        int finalImageKb = submission.rawImageSizeKb();
        boolean compressed = false;
        if (submission.rawImageSizeKb() > MAX_RAW_UPLOAD_KB) {
            reasons.add("IMAGE_EXCEEDS_MAX_15MB");
        } else if (submission.rawImageSizeKb() > MAX_COMPRESSED_IMAGE_KB) {
            // Deterministic target JPEG/WebP compression budget (e.g. 4200 KB -> 840 KB)
            finalImageKb = Math.min(MAX_COMPRESSED_IMAGE_KB, Math.max(250, submission.rawImageSizeKb() / 5));
            compressed = true;
        }

        return new ModerationDecision(reasons.isEmpty(), List.copyOf(reasons), finalImageKb, compressed);
    }

    public boolean isVisibleToViewer(ClassifiedSubmission listing, Long viewerCommunityId,
                                     double viewerLat, double viewerLon) {
        if (listing == null || viewerCommunityId == null) {
            return false;
        }
        if (listing.sellerCommunityId().equals(viewerCommunityId)) {
            return true;
        }
        if (listing.geoScope() == GeoVisibilityScope.SOCIETY_ONLY) {
            return false;
        }
        return haversineKm(listing.sellerLat(), listing.sellerLon(), viewerLat, viewerLon) <= NEARBY_RADIUS_KM;
    }

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
