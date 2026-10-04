package com.manacommunity.api.cfbos.statements.controller;

import com.manacommunity.api.cfbos.statements.dto.BalanceSheetDto;
import com.manacommunity.api.cfbos.statements.dto.CashFlowStatementDto;
import com.manacommunity.api.cfbos.statements.dto.ProfitAndLossDto;
import com.manacommunity.api.cfbos.statements.service.FinancialStatementsService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/finance/statements")
@RequiredArgsConstructor
public class FinancialStatementsController {

    private final FinancialStatementsService statementsService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/balance-sheet")
    @PreAuthorize("hasAuthority('View Financial Statements') or hasAuthority('Admin')")
    public ResponseEntity<BalanceSheetDto> getBalanceSheet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 1L;
        return ResponseEntity.ok(statementsService.generateBalanceSheet(communityId, asOfDate));
    }

    @GetMapping("/profit-and-loss")
    @PreAuthorize("hasAuthority('View Financial Statements') or hasAuthority('Admin')")
    public ResponseEntity<ProfitAndLossDto> getProfitAndLoss(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 1L;
        return ResponseEntity.ok(statementsService.generateProfitAndLoss(communityId, startDate, endDate));
    }

    @GetMapping("/cash-flow")
    @PreAuthorize("hasAuthority('View Financial Statements') or hasAuthority('Admin')")
    public ResponseEntity<CashFlowStatementDto> getCashFlow(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 1L;
        return ResponseEntity.ok(statementsService.generateCashFlow(communityId, startDate, endDate));
    }
}
