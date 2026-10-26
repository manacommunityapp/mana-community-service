package com.manacommunity.api.mobility.service.impl;

import com.manacommunity.api.mobility.dto.*;
import com.manacommunity.api.mobility.model.*;
import com.manacommunity.api.mobility.repository.*;
import com.manacommunity.api.mobility.service.MobilityService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.trip.engine.CommuteMatchingEngine;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MobilityServiceImpl implements MobilityService {

    private final MobilityTripRepository         tripRepo;
    private final MobilityDriverRepository       driverRepo;
    private final MobilityDriverDocumentRepository docRepo;
    private final MobilityVehicleRepository      vehicleRepo;
    private final MobilityDriverOfferRepository  offerRepo;
    private final MobilityCarpoolOfferRepository carpoolRepo;
    private final CommunityRepository            communityRepo;
    private final CommuteMatchingEngine          matchingEngine;

    // ── Fare rates ────────────────────────────────────────────────
    private static final BigDecimal BASE_FARE       = new BigDecimal("20");
    private static final BigDecimal RATE_PER_KM_BIKE  = new BigDecimal("4");
    private static final BigDecimal RATE_PER_KM_AUTO  = new BigDecimal("8");
    private static final BigDecimal RATE_PER_KM_CAB   = new BigDecimal("12");
    private static final BigDecimal RATE_PER_KM_POOL  = new BigDecimal("5");
    private static final BigDecimal TAX_RATE         = new BigDecimal("0.05");
    private static final int OFFER_TTL_SECONDS       = 35;

    // ── Passenger ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public MobilityFareQuoteDto getFareQuote(AppUser user, FareQuoteRequest req) {
        double distKm = matchingEngine.calculateDistanceKm(
                req.getPickupLat(), req.getPickupLng(), req.getDestLat(), req.getDestLng());

        BigDecimal ratePerKm = rateForType(req.getServiceType());
        BigDecimal base      = BASE_FARE;
        BigDecimal dist      = ratePerKm.multiply(BigDecimal.valueOf(distKm)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal duration  = BigDecimal.ZERO;
        BigDecimal subtotal  = base.add(dist).add(duration);
        BigDecimal taxes     = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total     = subtotal.add(taxes).setScale(2, RoundingMode.HALF_UP);
        int eta              = Math.max(5, (int) (distKm / 0.5));  // rough: 30km/h avg

        return MobilityFareQuoteDto.builder()
                .quoteId(UUID.randomUUID().toString())
                .baseFare(base)
                .distanceFare(dist)
                .durationFare(duration)
                .taxes(taxes)
                .total(total)
                .distanceKm(Math.round(distKm * 10.0) / 10.0)
                .etaMinutes(eta)
                .expiresAt(Instant.now().plusSeconds(300).toEpochMilli())
                .build();
    }

    @Override
    public MobilityTripDto requestRide(AppUser user, CreateMobilityTripRequest req) {
        if (tripRepo.existsByPassengerIdAndIdempotencyKey(user.getId(), req.getIdempotencyKey())) {
            // Idempotent re-submit: return the existing trip
            return tripRepo.findActiveByPassenger(user.getId(), activeStatuses())
                    .map(this::toDto)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate request"));
        }

        Community community = communityRepo.findById(user.getCommunity().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Community not found"));

        double distKm = matchingEngine.calculateDistanceKm(
                req.getPickupLat(), req.getPickupLng(), req.getDestLat(), req.getDestLng());

        BigDecimal ratePerKm = rateForType(req.getServiceType());
        BigDecimal base      = BASE_FARE;
        BigDecimal dist      = ratePerKm.multiply(BigDecimal.valueOf(distKm)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxes     = base.add(dist).multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total     = base.add(dist).add(taxes).setScale(2, RoundingMode.HALF_UP);

        MobilityTrip trip = MobilityTrip.builder()
                .tripRef(generateRef())
                .community(community)
                .passenger(user)
                .serviceType(req.getServiceType())
                .status(MobilityTripStatus.SEARCHING)
                .pickupLat(req.getPickupLat())
                .pickupLng(req.getPickupLng())
                .pickupAddress(req.getPickupAddress())
                .destLat(req.getDestLat())
                .destLng(req.getDestLng())
                .destAddress(req.getDestAddress())
                .pickupPin(generatePin())
                .idempotencyKey(req.getIdempotencyKey())
                .quoteId(req.getQuoteId())
                .baseFare(base)
                .distanceFare(dist)
                .durationFare(BigDecimal.ZERO)
                .taxes(taxes)
                .quotedTotal(total)
                .distanceKm(BigDecimal.valueOf(distKm).setScale(2, RoundingMode.HALF_UP))
                .etaMinutes(Math.max(5, (int)(distKm / 0.5)))
                .paymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "CASH")
                .promoCode(req.getPromoCode())
                .build();

        tripRepo.save(trip);
        dispatchToNearestDriver(trip);
        return toDto(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public MobilityTripDto getActiveTrip(AppUser user) {
        return tripRepo.findActiveByPassenger(user.getId(), activeStatuses())
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public MobilityTripDto getTrip(AppUser user, Long tripId) {
        return toDto(findTripForUser(user, tripId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MobilityTripDto> getTripHistory(AppUser user, int page, int size) {
        return tripRepo.findByPassengerIdOrderByRequestedAtDesc(
                user.getId(), PageRequest.of(page, Math.min(size, 50))).map(this::toDto);
    }

    @Override
    public MobilityTripDto cancelTrip(AppUser user, Long tripId, CancelTripRequest req) {
        MobilityTrip trip = findTripForUser(user, tripId);
        if (!activeStatuses().contains(trip.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trip cannot be cancelled in status: " + trip.getStatus());
        }
        trip.setStatus(MobilityTripStatus.CANCELLED);
        trip.setCancelReason(req.getReason());
        trip.setCancelledAt(Instant.now());
        offerRepo.deleteByTripIdAndAcceptedIsNull(tripId);
        return toDto(trip);
    }

    @Override
    public void rateTrip(AppUser user, Long tripId, RateTripRequest req) {
        MobilityTrip trip = findTripForUser(user, tripId);
        if (trip.getStatus() != MobilityTripStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only rate completed trips");
        }
        trip.setPassengerRating(req.getRating().shortValue());
        // Update driver average asynchronously; simplified: recalc in-place
        if (trip.getDriver() != null) {
            MobilityDriver driver = trip.getDriver();
            long prior = driver.getTotalTrips();
            BigDecimal newAvg = driver.getAvgRating()
                    .multiply(BigDecimal.valueOf(prior))
                    .add(BigDecimal.valueOf(req.getRating()))
                    .divide(BigDecimal.valueOf(prior + 1), 2, RoundingMode.HALF_UP);
            driver.setAvgRating(newAvg);
            driverRepo.save(driver);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MobilityStatsDto getPassengerStats(AppUser user) {
        List<MobilityTrip> all = tripRepo.findByPassengerIdOrderByRequestedAtDesc(user.getId(), PageRequest.of(0, 1000)).getContent();
        long completed  = all.stream().filter(t -> t.getStatus() == MobilityTripStatus.COMPLETED).count();
        long cancelled  = all.stream().filter(t -> t.getStatus() == MobilityTripStatus.CANCELLED).count();
        BigDecimal spend = all.stream()
                .filter(t -> t.getStatus() == MobilityTripStatus.COMPLETED)
                .map(t -> t.getFinalFare() != null ? t.getFinalFare() : t.getQuotedTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return MobilityStatsDto.builder()
                .totalTrips(all.size())
                .completedTrips(completed)
                .cancelledTrips(cancelled)
                .totalSpend(spend)
                .avgRating(0)
                .build();
    }

    // ── Driver ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public MobilityDriverDto getMyDriverProfile(AppUser user) {
        return driverRepo.findByUserId(user.getId()).map(this::toDriverDto).orElse(null);
    }

    @Override
    public MobilityDriverDto registerDriver(AppUser user, RegisterDriverRequest req) {
        if (driverRepo.existsByUserId(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already registered as a driver");
        }
        MobilityDriver driver = driverRepo.save(MobilityDriver.builder()
                .user(user)
                .status(MobilityDriverStatus.PENDING)
                .availability("OFFLINE")
                .build());

        vehicleRepo.save(MobilityVehicle.builder()
                .driver(driver)
                .vehicleType(req.getVehicleType())
                .model(req.getVehicleModel())
                .numberPlate(req.getVehicleNumberPlate())
                .color(req.getVehicleColor())
                .year(req.getVehicleYear())
                .active(true)
                .build());

        return toDriverDto(driver);
    }

    @Override
    public MobilityDriverDto setAvailability(AppUser user, String availability) {
        MobilityDriver driver = requireDriver(user);
        if (driver.getStatus() != MobilityDriverStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver not approved yet");
        }
        driver.setAvailability(availability.toUpperCase());
        return toDriverDto(driverRepo.save(driver));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MobilityDriverDocumentDto> getMyDocuments(AppUser user) {
        MobilityDriver driver = requireDriver(user);
        return docRepo.findByDriverId(driver.getId()).stream().map(this::toDocDto).collect(Collectors.toList());
    }

    @Override
    public MobilityDriverDocumentDto uploadDocument(AppUser user, String docType, MultipartFile file) {
        MobilityDriver driver = requireDriver(user);
        // File upload to storage omitted; store a placeholder URL
        String fileUrl = "/uploads/mobility/driver/" + driver.getId() + "/" + docType + "_" + System.currentTimeMillis();
        MobilityDriverDocument doc = docRepo.save(MobilityDriverDocument.builder()
                .driver(driver)
                .docType(MobilityDocumentType.valueOf(docType.toUpperCase()))
                .fileUrl(fileUrl)
                .status(MobilityDocumentStatus.PENDING)
                .build());
        return toDocDto(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MobilityVehicleDto> getMyVehicles(AppUser user) {
        MobilityDriver driver = requireDriver(user);
        return vehicleRepo.findByDriverIdAndActiveTrue(driver.getId()).stream().map(this::toVehicleDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DriverEarningsSummaryDto getMyEarnings(AppUser user) {
        MobilityDriver driver = requireDriver(user);
        // Simplified: scan recent completed trips; in production, use earnings daily ledger
        List<MobilityTrip> completed = tripRepo.findByPassengerIdOrderByRequestedAtDesc(
                driver.getUser().getId(), PageRequest.of(0, 500)).stream()
                .filter(t -> t.getStatus() == MobilityTripStatus.COMPLETED
                          && t.getDriver() != null
                          && t.getDriver().getId().equals(driver.getId()))
                .collect(Collectors.toList());

        Instant today = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant weekAgo = today.minusSeconds(7 * 86400L);
        Instant monthAgo = today.minusSeconds(30L * 86400);

        BigDecimal todayEarn = completed.stream().filter(t -> t.getCompletedAt() != null && t.getCompletedAt().isAfter(today))
                .map(t -> t.getFinalFare() != null ? t.getFinalFare() : t.getQuotedTotal()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal weekEarn = completed.stream().filter(t -> t.getCompletedAt() != null && t.getCompletedAt().isAfter(weekAgo))
                .map(t -> t.getFinalFare() != null ? t.getFinalFare() : t.getQuotedTotal()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthEarn = completed.stream().filter(t -> t.getCompletedAt() != null && t.getCompletedAt().isAfter(monthAgo))
                .map(t -> t.getFinalFare() != null ? t.getFinalFare() : t.getQuotedTotal()).reduce(BigDecimal.ZERO, BigDecimal::add);

        return DriverEarningsSummaryDto.builder()
                .todayEarnings(todayEarn)
                .weekEarnings(weekEarn)
                .monthEarnings(monthEarn)
                .pendingSettlement(BigDecimal.ZERO)
                .todayTrips((int) completed.stream().filter(t -> t.getCompletedAt() != null && t.getCompletedAt().isAfter(today)).count())
                .totalTrips(driver.getTotalTrips())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DriverRideOfferDto getPendingOffer(AppUser user) {
        MobilityDriver driver = requireDriver(user);
        return offerRepo.findPendingByDriver(driver.getId(), Instant.now()).map(offer -> {
            MobilityTrip trip = offer.getTrip();
            String passengerName = trip.getPassenger().getFullName() != null
                    ? trip.getPassenger().getFullName()
                    : "Passenger";
            return DriverRideOfferDto.builder()
                    .offerId(String.valueOf(offer.getId()))
                    .tripId(trip.getId())
                    .passengerName(passengerName.trim())
                    .pickupAddress(trip.getPickupAddress())
                    .destAddress(trip.getDestAddress())
                    .fareAmount(trip.getQuotedTotal())
                    .distanceKm(trip.getDistanceKm() != null ? trip.getDistanceKm().doubleValue() : 0)
                    .expiresAt(offer.getExpiresAt())
                    .build();
        }).orElse(null);
    }

    @Override
    public void respondToOffer(AppUser user, Long offerId, AcceptRejectOfferRequest req) {
        MobilityDriver driver = requireDriver(user);
        MobilityDriverOffer offer = offerRepo.findById(offerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
        if (!offer.getDriver().getId().equals(driver.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your offer");
        }
        if (offer.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Offer has expired");
        }
        offer.setAccepted(req.getAccept());
        offerRepo.save(offer);

        if (Boolean.TRUE.equals(req.getAccept())) {
            MobilityTrip trip = offer.getTrip();
            trip.setDriver(driver);
            trip.setStatus(MobilityTripStatus.ASSIGNED);
            trip.setAssignedAt(Instant.now());
            tripRepo.save(trip);
            driver.setAvailability("BUSY");
            driverRepo.save(driver);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MobilityTripDto getDriverActiveTrip(AppUser user) {
        MobilityDriver driver = requireDriver(user);
        return tripRepo.findActiveByDriver(driver.getId(), activeStatuses()).map(this::toDto).orElse(null);
    }

    @Override
    public MobilityTripDto updateTripStatus(AppUser user, Long tripId, String statusStr) {
        MobilityDriver driver = requireDriver(user);
        MobilityTrip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your trip");
        }
        MobilityTripStatus newStatus = MobilityTripStatus.valueOf(statusStr.toUpperCase());
        trip.setStatus(newStatus);
        switch (newStatus) {
            case ARRIVED     -> trip.setArrivedAt(Instant.now());
            case PICKED_UP   -> trip.setPickedUpAt(Instant.now());
            case COMPLETED   -> {
                trip.setCompletedAt(Instant.now());
                trip.setFinalFare(trip.getQuotedTotal());
                driver.setTotalTrips(driver.getTotalTrips() + 1);
                driver.setAvailability("ONLINE");
                driverRepo.save(driver);
            }
            default -> {}
        }
        return toDto(tripRepo.save(trip));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyPickupPin(AppUser user, Long tripId, String pin) {
        MobilityDriver driver = requireDriver(user);
        MobilityTrip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));
        return pin != null && pin.equals(trip.getPickupPin());
    }

    // ── Carpool ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<MobilityCarpoolOfferDto> getCarpoolOffers(AppUser user, String destination, int page, int size) {
        Long communityId = user.getCommunity().getId();
        return carpoolRepo.findActive(communityId, destination, PageRequest.of(page, Math.min(size, 50)))
                .map(this::toCarpoolDto);
    }

    @Override
    @Transactional(readOnly = true)
    public MobilityCarpoolOfferDto getCarpoolOffer(AppUser user, Long offerId) {
        return toCarpoolDto(carpoolRepo.findById(offerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found")));
    }

    @Override
    public void joinCarpoolOffer(AppUser user, Long offerId, JoinCarpoolRequest req) {
        MobilityCarpoolOffer offer = carpoolRepo.findById(offerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
        if (offer.getAvailableSeats() < req.getSeats()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough seats available");
        }
        offer.setAvailableSeats((short) (offer.getAvailableSeats() - req.getSeats()));
        if (offer.getAvailableSeats() == 0) offer.setStatus("FULL");
        carpoolRepo.save(offer);
    }

    @Override
    public MobilityCarpoolOfferDto createCarpoolOffer(AppUser user, CreateCarpoolOfferRequest req) {
        Community community = communityRepo.findById(user.getCommunity().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Community not found"));
        MobilityCarpoolOffer offer = carpoolRepo.save(MobilityCarpoolOffer.builder()
                .community(community)
                .driverUser(user)
                .origin(req.getOrigin())
                .destination(req.getDestination())
                .departureTime(Instant.parse(req.getDepartureTime()))
                .totalSeats((short) req.getTotalSeats())
                .availableSeats((short) req.getTotalSeats())
                .pricePerSeat(req.getPricePerSeat() != null ? req.getPricePerSeat() : BigDecimal.ZERO)
                .vehicleModel(req.getVehicleModel())
                .vehicleNumber(req.getVehicleNumber())
                .status("ACTIVE")
                .build());
        return toCarpoolDto(offer);
    }

    // ── Ops ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<PendingDocumentReviewDto> getPendingDocuments(int page, int size) {
        return docRepo.findByStatus(MobilityDocumentStatus.PENDING, PageRequest.of(page, Math.min(size, 50)))
                .map(doc -> PendingDocumentReviewDto.builder()
                        .documentId(doc.getId())
                        .driverId(doc.getDriver().getId())
                        .driverName(driverDisplayName(doc.getDriver()))
                        .docType(doc.getDocType().name())
                        .fileUrl(doc.getFileUrl())
                        .uploadedAt(doc.getUploadedAt())
                        .build());
    }

    @Override
    public void reviewDocument(AppUser reviewer, Long driverId, Long documentId, ReviewDocumentRequest req) {
        MobilityDriverDocument doc = docRepo.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (!doc.getDriver().getId().equals(driverId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document does not belong to driver");
        }
        doc.setStatus(req.getApproved() ? MobilityDocumentStatus.APPROVED : MobilityDocumentStatus.REJECTED);
        doc.setRejectReason(req.getRejectReason());
        doc.setReviewedBy(reviewer);
        doc.setReviewedAt(Instant.now());
        docRepo.save(doc);

        // Promote driver to APPROVED if all docs are approved
        MobilityDriver driver = doc.getDriver();
        List<MobilityDriverDocument> allDocs = docRepo.findByDriverId(driver.getId());
        boolean allApproved = !allDocs.isEmpty()
                && allDocs.stream().allMatch(d -> d.getStatus() == MobilityDocumentStatus.APPROVED);
        if (allApproved && driver.getStatus() == MobilityDriverStatus.UNDER_REVIEW) {
            driver.setStatus(MobilityDriverStatus.APPROVED);
            driverRepo.save(driver);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MobilityTripDto> getActiveTripsOps(int page, int size) {
        return tripRepo.findAll(PageRequest.of(page, Math.min(size, 100))).map(this::toDto);
    }

    @Override
    public void suspendDriver(AppUser ops, Long driverId, String reason) {
        MobilityDriver driver = driverRepo.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
        driver.setStatus(MobilityDriverStatus.SUSPENDED);
        driver.setAvailability("OFFLINE");
        driverRepo.save(driver);
    }

    @Override
    public void reinstateDriver(AppUser ops, Long driverId) {
        MobilityDriver driver = driverRepo.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
        driver.setStatus(MobilityDriverStatus.APPROVED);
        driverRepo.save(driver);
    }

    // ── Dispatch (simple nearest-driver) ─────────────────────────

    private void dispatchToNearestDriver(MobilityTrip trip) {
        List<MobilityDriver> online = driverRepo.findOnlineApprovedDrivers();
        if (online.isEmpty()) {
            trip.setStatus(MobilityTripStatus.NO_DRIVER_FOUND);
            return;
        }
        // Pick driver with non-null location closest to pickup
        MobilityDriver best = online.stream()
                .filter(d -> d.getCurrentLat() != null && d.getCurrentLng() != null)
                .min((a, b) -> {
                    double da = matchingEngine.calculateDistanceKm(trip.getPickupLat(), trip.getPickupLng(), a.getCurrentLat(), a.getCurrentLng());
                    double db = matchingEngine.calculateDistanceKm(trip.getPickupLat(), trip.getPickupLng(), b.getCurrentLat(), b.getCurrentLng());
                    return Double.compare(da, db);
                })
                .orElse(online.get(0));  // fallback: first online driver if none have location

        MobilityDriverOffer offer = MobilityDriverOffer.builder()
                .trip(trip)
                .driver(best)
                .expiresAt(Instant.now().plusSeconds(OFFER_TTL_SECONDS))
                .build();
        offerRepo.save(offer);
        best.setAvailability("OFFER_PENDING");
        driverRepo.save(best);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private MobilityTrip findTripForUser(AppUser user, Long tripId) {
        MobilityTrip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));
        if (!trip.getPassenger().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your trip");
        }
        return trip;
    }

    private MobilityDriver requireDriver(AppUser user) {
        return driverRepo.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found"));
    }

    private List<MobilityTripStatus> activeStatuses() {
        return List.of(
                MobilityTripStatus.REQUESTED, MobilityTripStatus.SEARCHING,
                MobilityTripStatus.ASSIGNED, MobilityTripStatus.DRIVER_ARRIVING,
                MobilityTripStatus.ARRIVED, MobilityTripStatus.PICKED_UP,
                MobilityTripStatus.IN_PROGRESS);
    }

    private BigDecimal rateForType(MobilityServiceType type) {
        return switch (type) {
            case BIKE       -> RATE_PER_KM_BIKE;
            case AUTO       -> RATE_PER_KM_AUTO;
            case CAB        -> RATE_PER_KM_CAB;
            case CARPOOL, GROUP_RIDE -> RATE_PER_KM_POOL;
        };
    }

    private String generateRef() {
        return "MB-" + (System.currentTimeMillis() % 100000000L);
    }

    private String generatePin() {
        return String.format("%04d", (int)(Math.random() * 9000) + 1000);
    }

    private String driverDisplayName(MobilityDriver driver) {
        AppUser u = driver.getUser();
        if (u.getFullName() != null) return u.getFullName();
        return "Driver #" + driver.getId();
    }

    // ── Mappers ───────────────────────────────────────────────────

    private MobilityTripDto toDto(MobilityTrip t) {
        MobilityTripDriverDto driverDto = null;
        if (t.getDriver() != null) {
            MobilityDriver d = t.getDriver();
            AppUser du = d.getUser();
            List<MobilityVehicle> vehicles = vehicleRepo.findByDriverIdAndActiveTrue(d.getId());
            MobilityVehicle v = vehicles.isEmpty() ? null : vehicles.get(0);
            driverDto = MobilityTripDriverDto.builder()
                    .driverId(d.getId())
                    .driverName(driverDisplayName(d))
                    .phone(du.getPhone())
                    .vehicleModel(v != null ? v.getModel() : null)
                    .vehicleNumber(v != null ? v.getNumberPlate() : null)
                    .rating(d.getAvgRating())
                    .build();
        }
        return MobilityTripDto.builder()
                .id(t.getId())
                .tripRef(t.getTripRef())
                .serviceType(t.getServiceType().name())
                .status(t.getStatus().name())
                .pickup(MobilityTripDto.GeoPoint.builder()
                        .lat(t.getPickupLat()).lng(t.getPickupLng()).address(t.getPickupAddress()).build())
                .destination(MobilityTripDto.GeoPoint.builder()
                        .lat(t.getDestLat()).lng(t.getDestLng()).address(t.getDestAddress()).build())
                .pickupPin(t.getPickupPin())
                .driver(driverDto)
                .baseFare(t.getBaseFare())
                .distanceFare(t.getDistanceFare())
                .durationFare(t.getDurationFare())
                .taxes(t.getTaxes())
                .quotedTotal(t.getQuotedTotal())
                .finalFare(t.getFinalFare())
                .distanceKm(t.getDistanceKm() != null ? t.getDistanceKm().doubleValue() : 0)
                .etaMinutes(t.getEtaMinutes() != null ? t.getEtaMinutes() : 0)
                .paymentMethod(t.getPaymentMethod())
                .passengerRating(t.getPassengerRating())
                .requestedAt(t.getRequestedAt())
                .completedAt(t.getCompletedAt())
                .build();
    }

    private MobilityDriverDto toDriverDto(MobilityDriver d) {
        List<MobilityVehicleDto> vehicles = vehicleRepo.findByDriverIdAndActiveTrue(d.getId())
                .stream().map(this::toVehicleDto).collect(Collectors.toList());
        return MobilityDriverDto.builder()
                .id(d.getId())
                .userId(d.getUser().getId())
                .driverName(driverDisplayName(d))
                .status(d.getStatus().name())
                .availability(d.getAvailability())
                .avgRating(d.getAvgRating())
                .totalTrips(d.getTotalTrips())
                .createdAt(d.getCreatedAt())
                .vehicles(vehicles)
                .build();
    }

    private MobilityVehicleDto toVehicleDto(MobilityVehicle v) {
        return MobilityVehicleDto.builder()
                .id(v.getId())
                .vehicleType(v.getVehicleType().name())
                .make(v.getMake())
                .model(v.getModel())
                .numberPlate(v.getNumberPlate())
                .color(v.getColor())
                .year(v.getYear())
                .active(v.isActive())
                .build();
    }

    private MobilityDriverDocumentDto toDocDto(MobilityDriverDocument d) {
        return MobilityDriverDocumentDto.builder()
                .id(d.getId())
                .docType(d.getDocType().name())
                .fileUrl(d.getFileUrl())
                .status(d.getStatus().name())
                .rejectReason(d.getRejectReason())
                .uploadedAt(d.getUploadedAt())
                .reviewedAt(d.getReviewedAt())
                .build();
    }

    private MobilityCarpoolOfferDto toCarpoolDto(MobilityCarpoolOffer o) {
        AppUser u = o.getDriverUser();
        String name = u.getFullName() != null ? u.getFullName() : "Driver";
        return MobilityCarpoolOfferDto.builder()
                .id(o.getId())
                .driverName(name.trim())
                .rating(o.getDriverRating())
                .origin(o.getOrigin())
                .destination(o.getDestination())
                .departureTime(o.getDepartureTime())
                .totalSeats(o.getTotalSeats())
                .availableSeats(o.getAvailableSeats())
                .pricePerSeat(o.getPricePerSeat())
                .vehicleModel(o.getVehicleModel())
                .vehicleNumber(o.getVehicleNumber())
                .build();
    }
}
