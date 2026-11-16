package com.manacommunity.api.access.controller;

import com.manacommunity.api.access.entity.AccessCard;
import com.manacommunity.api.access.service.AccessCardService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/access/cards")
@RequiredArgsConstructor
public class AccessCardController {

    private final AccessCardService accessCardService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<AccessCard>> getCards(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long communityId,
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(accessCardService.getCards(communityId, userId));
    }

    @PostMapping
    public ResponseEntity<AccessCard> issueCard(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody IssueCardRequest request) {
        AppUser caller = loggedInUserService.resolve(principal);
        AccessCard card = AccessCard.builder()
                .communityId(caller.getCommunity().getId())
                .userId(request.getUserId())
                .flatNo(request.getFlatNo())
                .cardNumber(request.getCardNumber())
                .cardType(request.getCardType() != null ? request.getCardType() : "RFID")
                .issuedDate(request.getIssuedDate() != null ? request.getIssuedDate() : LocalDate.now())
                .expiryDate(request.getExpiryDate())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(accessCardService.issueCard(card));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AccessCard> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(accessCardService.updateStatus(id, body.get("status"), body.get("reason")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateCard(@PathVariable Long id) {
        accessCardService.deactivateCard(id);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class IssueCardRequest {
        private Long userId;
        private String flatNo;
        private String cardNumber;
        private String cardType;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate issuedDate;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate expiryDate;
    }
}
