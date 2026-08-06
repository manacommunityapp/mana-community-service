package com.cpn.web;

import com.cpn.application.networking.NetworkingService;
import com.cpn.domain.networking.model.Connection;
import com.cpn.domain.networking.model.ConnectionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cpn/networking")
@RequiredArgsConstructor
public class NetworkingController {

    private final NetworkingService networkingService;

    @PostMapping("/connect")
    public ResponseEntity<Connection> sendRequest(
            @RequestParam UUID requesterId,
            @RequestParam UUID recipientId,
            @RequestParam(required = false) String message) {
        return ResponseEntity.ok(networkingService.sendConnectionRequest(requesterId, recipientId, message));
    }

    @PostMapping("/respond/{connectionId}")
    public ResponseEntity<Connection> respondRequest(
            @PathVariable UUID connectionId,
            @RequestParam ConnectionStatus status) {
        return ResponseEntity.ok(networkingService.respondToRequest(connectionId, status));
    }

    @GetMapping("/pending/{userId}")
    public ResponseEntity<List<Connection>> getPending(@PathVariable UUID userId) {
        return ResponseEntity.ok(networkingService.getPendingRequests(userId));
    }

    @GetMapping("/connections/{userId}")
    public ResponseEntity<List<Connection>> getAccepted(@PathVariable UUID userId) {
        return ResponseEntity.ok(networkingService.getAcceptedConnections(userId));
    }
}
