package com.manacommunity.api.cfbos.treasury.controller;

import com.manacommunity.api.cfbos.treasury.dto.BankStatementUploadResponse;
import com.manacommunity.api.cfbos.treasury.entity.BankStatementTransaction;
import com.manacommunity.api.cfbos.treasury.service.BankStatementReconciliationService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/finance/treasury/reconciliation")
@RequiredArgsConstructor
public class BankReconciliationController {

    private final BankStatementReconciliationService reconciliationService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('Manage Treasury') or hasAuthority('Admin')")
    public ResponseEntity<BankStatementUploadResponse> uploadStatement(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) throws Exception {
        AppUser user = loggedInUserService.resolve(principal);
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "statement.txt";
        return ResponseEntity.ok(reconciliationService.processStatement(content, filename, user.getCommunity()));
    }

    @GetMapping("/unmatched")
    @PreAuthorize("hasAuthority('Manage Treasury') or hasAuthority('Admin')")
    public ResponseEntity<List<BankStatementTransaction>> getUnmatched(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 1L;
        return ResponseEntity.ok(reconciliationService.getUnmatchedTransactions(communityId));
    }
}
