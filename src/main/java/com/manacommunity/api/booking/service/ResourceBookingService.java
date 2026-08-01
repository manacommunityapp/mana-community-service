package com.manacommunity.api.booking.service;

import com.manacommunity.api.booking.dto.EquipmentResponse;
import com.manacommunity.api.booking.dto.ResourceBookingRequest;
import com.manacommunity.api.booking.dto.ResourceBookingResponse;
import com.manacommunity.api.booking.dto.SlotResponse;
import com.manacommunity.api.booking.entity.*;
import com.manacommunity.api.booking.entity.enums.*;
import com.manacommunity.api.booking.repository.*;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceBookingService {

    private final ResourceBookingRepository bookingRepo;
    private final ResourceRepository resourceRepo;
    private final BookingEquipmentRepository equipmentRepo;
    private final BookingWaitlistRepository waitlistRepo;
    private final BusinessRuleRepository ruleRepo;
    private final PricingRuleRepository pricingRepo;
    private final ApprovalWorkflowRepository workflowRepo;
    private final BookingApprovalRepository approvalRepo;
    private final CouponRepository couponRepo;
    private final ResourceHolidayRepository holidayRepo;
    private final ResourceScheduleRepository scheduleRepo;
    private final ResourceMaintenanceRepository maintenanceRepo;

    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(
            BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);

    // ── Create Booking ─────────────────────────────────────────────

    @Transactional
    public ResourceBookingResponse createBooking(ResourceBookingRequest req, AppUser user, Community community) {
        // 1. Load and validate resource
        Resource resource = resourceRepo.findById(req.getResourceId())
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + req.getResourceId()));

        if (resource.getStatus() != ResourceStatus.ACTIVE) {
            throw new IllegalStateException("Resource is not available for booking. Current status: " + resource.getStatus());
        }

        // 2. Parse dates/times
        LocalDate bookingDate = LocalDate.parse(req.getBookingDate());
        LocalTime startTime = LocalTime.parse(req.getStartTime());
        LocalTime endTime = LocalTime.parse(req.getEndTime());
        LocalDate endDate = req.getEndDate() != null ? LocalDate.parse(req.getEndDate()) : null;

        if (!endTime.isAfter(startTime) && endDate == null) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        // 3. Validate business rules
        validateBusinessRules(resource, user);

        // 4. Check for conflicts
        List<ResourceBooking> conflicts = bookingRepo.findConflicting(
                resource.getId(), bookingDate, startTime, endTime);
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("The requested time slot conflicts with an existing booking");
        }

        // 5. Check for holidays
        if (holidayRepo.existsByResourceIdAndHolidayDate(resource.getId(), bookingDate)) {
            throw new IllegalStateException("The resource is not available on this date due to a holiday");
        }

        // 6. Check for maintenance windows
        LocalDateTime startDateTime = LocalDateTime.of(bookingDate, startTime);
        LocalDateTime endDateTime = LocalDateTime.of(endDate != null ? endDate : bookingDate, endTime);
        List<ResourceMaintenance> maintenanceWindows = maintenanceRepo.findOverlapping(
                resource.getId(), startDateTime, endDateTime);
        if (!maintenanceWindows.isEmpty()) {
            throw new IllegalStateException("The resource is under maintenance during the requested time");
        }

        // 7. Generate booking number
        String bookingNumber = generateBookingNumber();

        // 8. Calculate pricing
        BigDecimal totalAmount = calculateTotalAmount(resource, bookingDate, startTime, endTime, req.getCouponCode());
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;

        // Calculate tax from pricing rules
        List<PricingRule> pricingRules = pricingRepo.findByResourceIdAndIsActiveTrueOrderByPricingTypeAsc(resource.getId());
        for (PricingRule rule : pricingRules) {
            if (rule.getPricingType() == PricingType.TAX && rule.getPercentage() != null) {
                taxAmount = totalAmount.multiply(rule.getPercentage()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
        }

        // Apply coupon discount
        if (req.getCouponCode() != null && !req.getCouponCode().isBlank()) {
            Coupon coupon = couponRepo.findByCodeAndIsActiveTrue(req.getCouponCode()).orElse(null);
            if (coupon != null) {
                discountAmount = calculateDiscount(coupon, totalAmount);
            }
        }

        BigDecimal finalAmount = totalAmount.add(taxAmount).subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        // 9. Determine if approval is required
        boolean approvalRequired = resource.isApprovalRequired();
        List<ApprovalWorkflow> workflows = workflowRepo.findByResourceIdAndIsActiveTrueOrderByStepOrderAsc(resource.getId());
        if (!workflows.isEmpty()) {
            approvalRequired = true;
        }

        ApprovalStatus approvalStatus = approvalRequired ? ApprovalStatus.PENDING : ApprovalStatus.NOT_REQUIRED;
        BookingStatus bookingStatus = approvalRequired ? BookingStatus.PENDING : BookingStatus.CONFIRMED;

        // 10. Create booking entity
        ResourceBooking booking = ResourceBooking.builder()
                .bookingNumber(bookingNumber)
                .resource(resource)
                .bookedBy(user)
                .bookingDate(bookingDate)
                .startTime(startTime)
                .endTime(endTime)
                .endDate(endDate)
                .purpose(req.getPurpose())
                .numberOfGuests(req.getNumberOfGuests())
                .status(bookingStatus)
                .approvalStatus(approvalStatus)
                .totalAmount(finalAmount)
                .taxAmount(taxAmount)
                .discountAmount(discountAmount)
                .paymentStatus(resource.isPaymentRequired() ? PaymentStatus.PENDING : PaymentStatus.WAIVED)
                .community(community)
                .build();

        booking = bookingRepo.save(booking);

        // Create approval records if workflow exists
        if (approvalRequired && !workflows.isEmpty()) {
            for (ApprovalWorkflow workflow : workflows) {
                BookingApproval approval = BookingApproval.builder()
                        .booking(booking)
                        .workflowStep(workflow)
                        .build();
                approvalRepo.save(approval);
            }
        }

        // 11. Handle equipment allocation
        if (req.getEquipmentIds() != null && !req.getEquipmentIds().isEmpty()) {
            for (Long equipmentId : req.getEquipmentIds()) {
                Resource equipmentResource = resourceRepo.findById(equipmentId).orElse(null);
                if (equipmentResource != null) {
                    BookingEquipment equipment = BookingEquipment.builder()
                            .booking(booking)
                            .resource(equipmentResource)
                            .quantity(1)
                            .build();
                    equipmentRepo.save(equipment);
                }
            }
        }

        // Increment coupon usage if applied
        if (req.getCouponCode() != null && !req.getCouponCode().isBlank() && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            couponRepo.findByCodeAndIsActiveTrue(req.getCouponCode()).ifPresent(coupon -> {
                coupon.setCurrentUses(coupon.getCurrentUses() + 1);
                couponRepo.save(coupon);
            });
        }

        // 12. Return response
        return toResponse(booking);
    }

    // ── Query Methods ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ResourceBookingResponse> getMyBookings(Long userId, BookingStatus status) {
        List<ResourceBooking> bookings;
        if (status != null) {
            bookings = bookingRepo.findByBookedByIdAndStatusOrderByBookingDateDesc(userId, status);
        } else {
            bookings = bookingRepo.findByBookedByIdOrderByBookingDateDescStartTimeDesc(userId);
        }
        return bookings.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceBookingResponse> getTodaysBookings(Long communityId) {
        return bookingRepo.findTodaysByCommunity(communityId, LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ResourceBookingResponse> getAllBookings(Long communityId, List<BookingStatus> statuses, Pageable pageable) {
        List<BookingStatus> effectiveStatuses = (statuses != null && !statuses.isEmpty())
                ? statuses
                : List.of(BookingStatus.values());
        return bookingRepo.findByCommunityIdAndStatusIn(communityId, effectiveStatuses, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ResourceBookingResponse getBooking(Long id) {
        ResourceBooking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));
        return toResponse(booking);
    }

    // ── Cancel ─────────────────────────────────────────────────────

    @Transactional
    public ResourceBookingResponse cancelBooking(Long id, Long userId, String reason) {
        ResourceBooking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));

        if (!booking.getBookedBy().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only cancel your own bookings");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }

        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            throw new IllegalStateException("Cannot cancel a booking that is already checked in");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledAt(LocalDateTime.now());
        return toResponse(bookingRepo.save(booking));
    }

    @Transactional
    public ResourceBookingResponse adminCancelBooking(Long id, String reason) {
        ResourceBooking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledAt(LocalDateTime.now());
        return toResponse(bookingRepo.save(booking));
    }

    // ── Approval ───────────────────────────────────────────────────

    @Transactional
    public ResourceBookingResponse approveBooking(Long id, AppUser approver, String comments) {
        ResourceBooking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));

        if (booking.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Booking is not pending approval. Current status: " + booking.getApprovalStatus());
        }

        booking.setApprovalStatus(ApprovalStatus.APPROVED);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setApprovedBy(approver);
        booking.setApprovedAt(LocalDateTime.now());

        // Update approval records
        List<BookingApproval> approvals = approvalRepo.findByBookingIdOrderByCreatedAtAsc(id);
        for (BookingApproval approval : approvals) {
            if (approval.getStatus() == BookingApproval.ApprovalStepStatus.PENDING) {
                approval.setStatus(BookingApproval.ApprovalStepStatus.APPROVED);
                approval.setApprovedBy(approver);
                approval.setApprovedAt(LocalDateTime.now());
                approval.setComments(comments);
                approvalRepo.save(approval);
            }
        }

        return toResponse(bookingRepo.save(booking));
    }

    @Transactional
    public ResourceBookingResponse rejectBooking(Long id, AppUser approver, String comments) {
        ResourceBooking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));

        if (booking.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Booking is not pending approval. Current status: " + booking.getApprovalStatus());
        }

        booking.setApprovalStatus(ApprovalStatus.REJECTED);
        booking.setStatus(BookingStatus.REJECTED);
        booking.setApprovedBy(approver);
        booking.setApprovedAt(LocalDateTime.now());

        // Update approval records
        List<BookingApproval> approvals = approvalRepo.findByBookingIdOrderByCreatedAtAsc(id);
        for (BookingApproval approval : approvals) {
            if (approval.getStatus() == BookingApproval.ApprovalStepStatus.PENDING) {
                approval.setStatus(BookingApproval.ApprovalStepStatus.REJECTED);
                approval.setApprovedBy(approver);
                approval.setApprovedAt(LocalDateTime.now());
                approval.setComments(comments);
                approvalRepo.save(approval);
            }
        }

        return toResponse(bookingRepo.save(booking));
    }

    // ── Check-in / Check-out ───────────────────────────────────────

    @Transactional
    public ResourceBookingResponse checkIn(Long id) {
        ResourceBooking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be checked in. Current status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckedInAt(LocalDateTime.now());
        return toResponse(bookingRepo.save(booking));
    }

    @Transactional
    public ResourceBookingResponse checkOut(Long id) {
        ResourceBooking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new IllegalStateException("Only checked-in bookings can be checked out. Current status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CHECKED_OUT);
        booking.setCheckedOutAt(LocalDateTime.now());
        return toResponse(bookingRepo.save(booking));
    }

    // ── Rating ─────────────────────────────────────────────────────

    @Transactional
    public ResourceBookingResponse rateBooking(Long id, Long userId, int rating, String comment) {
        ResourceBooking booking = bookingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));

        if (!booking.getBookedBy().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only rate your own bookings");
        }

        if (booking.getStatus() != BookingStatus.CHECKED_OUT && booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("You can only rate completed bookings");
        }

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        booking.setRating(rating);
        booking.setRatingComment(comment);
        booking.setRatedAt(LocalDateTime.now());
        return toResponse(bookingRepo.save(booking));
    }

    // ── Slot Generation ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SlotResponse> generateSlots(Long resourceId, LocalDate date) {
        Resource resource = resourceRepo.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + resourceId));

        // Check if holiday
        if (holidayRepo.existsByResourceIdAndHolidayDate(resourceId, date)) {
            return List.of(); // No slots on holidays
        }

        // Get schedule for the day of week
        ResourceSchedule schedule = scheduleRepo.findByResourceIdAndDayOfWeek(resourceId, date.getDayOfWeek())
                .orElse(null);

        // Determine open/close times
        LocalTime openTime;
        LocalTime closeTime;
        if (schedule != null) {
            if (!schedule.isAvailable()) {
                return List.of(); // Resource not available on this day
            }
            openTime = schedule.getOpenTime() != null ? schedule.getOpenTime() : resource.getOpenTime();
            closeTime = schedule.getCloseTime() != null ? schedule.getCloseTime() : resource.getCloseTime();
        } else {
            openTime = resource.getOpenTime();
            closeTime = resource.getCloseTime();
        }

        if (openTime == null || closeTime == null) {
            return List.of(); // No schedule configured
        }

        int durationMinutes = resource.getBookingDurationMinutes() != null ? resource.getBookingDurationMinutes() : 60;
        int bufferMinutes = resource.getBufferTimeMinutes() != null ? resource.getBufferTimeMinutes() : 0;
        int slotIncrement = durationMinutes + bufferMinutes;

        if (slotIncrement <= 0) {
            return List.of();
        }

        // Get existing bookings for the date
        List<ResourceBooking> existingBookings = bookingRepo.findByResourceIdAndBookingDateAndStatusInOrderByStartTimeAsc(
                resourceId, date, ACTIVE_STATUSES);

        // Check maintenance windows
        List<ResourceMaintenance> maintenanceWindows = maintenanceRepo.findOverlapping(
                resourceId,
                LocalDateTime.of(date, openTime),
                LocalDateTime.of(date, closeTime));

        List<SlotResponse> slots = new ArrayList<>();
        LocalTime slotStart = openTime;

        while (true) {
            LocalTime slotEnd = slotStart.plusMinutes(durationMinutes);

            // Check if slot exceeds close time (handle midnight crossing)
            if (closeTime.isAfter(openTime)) {
                // Normal case: close time is after open time
                if (slotEnd.isAfter(closeTime)) break;
            } else {
                // Midnight crossing: close time is before open time (e.g., 22:00 - 02:00)
                if (slotStart.isBefore(closeTime) && slotStart.isAfter(openTime)) {
                    // This shouldn't happen in midnight crossing after wrapping
                    break;
                }
                if (slotEnd.isBefore(slotStart) && slotEnd.isAfter(closeTime)) break;
            }

            boolean available = true;
            Long bookingId = null;
            String bookedByName = null;

            // Check against existing bookings
            for (ResourceBooking existing : existingBookings) {
                if (slotStart.isBefore(existing.getEndTime()) && slotEnd.isAfter(existing.getStartTime())) {
                    available = false;
                    bookingId = existing.getId();
                    bookedByName = existing.getBookedBy() != null ? existing.getBookedBy().getFullName() : null;
                    break;
                }
            }

            // Check against maintenance windows
            if (available) {
                LocalDateTime slotStartDt = LocalDateTime.of(date, slotStart);
                LocalDateTime slotEndDt = LocalDateTime.of(date, slotEnd);
                for (ResourceMaintenance maint : maintenanceWindows) {
                    if (slotStartDt.isBefore(maint.getEndDate()) && slotEndDt.isAfter(maint.getStartDate())) {
                        available = false;
                        break;
                    }
                }
            }

            slots.add(SlotResponse.builder()
                    .startTime(slotStart.format(DateTimeFormatter.ISO_LOCAL_TIME))
                    .endTime(slotEnd.format(DateTimeFormatter.ISO_LOCAL_TIME))
                    .available(available)
                    .bookingId(bookingId)
                    .bookedByName(bookedByName)
                    .build());

            slotStart = slotStart.plusMinutes(slotIncrement);

            // Safety: prevent infinite loop for midnight-crossing schedules
            if (slotStart.equals(openTime)) break;
        }

        return slots;
    }

    @Transactional(readOnly = true)
    public List<ResourceBookingResponse> getBookingsByResource(Long resourceId, LocalDate date) {
        return bookingRepo.findByResourceIdAndBookingDateAndStatusInOrderByStartTimeAsc(
                        resourceId, date, ACTIVE_STATUSES)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Private Helpers ────────────────────────────────────────────

    private String generateBookingNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String randomPart = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 5).toUpperCase();
        return "BK-" + datePart + "-" + randomPart;
    }

    private BigDecimal calculateTotalAmount(Resource resource, LocalDate bookingDate,
                                            LocalTime startTime, LocalTime endTime, String couponCode) {
        // Get pricing rules for the resource
        List<PricingRule> rules = pricingRepo.findByResourceIdAndIsActiveTrueOrderByPricingTypeAsc(resource.getId());

        BigDecimal baseAmount = BigDecimal.ZERO;
        BigDecimal adjustments = BigDecimal.ZERO;

        // Calculate duration in hours
        long durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        if (durationMinutes <= 0) durationMinutes = 60; // Fallback
        BigDecimal durationHours = BigDecimal.valueOf(durationMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        for (PricingRule rule : rules) {
            if (!isRuleApplicable(rule, bookingDate)) continue;

            switch (rule.getPricingType()) {
                case BASE_HOURLY:
                    if (rule.getAmount() != null) {
                        baseAmount = rule.getAmount().multiply(durationHours);
                    }
                    break;
                case WEEKEND:
                    if (isWeekend(bookingDate) && rule.getPercentage() != null) {
                        adjustments = adjustments.add(
                                baseAmount.multiply(rule.getPercentage()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                    }
                    break;
                case PEAK_HOUR:
                    if (isPeakHour(rule, startTime) && rule.getPercentage() != null) {
                        adjustments = adjustments.add(
                                baseAmount.multiply(rule.getPercentage()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                    }
                    break;
                case FESTIVAL:
                    if (rule.getPercentage() != null) {
                        adjustments = adjustments.add(
                                baseAmount.multiply(rule.getPercentage()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                    }
                    break;
                default:
                    // TAX, DISCOUNT, SECURITY_DEPOSIT, etc. handled separately
                    break;
            }
        }

        return baseAmount.add(adjustments);
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal amount) {
        if (coupon.getMaxUses() != null && coupon.getCurrentUses() >= coupon.getMaxUses()) {
            return BigDecimal.ZERO;
        }

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            return BigDecimal.ZERO;
        }
        if (coupon.getValidTo() != null && now.isAfter(coupon.getValidTo())) {
            return BigDecimal.ZERO;
        }

        if (coupon.getMinBookingAmount() != null && amount.compareTo(coupon.getMinBookingAmount()) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = amount.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = coupon.getDiscountValue();
        }

        // Cap at max discount amount
        if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            discount = coupon.getMaxDiscountAmount();
        }

        return discount;
    }

    private void validateBusinessRules(Resource resource, AppUser user) {
        List<BusinessRule> rules = ruleRepo.findByResourceIdAndIsActiveTrue(resource.getId());

        for (BusinessRule rule : rules) {
            if (rule.getRuleType() == RuleType.MAX_BOOKINGS) {
                int maxBookings = resource.getMaxBookingsPerUser() != null ? resource.getMaxBookingsPerUser() : Integer.MAX_VALUE;
                if (rule.getRuleValue() != null) {
                    try {
                        maxBookings = Integer.parseInt(rule.getRuleValue());
                    } catch (NumberFormatException ignored) {
                        // Use resource default
                    }
                }
                long currentBookings = bookingRepo.countByBookedByIdAndResourceIdAndStatusIn(
                        user.getId(), resource.getId(), ACTIVE_STATUSES);
                if (currentBookings >= maxBookings) {
                    throw new IllegalStateException(
                            "You have reached the maximum number of active bookings (" + maxBookings + ") for this resource");
                }
            }

            if (rule.getRuleType() == RuleType.CAPACITY_LIMIT) {
                int maxActive = resource.getMaxActiveBookings() != null ? resource.getMaxActiveBookings() : Integer.MAX_VALUE;
                if (rule.getRuleValue() != null) {
                    try {
                        maxActive = Integer.parseInt(rule.getRuleValue());
                    } catch (NumberFormatException ignored) {
                        // Use resource default
                    }
                }
                long activeBookings = bookingRepo.countByBookedByIdAndStatusIn(user.getId(), ACTIVE_STATUSES);
                if (activeBookings >= maxActive) {
                    throw new IllegalStateException(
                            "You have reached the maximum number of active bookings (" + maxActive + ") across all resources");
                }
            }
        }

        // Also check resource-level limits even without explicit rules
        if (resource.getMaxBookingsPerUser() != null) {
            long currentBookings = bookingRepo.countByBookedByIdAndResourceIdAndStatusIn(
                    user.getId(), resource.getId(), ACTIVE_STATUSES);
            if (currentBookings >= resource.getMaxBookingsPerUser()) {
                throw new IllegalStateException(
                        "You have reached the maximum number of active bookings (" + resource.getMaxBookingsPerUser() + ") for this resource");
            }
        }
    }

    private boolean isRuleApplicable(PricingRule rule, LocalDate bookingDate) {
        if (rule.getValidFrom() != null && bookingDate.isBefore(rule.getValidFrom())) return false;
        if (rule.getValidTo() != null && bookingDate.isAfter(rule.getValidTo())) return false;
        return true;
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                || date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
    }

    private boolean isPeakHour(PricingRule rule, LocalTime startTime) {
        if (rule.getStartTime() == null || rule.getEndTime() == null) return false;
        return !startTime.isBefore(rule.getStartTime()) && startTime.isBefore(rule.getEndTime());
    }

    private ResourceBookingResponse toResponse(ResourceBooking b) {
        List<EquipmentResponse> equipmentList = null;
        List<BookingEquipment> equipment = equipmentRepo.findByBookingId(b.getId());
        if (!equipment.isEmpty()) {
            equipmentList = equipment.stream()
                    .map(e -> EquipmentResponse.builder()
                            .id(e.getId())
                            .resourceId(e.getResource() != null ? e.getResource().getId() : null)
                            .resourceName(e.getResource() != null ? e.getResource().getName() : null)
                            .quantity(e.getQuantity())
                            .notes(e.getNotes())
                            .build())
                    .toList();
        }

        Resource resource = b.getResource();
        return ResourceBookingResponse.builder()
                .id(b.getId())
                .bookingNumber(b.getBookingNumber())
                .resourceId(resource != null ? resource.getId() : null)
                .resourceName(resource != null ? resource.getName() : null)
                .resourceCategory(resource != null && resource.getCategory() != null ? resource.getCategory().getName() : null)
                .bookingDate(b.getBookingDate() != null ? b.getBookingDate().toString() : null)
                .startTime(b.getStartTime() != null ? b.getStartTime().format(DateTimeFormatter.ISO_LOCAL_TIME) : null)
                .endTime(b.getEndTime() != null ? b.getEndTime().format(DateTimeFormatter.ISO_LOCAL_TIME) : null)
                .endDate(b.getEndDate() != null ? b.getEndDate().toString() : null)
                .purpose(b.getPurpose())
                .numberOfGuests(b.getNumberOfGuests())
                .status(b.getStatus() != null ? b.getStatus().name() : null)
                .approvalStatus(b.getApprovalStatus() != null ? b.getApprovalStatus().name() : null)
                .qrCode(b.getQrCode())
                .checkedInAt(formatDt(b.getCheckedInAt()))
                .checkedOutAt(formatDt(b.getCheckedOutAt()))
                .totalAmount(b.getTotalAmount())
                .depositAmount(b.getDepositAmount())
                .taxAmount(b.getTaxAmount())
                .discountAmount(b.getDiscountAmount())
                .paymentStatus(b.getPaymentStatus() != null ? b.getPaymentStatus().name() : null)
                .paymentReference(b.getPaymentReference())
                .isRecurring(b.isRecurring())
                .rating(b.getRating())
                .ratingComment(b.getRatingComment())
                .bookedById(b.getBookedBy() != null ? b.getBookedBy().getId() : null)
                .bookedByName(b.getBookedBy() != null ? b.getBookedBy().getFullName() : null)
                .approvedByName(b.getApprovedBy() != null ? b.getApprovedBy().getFullName() : null)
                .cancellationReason(b.getCancellationReason())
                .equipment(equipmentList)
                .createdAt(formatDt(b.getCreatedAt()))
                .updatedAt(formatDt(b.getUpdatedAt()))
                .build();
    }

    private String formatDt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }
}
