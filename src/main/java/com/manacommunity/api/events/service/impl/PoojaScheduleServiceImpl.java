package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.dto.PoojaScheduleDto;
import com.manacommunity.api.events.dto.PoojaScheduleRequest;
import com.manacommunity.api.events.entity.EventPoojaSchedule;
import com.manacommunity.api.events.entity.PoojaSeva;
import com.manacommunity.api.events.enums.PoojaScheduleStatus;
import com.manacommunity.api.events.repository.EventPoojaUserRegistrationRepository;
import com.manacommunity.api.events.repository.EventPoojaScheduleRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.events.repository.EventPoojaSlotReservationRepository;
import com.manacommunity.api.events.service.PoojaScheduleService;
import com.manacommunity.api.exception.ResourceNotFoundException;
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
        PoojaSeva seva = poojaSevaRepo.findById(req.getPoojaId())
                .orElseThrow(() -> new ResourceNotFoundException("PoojaSeva", req.getPoojaId()));

        EventPoojaSchedule schedule = EventPoojaSchedule.builder()
                .poojaSeva(seva)
                .scheduleDate(req.getScheduleDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .familyCapacity(req.getFamilyCapacity() != null ? req.getFamilyCapacity() : 10)
                .devoteeCapacity(req.getDevoteeCapacity() != null ? req.getDevoteeCapacity() : 30)
                .status(req.getStatus() != null ? req.getStatus() : PoojaScheduleStatus.OPEN)
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
    @Transactional(readOnly = true)
    public List<PoojaScheduleDto> getByPooja(Long poojaId) {
        return scheduleRepo.findByPoojaSeva_IdOrderByScheduleDateAscStartTimeAsc(poojaId)
                .stream().map(this::toDtoWithLiveAvailability).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PoojaScheduleDto> getByPoojaAndDate(Long poojaId, LocalDate date) {
        return scheduleRepo.findByPoojaSeva_IdAndScheduleDateOrderByStartTimeAsc(poojaId, date)
                .stream().map(this::toDtoWithLiveAvailability).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDate> getAvailableDates(Long poojaId) {
        return scheduleRepo.findAvailableDatesByPoojaId(poojaId);
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
                .build();
    }

    private PoojaScheduleDto toDtoWithLiveAvailability(EventPoojaSchedule s) {
        LocalDateTime now = LocalDateTime.now();
        int confirmedFamilies  = reservationRepo.sumConfirmedFamilies(s.getId());
        int reservedFamilies   = reservationRepo.sumActiveReservedFamilies(s.getId(), now);
        int confirmedDevotees  = reservationRepo.sumConfirmedDevotees(s.getId());
        int reservedDevotees   = reservationRepo.sumActiveReservedDevotees(s.getId(), now);

        int availFamilies  = Math.max(0, s.getFamilyCapacity()  - confirmedFamilies  - reservedFamilies);
        int availDevotees  = Math.max(0, s.getDevoteeCapacity() - confirmedDevotees  - reservedDevotees);

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
                .build();
    }
}
