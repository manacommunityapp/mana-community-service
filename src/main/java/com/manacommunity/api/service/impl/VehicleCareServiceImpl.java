package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.service.VehicleCareService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleCareServiceImpl implements VehicleCareService {

    private final VehicleProfileRepository vehicleProfileRepo;
    private final VehicleServiceCatalogRepository catalogRepo;
    private final VehicleServiceAddonRepository addonRepo;
    private final VehicleServiceSlotRepository slotRepo;
    private final VehicleCareBookingRepository bookingRepo;
    private final VehicleBookingAddonRepository bookingAddonRepo;
    private final VehicleCarePlanCatalogRepository planCatalogRepo;
    private final VehicleCarePlanRepository planRepo;
    private final AppUserRepository appUserRepo;

    // ── Vehicles ─────────────────────────────────────────────────

    @Override
    public List<VehicleProfileResponse> getVehicles(AppUser user) {
        return vehicleProfileRepo.findByOwnerOrderByIsDefaultDescCreatedAtAsc(user)
                .stream().map(this::toVehicleResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VehicleProfileResponse createVehicle(AppUser user, CreateVehicleProfileRequest req) {
        if (req.isSetAsDefault()) {
            vehicleProfileRepo.clearDefaultForOwner(user);
        }
        VehicleProfile vehicle = VehicleProfile.builder()
                .owner(user)
                .communityId(user.getCommunity().getId())
                .nickname(req.getNickname())
                .vehicleType(req.getVehicleType())
                .make(req.getMake())
                .model(req.getModel())
                .color(req.getColor())
                .plateNumber(req.getPlateNumber().toUpperCase())
                .isDefault(req.isSetAsDefault())
                .build();
        return toVehicleResponse(vehicleProfileRepo.save(vehicle));
    }

    @Override
    @Transactional
    public VehicleProfileResponse updateVehicle(AppUser user, Long vehicleId, CreateVehicleProfileRequest req) {
        VehicleProfile vehicle = vehicleProfileRepo.findByIdAndOwner(vehicleId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        if (req.isSetAsDefault()) {
            vehicleProfileRepo.clearDefaultForOwner(user);
            vehicle.setDefault(true);
        }
        vehicle.setNickname(req.getNickname());
        vehicle.setVehicleType(req.getVehicleType());
        vehicle.setMake(req.getMake());
        vehicle.setModel(req.getModel());
        vehicle.setColor(req.getColor());
        vehicle.setPlateNumber(req.getPlateNumber().toUpperCase());
        return toVehicleResponse(vehicleProfileRepo.save(vehicle));
    }

    @Override
    @Transactional
    public void deleteVehicle(AppUser user, Long vehicleId) {
        VehicleProfile vehicle = vehicleProfileRepo.findByIdAndOwner(vehicleId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        boolean hasBookings = bookingRepo.findByResidentOrderByCreatedAtDesc(user, PageRequest.of(0, 50))
                .getContent().stream().anyMatch(b -> b.getVehicle() != null && b.getVehicle().getId().equals(vehicleId));
        if (hasBookings) {
            throw new IllegalStateException("Cannot delete vehicle with linked service bookings.");
        }
        vehicleProfileRepo.delete(vehicle);
    }

    @Override
    @Transactional
    public VehicleProfileResponse setDefaultVehicle(AppUser user, Long vehicleId) {
        VehicleProfile vehicle = vehicleProfileRepo.findByIdAndOwner(vehicleId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        vehicleProfileRepo.clearDefaultForOwner(user);
        vehicle.setDefault(true);
        return toVehicleResponse(vehicleProfileRepo.save(vehicle));
    }

    // ── Catalog ──────────────────────────────────────────────────

    @Override
    public List<VehicleServiceCatalogResponse> getCatalog() {
        return catalogRepo.findByIsActiveTrueOrderBySortOrderAsc()
                .stream().map(svc -> {
                    List<VehicleServiceAddonResponse> addons = addonRepo
                            .findByServiceAndIsActiveTrueOrderByPriceAsc(svc)
                            .stream().map(this::toAddonResponse).collect(Collectors.toList());
                    return toCatalogResponse(svc, addons);
                }).collect(Collectors.toList());
    }

    @Override
    public List<VehicleServiceAddonResponse> getAddons(Long serviceId) {
        VehicleServiceCatalog svc = catalogRepo.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        return addonRepo.findByServiceAndIsActiveTrueOrderByPriceAsc(svc)
                .stream().map(this::toAddonResponse).collect(Collectors.toList());
    }

    // ── Slots ────────────────────────────────────────────────────

    @Override
    @Transactional
    public List<VehicleServiceSlotResponse> getAvailableSlots(AppUser user, LocalDate from, LocalDate to) {
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 1L;
        LocalDate effectiveFrom = from != null ? from : LocalDate.now();
        LocalDate effectiveTo = to != null ? to : effectiveFrom.plusDays(7);
        if (effectiveTo.isBefore(effectiveFrom)) {
            effectiveTo = effectiveFrom.plusDays(7);
        }

        List<VehicleServiceSlot> slots = slotRepo.findByCommunityIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(
                communityId, effectiveFrom, effectiveTo);

        if (slots.isEmpty()) {
            // Auto-generate standard operational slots (8 AM to 6 PM) for the requested range if none exist
            LocalDate cur = effectiveFrom;
            java.time.LocalTime[] startTimes = {
                    java.time.LocalTime.of(8, 0), java.time.LocalTime.of(9, 0),
                    java.time.LocalTime.of(10, 0), java.time.LocalTime.of(11, 0),
                    java.time.LocalTime.of(14, 0), java.time.LocalTime.of(15, 0),
                    java.time.LocalTime.of(16, 0), java.time.LocalTime.of(17, 0)
            };
            while (!cur.isAfter(effectiveTo)) {
                for (java.time.LocalTime st : startTimes) {
                    VehicleServiceSlot s = VehicleServiceSlot.builder()
                            .communityId(communityId)
                            .slotDate(cur)
                            .startTime(st)
                            .endTime(st.plusHours(1))
                            .totalCapacity(3)
                            .bookedCount(0)
                            .build();
                    slotRepo.save(s);
                    slots.add(s);
                }
                cur = cur.plusDays(1);
            }
        }

        return slots.stream().map(this::toSlotResponse).collect(Collectors.toList());
    }

    // ── Bookings ─────────────────────────────────────────────────

    @Override
    @Transactional
    public VehicleCareBookingResponse createBooking(AppUser user, CreateVehicleCareBookingRequest req) {
        VehicleProfile vehicle = vehicleProfileRepo.findByIdAndOwner(req.getVehicleId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        VehicleServiceCatalog service = catalogRepo.findById(req.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        VehicleServiceSlot slot = slotRepo.findByIdForUpdate(req.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));

        if (user.getCommunity() != null && !slot.getCommunityId().equals(user.getCommunity().getId())) {
            throw new IllegalArgumentException("Selected slot belongs to a different community");
        }

        if (slot.getBookedCount() >= slot.getTotalCapacity()) {
            throw new IllegalStateException("Slot is fully booked");
        }

        slot.setBookedCount(slot.getBookedCount() + 1);
        slotRepo.save(slot);


        BigDecimal addonTotal = BigDecimal.ZERO;
        VehicleCareBooking booking = VehicleCareBooking.builder()
                .bookingNumber(generateBookingNumber())
                .resident(user)
                .communityId(user.getCommunity().getId())
                .vehicle(vehicle)
                .service(service)
                .slot(slot)
                .status("PENDING_PAYMENT")
                .baseAmount(service.getCommunityPrice())
                .addonAmount(addonTotal)
                .totalAmount(service.getCommunityPrice())
                .locationNotes(req.getLocationNotes())
                .build();
        booking = bookingRepo.save(booking);

        if (req.getAddonIds() != null && !req.getAddonIds().isEmpty()) {
            for (Long addonId : req.getAddonIds()) {
                VehicleServiceAddon addon = addonRepo.findById(addonId).orElse(null);
                if (addon != null) {
                    VehicleBookingAddon ba = VehicleBookingAddon.builder()
                            .booking(booking).addon(addon).price(addon.getPrice()).build();
                    bookingAddonRepo.save(ba);
                    addonTotal = addonTotal.add(addon.getPrice());
                }
            }
            booking.setAddonAmount(addonTotal);
            booking.setTotalAmount(service.getCommunityPrice().add(addonTotal));
            booking = bookingRepo.save(booking);
        }

        return toBookingResponse(booking);
    }

    @Override
    public Page<VehicleCareBookingResponse> getMyBookings(AppUser user, String filter, int page, int size) {
        return bookingRepo.findByResidentOrderByCreatedAtDesc(user, PageRequest.of(page, size))
                .map(this::toBookingResponse);
    }

    @Override
    public VehicleCareBookingResponse getBooking(AppUser user, Long bookingId) {
        VehicleCareBooking booking = bookingRepo.findByIdAndResident(bookingId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        return toBookingResponse(booking);
    }

    @Override
    @Transactional
    public void cancelBooking(AppUser user, Long bookingId) {
        VehicleCareBooking booking = bookingRepo.findByIdAndResident(bookingId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        List<String> cancellable = Arrays.asList("PENDING_PAYMENT", "CONFIRMED", "ASSIGNED");
        if (!cancellable.contains(booking.getStatus())) {
            throw new IllegalStateException("Booking cannot be cancelled in status: " + booking.getStatus());
        }
        booking.setStatus("CANCELLED");
        VehicleServiceSlot slot = booking.getSlot();
        if (slot.getBookedCount() > 0) {
            slot.setBookedCount(slot.getBookedCount() - 1);
            slotRepo.save(slot);
        }
        bookingRepo.save(booking);
    }

    @Override
    @Transactional
    public VehicleCareBookingResponse submitReview(AppUser user, Long bookingId, SubmitVehicleReviewRequest req) {
        VehicleCareBooking booking = bookingRepo.findByIdAndResident(bookingId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!"COMPLETED".equals(booking.getStatus())) {
            throw new IllegalStateException("Can only review completed bookings");
        }
        booking.setRating(req.getRating());
        booking.setReviewText(req.getReviewText());
        return toBookingResponse(bookingRepo.save(booking));
    }

    // ── Plans ────────────────────────────────────────────────────

    @Override
    public List<VehicleCarePlanCatalogResponse> getPlanCatalog() {
        return planCatalogRepo.findByIsActiveTrueOrderBySortOrderAsc()
                .stream().map(this::toPlanCatalogResponse).collect(Collectors.toList());
    }

    @Override
    public List<VehicleCarePlanResponse> getMyPlans(AppUser user) {
        return planRepo.findByResidentOrderByCreatedAtDesc(user)
                .stream().map(this::toPlanResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VehicleCarePlanResponse subscribePlan(AppUser user, SubscribeVehiclePlanRequest req) {
        VehicleCarePlanCatalog catalog = planCatalogRepo.findById(req.getPlanCatalogId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        VehicleProfile vehicle = vehicleProfileRepo.findByIdAndOwner(req.getVehicleId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        LocalDate now = LocalDate.now();
        VehicleCarePlan plan = VehicleCarePlan.builder()
                .resident(user)
                .communityId(user.getCommunity().getId())
                .planCatalog(catalog)
                .vehicle(vehicle)
                .status("ACTIVE")
                .creditsTotal(catalog.getWashesPerMonth())
                .creditsUsed(0)
                .startDate(now)
                .endDate(now.plusMonths(1))
                .paymentReference(req.getPaymentReference())
                .build();
        return toPlanResponse(planRepo.save(plan));
    }

    @Override
    @Transactional
    public VehicleCarePlanResponse updatePlanStatus(AppUser user, Long planId, String newStatus) {
        List<String> allowedPlanStatuses = Arrays.asList("ACTIVE", "PAUSED", "CANCELLED");
        if (newStatus == null || !allowedPlanStatuses.contains(newStatus.toUpperCase())) {
            throw new IllegalArgumentException("Invalid plan status: " + newStatus);
        }
        VehicleCarePlan plan = planRepo.findByIdAndResident(planId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        plan.setStatus(newStatus.toUpperCase());
        return toPlanResponse(planRepo.save(plan));
    }

    // ── Vendor jobs ──────────────────────────────────────────────

    @Override
    public List<VehicleCareBookingResponse> getMyJobs(AppUser technician, String filter) {
        List<String> statuses = "history".equalsIgnoreCase(filter)
                ? Arrays.asList("COMPLETED", "CANCELLED", "NO_SHOW")
                : Arrays.asList("CONFIRMED", "ASSIGNED", "EN_ROUTE", "ARRIVED", "IN_PROGRESS");
        return bookingRepo.findJobsForTechnician(technician, statuses)
                .stream().map(this::toBookingResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VehicleCareBookingResponse updateJobStatus(AppUser technician, Long bookingId, String newStatus) {
        List<String> allowedStatuses = Arrays.asList("CONFIRMED", "ASSIGNED", "EN_ROUTE", "ARRIVED", "IN_PROGRESS", "COMPLETED", "NO_SHOW", "CANCELLED");
        if (newStatus == null || !allowedStatuses.contains(newStatus.toUpperCase())) {
            throw new IllegalArgumentException("Invalid job status: " + newStatus);
        }
        VehicleCareBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (technician.getCommunity() != null && !booking.getCommunityId().equals(technician.getCommunity().getId())) {
            throw new AccessDeniedException("Cannot update booking for another community");
        }
        if (booking.getTechnician() != null && !booking.getTechnician().getId().equals(technician.getId())) {
            boolean isAdmin = (technician.getRole() != null && technician.getRole().toUpperCase().contains("ADMIN"))
                    || (technician.getUserRoles() != null && technician.getUserRoles().stream()
                    .anyMatch(r -> r.getName() != null && r.getName().toUpperCase().contains("ADMIN")));
            if (!isAdmin) {
                throw new AccessDeniedException("Not authorized to update this booking");
            }
        }
        if (booking.getTechnician() == null) {
            booking.setTechnician(technician);
        }
        booking.setStatus(newStatus.toUpperCase());
        return toBookingResponse(bookingRepo.save(booking));
    }

    // ── Payment & Admin ──────────────────────────────────────────

    @Override
    @Transactional
    public VehicleCareBookingResponse confirmPayment(AppUser user, Long bookingId, String paymentReference) {
        VehicleCareBooking booking = bookingRepo.findByIdAndResident(bookingId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        booking.setPaymentReference(paymentReference);
        if ("PENDING_PAYMENT".equals(booking.getStatus())) {
            booking.setStatus("CONFIRMED");
        }
        return toBookingResponse(bookingRepo.save(booking));
    }

    @Override
    @Transactional
    public VehicleCareBookingResponse assignTechnician(Long bookingId, Long technicianId) {
        VehicleCareBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        AppUser tech = appUserRepo.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found"));
        booking.setTechnician(tech);
        if ("CONFIRMED".equals(booking.getStatus()) || "PENDING_PAYMENT".equals(booking.getStatus())) {
            booking.setStatus("ASSIGNED");
        }
        return toBookingResponse(bookingRepo.save(booking));
    }

    @Override
    public Page<VehicleCareBookingResponse> getCommunityBookings(Long communityId, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        List<String> statuses;
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            statuses = Collections.singletonList(status.toUpperCase());
        } else {
            statuses = Arrays.asList("PENDING_PAYMENT", "CONFIRMED", "ASSIGNED", "EN_ROUTE", "ARRIVED", "IN_PROGRESS", "COMPLETED", "CANCELLED", "NO_SHOW");
        }
        return bookingRepo.findByCommunityAndStatuses(communityId, statuses, pageable)
                .map(this::toBookingResponse);
    }

    // ── Mapping helpers ──────────────────────────────────────────

    private VehicleProfileResponse toVehicleResponse(VehicleProfile v) {
        return VehicleProfileResponse.builder()
                .id(v.getId())
                .nickname(v.getNickname())
                .vehicleType(v.getVehicleType())
                .make(v.getMake())
                .model(v.getModel())
                .color(v.getColor())
                .plateNumber(v.getPlateNumber())
                .isDefault(v.isDefault())
                .createdAt(v.getCreatedAt())
                .build();
    }

    private VehicleServiceCatalogResponse toCatalogResponse(VehicleServiceCatalog s, List<VehicleServiceAddonResponse> addons) {
        return VehicleServiceCatalogResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .description(s.getDescription())
                .vehicleTypes(s.getVehicleTypes() != null
                        ? Arrays.asList(s.getVehicleTypes().split(",")) : Collections.emptyList())
                .durationMinutes(s.getDurationMinutes())
                .basePrice(s.getBasePrice())
                .communityPrice(s.getCommunityPrice())
                .isPopular(s.isPopular())
                .gradientStart(s.getGradientStart())
                .gradientEnd(s.getGradientEnd())
                .addons(addons)
                .build();
    }

    private VehicleServiceAddonResponse toAddonResponse(VehicleServiceAddon a) {
        return VehicleServiceAddonResponse.builder()
                .id(a.getId()).name(a.getName()).price(a.getPrice()).build();
    }

    private VehicleServiceSlotResponse toSlotResponse(VehicleServiceSlot s) {
        int remaining = s.getTotalCapacity() - s.getBookedCount();
        return VehicleServiceSlotResponse.builder()
                .id(s.getId())
                .slotDate(s.getSlotDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .totalCapacity(s.getTotalCapacity())
                .bookedCount(s.getBookedCount())
                .remaining(remaining)
                .available(remaining > 0)
                .build();
    }

    private VehicleCareBookingResponse toBookingResponse(VehicleCareBooking b) {
        VehicleProfile v = b.getVehicle();
        VehicleServiceCatalog svc = b.getService();
        VehicleServiceSlot slot = b.getSlot();
        List<VehicleBookingAddon> bAddons = bookingAddonRepo.findByBooking(b);
        List<VehicleServiceAddonResponse> addonResp = bAddons.stream()
                .map(ba -> toAddonResponse(ba.getAddon())).collect(Collectors.toList());
        List<String> addonNames = addonResp.stream()
                .map(VehicleServiceAddonResponse::getName).collect(Collectors.toList());

        LocalDate slotDate = slot != null ? slot.getSlotDate() : null;
        LocalTime startTime = slot != null ? slot.getStartTime() : null;
        LocalTime endTime = slot != null ? slot.getEndTime() : null;

        boolean canCancel = Arrays.asList("PENDING_PAYMENT", "CONFIRMED", "ASSIGNED").contains(b.getStatus());
        boolean canRate = "COMPLETED".equals(b.getStatus()) && b.getRating() == null;

        return VehicleCareBookingResponse.builder()
                .id(b.getId())
                .bookingNumber(b.getBookingNumber())
                .status(b.getStatus())
                .vehicleId(v != null ? v.getId() : null)
                .vehicleNickname(v != null ? v.getNickname() : null)
                .vehicleType(v != null ? v.getVehicleType() : null)
                .vehiclePlate(v != null ? v.getPlateNumber() : null)
                .vehicleDisplay(v != null ? (v.getMake() + " " + v.getModel()) : null)
                .serviceId(svc != null ? svc.getId() : null)
                .serviceName(svc != null ? svc.getName() : null)
                .durationMinutes(svc != null ? svc.getDurationMinutes() : 0)
                .slotId(slot != null ? slot.getId() : null)
                .scheduledDate(slotDate)
                .scheduledStart(startTime)
                .scheduledEnd(endTime)
                .technicianId(b.getTechnician() != null ? b.getTechnician().getId() : null)
                .technicianName(b.getTechnician() != null ? b.getTechnician().getFullName() : null)
                .baseAmount(b.getBaseAmount())
                .addonAmount(b.getAddonAmount())
                .totalAmount(b.getTotalAmount())
                .addons(addonResp)
                .locationNotes(b.getLocationNotes())
                .etaMinutes(b.getEtaMinutes())
                .rating(b.getRating())
                .reviewText(b.getReviewText())
                .createdAt(b.getCreatedAt())
                // Mobile UI compatibility helpers
                .date(slotDate != null ? slotDate.toString() : "")
                .startTime(startTime != null ? startTime.toString() : "")
                .endTime(endTime != null ? endTime.toString() : "")
                .location(b.getLocationNotes() != null ? b.getLocationNotes() : "")
                .quotedPrice(b.getTotalAmount())
                .paidAmount(b.getTotalAmount())
                .addonNames(addonNames)
                .canCancel(canCancel)
                .canRate(canRate)
                .build();
    }

    private VehicleCarePlanCatalogResponse toPlanCatalogResponse(VehicleCarePlanCatalog p) {
        int savings = 0;
        if (p.getOriginalPrice() != null && p.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0 && p.getPrice() != null) {
            savings = p.getOriginalPrice().subtract(p.getPrice())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(p.getOriginalPrice(), 0, java.math.RoundingMode.HALF_UP).intValue();
        }
        return VehicleCarePlanCatalogResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .frequencyLabel(p.getFrequencyLabel())
                .washesPerMonth(p.getWashesPerMonth())
                .price(p.getPrice())
                .originalPrice(p.getOriginalPrice())
                .vehicleTypes(p.getVehicleTypes() != null
                        ? Arrays.asList(p.getVehicleTypes().split(",")) : Collections.emptyList())
                .creditsPerCycle(p.getWashesPerMonth())
                .savingsPercent(Math.max(savings, 0))
                .frequency(p.getFrequencyLabel() != null ? p.getFrequencyLabel() : "Monthly")
                .emoji("🚗")
                .popular(p.getSortOrder() == 1)
                .build();
    }

    private VehicleCarePlanResponse toPlanResponse(VehicleCarePlan p) {
        String planName = p.getPlanCatalog() != null ? p.getPlanCatalog().getName() : "Wash Plan";
        String freq = p.getPlanCatalog() != null ? p.getPlanCatalog().getFrequencyLabel() : "Monthly";
        String vNick = p.getVehicle() != null ? p.getVehicle().getNickname() : "";
        String vPlate = p.getVehicle() != null ? p.getVehicle().getPlateNumber() : "";
        String vDisplay = p.getVehicle() != null ? (p.getVehicle().getMake() + " " + p.getVehicle().getModel()) : "";

        return VehicleCarePlanResponse.builder()
                .id(p.getId())
                .planName(planName)
                .frequencyLabel(freq)
                .vehicleNickname(vNick)
                .vehiclePlate(vPlate)
                .status(p.getStatus())
                .creditsTotal(p.getCreditsTotal())
                .creditsUsed(p.getCreditsUsed())
                .creditsRemaining(p.getCreditsTotal() - p.getCreditsUsed())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .name(planName)
                .vehicleDisplay(vDisplay)
                .nextRenewalDate(p.getEndDate() != null ? p.getEndDate().toString() : "")
                .frequency(freq)
                .build();
    }

    private String generateBookingNumber() {
        return "VC-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now())
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
