package com.manacommunity.api.controller;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.service.FlightGatewayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FlightController {

    private final FlightGatewayService flightGatewayService;
    private final LoggedInUserService loggedInUserService;

    // Step 1 — Search available flights
    @PostMapping("/search")
    public ResponseEntity<FlightSearchResponse> search(
            @Valid @RequestBody FlightSearchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(flightGatewayService.search(request));
    }

    // Step 2 — Re-validate fare before payment (prices change every 20-30 s)
    @PostMapping("/fare-quote")
    public ResponseEntity<FlightFareQuoteResponse> fareQuote(
            @Valid @RequestBody FlightFareQuoteRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(flightGatewayService.fareQuote(request));
    }

    // Step 3 — Hold PNR + initiate Razorpay order
    @PostMapping("/book")
    public ResponseEntity<FlightBookResponse> book(
            @Valid @RequestBody FlightBookRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(flightGatewayService.book(request));
    }

    // Step 4 — Issue ticket after successful Razorpay payment
    @PostMapping("/ticket")
    public ResponseEntity<FlightTicketResponse> issueTicket(
            @Valid @RequestBody FlightTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(flightGatewayService.issueTicket(request));
    }
}
