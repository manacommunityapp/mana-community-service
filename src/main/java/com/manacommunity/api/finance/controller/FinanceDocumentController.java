package com.manacommunity.api.finance.controller;

import com.manacommunity.api.finance.dto.FinanceDocumentDto;
import com.manacommunity.api.finance.entity.FinanceDocument;
import com.manacommunity.api.finance.service.FinanceDocumentService;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.manacommunity.api.constants.permissions.AdminPermissions.VIEW_ADMIN;

@RestController
@RequestMapping("/api/finance/documents")
@RequiredArgsConstructor
public class FinanceDocumentController {

    private final FinanceDocumentService service;
    private final PermissionCheckService permissionCheckService;

    @GetMapping
    public ResponseEntity<List<FinanceDocumentDto>> getAll(
            @RequestParam(required = false) FinanceDocument.Type type,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.getAll(type));
    }

    @PostMapping
    public ResponseEntity<FinanceDocumentDto> create(@RequestBody FinanceDocumentDto body,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.create(body));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FinanceDocumentDto> update(@PathVariable Long id, @RequestBody FinanceDocumentDto body,
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
