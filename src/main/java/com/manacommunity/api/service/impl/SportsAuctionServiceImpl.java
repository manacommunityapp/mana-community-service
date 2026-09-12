package com.manacommunity.api.service.impl;

import com.manacommunity.api.security.AuditAction;

import com.manacommunity.api.security.AuditModule;

import com.manacommunity.api.security.AuditService;

import com.manacommunity.api.user.repository.AppUserRepository;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.exception.AuctionStateException;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.service.SportsAuctionService;
import com.manacommunity.api.service.SportsAuctionWebSocketService;
import com.manacommunity.api.service.NotificationManagementService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsAuctionServiceImpl implements SportsAuctionService {

    private final SportsAuctionConfigRepository configRepo;
    private final SportsAuctionPlayerRepository playerRepo;
    private final SportsAuctionBidRepository bidRepo;
    private final SportsAuctionTeamRepository teamRepo;
    private final SportsAuctionSessionLogRepository logRepo;
    private final SportsMetaRepository sportRepo;
    private final AppUserRepository userRepo;
    private final SportsAuctionConfigCategoryRepository auctionConfigCategoryRepo;
    private final SportsAuctionDisputeCommitteeRepository committeeRepo;
    private final com.manacommunity.api.repository.SportsEventRegistrationRepository registrationRepo;
    private final com.manacommunity.api.repository.SportsEventRepository eventRepo;
    private final com.manacommunity.api.security.AuditService auditService;
    private final MeterRegistry meterRegistry;
    private final NotificationManagementService notificationService;
    private final SportsAuctionWebSocketService auctionWs;

    @Override
    public List<SportsAuctionConfig> getConfigsBySportAndCommunity(Long sportId, Long communityId) {
        if (communityId == null) {
            return configRepo.findBySportIdOrderByCreatedAtDesc(sportId);
        }
        return configRepo.findBySportIdAndCreatedByCommunityIdOrderByCreatedAtDesc(sportId, communityId);
    }
    @Override
    public List<SportsAuctionConfig> getAllConfigsByCommunity(Long communityId) {
        if (communityId == null) return List.of();
        return configRepo.findByCreatedByCommunityIdOrderByCreatedAtDesc(communityId);
    }
    @Override
    @Transactional(readOnly = true)
    public SportsAuctionConfigResponse getConfigResponse(Long id) {
        SportsAuctionConfig config = configRepo.findById(id).orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("SportsAuctionConfig", id));
        return toConfigResponse(config);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportsAuctionConfigResponse> getConfigResponsesBySportAndCommunity(Long sportId, Long communityId) {
        List<SportsAuctionConfig> configs = communityId == null
                ? configRepo.findBySportIdWithAssociations(sportId)
                : configRepo.findBySportIdAndCommunityIdWithAssociations(sportId, communityId);
        return configs.stream().map(this::toConfigResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportsAuctionConfigResponse> getConfigResponsesByCommunity(Long communityId) {
        if (communityId == null) return List.of();
        return configRepo.findByCommunityIdWithAssociations(communityId).stream()
                .map(this::toConfigResponse).toList();
    }

    private SportsAuctionConfigResponse toConfigResponse(SportsAuctionConfig config) {
        return new SportsAuctionConfigResponse(
                config.getId(),
                config.getEvent() != null ? config.getEvent().getId() : null,
                config.getEvent() != null ? config.getEvent().getName() : null,
                config.getSport().getName(),
                config.getSeasonName(),
                config.getAuctionFormat().name(),
                config.getTotalTeams(),
                config.getTotalPlayers(),
                config.getBudgetPerTeam(),
                config.getBasePrice(),
                config.getBidIncrementDefault(),
                config.getBidIncrementThreshold(),
                config.getBidIncrementAbove(),
                config.getBidTimerSeconds(),
                config.getRtmEnabled(),
                config.getUnsoldRule().name(),
                config.getStatus().name(),
                config.getPlayers().stream().map(SportsAuctionPlayer::getCategory).distinct().collect(Collectors.toList()),
                config.getCommitteeMembers().stream().map(SportsAuctionDisputeCommittee::getMemberName).collect(Collectors.toList())
        );
    }

    // ── CREATE AUCTION CONFIG ─────────────────────────────────────
    @Override
    @Transactional
    public SportsAuctionConfig createConfig(SportsAuctionConfigRequest req, Long adminUserId) {
        if (configRepo.existsBySportIdAndSeasonName(req.sportId(), req.seasonName()))
            throw new AuctionStateException("Auction already exists for this sport and season");

        SportsEvent sportsEvent = req.eventId() != null ? eventRepo.findById(req.eventId()).orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("SportsEvent", req.eventId())) : null;
        SportsAuctionConfig config = SportsAuctionConfig.builder()
            .sport(sportRepo.findById(req.sportId()).orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("Sport", req.sportId())))
            .event(sportsEvent)
            .community(sportsEvent != null ? sportsEvent.getCommunity() : null)
            .seasonName(req.seasonName())
            .auctionFormat(SportsAuctionConfig.AuctionFormat.valueOf(req.auctionFormat()))
            .totalTeams(req.totalTeams())
            .totalPlayers(req.totalPlayers())
            .budgetPerTeam(req.budgetPerTeam())
            .basePrice(req.basePrice())
            .bidIncrementDefault(req.bidIncrementDefault())
            .bidIncrementThreshold(req.bidIncrementThreshold())
            .bidIncrementAbove(req.bidIncrementAbove())
            .bidTimerSeconds(req.bidTimerSeconds())
            .rtmEnabled(Boolean.TRUE.equals(req.rtmEnabled()))
            .unsoldRule(SportsAuctionConfig.UnsoldRule.valueOf(
                req.unsoldRule() != null ? req.unsoldRule() : "ROTATION_AUCTION"))
            .status(SportsAuctionConfig.AuctionStatus.DRAFT)
            .createdBy(userRepo.getReferenceById(adminUserId))
            .build();

        SportsAuctionConfig saved = configRepo.save(config);

        // Persist categories
        if (req.categories() != null) {
            req.categories().forEach(cat ->
                auctionConfigCategoryRepo.save(new SportsAuctionConfigCategory(saved, cat)));
        }
        // Persist committee members
        if (req.committeeMembers() != null) {
            req.committeeMembers().forEach(name ->
                committeeRepo.save(SportsAuctionDisputeCommittee.builder()
                    .config(saved).memberName(name).role("COMMITTEE_MEMBER").build()));
        }
        log.info("Auction config created: sport={} season={}", req.sportId(), req.seasonName());

        auditService.record(
            com.manacommunity.api.security.AuditAction.AUCTION_CONFIG_CREATED,
            com.manacommunity.api.security.AuditModule.AUCTION,
            "SportsAuctionConfig",
            String.valueOf(saved.getId()),
            null,
            "sportId=" + req.sportId() + ", season=" + req.seasonName() + ", teams=" + req.totalTeams()
        );

        return saved;
    }

    // ── UPDATE CONFIG (dynamically update pricing/rules) ──────────
    @Override
    @Transactional
    public SportsAuctionConfig updateConfig(Long configId, SportsAuctionConfigRequest req) {
        SportsAuctionConfig config = configRepo.findById(configId)
            .orElseThrow(() -> new IllegalArgumentException("Auction config not found: " + configId));

        if (config.getStatus() == SportsAuctionConfig.AuctionStatus.LIVE)
            throw new AuctionStateException("Cannot update rules while auction is LIVE. Pause first.");

        // Apply all dynamic rule changes
        if (req.eventId() != null) {
            config.setEvent(eventRepo.findById(req.eventId()).orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("SportsEvent", req.eventId())));
        } else {
            config.setEvent(null);
        }
        config.setTotalTeams(req.totalTeams());
        config.setTotalPlayers(req.totalPlayers());
        config.setBudgetPerTeam(req.budgetPerTeam());
        config.setBasePrice(req.basePrice());
        config.setBidIncrementDefault(req.bidIncrementDefault());
        config.setBidIncrementThreshold(req.bidIncrementThreshold());
        config.setBidIncrementAbove(req.bidIncrementAbove());
        config.setBidTimerSeconds(req.bidTimerSeconds());
        config.setRtmEnabled(Boolean.TRUE.equals(req.rtmEnabled()));
        if (req.unsoldRule() != null)
            config.setUnsoldRule(SportsAuctionConfig.UnsoldRule.valueOf(req.unsoldRule()));

        SportsAuctionConfig saved = configRepo.save(config);

        auditService.record(
            com.manacommunity.api.security.AuditAction.AUCTION_CONFIG_UPDATED,
            com.manacommunity.api.security.AuditModule.AUCTION,
            "SportsAuctionConfig",
            String.valueOf(saved.getId()),
            null,
            "status=" + saved.getStatus() + ", teams=" + saved.getTotalTeams()
        );

        return saved;
    }

    @Override
    @Transactional
    public SportsAuctionConfig updateStatus(Long configId, String status) {
        SportsAuctionConfig config = configRepo.findById(configId)
                .orElseThrow(() -> new com.manacommunity.api.exception.ManaCommunityException(
                        "Please configure the auction configuration before starting the auction.",
                        org.springframework.http.HttpStatus.BAD_REQUEST, "CONFIG_NOT_FOUND"));

        SportsAuctionConfig.AuctionStatus newStatus;
        try {
            newStatus = SportsAuctionConfig.AuctionStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new AuctionStateException("Invalid auction status: " + status
                    + ". Valid values: " + java.util.Arrays.toString(SportsAuctionConfig.AuctionStatus.values()));
        }

        SportsAuctionConfig.AuctionStatus oldStatus = config.getStatus();
        if (oldStatus == newStatus) return config;

        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new AuctionStateException("Cannot transition auction from " + oldStatus + " to " + newStatus + ".");
        }

        if (newStatus == SportsAuctionConfig.AuctionStatus.LIVE || newStatus == SportsAuctionConfig.AuctionStatus.ACTIVE) {
            long teamCount = teamRepo.countByConfigId(configId);
            if (teamCount < 2) {
                throw new AuctionStateException("Cannot start auction: At least 2 teams must be configured.");
            }
            long playerCount = playerRepo.countByConfigId(configId);
            if (playerCount == 0) {
                throw new AuctionStateException("Cannot start auction: Player pool is empty.");
            }
        }

        config.setStatus(newStatus);
        SportsAuctionConfig savedConfig = configRepo.save(config);

        auctionWs.broadcastStatusChange(configId, oldStatus.name(), newStatus.name());

        // Audit the lifecycle transitions only (start / end), not every save.
        if (oldStatus != newStatus) {
            if (newStatus == SportsAuctionConfig.AuctionStatus.LIVE || newStatus == SportsAuctionConfig.AuctionStatus.ACTIVE) {
                auditService.record(
                    com.manacommunity.api.security.AuditAction.AUCTION_STARTED,
                    com.manacommunity.api.security.AuditModule.AUCTION,
                    "SportsAuctionConfig", String.valueOf(configId),
                    String.valueOf(oldStatus), String.valueOf(newStatus));
                notifyAuctionTeamOwners(savedConfig, NotificationType.AUCTION_STARTED,
                        "Auction Started — " + savedConfig.getSeasonName(),
                        "The auction is now live!", NotificationPriority.HIGH);
            } else if (newStatus == SportsAuctionConfig.AuctionStatus.COMPLETED) {
                auditService.record(
                    com.manacommunity.api.security.AuditAction.AUCTION_ENDED,
                    com.manacommunity.api.security.AuditModule.AUCTION,
                    "SportsAuctionConfig", String.valueOf(configId),
                    String.valueOf(oldStatus), String.valueOf(newStatus));
                notifyAuctionTeamOwners(savedConfig, NotificationType.AUCTION_COMPLETED,
                        "Auction Completed — " + savedConfig.getSeasonName(),
                        "The auction has concluded. Check your team roster.", NotificationPriority.NORMAL);
            }
        }
        return savedConfig;
    }

    // ── GET CURRENT PLAYER (with bid info) ────────────────────────
    @Override
    @Transactional(readOnly = true)
    public PlayerWithBidResponse getCurrentPlayer(Long configId) {
        SportsAuctionConfig config = configRepo.findById(configId).orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("SportsAuctionConfig", configId));

        // First check if a player is already in SELLING state
        java.util.Optional<SportsAuctionPlayer> sellingPlayerOpt = playerRepo.findSellingPlayer(configId);
        
        SportsAuctionPlayer player;
        if (sellingPlayerOpt.isPresent()) {
            player = sellingPlayerOpt.get();
        } else {
            List<SportsAuctionPlayer> queued = playerRepo.findQueuedByConfig(configId);
            if (queued.isEmpty()) {
                throw new AuctionStateException("No more players in queue");
            }
            player = queued.get(0);
        }

        Long currentBid = bidRepo.findMaxBidForPlayer(player.getId())
            .orElse((long) player.getBasePrice());

        String currentBidTeam = bidRepo.findTopBidsForPlayer(player.getId(), PageRequest.of(0, 1))
            .stream().findFirst()
            .map(b -> b.getTeam().getTeamName())
            .orElse(null);

        int nextIncrement = config.calculateNextIncrement(currentBid);
        long nextBid = currentBid + nextIncrement;

        return new PlayerWithBidResponse(
            player.getId(), player.getPlayerName(), player.getCategory(),
            player.getPlayerRole(), player.getAge(), player.getBasePrice(),
            player.getStatsJson(), currentBid, nextBid, nextIncrement,
            currentBidTeam, player.getQueueOrder(), player.getStatus().name()
        );
    }

    // ── PLACE BID ─────────────────────────────────────────────────
    @Override
    @Transactional
    public SportsAuctionBid placeBid(BidRequest req, Long biddingUserId) {
        SportsAuctionConfig config = configRepo.findById(req.configId()).orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("SportsAuctionConfig", req.configId()));

        // Validate auction is live
        if (config.getStatus() !=  SportsAuctionConfig.AuctionStatus.LIVE  &&
                config.getStatus() != SportsAuctionConfig.AuctionStatus.ACTIVE)
            throw new AuctionStateException("Auction is not LIVE");

        // Lock player row to serialize all bids on the same player
        SportsAuctionPlayer player = playerRepo.findByIdForUpdate(req.playerId()).orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("SportsAuctionPlayer", req.playerId()));

        // Lock team row to get a consistent budget snapshot
        SportsAuctionTeam team = teamRepo.findByIdForUpdate(req.teamId()).orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("SportsAuctionTeam", req.teamId()));

        // Budget check (under lock — no concurrent bid can see stale budget)
        if (team.getRemainingBudget() < req.bidAmount())
            throw new AuctionStateException("Team budget insufficient. Available: ₹"
                + team.getRemainingBudget());

        // Minimum bid check (under player lock — serialized per player)
        Long currentMax = bidRepo.findMaxBidForPlayer(player.getId()).orElse(0L);
        long minRequired = currentMax == 0
            ? config.getBasePrice()
            : currentMax + config.calculateNextIncrement(currentMax);

        if (req.bidAmount() < minRequired)
            throw new AuctionStateException("Bid must be at least ₹" + minRequired
                + ". Increment rule: "
                + (currentMax >= config.getBidIncrementThreshold() ? "₹" + config.getBidIncrementAbove() : "₹" + config.getBidIncrementDefault()));

        // Set player to SELLING if still QUEUED
        if (player.getStatus() == SportsAuctionPlayer.PlayerStatus.QUEUED) {
            player.setStatus(SportsAuctionPlayer.PlayerStatus.SELLING);
            playerRepo.save(player);
        }

        long baseline = currentMax == 0 ? config.getBasePrice() : currentMax;
        int incrementUsed = (int) (req.bidAmount() - baseline);

        SportsAuctionBid bid = SportsAuctionBid.builder()
            .config(config)
            .player(player)
            .team(team)
            .bidAmount(req.bidAmount())
            .incrementUsed(incrementUsed)
            .isRtm(Boolean.TRUE.equals(req.isRtm()))
            .bidByUser(userRepo.getReferenceById(biddingUserId))
            .build();

        // Capture previous leading team before saving new bid (for outbid notification)
        SportsAuctionTeam previousLeader = bidRepo.findTopBidsForPlayer(player.getId(), PageRequest.of(0, 1))
                .stream().findFirst().map(SportsAuctionBid::getTeam).orElse(null);

        log.info("Bid placed: player={} team={} amount={}", player.getPlayerName(),
            team.getTeamName(), req.bidAmount());
        SportsAuctionBid saved = bidRepo.save(bid);
        // Metric: count every accepted bid (after all validation passed). Tagged with
        // RTM vs normal so the live-auction bid rate is visible in Grafana / alarms.
        meterRegistry.counter("auction.bids.placed",
            "rtm", String.valueOf(Boolean.TRUE.equals(req.isRtm()))).increment();
        auditService.record(
            com.manacommunity.api.security.AuditAction.BID_PLACED,
            com.manacommunity.api.security.AuditModule.AUCTION,
            "SportsAuctionPlayer", String.valueOf(player.getId()),
            null,
            "team=" + team.getTeamName() + ", amount=" + req.bidAmount());

        try {
            if (previousLeader != null && previousLeader.getOwnerUser() != null
                    && !previousLeader.getId().equals(team.getId())) {
                notificationService.createNotification(
                        previousLeader.getOwnerUser().getId(), NotificationType.BID_OUTBID, NotificationCategory.AUCTION,
                        "Outbid on " + player.getPlayerName(),
                        team.getTeamName() + " bid ₹" + req.bidAmount(),
                        null, ReferenceType.AUCTION_PLAYER, player.getId(),
                        NotificationPriority.HIGH, null, null);
            }
        } catch (Exception e) {
            log.warn("Failed to persist outbid notification: {}", e.getMessage());
        }


        auctionWs.broadcastBid(config.getId(), SportsAuctionBidResponse.builder()
            .id(saved.getId()).configId(config.getId())
            .playerId(player.getId()).teamId(team.getId())
            .teamName(team.getTeamName()).bidAmount(saved.getBidAmount())
            .incrementUsed(saved.getIncrementUsed()).isRtm(saved.getIsRtm())
            .bidByUserId(biddingUserId).bidAt(saved.getBidAt()).build());

        return saved;
    }

    // ── SOLD PLAYER ───────────────────────────────────────────────
    @Override
    @Transactional
    public SportsAuctionPlayer soldPlayer(SoldPlayerRequest req, Long adminUserId) {
        // Lock player first, then team — consistent ordering prevents deadlocks
        SportsAuctionPlayer player = playerRepo.findByIdForUpdate(req.playerId()).orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("SportsAuctionPlayer", req.playerId()));
        SportsAuctionTeam   team   = teamRepo.findByIdForUpdate(req.teamId()).orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("SportsAuctionTeam", req.teamId()));

        if (player.getStatus() == SportsAuctionPlayer.PlayerStatus.SOLD)
            throw new AuctionStateException("Player " + player.getPlayerName() + " is already SOLD");

        Long soldPrice = bidRepo.findMaxBidForPlayer(player.getId())
            .orElseThrow(() -> new IllegalStateException("No bids placed for this player"));

        // Atomic budget deduction under pessimistic lock
        if (team.getRemainingBudget() < soldPrice)
            throw new AuctionStateException("Team budget insufficient for final sale");

        team.setRemainingBudget(team.getRemainingBudget() - soldPrice);
        team.setSpent((team.getSpent() == null ? 0L : team.getSpent()) + soldPrice);
        teamRepo.save(team);

        player.setStatus(SportsAuctionPlayer.PlayerStatus.SOLD);
        player.setAssignedTeam(team);
        player.setSoldPrice(soldPrice);
        player.setRtmUsed(false);
        player.setSoldAt(LocalDateTime.now());

        // Log it
        logRepo.save(SportsAuctionSessionLog.builder()
            .config(player.getConfig())
            .action("PLAYER_SOLD")
            .player(player)
            .team(team)
            .amount(soldPrice)
            .performedBy(userRepo.getReferenceById(adminUserId))
            .notes(player.getPlayerName() + " sold to " + team.getTeamName() + " for ₹" + soldPrice)
            .build());

        log.info("SOLD: {} → {} for ₹{}", player.getPlayerName(), team.getTeamName(), soldPrice);
        SportsAuctionPlayer savedPlayer = playerRepo.save(player);
        auditService.record(
            com.manacommunity.api.security.AuditAction.PLAYER_SOLD,
            com.manacommunity.api.security.AuditModule.AUCTION,
            "SportsAuctionPlayer", String.valueOf(player.getId()),
            null,
            "soldTo=" + team.getTeamName() + ", price=" + soldPrice);

        try {
            if (team.getOwnerUser() != null && team.getOwnerUser().getId() != null) {
                notificationService.createNotification(
                        team.getOwnerUser().getId(), NotificationType.PLAYER_SOLD, NotificationCategory.AUCTION,
                        player.getPlayerName() + " sold to " + team.getTeamName(),
                        "Acquired for ₹" + soldPrice + " | Budget remaining: ₹" + team.getRemainingBudget(),
                        null, ReferenceType.AUCTION_PLAYER, player.getId(),
                        NotificationPriority.HIGH, null, null);
            }
        } catch (Exception e) {
            log.warn("Failed to persist player-sold notification: {}", e.getMessage());
        }


        auctionWs.broadcastPlayerSold(player.getConfig().getId(),
            new SportsAuctionWebSocketService.PlayerSoldPayload(
                player.getId(), player.getPlayerName(),
                team.getId(), team.getTeamName(), soldPrice));

        return savedPlayer;
    }

    // ── PASS PLAYER ───────────────────────────────────────────────
    @Override
    @Transactional
    public SportsAuctionPlayer passPlayer(Long playerId, Long adminUserId) {
        SportsAuctionPlayer player = playerRepo.findByIdForUpdate(playerId).orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("SportsAuctionPlayer", playerId));
        SportsAuctionConfig config = player.getConfig();

        if (config.getUnsoldRule() == SportsAuctionConfig.UnsoldRule.ROTATION_AUCTION) {
            // Rotation: move player to end of queue
            int maxOrder = playerRepo.findQueuedByConfig(config.getId())
                .stream().mapToInt(SportsAuctionPlayer::getQueueOrder).max().orElse(0);
            player.setQueueOrder(maxOrder + 1);
            player.setStatus(SportsAuctionPlayer.PlayerStatus.QUEUED);
        } else {
            player.setStatus(SportsAuctionPlayer.PlayerStatus.PASSED);
        }

        logRepo.save(SportsAuctionSessionLog.builder()
            .config(config).action("PLAYER_PASSED").player(player)
            .performedBy(userRepo.getReferenceById(adminUserId)).build());

        SportsAuctionPlayer saved = playerRepo.save(player);

        auctionWs.broadcastPlayerPassed(config.getId(),
            new SportsAuctionWebSocketService.PlayerPassedPayload(
                player.getId(), player.getPlayerName(),
                player.getStatus().name(), player.getQueueOrder()));

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public SportsAuctionStatsResponse getAuctionStats(Long configId) {
        SportsAuctionConfig config = configRepo.findById(configId)
                .orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("SportsAuctionConfig", configId));

        long totalPlayers = 0;
        if (config.getEvent() != null) {
            // Direct reference to the linked event is now available
            totalPlayers = registrationRepo.countByEventIdAndStatus(config.getEvent().getId(), com.manacommunity.api.model.SportsEventRegistration.RegistrationStatus.CONFIRMED);
        } else {
            // Fallback if no event linked
            Long communityId = config.getCreatedBy() != null && config.getCreatedBy().getCommunity() != null 
                    ? config.getCreatedBy().getCommunity().getId() : null;
            Long sportId = config.getSport().getId();
            if (communityId != null) {
                totalPlayers = registrationRepo.countActiveRegistrationsForCommunityAndSport(
                        communityId, 
                        sportId, 
                        //com.manacommunity.api.model.SportsEventRegistration.RegistrationStatus.CONFIRMED,
                        java.util.Arrays.asList(com.manacommunity.api.model.SportsEvent.EventStatus.COMPLETED, com.manacommunity.api.model.SportsEvent.EventStatus.CANCELLED)
                );
            } else {
                totalPlayers = playerRepo.countByConfigId(configId);
            }
        }

        // Live stats directly from database using specific count/sum queries
        long soldPlayers = playerRepo.countByConfigIdAndStatus(configId, SportsAuctionPlayer.PlayerStatus.SOLD);
        long queuedPlayers = playerRepo.countQueuedByConfig(configId);
        
        long totalTeams = teamRepo.countByConfigId(configId);
        long totalBudget = teamRepo.sumBudgetByConfigId(configId);
        long totalSpent = teamRepo.sumSpentByConfigId(configId);

        return new SportsAuctionStatsResponse(totalPlayers, soldPlayers, queuedPlayers, totalTeams, totalBudget, totalSpent);
    }

    @Override
    @Transactional(readOnly = true)
    public long getConfirmedRegistrationCount(Long configId) {
        SportsAuctionConfig config = configRepo.findById(configId)
                .orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("SportsAuctionConfig", configId));

        if (config.getEvent() != null) {
            return registrationRepo.countByEventIdAndStatus(config.getEvent().getId(), com.manacommunity.api.model.SportsEventRegistration.RegistrationStatus.CONFIRMED);
        }

        // Fallback
        Long communityId = config.getCreatedBy() != null && config.getCreatedBy().getCommunity() != null 
                ? config.getCreatedBy().getCommunity().getId() : null;
        Long sportId = config.getSport().getId();
        if (communityId != null) {
            return registrationRepo.countActiveRegistrationsForCommunityAndSport(
                    communityId, 
                    sportId, 
                    //com.manacommunity.api.model.SportsEventRegistration.RegistrationStatus.CONFIRMED,
                    java.util.Arrays.asList(com.manacommunity.api.model.SportsEvent.EventStatus.COMPLETED, com.manacommunity.api.model.SportsEvent.EventStatus.CANCELLED)
            );
        }
        return 0;
    }

    // ── GET BID HISTORY ───────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<SportsAuctionBid> getBidHistory(Long playerId) {
        return bidRepo.findByPlayerIdOrderByBidAtDesc(playerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportsAuctionPlayer> getPlayers(Long configId, String category, String status) {
        if (category != null) {
            return playerRepo.findByConfigIdAndCategoryOrderByQueueOrder(configId, category);
        }
        if (status != null) {
             return playerRepo.findByConfigIdAndStatusOrderByQueueOrderAsc(configId, SportsAuctionPlayer.PlayerStatus.valueOf(status));
        }
        return playerRepo.findByConfigId(configId);
    }

    @Override
    @Transactional
    public SportsAuctionPlayer createPlayer(Long configId, SportsAuctionPlayerRequest req) {
        SportsAuctionConfig config = configRepo.findById(configId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid auction config ID"));

        // Compile stats JSON string manually or using a library.
        // For simplicity, constructing a JSON string from the DTO properties.
        StringBuilder statsJson = new StringBuilder("{");
        if (req.getMatches() != null) statsJson.append("\"matches\":").append(req.getMatches()).append(",");
        if (req.getRuns() != null) statsJson.append("\"runs\":").append(req.getRuns()).append(",");
        if (req.getWickets() != null) statsJson.append("\"wickets\":").append(req.getWickets()).append(",");
        if (req.getStrikeRate() != null) statsJson.append("\"strikeRate\":").append(req.getStrikeRate()).append(",");
        if (req.getEconomy() != null) statsJson.append("\"economy\":").append(req.getEconomy()).append(",");
        if (req.getAvgScore() != null) statsJson.append("\"avgScore\":").append(req.getAvgScore()).append(",");
        
        String finalStats = "{}";
        if (statsJson.length() > 1) {
            statsJson.deleteCharAt(statsJson.length() - 1); // remove trailing comma
            statsJson.append("}");
            finalStats = statsJson.toString();
        }

        // Get max queue order for the config to append this player
        int maxOrder = playerRepo.findByConfigIdAndCategoryOrderByQueueOrder(configId, req.getCategory())
                                 .stream().mapToInt(SportsAuctionPlayer::getQueueOrder).max().orElse(0);

        SportsAuctionPlayer player = SportsAuctionPlayer.builder()
            .config(config)
            .community(config.getCommunity() != null ? config.getCommunity() : (config.getEvent() != null ? config.getEvent().getCommunity() : null))
            .playerName(req.getPlayerName())
            .category(req.getCategory())
            .playerRole(req.getPlayerRole())
            .age(req.getAge())
            .basePrice(req.getBasePrice())
            .queueOrder(maxOrder + 1)
            .status(SportsAuctionPlayer.PlayerStatus.QUEUED)
            .statsJson(finalStats)
            .build();

        return playerRepo.save(player);
    }

    private void notifyAuctionTeamOwners(SportsAuctionConfig config, NotificationType type,
                                           String title, String body, NotificationPriority priority) {
        try {
            List<Long> ownerIds = teamRepo.findByConfigIdOrderByTeamName(config.getId()).stream()
                    .filter(t -> t.getOwnerUser() != null && t.getOwnerUser().getId() != null)
                    .map(t -> t.getOwnerUser().getId())
                    .distinct()
                    .toList();
            if (!ownerIds.isEmpty()) {
                notificationService.createBulkNotifications(
                        ownerIds, type, NotificationCategory.AUCTION,
                        title, body, null,
                        ReferenceType.AUCTION_CONFIG, config.getId(),
                        priority, null, null);
            }
        } catch (Exception e) {
            log.warn("Failed to persist {} notifications for auction {}: {}", type, config.getId(), e.getMessage());
        }
    }

    // ── PICK RANDOM PLAYER FOR LIVE AUCTION ──────────────────────
    @Override
    @Transactional
    public PlayerWithBidResponse pickRandomPlayer(Long configId) {
        // Lock the config row to serialize all pick operations for this auction
        SportsAuctionConfig config = configRepo.findByIdForUpdate(configId)
            .orElseThrow(() -> new IllegalArgumentException("Auction config not found: " + configId));

        // Start the auction automatically if it's currently ACTIVE
        if (config.getStatus() == SportsAuctionConfig.AuctionStatus.ACTIVE) {
            config.setStatus(SportsAuctionConfig.AuctionStatus.LIVE);
            configRepo.save(config);
        }

        // Check if there's already a SELLING player — return them first
        var sellingOpt = playerRepo.findSellingPlayer(configId);
        if (sellingOpt.isPresent()) {
            SportsAuctionPlayer selling = sellingOpt.get();
            Long currentBid = bidRepo.findMaxBidForPlayer(selling.getId())
                .orElse((long) selling.getBasePrice());
            String currentBidTeam = bidRepo.findTopBidsForPlayer(selling.getId(),
                    org.springframework.data.domain.PageRequest.of(0, 1))
                .stream().findFirst()
                .map(b -> b.getTeam().getTeamName())
                .orElse(null);
            int nextIncrement = config.calculateNextIncrement(currentBid);
            return new PlayerWithBidResponse(
                selling.getId(), selling.getPlayerName(), selling.getCategory(),
                selling.getPlayerRole(), selling.getAge(), selling.getBasePrice(),
                selling.getStatsJson(), currentBid, currentBid + nextIncrement,
                nextIncrement, currentBidTeam, selling.getQueueOrder(),
                selling.getStatus().name()
            );
        }

        // Pick a random QUEUED player
        List<SportsAuctionPlayer> queued = playerRepo.findQueuedByConfig(configId);
        if (queued.isEmpty()) {
            throw new AuctionStateException("No more players in queue. Auction pool is empty.");
        }

        java.util.Random random = new java.util.Random();
        int randomIndex = random.nextInt(queued.size());
        SportsAuctionPlayer picked = queued.get(randomIndex);

        // Lock the chosen player row before mutating status
        picked = playerRepo.findByIdForUpdate(picked.getId())
            .orElseThrow(() -> new IllegalStateException("Player disappeared during pick"));
        if (picked.getStatus() != SportsAuctionPlayer.PlayerStatus.QUEUED) {
            throw new AuctionStateException("Player " + picked.getPlayerName() + " is no longer QUEUED");
        }

        picked.setStatus(SportsAuctionPlayer.PlayerStatus.SELLING);
        playerRepo.save(picked);

        log.info("Random player picked for auction: {} (id={})", picked.getPlayerName(), picked.getId());

        PlayerWithBidResponse response = new PlayerWithBidResponse(
            picked.getId(), picked.getPlayerName(), picked.getCategory(),
            picked.getPlayerRole(), picked.getAge(), picked.getBasePrice(),
            picked.getStatsJson(), (long) picked.getBasePrice(),
            config.calculateNextBid(picked.getBasePrice()),
            config.calculateNextIncrement(picked.getBasePrice()),
            null, picked.getQueueOrder(), picked.getStatus().name()
        );

        auctionWs.broadcastPlayerPicked(configId, response);

        return response;
    }
}
