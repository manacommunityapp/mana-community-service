package com.manacommunity.api.mobility.service;

import com.manacommunity.api.mobility.dto.*;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MobilityService {

    // ── Passenger ──────────────────────────────────────────────────
    MobilityFareQuoteDto getFareQuote(AppUser user, FareQuoteRequest req);
    MobilityTripDto requestRide(AppUser user, CreateMobilityTripRequest req);
    MobilityTripDto getActiveTrip(AppUser user);
    MobilityTripDto getTrip(AppUser user, Long tripId);
    Page<MobilityTripDto> getTripHistory(AppUser user, int page, int size);
    MobilityTripDto cancelTrip(AppUser user, Long tripId, CancelTripRequest req);
    void rateTrip(AppUser user, Long tripId, RateTripRequest req);
    MobilityStatsDto getPassengerStats(AppUser user);

    // ── Driver ─────────────────────────────────────────────────────
    MobilityDriverDto getMyDriverProfile(AppUser user);
    MobilityDriverDto registerDriver(AppUser user, RegisterDriverRequest req);
    MobilityDriverDto setAvailability(AppUser user, String availability);
    List<MobilityDriverDocumentDto> getMyDocuments(AppUser user);
    MobilityDriverDocumentDto uploadDocument(AppUser user, String docType, MultipartFile file);
    List<MobilityVehicleDto> getMyVehicles(AppUser user);
    DriverEarningsSummaryDto getMyEarnings(AppUser user);
    DriverRideOfferDto getPendingOffer(AppUser user);
    void respondToOffer(AppUser user, Long offerId, AcceptRejectOfferRequest req);
    MobilityTripDto getDriverActiveTrip(AppUser user);
    MobilityTripDto updateTripStatus(AppUser user, Long tripId, String status);
    boolean verifyPickupPin(AppUser user, Long tripId, String pin);

    // ── Carpool ────────────────────────────────────────────────────
    Page<MobilityCarpoolOfferDto> getCarpoolOffers(AppUser user, String destination, int page, int size);
    MobilityCarpoolOfferDto getCarpoolOffer(AppUser user, Long offerId);
    void joinCarpoolOffer(AppUser user, Long offerId, JoinCarpoolRequest req);
    MobilityCarpoolOfferDto createCarpoolOffer(AppUser user, CreateCarpoolOfferRequest req);
    Page<MobilityCarpoolOfferDto> getAirportRides(AppUser user, String airportCode, String direction, String area, String travelDate, int page, int size);
    Page<MobilityCarpoolOfferDto> getMyAirportRides(AppUser user, int page, int size);
    AirportRideRequestDto createAirportRideRequest(AppUser user, CreateAirportRideRequest req);
    Page<AirportRideRequestDto> getMyAirportRideRequests(AppUser user, int page, int size);
    Page<MobilityCarpoolOfferDto> getAirportRideMatches(AppUser user, Long requestId, int page, int size);
    List<JourneyTypeConfigDto> getJourneyTypeConfigs();
    Page<MobilityCarpoolOfferDto> searchJourneyOffers(AppUser user, String journeyType, String hubCode, String direction, String area,
                                                      String travelAt, int timeWindowMinutes, int seats,
                                                       int luggageCount, int page, int size);
    AirportRideRequestDto createJourneyRequest(AppUser user, CreateJourneyRequest req);
    Page<AirportRideRequestDto> getMyJourneyRequests(AppUser user, int page, int size);
    Page<MobilityCarpoolOfferDto> getJourneyMatches(AppUser user, Long requestId, int page, int size);
    void cancelCarpoolBooking(AppUser user, Long offerId);
    void joinCarpoolWaitlist(AppUser user, Long offerId, JoinCarpoolRequest req);
    List<JourneySlotDto> getJourneySlots(AppUser user, String journeyType, String hubCode);
    JourneySlotDto createJourneySlot(AppUser user, CreateJourneySlotRequest req);
    JourneySlotDto updateJourneySlotStatus(AppUser user, Long slotId, String status);
    Page<JourneyMatchProposalDto> getJourneyProposals(AppUser user, int page, int size);
    void respondToJourneyProposal(AppUser user, Long proposalId, boolean accept);
    Page<JourneyBookingDto> getMyJourneyBookings(AppUser user, int page, int size);
    List<JourneyBookingDto> getJourneyPassengers(AppUser user, Long offerId);
    void cancelJourneyOffer(AppUser user, Long offerId);
    void cancelJourneyRequest(AppUser user, Long requestId);

    // ── Ops / Admin ────────────────────────────────────────────────
    Page<PendingDocumentReviewDto> getPendingDocuments(int page, int size);
    void reviewDocument(AppUser reviewer, Long driverId, Long documentId, ReviewDocumentRequest req);
    Page<MobilityTripDto> getActiveTripsOps(int page, int size);
    void suspendDriver(AppUser ops, Long driverId, String reason);
    void reinstateDriver(AppUser ops, Long driverId);
}
