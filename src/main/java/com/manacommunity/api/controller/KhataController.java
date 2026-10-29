package com.manacommunity.api.controller;

import com.manacommunity.api.model.KhataEntry;
import com.manacommunity.api.model.KhataEntry.TransactionKind;
import com.manacommunity.api.model.KhataParty;
import com.manacommunity.api.model.KhataParty.KhataType;
import com.manacommunity.api.model.KhataParty.PartyType;
import com.manacommunity.api.repository.KhataEntryRepository;
import com.manacommunity.api.repository.KhataPartyRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/khata")
@RequiredArgsConstructor
public class KhataController {

    private final KhataPartyRepository partyRepo;
    private final KhataEntryRepository entryRepo;
    private final LoggedInUserService loggedInUserService;

    // ── Vendor Khata ──────────────────────────────────────────────────────

    @GetMapping("/vendor/parties")
    public ResponseEntity<List<KhataParty>> getVendorParties(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(partyRepo.findByUserIdAndKhataTypeOrderByLastActivityDesc(
                user.getId(), KhataType.VENDOR));
    }

    @PostMapping("/vendor/parties")
    public ResponseEntity<KhataParty> addVendorParty(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody AddPartyRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 0L;
        KhataParty party = KhataParty.builder()
                .user(user)
                .communityId(communityId)
                .name(req.name())
                .phone(req.phone())
                .khataType(KhataType.VENDOR)
                .partyType(req.partyType() != null ? PartyType.valueOf(req.partyType()) : PartyType.CUSTOMER)
                .notes(req.notes())
                .build();
        return ResponseEntity.ok(partyRepo.save(party));
    }

    @GetMapping("/vendor/stats")
    public ResponseEntity<Map<String, Object>> getVendorStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(buildStats(user.getId(), KhataType.VENDOR));
    }

    // ── Personal Khata ────────────────────────────────────────────────────

    @GetMapping("/personal/parties")
    public ResponseEntity<List<KhataParty>> getPersonalParties(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(partyRepo.findByUserIdAndKhataTypeOrderByLastActivityDesc(
                user.getId(), KhataType.PERSONAL));
    }

    @PostMapping("/personal/parties")
    public ResponseEntity<KhataParty> addPersonalParty(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody AddPartyRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 0L;
        KhataParty party = KhataParty.builder()
                .user(user)
                .communityId(communityId)
                .name(req.name())
                .phone(req.phone())
                .khataType(KhataType.PERSONAL)
                .partyType(req.partyType() != null ? PartyType.valueOf(req.partyType()) : PartyType.FRIEND)
                .notes(req.notes())
                .build();
        return ResponseEntity.ok(partyRepo.save(party));
    }

    @GetMapping("/personal/stats")
    public ResponseEntity<Map<String, Object>> getPersonalStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(buildStats(user.getId(), KhataType.PERSONAL));
    }

    // ── Entries (shared) ──────────────────────────────────────────────────

    @GetMapping("/parties/{partyId}/entries")
    public ResponseEntity<List<KhataEntry>> getEntries(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long partyId) {
        AppUser user = loggedInUserService.resolve(principal);
        partyRepo.findByIdAndUserId(partyId, user.getId())
                .orElseThrow(() -> new RuntimeException("Party not found"));
        return ResponseEntity.ok(entryRepo.findByPartyIdOrderByEntryDateDescCreatedAtDesc(partyId));
    }

    @PostMapping("/parties/{partyId}/entries")
    public ResponseEntity<KhataEntry> addEntry(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long partyId,
            @RequestBody AddEntryRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        KhataParty party = partyRepo.findByIdAndUserId(partyId, user.getId())
                .orElseThrow(() -> new RuntimeException("Party not found"));

        KhataEntry entry = KhataEntry.builder()
                .party(party)
                .kind(TransactionKind.valueOf(req.kind()))
                .amount(BigDecimal.valueOf(req.amount()))
                .note(req.note())
                .entryDate(req.date() != null ? LocalDate.parse(req.date()) : LocalDate.now())
                .build();
        entryRepo.save(entry);

        // Update party balance and totals
        BigDecimal amt = entry.getAmount();
        if (entry.getKind() == TransactionKind.GAVE) {
            party.setBalance(party.getBalance().add(amt));
            party.setTotalGave(party.getTotalGave().add(amt));
        } else {
            party.setBalance(party.getBalance().subtract(amt));
            party.setTotalGot(party.getTotalGot().add(amt));
        }
        party.setLastActivity(LocalDateTime.now());
        partyRepo.save(party);

        return ResponseEntity.ok(entry);
    }

    @PostMapping("/parties/{partyId}/remind")
    public ResponseEntity<Map<String, Object>> sendReminder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long partyId) {
        AppUser user = loggedInUserService.resolve(principal);
        KhataParty party = partyRepo.findByIdAndUserId(partyId, user.getId())
                .orElseThrow(() -> new RuntimeException("Party not found"));
        return ResponseEntity.ok(Map.of(
                "success", true,
                "phone", party.getPhone() != null ? party.getPhone() : "",
                "name", party.getName(),
                "balance", party.getBalance()
        ));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Map<String, Object> buildStats(Long userId, KhataType type) {
        BigDecimal receivable = partyRepo.sumReceivable(userId, type);
        BigDecimal payable = partyRepo.sumPayable(userId, type);
        long parties = partyRepo.countWithBalance(userId, type);
        return Map.of(
                "totalReceivable", receivable,
                "totalPayable", payable,
                "netBalance", receivable.subtract(payable),
                "partiesWithBalance", parties
        );
    }

    // ── Request records ───────────────────────────────────────────────────

    record AddPartyRequest(String name, String phone, String partyType, String notes) {}

    record AddEntryRequest(String kind, double amount, String note, String date) {}
}
