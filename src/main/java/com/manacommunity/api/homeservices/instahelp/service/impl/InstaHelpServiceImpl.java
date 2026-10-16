package com.manacommunity.api.homeservices.instahelp.service.impl;

import com.manacommunity.api.homeservices.instahelp.dto.InstaHelpRequest;
import com.manacommunity.api.homeservices.instahelp.dto.InstaHelpResponse;
import com.manacommunity.api.homeservices.instahelp.dto.RateRequest;
import com.manacommunity.api.homeservices.instahelp.model.InstaHelpBooking;
import com.manacommunity.api.homeservices.instahelp.repository.InstaHelpBookingRepository;
import com.manacommunity.api.homeservices.instahelp.service.InstaHelpService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class InstaHelpServiceImpl implements InstaHelpService {

    private final InstaHelpBookingRepository bookingRepository;

    private static final Map<InstaHelpBooking.ServiceType, String> HELPER_MAP = Map.of(
            InstaHelpBooking.ServiceType.CLEANING,    "Lakshmi B.",
            InstaHelpBooking.ServiceType.COOKING,     "Priya K.",
            InstaHelpBooking.ServiceType.MAID,        "Sunita R.",
            InstaHelpBooking.ServiceType.IRONING,     "Meena D.",
            InstaHelpBooking.ServiceType.LAUNDRY,     "Kavitha S.",
            InstaHelpBooking.ServiceType.BABYSITTING, "Anitha R."
    );

    private static final Map<InstaHelpBooking.Duration, BigDecimal> PRICE_MAP = Map.of(
            InstaHelpBooking.Duration.ONE_HOUR,   BigDecimal.valueOf(149),
            InstaHelpBooking.Duration.TWO_HOURS,  BigDecimal.valueOf(249),
            InstaHelpBooking.Duration.FOUR_HOURS, BigDecimal.valueOf(449),
            InstaHelpBooking.Duration.FULL_DAY,   BigDecimal.valueOf(799)
    );

    @Override
    @Transactional
    public InstaHelpResponse create(Long communityId, Long residentId, InstaHelpRequest request) {
        InstaHelpBooking.ServiceType serviceType =
                InstaHelpBooking.ServiceType.valueOf(request.getServiceType().toUpperCase());
        InstaHelpBooking.Duration duration =
                InstaHelpBooking.Duration.valueOf(request.getDuration().toUpperCase());

        String bookingRef = generateBookingRef();

        int ordinal = serviceType.ordinal();
        double[] ratings = {4.8, 4.6, 4.7, 4.9, 4.6, 4.8};
        int[] etas = {8, 12, 6, 10, 14, 9};
        String[] distances = {"Inside your complex", "~0.4 km away"};

        double helperRating = ratings[ordinal % ratings.length];
        int helperEta = etas[ordinal % etas.length];
        String helperDistance = distances[ordinal % distances.length];

        int closureOtpInt = Math.abs(bookingRef.hashCode()) % 9000 + 1000;
        String closureOtp = String.valueOf(closureOtpInt);

        Community community = new Community();
        community.setId(communityId);

        AppUser resident = new AppUser();
        resident.setId(residentId);

        InstaHelpBooking booking = InstaHelpBooking.builder()
                .bookingRef(bookingRef)
                .community(community)
                .resident(resident)
                .serviceType(serviceType)
                .subtask(request.getSubtask())
                .duration(duration)
                .priceAmount(PRICE_MAP.get(duration))
                .notes(request.getNotes())
                .status(InstaHelpBooking.BookingStatus.MATCHED)
                .closureOtp(closureOtp)
                .helperName(HELPER_MAP.get(serviceType))
                .helperRating(helperRating)
                .helperEtaMinutes(helperEta)
                .helperDistanceLabel(helperDistance)
                .bookedAt(LocalDateTime.now())
                .matchedAt(LocalDateTime.now())
                .build();

        return toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstaHelpResponse> getMyBookings(Long residentId) {
        return bookingRepository.findByResidentIdOrderByBookedAtDesc(residentId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public InstaHelpResponse startSession(String bookingRef, Long residentId) {
        InstaHelpBooking booking = findAndVerify(bookingRef, residentId);
        if (booking.getStatus() != InstaHelpBooking.BookingStatus.MATCHED) {
            throw new RuntimeException("Booking must be in MATCHED status to start. Current: " + booking.getStatus());
        }
        booking.setStatus(InstaHelpBooking.BookingStatus.IN_PROGRESS);
        booking.setStartedAt(LocalDateTime.now());
        return toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public InstaHelpResponse completeSession(String bookingRef, Long residentId) {
        InstaHelpBooking booking = findAndVerify(bookingRef, residentId);
        if (booking.getStatus() != InstaHelpBooking.BookingStatus.IN_PROGRESS) {
            throw new RuntimeException("Booking must be IN_PROGRESS to complete. Current: " + booking.getStatus());
        }
        booking.setStatus(InstaHelpBooking.BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());
        return toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public InstaHelpResponse cancel(String bookingRef, Long residentId) {
        InstaHelpBooking booking = findAndVerify(bookingRef, residentId);
        if (booking.getStatus() == InstaHelpBooking.BookingStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel a completed booking.");
        }
        booking.setStatus(InstaHelpBooking.BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        return toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public InstaHelpResponse rate(String bookingRef, Long residentId, RateRequest req) {
        InstaHelpBooking booking = findAndVerify(bookingRef, residentId);
        if (booking.getStatus() != InstaHelpBooking.BookingStatus.COMPLETED) {
            throw new RuntimeException("Can only rate a completed booking. Current: " + booking.getStatus());
        }
        booking.setResidentRating(req.getRating());
        booking.setResidentFeedback(req.getFeedback());
        return toResponse(bookingRepository.save(booking));
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private InstaHelpBooking findAndVerify(String bookingRef, Long residentId) {
        InstaHelpBooking booking = bookingRepository.findByBookingRef(bookingRef)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingRef));
        if (!booking.getResident().getId().equals(residentId)) {
            throw new RuntimeException("Access denied for booking: " + bookingRef);
        }
        return booking;
    }

    private String generateBookingRef() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE).substring(2);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return "IH-" + datePart + "-" + sb;
    }

    private InstaHelpResponse toResponse(InstaHelpBooking b) {
        return InstaHelpResponse.builder()
                .id(b.getId())
                .bookingRef(b.getBookingRef())
                .serviceType(b.getServiceType() != null ? b.getServiceType().name() : null)
                .subtask(b.getSubtask())
                .duration(b.getDuration() != null ? b.getDuration().name() : null)
                .priceAmount(b.getPriceAmount())
                .notes(b.getNotes())
                .status(b.getStatus() != null ? b.getStatus().name() : null)
                .closureOtp(b.getClosureOtp())
                .helperName(b.getHelperName())
                .helperRating(b.getHelperRating())
                .helperEtaMinutes(b.getHelperEtaMinutes())
                .helperDistanceLabel(b.getHelperDistanceLabel())
                .residentRating(b.getResidentRating())
                .residentFeedback(b.getResidentFeedback())
                .bookedAt(b.getBookedAt() != null ? b.getBookedAt().toString() : null)
                .matchedAt(b.getMatchedAt() != null ? b.getMatchedAt().toString() : null)
                .startedAt(b.getStartedAt() != null ? b.getStartedAt().toString() : null)
                .completedAt(b.getCompletedAt() != null ? b.getCompletedAt().toString() : null)
                .cancelledAt(b.getCancelledAt() != null ? b.getCancelledAt().toString() : null)
                .build();
    }
}
