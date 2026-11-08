package com.manacommunity.api.unit.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.sports.model.SportsAuctionTeam;
import com.manacommunity.api.sports.repository.SportsMatchOfficialRepository;
import com.manacommunity.api.sports.repository.SportsTournamentMatchRepository;
import com.manacommunity.api.sports.scheduler.SportsTournamentConfig;
import com.manacommunity.api.sports.scheduler.SportsTournamentMatch;
import com.manacommunity.api.sports.service.SportsScoringAuthorizationService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SportsScoringAuthorizationService Tests")
class SportsScoringAuthorizationServiceTest {

    @Mock
    private SportsTournamentMatchRepository matchRepo;

    @Mock
    private AppUserRepository userRepo;

    @Mock
    private SportsMatchOfficialRepository officialRepo;

    @InjectMocks
    private SportsScoringAuthorizationService authService;

    private Community community1;
    private Community community2;
    private SportsTournamentMatch match;

    @BeforeEach
    void setUp() {
        community1 = Community.builder().id(100L).name("Community 1").build();
        community2 = Community.builder().id(200L).name("Community 2").build();

        SportsTournamentConfig config = SportsTournamentConfig.builder()
                .id(1L)
                .community(community1)
                .build();

        match = SportsTournamentMatch.builder()
                .id(10L)
                .config(config)
                .community(community1)
                .teamA(SportsAuctionTeam.builder().id(1L).teamName("Team Alpha").build())
                .teamB(SportsAuctionTeam.builder().id(2L).teamName("Team Beta").build())
                .build();
    }

    @Test
    @DisplayName("Super admin can score matches across all communities")
    void superAdminAllowedAcrossCommunities() {
        AppUser superAdmin = AppUser.builder()
                .id(999L)
                .role("SUPER_ADMIN")
                .community(community2)
                .build();

        when(matchRepo.findById(10L)).thenReturn(Optional.of(match));
        when(userRepo.findById(999L)).thenReturn(Optional.of(superAdmin));

        assertThatCode(() -> authService.validateScorerForMatch(10L, 999L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Admin in same community is authorized to score")
    void communityAdminAuthorized() {
        AppUser admin = AppUser.builder()
                .id(101L)
                .role("ADMIN")
                .community(community1)
                .build();

        when(matchRepo.findById(10L)).thenReturn(Optional.of(match));
        when(userRepo.findById(101L)).thenReturn(Optional.of(admin));
        when(officialRepo.existsByMatchIdAndUserId(10L, 101L)).thenReturn(false);

        assertThatCode(() -> authService.validateScorerForMatch(10L, 101L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Assigned match official is authorized even without admin role")
    void assignedOfficialAuthorized() {
        AppUser officialUser = AppUser.builder()
                .id(102L)
                .role("USER")
                .community(community1)
                .build();

        when(matchRepo.findById(10L)).thenReturn(Optional.of(match));
        when(userRepo.findById(102L)).thenReturn(Optional.of(officialUser));
        when(officialRepo.existsByMatchIdAndUserId(10L, 102L)).thenReturn(true);

        assertThatCode(() -> authService.validateScorerForMatch(10L, 102L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("User from a different community is denied access")
    void differentCommunityDenied() {
        AppUser externalAdmin = AppUser.builder()
                .id(201L)
                .role("SPORTS_ADMIN")
                .community(community2)
                .build();

        when(matchRepo.findById(10L)).thenReturn(Optional.of(match));
        when(userRepo.findById(201L)).thenReturn(Optional.of(externalAdmin));
        when(officialRepo.existsByMatchIdAndUserId(10L, 201L)).thenReturn(false);

        assertThatThrownBy(() -> authService.validateScorerForMatch(10L, 201L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User does not belong to the match's community");
    }

    @Test
    @DisplayName("Regular user without official assignment is denied access")
    void regularUserDenied() {
        AppUser normalUser = AppUser.builder()
                .id(103L)
                .role("USER")
                .community(community1)
                .build();

        when(matchRepo.findById(10L)).thenReturn(Optional.of(match));
        when(userRepo.findById(103L)).thenReturn(Optional.of(normalUser));
        when(officialRepo.existsByMatchIdAndUserId(10L, 103L)).thenReturn(false);

        assertThatThrownBy(() -> authService.validateScorerForMatch(10L, 103L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User is not authorized as a scorer or official for this match");
    }

    @Test
    @DisplayName("Team validation rejects teams not participating in the match")
    void teamValidationRejectsForeignTeam() {
        assertThatThrownBy(() -> authService.validateTeamAndPlayer(match, 999L, null))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Team 999 does not belong to match 10");
    }
}
