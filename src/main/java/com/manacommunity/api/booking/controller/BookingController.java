package com.manacommunity.api.booking.controller;

import com.manacommunity.api.booking.dto.*;
import com.manacommunity.api.booking.entity.enums.BookingStatus;
import com.manacommunity.api.booking.service.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resource-booking")
@RequiredArgsConstructor
public class BookingController {

    private final ResourceCategoryService categoryService;
    private final ResourceService resourceService;
    private final ResourceBookingService bookingService;
    private final WaitlistService waitlistService;
    private final BookingRuleService ruleService;
    private final MaintenanceService maintenanceService;
    private final CouponService couponService;
    private final BookingDashboardService dashboardService;
    private final LoggedInUserService loggedInUserService;

    // ── Resource Categories ──────────────────────────────────────

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('View Resource Booking')")
    public ResponseEntity<List<ResourceCategoryResponse>> getCategories(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(categoryService.getCategories(communityId));
    }

    @GetMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('View Resource Booking')")
    public ResponseEntity<ResourceCategoryResponse> getCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(categoryService.getCategory(id));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('Manage Resource Categories')")
    public ResponseEntity<ResourceCategoryResponse> createCategory(
            @Valid @RequestBody ResourceCategoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(request, user.getCommunity(), user));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('Manage Resource Categories')")
    public ResponseEntity<ResourceCategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody ResourceCategoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('Manage Resource Categories')")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // ── Resources ────────────────────────────────────────────────

    @GetMapping("/resources")
    @PreAuthorize("hasAuthority('View Resource Booking')")
    public ResponseEntity<List<ResourceResponse>> getResources(
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(resourceService.getResources(communityId, categoryId));
    }

    @GetMapping("/resources/{id}")
    @PreAuthorize("hasAuthority('View Resource Booking')")
    public ResponseEntity<ResourceResponse> getResource(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(resourceService.getResource(id));
    }

    @PostMapping("/resources")
    @PreAuthorize("hasAuthority('Manage Resources')")
    public ResponseEntity<ResourceResponse> createResource(
            @Valid @RequestBody ResourceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resourceService.createResource(request, user.getCommunity(), user));
    }

    @PutMapping("/resources/{id}")
    @PreAuthorize("hasAuthority('Manage Resources')")
    public ResponseEntity<ResourceResponse> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody ResourceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(resourceService.updateResource(id, request));
    }

    @DeleteMapping("/resources/{id}")
    @PreAuthorize("hasAuthority('Manage Resources')")
    public ResponseEntity<Void> deleteResource(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }

    // ── Slots & Availability ─────────────────────────────────────

    @GetMapping("/resources/{resourceId}/slots")
    @PreAuthorize("hasAuthority('View Resource Booking')")
    public ResponseEntity<List<SlotResponse>> getSlots(
            @PathVariable Long resourceId,
            @RequestParam String date,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(bookingService.generateSlots(resourceId, LocalDate.parse(date)));
    }

    @GetMapping("/resources/{resourceId}/bookings")
    @PreAuthorize("hasAuthority('View Resource Booking')")
    public ResponseEntity<List<ResourceBookingResponse>> getBookingsByResource(
            @PathVariable Long resourceId,
            @RequestParam String date,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(bookingService.getBookingsByResource(resourceId, LocalDate.parse(date)));
    }

    // ── Bookings ─────────────────────────────────────────────────

    @PostMapping("/bookings")
    @PreAuthorize("hasAuthority('View Resource Booking')")
    public ResponseEntity<ResourceBookingResponse> createBooking(
            @Valid @RequestBody ResourceBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(request, user, user.getCommunity()));
    }

    @GetMapping("/bookings/mine")
    @PreAuthorize("hasAuthority('View Resource Booking')")
    public ResponseEntity<List<ResourceBookingResponse>> getMyBookings(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        BookingStatus bookingStatus = status != null && !status.isBlank()
                ? BookingStatus.valueOf(status.toUpperCase()) : null;
        return ResponseEntity.ok(bookingService.getMyBookings(user.getId(), bookingStatus));
    }

    @GetMapping("/bookings/today")
    @PreAuthorize("hasAuthority('Manage Resources')")
    public ResponseEntity<List<ResourceBookingResponse>> getTodaysBookings(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(bookingService.getTodaysBookings(communityId));
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasAuthority('Manage Resources')")
    public ResponseEntity<Page<ResourceBookingResponse>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        List<BookingStatus> statuses = null;
        if (status != null && !status.isBlank()) {
            statuses = List.of(BookingStatus.valueOf(status.toUpperCase()));
        }
        return ResponseEntity.ok(bookingService.getAllBookings(communityId, statuses, PageRequest.of(page, size)));
    }

    @GetMapping("/bookings/{id}")
    @PreAuthorize("hasAuthority('View Resource Booking')")
    public ResponseEntity<ResourceBookingResponse> getBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(bookingService.getBooking(id));
    }

    @PutMapping("/bookings/{id}/cancel")
    @PreAuthorize("hasAuthority('View Resource Booking')")
    public ResponseEntity<ResourceBookingResponse> cancelBooking(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String reason = body.getOrDefault("reason", null);
        return ResponseEntity.ok(bookingService.cancelBooking(id, user.getId(), reason));
    }

    @PutMapping("/bookings/{id}/approve")
    @PreAuthorize("hasAuthority('Approve Bookings')")
    public ResponseEntity<ResourceBookingResponse> approveBooking(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String comments = body.getOrDefault("comments", null);
        return ResponseEntity.ok(bookingService.approveBooking(id, user, comments));
    }

    @PutMapping("/bookings/{id}/reject")
    @PreAuthorize("hasAuthority('Approve Bookings')")
    public ResponseEntity<ResourceBookingResponse> rejectBooking(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String comments = body.getOrDefault("comments", null);
        return ResponseEntity.ok(bookingService.rejectBooking(id, user, comments));
    }

    @PutMapping("/bookings/{id}/check-in")
    @PreAuthorize("hasAuthority('Manage Resources')")
    public ResponseEntity<ResourceBookingResponse> checkIn(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(bookingService.checkIn(id));
    }

    @PutMapping("/bookings/{id}/check-out")
    @PreAuthorize("hasAuthority('Manage Resources')")
    public ResponseEntity<ResourceBookingResponse> checkOut(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(bookingService.checkOut(id));
    }

    @PutMapping("/bookings/{id}/rate")
    @PreAuthorize("hasAuthority('View Resource Booking')")
    public ResponseEntity<ResourceBookingResponse> rateBooking(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Integer rating = (Integer) body.get("rating");
        String comment = (String) body.get("comment");
        return ResponseEntity.ok(bookingService.rateBooking(id, user.getId(), rating, comment));
    }

    // ── Waitlist ─────────────────────────────────────────────────

    @PostMapping("/waitlist")
    @PreAuthorize("hasAuthority('View Resource Booking')")
    public ResponseEntity<WaitlistResponse> joinWaitlist(
            @Valid @RequestBody WaitlistRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(waitlistService.joinWaitlist(request, user, user.getCommunity()));
    }

    @GetMapping("/waitlist/mine")
    @PreAuthorize("hasAuthority('View Resource Booking')")
    public ResponseEntity<List<WaitlistResponse>> getMyWaitlist(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(waitlistService.getMyWaitlist(user.getId()));
    }

    @DeleteMapping("/waitlist/{id}")
    @PreAuthorize("hasAuthority('View Resource Booking')")
    public ResponseEntity<Void> cancelWaitlist(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        waitlistService.cancelWaitlist(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    // ── Business Rules ───────────────────────────────────────────

    @GetMapping("/rules")
    @PreAuthorize("hasAuthority('Manage Booking Rules')")
    public ResponseEntity<List<BusinessRuleResponse>> getRules(
            @RequestParam(required = false) Long resourceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(ruleService.getRules(communityId, resourceId));
    }

    @PostMapping("/rules")
    @PreAuthorize("hasAuthority('Manage Booking Rules')")
    public ResponseEntity<BusinessRuleResponse> createRule(
            @Valid @RequestBody BusinessRuleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ruleService.createRule(request, user.getCommunity()));
    }

    @PutMapping("/rules/{id}")
    @PreAuthorize("hasAuthority('Manage Booking Rules')")
    public ResponseEntity<BusinessRuleResponse> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody BusinessRuleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ruleService.updateRule(id, request));
    }

    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasAuthority('Manage Booking Rules')")
    public ResponseEntity<Void> deleteRule(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        ruleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    // ── Pricing Rules ────────────────────────────────────────────

    @GetMapping("/pricing")
    @PreAuthorize("hasAuthority('Manage Pricing')")
    public ResponseEntity<List<PricingRuleResponse>> getPricingRules(
            @RequestParam(required = false) Long resourceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(ruleService.getPricingRules(communityId, resourceId));
    }

    @PostMapping("/pricing")
    @PreAuthorize("hasAuthority('Manage Pricing')")
    public ResponseEntity<PricingRuleResponse> createPricingRule(
            @Valid @RequestBody PricingRuleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ruleService.createPricingRule(request, user.getCommunity()));
    }

    @PutMapping("/pricing/{id}")
    @PreAuthorize("hasAuthority('Manage Pricing')")
    public ResponseEntity<PricingRuleResponse> updatePricingRule(
            @PathVariable Long id,
            @Valid @RequestBody PricingRuleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ruleService.updatePricingRule(id, request));
    }

    @DeleteMapping("/pricing/{id}")
    @PreAuthorize("hasAuthority('Manage Pricing')")
    public ResponseEntity<Void> deletePricingRule(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        ruleService.deletePricingRule(id);
        return ResponseEntity.noContent().build();
    }

    // ── Approval Workflows ───────────────────────────────────────

    @GetMapping("/workflows")
    @PreAuthorize("hasAuthority('Manage Workflows')")
    public ResponseEntity<List<ApprovalWorkflowResponse>> getWorkflows(
            @RequestParam(required = false) Long resourceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(ruleService.getWorkflows(communityId, resourceId));
    }

    // ── Maintenance ──────────────────────────────────────────────

    @GetMapping("/maintenance")
    @PreAuthorize("hasAuthority('Manage Maintenance')")
    public ResponseEntity<List<MaintenanceResponse>> getMaintenanceRecords(
            @RequestParam(required = false) Long resourceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(maintenanceService.getMaintenanceRecords(communityId, resourceId));
    }

    @PostMapping("/maintenance")
    @PreAuthorize("hasAuthority('Manage Maintenance')")
    public ResponseEntity<MaintenanceResponse> createMaintenance(
            @Valid @RequestBody MaintenanceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.createMaintenance(request, user, user.getCommunity()));
    }

    @PutMapping("/maintenance/{id}")
    @PreAuthorize("hasAuthority('Manage Maintenance')")
    public ResponseEntity<MaintenanceResponse> updateMaintenance(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(maintenanceService.updateMaintenance(id, request));
    }

    // ── Coupons ──────────────────────────────────────────────────

    @GetMapping("/coupons")
    @PreAuthorize("hasAuthority('Manage Coupons')")
    public ResponseEntity<List<CouponResponse>> getCoupons(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(couponService.getCoupons(communityId));
    }

    @PostMapping("/coupons")
    @PreAuthorize("hasAuthority('Manage Coupons')")
    public ResponseEntity<CouponResponse> createCoupon(
            @Valid @RequestBody CouponRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(couponService.createCoupon(request, user.getCommunity()));
    }

    @GetMapping("/coupons/validate")
    @PreAuthorize("hasAuthority('View Resource Booking')")
    public ResponseEntity<CouponResponse> validateCoupon(
            @RequestParam String code,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(couponService.validateCoupon(code));
    }

    // ── Dashboard & Analytics ────────────────────────────────────

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('View Booking Analytics')")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(dashboardService.getDashboardStats(communityId));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAuthority('View Booking Analytics')")
    public ResponseEntity<List<BookingAnalyticsResponse>> getAnalytics(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) Long resourceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(dashboardService.getAnalytics(communityId, resourceId,
                LocalDate.parse(from), LocalDate.parse(to)));
    }
}
