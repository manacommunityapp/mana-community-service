package com.manacommunity.api.unit.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.ai.config.AgentSecurityContext;
import com.manacommunity.api.ai.tool.AuctionQueryTools;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionQueryTools")
class AuctionQueryToolsTest extends BaseAiToolTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuctionQueryTools tools;

    @Nested
    @DisplayName("listMyCommunityAuctions")
    class ListAuctions {

        @Test
        @DisplayName("returns auctions scoped to user's community")
        void returnsAuctionsForCommunity() {
            List<Object[]> rows = List.of(
                    new Object[]{1L, "Season 1", "STANDARD", "ACTIVE", 8, 40}
            );
            TypedQuery<Object[]> query = mockListQuery(rows);
            when(em.createQuery(contains("FROM AuctionConfig"), eq(Object[].class)))
                    .thenReturn(query);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> result = (List<Map<String, Object>>) tools.listMyCommunityAuctions();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("season_name")).isEqualTo("Season 1");
            verify(query).setParameter("comId", TEST_COMMUNITY_ID);
        }
    }

    @Nested
    @DisplayName("searchPlayers")
    class SearchPlayers {

        @Test
        @DisplayName("denies access for auction not in user's community")
        @SuppressWarnings("unchecked")
        void deniesWrongCommunity() {
            TypedQuery<Long> countQuery = mockCountQuery(0L);
            when(em.createQuery(contains("SELECT COUNT"), eq(Long.class)))
                    .thenReturn(countQuery);

            Map<String, Object> result = (Map<String, Object>) tools.searchPlayers(
                    999L, null, null, null, null, null);

            assertThat(result.get("error")).isEqualTo(true);
            assertThat((String) result.get("message")).contains("Access denied");
        }

        @Test
        @DisplayName("returns players for valid auction in community")
        @SuppressWarnings("unchecked")
        void returnsPlayersForValidAuction() {
            // Mock community check
            TypedQuery<Long> countQuery = mockCountQuery(1L);
            when(em.createQuery(contains("SELECT COUNT"), eq(Long.class)))
                    .thenReturn(countQuery);

            // Mock player query
            List<Object[]> rows = List.of(
                    new Object[]{"Virat", "BATSMEN", "Batsman", 35, 150000, "QUEUED", null, 1, null, null}
            );
            TypedQuery<Object[]> playerQuery = mockListQuery(rows);
            when(em.createQuery(contains("FROM AuctionPlayer"), eq(Object[].class)))
                    .thenReturn(playerQuery);

            Object result = tools.searchPlayers(1L, "BATSMEN", null, null, null, null);
            assertThat(result).isInstanceOf(List.class);

            List<Map<String, Object>> players = (List<Map<String, Object>>) result;
            assertThat(players).hasSize(1);
            assertThat(players.get(0).get("name")).isEqualTo("Virat");
            assertThat(players.get(0).get("category")).isEqualTo("BATSMEN");
        }
    }

    @Nested
    @DisplayName("updateAuctionStatus")
    class UpdateStatus {

        @Test
        @DisplayName("rejects non-admin users")
        @SuppressWarnings("unchecked")
        void rejectsNonAdmin() {
            // Default context is MEMBER, not SUPER_ADMIN
            Map<String, Object> result = (Map<String, Object>) tools.updateAuctionStatus(
                    1L, "LIVE", true);

            assertThat(result.get("error")).isEqualTo(true);
            assertThat((String) result.get("message")).contains("Write permission denied");
        }

        @Test
        @DisplayName("requires explicit confirmation")
        @SuppressWarnings("unchecked")
        void requiresConfirmation() {
            // Switch to admin context
            AgentSecurityContext.set(adminContext());

            TypedQuery<Long> countQuery = mockCountQuery(1L);
            when(em.createQuery(contains("SELECT COUNT"), eq(Long.class)))
                    .thenReturn(countQuery);

            Map<String, Object> result = (Map<String, Object>) tools.updateAuctionStatus(
                    1L, "LIVE", false);

            assertThat(result.get("requires_confirmation")).isEqualTo(true);
        }
    }
}
