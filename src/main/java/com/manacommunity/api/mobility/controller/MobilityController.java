package com.manacommunity.api.mobility.controller;

import com.manacommunity.api.mobility.dto.*;
import com.manacommunity.api.mobility.service.MobilityService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mobility")
@RequiredArgsConstructor
public class MobilityController {

    private final MobilityService        mobilityService;
    private final LoggedInUserService    loggedInUserService;

    // ── Passenger ──────────────────────────────────────────────────────────

    @PostMapping("/fare-quote")
    public ResponseEntity<MobilityFareQuoteDto> getFareQuote(
            @AuthenticationPrincipal UserPrincipal p,
            @Valid @RequestBody FareQuoteRequest req) {
        return ResponseEntity.ok(mobilityService.getFareQuote(user(p), req));
    }

    @PostMapping("/trips")
    public ResponseEntity<MobilityTripDto> requestRide(
            @AuthenticationPrincipal UserPrincipal p,
            @Valid @RequestBody CreateMobilityTripRequest req) {
        return ResponseEntity.ok(mobilityService.requestRide(user(p), req));
    }

    @GetMapping("/trips/active")
    public ResponseEntity<MobilityTripDto> getActiveTrip(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(mobilityService.getActiveTrip(user(p)));
    }

    @GetMapping("/trips/history")
    public ResponseEntity<Page<MobilityTripDto>> getTripHistory(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(mobilityService.getTripHistory(user(p), page, size));
    }

    @GetMapping("/trips/{id}")
    public ResponseEntity<MobilityTripDto> getTrip(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long id) {
        return ResponseEntity.ok(mobilityService.getTrip(user(p), id));
    }

    @PostMapping("/trips/{id}/cancel")
    public ResponseEntity<MobilityTripDto> cancelTrip(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long id,
            @RequestBody(required = false) CancelTripRequest req) {
        return ResponseEntity.ok(mobilityService.cancelTrip(user(p), id, req != null ? req : new CancelTripRequest()));
    }

    @PostMapping("/trips/{id}/rate")
    public ResponseEntity<Void> rateTrip(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long id,
            @Valid @RequestBody RateTripRequest req) {
        mobilityService.rateTrip(user(p), id, req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<MobilityStatsDto> getStats(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(mobilityService.getPassengerStats(user(p)));
    }

    @GetMapping("/service-zones")
    public ResponseEntity<List<Map<String, Object>>> getServiceZones(@AuthenticationPrincipal UserPrincipal p) {
        // Placeholder — return empty list until zone seeding is done
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/trips/{id}/location")
    public ResponseEntity<Map<String, Object>> getDriverLocation(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long id) {
        // Returns driver's current lat/lng from the driver entity
        MobilityTripDto trip = mobilityService.getTrip(user(p), id);
        if (trip.getDriver() == null) return ResponseEntity.ok(Map.of());
        return ResponseEntity.ok(Map.of("driverId", trip.getDriver().getDriverId()));
    }

    // ── Driver ─────────────────────────────────────────────────────────────

    @GetMapping("/driver/profile")
    public ResponseEntity<MobilityDriverDto> getDriverProfile(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(mobilityService.getMyDriverProfile(user(p)));
    }

    @PostMapping("/driver/register")
    public ResponseEntity<MobilityDriverDto> registerDriver(
            @AuthenticationPrincipal UserPrincipal p,
            @Valid @RequestBody RegisterDriverRequest req) {
        return ResponseEntity.ok(mobilityService.registerDriver(user(p), req));
    }

    @PatchMapping("/driver/availability")
    public ResponseEntity<MobilityDriverDto> setAvailability(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(mobilityService.setAvailability(user(p), body.getOrDefault("availability", "OFFLINE")));
    }

    @GetMapping("/driver/documents")
    public ResponseEntity<List<MobilityDriverDocumentDto>> getDocuments(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(mobilityService.getMyDocuments(user(p)));
    }

    @PostMapping("/driver/documents")
    public ResponseEntity<MobilityDriverDocumentDto> uploadDocument(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam String docType,
            @RequestParam MultipartFile file) {
        return ResponseEntity.ok(mobilityService.uploadDocument(user(p), docType, file));
    }

    @GetMapping("/driver/vehicles")
    public ResponseEntity<List<MobilityVehicleDto>> getVehicles(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(mobilityService.getMyVehicles(user(p)));
    }

    @GetMapping("/driver/earnings")
    public ResponseEntity<DriverEarningsSummaryDto> getEarnings(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(mobilityService.getMyEarnings(user(p)));
    }

    @GetMapping("/driver/offer")
    public ResponseEntity<DriverRideOfferDto> getPendingOffer(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(mobilityService.getPendingOffer(user(p)));
    }

    @PostMapping("/driver/offer/{offerId}/respond")
    public ResponseEntity<Void> respondToOffer(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long offerId,
            @Valid @RequestBody AcceptRejectOfferRequest req) {
        mobilityService.respondToOffer(user(p), offerId, req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/driver/trip/active")
    public ResponseEntity<MobilityTripDto> getDriverActiveTrip(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(mobilityService.getDriverActiveTrip(user(p)));
    }

    @PatchMapping("/driver/trip/{tripId}/status")
    public ResponseEntity<MobilityTripDto> updateTripStatus(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long tripId,
            @Valid @RequestBody UpdateTripStatusRequest req) {
        return ResponseEntity.ok(mobilityService.updateTripStatus(user(p), tripId, req.getStatus()));
    }

    @PostMapping("/driver/trip/{tripId}/verify-pin")
    public ResponseEntity<Map<String, Boolean>> verifyPin(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long tripId,
            @RequestBody Map<String, String> body) {
        boolean valid = mobilityService.verifyPickupPin(user(p), tripId, body.get("pin"));
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    // ── Carpool ────────────────────────────────────────────────────────────

    @GetMapping("/carpool/offers")
    public ResponseEntity<Page<MobilityCarpoolOfferDto>> getCarpoolOffers(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(required = false) String destination,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(mobilityService.getCarpoolOffers(user(p), destination, page, size));
    }

    @GetMapping("/carpool/offers/{id}")
    public ResponseEntity<MobilityCarpoolOfferDto> getCarpoolOffer(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long id) {
        return ResponseEntity.ok(mobilityService.getCarpoolOffer(user(p), id));
    }

    @PostMapping("/carpool/offers/{id}/join")
    public ResponseEntity<Void> joinCarpool(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long id,
            @RequestBody(required = false) JoinCarpoolRequest req) {
        mobilityService.joinCarpoolOffer(user(p), id, req != null ? req : new JoinCarpoolRequest());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/carpool/offers")
    public ResponseEntity<MobilityCarpoolOfferDto> createCarpoolOffer(
            @AuthenticationPrincipal UserPrincipal p,
            @Valid @RequestBody CreateCarpoolOfferRequest req) {
        return ResponseEntity.ok(mobilityService.createCarpoolOffer(user(p), req));
    }

    @GetMapping("/journeys/config")
    public ResponseEntity<List<JourneyTypeConfigDto>> getJourneyTypeConfigs() {
        return ResponseEntity.ok(mobilityService.getJourneyTypeConfigs());
    }

    @GetMapping("/journeys/search")
    public ResponseEntity<Page<MobilityCarpoolOfferDto>> searchJourneyOffers(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam String journeyType,
            @RequestParam String hubCode,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String travelAt,
            @RequestParam(defaultValue = "30") int timeWindowMinutes,
            @RequestParam(defaultValue = "1") int seats,
            @RequestParam(defaultValue = "0") int luggageCount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(mobilityService.searchJourneyOffers(user(p), journeyType, hubCode, area,
                travelAt, timeWindowMinutes, seats, luggageCount, page, size));
    }

    @PostMapping("/journeys/offers")
    public ResponseEntity<MobilityCarpoolOfferDto> createJourneyOffer(
            @AuthenticationPrincipal UserPrincipal p,
            @Valid @RequestBody CreateCarpoolOfferRequest req) {
        return ResponseEntity.ok(mobilityService.createCarpoolOffer(user(p), req));
    }

    @GetMapping("/journeys/offers/{id}")
    public ResponseEntity<MobilityCarpoolOfferDto> getJourneyOffer(
            @AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        return ResponseEntity.ok(mobilityService.getCarpoolOffer(user(p), id));
    }

    @PostMapping("/journeys/offers/{id}/join")
    public ResponseEntity<Void> joinJourneyOffer(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long id,
            @RequestBody(required = false) JoinCarpoolRequest req) {
        mobilityService.joinCarpoolOffer(user(p), id, req != null ? req : new JoinCarpoolRequest());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/journeys/requests")
    public ResponseEntity<AirportRideRequestDto> createJourneyRequest(
            @AuthenticationPrincipal UserPrincipal p,
            @Valid @RequestBody CreateJourneyRequest req) {
        return ResponseEntity.ok(mobilityService.createJourneyRequest(user(p), req));
    }

    @GetMapping("/journeys/requests/mine")
    public ResponseEntity<Page<AirportRideRequestDto>> getMyJourneyRequests(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(mobilityService.getMyJourneyRequests(user(p), page, size));
    }

    @GetMapping("/airport-rides/airports")
    public ResponseEntity<List<Map<String, String>>> getAirportRidesAirports() {
        return ResponseEntity.ok(List.of(
                Map.of("code", "HYD", "name", "Rajiv Gandhi International Airport", "city", "Hyderabad"),
                Map.of("code", "BLR", "name", "Kempegowda International Airport", "city", "Bengaluru"),
                Map.of("code", "DEL", "name", "Indira Gandhi International Airport", "city", "Delhi"),
                Map.of("code", "BOM", "name", "Chhatrapati Shivaji Maharaj International Airport", "city", "Mumbai")
        ));
    }

    @GetMapping("/airport-rides/search")
    public ResponseEntity<Page<MobilityCarpoolOfferDto>> searchAirportRides(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam String airportCode,
            @RequestParam String direction,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String travelDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(mobilityService.getAirportRides(user(p), airportCode, direction, area, travelDate, page, size));
    }

    @PostMapping("/airport-rides")
    public ResponseEntity<MobilityCarpoolOfferDto> createAirportRide(
            @AuthenticationPrincipal UserPrincipal p,
            @Valid @RequestBody CreateCarpoolOfferRequest req) {
        if (req.getAirportCode() == null || req.getAirportDirection() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "airportCode and airportDirection are required");
        }
        return ResponseEntity.ok(mobilityService.createCarpoolOffer(user(p), req));
    }

    @GetMapping("/airport-rides/{id}")
    public ResponseEntity<MobilityCarpoolOfferDto> getAirportRide(
            @AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        return ResponseEntity.ok(mobilityService.getCarpoolOffer(user(p), id));
    }

    @PostMapping("/airport-rides/{id}/join")
    public ResponseEntity<Void> joinAirportRide(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long id,
            @RequestBody(required = false) JoinCarpoolRequest req) {
        mobilityService.joinCarpoolOffer(user(p), id, req != null ? req : new JoinCarpoolRequest());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/airport-rides/my-trips")
    public ResponseEntity<Page<MobilityCarpoolOfferDto>> getMyAirportTrips(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(mobilityService.getMyAirportRides(user(p), page, size));
    }

    @PostMapping("/airport-rides/requests")
    public ResponseEntity<AirportRideRequestDto> createAirportRideRequest(
            @AuthenticationPrincipal UserPrincipal p,
            @Valid @RequestBody CreateAirportRideRequest req) {
        return ResponseEntity.ok(mobilityService.createAirportRideRequest(user(p), req));
    }

    @GetMapping("/airport-rides/requests/mine")
    public ResponseEntity<Page<AirportRideRequestDto>> getMyAirportRideRequests(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(mobilityService.getMyAirportRideRequests(user(p), page, size));
    }

    @GetMapping("/airport-rides/requests/{requestId}/matches")
    public ResponseEntity<Page<MobilityCarpoolOfferDto>> getAirportRideMatches(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long requestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(mobilityService.getAirportRideMatches(user(p), requestId, page, size));
    }

    // ── Ops / Admin ────────────────────────────────────────────────────────

    @GetMapping("/ops/documents/pending")
    public ResponseEntity<Page<PendingDocumentReviewDto>> getPendingDocuments(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(mobilityService.getPendingDocuments(page, size));
    }

    @PostMapping("/ops/drivers/{driverId}/documents/{documentId}/review")
    public ResponseEntity<Void> reviewDocument(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long driverId,
            @PathVariable Long documentId,
            @Valid @RequestBody ReviewDocumentRequest req) {
        mobilityService.reviewDocument(user(p), driverId, documentId, req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ops/trips/active")
    public ResponseEntity<Page<MobilityTripDto>> getActiveTrips(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(mobilityService.getActiveTripsOps(page, size));
    }

    @PostMapping("/ops/drivers/{driverId}/suspend")
    public ResponseEntity<Void> suspendDriver(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long driverId,
            @RequestBody(required = false) Map<String, String> body) {
        mobilityService.suspendDriver(user(p), driverId, body != null ? body.get("reason") : null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ops/drivers/{driverId}/reinstate")
    public ResponseEntity<Void> reinstateDriver(
            @AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long driverId) {
        mobilityService.reinstateDriver(user(p), driverId);
        return ResponseEntity.noContent().build();
    }

    // ── Util ───────────────────────────────────────────────────────────────

    private AppUser user(UserPrincipal p) {
        return loggedInUserService.resolve(p);
    }
}
