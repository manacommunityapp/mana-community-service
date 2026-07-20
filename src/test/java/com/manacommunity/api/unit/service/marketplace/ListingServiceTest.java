package com.manacommunity.api.unit.service.marketplace;

import com.manacommunity.api.marketplace.dto.ListingRequest;
import com.manacommunity.api.marketplace.dto.ListingResponse;
import com.manacommunity.api.marketplace.entity.Listing;
import com.manacommunity.api.marketplace.entity.Listing.ListingStatus;
import com.manacommunity.api.marketplace.repository.ListingRepository;
import com.manacommunity.api.marketplace.service.ListingService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListingService")
class ListingServiceTest {

    @Mock ListingRepository listingRepo;
    @Mock AuditService auditService;

    @InjectMocks ListingService listingService;

    private Listing buildListing(Long id, AppUser seller, Community community) {
        Listing listing = Listing.builder()
                .id(id)
                .title("Test Item")
                .description("A test listing")
                .price(BigDecimal.valueOf(500))
                .category("Electronics")
                .status(ListingStatus.ACTIVE)
                .seller(seller)
                .community(community)
                .images(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return listing;
    }

    private ListingRequest buildRequest() {
        ListingRequest req = new ListingRequest();
        req.setTitle("New Item");
        req.setDescription("Brand new item");
        req.setPrice(BigDecimal.valueOf(1000));
        req.setCategory("Electronics");
        return req;
    }

    @Nested
    @DisplayName("getCommunityListings")
    class GetCommunityListings {

        @Test
        @DisplayName("returns paginated active listings for a community")
        void returnsPaginatedActiveListings() {
            Community community = TestDataBuilder.community();
            AppUser seller = TestDataBuilder.adminUser();
            Listing listing = buildListing(1L, seller, community);
            Pageable pageable = PageRequest.of(0, 12);
            Page<Listing> page = new PageImpl<>(List.of(listing), pageable, 1);

            when(listingRepo.findByCommunityIdAndStatus(1L, ListingStatus.ACTIVE, pageable))
                    .thenReturn(page);

            Page<ListingResponse> result = listingService.getCommunityListings(1L, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Item");
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("filters by category when provided")
        void filtersByCategory() {
            Community community = TestDataBuilder.community();
            AppUser seller = TestDataBuilder.adminUser();
            Listing listing = buildListing(1L, seller, community);
            Pageable pageable = PageRequest.of(0, 12);
            Page<Listing> page = new PageImpl<>(List.of(listing), pageable, 1);

            when(listingRepo.findByCommunityIdAndCategoryAndStatus(1L, "Electronics", ListingStatus.ACTIVE, pageable))
                    .thenReturn(page);

            Page<ListingResponse> result = listingService.getCommunityListings(1L, "Electronics", pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(listingRepo).findByCommunityIdAndCategoryAndStatus(1L, "Electronics", ListingStatus.ACTIVE, pageable);
        }

        @Test
        @DisplayName("treats 'All' category as no filter")
        void treatsAllAsNoFilter() {
            Pageable pageable = PageRequest.of(0, 12);
            when(listingRepo.findByCommunityIdAndStatus(1L, ListingStatus.ACTIVE, pageable))
                    .thenReturn(Page.empty(pageable));

            listingService.getCommunityListings(1L, "All", pageable);

            verify(listingRepo).findByCommunityIdAndStatus(1L, ListingStatus.ACTIVE, pageable);
            verify(listingRepo, never()).findByCommunityIdAndCategoryAndStatus(anyLong(), anyString(), any(), any());
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("creates a listing and records audit event")
        void createsAndAudits() {
            AppUser seller = TestDataBuilder.adminUser();
            Community community = TestDataBuilder.community();
            ListingRequest req = buildRequest();
            Listing saved = buildListing(10L, seller, community);

            when(listingRepo.save(any(Listing.class))).thenReturn(saved);

            ListingResponse result = listingService.create(req, seller, community);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getTitle()).isEqualTo("Test Item");
            verify(auditService).record(AuditAction.PRODUCT_CREATED, AuditModule.MARKETPLACE,
                    "Listing", "10");
        }

        @Test
        @DisplayName("maps seller verification from KYC status")
        void mapsSellerVerification() {
            AppUser seller = TestDataBuilder.adminUser();
            seller.setKycStatus("APPROVED");
            Community community = TestDataBuilder.community();
            ListingRequest req = buildRequest();
            Listing saved = buildListing(1L, seller, community);

            when(listingRepo.save(any(Listing.class))).thenReturn(saved);

            ListingResponse result = listingService.create(req, seller, community);

            assertThat(result.getSeller().isVerified()).isTrue();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("allows owner to update their listing")
        void allowsOwnerUpdate() {
            AppUser seller = TestDataBuilder.adminUser();
            Community community = TestDataBuilder.community();
            Listing existing = buildListing(1L, seller, community);
            ListingRequest req = buildRequest();

            when(listingRepo.findById(1L)).thenReturn(Optional.of(existing));
            when(listingRepo.save(any(Listing.class))).thenReturn(existing);

            ListingResponse result = listingService.update(1L, req, seller.getId());

            assertThat(result).isNotNull();
            verify(auditService).record(AuditAction.PRODUCT_UPDATED, AuditModule.MARKETPLACE,
                    "Listing", "1");
        }

        @Test
        @DisplayName("rejects update from non-owner")
        void rejectsNonOwnerUpdate() {
            AppUser seller = TestDataBuilder.adminUser();
            Community community = TestDataBuilder.community();
            Listing existing = buildListing(1L, seller, community);
            ListingRequest req = buildRequest();

            when(listingRepo.findById(1L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> listingService.update(1L, req, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only edit your own");

            verify(auditService, never()).record(any(), any(), anyString(), anyString());
        }

        @Test
        @DisplayName("throws when listing not found")
        void throwsWhenNotFound() {
            when(listingRepo.findById(99L)).thenReturn(Optional.empty());
            ListingRequest req = buildRequest();

            assertThatThrownBy(() -> listingService.update(99L, req, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("allows owner to change status and records audit with old/new values")
        void allowsOwnerStatusChange() {
            AppUser seller = TestDataBuilder.adminUser();
            Community community = TestDataBuilder.community();
            Listing existing = buildListing(1L, seller, community);

            when(listingRepo.findById(1L)).thenReturn(Optional.of(existing));
            when(listingRepo.save(any(Listing.class))).thenReturn(existing);

            listingService.updateStatus(1L, "SOLD", seller.getId());

            assertThat(existing.getStatus()).isEqualTo(ListingStatus.SOLD);
            verify(auditService).record(AuditAction.PRODUCT_UPDATED, AuditModule.MARKETPLACE,
                    "Listing", "1", "ACTIVE", "SOLD");
        }

        @Test
        @DisplayName("rejects status change from non-owner")
        void rejectsNonOwnerStatusChange() {
            AppUser seller = TestDataBuilder.adminUser();
            Community community = TestDataBuilder.community();
            Listing existing = buildListing(1L, seller, community);

            when(listingRepo.findById(1L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> listingService.updateStatus(1L, "SOLD", 999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("adminDelete")
    class AdminDelete {

        @Test
        @DisplayName("soft-deletes listing and records audit")
        void softDeletesAndAudits() {
            AppUser seller = TestDataBuilder.adminUser();
            Community community = TestDataBuilder.community();
            Listing existing = buildListing(1L, seller, community);

            when(listingRepo.findById(1L)).thenReturn(Optional.of(existing));
            when(listingRepo.save(any(Listing.class))).thenReturn(existing);

            listingService.adminDelete(1L);

            assertThat(existing.getStatus()).isEqualTo(ListingStatus.DELETED);
            verify(auditService).record(AuditAction.PRODUCT_DELETED, AuditModule.MARKETPLACE,
                    "Listing", "1");
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("returns listing when found")
        void returnsWhenFound() {
            AppUser seller = TestDataBuilder.adminUser();
            Community community = TestDataBuilder.community();
            Listing existing = buildListing(1L, seller, community);

            when(listingRepo.findById(1L)).thenReturn(Optional.of(existing));

            ListingResponse result = listingService.getById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getSeller().getFullName()).isEqualTo(seller.getFullName());
        }

        @Test
        @DisplayName("throws when not found")
        void throwsWhenNotFound() {
            when(listingRepo.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> listingService.getById(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("getMyListings")
    class GetMyListings {

        @Test
        @DisplayName("returns seller's non-deleted listings")
        void returnsSellerListings() {
            AppUser seller = TestDataBuilder.adminUser();
            Community community = TestDataBuilder.community();
            Listing l1 = buildListing(1L, seller, community);
            Listing l2 = buildListing(2L, seller, community);
            l2.setStatus(ListingStatus.SOLD);

            when(listingRepo.findBySellerIdAndStatusNotOrderByCreatedAtDesc(seller.getId(), ListingStatus.DELETED))
                    .thenReturn(List.of(l1, l2));

            List<ListingResponse> result = listingService.getMyListings(seller.getId());

            assertThat(result).hasSize(2);
            assertThat(result.get(1).getStatus()).isEqualTo("SOLD");
        }
    }
}
