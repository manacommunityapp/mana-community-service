package com.manacommunity.api.food.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.food.service.FoodCorporateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/food/corporate")
@RequiredArgsConstructor
public class FoodCorporateController {

    private final FoodCorporateService corporateService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/accounts")
    @PreAuthorize("hasAuthority('View Food Corporate')")
    public ResponseEntity<?> list(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(corporateService.list(communityId));
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasAuthority('Manage Food Corporate')")
    public ResponseEntity<?> create(@AuthenticationPrincipal UserPrincipal principal,
                                    @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(corporateService.create(communityId, request));
    }

    @PostMapping("/meal-cards")
    @PreAuthorize("hasAuthority('Manage Food Corporate')")
    public ResponseEntity<?> issue(@AuthenticationPrincipal UserPrincipal principal,
                                   @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(corporateService.issue(communityId, request));
    }

    @GetMapping("/accounts/{accountId}/meal-cards")
    @PreAuthorize("hasAuthority('View Food Corporate')")
    public ResponseEntity<?> getCards(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long accountId) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(corporateService.getCards(communityId, accountId));
    }

    @GetMapping("/meal-cards/{cardId}/transactions")
    @PreAuthorize("hasAuthority('View Food Corporate')")
    public ResponseEntity<?> getTransactions(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable Long cardId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(corporateService.getTransactions(communityId, cardId, PageRequest.of(page, size)));
    }

    @GetMapping("/cafeterias")
    @PreAuthorize("hasAuthority('View Food Corporate')")
    public ResponseEntity<?> listCafeterias(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(corporateService.listCafeterias(communityId));
    }

    @PostMapping("/cafeterias")
    @PreAuthorize("hasAuthority('Manage Food Corporate')")
    public ResponseEntity<?> createCafeteria(@AuthenticationPrincipal UserPrincipal principal,
                                             @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(corporateService.createCafeteria(communityId, request));
    }

    @GetMapping("/cafeterias/{id}/menu")
    @PreAuthorize("hasAuthority('View Food Corporate')")
    public ResponseEntity<?> getMenu(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long id,
                                     @RequestParam LocalDate date) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(corporateService.getMenu(communityId, id, date));
    }

    @PostMapping("/catering-requests")
    @PreAuthorize("hasAuthority('Manage Food Corporate')")
    public ResponseEntity<?> createRequest(@AuthenticationPrincipal UserPrincipal principal,
                                           @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(corporateService.createRequest(communityId, request, user));
    }
}
