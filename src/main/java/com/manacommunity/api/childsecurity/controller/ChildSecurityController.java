package com.manacommunity.api.childsecurity.controller;

import com.manacommunity.api.childsecurity.entity.*;
import com.manacommunity.api.childsecurity.entity.BusBoardingLog.*;
import com.manacommunity.api.childsecurity.entity.BusGateEvent.GateEventType;
import com.manacommunity.api.childsecurity.entity.ChildExitRequest.*;
import com.manacommunity.api.childsecurity.entity.ChildProfile.ChildStatus;
import com.manacommunity.api.childsecurity.entity.EscalationLog.*;
import com.manacommunity.api.childsecurity.entity.GuardianAuthorization.*;
import com.manacommunity.api.childsecurity.entity.SchoolBusRoute.BusStatus;
import com.manacommunity.api.childsecurity.repository.*;
import com.manacommunity.api.childsecurity.service.BusNotificationPipeline;
import com.manacommunity.api.childsecurity.service.ExitEscalationStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/child-security")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChildSecurityController {

    private final ExitEscalationStateMachine escalationService;
    private final BusNotificationPipeline busService;
    private final ChildProfileRepository childRepo;
    private final GuardianAuthorizationRepository guardianRepo;
    private final ChildExitRequestRepository exitRepo;
    private final EscalationLogRepository escalationRepo;
    private final SchoolBusRouteRepository busRouteRepo;
    private final BusBoardingLogRepository boardingRepo;
    private final BusGateEventRepository gateEventRepo;

    @GetMapping("/exit/dashboard")
    public ResponseEntity<?> getExitDashboard() {
        return ResponseEntity.ok(escalationService.getDashboard());
    }

    @GetMapping("/bus/dashboard")
    public ResponseEntity<?> getBusDashboard() {
        return ResponseEntity.ok(busService.getDashboard());
    }

    @PostMapping("/exit/request")
    public ResponseEntity<?> initiateExit(@RequestParam Long childId, @RequestParam String guard,
                                            @RequestParam String gate, @RequestParam String exitType,
                                            @RequestParam(required = false) String companionName,
                                            @RequestParam(required = false) String companionPhone,
                                            @RequestParam(required = false) String companionRelation) {
        return ResponseEntity.ok(escalationService.initiateExitRequest(childId, guard, gate,
                ExitType.valueOf(exitType), companionName, companionPhone, companionRelation));
    }

    @PostMapping("/exit/{requestId}/respond")
    public ResponseEntity<?> guardianRespond(@PathVariable Long requestId,
                                               @RequestParam Long guardianId, @RequestParam boolean approved) {
        return ResponseEntity.ok(escalationService.guardianRespond(requestId, guardianId, approved));
    }

    @PostMapping("/exit/{requestId}/timeout")
    public ResponseEntity<?> processTimeout(@PathVariable Long requestId) {
        return ResponseEntity.ok(escalationService.processTimeout(requestId));
    }

    @PostMapping("/bus/rfid-scan")
    public ResponseEntity<?> rfidScan(@RequestParam String rfidTagId, @RequestParam String gateId,
                                        @RequestParam String guard) {
        return ResponseEntity.ok(busService.processRfidGateEntry(rfidTagId, gateId, guard));
    }

    @PostMapping("/bus/boarding/{boardingId}/confirm")
    public ResponseEntity<?> confirmDropoff(@PathVariable Long boardingId, @RequestParam String confirmedBy) {
        return ResponseEntity.ok(busService.confirmDropoff(boardingId, confirmedBy));
    }

    @GetMapping("/children")
    public ResponseEntity<?> getAllChildren() {
        return ResponseEntity.ok(childRepo.findAll());
    }

    @GetMapping("/bus/routes")
    public ResponseEntity<?> getAllRoutes() {
        return ResponseEntity.ok(busRouteRepo.findAll());
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<?> seedDemoData() {
        // --- School Bus Routes ---
        SchoolBusRoute route1 = busRouteRepo.save(SchoolBusRoute.builder()
                .communityId("COMM-001").routeCode("RT-DPS-01").schoolName("Delhi Public School")
                .busNumber("KA-01-MN-5678").driverName("Ramesh Yadav").driverPhone("9876500001")
                .attendantName("Sunita Didi").attendantPhone("9876500002")
                .rfidTagId("RFID-BUS-001")
                .morningPickupTime(LocalTime.of(7, 15)).afternoonDropTime(LocalTime.of(14, 30))
                .totalChildren(12).isActive(true).currentStatus(BusStatus.AT_GATE).build());

        SchoolBusRoute route2 = busRouteRepo.save(SchoolBusRoute.builder()
                .communityId("COMM-001").routeCode("RT-RIS-01").schoolName("Ryan International")
                .busNumber("KA-01-MN-9012").driverName("Suresh Kumar").driverPhone("9876500003")
                .attendantName("Meena Didi").attendantPhone("9876500004")
                .rfidTagId("RFID-BUS-002")
                .morningPickupTime(LocalTime.of(7, 30)).afternoonDropTime(LocalTime.of(15, 0))
                .totalChildren(8).isActive(true).currentStatus(BusStatus.EN_ROUTE_DROP).build());

        SchoolBusRoute route3 = busRouteRepo.save(SchoolBusRoute.builder()
                .communityId("COMM-001").routeCode("RT-KEN-01").schoolName("Kendriya Vidyalaya")
                .busNumber("KA-01-MN-3456").driverName("Mohan Singh").driverPhone("9876500005")
                .attendantName("Priya Didi").attendantPhone("9876500006")
                .rfidTagId("RFID-BUS-003")
                .morningPickupTime(LocalTime.of(7, 0)).afternoonDropTime(LocalTime.of(14, 0))
                .totalChildren(15).isActive(true).currentStatus(BusStatus.COMPLETED).build());

        // --- Children ---
        ChildProfile aarav = childRepo.save(ChildProfile.builder()
                .communityId("COMM-001").childName("Aarav Sharma").dateOfBirth(LocalDate.of(2018, 3, 15))
                .age(8).flatNumber("A-101").towerName("Tower A").photoUrl("/children/aarav.jpg")
                .schoolName("Delhi Public School").schoolGrade("Grade 3")
                .busRouteId(route1.getId()).busRouteCode("RT-DPS-01")
                .exitLockEnabled(true).busTrackingEnabled(true).status(ChildStatus.ACTIVE).build());

        ChildProfile ananya = childRepo.save(ChildProfile.builder()
                .communityId("COMM-001").childName("Ananya Sharma").dateOfBirth(LocalDate.of(2020, 7, 20))
                .age(6).flatNumber("A-101").towerName("Tower A").photoUrl("/children/ananya.jpg")
                .schoolName("Delhi Public School").schoolGrade("Grade 1")
                .busRouteId(route1.getId()).busRouteCode("RT-DPS-01")
                .exitLockEnabled(true).busTrackingEnabled(true).status(ChildStatus.ACTIVE).build());

        ChildProfile vihaan = childRepo.save(ChildProfile.builder()
                .communityId("COMM-001").childName("Vihaan Gupta").dateOfBirth(LocalDate.of(2017, 11, 5))
                .age(9).flatNumber("B-204").towerName("Tower B").photoUrl("/children/vihaan.jpg")
                .schoolName("Ryan International").schoolGrade("Grade 4")
                .busRouteId(route2.getId()).busRouteCode("RT-RIS-01")
                .exitLockEnabled(true).busTrackingEnabled(true).status(ChildStatus.ACTIVE).build());

        ChildProfile isha = childRepo.save(ChildProfile.builder()
                .communityId("COMM-001").childName("Isha Patel").dateOfBirth(LocalDate.of(2019, 1, 12))
                .age(7).flatNumber("C-302").towerName("Tower C").photoUrl("/children/isha.jpg")
                .schoolName("Kendriya Vidyalaya").schoolGrade("Grade 2")
                .busRouteId(route3.getId()).busRouteCode("RT-KEN-01")
                .exitLockEnabled(true).busTrackingEnabled(true).status(ChildStatus.ACTIVE).build());

        ChildProfile arjun = childRepo.save(ChildProfile.builder()
                .communityId("COMM-001").childName("Arjun Singh").dateOfBirth(LocalDate.of(2016, 5, 8))
                .age(10).flatNumber("D-401").towerName("Tower D").photoUrl("/children/arjun.jpg")
                .schoolName("Delhi Public School").schoolGrade("Grade 5")
                .busRouteId(route1.getId()).busRouteCode("RT-DPS-01")
                .exitLockEnabled(false).busTrackingEnabled(true).status(ChildStatus.ACTIVE).build());

        // --- Guardian Authorizations ---
        // Aarav & Ananya's guardians (same family)
        GuardianAuthorization g1 = guardianRepo.save(GuardianAuthorization.builder()
                .childId(aarav.getId()).communityId("COMM-001").guardianName("Priya Sharma")
                .guardianPhone("9876543001").guardianEmail("priya.sharma@email.com")
                .relationship("Mother").role(GuardianRole.PRIMARY_PARENT)
                .authLevel(AuthorizationLevel.FULL_APPROVE).escalationOrder(1)
                .ivrEnabled(true).pushNotificationEnabled(true).isActive(true).build());

        guardianRepo.save(GuardianAuthorization.builder()
                .childId(aarav.getId()).communityId("COMM-001").guardianName("Rahul Sharma")
                .guardianPhone("9876543002").guardianEmail("rahul.sharma@email.com")
                .relationship("Father").role(GuardianRole.SECONDARY_PARENT)
                .authLevel(AuthorizationLevel.FULL_APPROVE).escalationOrder(2)
                .ivrEnabled(true).pushNotificationEnabled(true).isActive(true).build());

        guardianRepo.save(GuardianAuthorization.builder()
                .childId(aarav.getId()).communityId("COMM-001").guardianName("Kamla Sharma")
                .guardianPhone("9876543003").relationship("Grandmother")
                .role(GuardianRole.GRANDPARENT).authLevel(AuthorizationLevel.APPROVE_WITH_OTP)
                .escalationOrder(3).ivrEnabled(false).pushNotificationEnabled(true).isActive(true).build());

        guardianRepo.save(GuardianAuthorization.builder()
                .childId(aarav.getId()).communityId("COMM-001").guardianName("Vikram Uncle")
                .guardianPhone("9876543004").relationship("Uncle")
                .role(GuardianRole.EMERGENCY_CONTACT).authLevel(AuthorizationLevel.EMERGENCY_ONLY)
                .escalationOrder(4).ivrEnabled(true).pushNotificationEnabled(false).isActive(true).build());

        // Same guardians for Ananya
        guardianRepo.save(GuardianAuthorization.builder()
                .childId(ananya.getId()).communityId("COMM-001").guardianName("Priya Sharma")
                .guardianPhone("9876543001").relationship("Mother")
                .role(GuardianRole.PRIMARY_PARENT).authLevel(AuthorizationLevel.FULL_APPROVE)
                .escalationOrder(1).ivrEnabled(true).pushNotificationEnabled(true).isActive(true).build());

        guardianRepo.save(GuardianAuthorization.builder()
                .childId(ananya.getId()).communityId("COMM-001").guardianName("Rahul Sharma")
                .guardianPhone("9876543002").relationship("Father")
                .role(GuardianRole.SECONDARY_PARENT).authLevel(AuthorizationLevel.FULL_APPROVE)
                .escalationOrder(2).ivrEnabled(true).pushNotificationEnabled(true).isActive(true).build());

        // Vihaan's guardians
        guardianRepo.save(GuardianAuthorization.builder()
                .childId(vihaan.getId()).communityId("COMM-001").guardianName("Neha Gupta")
                .guardianPhone("9876543005").relationship("Mother")
                .role(GuardianRole.PRIMARY_PARENT).authLevel(AuthorizationLevel.FULL_APPROVE)
                .escalationOrder(1).ivrEnabled(true).pushNotificationEnabled(true).isActive(true).build());

        guardianRepo.save(GuardianAuthorization.builder()
                .childId(vihaan.getId()).communityId("COMM-001").guardianName("Amit Gupta")
                .guardianPhone("9876543006").relationship("Father")
                .role(GuardianRole.SECONDARY_PARENT).authLevel(AuthorizationLevel.FULL_APPROVE)
                .escalationOrder(2).ivrEnabled(true).pushNotificationEnabled(true).isActive(true).build());

        // --- Exit Requests (demo scenarios) ---
        // Scenario 1: Aarav tries to exit — approved by mother
        ChildExitRequest exit1 = exitRepo.save(ChildExitRequest.builder()
                .childId(aarav.getId()).childName("Aarav Sharma").communityId("COMM-001")
                .flatNumber("A-101").towerName("Tower A")
                .exitType(ExitType.WITH_AUTHORIZED_PICKUP).status(ExitRequestStatus.APPROVED)
                .escalationState(EscalationState.GUARDIAN_RESPONDED)
                .guardOnDuty("Guard Rajan").gateId("GATE-MAIN")
                .requestedAt(LocalDateTime.of(2026, 11, 28, 16, 30))
                .primaryNotifiedAt(LocalDateTime.of(2026, 11, 28, 16, 30))
                .approvedAt(LocalDateTime.of(2026, 11, 28, 16, 31))
                .approvedByGuardianId(g1.getId()).approvedByName("Priya Sharma")
                .approvalMethod("PRIMARY_PARENT")
                .companionName("Meera Aunty").companionPhone("9876599001")
                .companionRelation("Family friend")
                .escalationTimeoutSeconds(60).totalEscalationAttempts(1).build());

        // Scenario 2: Ananya tries unaccompanied exit — denied after full escalation
        ChildExitRequest exit2 = exitRepo.save(ChildExitRequest.builder()
                .childId(ananya.getId()).childName("Ananya Sharma").communityId("COMM-001")
                .flatNumber("A-101").towerName("Tower A")
                .exitType(ExitType.UNACCOMPANIED).status(ExitRequestStatus.AUTO_LOCKED)
                .escalationState(EscalationState.ALL_ESCALATIONS_EXHAUSTED)
                .guardOnDuty("Guard Rajan").gateId("GATE-MAIN")
                .requestedAt(LocalDateTime.of(2026, 11, 27, 17, 0))
                .primaryNotifiedAt(LocalDateTime.of(2026, 11, 27, 17, 0))
                .escalatedAt(LocalDateTime.of(2026, 11, 27, 17, 1))
                .ivrTriggeredAt(LocalDateTime.of(2026, 11, 27, 17, 2))
                .escalationTimeoutSeconds(60).totalEscalationAttempts(4)
                .remarks("All escalation attempts exhausted. Child held at gate.").build());

        // Scenario 3: Vihaan — pending exit, awaiting primary parent
        ChildExitRequest exit3 = exitRepo.save(ChildExitRequest.builder()
                .childId(vihaan.getId()).childName("Vihaan Gupta").communityId("COMM-001")
                .flatNumber("B-204").towerName("Tower B")
                .exitType(ExitType.WITH_GUARDIAN).status(ExitRequestStatus.PENDING)
                .escalationState(EscalationState.AWAITING_PRIMARY)
                .guardOnDuty("Guard Sunil").gateId("GATE-SIDE")
                .requestedAt(LocalDateTime.now())
                .primaryNotifiedAt(LocalDateTime.now())
                .companionName("Neha Gupta").companionPhone("9876543005")
                .companionRelation("Mother")
                .escalationTimeoutSeconds(60).totalEscalationAttempts(1).build());

        // --- Escalation Logs ---
        escalationRepo.saveAll(List.of(
                EscalationLog.builder().exitRequestId(exit1.getId()).guardianId(g1.getId())
                        .guardianName("Priya Sharma").guardianPhone("9876543001")
                        .method(EscalationMethod.PUSH_NOTIFICATION).outcome(EscalationOutcome.APPROVED)
                        .initiatedAt(LocalDateTime.of(2026, 11, 28, 16, 30))
                        .respondedAt(LocalDateTime.of(2026, 11, 28, 16, 31))
                        .responseTimeSeconds(45).attemptNumber(1).build(),
                EscalationLog.builder().exitRequestId(exit2.getId()).guardianId(g1.getId())
                        .guardianName("Priya Sharma").guardianPhone("9876543001")
                        .method(EscalationMethod.PUSH_NOTIFICATION).outcome(EscalationOutcome.NO_RESPONSE)
                        .initiatedAt(LocalDateTime.of(2026, 11, 27, 17, 0))
                        .attemptNumber(1).responseTimeSeconds(0).build(),
                EscalationLog.builder().exitRequestId(exit2.getId()).guardianId(g1.getId() + 1)
                        .guardianName("Rahul Sharma").guardianPhone("9876543002")
                        .method(EscalationMethod.PUSH_NOTIFICATION).outcome(EscalationOutcome.NO_RESPONSE)
                        .initiatedAt(LocalDateTime.of(2026, 11, 27, 17, 1))
                        .attemptNumber(2).responseTimeSeconds(0).build(),
                EscalationLog.builder().exitRequestId(exit2.getId()).guardianId(g1.getId())
                        .guardianName("Priya Sharma").guardianPhone("9876543001")
                        .method(EscalationMethod.IVR_CALL).outcome(EscalationOutcome.CALL_FAILED)
                        .initiatedAt(LocalDateTime.of(2026, 11, 27, 17, 2))
                        .attemptNumber(3).ivrCallSid("IVR-abc12345").responseTimeSeconds(0).build(),
                EscalationLog.builder().exitRequestId(exit2.getId()).guardianId(g1.getId() + 3)
                        .guardianName("Vikram Uncle").guardianPhone("9876543004")
                        .method(EscalationMethod.IVR_CALL).outcome(EscalationOutcome.VOICEMAIL)
                        .initiatedAt(LocalDateTime.of(2026, 11, 27, 17, 3))
                        .attemptNumber(4).ivrCallSid("IVR-def67890").responseTimeSeconds(0).build()
        ));

        // --- Bus boarding logs ---
        boardingRepo.saveAll(List.of(
                BusBoardingLog.builder().busRouteId(route1.getId()).childId(aarav.getId())
                        .childName("Aarav Sharma").communityId("COMM-001")
                        .boardingDate(LocalDate.now()).boardingType(BoardingType.AFTERNOON_DROP)
                        .boardingStatus(BoardingStatus.DROPPED_SAFE)
                        .boardedAt(LocalDateTime.now().minusHours(1))
                        .droppedAt(LocalDateTime.now().minusMinutes(30))
                        .droppedConfirmedBy("Guard Rajan").parentNotified(true)
                        .parentNotifiedAt(LocalDateTime.now().minusMinutes(30)).build(),
                BusBoardingLog.builder().busRouteId(route1.getId()).childId(ananya.getId())
                        .childName("Ananya Sharma").communityId("COMM-001")
                        .boardingDate(LocalDate.now()).boardingType(BoardingType.AFTERNOON_DROP)
                        .boardingStatus(BoardingStatus.DROPPED_SAFE)
                        .boardedAt(LocalDateTime.now().minusHours(1))
                        .droppedAt(LocalDateTime.now().minusMinutes(28))
                        .droppedConfirmedBy("Guard Rajan").parentNotified(true)
                        .parentNotifiedAt(LocalDateTime.now().minusMinutes(28)).build(),
                BusBoardingLog.builder().busRouteId(route2.getId()).childId(vihaan.getId())
                        .childName("Vihaan Gupta").communityId("COMM-001")
                        .boardingDate(LocalDate.now()).boardingType(BoardingType.AFTERNOON_DROP)
                        .boardingStatus(BoardingStatus.BOARDED)
                        .boardedAt(LocalDateTime.now().minusMinutes(15))
                        .parentNotified(false).build(),
                BusBoardingLog.builder().busRouteId(route3.getId()).childId(isha.getId())
                        .childName("Isha Patel").communityId("COMM-001")
                        .boardingDate(LocalDate.now()).boardingType(BoardingType.AFTERNOON_DROP)
                        .boardingStatus(BoardingStatus.DROPPED_SAFE)
                        .boardedAt(LocalDateTime.now().minusHours(2))
                        .droppedAt(LocalDateTime.now().minusHours(1))
                        .droppedConfirmedBy("Guard Sunil").parentNotified(true)
                        .parentNotifiedAt(LocalDateTime.now().minusHours(1)).build(),
                BusBoardingLog.builder().busRouteId(route1.getId()).childId(arjun.getId())
                        .childName("Arjun Singh").communityId("COMM-001")
                        .boardingDate(LocalDate.now()).boardingType(BoardingType.AFTERNOON_DROP)
                        .boardingStatus(BoardingStatus.PARENT_PICKUP)
                        .boardedAt(null).parentNotified(false)
                        .remarks("Parent picked up directly from school").build()
        ));

        // --- Bus gate events ---
        gateEventRepo.saveAll(List.of(
                BusGateEvent.builder().busRouteId(route1.getId()).communityId("COMM-001")
                        .rfidTagId("RFID-BUS-001").gateId("GATE-MAIN")
                        .eventType(GateEventType.RFID_ENTRY)
                        .scannedAt(LocalDateTime.now().minusMinutes(35))
                        .childrenOnBoard(3).childrenDropped(2)
                        .allParentsNotified(true).notificationsSentAt(LocalDateTime.now().minusMinutes(35))
                        .guardOnDuty("Guard Rajan").build(),
                BusGateEvent.builder().busRouteId(route3.getId()).communityId("COMM-001")
                        .rfidTagId("RFID-BUS-003").gateId("GATE-MAIN")
                        .eventType(GateEventType.RFID_ENTRY)
                        .scannedAt(LocalDateTime.now().minusHours(1).minusMinutes(10))
                        .childrenOnBoard(1).childrenDropped(1)
                        .allParentsNotified(true).notificationsSentAt(LocalDateTime.now().minusHours(1).minusMinutes(10))
                        .guardOnDuty("Guard Sunil").build()
        ));

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Demo data seeded successfully");
        result.put("children", List.of(aarav, ananya, vihaan, isha, arjun));
        result.put("busRoutes", List.of(route1, route2, route3));
        result.put("exitRequests", 3);
        result.put("escalationLogs", 5);
        result.put("boardingLogs", 5);
        result.put("gateEvents", 2);
        return ResponseEntity.ok(result);
    }
}
