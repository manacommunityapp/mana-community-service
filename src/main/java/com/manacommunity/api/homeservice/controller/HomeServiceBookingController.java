package com.manacommunity.api.homeservice.controller;

import com.manacommunity.api.homeservice.dto.HomeServiceBookingRequest;
import com.manacommunity.api.homeservice.model.entity.HomeServiceBookingEntity;
import com.manacommunity.api.homeservice.model.enums.HomeServiceBookingStatus;
import com.manacommunity.api.homeservice.model.enums.HomeServiceBookingType;
import com.manacommunity.api.homeservice.model.enums.HomeServicePricingModel;
import com.manacommunity.api.homeservice.service.HomeServiceBookingService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController("homeServiceBookingController")
@RequestMapping("/api/v1/home-services/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HomeServiceBookingController {
    private final HomeServiceBookingService bookingService;

    @PostMapping
    public ResponseEntity<HomeServiceBookingEntity> createBooking(
            @RequestBody HomeServiceBookingRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (req.getResidentUserId() == null || req.getResidentUserId().isBlank()) {
            req.setResidentUserId(principal != null ? String.valueOf(principal.getId()) : "1");
        }
        if (req.getCommunityId() == null || req.getCommunityId().isBlank()) {
            req.setCommunityId("comm-mana-1");
        }
        if (req.getFlatNumber() == null || req.getFlatNumber().isBlank()) {
            req.setFlatNumber("A-101");
        }
        if (req.getBookingType() == null) {
            req.setBookingType(HomeServiceBookingType.ONE_TIME);
        }
        if (req.getPricingModel() == null) {
            req.setPricingModel(HomeServicePricingModel.PER_VISIT);
        }
        if (req.getStartDate() == null) {
            req.setStartDate(LocalDate.now());
        }
        if (req.getStartTime() == null) {
            req.setStartTime(LocalTime.of(10, 0));
        }
        if (req.getEndTime() == null) {
            req.setEndTime(LocalTime.of(11, 0));
        }
        if (req.getPrice() == null) {
            req.setPrice(new BigDecimal("350.00"));
        }
        if (req.getCategoryId() == null || req.getCategoryId().isBlank()) {
            req.setCategoryId("PLUMBING");
        }

        return ResponseEntity.ok(bookingService.createBooking(req));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<HomeServiceBookingEntity>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal principal) {
        String residentId = principal != null ? String.valueOf(principal.getId()) : "1";
        return ResponseEntity.ok(bookingService.getBookingsByResident(residentId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<HomeServiceBookingEntity> updateStatus(
            @PathVariable String id,
            @RequestParam HomeServiceBookingStatus status,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, status, reason));
    }

    @GetMapping("/resident/{residentId}")
    public ResponseEntity<List<HomeServiceBookingEntity>> getByResident(@PathVariable String residentId) {
        return ResponseEntity.ok(bookingService.getBookingsByResident(residentId));
    }

    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<HomeServiceBookingEntity>> getByWorker(@PathVariable String workerId) {
        return ResponseEntity.ok(bookingService.getBookingsByWorker(workerId));
    }
}

