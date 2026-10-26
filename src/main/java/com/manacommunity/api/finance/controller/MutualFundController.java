package com.manacommunity.api.finance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/mf")
public class MutualFundController {

    record FundDto(String id, String name, String short_name, String category, String amc,
                   double nav, double nav_change_pct, double returns_1y, double returns_3y,
                   double returns_5y, String risk, int min_sip, int min_lumpsum, int rating) {}
    record HoldingDto(String fund_id, String fund_name, double units, double avg_nav,
                      double current_nav, double invested_amount, double current_value, double pnl, double pnl_pct) {}
    record SipDto(String id, String fund_id, String fund_name, double amount, int sip_date,
                  String status, String next_installment, double total_invested, double current_value) {}
    record InvestRequest(String fund_id, double amount, String type, Integer sip_date) {}
    record InvestResult(String upi_intent_url, String phone_pe_url, String transaction_ref, double units, double amount) {}

    private static final List<FundDto> CATALOGUE = List.of(
        new FundDto("HDFC_ELSS",    "HDFC Tax Saver Fund – Direct Growth",              "HDFC Tax Saver",   "ELSS",      "HDFC AMC",  1024.50, 0.82, 22.4, 18.6, 16.2, "HIGH",     500, 500,   5),
        new FundDto("AXIS_BLUECHIP","Axis Bluechip Fund – Direct Growth",               "Axis Bluechip",    "LARGE_CAP", "Axis AMC",    63.18, 0.54, 18.2, 15.8, 14.1, "MODERATE", 500, 1000,  4),
        new FundDto("MIRAE_MIDCAP", "Mirae Asset Midcap Fund – Direct Growth",          "Mirae Midcap",     "MID_CAP",   "Mirae AMC",   38.47, 1.12, 28.6, 22.1, 19.4, "HIGH",    1000, 5000,  5),
        new FundDto("NIFTY50_INDEX","UTI Nifty 50 Index Fund – Direct Growth",          "UTI Nifty 50",     "INDEX",     "UTI AMC",    148.22, 0.46, 17.8, 15.2, 13.8, "MODERATE", 500, 1000,  4),
        new FundDto("LIQUID_HDFC",  "HDFC Liquid Fund – Direct Growth",                 "HDFC Liquid",      "LIQUID",    "HDFC AMC",  4921.80, 0.02,  7.2,  6.8,  6.4, "LOW",     1000, 5000,  4),
        new FundDto("ICICI_HYBRID", "ICICI Pru Balanced Advantage – Direct Growth",     "ICICI Balanced",   "HYBRID",    "ICICI AMC",   58.94, 0.38, 16.4, 14.2, 12.6, "MODERATE",1000, 5000,  4)
    );

    @GetMapping("/funds")
    public ResponseEntity<List<FundDto>> getFunds(@RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(CATALOGUE.stream().filter(f -> f.category().equals(category)).toList());
        }
        return ResponseEntity.ok(CATALOGUE);
    }

    @GetMapping("/holdings")
    public ResponseEntity<List<HoldingDto>> getHoldings() {
        FundDto fund = CATALOGUE.stream().filter(f -> f.id().equals("AXIS_BLUECHIP")).findFirst().orElseThrow();
        double units = 47.82, avgNav = 58.20;
        double invested = units * avgNav;
        double current = units * fund.nav();
        double pnl = current - invested;
        return ResponseEntity.ok(List.of(
            new HoldingDto(fund.id(), fund.name(), units, avgNav, fund.nav(), invested, current, pnl, pnl / invested * 100)
        ));
    }

    @GetMapping("/sips")
    public ResponseEntity<List<SipDto>> getSips() {
        LocalDate next = LocalDate.now().withDayOfMonth(5);
        if (next.isBefore(LocalDate.now())) next = next.plusMonths(1);
        return ResponseEntity.ok(List.of(
            new SipDto("sip-1", "AXIS_BLUECHIP", "Axis Bluechip Fund", 2000, 5, "ACTIVE",
                next.toString(), 24000, 28400)
        ));
    }

    @PostMapping("/invest")
    public ResponseEntity<InvestResult> invest(@RequestBody InvestRequest req) {
        FundDto fund = CATALOGUE.stream().filter(f -> f.id().equals(req.fund_id())).findFirst()
            .orElse(CATALOGUE.get(0));
        double units = Math.round((req.amount() / fund.nav()) * 1000.0) / 1000.0;
        String tr = "MANA-MF-" + System.currentTimeMillis();
        return ResponseEntity.ok(new InvestResult(
            String.format("upi://pay?pa=mf@setu&pn=Mana+MF+%s&am=%.2f&cu=INR&tr=%s&tn=Mutual+Fund+%s", req.type(), req.amount(), tr, req.type()),
            String.format("phonepe://pay?pa=mf@setu&pn=Mana+MF&am=%.2f&cu=INR&tr=%s", req.amount(), tr),
            tr, units, req.amount()
        ));
    }

    @PostMapping("/sips/{sipId}/pause")
    public ResponseEntity<Void> pauseSip(@PathVariable String sipId) { return ResponseEntity.ok().build(); }

    @DeleteMapping("/sips/{sipId}/cancel")
    public ResponseEntity<Void> cancelSip(@PathVariable String sipId) { return ResponseEntity.noContent().build(); }
}
