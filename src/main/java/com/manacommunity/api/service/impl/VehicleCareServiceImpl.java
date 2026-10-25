package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.service.VehicleCareService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
    public List<VehicleServiceSlotResponse> getAvailableSlots(AppUser user, LocalDate from, LocalDate to) {
        return slotRepo.findByCommunityIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(
                        user.getCommunity().getId(), from, to)
                .stream().map(this::toSlotResponse).collect(Collectors.toList());
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
        VehicleCarePlan plan = planRepo.findByIdAndResident(planId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        plan.setStatus(newStatus);
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
        VehicleCareBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        booking.setStatus(newStatus);
        return toBookingResponse(bookingRepo.save(booking));
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
        return VehicleCareBookingResponse.builder()
                .id(b.getId())
                .bookingNumber(b.getBookingNumber())
                .status(b.getStatus())
                .vehicleId(v.getId())
                .vehicleNickname(v.getNickname())
                .vehicleType(v.getVehicleType())
                .vehiclePlate(v.getPlateNumber())
                .vehicleDisplay(v.getMake() + " " + v.getModel())
                .serviceId(svc.getId())
                .serviceName(svc.getName())
                .durationMinutes(svc.getDurationMinutes())
                .slotId(slot.getId())
                .scheduledDate(slot.getSlotDate())
                .scheduledStart(slot.getStartTime())
                .scheduledEnd(slot.getEndTime())
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
                .build();
    }

    private VehicleCarePlanCatalogResponse toPlanCatalogResponse(VehicleCarePlanCatalog p) {
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
                .build();
    }

    private VehicleCarePlanResponse toPlanResponse(VehicleCarePlan p) {
        return VehicleCarePlanResponse.builder()
                .id(p.getId())
                .planName(p.getPlanCatalog().getName())
                .frequencyLabel(p.getPlanCatalog().getFrequencyLabel())
                .vehicleNickname(p.getVehicle().getNickname())
                .vehiclePlate(p.getVehicle().getPlateNumber())
                .status(p.getStatus())
                .creditsTotal(p.getCreditsTotal())
                .creditsUsed(p.getCreditsUsed())
                .creditsRemaining(p.getCreditsTotal() - p.getCreditsUsed())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .build();
    }

    private String generateBookingNumber() {
        return "VC-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now())
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
