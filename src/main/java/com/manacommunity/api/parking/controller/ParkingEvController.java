package com.manacommunity.api.parking.controller;

import com.manacommunity.api.parking.entity.EvCharger;
import com.manacommunity.api.parking.entity.EvChargingSession;
import com.manacommunity.api.parking.entity.ParkingBay;
import com.manacommunity.api.parking.entity.ParkingBooking;
import com.manacommunity.api.parking.repository.EvChargerRepository;
import com.manacommunity.api.parking.repository.ParkingBayRepository;
import com.manacommunity.api.parking.service.EvChargerQueueService;
import com.manacommunity.api.parking.service.VisitorParkingService;
import com.manacommunity.api.parking.service.WrongParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parking-ev")
@RequiredArgsConstructor
public class ParkingEvController {

    private final VisitorParkingService visitorService;
    private final EvChargerQueueService evService;
    private final WrongParkingService violationService;
    private final ParkingBayRepository bayRepository;
    private final EvChargerRepository chargerRepository;

    // === Visitor Parking ===

    @PostMapping("/visitor/allocate")
    public ResponseEntity<VisitorParkingService.AllocationResult> allocateVisitor(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(visitorService.allocateVisitorBay(
                Long.valueOf(body.getOrDefault("communityId", "1")),
                body.get("vehicleNumber"), body.get("vehicleType"),
                body.get("visitorName"), body.get("hostFlat"),
                body.get("preferredBasement")));
    }

    @PostMapping("/visitor/release")
    public ResponseEntity<VisitorParkingService.ReleaseResult> releaseParking(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(visitorService.releaseParking(
                Long.valueOf(body.getOrDefault("communityId", "1")),
                body.get("vehicleNumber")));
    }

    @GetMapping("/status")
    public ResponseEntity<VisitorParkingService.ParkingStatus> getStatus(@RequestParam(defaultValue = "1") Long communityId) {
        return ResponseEntity.ok(visitorService.getStatus(communityId));
    }

    @GetMapping("/bays")
    public ResponseEntity<List<ParkingBay>> getBays(@RequestParam(defaultValue = "1") Long communityId) {
        return ResponseEntity.ok(bayRepository.findByCommunityId(communityId));
    }

    @PostMapping("/bays")
    public ResponseEntity<ParkingBay> createBay(@RequestBody ParkingBay bay) {
        return ResponseEntity.ok(bayRepository.save(bay));
    }

    // === EV Charging ===

    @PostMapping("/ev/reserve")
    public ResponseEntity<EvChargerQueueService.ReservationResult> reserveCharger(@RequestBody Map<String, String> body) {
        Long preferredCharger = body.containsKey("preferredChargerId") ? Long.valueOf(body.get("preferredChargerId")) : null;
        return ResponseEntity.ok(evService.reserveCharger(
                Long.valueOf(body.getOrDefault("communityId", "1")),
                body.get("flatNumber"), Long.valueOf(body.getOrDefault("userId", "1")),
                body.get("vehicleNumber"), preferredCharger));
    }

    @PostMapping("/ev/end-session/{sessionId}")
    public ResponseEntity<EvChargerQueueService.SessionEndResult> endSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(evService.endSession(sessionId));
    }

    @PostMapping("/ev/auto-cutoff")
    public ResponseEntity<List<EvChargerQueueService.AutoCutoffResult>> autoCutoff(@RequestParam(defaultValue = "1") Long communityId) {
        return ResponseEntity.ok(evService.runAutoCutoff(communityId));
    }

    @GetMapping("/ev/chargers")
    public ResponseEntity<List<EvCharger>> getChargers(@RequestParam(defaultValue = "1") Long communityId) {
        return ResponseEntity.ok(evService.getChargers(communityId));
    }

    @GetMapping("/ev/unbilled")
    public ResponseEntity<List<EvChargingSession>> getUnbilled(
            @RequestParam(defaultValue = "1") Long communityId, @RequestParam String flatNumber) {
        return ResponseEntity.ok(evService.getUnbilledSessions(communityId, flatNumber));
    }

    @PostMapping("/ev/chargers")
    public ResponseEntity<EvCharger> createCharger(@RequestBody EvCharger charger) {
        return ResponseEntity.ok(chargerRepository.save(charger));
    }

    // === Wrong Parking & Violations ===

    @PostMapping("/violation/report")
    public ResponseEntity<WrongParkingService.ViolationResult> reportViolation(@RequestBody Map<String, String> body) {
        Long scannedBayId = body.containsKey("scannedBayId") ? Long.valueOf(body.get("scannedBayId")) : null;
        return ResponseEntity.ok(violationService.reportViolation(
                Long.valueOf(body.getOrDefault("communityId", "1")),
                body.get("vehicleNumber"), body.get("violationType"),
                scannedBayId, Long.valueOf(body.getOrDefault("guardId", "1"))));
    }

    @GetMapping("/violation/check")
    public ResponseEntity<WrongParkingService.ViolationCheckResult> checkVehicle(
            @RequestParam(defaultValue = "1") Long communityId, @RequestParam String vehicleNumber) {
        return ResponseEntity.ok(violationService.checkVehicle(communityId, vehicleNumber));
    }

    @GetMapping("/violations")
    public ResponseEntity<List<ParkingBooking>> getViolations(@RequestParam(defaultValue = "1") Long communityId) {
        return ResponseEntity.ok(violationService.getActiveViolations(communityId));
    }
}
