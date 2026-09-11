package com.manacommunity.api.service.impl;

import com.manacommunity.api.security.AuditAction;

import com.manacommunity.api.security.AuditModule;

import com.manacommunity.api.security.AuditService;

import com.manacommunity.api.dto.SportsAuctionTeamRequest;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.*;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.repository.SportsAuctionConfigRepository;
import com.manacommunity.api.repository.SportsAuctionTeamRepository;
import com.manacommunity.api.service.SportsAuctionTeamService;
import com.manacommunity.api.service.NotificationManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsAuctionTeamServiceImpl implements SportsAuctionTeamService {

    private final SportsAuctionTeamRepository teamRepo;
    private final SportsAuctionConfigRepository configRepo;
    private final AppUserRepository userRepo;
    private final com.manacommunity.api.security.AuditService auditService;
    private final NotificationManagementService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<SportsAuctionTeam> getTeams(Long configId) {
        return teamRepo.findByConfigIdOrderByTeamName(configId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportsAuctionTeam> getNominatedCaptains(Long eventId) {
        SportsAuctionConfig config = configRepo.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsAuctionConfig for event", eventId));
        return teamRepo.findByConfigIdAndCaptainNominationTrue(config.getId());
    }

    @Override
    @Transactional
    public SportsAuctionTeam createTeam(SportsAuctionTeamRequest req, Long adminUserId) {
        SportsAuctionConfig config = configRepo.findById(req.configId())
                .orElseThrow(() -> new ResourceNotFoundException("SportsAuctionConfig", req.configId()));
        
        Long ownerUserId = req.ownerUserId() != null ? req.ownerUserId() : adminUserId;
        AppUser teamOwner = userRepo.findById(ownerUserId).orElseThrow(() -> new ResourceNotFoundException("AppUser", ownerUserId));
        SportsAuctionTeam team = SportsAuctionTeam.builder()
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

        SportsAuctionTeam savedTeam = teamRepo.save(team);
        auditService.record(
            com.manacommunity.api.security.AuditAction.TEAM_CREATED,
            com.manacommunity.api.security.AuditModule.AUCTION,
            "SportsAuctionTeam", String.valueOf(savedTeam.getId()),
            null,
            "name=" + savedTeam.getTeamName() + ", budget=" + savedTeam.getTotalBudget());

        try {
            notificationService.createNotification(
                    teamOwner.getId(), NotificationType.TEAM_CREATED, NotificationCategory.AUCTION,
                    "Team Created — " + savedTeam.getTeamName(),
                    "Budget: ₹" + savedTeam.getTotalBudget(),
                    null, ReferenceType.AUCTION_TEAM, savedTeam.getId(),
                    NotificationPriority.NORMAL, null, null);
        } catch (Exception e) {
            log.warn("Failed to persist team-created notification: {}", e.getMessage());
        }

        return savedTeam;
    }

    @Override
    @Transactional
    public SportsAuctionTeam confirmCaptain(Long teamId, boolean confirm, Long callerUserId, boolean isAdmin) {
        SportsAuctionTeam team = teamRepo.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsAuctionTeam", teamId));

        if (!isAdmin) {
            boolean isOwner = team.getOwnerUser() != null && team.getOwnerUser().getId().equals(callerUserId);
            boolean isCaptain = team.getCaptainUser() != null && team.getCaptainUser().getId().equals(callerUserId);
            if (!isOwner && !isCaptain) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You can only confirm captaincy for your own team");
            }
        }

        team.setCaptainConfirmation(confirm);
        SportsAuctionTeam saved = teamRepo.save(team);

        try {
            Long recipientId = saved.getCaptainUser() != null ? saved.getCaptainUser().getId()
                    : (saved.getOwnerUser() != null ? saved.getOwnerUser().getId() : null);
            if (recipientId != null && confirm) {
                notificationService.createNotification(
                        recipientId, NotificationType.CAPTAIN_CONFIRMED, NotificationCategory.AUCTION,
                        "Captain Confirmed — " + saved.getTeamName(),
                        "You are confirmed as captain",
                        null, ReferenceType.AUCTION_TEAM, saved.getId(),
                        NotificationPriority.NORMAL, null, null);
            }
        } catch (Exception e) {
            log.warn("Failed to persist captain-confirmed notification: {}", e.getMessage());
        }

        return saved;
    }

    @Override
    @Transactional
    public SportsAuctionTeam nominateCaptain(Long eventId, Long userId, boolean nominate, String teamName) {
        SportsAuctionConfig config = configRepo.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsAuctionConfig for event", eventId));
        
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        SportsAuctionTeam team = teamRepo.findByConfigIdAndOwnerUserId(config.getId(), userId)
                .orElseGet(() -> SportsAuctionTeam.builder()
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

        if (!nominate) {
            team.setCaptainConfirmation(false);
        }

        SportsAuctionTeam saved = teamRepo.save(team);

        try {
            if (nominate) {
                notificationService.createNotification(
                        user.getId(), NotificationType.CAPTAIN_NOMINATED, NotificationCategory.AUCTION,
                        "Captain Nomination Submitted",
                        "You've been nominated as captain for " + saved.getTeamName(),
                        null, ReferenceType.AUCTION_TEAM, saved.getId(),
                        NotificationPriority.NORMAL, null, null);
            }
        } catch (Exception e) {
            log.warn("Failed to persist captain-nomination notification: {}", e.getMessage());
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportsAuctionTeam> getMyNominations(Long userId) {
        return teamRepo.findByOwnerUserIdOrCaptainUserId(userId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportsAuctionTeam> getCaptainRegistration(Long userId) {
        return teamRepo.findByOwnerUserIdOrCaptainUserId(userId, userId);
    }
}
