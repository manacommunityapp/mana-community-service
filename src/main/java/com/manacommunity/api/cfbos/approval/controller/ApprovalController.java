package com.manacommunity.api.cfbos.approval.controller;

import com.manacommunity.api.cfbos.approval.dto.*;
import com.manacommunity.api.cfbos.approval.service.ApprovalWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cfbos/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalWorkflowService workflowService;

    @PostMapping("/submit")
    public ResponseEntity<ApprovalResponse> submit(@RequestBody SubmitApprovalRequest request) {
        return ResponseEntity.ok(workflowService.submitRequest(request));
    }

    @PostMapping("/action")
    public ResponseEntity<ApprovalResponse> action(@RequestBody ApprovalActionRequest request) {
        return ResponseEntity.ok(workflowService.processAction(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApprovalResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getRequestById(id));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ApprovalResponse>> getPending() {
        return ResponseEntity.ok(workflowService.getPendingRequests());
    }
}
