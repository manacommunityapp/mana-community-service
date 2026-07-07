package com.manacommunity.api.finance.controller;

import com.manacommunity.api.finance.dto.FinanceCreditNoteDto;
import com.manacommunity.api.finance.service.FinanceCreditNoteService;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.manacommunity.api.constants.PermissionConstants.VIEW_ADMIN;

/** Dedicated REST API for the Income → Credit Notes menu. */
@RestController
@RequestMapping("/api/finance/credit-notes")
@RequiredArgsConstructor
public class FinanceCreditNoteController {

    private final FinanceCreditNoteService service;
    private final PermissionCheckService permissionCheckService;

    @GetMapping
    public ResponseEntity<List<FinanceCreditNoteDto>> getAll(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.getAll(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinanceCreditNoteDto> get(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping
    public ResponseEntity<FinanceCreditNoteDto> create(@RequestBody FinanceCreditNoteDto body,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.create(body));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FinanceCreditNoteDto> update(@PathVariable Long id, @RequestBody FinanceCreditNoteDto body,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
