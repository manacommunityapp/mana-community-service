package com.manacommunity.api.cfbos.treasury.controller;

import com.manacommunity.api.cfbos.treasury.dto.FundAccountDto;
import com.manacommunity.api.cfbos.treasury.dto.FundExpenditureRequest;
import com.manacommunity.api.cfbos.treasury.entity.FundContribution;
import com.manacommunity.api.cfbos.treasury.entity.FundExpenditure;
import com.manacommunity.api.cfbos.treasury.service.TreasuryFundService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/funds")
@RequiredArgsConstructor
public class TreasuryFundController {

    private final TreasuryFundService fundService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/accounts")
    public ResponseEntity<List<FundAccountDto>> getAccounts(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 1L;
        return ResponseEntity.ok(fundService.getFundAccounts(communityId));
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasAuthority('Manage Treasury') or hasAuthority('Admin')")
    public ResponseEntity<FundAccountDto> createAccount(
            @Valid @RequestBody FundAccountDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(fundService.createFundAccount(dto, user.getCommunity()));
    }

    @PostMapping("/expenditures")
    @PreAuthorize("hasAuthority('Manage Treasury') or hasAuthority('Admin')")
    public ResponseEntity<FundExpenditure> recordExpenditure(
            @Valid @RequestBody FundExpenditureRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(fundService.recordExpenditure(req, user));
    }
}
