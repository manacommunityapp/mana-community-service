package com.manacommunity.api.vendor.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vendor.dto.ContractRequest;
import com.manacommunity.api.vendor.dto.ContractResponse;
import com.manacommunity.api.vendor.service.VmsContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendor/contracts")
@RequiredArgsConstructor
public class VmsContractController {

    private final VmsContractService contractService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('Manage Contracts')")
    public ResponseEntity<Page<ContractResponse>> getContracts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(contractService.getCommunityContracts(user.getCommunity().getId(), status, pageable));
    }

    @GetMapping("/vendor/{vendorId}")
    @PreAuthorize("hasAuthority('Manage Contracts')")
    public ResponseEntity<Page<ContractResponse>> getVendorContracts(
            @PathVariable Long vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(contractService.getVendorContracts(vendorId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Contracts')")
    public ResponseEntity<ContractResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(contractService.getById(id, user.getCommunity().getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Manage Contracts')")
    public ResponseEntity<ContractResponse> create(
            @Valid @RequestBody ContractRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contractService.create(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Contracts')")
    public ResponseEntity<ContractResponse> update(@PathVariable Long id, @Valid @RequestBody ContractRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(contractService.update(id, req, user.getCommunity().getId()));
    }

    @PutMapping("/{id}/renew")
    @PreAuthorize("hasAuthority('Manage Contracts')")
    public ResponseEntity<ContractResponse> renew(@PathVariable Long id, @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(contractService.renew(id, java.time.LocalDate.parse(body.get("newEndDate")), user.getCommunity().getId()));
    }

    @PutMapping("/{id}/terminate")
    @PreAuthorize("hasAuthority('Manage Contracts')")
    public ResponseEntity<ContractResponse> terminate(@PathVariable Long id, @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(contractService.terminate(id, body.getOrDefault("reason", ""), user.getCommunity().getId()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('Manage Contracts')")
    public ResponseEntity<ContractResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(contractService.updateStatus(id, status, user.getCommunity().getId()));
    }
}
