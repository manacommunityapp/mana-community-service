package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.AuctionTeamRequest;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.model.AuctionConfig;
import com.manacommunity.api.model.AuctionTeam;
import com.manacommunity.api.repository.AppUserRepository;
import com.manacommunity.api.repository.AuctionConfigRepository;
import com.manacommunity.api.repository.AuctionTeamRepository;
import com.manacommunity.api.service.AuctionTeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionTeamServiceImpl implements AuctionTeamService {

    private final AuctionTeamRepository teamRepo;
    private final AuctionConfigRepository configRepo;
    private final AppUserRepository userRepo;
    private final com.manacommunity.api.security.AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<AuctionTeam> getTeams(Long configId) {
        return teamRepo.findByConfigIdOrderByTeamName(configId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuctionTeam> getNominatedCaptains(Long eventId) {
        AuctionConfig config = configRepo.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("AuctionConfig for event", eventId));
        return teamRepo.findByConfigIdAndCaptainNominationTrue(config.getId());
    }

    @Override
    @Transactional
    public AuctionTeam createTeam(AuctionTeamRequest req, Long adminUserId) {
        AuctionConfig config = configRepo.findById(req.configId())
                .orElseThrow(() -> new ResourceNotFoundException("AuctionConfig", req.configId()));
        
        Long ownerUserId = req.ownerUserId() != null ? req.ownerUserId() : adminUserId;
        AppUser teamOwner = userRepo.findById(ownerUserId).orElseThrow(() -> new ResourceNotFoundException("AppUser", ownerUserId));
        AuctionTeam team = AuctionTeam.builder()
            .config(config)
            .teamName(req.teamName())
            .ownerName(req.ownerName())
            .ownerUser(teamOwner)
            .captainUser(teamOwner)
            .eventId(config.getEvent() != null ? config.getEvent().getId() : null)
            .colorHex(req.colorHex())
            .totalBudget(req.totalBudget())
            .remainingBudget(req.totalBudget())
            .spent(0L)
            .captainNomination(false)
            .captainConfirmation(false)
            .build();

        AuctionTeam savedTeam = teamRepo.save(team);
        auditService.record(
            com.manacommunity.api.security.AuditAction.TEAM_CREATED,
            com.manacommunity.api.security.AuditModule.AUCTION,
            "AuctionTeam", String.valueOf(savedTeam.getId()),
            null,
            "name=" + savedTeam.getTeamName() + ", budget=" + savedTeam.getTotalBudget());
        return savedTeam;
    }

    @Override
    @Transactional
    public AuctionTeam confirmCaptain(Long teamId, boolean confirm, Long callerUserId, boolean isAdmin) {
        AuctionTeam team = teamRepo.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("AuctionTeam", teamId));

        if (!isAdmin) {
            boolean isOwner = team.getOwnerUser() != null && team.getOwnerUser().getId().equals(callerUserId);
            boolean isCaptain = team.getCaptainUser() != null && team.getCaptainUser().getId().equals(callerUserId);
            if (!isOwner && !isCaptain) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You can only confirm captaincy for your own team");
            }
        }

        team.setCaptainConfirmation(confirm);
        return teamRepo.save(team);
    }

    @Override
    @Transactional
    public AuctionTeam nominateCaptain(Long eventId, Long userId, boolean nominate, String teamName) {
        AuctionConfig config = configRepo.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("AuctionConfig for event", eventId));
        
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        AuctionTeam team = teamRepo.findByConfigIdAndOwnerUserId(config.getId(), userId)
                .orElseGet(() -> AuctionTeam.builder()
                        .config(config)
                        .ownerUser(user)
                        .captainUser(user)
                        .ownerName(user.getFullName())
                        .eventId(eventId)
                        .totalBudget(config.getBudgetPerTeam())
                        .remainingBudget(config.getBudgetPerTeam())
                        .spent(0L)
                        .build());

        team.setCaptainNomination(nominate);
        if (teamName != null && !teamName.isEmpty()) {
            team.setTeamName(teamName);
        } else if (team.getTeamName() == null) {
            team.setTeamName(user.getFullName() + "'s Team");
        }
        
        // If withdrawing, we might want to keep the record but set flags to false
        // or potentially delete if it was just a nomination. 
        // For now, setting flags to false is safer.
        if (!nominate) {
            team.setCaptainConfirmation(false);
        }

        return teamRepo.save(team);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuctionTeam> getMyNominations(Long userId) {
        return teamRepo.findByOwnerUserIdOrCaptainUserId(userId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuctionTeam> getCaptainRegistration(Long userId) {
        return teamRepo.findByOwnerUserIdOrCaptainUserId(userId, userId);
    }
}
