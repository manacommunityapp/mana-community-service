package com.manacommunity.api.concurrencydrill.controller;

import com.manacommunity.api.concurrencydrill.service.AtomicGateApprovalService;
import com.manacommunity.api.concurrencydrill.service.AtomicGateApprovalService.ApprovalConflictException;
import com.manacommunity.api.concurrencydrill.service.AtomicGateApprovalService.ApprovalDecisionResult;
import com.manacommunity.api.concurrencydrill.service.KeysetErpExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/concurrency-drill")
@RequiredArgsConstructor
public class ConcurrencyDrillController {

    private final AtomicGateApprovalService approvalService;
    private final KeysetErpExportService erpExportService;

    @PostMapping("/approvals/{approvalId}/decide")
    public ResponseEntity<?> decideApproval(
            @PathVariable Long approvalId,
            @RequestBody Map<String, Object> body) {
        try {
            ApprovalDecisionResult res = approvalService.decideApproval(
                    approvalId,
                    body.get("decision").toString(),
                    Long.parseLong(body.get("actorId").toString()),
                    body.get("idempotencyKey").toString()
            );
            return ResponseEntity.ok(res);
        } catch (ApprovalConflictException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "APPROVAL_CONFLICT", "message", ex.getMessage()));
        }
    }

    @GetMapping(value = "/erp/receipts/export.csv", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> exportReceipts(
            @RequestParam Long communityId,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(defaultValue = "500") int batchSize) {

        StreamingResponseBody body = outputStream -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                erpExportService.streamReceiptsCsv(communityId, from, to, writer, batchSize);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"erp-receipts-" + communityId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }
}
