package com.manacommunity.api.homeservices.controller;

import com.manacommunity.api.homeservices.entity.*;
import com.manacommunity.api.homeservices.service.HsQualityService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/home-services/quality")
@RequiredArgsConstructor
public class HsQualityController {

    private final HsQualityService qualityService;

    @PostMapping("/checks")
    public ResponseEntity<HsQualityCheck> check(@RequestBody HsQualityCheck check,
            @AuthenticationPrincipal UserPrincipal p) {
        check.setCheckedBy(p.getUserId());
        return ResponseEntity.ok(qualityService.recordCheck(check));
    }

    @GetMapping("/checks/{bookingId}")
    public ResponseEntity<HsQualityCheck> getCheck(@PathVariable Long bookingId) {
        return ResponseEntity.ok(qualityService.getByBooking(bookingId));
    }

    @PostMapping("/photos")
    public ResponseEntity<HsBookingPhoto> uploadPhoto(@RequestBody HsBookingPhoto photo,
            @AuthenticationPrincipal UserPrincipal p) {
        photo.setUploadedBy(p.getUserId());
        return ResponseEntity.ok(qualityService.uploadPhoto(photo));
    }

    @GetMapping("/photos/{bookingId}")
    public ResponseEntity<List<HsBookingPhoto>> photos(@PathVariable Long bookingId) {
        return ResponseEntity.ok(qualityService.photos(bookingId));
    }

    @PostMapping("/warranty/{bookingId}")
    public ResponseEntity<HsWarranty> issueWarranty(@PathVariable Long bookingId,
            @RequestBody Map<String, Integer> body,
            @AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(qualityService.issueWarranty(bookingId, p.getCommunityId(),
            body.getOrDefault("warrantyDays", 30)));
    }

    @PostMapping("/warranty/{bookingId}/claim")
    public ResponseEntity<HsWarranty> claimWarranty(@PathVariable Long bookingId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(qualityService.claimWarranty(bookingId, body.get("reason")));
    }

    @PostMapping("/rework")
    public ResponseEntity<HsReworkRequest> requestRework(@RequestBody HsReworkRequest req,
            @AuthenticationPrincipal UserPrincipal p) {
        req.setCommunityId(p.getCommunityId());
        req.setUserId(p.getUserId());
        return ResponseEntity.ok(qualityService.requestRework(req));
    }

    @PutMapping("/rework/{id}/approve")
    public ResponseEntity<HsReworkRequest> approveRework(@PathVariable Long id) {
        return ResponseEntity.ok(qualityService.approveRework(id));
    }
}
