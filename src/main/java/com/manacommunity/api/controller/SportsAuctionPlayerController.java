package com.manacommunity.api.controller;

import com.manacommunity.api.dto.SportsAuctionPlayerRequest;
import com.manacommunity.api.dto.SportsAuctionPlayerResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.SportsAuctionPlayer;
import com.manacommunity.api.model.SportsAuctionConfig;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.service.SportsAuctionPlayerService;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auction/players")
@RequiredArgsConstructor
public class SportsAuctionPlayerController {

    private final SportsAuctionPlayerService auctionPlayerService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AUCTION_ADMIN','SPORTS_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<SportsAuctionPlayerResponse> createPlayer(
            @Valid @RequestBody SportsAuctionPlayerRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        SportsAuctionPlayer player = SportsAuctionPlayer.builder()
                .config(SportsAuctionConfig.builder().id(req.getConfigId()).build())
                .user(AppUser.builder().id(req.getUserId()).build())
                .playerName(req.getPlayerName())
                .category(req.getCategory())
                .playerRole(req.getPlayerRole())
                .age(req.getAge())
                .basePrice(req.getBasePrice())
                .status(SportsAuctionPlayer.PlayerStatus.QUEUED)
                .build();
        SportsAuctionPlayer saved = auctionPlayerService.savePlayer(player);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    static SportsAuctionPlayerResponse toResponse(SportsAuctionPlayer p) {
        return SportsAuctionPlayerResponse.builder()
                .id(p.getId())
                .configId(p.getConfig() != null ? p.getConfig().getId() : null)
                .userId(p.getUser() != null ? p.getUser().getId() : null)
                .playerName(p.getPlayerName())
                .category(p.getCategory())
                .playerRole(p.getPlayerRole())
                .age(p.getAge())
                .basePrice(p.getBasePrice())
                .statsJson(p.getStatsJson())
                .queueOrder(p.getQueueOrder())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .assignedTeamId(p.getAssignedTeam() != null ? p.getAssignedTeam().getId() : null)
                .assignedTeamName(p.getAssignedTeam() != null ? p.getAssignedTeam().getTeamName() : null)
                .soldPrice(p.getSoldPrice())
                .rtmUsed(p.getRtmUsed())
                .soldAt(p.getSoldAt())
                .build();
    }
}
