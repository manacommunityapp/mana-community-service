package com.manacommunity.api.cfbos.statements.controller;

import com.manacommunity.api.cfbos.statements.dto.CashflowForecastDto;
import com.manacommunity.api.cfbos.statements.service.AiCashflowForecastingService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/analytics")
@RequiredArgsConstructor
public class AiFinancialAnalyticsController {

    private final AiCashflowForecastingService forecastingService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/cashflow-forecast")
    @PreAuthorize("hasAuthority('View Financial Statements') or hasAuthority('Admin')")
    public ResponseEntity<CashflowForecastDto> getForecast(
            @RequestParam(defaultValue = "90") int horizonDays,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 1L;
        return ResponseEntity.ok(forecastingService.forecastCashflow(communityId, horizonDays));
    }
}
