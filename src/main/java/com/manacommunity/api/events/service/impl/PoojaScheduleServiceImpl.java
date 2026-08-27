package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.dto.PoojaScheduleDto;
import com.manacommunity.api.events.dto.PoojaScheduleRequest;
import com.manacommunity.api.events.entity.EventPoojaSchedule;
import com.manacommunity.api.events.entity.EventPoojaSeva;
import com.manacommunity.api.events.enums.PoojaScheduleStatus;
import com.manacommunity.api.events.repository.EventPoojaUserRegistrationRepository;
import com.manacommunity.api.events.repository.EventPoojaScheduleRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.events.repository.EventPoojaSlotReservationRepository;
import com.manacommunity.api.events.service.PoojaScheduleService;
import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PoojaScheduleServiceImpl implements PoojaScheduleService {

    private final EventPoojaScheduleRepository scheduleRepo;
    private final PoojaSevaRepository poojaSevaRepo;
    private final EventPoojaSlotReservationRepository reservationRepo;
    private final EventPoojaUserRegistrationRepository registrationRepo;
    private final AuditService auditService;

    public PoojaScheduleServiceImpl(EventPoojaScheduleRepository scheduleRepo,
                                    PoojaSevaRepository poojaSevaRepo,
                                    EventPoojaSlotReservationRepository reservationRepo,
                                    EventPoojaUserRegistrationRepository registrationRepo,
                                    AuditService auditService) {
        this.scheduleRepo = scheduleRepo;
        this.poojaSevaRepo = poojaSevaRepo;
        this.reservationRepo = reservationRepo;
        this.registrationRepo = registrationRepo;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public PoojaScheduleDto createSchedule(PoojaScheduleRequest req) {
        EventPoojaSeva seva = poojaSevaRepo.findById(req.getPoojaId())
                .orElseThrow(() -> new ResourceNotFoundException("EventPoojaSeva", req.getPoojaId()));

        // G-2: Reject slots whose date+time is already in the past
        validateScheduleNotInPast(req.getScheduleDate(), req.getStartTime());

        EventPoojaSchedule schedule = EventPoojaSchedule.builder()
                .poojaSeva(seva)
                .communityId(seva.getCommunityId())
                .scheduleDate(req.getScheduleDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .familyCapacity(req.getFamilyCapacity() != null ? req.getFamilyCapacity() : 10)
                .devoteeCapacity(req.getDevoteeCapacity() != null ? req.getDevoteeCapacity() : 30)
                .status(req.getStatus() != null ? req.getStatus() : PoojaScheduleStatus.OPEN)
                .timeSlotConfigId(req.getTimeSlotConfigId())
                .build();

        EventPoojaSchedule saved = scheduleRepo.save(schedule);
        auditService.record(AuditAction.POOJA_SCHEDULE_CREATED, AuditModule.EVENTS,
                "EventPoojaSchedule", saved.getId().toString());
        return toDto(saved);
    }

    @Override
    @Transactional
    public PoojaScheduleDto updateSchedule(Long id, PoojaScheduleRequest req) {
        EventPoojaSchedule schedule = scheduleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventPoojaSchedule", id));

        if (req.getScheduleDate() != null) schedule.setScheduleDate(req.getScheduleDate());
        if (req.getStartTime() != null) schedule.setStartTime(req.getStartTime());
        if (req.getEndTime() != null) schedule.setEndTime(req.getEndTime());
        if (req.getFamilyCapacity() != null) schedule.setFamilyCapacity(req.getFamilyCapacity());
        if (req.getDevoteeCapacity() != null) schedule.setDevoteeCapacity(req.getDevoteeCapacity());
        if (req.getStatus() != null) schedule.setStatus(req.getStatus());

        // G-2: Reject if the resolved date+time is in the past (validate after fields are merged)
        validateScheduleNotInPast(schedule.getScheduleDate(), schedule.getStartTime());

        EventPoojaSchedule saved = scheduleRepo.save(schedule);
        auditService.record(AuditAction.POOJA_SCHEDULE_UPDATED, AuditModule.EVENTS,
                "EventPoojaSchedule", id.toString());
        return toDtoWithLiveAvailability(saved);
    }

    @Override
    @Transactional
    public PoojaScheduleDto updateStatus(Long id, PoojaScheduleStatus status) {
        EventPoojaSchedule schedule = scheduleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventPoojaSchedule", id));
        schedule.setStatus(status);
        EventPoojaSchedule saved = scheduleRepo.save(schedule);
        auditService.record(AuditAction.POOJA_SCHEDULE_STATUS_CHANGED, AuditModule.EVENTS,
                "EventPoojaSchedule", id.toString(), null, status.name());
        return toDtoWithLiveAvailability(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PoojaScheduleDto getById(Long id) {
        EventPoojaSchedule schedule = scheduleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventPoojaSchedule", id));
        return toDtoWithLiveAvailability(schedule);
    }

    @Override
    @Transactional
    public List<PoojaScheduleDto> getByPooja(Long poojaId) {
        List<EventPoojaSchedule> schedules = ensureSchedulesExist(poojaId);
        return schedules.stream().map(this::toDtoWithLiveAvailability).toList();
    }

    @Override
    @Transactional
    public List<PoojaScheduleDto> getByPoojaAndDate(Long poojaId, LocalDate date) {
        ensureSchedulesExist(poojaId);
        return scheduleRepo.findByPoojaSeva_IdAndScheduleDateOrderByStartTimeAsc(poojaId, date)
                .stream().map(this::toDtoWithLiveAvailability).toList();
    }

    @Override
    @Transactional
    public List<LocalDate> getAvailableDates(Long poojaId) {
        ensureSchedulesExist(poojaId);
        return scheduleRepo.findAvailableDatesByPoojaId(poojaId);
    }

    private List<EventPoojaSchedule> ensureSchedulesExist(Long poojaId) {
        EventPoojaSeva seva = poojaSevaRepo.findById(poojaId).orElse(null);
        if (seva == null) {
            return List.of();
        }

        List<EventPoojaSchedule> existing = scheduleRepo.findByPoojaSeva_IdOrderByScheduleDateAscStartTimeAsc(poojaId);
        if (!existing.isEmpty()) {
            boolean updated = false;
            for (var sch : existing) {
                int expectedCap = seva.getSlots() != null ? seva.getSlots() : 30;
                if (seva.getTimeSlotConfig() != null && !seva.getTimeSlotConfig().isEmpty()) {
                    for (var slot : seva.getTimeSlotConfig()) {
                        LocalDate sDate = slot.getSlotDate() != null ? slot.getSlotDate() : seva.getDate();
                        java.time.LocalTime sTime = parseLocalTime(slot.getStartTime());
                        if (sDate != null && sTime != null && sDate.equals(sch.getScheduleDate()) && sTime.equals(sch.getStartTime())) {
                            expectedCap = slot.getSlotCount() != null ? slot.getSlotCount() : (seva.getSlots() != null ? seva.getSlots() : 30);
                            if (sch.getTimeSlotConfigId() == null && slot.getId() != null) {
                                sch.setTimeSlotConfigId(slot.getId());
                                updated = true;
                            }
                            break;
                        }
                    }
                }
                if (!sch.getFamilyCapacity().equals(expectedCap) || !sch.getDevoteeCapacity().equals(expectedCap)) {
                    sch.setFamilyCapacity(Math.max(1, expectedCap));
                    sch.setDevoteeCapacity(Math.max(1, expectedCap));
                    updated = true;
                }
            }
            if (updated) {
                scheduleRepo.saveAll(existing);
            }
            return existing;
        }

        List<EventPoojaSchedule> toSave = new java.util.ArrayList<>();
        if (seva.getTimeSlotConfig() != null && !seva.getTimeSlotConfig().isEmpty()) {
            for (var slot : seva.getTimeSlotConfig()) {
                // CLOSED config slots must never have schedule rows created for them.
                // BLOCKED slots are also skipped on initial creation (admin can open a schedule row manually).
                if (slot.getStatus() != null && slot.getStatus() != PoojaScheduleStatus.OPEN) {
                    continue;
                }
                LocalDate sDate = slot.getSlotDate() != null ? slot.getSlotDate() : seva.getDate();
                java.time.LocalTime sTime = parseLocalTime(slot.getStartTime());
                if (sDate != null && sTime != null) {
                    if (scheduleRepo.findByPoojaSeva_IdAndScheduleDateAndStartTime(seva.getId(), sDate, sTime).isEmpty()) {
                        int cap = slot.getSlotCount() != null ? slot.getSlotCount() : (seva.getSlots() != null ? seva.getSlots() : 30);
                        toSave.add(EventPoojaSchedule.builder()
                                .poojaSeva(seva)
                                .communityId(seva.getCommunityId())
                                .scheduleDate(sDate)
                                .startTime(sTime)
                                .familyCapacity(Math.max(1, cap))
                                .devoteeCapacity(Math.max(1, cap))
                                .status(PoojaScheduleStatus.OPEN)
                                .notes(slot.getTitle())
                                .timeSlotConfigId(slot.getId())
                                .build());
                    }
                }
            }
        } else if (seva.getDate() != null) {
            List<String> times = (seva.getStartTimes() != null && !seva.getStartTimes().isEmpty())
                    ? seva.getStartTimes()
                    : (seva.getStartTime() != null ? List.of(seva.getStartTime().toString()) : List.of("08:30"));
            LocalDate cur = seva.getDate();
            LocalDate end = seva.getEndDate() != null ? seva.getEndDate() : cur;
            while (!cur.isAfter(end)) {
                for (String tStr : times) {
                    java.time.LocalTime sTime = parseLocalTime(tStr);
                    if (sTime != null) {
                        if (scheduleRepo.findByPoojaSeva_IdAndScheduleDateAndStartTime(seva.getId(), cur, sTime).isEmpty()) {
                            int cap = seva.getSlots() != null ? seva.getSlots() : 30;
                            toSave.add(EventPoojaSchedule.builder()
                                    .poojaSeva(seva)
                                    .communityId(seva.getCommunityId())
                                    .scheduleDate(cur)
                                    .startTime(sTime)
                                    .familyCapacity(Math.max(1, cap))
                                    .devoteeCapacity(Math.max(1, cap))
                                    .status(PoojaScheduleStatus.OPEN)
                                    .build());
                        }
                    }
                }
                cur = cur.plusDays(1);
            }
        }

        if (!toSave.isEmpty()) {
            scheduleRepo.saveAll(toSave);
            return scheduleRepo.findByPoojaSeva_IdOrderByScheduleDateAscStartTimeAsc(poojaId);
        }
        return existing;
    }

    private java.time.LocalTime parseLocalTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return java.time.LocalTime.of(8, 30);
        String clean = timeStr.trim().toUpperCase();
        try {
            if (clean.endsWith("AM") || clean.endsWith("PM")) {
                boolean isPm = clean.endsWith("PM");
                String raw = clean.replace("AM", "").replace("PM", "").trim();
                String[] parts = raw.split(":");
                int hr = Integer.parseInt(parts[0]);
                int min = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                if (isPm && hr < 12) hr += 12;
                if (!isPm && hr == 12) hr = 0;
                return java.time.LocalTime.of(hr, min);
            }
            String[] parts = clean.split(":");
            int hr = Integer.parseInt(parts[0]);
            int min = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return java.time.LocalTime.of(hr, min);
        } catch (Exception e) {
            return java.time.LocalTime.of(8, 30);
        }
    }

    @Override
    @Transactional
    public void deleteSchedule(Long id) {
        EventPoojaSchedule schedule = scheduleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventPoojaSchedule", id));
        // #7: Prevent deletion if confirmed registrations exist for this slot
        long activeCount = registrationRepo.countConfirmedByScheduleId(id);
        if (activeCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete this slot — it has " + activeCount + " active registration(s). " +
                    "Cancel or reassign all registrations before deleting.");
        }
        scheduleRepo.delete(schedule);
        auditService.record(AuditAction.POOJA_SCHEDULE_DELETED, AuditModule.EVENTS,
                "EventPoojaSchedule", id.toString());
    }

    // ── Mappers ──

    private PoojaScheduleDto toDto(EventPoojaSchedule s) {
        return PoojaScheduleDto.builder()
                .id(s.getId())
                .poojaId(s.getPoojaSeva().getId())
                .poojaName(s.getPoojaSeva().getName())
                .scheduleDate(s.getScheduleDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .familyCapacity(s.getFamilyCapacity())
                .devoteeCapacity(s.getDevoteeCapacity())
                .status(s.getStatus())
                .nextTokenSeq(s.getNextTokenSeq())
                .availableFamilies(s.getFamilyCapacity())
                .availableDevotees(s.getDevoteeCapacity())
                .timeSlotConfigId(s.getTimeSlotConfigId())
                .build();
    }

    private PoojaScheduleDto toDtoWithLiveAvailability(EventPoojaSchedule s) {
        LocalDateTime now = LocalDateTime.now();
        int confirmedFamilies  = reservationRepo.sumConfirmedFamilies(s.getId());
        int reservedFamilies   = reservationRepo.sumActiveReservedFamilies(s.getId(), now);
        int confirmedDevotees  = reservationRepo.sumConfirmedDevotees(s.getId());
        int reservedDevotees   = reservationRepo.sumActiveReservedDevotees(s.getId(), now);

        // M-3: also count admin-direct registrations that have no reservation row
        int directFamilies = (int) registrationRepo.countDirectRegistrationsByScheduleId(s.getId());
        int directDevotees = registrationRepo.sumDirectDevoteesByScheduleId(s.getId());

        int availFamilies  = Math.max(0, s.getFamilyCapacity()  - confirmedFamilies  - reservedFamilies  - directFamilies);
        int availDevotees  = Math.max(0, s.getDevoteeCapacity() - confirmedDevotees  - reservedDevotees  - directDevotees);

        return PoojaScheduleDto.builder()
                .id(s.getId())
                .poojaId(s.getPoojaSeva().getId())
                .poojaName(s.getPoojaSeva().getName())
                .scheduleDate(s.getScheduleDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .familyCapacity(s.getFamilyCapacity())
                .devoteeCapacity(s.getDevoteeCapacity())
                .status(s.getStatus())
                .nextTokenSeq(s.getNextTokenSeq())
                .availableFamilies(availFamilies)
                .availableDevotees(availDevotees)
                .timeSlotConfigId(s.getTimeSlotConfigId())
                .build();
    }
    // ── Validation helpers ──

    /**
     * G-2: Rejects a schedule slot if its date+time combination is already in the past.
     * <ul>
     *   <li>If {@code date} is strictly before today → rejected.</li>
     *   <li>If {@code date} is today AND {@code startTime} is before the current time → rejected.</li>
     * </ul>
     * Null values are treated permissively (no rejection) so that callers handling optional
     * fields do not have to guard every call site.
     */
    private void validateScheduleNotInPast(LocalDate date, java.time.LocalTime startTime) {
        if (date == null) return;
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new ManaCommunityException(
                    "Cannot create or update a Pooja schedule in the past (date: " + date + ").",
                    HttpStatus.BAD_REQUEST,
                    "SLOT_IN_PAST"
            );
        }
        if (date.isEqual(today) && startTime != null && startTime.isBefore(java.time.LocalTime.now())) {
            throw new ManaCommunityException(
                    "Cannot create or update a Pooja schedule whose start time has already passed " +
                    "(today " + date + " at " + startTime + ").",
                    HttpStatus.BAD_REQUEST,
                    "SLOT_IN_PAST"
            );
        }
    }
}
