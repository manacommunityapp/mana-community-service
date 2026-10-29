package com.manacommunity.api.finance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/split")
public class SplitBillController {

    record SplitMemberDto(String user_id, String name, String flat, double share_amount,
                          String status, String paid_at) {}
    record SplitBillDto(String id, String title, String description, double total_amount,
                        String split_type, String created_by, String created_by_name,
                        List<SplitMemberDto> members, double settled_amount, double pending_amount,
                        String status, String created_at, String category) {}
    record CreateMemberInput(String user_id, String name, String flat, double share_amount) {}
    record CreateRequest(String title, String description, double total_amount, String split_type,
                         List<CreateMemberInput> members, String category) {}
    record SettleRequest(String member_id, double amount) {}
    record SettleResult(String upi_intent_url, String phone_pe_url, String transaction_ref, double amount) {}

    @GetMapping("/my-splits")
    public ResponseEntity<List<SplitBillDto>> getMySplits() {
        long now = System.currentTimeMillis();
        return ResponseEntity.ok(List.of(
            new SplitBillDto("split-1", "Annual Day Event — Decoration",
                "Community hall decoration for Annual Day 2025",
                12000, "EQUAL", "u-001", "Rajesh Kumar (A-201)",
                List.of(
                    new SplitMemberDto("u-001", "Rajesh Kumar", "A-201", 4000, "PAID",   Instant.ofEpochMilli(now - 86400000L).toString()),
                    new SplitMemberDto("u-002", "Priya Sharma",  "A-202", 4000, "PENDING", null),
                    new SplitMemberDto("u-003", "Amit Verma",    "B-101", 4000, "PENDING", null)
                ),
                4000, 8000, "PARTIALLY_SETTLED",
                Instant.ofEpochMilli(now - 3 * 86400000L).toString(), "EVENT"),
            new SplitBillDto("split-2", "Generator Diesel — October", null,
                8500, "EQUAL", "u-004", "Society Treasurer",
                List.of(
                    new SplitMemberDto("u-002", "Priya Sharma", "A-202", 2833, "PENDING", null),
                    new SplitMemberDto("u-003", "Amit Verma",   "B-101", 2833, "PAID",    Instant.ofEpochMilli(now - 86400000L).toString()),
                    new SplitMemberDto("u-001", "Rajesh Kumar", "A-201", 2834, "PENDING", null)
                ),
                2833, 5667, "PARTIALLY_SETTLED",
                Instant.ofEpochMilli(now - 7 * 86400000L).toString(), "UTILITY")
        ));
    }

    @PostMapping("/create")
    public ResponseEntity<SplitBillDto> createSplit(@RequestBody CreateRequest req) {
        String id = "split-" + System.currentTimeMillis();
        List<SplitMemberDto> members = req.members().stream()
            .map(m -> new SplitMemberDto(m.user_id(), m.name(), m.flat(), m.share_amount(), "PENDING", null))
            .toList();
        double settled = 0;
        return ResponseEntity.ok(new SplitBillDto(id, req.title(), req.description(),
            req.total_amount(), req.split_type(), "me", "You", members,
            settled, req.total_amount() - settled, "OPEN",
            Instant.now().toString(), req.category()));
    }

    @PostMapping("/{splitId}/settle")
    public ResponseEntity<SettleResult> settle(@PathVariable String splitId, @RequestBody SettleRequest req) {
        String tr = "MANA-SPLIT-" + System.currentTimeMillis();
        return ResponseEntity.ok(new SettleResult(
            String.format("upi://pay?pa=split@setu&pn=Mana+Split&am=%.2f&cu=INR&tr=%s&tn=Split+Bill+Settlement", req.amount(), tr),
            String.format("phonepe://pay?pa=split@setu&pn=Mana+Split&am=%.2f&cu=INR&tr=%s", req.amount(), tr),
            tr, req.amount()
        ));
    }
}
