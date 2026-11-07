package com.manacommunity.api.socialcircle.controller;

import com.manacommunity.api.socialcircle.dto.KittyPartyHousieDtos.*;
import com.manacommunity.api.socialcircle.service.KittyPartyHousieEngine;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/kitty-games")
@RequiredArgsConstructor
public class KittyPartyHousieController {

    private final KittyPartyHousieEngine housieEngine;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/circles/{circleId}/rooms")
    public ResponseEntity<GameRoomResponse> createRoom(
            @PathVariable Long circleId,
            @Valid @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        GameRoomResponse response = housieEngine.createRoom(circleId, user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<GameRoomResponse> getRoomDetails(@PathVariable Long roomId) {
        return ResponseEntity.ok(housieEngine.getRoomDetails(roomId));
    }

    @PostMapping("/rooms/{roomId}/join")
    public ResponseEntity<Map<String, Object>> joinRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        housieEngine.joinRoom(roomId, user);
        return ResponseEntity.ok(Map.of("success", true, "message", "Joined game room"));
    }

    @PostMapping("/rooms/{roomId}/tickets")
    public ResponseEntity<TicketResponse> generateTicket(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        TicketResponse ticket = housieEngine.generateTicketForPlayer(roomId, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    @GetMapping("/rooms/{roomId}/my-tickets")
    public ResponseEntity<List<TicketResponse>> getMyTickets(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(housieEngine.getPlayerTickets(roomId, user.getId()));
    }

    @PostMapping("/rooms/{roomId}/start")
    public ResponseEntity<Map<String, Object>> startGame(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        housieEngine.startGame(roomId, user);
        return ResponseEntity.ok(Map.of("success", true, "message", "Game started"));
    }

    @PostMapping("/rooms/{roomId}/call-number")
    public ResponseEntity<CallNumberResponse> callNextNumber(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(housieEngine.callNextNumber(roomId, user));
    }

    @PostMapping("/rooms/{roomId}/claim")
    public ResponseEntity<WinnerClaimResponse> claimPrize(
            @PathVariable Long roomId,
            @Valid @RequestBody ClaimPrizeRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(housieEngine.claimPrize(roomId, user, request));
    }
}
