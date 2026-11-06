package com.manacommunity.api.sports.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.sports.repository.SportsMatchOfficialRepository;
import com.manacommunity.api.sports.repository.SportsTournamentMatchRepository;
import com.manacommunity.api.sports.scheduler.SportsTournamentMatch;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsScoringAuthorizationService {

    private final SportsTournamentMatchRepository matchRepo;
    private final AppUserRepository userRepo;
    private final SportsMatchOfficialRepository officialRepo;

    private static final Set<String> PRIVILEGED_ROLES = Set.of(
            "ROLE_SUPER_ADMIN", "SUPER_ADMIN",
            "ROLE_ADMIN", "ADMIN",
            "ROLE_SPORTS_ADMIN", "SPORTS_ADMIN",
            "ROLE_SPORTS_REFEREE", "SPORTS_REFEREE"
    );

    @Transactional(readOnly = true)
    public void validateScorerForMatch(Long matchId, Long userId) {
        if (userId == null) {
            throw new AccessDeniedException("Authentication required to score match");
        }

        SportsTournamentMatch match = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsTournamentMatch", matchId));

        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", userId));

        // 1. Check if user is Super Admin
        if (hasAnyRole(user, Set.of("ROLE_SUPER_ADMIN", "SUPER_ADMIN"))) {
            return;
        }

        // 2. Check if user is assigned as match official (referee/scorer/umpire)
        boolean isOfficial = officialRepo.existsByMatchIdAndUserId(matchId, userId);
        if (isOfficial) {
            return;
        }

        // 3. Community tenancy verification
        Long matchCommunityId = match.getCommunity() != null ? match.getCommunity().getId()
                : (match.getConfig() != null && match.getConfig().getCommunity() != null
                ? match.getConfig().getCommunity().getId() : null);

        if (matchCommunityId != null) {
            Long userCommunityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
            if (userCommunityId == null || !userCommunityId.equals(matchCommunityId)) {
                log.warn("Scorer access denied: user {} (community {}) tried to score match {} (community {})",
                        userId, userCommunityId, matchId, matchCommunityId);
                throw new AccessDeniedException("User does not belong to the match's community");
            }
        }

        // 4. Role-based scoring authorization
        if (hasAnyRole(user, PRIVILEGED_ROLES)) {
            return;
        }

        log.warn("Scorer access denied: user {} lacks scoring privileges for match {}", userId, matchId);
        throw new AccessDeniedException("User is not authorized as a scorer or official for this match");
    }

    public void validateTeamAndPlayer(SportsTournamentMatch match, Long teamId, Long playerId) {
        if (teamId != null) {
            boolean isTeamA = match.getTeamA() != null && match.getTeamA().getId().equals(teamId);
            boolean isTeamB = match.getTeamB() != null && match.getTeamB().getId().equals(teamId);
            if (!isTeamA && !isTeamB) {
                throw new InvalidInputException("Team " + teamId + " does not belong to match " + match.getId());
            }
        }
    }

    private boolean hasAnyRole(AppUser user, Set<String> targetRoles) {
        if (user.getRole() != null) {
            for (String r : user.getRole().split(",")) {
                String trimmed = r.trim().toUpperCase();
                if (targetRoles.contains(trimmed) || targetRoles.contains("ROLE_" + trimmed)) {
                    return true;
                }
            }
        }
        if (user.getUserRoles() != null) {
            for (Role role : user.getUserRoles()) {
                if (role.getName() != null) {
                    String name = role.getName().toUpperCase();
                    if (targetRoles.contains(name) || targetRoles.contains("ROLE_" + name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
