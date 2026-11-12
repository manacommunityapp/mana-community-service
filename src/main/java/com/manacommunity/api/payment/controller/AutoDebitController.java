package com.manacommunity.api.payment.controller;

import com.manacommunity.api.payment.dto.SetupMandateRequest;
import com.manacommunity.api.payment.entity.BankMandate;
import com.manacommunity.api.payment.service.AutoDebitService;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments/auto-debit/mandate")
@RequiredArgsConstructor
public class AutoDebitController {

    private final AutoDebitService autoDebitService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<Optional<BankMandate>> getMandate(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(autoDebitService.getMandate(loggedInUserService.resolve(principal)));
    }

    @PostMapping
    public ResponseEntity<BankMandate> setupMandate(@RequestBody SetupMandateRequest req,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(autoDebitService.setupMandate(req, loggedInUserService.resolve(principal)));
    }

    @DeleteMapping
    public ResponseEntity<Void> cancelMandate(@AuthenticationPrincipal UserPrincipal principal) {
        autoDebitService.cancelMandate(loggedInUserService.resolve(principal));
        return ResponseEntity.noContent().build();
    }
}
