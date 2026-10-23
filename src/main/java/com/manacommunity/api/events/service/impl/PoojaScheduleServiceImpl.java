package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.dto.PoojaScheduleDto;
import com.manacommunity.api.events.dto.PoojaScheduleRequest;
import com.manacommunity.api.events.entity.EventPoojaSchedule;
import com.manacommunity.api.events.entity.EventPoojaSeva;
import com.manacommunity.api.events.entity.EventPoojaSevaDayTimeSlot;
import com.manacommunity.api.events.enums.PoojaScheduleStatus;
import com.manacommunity.api.events.repository.EventPoojaSevaTimeSlotRepository;
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
    private final EventPoojaSevaTimeSlotRepository timeSlotRepo;
    private final EventPoojaSlotReservationRepository reservationRepo;
    private final EventPoojaUserRegistrationRepository registrationRepo;
    private final AuditService auditService;

    public PoojaScheduleServiceImpl(EventPoojaScheduleRepository scheduleRepo,
                                    PoojaSevaRepository poojaSevaRepo,
                                    EventPoojaSevaTimeSlotRepository timeSlotRepo,
                                    EventPoojaSlotReservationRepository reservationRepo,
                                    EventPoojaUserRegistrationRepository registrationRepo,
                                    AuditService auditService) {
        this.scheduleRepo = scheduleRepo;
        this.poojaSevaRepo = poojaSevaRepo;
        this.timeSlotRepo = timeSlotRepo;
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

        Integer familyCap = req.getFamilyCapacity();
        Integer devoteeCap = req.getDevoteeCapacity();
        java.time.LocalTime startTime = req.getStartTime();
        java.time.LocalTime endTime = req.getEndTime();
        LocalDate scheduleDate = req.getScheduleDate();
        Long timeSlotConfigId = req.getTimeSlotConfigId();

        if (timeSlotConfigId != null) {
            EventPoojaSevaDayTimeSlot slot = timeSlotRepo.findById(timeSlotConfigId).orElse(null);
            if (slot != null) {
                if (familyCap == null && slot.getSlotCount() != null && slot.getSlotCount() > 0) {
                    familyCap = slot.getSlotCount();
                }
                if (devoteeCap == null && slot.getSlotCount() != null && slot.getSlotCount() > 0) {
                    devoteeCap = slot.getSlotCount();
                }
                if (startTime == null && slot.getStartTime() != null && !slot.getStartTime().isBlank()) {
                    startTime = parseLocalTime(slot.getStartTime());
                }
                if (endTime == null && slot.getEndTime() != null && !slot.getEndTime().isBlank()) {
                    endTime = parseLocalTime(slot.getEndTime());
                }
                if (scheduleDate == null && slot.getSlotDate() != null) {
                    scheduleDate = slot.getSlotDate();
                }
            }
        }

        int defaultCap = (seva.getSlots() != null && seva.getSlots() > 0) ? seva.getSlots() : 1;

        EventPoojaSchedule schedule = EventPoojaSchedule.builder()
                .poojaSeva(seva)
                .communityId(seva.getCommunityId())
                .scheduleDate(scheduleDate != null ? scheduleDate : req.getScheduleDate())
                .startTime(startTime != null ? startTime : req.getStartTime())
                .endTime(endTime)
                .familyCapacity(familyCap != null ? familyCap : (devoteeCap != null ? devoteeCap : defaultCap))
                .devoteeCapacity(devoteeCap != null ? devoteeCap : (familyCap != null ? familyCap : defaultCap))
                .status(req.getStatus() != null ? req.getStatus() : PoojaScheduleStatus.OPEN)
                .timeSlotConfigId(timeSlotConfigId)
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
        if (req.getTimeSlotConfigId() != null) schedule.setTimeSlotConfigId(req.getTimeSlotConfigId());

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

        // Always load time slots directly from DB — the @Transient field on EventPoojaSeva
        // is never populated in this service, so seva.getTimeSlotConfig() is always empty here.
        List<EventPoojaSevaDayTimeSlot> timeSlots =
                timeSlotRepo.findByPoojaSevaIdOrderBySlotDateAscStartTimeAsc(poojaId);

        List<EventPoojaSchedule> existing = scheduleRepo.findByPoojaSeva_IdOrderByScheduleDateAscStartTimeAsc(poojaId);

        if (!timeSlots.isEmpty()) {
            boolean updated = false;
            java.util.Set<Long> matchedExistingIds = new java.util.HashSet<>();
            java.util.Set<Long> matchedSlotIds = new java.util.HashSet<>();

            // 1. Match each configured time slot to an existing schedule row
            for (EventPoojaSevaDayTimeSlot slot : timeSlots) {
                if (slot.getStatus() != null && slot.getStatus() != PoojaScheduleStatus.OPEN) {
                    continue;
                }
                LocalDate sDate = slot.getSlotDate() != null ? slot.getSlotDate() : seva.getDate();
                java.time.LocalTime sTime = parseLocalTime(slot.getStartTime());
                java.time.LocalTime eTime = parseLocalTime(slot.getEndTime());

                if (sDate == null || sTime == null) {
                    continue;
                }

                // Try finding matching schedule: first by timeSlotConfigId, then by (scheduleDate, startTime)
                EventPoojaSchedule matched = null;
                for (EventPoojaSchedule sch : existing) {
                    if (matchedExistingIds.contains(sch.getId())) {
                        continue;
                    }
                    if (slot.getId() != null && slot.getId().equals(sch.getTimeSlotConfigId())) {
                        matched = sch;
                        break;
                    }
                }
                if (matched == null) {
                    for (EventPoojaSchedule sch : existing) {
                        if (matchedExistingIds.contains(sch.getId())) {
                            continue;
                        }
                        if (sDate.equals(sch.getScheduleDate()) && sTime.equals(sch.getStartTime())) {
                            matched = sch;
                            break;
                        }
                    }
                }

                int expectedCap = (slot.getSlotCount() != null && slot.getSlotCount() > 0) ? slot.getSlotCount() : 1;

                if (matched != null) {
                    matchedExistingIds.add(matched.getId());
                    matchedSlotIds.add(slot.getId());

                    if (matched.getFamilyCapacity() == null || !matched.getFamilyCapacity().equals(expectedCap)) {
                        matched.setFamilyCapacity(expectedCap);
                        updated = true;
                    }
                    if (matched.getDevoteeCapacity() == null || !matched.getDevoteeCapacity().equals(expectedCap)) {
                        matched.setDevoteeCapacity(expectedCap);
                        updated = true;
                    }
                    if (eTime != null && !eTime.equals(matched.getEndTime())) {
                        matched.setEndTime(eTime);
                        updated = true;
                    }
                    if (!sTime.equals(matched.getStartTime())) {
                        matched.setStartTime(sTime);
                        updated = true;
                    }
                    if (!sDate.equals(matched.getScheduleDate())) {
                        matched.setScheduleDate(sDate);
                        updated = true;
                    }
                    if (slot.getId() != null && !slot.getId().equals(matched.getTimeSlotConfigId())) {
                        matched.setTimeSlotConfigId(slot.getId());
                        updated = true;
                    }
                    if (slot.getTitle() != null && !slot.getTitle().isBlank() && (matched.getNotes() == null || matched.getNotes().isBlank())) {
                        matched.setNotes(slot.getTitle());
                        updated = true;
                    }
                    if (slot.getStatus() != null && !slot.getStatus().equals(matched.getStatus())) {
                        matched.setStatus(slot.getStatus());
                        updated = true;
                    }

                    // Self-healing: if any duplicate schedule exists for the same slotDate & startTime,
                    // migrate its registrations and reservations to this canonical matched schedule
                    for (EventPoojaSchedule dup : existing) {
                        if (!matched.getId().equals(dup.getId()) && !matchedExistingIds.contains(dup.getId())) {
                            boolean isSameDateAndTime = sDate.equals(dup.getScheduleDate()) && sTime.equals(dup.getStartTime());
                            if (isSameDateAndTime) {
                                registrationRepo.migrateScheduleId(dup.getId(), matched.getId());
                                reservationRepo.migrateScheduleId(dup.getId(), matched.getId());
                            }
                        }
                    }
                }
            }

            // 2. Remove stale / duplicate existing schedules that don't match any time slot AND have no active bookings
            List<EventPoojaSchedule> toDelete = new java.util.ArrayList<>();
            for (EventPoojaSchedule sch : existing) {
                if (!matchedExistingIds.contains(sch.getId())) {
                    long activeCount = registrationRepo.countConfirmedByScheduleId(sch.getId());
                    int reservedCount = reservationRepo.sumConfirmedFamilies(sch.getId());
                    if (activeCount == 0 && reservedCount == 0) {
                        toDelete.add(sch);
                    }
                }
            }
            if (!toDelete.isEmpty()) {
                scheduleRepo.deleteAll(toDelete);
                existing.removeAll(toDelete);
            }

            // 3. Create new schedules for timeSlots that had no existing match
            List<EventPoojaSchedule> toSave = new java.util.ArrayList<>();
            for (EventPoojaSevaDayTimeSlot slot : timeSlots) {
                if (slot.getStatus() != null && slot.getStatus() != PoojaScheduleStatus.OPEN) {
                    continue;
                }
                if (matchedSlotIds.contains(slot.getId())) {
                    continue;
                }
                LocalDate sDate = slot.getSlotDate() != null ? slot.getSlotDate() : seva.getDate();
                java.time.LocalTime sTime = parseLocalTime(slot.getStartTime());
                java.time.LocalTime eTime = parseLocalTime(slot.getEndTime());

                if (sDate != null && sTime != null) {
                    int cap = (slot.getSlotCount() != null && slot.getSlotCount() > 0) ? slot.getSlotCount() : 1;
                    toSave.add(EventPoojaSchedule.builder()
                            .poojaSeva(seva)
                            .communityId(seva.getCommunityId())
                            .scheduleDate(sDate)
                            .startTime(sTime)
                            .endTime(eTime)
                            .familyCapacity(cap)
                            .devoteeCapacity(cap)
                            .status(PoojaScheduleStatus.OPEN)
                            .notes(slot.getTitle())
                            .timeSlotConfigId(slot.getId())
                            .build());
                }
            }

            if (updated) {
                scheduleRepo.saveAll(existing);
            }
            if (!toSave.isEmpty()) {
                scheduleRepo.saveAll(toSave);
                return scheduleRepo.findByPoojaSeva_IdOrderByScheduleDateAscStartTimeAsc(poojaId);
            }
            return existing;
        } else {
            // Fallback path: no time_slot rows configured
            if (!existing.isEmpty()) {
                boolean fallbackUpdated = false;
                int expectedCap = (seva.getSlots() != null && seva.getSlots() > 0) ? seva.getSlots() : 1;
                for (EventPoojaSchedule sch : existing) {
                    if (sch.getFamilyCapacity() == null || (sch.getFamilyCapacity() == 10 && expectedCap != 10)
                            || (sch.getDevoteeCapacity() == 30 && expectedCap != 30)) {
                        sch.setFamilyCapacity(expectedCap);
                        sch.setDevoteeCapacity(expectedCap);
                        fallbackUpdated = true;
                    }
                }
                if (fallbackUpdated) {
                    scheduleRepo.saveAll(existing);
                }
                return existing;
            }
            List<EventPoojaSchedule> toSave = new java.util.ArrayList<>();
            if (seva.getDate() != null) {
                List<String> times = (seva.getStartTimes() != null && !seva.getStartTimes().isEmpty())
                        ? seva.getStartTimes()
                        : (seva.getStartTime() != null ? List.of(seva.getStartTime().toString()) : List.of("08:30"));
                String defaultEndStr = seva.getEndTime() != null ? seva.getEndTime().toString() : null;
                LocalDate cur = seva.getDate();
                LocalDate end = seva.getEndDate() != null ? seva.getEndDate() : cur;
                while (!cur.isAfter(end)) {
                    for (String tStr : times) {
                        java.time.LocalTime sTime = parseLocalTime(tStr);
                        java.time.LocalTime eTime = parseLocalTime(defaultEndStr);
                        if (sTime != null) {
                            if (scheduleRepo.findByPoojaSeva_IdAndScheduleDateAndStartTime(seva.getId(), cur, sTime).isEmpty()) {
                                int fallbackCap = (seva.getSlots() != null && seva.getSlots() > 0) ? seva.getSlots() : 1;
                                toSave.add(EventPoojaSchedule.builder()
                                        .poojaSeva(seva)
                                        .communityId(seva.getCommunityId())
                                        .scheduleDate(cur)
                                        .startTime(sTime)
                                        .endTime(eTime)
                                        .familyCapacity(fallbackCap)
                                        .devoteeCapacity(fallbackCap)
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
    }

    private java.time.LocalTime parseLocalTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;
        String clean = timeStr.trim();
        try {
            return java.time.LocalTime.parse(clean);
        } catch (Exception ignored) {}
        try {
            String upper = clean.toUpperCase();
            if (upper.endsWith("AM") || upper.endsWith("PM")) {
                boolean isPm = upper.endsWith("PM");
                String raw = upper.replace("AM", "").replace("PM", "").trim();
                String[] parts = raw.split(":");
                int hr = Integer.parseInt(parts[0].trim());
                int min = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
                if (isPm && hr < 12) hr += 12;
                if (!isPm && hr == 12) hr = 0;
                return java.time.LocalTime.of(hr, min);
            }
            String[] parts = upper.split(":");
            int hr = Integer.parseInt(parts[0].trim());
            int min = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
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
