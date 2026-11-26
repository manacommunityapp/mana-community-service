package com.manacommunity.api.security;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedJwtSecurityTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String TEST_SECRET = "very-secure-256-bit-secret-key-for-testing-only-1234567890!";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                TEST_SECRET,
                900000L,
                604800000L,
                "mana-community",
                "mana-api",
                true
        );
    }

    @Test
    @DisplayName("Should generate token with unified claims (uid, sub, role, roles, permissions, communityId)")
    void testTokenGenerationWithUnifiedClaims() {
        Community community = Community.builder().name("Palm Grove").build();
        community.setId(42L);

        AppUser user = AppUser.builder()
                .id(101L)
                .email("resident@example.com")
                .fullName("Resident User")
                .role("RESIDENT, ADMIN")
                .community(community)
                .build();

        List<String> permissions = List.of("VIEW_VISITORS", "CREATE_PASS", "BOOK_AMENITY");

        String token = jwtTokenProvider.generateToken(user, permissions);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.isAccessToken(token)).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(token)).isFalse();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(101L);
        assertThat(jwtTokenProvider.getEmail(token)).isEqualTo("resident@example.com");
        assertThat(jwtTokenProvider.getCommunityId(token)).isEqualTo(42L);
        assertThat(jwtTokenProvider.getRoles(token)).containsExactlyInAnyOrder("RESIDENT", "ADMIN");
        assertThat(jwtTokenProvider.getPermissions(token)).containsExactlyInAnyOrder("VIEW_VISITORS", "CREATE_PASS", "BOOK_AMENITY");
    }

    @Test
    @DisplayName("Should generate valid refresh token with typ=refresh")
    void testRefreshTokenGeneration() {
        AppUser user = AppUser.builder()
                .id(102L)
                .email("user2@example.com")
                .role("MEMBER")
                .build();

        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(refreshToken)).isTrue();
        assertThat(jwtTokenProvider.isAccessToken(refreshToken)).isFalse();
        assertThat(jwtTokenProvider.getTokenType(refreshToken)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("Should detect tokens issued strictly before tokenInvalidatedBefore and allow same-second/later tokens")
    void testTokenInvalidatedBeforeCheck() {
        AppUser user = AppUser.builder()
                .id(103L)
                .email("user3@example.com")
                .role("RESIDENT")
                .build();

        String token = jwtTokenProvider.generateToken(user);
        LocalDateTime tokenSec = jwtTokenProvider.getIssuedAt(token).toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime futureCutoff = tokenSec.plusSeconds(10);
        LocalDateTime sameSecondCutoff = tokenSec;
        LocalDateTime pastCutoff = tokenSec.minusSeconds(10);

        assertThat(jwtTokenProvider.isIssuedBefore(token, futureCutoff)).isTrue();
        assertThat(jwtTokenProvider.isIssuedBefore(token, sameSecondCutoff)).isFalse();
        assertThat(jwtTokenProvider.isIssuedBefore(token, pastCutoff)).isFalse();
        assertThat(jwtTokenProvider.isIssuedBefore(token, null)).isFalse();
    }
}
