package com.manacommunity.api.homeservice.controller;

import com.manacommunity.api.homeservice.dto.HomeServiceBookingRequest;
import com.manacommunity.api.homeservice.model.entity.HomeServiceBookingEntity;
import com.manacommunity.api.homeservice.model.enums.HomeServiceBookingStatus;
import com.manacommunity.api.homeservice.service.HomeServiceBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController("homeServiceBookingController")
@RequestMapping("/api/v1/home-services/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HomeServiceBookingController {
    private final HomeServiceBookingService bookingService;

    @PostMapping
    public ResponseEntity<HomeServiceBookingEntity> createBooking(@RequestBody HomeServiceBookingRequest req) {
        return ResponseEntity.ok(bookingService.createBooking(req));
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
