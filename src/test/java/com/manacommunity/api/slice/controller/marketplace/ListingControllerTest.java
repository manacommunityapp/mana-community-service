package com.manacommunity.api.slice.controller.marketplace;

import com.manacommunity.api.marketplace.controller.ListingController;
import com.manacommunity.api.marketplace.dto.ListingRequest;
import com.manacommunity.api.marketplace.dto.ListingResponse;
import com.manacommunity.api.marketplace.service.ListingService;
import com.manacommunity.api.repository.RolePermissionRepository;
import com.manacommunity.api.security.JwtTokenProvider;
import com.manacommunity.api.support.WithMockUserPrincipal;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.support.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
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

@WebMvcTest(ListingController.class)
@ActiveProfiles("test")
@DisplayName("ListingController")
class ListingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ListingService listingService;
    @MockitoBean LoggedInUserService loggedInUserService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean AppUserRepository appUserRepository;
    @MockitoBean RolePermissionRepository rolePermissionRepository;

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private ListingResponse sampleResponse() {
        return ListingResponse.builder()
                .id(1L)
                .title("Test Item")
                .description("A test listing")
                .price(BigDecimal.valueOf(500))
                .category("Electronics")
                .status("ACTIVE")
                .imageUrls(List.of())
                .seller(ListingResponse.SellerRef.builder()
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
            Page<ListingResponse> page = new PageImpl<>(
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
            Page<ListingResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 12), 0);
            when(listingService.getCommunityListings(eq(1L), eq("Electronics"), any())).thenReturn(page);

            mockMvc.perform(get("/api/marketplace/listings").param("category", "Electronics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"View Marketplace"})
        @DisplayName("delegates to search when search param provided")
        void delegatesToSearch() throws Exception {
            stubLoggedInUser();
            Page<ListingResponse> page = new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 12), 1);
            when(listingService.searchListings(eq(1L), eq("bike"), any())).thenReturn(page);

            mockMvc.perform(get("/api/marketplace/listings").param("search", "bike"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Test Item"));
        }

        @Test
        @DisplayName("unauthenticated returns 4xx")
        void unauthenticated() throws Exception {
            mockMvc.perform(get("/api/marketplace/listings"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("POST /api/marketplace/listings")
    class CreateListing {

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"Create Listing"})
        @DisplayName("creates listing and returns 201")
        void creates201() throws Exception {
            stubLoggedInUser();
            when(listingService.create(any(), any(), any())).thenReturn(sampleResponse());

            ListingRequest req = new ListingRequest();
            req.setTitle("New Item");
            req.setPrice(BigDecimal.valueOf(1000));
            req.setCategory("Electronics");

            mockMvc.perform(post("/api/marketplace/listings")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"Create Listing"})
        @DisplayName("missing required fields returns 400")
        void missingFields400() throws Exception {
            stubLoggedInUser();

            mockMvc.perform(post("/api/marketplace/listings")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/marketplace/listings/{id}")
    class GetById {

        @Test
        @WithMockUserPrincipal(role = "MEMBER", permissions = {"View Marketplace"})
        @DisplayName("returns listing by id")
        void returnsById() throws Exception {
            when(listingService.getById(1L)).thenReturn(sampleResponse());

            mockMvc.perform(get("/api/marketplace/listings/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Test Item"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/marketplace/listings/{id}")
    class DeleteListing {

        @Test
        @WithMockUserPrincipal(role = "ADMIN", permissions = {"Delete Listing"})
        @DisplayName("admin delete returns 204")
        void adminDelete204() throws Exception {
            mockMvc.perform(delete("/api/marketplace/listings/1").with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }
}
