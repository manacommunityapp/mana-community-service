package com.manacommunity.api.controller;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.service.VehicleCareService;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/vehicle-care")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class VehicleCareController {

    private final LoggedInUserService loggedInUserService;
    private final VehicleCareService vehicleCareService;

    // ── Vehicles ─────────────────────────────────────────────────

    @GetMapping("/vehicles")
    public ResponseEntity<List<VehicleProfileResponse>> getVehicles(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleCareService.getVehicles(user));
    }

    @PostMapping("/vehicles")
    public ResponseEntity<VehicleProfileResponse> createVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateVehicleProfileRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleCareService.createVehicle(user, request));
    }

    @PutMapping("/vehicles/{vehicleId}")
    public ResponseEntity<VehicleProfileResponse> updateVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long vehicleId,
            @Valid @RequestBody CreateVehicleProfileRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleCareService.updateVehicle(user, vehicleId, request));
    }

    @DeleteMapping("/vehicles/{vehicleId}")
    public ResponseEntity<Void> deleteVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long vehicleId) {
        AppUser user = loggedInUserService.resolve(principal);
        vehicleCareService.deleteVehicle(user, vehicleId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/vehicles/{vehicleId}/default")
    public ResponseEntity<VehicleProfileResponse> setDefaultVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long vehicleId) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleCareService.setDefaultVehicle(user, vehicleId));
    }

    // ── Catalog ──────────────────────────────────────────────────

    @GetMapping("/catalog")
    public ResponseEntity<List<VehicleServiceCatalogResponse>> getCatalog(
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleCareService.getCatalog());
    }

    @GetMapping("/catalog/{serviceId}/addons")
    public ResponseEntity<List<VehicleServiceAddonResponse>> getAddons(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long serviceId) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleCareService.getAddons(serviceId));
    }

    // ── Slots ────────────────────────────────────────────────────

    @GetMapping("/slots")
    public ResponseEntity<List<VehicleServiceSlotResponse>> getSlots(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleCareService.getAvailableSlots(user, from, to));
    }

    // ── Bookings ─────────────────────────────────────────────────

    @PostMapping("/bookings")
    public ResponseEntity<VehicleCareBookingResponse> createBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateVehicleCareBookingRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleCareService.createBooking(user, request));
    }

    @GetMapping("/bookings")
    public ResponseEntity<Page<VehicleCareBookingResponse>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        int safeSize = Math.min(Math.max(size, 1), 50);
        return ResponseEntity.ok(vehicleCareService.getMyBookings(user, filter, Math.max(page, 0), safeSize));
    }

    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<VehicleCareBookingResponse> getBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleCareService.getBooking(user, bookingId));
    }

    @PatchMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId) {
        AppUser user = loggedInUserService.resolve(principal);
        vehicleCareService.cancelBooking(user, bookingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bookings/{bookingId}/confirm-payment")
    public ResponseEntity<VehicleCareBookingResponse> confirmPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId,
            @RequestParam(required = false) String paymentReference) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleCareService.confirmPayment(user, bookingId, paymentReference));
    }

    @PostMapping("/bookings/{bookingId}/review")
    public ResponseEntity<VehicleCareBookingResponse> submitReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId,
            @Valid @RequestBody SubmitVehicleReviewRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleCareService.submitReview(user, bookingId, request));
    }

    // ── Plans ────────────────────────────────────────────────────

    @GetMapping("/plans/catalog")
    public ResponseEntity<List<VehicleCarePlanCatalogResponse>> getPlanCatalog(
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleCareService.getPlanCatalog());
    }

    @GetMapping("/plans")
    public ResponseEntity<List<VehicleCarePlanResponse>> getMyPlans(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleCareService.getMyPlans(user));
    }

    @PostMapping("/plans/subscribe")
    public ResponseEntity<VehicleCarePlanResponse> subscribePlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubscribeVehiclePlanRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleCareService.subscribePlan(user, request));
    }

    @PatchMapping("/plans/{planId}/status")
    public ResponseEntity<VehicleCarePlanResponse> updatePlanStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long planId,
            @RequestParam String status) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleCareService.updatePlanStatus(user, planId, status));
    }

    // ── Vendor / Technician ──────────────────────────────────────

    @GetMapping("/vendor/jobs")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMUNITY_ADMIN', 'SUPER_ADMIN', 'TECHNICIAN', 'VENDOR') or hasAuthority('Manage Vehicle Care')")
    public ResponseEntity<List<VehicleCareBookingResponse>> getMyJobs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "active") String filter) {
        AppUser technician = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleCareService.getMyJobs(technician, filter));
    }

    @PatchMapping("/vendor/jobs/{bookingId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMUNITY_ADMIN', 'SUPER_ADMIN', 'TECHNICIAN', 'VENDOR') or hasAuthority('Manage Vehicle Care')")
    public ResponseEntity<VehicleCareBookingResponse> updateJobStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId,
            @RequestParam String status) {
        AppUser technician = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleCareService.updateJobStatus(technician, bookingId, status));
    }

    // ── Admin Endpoints ──────────────────────────────────────────

    @GetMapping("/admin/bookings")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMUNITY_ADMIN', 'SUPER_ADMIN') or hasAuthority('Manage Vehicle Care')")
    public ResponseEntity<Page<VehicleCareBookingResponse>> getCommunityBookings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AppUser admin = loggedInUserService.resolve(principal);
        int safeSize = Math.min(Math.max(size, 1), 50);
        return ResponseEntity.ok(vehicleCareService.getCommunityBookings(admin.getCommunity().getId(), status, Math.max(page, 0), safeSize));
    }

    @PatchMapping("/admin/bookings/{bookingId}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMUNITY_ADMIN', 'SUPER_ADMIN') or hasAuthority('Manage Vehicle Care')")
    public ResponseEntity<VehicleCareBookingResponse> assignTechnician(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId,
            @RequestParam Long technicianId) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vehicleCareService.assignTechnician(bookingId, technicianId));
    }
}
