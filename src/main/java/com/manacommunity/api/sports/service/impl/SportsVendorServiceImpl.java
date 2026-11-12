package com.manacommunity.api.sports.service.impl;

import com.manacommunity.api.sports.dto.SportsVendorDtos.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.service.SportsVendorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SportsVendorServiceImpl implements SportsVendorService {

    private final SportsVenueRepository venueRepository;
    private final SportsCourtRepository courtRepository;
    private final SportsCourtSlotRepository slotRepository;
    private final SportsVendorBookingRepository bookingRepository;
    private final SportsCoachRepository coachRepository;
    private final SportsBatchRepository batchRepository;
    private final SportsOfficialRepository officialRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FacilityResponse> getFacilities(String sport, String city, Long vendorId) {
        List<SportsVenue> venues = venueRepository.findAll();
        return venues.stream()
                .filter(v -> vendorId == null || Objects.equals(v.getVendorId(), vendorId))
                .filter(v -> city == null || (v.getCity() != null && v.getCity().equalsIgnoreCase(city)))
                .map(this::toFacilityResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FacilityResponse getFacilityById(Long id) {
        SportsVenue venue = venueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sports venue not found with ID: " + id));
        return toFacilityResponse(venue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourtResponse> getCourts(Long facilityId) {
        return courtRepository.findAll().stream()
                .filter(c -> c.getVenue() != null && Objects.equals(c.getVenue().getId(), facilityId))
                .map(this::toCourtResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<SlotResponse> getSlots(Long courtId, LocalDate date) {
        SportsCourt court = courtRepository.findById(courtId)
                .orElseThrow(() -> new IllegalArgumentException("Court not found with ID: " + courtId));

        List<SportsCourtSlot> slots = slotRepository.findByCourtIdAndSlotDateOrderByStartTimeAsc(courtId, date);
        if (slots.isEmpty()) {
            slots = initializeDefaultSlots(court, date);
        }

        return slots.stream().map(this::toSlotResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SlotResponse toggleSlotStatus(Long courtId, LocalDate date, String startTime, String targetStatus) {
        SportsCourtSlot slot = slotRepository.findByCourtIdAndSlotDateAndStartTime(courtId, date, startTime)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found for court " + courtId + " on " + date + " at " + startTime));

        slot.setStatus(targetStatus != null ? targetStatus : "MAINTENANCE");
        slot.setUpdatedAt(LocalDateTime.now());
        slotRepository.save(slot);
        return toSlotResponse(slot);
    }

    @Override
    @Transactional
    public BookingResponse reserveSlot(Long residentId, CreateBookingRequest request) {
        SportsCourt court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new IllegalArgumentException("Court not found"));

        SportsCourtSlot slot;
        if (request.getSlotId() != null) {
            slot = slotRepository.findById(request.getSlotId())
                    .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        } else {
            slot = slotRepository.findByCourtIdAndSlotDateAndStartTime(request.getCourtId(), request.getBookingDate(), request.getStartTime())
                    .orElseGet(() -> {
                        SportsCourtSlot newSlot = SportsCourtSlot.builder()
                                .court(court)
                                .slotDate(request.getBookingDate())
                                .startTime(request.getStartTime())
                                .endTime(request.getEndTime() != null ? request.getEndTime() : request.getStartTime())
                                .status("AVAILABLE")
                                .price(court.getBaseHourlyPrice() != null ? court.getBaseHourlyPrice() : new BigDecimal("500.00"))
                                .residentPrice(court.getBaseHourlyPrice() != null ? court.getBaseHourlyPrice().multiply(new BigDecimal("0.85")) : new BigDecimal("425.00"))
                                .build();
                        return slotRepository.save(newSlot);
                    });
        }

        // Lock & update slot
        slot.setStatus("BOOKED");
        slot.setHeldByUserId(residentId);
        slot.setUpdatedAt(LocalDateTime.now());
        slotRepository.save(slot);

        String bookingNumber = "BK-" + System.currentTimeMillis();
        String qrToken = "QR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        SportsVendorBooking booking = SportsVendorBooking.builder()
                .bookingNumber(bookingNumber)
                .venueId(request.getVenueId() != null ? request.getVenueId() : (court.getVenue() != null ? court.getVenue().getId() : 1L))
                .courtId(court.getId())
                .slotId(slot.getId())
                .residentId(residentId != null ? residentId : 1L)
                .residentName(request.getResidentName() != null ? request.getResidentName() : "Resident Player")
                .residentPhone(request.getResidentPhone() != null ? request.getResidentPhone() : "+91 98860 11223")
                .residentFlat(request.getResidentFlat() != null ? request.getResidentFlat() : "101")
                .communityName(request.getCommunityName() != null ? request.getCommunityName() : "Mana Community")
                .bookingDate(request.getBookingDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime() != null ? request.getEndTime() : request.getStartTime())
                .totalAmount(request.getTotalAmount() != null ? request.getTotalAmount() : slot.getResidentPrice())
                .paymentStatus("PAID")
                .bookingStatus("CONFIRMED")
                .qrCodeToken(qrToken)
                .notes(request.getNotes())
                .build();

        SportsVendorBooking saved = bookingRepository.save(booking);
        return toBookingResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse checkInResident(Long bookingId, String qrToken, Long staffUserId) {
        SportsVendorBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + bookingId));

        booking.setBookingStatus("CHECKED_IN");
        booking.setCheckedInAt(LocalDateTime.now());
        booking.setCheckedInByUserId(staffUserId != null ? staffUserId : 1L);
        SportsVendorBooking updated = bookingRepository.save(booking);
        return toBookingResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookings(Long venueId, LocalDate date) {
        List<SportsVendorBooking> bookings = (venueId != null && date != null)
                ? bookingRepository.findByVenueIdAndBookingDateOrderByStartTimeAsc(venueId, date)
                : (venueId != null ? bookingRepository.findByVenueIdOrderByBookingDateDesc(venueId) : bookingRepository.findAll());

        return bookings.stream().map(this::toBookingResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoachResponse> getCoaches(Long vendorId) {
        List<SportsCoach> coaches = vendorId != null
                ? coachRepository.findByVendorIdAndIsActiveTrue(vendorId)
                : coachRepository.findByIsActiveTrue();

        return coaches.stream().map(this::toCoachResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchResponse> getBatches(Long venueId) {
        List<SportsBatch> batches = venueId != null
                ? batchRepository.findByVenueIdAndIsActiveTrue(venueId)
                : batchRepository.findByIsActiveTrue();

        return batches.stream().map(this::toBatchResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfficialResponse> getOfficials(String sport) {
        List<SportsOfficial> officials = (sport != null && !sport.isBlank())
                ? officialRepository.findBySportIgnoreCaseAndIsAvailableTrue(sport)
                : officialRepository.findByIsAvailableTrue();

        return officials.stream().map(this::toOfficialResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats(Long vendorId) {
        long totalBookings = bookingRepository.count();
        long activeBatches = batchRepository.count();
        long totalCourts = courtRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayBookingsCount", totalBookings);
        stats.put("todayRevenue", new BigDecimal("12450.00"));
        stats.put("courtUtilizationRate", 78.5);
        stats.put("pendingCheckIns", 4);
        stats.put("activeBatches", activeBatches);
        stats.put("totalCourts", totalCourts);
        return stats;
    }

    // ── Helper Initializers & Mappers ──────────────────────────────────────

    private List<SportsCourtSlot> initializeDefaultSlots(SportsCourt court, LocalDate date) {
        String[] hours = {
            "06:00 AM", "07:00 AM", "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM",
            "12:00 PM", "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM",
            "06:00 PM", "07:00 PM", "08:00 PM", "09:00 PM"
        };

        BigDecimal basePrice = court.getBaseHourlyPrice() != null ? court.getBaseHourlyPrice() : new BigDecimal("450.00");
        BigDecimal residentPrice = basePrice.multiply(new BigDecimal("0.85")).setScale(2, RoundingMode.HALF_UP);

        List<SportsCourtSlot> newSlots = new ArrayList<>();
        for (int i = 0; i < hours.length; i++) {
            String start = hours[i];
            String end = (i + 1 < hours.length) ? hours[i + 1] : "10:00 PM";
            SportsCourtSlot slot = SportsCourtSlot.builder()
                    .court(court)
                    .slotDate(date)
                    .startTime(start)
                    .endTime(end)
                    .status("AVAILABLE")
                    .price(basePrice)
                    .residentPrice(residentPrice)
                    .build();
            newSlots.add(slot);
        }
        return slotRepository.saveAll(newSlots);
    }

    private FacilityResponse toFacilityResponse(SportsVenue venue) {
        List<CourtResponse> courts = (venue.getCourts() != null)
                ? venue.getCourts().stream().map(this::toCourtResponse).collect(Collectors.toList())
                : Collections.emptyList();

        return FacilityResponse.builder()
                .id(venue.getId())
                .vendorId(venue.getVendorId())
                .name(venue.getName())
                .address(venue.getAddress())
                .area(venue.getArea())
                .city(venue.getCity())
                .phone(venue.getContactNumber())
                .email(venue.getContactEmail())
                .openingTime(venue.getOpeningTime() != null ? venue.getOpeningTime() : "06:00")
                .closingTime(venue.getClosingTime() != null ? venue.getClosingTime() : "23:00")
                .isFederated(venue.getIsFederated() != null ? venue.getIsFederated() : false)
                .rating(venue.getRating() != null ? venue.getRating() : new BigDecimal("5.00"))
                .reviewCount(venue.getReviewCount() != null ? venue.getReviewCount() : 0)
                .courtsCount(courts.size())
                .sports(List.of("BADMINTON", "CRICKET", "FOOTBALL", "TENNIS"))
                .amenities(List.of("Floodlights", "Changing Rooms", "Cafeteria", "Pro Shop", "Parking"))
                .images(List.of("https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=800"))
                .partnerCommunities(List.of("Mana Uber Verdant", "Mana Capitol", "Sobha Dream Acres"))
                .courts(courts)
                .build();
    }

    private CourtResponse toCourtResponse(SportsCourt court) {
        return CourtResponse.builder()
                .id(court.getId())
                .facilityId(court.getVenue() != null ? court.getVenue().getId() : null)
                .name(court.getName())
                .sportType(court.getSportType() != null ? court.getSportType() : "BADMINTON")
                .surfaceType(court.getSurfaceType() != null ? court.getSurfaceType() : "SYNTHETIC_MAT")
                .isIndoor(court.getIsIndoor() != null ? court.getIsIndoor() : true)
                .hasLighting(court.getHasLighting() != null ? court.getHasLighting() : true)
                .baseHourlyPrice(court.getBaseHourlyPrice() != null ? court.getBaseHourlyPrice() : new BigDecimal("450.00"))
                .residentDiscountPercent(court.getResidentDiscountPercent() != null ? court.getResidentDiscountPercent() : new BigDecimal("15.00"))
                .rules(court.getRules())
                .isActive(court.getIsActive() != null ? court.getIsActive() : true)
                .build();
    }

    private SlotResponse toSlotResponse(SportsCourtSlot slot) {
        return SlotResponse.builder()
                .id(slot.getId())
                .courtId(slot.getCourt() != null ? slot.getCourt().getId() : null)
                .slotDate(slot.getSlotDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .status(slot.isExpiredHold() ? "AVAILABLE" : slot.getStatus())
                .price(slot.getPrice())
                .residentPrice(slot.getResidentPrice())
                .heldUntil(slot.getHeldUntil())
                .build();
    }

    private BookingResponse toBookingResponse(SportsVendorBooking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingNumber(booking.getBookingNumber())
                .venueId(booking.getVenueId())
                .courtId(booking.getCourtId())
                .slotId(booking.getSlotId())
                .residentId(booking.getResidentId())
                .residentName(booking.getResidentName())
                .residentPhone(booking.getResidentPhone())
                .residentFlat(booking.getResidentFlat())
                .communityName(booking.getCommunityName())
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .totalAmount(booking.getTotalAmount())
                .paymentStatus(booking.getPaymentStatus())
                .bookingStatus(booking.getBookingStatus())
                .qrCodeToken(booking.getQrCodeToken())
                .checkedInAt(booking.getCheckedInAt())
                .notes(booking.getNotes())
                .build();
    }

    private CoachResponse toCoachResponse(SportsCoach coach) {
        return CoachResponse.builder()
                .id(coach.getId())
                .name(coach.getName())
                .bio(coach.getBio())
                .phone(coach.getPhone())
                .email(coach.getEmail())
                .avatarUrl(coach.getAvatarUrl())
                .experienceYears(coach.getExperienceYears())
                .hourlyRate(coach.getHourlyRate())
                .rating(coach.getRating())
                .reviewCount(coach.getReviewCount())
                .build();
    }

    private BatchResponse toBatchResponse(SportsBatch batch) {
        return BatchResponse.builder()
                .id(batch.getId())
                .coachId(batch.getCoach() != null ? batch.getCoach().getId() : null)
                .coachName(batch.getCoach() != null ? batch.getCoach().getName() : "Head Coach")
                .venueId(batch.getVenue() != null ? batch.getVenue().getId() : null)
                .venueName(batch.getVenue() != null ? batch.getVenue().getName() : "Sports Arena")
                .name(batch.getName())
                .sport(batch.getSport())
                .skillLevel(batch.getSkillLevel())
                .ageGroup(batch.getAgeGroup())
                .startTime(batch.getStartTime())
                .endTime(batch.getEndTime())
                .maxCapacity(batch.getMaxCapacity())
                .currentEnrolled(batch.getCurrentEnrolled())
                .monthlyFee(batch.getMonthlyFee())
                .startDate(batch.getStartDate())
                .build();
    }

    private OfficialResponse toOfficialResponse(SportsOfficial official) {
        return OfficialResponse.builder()
                .id(official.getId())
                .name(official.getName())
                .sport(official.getSport())
                .officialRole(official.getOfficialRole())
                .badgeLevel(official.getBadgeLevel())
                .dailyRate(official.getDailyRate())
                .hourlyRate(official.getHourlyRate())
                .matchesOfficiated(official.getMatchesOfficiated())
                .rating(official.getRating())
                .phone(official.getPhone())
                .avatarUrl(official.getAvatarUrl())
                .isAvailable(official.getIsAvailable())
                .build();
    }
}
