package com.manacommunity.api.cfbos.charge.controller;

import com.manacommunity.api.cfbos.charge.dto.*;
import com.manacommunity.api.cfbos.charge.engine.ChargeCalculationEngine;
import com.manacommunity.api.cfbos.charge.service.FormulaService;
import com.manacommunity.api.cfbos.charge.service.SlabConfigService;
import com.manacommunity.api.constants.permissions.AdminPermissions;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.service.PermissionCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cfbos/v1/charges")
@RequiredArgsConstructor
public class ChargeController {

    private final ChargeCalculationEngine chargeCalculationEngine;
    private final FormulaService formulaService;
    private final SlabConfigService slabConfigService;
    private final PermissionCheckService permissionCheckService;

    @PostMapping("/calculate")
    public ResponseEntity<ChargeCalculationResult> calculate(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ChargeCalculationRequest request) {
        permissionCheckService.requireAnyPermission(principal, AdminPermissions.VIEW_ADMIN);
        return ResponseEntity.ok(chargeCalculationEngine.calculate(
                request.getMethod(), request.getFixedAmount(),
                request.getRatePerUnit(), request.getPropertyContext()));
    }

    @PostMapping("/calculate/formula")
    public ResponseEntity<BigDecimal> evaluateFormula(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String expression,
            @RequestBody Map<String, Object> variables) {
        permissionCheckService.requireAnyPermission(principal, AdminPermissions.VIEW_ADMIN);
        return ResponseEntity.ok(chargeCalculationEngine.evaluateFormula(expression, variables));
    }

    @GetMapping("/formulas")
    public ResponseEntity<List<FormulaDto>> getFormulas(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, AdminPermissions.VIEW_ADMIN);
        return ResponseEntity.ok(formulaService.getAll());
    }

    @GetMapping("/slabs")
    public ResponseEntity<List<SlabConfigDto>> getSlabConfigs(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, AdminPermissions.VIEW_ADMIN);
        return ResponseEntity.ok(slabConfigService.getAll());
    }
}
