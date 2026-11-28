package com.manacommunity.api.unit.marketplace;

import com.manacommunity.api.marketplace.service.ClassifiedsModerationEngine;
import com.manacommunity.api.marketplace.service.ClassifiedsModerationEngine.ClassifiedSubmission;
import com.manacommunity.api.marketplace.service.ClassifiedsModerationEngine.GeoVisibilityScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Hyperlocal Classifieds Geo-Fence, Image Compression & Spam/Phishing Moderation Tests")
class ClassifiedsModerationEngineTest {

    private final ClassifiedsModerationEngine engine = new ClassifiedsModerationEngine();

    @Test
    @DisplayName("Enforces society-only vs 3 km nearby radius geo-fencing accurately")
    void shouldEnforceSocietyOnlyAnd3KmGeoFence() {
        double condapurLat = 17.4580;
        double condapurLon = 78.3720;

        ClassifiedSubmission societyOnly = new ClassifiedSubmission(
                10L, condapurLat, condapurLon, GeoVisibilityScope.SOCIETY_ONLY,
                "Study Desk", "Mint condition IKEA desk", 600);
        ClassifiedSubmission nearby3km = new ClassifiedSubmission(
                10L, condapurLat, condapurLon, GeoVisibilityScope.NEARBY_3KM,
                "Hero Cycle", "Almost new hybrid cycle", 600);

        // Same society viewer always allowed
        assertThat(engine.isVisibleToViewer(societyOnly, 10L, 17.4600, 78.3730)).isTrue();
        // Neighboring society ~1.2 km away: blocked for SOCIETY_ONLY, visible for NEARBY_3KM
        assertThat(engine.isVisibleToViewer(societyOnly, 20L, 17.4670, 78.3780)).isFalse();
        assertThat(engine.isVisibleToViewer(nearby3km, 20L, 17.4670, 78.3780)).isTrue();
        // Distant society > 5 km away: blocked even for NEARBY_3KM
        assertThat(engine.isVisibleToViewer(nearby3km, 30L, 17.5150, 78.4100)).isFalse();
    }

    @Test
    @DisplayName("Compresses oversized photos and blocks profanity and phishing URLs")
    void shouldCompressOversizeImageAndBlockProfanityOrPhishing() {
        ClassifiedSubmission cleanLargePhoto = new ClassifiedSubmission(
                10L, 17.4580, 78.3720, GeoVisibilityScope.SOCIETY_ONLY,
                "2BHK Sofa Set", "3+1+1 wooden sofa set", 4200);
        var cleanDecision = engine.moderateSubmission(cleanLargePhoto);
        assertThat(cleanDecision.allowed()).isTrue();
        assertThat(cleanDecision.imageCompressed()).isTrue();
        assertThat(cleanDecision.compressedImageSizeKb()).isLessThanOrEqualTo(1024);

        ClassifiedSubmission phishingPost = new ClassifiedSubmission(
                10L, 17.4580, 78.3720, GeoVisibilityScope.NEARBY_3KM,
                "Free Fiber Router", "Claim at http://fr33-r0uter.xyz/claim now!", 500);
        var phishingDecision = engine.moderateSubmission(phishingPost);
        assertThat(phishingDecision.allowed()).isFalse();
        assertThat(phishingDecision.blockedReasons()).anyMatch(r -> r.startsWith("PHISHING_OR_UNSAFE_URL:"));

        ClassifiedSubmission profanePost = new ClassifiedSubmission(
                10L, 17.4580, 78.3720, GeoVisibilityScope.SOCIETY_ONLY,
                "Avoid this vendor", "Total scam and idiot behaviour", 400);
        var profaneDecision = engine.moderateSubmission(profanePost);
        assertThat(profaneDecision.allowed()).isFalse();
        assertThat(profaneDecision.blockedReasons()).anyMatch(r -> r.startsWith("PROFANITY_DETECTED:"));
    }
}
