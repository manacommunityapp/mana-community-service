package com.manacommunity.api.address.normalization.service;

import com.manacommunity.api.address.normalization.engine.AddressLexicalNormalizer;
import com.manacommunity.api.address.normalization.engine.AddressSegmentExtractor;
import com.manacommunity.api.address.normalization.engine.FuzzyStringMatcher;
import com.manacommunity.api.address.normalization.model.AddressNormalizationRequest;
import com.manacommunity.api.address.normalization.model.AddressNormalizationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddressEntityResolutionServiceTest {

    private AddressEntityResolutionService resolutionService;

    @BeforeEach
    void setUp() {
        AddressLexicalNormalizer normalizer = new AddressLexicalNormalizer();
        AddressSegmentExtractor extractor = new AddressSegmentExtractor(normalizer);
        FuzzyStringMatcher matcher = new FuzzyStringMatcher();
        resolutionService = new AddressEntityResolutionService(extractor, matcher);
        resolutionService.initCanonicalRegistry();
    }

    @Test
    void testResolveMessyAddressWithAbbreviationsAndTypo() {
        // Raw address from user prompt: "Flat 402, Blk B, Aparna Sarovar Grande, Nallagandla, Hyd"
        AddressNormalizationRequest request = new AddressNormalizationRequest(
                "Flat 402, Blk B, Aparna Sarovar Grande, Nallagandla, Hyd",
                "Hyderabad",
                "500019"
        );
        AddressNormalizationResponse response = resolutionService.resolveAddress(request);

        assertEquals("AUTO_RESOLVED", response.status());
        assertTrue(response.compositeConfidence() >= 0.85);
        assertNotNull(response.canonicalResolution());
        assertEquals(101L, response.canonicalResolution().communityId());
        assertEquals("Aparna Sarovar Grande", response.canonicalResolution().communityName());
        assertEquals("Tower B", response.canonicalResolution().towerName());
        assertEquals("402", response.canonicalResolution().flatNumber());
    }

    @Test
    void testResolvePhoneticTypoAndPermutedTokens() {
        // Typo "Sarowar Grand" instead of "Sarovar Grande"
        AddressNormalizationRequest request = new AddressNormalizationRequest(
                "B-402, Aparna Sarowar Grand, Nallagandla 500019",
                null,
                null
        );
        AddressNormalizationResponse response = resolutionService.resolveAddress(request);

        assertEquals("AUTO_RESOLVED", response.status());
        assertEquals(101L, response.canonicalResolution().communityId());
        assertEquals("402", response.canonicalResolution().flatNumber());
    }

    @Test
    void testResolvePrestigeShantiniketanBangalore() {
        AddressNormalizationRequest request = new AddressNormalizationRequest(
                "Flat 1204, Tower 2, Prestige Shantiniketan, Whitefield, Bangalore",
                null,
                "560048"
        );
        AddressNormalizationResponse response = resolutionService.resolveAddress(request);

        assertEquals("AUTO_RESOLVED", response.status());
        assertEquals(103L, response.canonicalResolution().communityId());
        assertEquals("Prestige Shantiniketan", response.canonicalResolution().communityName());
        assertEquals("Tower 2", response.canonicalResolution().towerName());
        assertEquals("1204", response.canonicalResolution().flatNumber());
    }

    @Test
    void testUnresolvedWhenAddressIsCompletelyUnknown() {
        AddressNormalizationRequest request = new AddressNormalizationRequest(
                "Random Flat 999, Unknown Apartment, Far Away City",
                null,
                null
        );
        AddressNormalizationResponse response = resolutionService.resolveAddress(request);

        assertEquals("MANUAL_VERIFICATION_REQUIRED", response.status());
        assertNull(response.canonicalResolution());
    }
}
