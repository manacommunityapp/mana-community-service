package com.manacommunity.api.sports.controller;

import com.manacommunity.api.sports.model.SportsTournament;
import com.manacommunity.api.sports.model.SportsTournamentPlayerPayment;
import com.manacommunity.api.sports.repository.SportsTournamentPlayerPaymentRepository;
import com.manacommunity.api.sports.repository.SportsTournamentRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.repository.FamilyMemberRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/sports/tournaments")
@RequiredArgsConstructor
public class SportsTournamentPaymentController {

    private final SportsTournamentPlayerPaymentRepository paymentRepo;
    private final SportsTournamentRepository tournamentRepo;
    private final AppUserRepository userRepo;
    private final FamilyMemberRepository familyMemberRepo;

    @GetMapping("/{tournamentId}/payments")
    public ResponseEntity<List<SportsTournamentPlayerPaymentResponse>> getPayments(@PathVariable Long tournamentId) {
        List<SportsTournamentPlayerPayment> payments = paymentRepo.findByTournamentId(tournamentId);
        List<SportsTournamentPlayerPaymentResponse> list = payments.stream().map(this::toResponse).toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{tournamentId}/payments/mark-paid")
    public ResponseEntity<SportsTournamentPlayerPaymentResponse> markPaid(
            @PathVariable Long tournamentId,
            @RequestBody SportsTournamentPlayerPaymentRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        SportsTournament tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found: " + tournamentId));

        SportsTournamentPlayerPayment payment = paymentRepo.findByTournamentIdAndPlayerKey(tournamentId, req.getPlayerKey())
                .orElseGet(() -> SportsTournamentPlayerPayment.builder()
                        .tournament(tournament)
                        .playerKey(req.getPlayerKey())
                        .build());

        payment.setPlayerName(req.getPlayerName());
        payment.setFlatNumber(req.getFlatNumber());
        payment.setPhone(req.getPhone());
        payment.setEmail(req.getEmail());
        payment.setAmount(req.getAmount() != null ? req.getAmount() : BigDecimal.ZERO);
        payment.setPaymentStatus(parsePaymentStatus(req.getPaymentStatus()));
        payment.setPaymentMode(parsePaymentMode(req.getPaymentMode()));
        payment.setTransactionRef(req.getTransactionRef());
        payment.setNotes(req.getNotes());
        payment.setPaidAt(LocalDateTime.now());

        if (req.getUserId() != null) {
            userRepo.findById(req.getUserId()).ifPresent(payment::setUser);
        }
        if (req.getFamilyMemberId() != null) {
            familyMemberRepo.findById(req.getFamilyMemberId()).ifPresent(payment::setFamilyMember);
        }

        // Audit: record who clicked Mark as Paid
        if (principal != null && principal.getId() != null) {
            userRepo.findById(principal.getId()).ifPresent(admin -> {
                payment.setMarkedByUser(admin);
                payment.setMarkedByUserName(admin.getFullName() != null && !admin.getFullName().isBlank() ? admin.getFullName() : admin.getUsername());
            });
        }
        if (payment.getMarkedByUserName() == null && req.getMarkedByUserName() != null && !req.getMarkedByUserName().isBlank()) {
            payment.setMarkedByUserName(req.getMarkedByUserName());
        }

        SportsTournamentPlayerPayment saved = paymentRepo.save(payment);
        log.info("Recorded payment for tournamentId={} playerKey={} markedBy={}", tournamentId, saved.getPlayerKey(), saved.getMarkedByUserName());
        return ResponseEntity.ok(toResponse(saved));
    }

    private SportsTournamentPlayerPayment.PaymentStatus parsePaymentStatus(String s) {
        if (s == null) return SportsTournamentPlayerPayment.PaymentStatus.PAID;
        try {
            return SportsTournamentPlayerPayment.PaymentStatus.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            return SportsTournamentPlayerPayment.PaymentStatus.PAID;
        }
    }

    private SportsTournamentPlayerPayment.PaymentMode parsePaymentMode(String s) {
        if (s == null) return SportsTournamentPlayerPayment.PaymentMode.UPI;
        try {
            return SportsTournamentPlayerPayment.PaymentMode.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            return SportsTournamentPlayerPayment.PaymentMode.UPI;
        }
    }

    private SportsTournamentPlayerPaymentResponse toResponse(SportsTournamentPlayerPayment p) {
        return SportsTournamentPlayerPaymentResponse.builder()
                .id(p.getId())
                .tournamentId(p.getTournament() != null ? p.getTournament().getId() : null)
                .playerKey(p.getPlayerKey())
                .userId(p.getUser() != null ? p.getUser().getId() : null)
                .familyMemberId(p.getFamilyMember() != null ? p.getFamilyMember().getId() : null)
                .playerName(p.getPlayerName())
                .flatNumber(p.getFlatNumber())
                .phone(p.getPhone())
                .email(p.getEmail())
                .paymentStatus(p.getPaymentStatus() != null ? p.getPaymentStatus().name() : "PAID")
                .amount(p.getAmount())
                .paymentMode(p.getPaymentMode() != null ? p.getPaymentMode().name() : "UPI")
                .transactionRef(p.getTransactionRef())
                .notes(p.getNotes())
                .markedByUserId(p.getMarkedByUser() != null ? p.getMarkedByUser().getId() : null)
                .markedByUserName(p.getMarkedByUserName())
                .paidAt(p.getPaidAt() != null ? p.getPaidAt().toString() : null)
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null)
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SportsTournamentPlayerPaymentRequest {
        private String playerKey;
        private Long userId;
        private Long familyMemberId;
        private String playerName;
        private String flatNumber;
        private String phone;
        private String email;
        private BigDecimal amount;
        private String paymentStatus;
        private String paymentMode;
        private String transactionRef;
        private String notes;
        private String markedByUserName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SportsTournamentPlayerPaymentResponse {
        private Long id;
        private Long tournamentId;
        private String playerKey;
        private Long userId;
        private Long familyMemberId;
        private String playerName;
        private String flatNumber;
        private String phone;
        private String email;
        private String paymentStatus;
        private BigDecimal amount;
        private String paymentMode;
        private String transactionRef;
        private String notes;
        private Long markedByUserId;
        private String markedByUserName;
        private String paidAt;
        private String createdAt;
    }
}
