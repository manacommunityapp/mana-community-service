package com.manacommunity.api.service;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface VehicleCareService {

    // Vehicles
    List<VehicleProfileResponse> getVehicles(AppUser user);
    VehicleProfileResponse createVehicle(AppUser user, CreateVehicleProfileRequest request);
    VehicleProfileResponse updateVehicle(AppUser user, Long vehicleId, CreateVehicleProfileRequest request);
    void deleteVehicle(AppUser user, Long vehicleId);
    VehicleProfileResponse setDefaultVehicle(AppUser user, Long vehicleId);

    // Catalog
    List<VehicleServiceCatalogResponse> getCatalog();
    List<VehicleServiceAddonResponse> getAddons(Long serviceId);

    // Slots
    List<VehicleServiceSlotResponse> getAvailableSlots(AppUser user, LocalDate from, LocalDate to);

    // Bookings
    VehicleCareBookingResponse createBooking(AppUser user, CreateVehicleCareBookingRequest request);
    Page<VehicleCareBookingResponse> getMyBookings(AppUser user, String filter, int page, int size);
    VehicleCareBookingResponse getBooking(AppUser user, Long bookingId);
    void cancelBooking(AppUser user, Long bookingId);
    VehicleCareBookingResponse submitReview(AppUser user, Long bookingId, SubmitVehicleReviewRequest request);

    // Plans
    List<VehicleCarePlanCatalogResponse> getPlanCatalog();
    List<VehicleCarePlanResponse> getMyPlans(AppUser user);
    VehicleCarePlanResponse subscribePlan(AppUser user, SubscribeVehiclePlanRequest request);
    VehicleCarePlanResponse updatePlanStatus(AppUser user, Long planId, String newStatus);

    // Vendor / Technician jobs
    List<VehicleCareBookingResponse> getMyJobs(AppUser technician, String filter);
    VehicleCareBookingResponse updateJobStatus(AppUser technician, Long bookingId, String newStatus);

    // Payment & Lifecycle
    VehicleCareBookingResponse confirmPayment(AppUser user, Long bookingId, String paymentReference);

    // Admin & Dispatch
    VehicleCareBookingResponse assignTechnician(Long bookingId, Long technicianId);
    Page<VehicleCareBookingResponse> getCommunityBookings(Long communityId, String status, int page, int size);
}

