// serviceplatform/controller/WorkOrderController.java
package com.manacommunity.api.serviceplatform.controller;

import com.manacommunity.api.serviceplatform.dto.response.WorkOrderResponse;
import com.manacommunity.api.serviceplatform.service.WorkOrderService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/service-platform/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Work Orders')")
    public ResponseEntity<WorkOrderResponse> getWorkOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(workOrderService.getWorkOrder(id, communityId));
    }

    @PatchMapping("/{id}/signoff")
    @PreAuthorize("hasAuthority('View Work Orders')")
    public ResponseEntity<WorkOrderResponse> residentSignoff(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.signoffResident(id, principal.getId()));
    }
}
