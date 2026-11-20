package com.manacommunity.api.grouporder.controller;

import com.manacommunity.api.grouporder.dto.*;
import com.manacommunity.api.grouporder.entity.*;
import com.manacommunity.api.grouporder.service.GroupOrderService;
import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/group-orders")
@RequiredArgsConstructor
public class GroupOrderController {

    private final GroupOrderService groupOrderService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<GroupOrder>>> listOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        AppUser user = loggedInUserService.resolve(principal);
        Page<GroupOrder> page = groupOrderService.getOpenOrders(
                user.getCommunity().getId(), category, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GroupOrder>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateGroupOrderRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        GroupOrder created = groupOrderService.createGroupOrder(
                user.getCommunity().getId(), user.getId(), req);
        return ResponseEntity.ok(ApiResponse.ok(created, "Group order created"));
    }

    @GetMapping("/my-joined")
    public ResponseEntity<ApiResponse<List<GroupOrderContribution>>> myJoined(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ApiResponse.ok(groupOrderService.getMyJoined(user.getId())));
    }

    @GetMapping("/my-organised")
    public ResponseEntity<ApiResponse<List<GroupOrder>>> myOrganised(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ApiResponse.ok(groupOrderService.getMyOrganised(user.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupOrder>> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(groupOrderService.getOrder(id)));
    }

    @GetMapping("/{id}/updates")
    public ResponseEntity<ApiResponse<List<GroupOrderUpdate>>> getUpdates(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(groupOrderService.getUpdates(id)));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<GroupOrderContribution>> join(
            @PathVariable Long id,
            @Valid @RequestBody JoinGroupOrderRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        GroupOrderContribution contribution = groupOrderService.joinGroupOrder(id, user.getId(), req.getQuantity());
        return ResponseEntity.ok(ApiResponse.ok(contribution, "Joined group order"));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<Void>> leave(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        groupOrderService.leaveGroupOrder(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Left group order"));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<GroupOrder>> close(
            @PathVariable Long id,
            @Valid @RequestBody CloseGroupOrderRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        GroupOrder order = groupOrderService.closeAndOrder(id, user.getId(), req);
        return ResponseEntity.ok(ApiResponse.ok(order, "Order placed with vendor"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelGroupOrderRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        groupOrderService.cancel(id, user.getId(), req.getReason());
        return ResponseEntity.ok(ApiResponse.ok(null, "Order cancelled"));
    }

    @PostMapping("/{id}/updates")
    public ResponseEntity<ApiResponse<GroupOrderUpdate>> postUpdate(
            @PathVariable Long id,
            @Valid @RequestBody PostUpdateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        GroupOrderUpdate update = groupOrderService.postUpdate(id, user.getId(), req);
        return ResponseEntity.ok(ApiResponse.ok(update, "Update posted"));
    }

    @PostMapping("/{id}/confirm-collection")
    public ResponseEntity<ApiResponse<Void>> confirmCollection(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        groupOrderService.confirmCollection(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Collection confirmed"));
    }
}
