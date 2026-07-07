package com.manacommunity.api.retail.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.retail.dto.CustomerDto;
import com.manacommunity.api.retail.service.CustomerService;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/retail/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<CustomerDto>> getAllCustomers(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(customerService.getAllCustomers(user.getCommunity().getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<CustomerDto> createCustomer(
            @RequestBody CustomerDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(customerService.createCustomer(dto, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<CustomerDto> updateCustomer(
            @PathVariable Long id,
            @RequestBody CustomerDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(customerService.updateCustomer(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok().build();
    }
}
