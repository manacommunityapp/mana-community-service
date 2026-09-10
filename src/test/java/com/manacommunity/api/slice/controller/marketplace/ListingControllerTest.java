package com.manacommunity.api.slice.controller.marketplace;

import com.manacommunity.api.marketplace.controller.MarketListingController;
import com.manacommunity.api.marketplace.dto.MarketListingRequest;
import com.manacommunity.api.marketplace.dto.MarketListingResponse;
import com.manacommunity.api.marketplace.entity.MarketListing;
import com.manacommunity.api.marketplace.service.MarketListingService;
import com.manacommunity.api.repository.RolePermissionRepository;
import com.manacommunity.api.security.JwtTokenProvider;
import com.manacommunity.api.support.TestDataBuilder;
import com.manacommunity.api.support.WithMockUserPrincipal;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MarketListingController.class)
@ActiveProfiles("test")
@DisplayName("MarketListingController")
class ListingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean MarketListingService listingService;
    @MockitoBean LoggedInUserService loggedInUserService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean AppUserRepository appUserRepository;
    @MockitoBean RolePermissionRepository rolePermissionRepository;

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private MarketListingResponse sampleResponse() {
        return MarketListingResponse.builder()
                .id(1L)
                .title("Test Item")
                .description("A test listing")
                .price(BigDecimal.valueOf(500))
                .category("Electronics")
                .status(MarketListing.ListingStatus.ACTIVE)
                .imageUrls(List.of())
                .seller(MarketListingResponse.SellerSummary.builder()
                        .id(1L).fullName("Test User").verified(true).build())
                .communityId(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void stubLoggedInUser() {
        AppUser user = TestDataBuilder.adminUser();
        when(loggedInUserService.resolve(any())).thenReturn(user);
    }

    @Nested
    @DisplayName("GET /api/marketplace/listings")
    class GetListings {

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"View Marketplace"})
        @DisplayName("returns paginated listings for authenticated user")
        void returnsPaginatedListings() throws Exception {
            stubLoggedInUser();
            Page<MarketListingResponse> page = new PageImpl<>(
                    List.of(sampleResponse()), PageRequest.of(0, 12), 1);
            when(listingService.getCommunityListings(eq(1L), isNull(), any())).thenReturn(page);

            mockMvc.perform(get("/api/marketplace/listings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].title").value("Test Item"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"View Marketplace"})
        @DisplayName("passes category filter to service")
        void passesCategoryFilter() throws Exception {
            stubLoggedInUser();
            Page<MarketListingResponse> page = new PageImpl<>(
                    List.of(sampleResponse()), PageRequest.of(0, 12), 1);
            when(listingService.getCommunityListings(eq(1L), eq("Electronics"), any())).thenReturn(page);

            mockMvc.perform(get("/api/marketplace/listings?category=Electronics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].category").value("Electronics"));
        }
    }

    @Nested
    @DisplayName("POST /api/marketplace/listings")
    class CreateListing {

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"Create Listing"})
        @DisplayName("creates a listing successfully")
        void createsListingSuccessfully() throws Exception {
            stubLoggedInUser();
            MarketListingRequest req = MarketListingRequest.builder()
                    .title("New Product")
                    .price(BigDecimal.valueOf(250))
                    .category("Books")
                    .build();

            when(listingService.create(any(), any(), any())).thenReturn(sampleResponse());

            mockMvc.perform(post("/api/marketplace/listings")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isCreated());
        }
    }
}
