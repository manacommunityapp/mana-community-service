package com.cpos.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/cpos/finance")
@RequiredArgsConstructor
@CrossOrigin
public class FinanceController {

    @GetMapping("/portfolio-summary")
    public ResponseEntity<Map<String, Object>> getPortfolioFinanceSummary(@RequestParam UUID tenantId) {
        Map<String, Object> fin = new HashMap<>();
        fin.put("tenantId", tenantId);
        fin.put("totalPortfolioValue", 28400000000L); // 2840 Cr
        fin.put("monthlyRentalIncome", 19500000);     // 1.95 Cr
        fin.put("outstandingLoans", 6800000000L);     // 680 Cr
        fin.put("securityDepositsHeld", 52000000);     // 5.2 Cr
        fin.put("collectionRatePct", 96.2);
        return ResponseEntity.ok(fin);
    }
}
