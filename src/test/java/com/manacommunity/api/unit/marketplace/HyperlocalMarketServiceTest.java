package com.manacommunity.api.unit.marketplace;

import com.manacommunity.api.marketplace.cluster.dto.MarketClusterDtos.*;
import com.manacommunity.api.marketplace.cluster.entity.MarketClusterListing;
import com.manacommunity.api.marketplace.cluster.repository.MarketClusterListingRepository;
import com.manacommunity.api.marketplace.cluster.service.impl.HyperlocalMarketServiceImpl;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HyperlocalMarketServiceTest {

    @Mock
    private MarketClusterListingRepository listingRepo;

    @InjectMocks
    private HyperlocalMarketServiceImpl marketService;

    private Community myCommunity;
    private Community neighborCommunity;
    private AppUser seller;

    @BeforeEach
    void setUp() {
        myCommunity = Community.builder().id(101L).name("Green Glen Society").build();
        neighborCommunity = Community.builder().id(102L).name("Silver Springs").build();
        seller = AppUser.builder().id(201L).fullName("Pooja Verma").build();
    }

    @Test
    @DisplayName("S2 Cell L13 Token computation generates consistent hex token for coordinates")
    void computeS2CellIdL13_generatesValidHexToken() {
        String token = marketService.computeS2CellIdL13(12.9352, 77.6245);
        assertThat(token).isNotBlank();

        List<String> clusterCells = marketService.getS2ClusterNeighborTokens(12.9352, 77.6245);
        assertThat(clusterCells).isNotEmpty();
        assertThat(clusterCells).contains(token);
    }

    @Test
    @DisplayName("Pre-ingestion spam filter quarantines listings containing prohibited keywords")
    void createListing_prohibitedKeywords_quarantinesPost() {
        when(listingRepo.countBySeller_IdAndCreatedAtAfter(eq(201L), any())).thenReturn(0L);
        when(listingRepo.save(any(MarketClusterListing.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateClusterListingRequest req = CreateClusterListingRequest.builder()
                .title("Cheap iPhone 15 Pro")
                .description("Brand new sealed pack, pay via crypto USDT or telegram me")
                .category("ELECTRONICS")
                .price(new BigDecimal("15000.00"))
                .latitude(new BigDecimal("12.9352"))
                .longitude(new BigDecimal("77.6245"))
                .durationDays(7)
                .build();

        RankedListingResponse resp = marketService.createListing(req, seller, myCommunity);

        assertThat(resp.getModerationStatus()).isEqualTo("QUARANTINED");
        verify(listingRepo).save(argThat(l -> "QUARANTINED".equals(l.getModerationStatus()) &&
                l.getFraudRiskScore().compareTo(BigDecimal.ZERO) > 0));
    }

    @Test
    @DisplayName("Multi-objective feed ranking prioritizes same-society and fresh perishable food over distant furniture")
    void getRankedHyperlocalFeed_ranksBySpatialFreshnessAndTrust() {
        LocalDateTime now = LocalDateTime.now();

        // Listing 1: Home-cooked Biryani in same society posted 30 mins ago
        MarketClusterListing foodSameSociety = MarketClusterListing.builder()
                .id(1L)
                .community(myCommunity)
                .seller(seller)
                .title("Hyderabadi Dum Biryani")
                .description("Hot and fresh homemade lunch")
                .category("HOME_FOOD")
                .price(new BigDecimal("250.00"))
                .visibilityScope("INTRA_SOCIETY")
                .latitude(new BigDecimal("12.9352"))
                .longitude(new BigDecimal("77.6245"))
                .s2CellIdL13("s2-token-1")
                .sellerTrustScore(new BigDecimal("1.00"))
                .isSellerVerifiedResident(true)
                .moderationStatus("ACTIVE")
                .fraudRiskScore(BigDecimal.ZERO)
                .reportCount(0)
                .expiresAt(now.plusHours(4))
                .createdAt(now.minusMinutes(30)) // Very fresh
                .build();

        // Listing 2: Used Study Table in neighbor society (1.2 km away) posted 3 days ago
        MarketClusterListing furnitureNeighbor = MarketClusterListing.builder()
                .id(2L)
                .community(neighborCommunity)
                .seller(seller)
                .title("Solid Wood Study Desk")
                .description("Good condition with drawers")
                .category("FURNITURE")
                .price(new BigDecimal("3500.00"))
                .visibilityScope("CLUSTER_3KM")
                .latitude(new BigDecimal("12.9450"))
                .longitude(new BigDecimal("77.6320")) // ~1.3 km away
                .s2CellIdL13("s2-token-1")
                .sellerTrustScore(new BigDecimal("0.90"))
                .isSellerVerifiedResident(true)
                .moderationStatus("ACTIVE")
                .fraudRiskScore(BigDecimal.ZERO)
                .reportCount(0)
                .expiresAt(now.plusDays(4))
                .createdAt(now.minusDays(3))
                .build();

        when(listingRepo.findActiveHyperlocalCandidates(eq(101L), any(), any()))
                .thenReturn(List.of(furnitureNeighbor, foodSameSociety));

        HyperlocalFeedQuery query = HyperlocalFeedQuery.builder()
                .userCommunityId(101L)
                .userLatitude(new BigDecimal("12.9352"))
                .userLongitude(new BigDecimal("77.6245"))
                .limit(20)
                .build();

        List<RankedListingResponse> ranked = marketService.getRankedHyperlocalFeed(query);

        assertThat(ranked).hasSize(2);
        // Same society fresh food must outrank distant older furniture
        assertThat(ranked.get(0).getId()).isEqualTo(1L);
        assertThat(ranked.get(0).getTitle()).isEqualTo("Hyderabadi Dum Biryani");
        assertThat(ranked.get(0).isSameSociety()).isTrue();
        assertThat(ranked.get(0).getRankingScore()).isGreaterThan(ranked.get(1).getRankingScore());

        // Neighbor listing has spatial decay applied
        assertThat(ranked.get(1).getId()).isEqualTo(2L);
        assertThat(ranked.get(1).isSameSociety()).isFalse();
        assertThat(ranked.get(1).getDistanceMeters()).isGreaterThan(BigDecimal.valueOf(1000));
    }
}
