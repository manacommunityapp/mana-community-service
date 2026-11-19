package com.manacommunity.api.healthcare.controller;

import com.manacommunity.api.healthcare.entity.AppointmentNoShow;
import com.manacommunity.api.healthcare.service.NoShowService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/healthcare/no-shows")
@RequiredArgsConstructor
public class NoShowController {

    private final NoShowService noShowService;

    @PostMapping
    public ResponseEntity<AppointmentNoShow> record(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        BigDecimal fee = body.containsKey("feeCharged") ? new BigDecimal(body.get("feeCharged").toString()) : null;
        return ResponseEntity.status(201).body(noShowService.recordNoShow(
            principal.getCommunityId(),
            Long.parseLong(body.get("appointmentId").toString()),
            Long.parseLong(body.get("doctorId").toString()),
            Long.parseLong(body.get("patientUserId").toString()),
            fee
        ));
    }

    @PutMapping("/{id}/waive")
    public ResponseEntity<AppointmentNoShow> waive(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(noShowService.waiveFee(id, principal.getUserId(), body.get("reason")));
    }

    @GetMapping
    public ResponseEntity<List<AppointmentNoShow>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(noShowService.getByCommunity(principal.getCommunityId()));
    }

    @GetMapping("/patient/{patientUserId}/count")
    public ResponseEntity<Map<String, Integer>> count(
            @PathVariable Long patientUserId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of("count", noShowService.countByPatient(principal.getCommunityId(), patientUserId)));
    }
}
