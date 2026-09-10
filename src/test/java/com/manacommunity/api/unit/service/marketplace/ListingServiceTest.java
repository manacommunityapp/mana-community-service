package com.manacommunity.api.unit.service.marketplace;

import com.manacommunity.api.marketplace.dto.MarketListingRequest;
import com.manacommunity.api.marketplace.dto.MarketListingResponse;
import com.manacommunity.api.marketplace.entity.MarketListing;
import com.manacommunity.api.marketplace.entity.MarketListing.ListingStatus;
import com.manacommunity.api.marketplace.repository.MarketListingImageRepository;
import com.manacommunity.api.marketplace.repository.MarketListingRepository;
import com.manacommunity.api.marketplace.service.MarketListingService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.support.TestDataBuilder;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketListingService")
class ListingServiceTest {

    @Mock MarketListingRepository listingRepo;
    @Mock MarketListingImageRepository imageRepo;

    @InjectMocks MarketListingService listingService;

    private MarketListing buildListing(Long id, AppUser seller, Community community) {
        return MarketListing.builder()
                .id(id)
                .title("Test Item")
                .description("A test listing")
                .price(BigDecimal.valueOf(500))
                .priceUnit("INR")
                .category("Electronics")
                .status(ListingStatus.ACTIVE)
                .seller(seller)
                .community(community)
                .images(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private MarketListingRequest buildRequest() {
        return MarketListingRequest.builder()
                .title("New Item")
                .description("Brand new item")
                .price(BigDecimal.valueOf(1000))
                .category("Electronics")
                .build();
    }

    @Nested
    @DisplayName("getCommunityListings")
    class GetCommunityListings {

        @Test
        @DisplayName("returns paginated active listings for a community")
        void returnsPaginatedActiveListings() {
            Community community = TestDataBuilder.community();
            AppUser seller = TestDataBuilder.adminUser();
            MarketListing listing = buildListing(1L, seller, community);
            Pageable pageable = PageRequest.of(0, 12);
            Page<MarketListing> page = new PageImpl<>(List.of(listing), pageable, 1);

            when(listingRepo.findByCommunityIdAndStatus(1L, ListingStatus.ACTIVE, pageable))
                    .thenReturn(page);

            Page<MarketListingResponse> result = listingService.getCommunityListings(1L, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Item");
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("filters by category when provided")
        void filtersByCategory() {
            Community community = TestDataBuilder.community();
            AppUser seller = TestDataBuilder.adminUser();
            MarketListing listing = buildListing(1L, seller, community);
            Pageable pageable = PageRequest.of(0, 12);
            Page<MarketListing> page = new PageImpl<>(List.of(listing), pageable, 1);

            when(listingRepo.findByCommunityIdAndCategoryAndStatus(1L, "Electronics", ListingStatus.ACTIVE, pageable))
                    .thenReturn(page);

            Page<MarketListingResponse> result = listingService.getCommunityListings(1L, "Electronics", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCategory()).isEqualTo("Electronics");
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("returns listing response when listing exists")
        void returnsListingResponse() {
            Community community = TestDataBuilder.community();
            AppUser seller = TestDataBuilder.adminUser();
            MarketListing listing = buildListing(1L, seller, community);

            when(listingRepo.findById(1L)).thenReturn(Optional.of(listing));

            MarketListingResponse result = listingService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Test Item");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("creates and saves new listing")
        void createsAndSavesListing() {
            Community community = TestDataBuilder.community();
            AppUser seller = TestDataBuilder.adminUser();
            MarketListing saved = buildListing(1L, seller, community);

            when(listingRepo.save(any(MarketListing.class))).thenReturn(saved);

            MarketListingRequest req = buildRequest();
            MarketListingResponse result = listingService.create(req, seller, community);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Test Item");
        }
    }
}
