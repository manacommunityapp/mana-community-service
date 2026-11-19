package com.manacommunity.api.healthcare.controller;

import com.manacommunity.api.healthcare.entity.Referral;
import com.manacommunity.api.healthcare.service.ReferralService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/healthcare/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @PostMapping
    public ResponseEntity<Referral> create(
            @RequestBody Referral referral,
            @AuthenticationPrincipal UserPrincipal principal) {
        referral.setCommunityId(principal.getCommunityId());
        return ResponseEntity.status(201).body(referralService.create(referral));
    }

    @PutMapping("/{id}/appointment-booked")
    public ResponseEntity<Referral> bookAppointment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        LocalDate date = body.containsKey("appointmentDate") ? LocalDate.parse(body.get("appointmentDate")) : null;
        return ResponseEntity.ok(referralService.markAppointmentBooked(id, date));
    }

    @PutMapping("/{id}/outcome")
    public ResponseEntity<Referral> updateOutcome(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(referralService.updateOutcome(id,
            body.get("outcomeNotes"),
            body.get("feedbackToReferrer"),
            body.getOrDefault("status", "COMPLETED")
        ));
    }

    @GetMapping
    public ResponseEntity<List<Referral>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(referralService.getByCommunity(principal.getCommunityId()));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Referral>> pending(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(referralService.getPendingReferrals(principal.getCommunityId()));
    }

    @GetMapping("/patient/{patientUserId}")
    public ResponseEntity<List<Referral>> forPatient(
            @PathVariable Long patientUserId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(referralService.getByPatient(principal.getCommunityId(), patientUserId));
    }
}
