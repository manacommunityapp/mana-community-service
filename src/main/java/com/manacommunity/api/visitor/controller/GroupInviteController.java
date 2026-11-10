package com.manacommunity.api.visitor.controller;

import com.manacommunity.api.visitor.dto.CreateGroupInviteRequest;
import com.manacommunity.api.visitor.entity.GroupInvite;
import com.manacommunity.api.visitor.service.GroupInviteService;
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
@RequestMapping("/api/visitors/group-invites")
@RequiredArgsConstructor
public class GroupInviteController {

    private final GroupInviteService groupInviteService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<GroupInvite>> getMyInvites(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(groupInviteService.getMyInvites(loggedInUserService.resolve(principal)));
    }

    @PostMapping
    public ResponseEntity<GroupInvite> create(@RequestBody CreateGroupInviteRequest req,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupInviteService.create(req, loggedInUserService.resolve(principal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        groupInviteService.cancel(id, loggedInUserService.resolve(principal));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/guests")
    public ResponseEntity<GroupInvite> addGuest(@PathVariable Long id,
                                                 @RequestBody CreateGroupInviteRequest.GuestDto guest,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupInviteService.addGuest(id, guest, loggedInUserService.resolve(principal)));
    }
}
