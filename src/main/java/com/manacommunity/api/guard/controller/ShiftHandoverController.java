package com.manacommunity.api.guard.controller;

import com.manacommunity.api.guard.dto.CreateHandoverRequest;
import com.manacommunity.api.guard.entity.ShiftHandover;
import com.manacommunity.api.guard.service.ShiftHandoverService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/guard/handovers")
@RequiredArgsConstructor
public class ShiftHandoverController {

    private final ShiftHandoverService service;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/today")
    public ResponseEntity<List<ShiftHandover>> today(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getTodayHandovers(loggedInUserService.resolve(principal)));
    }

    @GetMapping
    public ResponseEntity<List<ShiftHandover>> history(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getHistory(loggedInUserService.resolve(principal)));
    }

    @PostMapping
    public ResponseEntity<ShiftHandover> create(@RequestBody CreateHandoverRequest req,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, user));
    }

    @PatchMapping("/{id}/acknowledge")
    public ResponseEntity<ShiftHandover> acknowledge(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.acknowledge(id, user));
    }
}
