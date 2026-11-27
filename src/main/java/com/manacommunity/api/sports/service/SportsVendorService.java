package com.manacommunity.api.sports.service;

import com.manacommunity.api.sports.dto.SportsVendorDtos.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface SportsVendorService {

    List<FacilityResponse> getFacilities(String sport, String city, Long vendorId);

    FacilityResponse getFacilityById(Long id);

    FacilityResponse createFacility(FacilityResponse req);

    FacilityResponse updateFacility(Long id, FacilityResponse req);

    List<CourtResponse> getCourts(Long facilityId);

    CourtResponse createCourt(Long facilityId, CourtResponse req);

    List<SlotResponse> getSlots(Long courtId, LocalDate date);

    SlotResponse toggleSlotStatus(Long courtId, LocalDate date, String startTime, String targetStatus);

    BookingResponse reserveSlot(Long residentId, CreateBookingRequest request);

    BookingResponse checkInResident(Long bookingId, String qrToken, Long staffUserId);

    List<BookingResponse> getBookings(Long venueId, LocalDate date);

    List<CoachResponse> getCoaches(Long vendorId);

    List<BatchResponse> getBatches(Long venueId);

    List<OfficialResponse> getOfficials(String sport);

    Map<String, Object> getDashboardStats(Long vendorId);
}
