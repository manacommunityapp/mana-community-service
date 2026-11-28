package com.manacommunity.api.childsecurity.service;

import com.manacommunity.api.childsecurity.entity.*;
import com.manacommunity.api.childsecurity.entity.BusBoardingLog.*;
import com.manacommunity.api.childsecurity.entity.BusGateEvent.GateEventType;
import com.manacommunity.api.childsecurity.entity.SchoolBusRoute.BusStatus;
import com.manacommunity.api.childsecurity.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusNotificationPipeline {

    private final SchoolBusRouteRepository busRepo;
    private final ChildProfileRepository childRepo;
    private final BusBoardingLogRepository boardingRepo;
    private final BusGateEventRepository gateEventRepo;
    private final GuardianAuthorizationRepository guardianRepo;

    @Transactional
    public BusArrivalResult processRfidGateEntry(String rfidTagId, String gateId, String guardOnDuty) {
        SchoolBusRoute bus = busRepo.findByRfidTagId(rfidTagId)
                .orElseThrow(() -> new RuntimeException("Unknown RFID tag: " + rfidTagId));

        bus.setCurrentStatus(BusStatus.AT_GATE);
        busRepo.save(bus);

        List<ChildProfile> childrenOnRoute = childRepo.findByBusRouteId(bus.getId());

        BusGateEvent event = BusGateEvent.builder()
                .busRouteId(bus.getId()).communityId(bus.getCommunityId())
                .rfidTagId(rfidTagId).gateId(gateId)
                .eventType(GateEventType.RFID_ENTRY)
                .scannedAt(LocalDateTime.now())
                .childrenOnBoard(childrenOnRoute.size())
                .guardOnDuty(guardOnDuty).build();

        // Create boarding logs for afternoon drop
        List<BusBoardingLog> boardings = new ArrayList<>();
        for (ChildProfile child : childrenOnRoute) {
            boardings.add(BusBoardingLog.builder()
                    .busRouteId(bus.getId()).childId(child.getId())
                    .childName(child.getChildName()).communityId(bus.getCommunityId())
                    .boardingDate(LocalDate.now()).boardingType(BoardingType.AFTERNOON_DROP)
                    .boardingStatus(BoardingStatus.BOARDED)
                    .boardedAt(LocalDateTime.now()).build());
        }
        boardingRepo.saveAll(boardings);

        // Notify ONLY parents whose children ride this route
        List<ParentNotification> notifications = new ArrayList<>();
        for (ChildProfile child : childrenOnRoute) {
            List<GuardianAuthorization> guardians = guardianRepo.findByChildIdAndIsActiveTrue(child.getId());
            for (GuardianAuthorization g : guardians) {
                if (g.isPushNotificationEnabled()) {
                    notifications.add(new ParentNotification(
                            child.getId(), child.getChildName(), child.getFlatNumber(),
                            g.getGuardianName(), g.getGuardianPhone(),
                            "Bus " + bus.getBusNumber() + " (" + bus.getSchoolName() + ") has arrived at gate " + gateId
                    ));
                }
            }
        }

        event.setChildrenDropped(0);
        event.setAllParentsNotified(true);
        event.setNotificationsSentAt(LocalDateTime.now());
        gateEventRepo.save(event);

        return new BusArrivalResult(bus, event, childrenOnRoute.size(),
                notifications.size(), notifications, boardings);
    }

    @Transactional
    public BusBoardingLog confirmDropoff(Long boardingId, String confirmedBy) {
        BusBoardingLog boarding = boardingRepo.findById(boardingId)
                .orElseThrow(() -> new RuntimeException("Boarding log not found"));

        boarding.setBoardingStatus(BoardingStatus.DROPPED_SAFE);
        boarding.setDroppedAt(LocalDateTime.now());
        boarding.setDroppedConfirmedBy(confirmedBy);
        boarding.setParentNotified(true);
        boarding.setParentNotifiedAt(LocalDateTime.now());

        return boardingRepo.save(boarding);
    }

    public BusDashboard getDashboard() {
        List<SchoolBusRoute> routes = busRepo.findAll();
        List<BusBoardingLog> allBoardings = boardingRepo.findAll();
        List<BusGateEvent> allEvents = gateEventRepo.findAll();
        List<ChildProfile> busChildren = childRepo.findAll().stream()
                .filter(c -> c.getBusRouteId() != null).toList();

        long activeRoutes = routes.stream().filter(SchoolBusRoute::isActive).count();
        long todayBoardings = allBoardings.stream()
                .filter(b -> b.getBoardingDate().equals(LocalDate.now())).count();
        long safeDropoffs = allBoardings.stream()
                .filter(b -> b.getBoardingStatus() == BoardingStatus.DROPPED_SAFE).count();
        long pendingDropoffs = allBoardings.stream()
                .filter(b -> b.getBoardingStatus() == BoardingStatus.BOARDED
                        && b.getBoardingType() == BoardingType.AFTERNOON_DROP).count();

        Map<String, Long> boardingsByStatus = allBoardings.stream()
                .collect(Collectors.groupingBy(b -> b.getBoardingStatus().name(), Collectors.counting()));

        Map<String, Long> childrenBySchool = busChildren.stream()
                .collect(Collectors.groupingBy(ChildProfile::getSchoolName, Collectors.counting()));

        return new BusDashboard(routes.size(), activeRoutes, busChildren.size(),
                todayBoardings, safeDropoffs, pendingDropoffs,
                boardingsByStatus, childrenBySchool,
                routes, allBoardings, allEvents);
    }

    public record ParentNotification(Long childId, String childName, String flatNumber,
                                       String guardianName, String guardianPhone, String message) {}

    public record BusArrivalResult(SchoolBusRoute bus, BusGateEvent gateEvent,
                                     int childrenOnRoute, int notificationsSent,
                                     List<ParentNotification> notifications,
                                     List<BusBoardingLog> boardingLogs) {}

    public record BusDashboard(int totalRoutes, long activeRoutes, int busChildren,
                                 long todayBoardings, long safeDropoffs, long pendingDropoffs,
                                 Map<String, Long> boardingsByStatus, Map<String, Long> childrenBySchool,
                                 List<SchoolBusRoute> routes, List<BusBoardingLog> boardings,
                                 List<BusGateEvent> gateEvents) {}
}
