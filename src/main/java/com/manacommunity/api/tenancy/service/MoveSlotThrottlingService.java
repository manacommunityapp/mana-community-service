package com.manacommunity.api.tenancy.service;

import com.manacommunity.api.tenancy.entity.MoveSlot;
import com.manacommunity.api.tenancy.entity.MoveSlot.SlotStatus;
import com.manacommunity.api.tenancy.entity.MoveSlot.SlotType;
import com.manacommunity.api.tenancy.repository.MoveSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoveSlotThrottlingService {

    private static final int MAX_MOVES_PER_TOWER_PER_DAY = 2;
    private static final LocalTime FREIGHT_START = LocalTime.of(10, 0);
    private static final LocalTime FREIGHT_END = LocalTime.of(17, 0);
    private static final int SLOT_DURATION_HOURS = 2;

    private final MoveSlotRepository slotRepository;

    public SlotAvailability checkAvailability(String tower, LocalDate date, SlotType type) {
        List<MoveSlot> existing = slotRepository.findActiveSlotsByTowerAndDate(tower, date, type);
        int slotsUsed = existing.size();
        boolean available = slotsUsed < MAX_MOVES_PER_TOWER_PER_DAY;

        List<TimeWindow> availableWindows = new ArrayList<>();
        if (available) {
            Set<String> bookedHours = existing.stream()
                    .map(s -> s.getStartTime().toString())
                    .collect(Collectors.toSet());

            LocalTime cursor = FREIGHT_START;
            while (cursor.plusHours(SLOT_DURATION_HOURS).compareTo(FREIGHT_END) <= 0) {
                if (!bookedHours.contains(cursor.toString())) {
                    availableWindows.add(new TimeWindow(cursor, cursor.plusHours(SLOT_DURATION_HOURS)));
                }
                cursor = cursor.plusHours(SLOT_DURATION_HOURS);
            }
        }

        List<String> elevators = getAvailableElevators(tower, date);

        return new SlotAvailability(tower, date, type, slotsUsed, MAX_MOVES_PER_TOWER_PER_DAY,
                available, availableWindows, elevators);
    }

    @Transactional
    public MoveSlot bookSlot(Long communityId, String tower, String flatNumber, Long tenantRecordId,
                              LocalDate date, LocalTime startTime, SlotType type,
                              String vehicleNumber, String vehicleType, String driverName, String driverPhone) {

        // Validate throttle constraint
        List<MoveSlot> existing = slotRepository.findActiveSlotsByTowerAndDate(tower, date, type);
        if (existing.size() >= MAX_MOVES_PER_TOWER_PER_DAY) {
            throw new RuntimeException("Tower " + tower + " already has " + MAX_MOVES_PER_TOWER_PER_DAY +
                    " " + type + " slots on " + date + ". Try another date.");
        }

        // Validate freight elevator hours
        if (startTime.isBefore(FREIGHT_START) || startTime.plusHours(SLOT_DURATION_HOURS).isAfter(FREIGHT_END)) {
            throw new RuntimeException("Heavy furniture movement restricted to " + FREIGHT_START + " – " + FREIGHT_END);
        }

        // Check elevator availability
        String elevatorId = assignElevator(tower, date, startTime);

        MoveSlot slot = MoveSlot.builder()
                .communityId(communityId)
                .towerName(tower)
                .flatNumber(flatNumber)
                .tenantRecordId(tenantRecordId)
                .slotDate(date)
                .startTime(startTime)
                .endTime(startTime.plusHours(SLOT_DURATION_HOURS))
                .slotType(type)
                .serviceElevatorId(elevatorId)
                .status(SlotStatus.APPROVED)
                .vehicleNumber(vehicleNumber)
                .vehicleType(vehicleType)
                .driverName(driverName)
                .driverPhone(driverPhone)
                .gatePassIssued(false)
                .build();

        return slotRepository.save(slot);
    }

    @Transactional
    public MoveSlot issueGatePass(Long slotId) {
        MoveSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        String gatePass = "GP-" + slot.getSlotType().name().substring(0, 2) + "-" +
                slot.getId() + "-" + System.currentTimeMillis() % 10000;
        slot.setGatePassNumber(gatePass);
        slot.setGatePassIssued(true);
        slot.setStatus(SlotStatus.GATE_PASS_ISSUED);

        return slotRepository.save(slot);
    }

    @Transactional
    public MoveSlot validateGatePass(String gatePassNumber) {
        List<MoveSlot> all = slotRepository.findAll();
        MoveSlot slot = all.stream()
                .filter(s -> gatePassNumber.equals(s.getGatePassNumber()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Invalid gate pass: " + gatePassNumber));

        LocalDate today = LocalDate.now();
        boolean dateValid = slot.getSlotDate().equals(today);
        boolean timeValid = !LocalTime.now().isBefore(slot.getStartTime().minusMinutes(30))
                && !LocalTime.now().isAfter(slot.getEndTime().plusMinutes(30));

        if (!dateValid) {
            throw new RuntimeException("Gate pass not valid today. Scheduled for: " + slot.getSlotDate());
        }

        return slot;
    }

    public SlotScheduleSummary getDaySchedule(Long communityId, LocalDate date) {
        List<MoveSlot> slots = slotRepository.findAllSlotsByDate(communityId, date);

        Map<String, List<MoveSlot>> byTower = slots.stream()
                .collect(Collectors.groupingBy(MoveSlot::getTowerName));

        Map<String, TowerCapacity> towerCapacities = new HashMap<>();
        for (String tower : List.of("Tower A", "Tower B", "Tower C", "Tower D")) {
            List<MoveSlot> towerSlots = byTower.getOrDefault(tower, List.of());
            long moveIns = towerSlots.stream().filter(s -> s.getSlotType() == SlotType.MOVE_IN).count();
            long moveOuts = towerSlots.stream().filter(s -> s.getSlotType() == SlotType.MOVE_OUT).count();
            towerCapacities.put(tower, new TowerCapacity(tower, moveIns, moveOuts,
                    MAX_MOVES_PER_TOWER_PER_DAY, towerSlots));
        }

        // Elevator utilization
        Map<String, List<MoveSlot>> byElevator = slots.stream()
                .filter(s -> s.getServiceElevatorId() != null)
                .collect(Collectors.groupingBy(MoveSlot::getServiceElevatorId));

        List<ElevatorUtilization> elevatorUtil = byElevator.entrySet().stream()
                .map(e -> new ElevatorUtilization(e.getKey(), e.getValue().size(),
                        e.getValue().stream().mapToLong(s ->
                                java.time.Duration.between(s.getStartTime(), s.getEndTime()).toHours()).sum()))
                .toList();

        return new SlotScheduleSummary(date, slots.size(), towerCapacities, elevatorUtil, slots);
    }

    private String assignElevator(String tower, LocalDate date, LocalTime startTime) {
        List<String> elevators = List.of(tower + "-SVC-1", tower + "-SVC-2");
        for (String elevator : elevators) {
            List<MoveSlot> bookings = slotRepository.findElevatorBookings(elevator, date);
            boolean conflict = bookings.stream().anyMatch(s ->
                    startTime.isBefore(s.getEndTime()) && startTime.plusHours(SLOT_DURATION_HOURS).isAfter(s.getStartTime()));
            if (!conflict) return elevator;
        }
        return elevators.get(0) + "-OVERFLOW";
    }

    private List<String> getAvailableElevators(String tower, LocalDate date) {
        List<String> elevators = List.of(tower + "-SVC-1", tower + "-SVC-2");
        return elevators.stream()
                .filter(e -> slotRepository.findElevatorBookings(e, date).size() < 3)
                .toList();
    }

    public record TimeWindow(LocalTime start, LocalTime end) {}
    public record SlotAvailability(String tower, LocalDate date, SlotType type, int slotsUsed,
                                    int maxSlots, boolean available, List<TimeWindow> availableWindows,
                                    List<String> availableElevators) {}
    public record TowerCapacity(String tower, long moveIns, long moveOuts, int maxPerDay,
                                 List<MoveSlot> slots) {}
    public record ElevatorUtilization(String elevatorId, int bookings, long hoursBooked) {}
    public record SlotScheduleSummary(LocalDate date, int totalSlots,
                                       Map<String, TowerCapacity> towerCapacities,
                                       List<ElevatorUtilization> elevatorUtilization,
                                       List<MoveSlot> allSlots) {}
}
