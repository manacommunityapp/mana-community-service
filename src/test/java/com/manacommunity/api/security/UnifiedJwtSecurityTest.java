package com.manacommunity.api.security;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedJwtSecurityTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String TEST_SECRET = "very-secure-256-bit-secret-key-for-testing-only-1234567890!";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                TEST_SECRET,
                900000,
                604800000,
                "mana-community",
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
}
