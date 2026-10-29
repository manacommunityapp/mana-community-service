package com.manacommunity.api.finance.controller;

import com.manacommunity.api.finance.entity.TdsChallan;
import com.manacommunity.api.finance.repository.TdsChallanRepository;
import com.manacommunity.api.finance.repository.SocietyVendorBillRepository;
import com.manacommunity.api.finance.entity.SocietyVendorBill;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/tds")
@RequiredArgsConstructor
public class TdsChallanController {

    private final TdsChallanRepository challanRepo;
    private final SocietyVendorBillRepository vendorBillRepo;

    @GetMapping("/challans")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<TdsChallan>> listChallans(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String quarter) {
        Long communityId = principal.getCommunityId();
        List<TdsChallan> result;
        if (year != null && quarter != null) {
            result = challanRepo.findByCommunityIdAndFinancialYearAndQuarterOrderByTdsSectionAsc(
                    communityId, year, quarter);
        } else if (year != null) {
            result = challanRepo.findByCommunityIdAndFinancialYearOrderByQuarterDesc(communityId, year);
        } else {
            result = challanRepo.findByCommunityIdOrderByFinancialYearDescQuarterDesc(communityId);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/challans")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<TdsChallan> createChallan(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long communityId = principal.getCommunityId();
        TdsChallan challan = TdsChallan.builder()
                .communityId(communityId)
                .financialYear((String) body.get("financialYear"))
                .quarter((String) body.get("quarter"))
                .tdsSection((String) body.get("tdsSection"))
                .totalTdsAmount(new BigDecimal(body.get("totalTdsAmount").toString()))
                .remarks((String) body.get("remarks"))
                .status("PENDING")
                .build();
        return ResponseEntity.ok(challanRepo.save(challan));
    }

    @PutMapping("/challans/{id}/deposit")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<TdsChallan> deposit(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        TdsChallan challan = challanRepo.findById(id)
                .filter(c -> c.getCommunityId().equals(principal.getCommunityId()))
                .orElseThrow(() -> new IllegalArgumentException("Challan not found: " + id));
        challan.setChallanNumber((String) body.get("challanNumber"));
        challan.setBankName((String) body.get("bankName"));
        challan.setDepositedDate(body.get("depositedDate") != null
                ? LocalDate.parse(body.get("depositedDate").toString()) : LocalDate.now());
        challan.setStatus("DEPOSITED");
        return ResponseEntity.ok(challanRepo.save(challan));
    }

    @GetMapping("/liability")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, Object>> getTdsLiability(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getCommunityId();

        // Current financial year logic: April-March
        LocalDate today = LocalDate.now();
        int year = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
        String financialYear = year + "-" + String.valueOf(year + 1).substring(2);

        List<SocietyVendorBill> allBills = vendorBillRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);

        Map<String, BigDecimal> liabilityBySection = new HashMap<>();
        for (SocietyVendorBill bill : allBills) {
            if (bill.getTdsSection() != null && bill.getTdsAmount() != null
                    && bill.getTdsAmount().compareTo(BigDecimal.ZERO) > 0
                    && bill.getCreatedAt() != null) {
                LocalDate billDate = bill.getCreatedAt().toLocalDate();
                int billYear = billDate.getMonthValue() >= 4 ? billDate.getYear() : billDate.getYear() - 1;
                if (billYear == year) {
                    liabilityBySection.merge(bill.getTdsSection(), bill.getTdsAmount(), BigDecimal::add);
                }
            }
        }

        BigDecimal total = liabilityBySection.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new HashMap<>();
        result.put("financialYear", financialYear);
        result.put("liabilityBySection", liabilityBySection);
        result.put("totalOutstanding", total);
        return ResponseEntity.ok(result);
    }
}
