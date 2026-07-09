package com.manacommunity.api.booking.service;

import com.manacommunity.api.booking.dto.*;
import com.manacommunity.api.booking.entity.Amenity;
import com.manacommunity.api.booking.entity.AmenityBooking;
import com.manacommunity.api.booking.repository.AmenityBookingRepository;
import com.manacommunity.api.booking.repository.AmenityRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final AmenityRepository amenityRepo;
    private final AmenityBookingRepository bookingRepo;

    // ── Amenity CRUD ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AmenityResponse> getAmenities(Long communityId) {
        return amenityRepo.findByCommunityIdAndActiveTrueOrderByNameAsc(communityId)
                .stream().map(this::toAmenityResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AmenityResponse> getAllAmenities(Long communityId) {
        return amenityRepo.findByCommunityIdOrderByNameAsc(communityId)
                .stream().map(this::toAmenityResponse).toList();
    }

    @Transactional
    public AmenityResponse createAmenity(AmenityRequest req, Community community) {
        Amenity amenity = Amenity.builder()
                .name(req.getName())
                .description(req.getDescription())
                .type(parseEnum(Amenity.AmenityType.class, req.getType()))
                .maxCapacity(req.getMaxCapacity())
                .slotDurationMinutes(req.getSlotDurationMinutes() != null ? req.getSlotDurationMinutes() : 60)
                .community(community)
                .build();

        if (req.getOpenTime() != null && !req.getOpenTime().isBlank()) {
            amenity.setOpenTime(LocalTime.parse(req.getOpenTime()));
        }
        if (req.getCloseTime() != null && !req.getCloseTime().isBlank()) {
            amenity.setCloseTime(LocalTime.parse(req.getCloseTime()));
        }

        return toAmenityResponse(amenityRepo.save(amenity));
    }

    @Transactional
    public AmenityResponse updateAmenity(Long id, AmenityRequest req) {
        Amenity amenity = amenityRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Amenity not found: " + id));
        amenity.setName(req.getName());
        amenity.setDescription(req.getDescription());
        if (req.getType() != null) {
            Amenity.AmenityType t = parseEnum(Amenity.AmenityType.class, req.getType());
            if (t != null) amenity.setType(t);
        }
        amenity.setMaxCapacity(req.getMaxCapacity());
        if (req.getSlotDurationMinutes() != null) amenity.setSlotDurationMinutes(req.getSlotDurationMinutes());
        if (req.getOpenTime() != null) amenity.setOpenTime(LocalTime.parse(req.getOpenTime()));
        if (req.getCloseTime() != null) amenity.setCloseTime(LocalTime.parse(req.getCloseTime()));
        return toAmenityResponse(amenityRepo.save(amenity));
    }

    // ── Booking CRUD ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsForDate(Long amenityId, LocalDate date) {
        return bookingRepo.findByAmenityIdAndBookingDateAndStatusOrderByStartTimeAsc(
                        amenityId, date, AmenityBooking.BookingStatus.CONFIRMED)
                .stream().map(this::toBookingResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(Long userId) {
        return bookingRepo.findByBookedByIdOrderByBookingDateDescStartTimeDesc(userId)
                .stream().map(this::toBookingResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getTodaysBookings(Long communityId) {
        return bookingRepo.findTodaysByCommunity(communityId, LocalDate.now())
                .stream().map(this::toBookingResponse).toList();
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest req, AppUser user) {
        Amenity amenity = amenityRepo.findById(req.getAmenityId())
                .orElseThrow(() -> new IllegalArgumentException("Amenity not found: " + req.getAmenityId()));

        LocalDate date = LocalDate.parse(req.getBookingDate());
        LocalTime start = LocalTime.parse(req.getStartTime());
        LocalTime end = LocalTime.parse(req.getEndTime());

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        List<AmenityBooking> conflicts = bookingRepo.findConflicting(amenity.getId(), date, start, end);
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Time slot conflicts with an existing booking");
        }

        AmenityBooking booking = AmenityBooking.builder()
                .amenity(amenity)
                .bookedBy(user)
                .bookingDate(date)
                .startTime(start)
                .endTime(end)
                .purpose(req.getPurpose())
                .build();

        return toBookingResponse(bookingRepo.save(booking));
    }

    @Transactional
    public void cancelBooking(Long id, Long userId) {
        AmenityBooking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));
        if (!booking.getBookedBy().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only cancel your own bookings");
        }
        booking.setStatus(AmenityBooking.BookingStatus.CANCELLED);
        bookingRepo.save(booking);
    }

    @Transactional
    public void adminCancel(Long id) {
        AmenityBooking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));
        booking.setStatus(AmenityBooking.BookingStatus.CANCELLED);
        bookingRepo.save(booking);
    }

    // ── Mappers ─────────────────────────────────────────────────────

    private AmenityResponse toAmenityResponse(Amenity a) {
        return AmenityResponse.builder()
                .id(a.getId())
                .name(a.getName())
                .description(a.getDescription())
                .type(a.getType().name())
                .maxCapacity(a.getMaxCapacity())
                .openTime(a.getOpenTime() != null ? a.getOpenTime().toString() : null)
                .closeTime(a.getCloseTime() != null ? a.getCloseTime().toString() : null)
                .slotDurationMinutes(a.getSlotDurationMinutes())
                .active(a.isActive())
                .communityId(a.getCommunity() != null ? a.getCommunity().getId() : null)
                .build();
    }

    private BookingResponse toBookingResponse(AmenityBooking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .amenityId(b.getAmenity().getId())
                .amenityName(b.getAmenity().getName())
                .amenityType(b.getAmenity().getType().name())
                .bookingDate(b.getBookingDate().toString())
                .startTime(b.getStartTime().toString())
                .endTime(b.getEndTime().toString())
                .purpose(b.getPurpose())
                .status(b.getStatus().name())
                .bookedById(b.getBookedBy().getId())
                .bookedByName(b.getBookedBy().getFullName())
                .createdAt(formatDt(b.getCreatedAt()))
                .build();
    }

    private String formatDt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null || value.isBlank()) return null;
        try { return Enum.valueOf(enumClass, value); }
        catch (IllegalArgumentException e) { return null; }
    }
}
