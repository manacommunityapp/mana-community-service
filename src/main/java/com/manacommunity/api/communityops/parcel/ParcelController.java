package com.manacommunity.api.communityops.parcel;

import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parcels")
@RequiredArgsConstructor
public class ParcelController {

    private final ParcelService service;

    @PostMapping("/receive")
    public ResponseEntity<ParcelLog> receive(@RequestBody ParcelReceiveRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(201).body(service.receive(principal.getCommunityId(), principal.getUserId(), req));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ParcelLog>> pending(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getPendingForCommunity(principal.getCommunityId()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ParcelLog>> myParcels(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getMyParcels(principal.getUserId()));
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<ParcelLog> acknowledge(@PathVariable Long id,
            @RequestBody AcknowledgeRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.acknowledge(id, principal.getUserId(), req.otp()));
    }

    record AcknowledgeRequest(String otp) {}
}
