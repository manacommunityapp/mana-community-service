package com.manacommunity.api.broadcast.controller;

import com.manacommunity.api.broadcast.dto.SendBroadcastRequest;
import com.manacommunity.api.broadcast.entity.Broadcast;
import com.manacommunity.api.broadcast.service.BroadcastService;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/broadcasts")
@RequiredArgsConstructor
public class BroadcastController {

    private final BroadcastService broadcastService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<Broadcast>> getBroadcasts(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(broadcastService.getBroadcasts(loggedInUserService.resolve(principal)));
    }

    @PostMapping
    public ResponseEntity<Broadcast> send(@RequestBody SendBroadcastRequest req,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(broadcastService.send(req, loggedInUserService.resolve(principal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        broadcastService.delete(id, loggedInUserService.resolve(principal));
        return ResponseEntity.noContent().build();
    }
}
