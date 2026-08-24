package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.dto.PoojaReserveRequest;
import com.manacommunity.api.events.dto.PoojaReserveResponse;
import com.manacommunity.api.events.entity.PoojaSchedule;
import com.manacommunity.api.events.entity.PoojaSlotReservation;
import com.manacommunity.api.events.enums.PoojaScheduleStatus;
import com.manacommunity.api.events.enums.ReservationStatus;
import com.manacommunity.api.events.repository.PoojaScheduleRepository;
import com.manacommunity.api.events.repository.PoojaSlotReservationRepository;
import com.manacommunity.api.events.service.PoojaSlotReservationService;
import com.manacommunity.api.exception.EventFullException;
import com.manacommunity.api.exception.RegistrationClosedException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PoojaSlotReservationServiceImpl implements PoojaSlotReservationService {

    private final PoojaScheduleRepository scheduleRepo;
    private final PoojaSlotReservationRepository reservationRepo;
    private final AuditService auditService;

    @Value("${pooja.reservation.ttl-minutes:5}")
    private int reservationTtlMinutes;

    public PoojaSlotReservationServiceImpl(PoojaScheduleRepository scheduleRepo,
                                           PoojaSlotReservationRepository reservationRepo,
                                           AuditService auditService) {
        this.scheduleRepo = scheduleRepo;
        this.reservationRepo = reservationRepo;
        this.auditService = auditService;
    }

    /**
     * Core booking-engine transaction:
     * 1. SELECT … FOR UPDATE on the schedule row (no other thread can modify it)
     * 2. Expire stale reservations within the same TX
     * 3. Calculate live availability
     * 4. Reject if full
     * 5. Create + persist the reservation
     * 6. Increment token sequence
     */
    @Override
    @Transactional
    public PoojaReserveResponse reserve(Long scheduleId, PoojaReserveRequest req, AppUser user) {

        // ── Idempotency: return existing reservation if the same key was already used ──
        if (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()) {
            Optional<PoojaSlotReservation> existing =
                    reservationRepo.findByIdempotencyKey(req.getIdempotencyKey());
            if (existing.isPresent()) {
                PoojaSlotReservation r = existing.get();
                return PoojaReserveResponse.builder()
                        .reservationId(r.getId())
                        .scheduleId(scheduleId)
                        .idempotencyKey(r.getIdempotencyKey())
                        .reservedFamilyCount(r.getReservedFamilyCount())
                        .reservedDevoteeCount(r.getReservedDevoteeCount())
                        .expiresAt(r.getExpiresAt())
                        .status(r.getStatus().name())
                        .tokenNumber(0)
                        .build();
            }
        }

        // ── 1. Acquire pessimistic write lock ──
        PoojaSchedule schedule = scheduleRepo.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("PoojaSchedule", scheduleId));

        if (schedule.getStatus() == PoojaScheduleStatus.BLOCKED ||
            schedule.getStatus() == PoojaScheduleStatus.CLOSED) {
            throw new RegistrationClosedException(
                    schedule.getPoojaSeva().getName(), schedule.getStatus().name());
        }

        // ── 2. Expire stale reservations (inside the lock) ──
        LocalDateTime now = LocalDateTime.now();
        reservationRepo.expireStaleForSchedule(scheduleId, now);

        // ── 3. Calculate live availability ──
        int confirmedFamilies = reservationRepo.sumConfirmedFamilies(scheduleId);
        int reservedFamilies  = reservationRepo.sumActiveReservedFamilies(scheduleId, now);
        int availFamilies     = schedule.getFamilyCapacity() - confirmedFamilies - reservedFamilies;

        int confirmedDevotees = reservationRepo.sumConfirmedDevotees(scheduleId);
        int reservedDevotees  = reservationRepo.sumActiveReservedDevotees(scheduleId, now);
        int availDevotees     = schedule.getDevoteeCapacity() - confirmedDevotees - reservedDevotees;

        // ── 4. Capacity check ──
        int requestedFamilies = Math.max(1, req.getFamilyCount());
        int requestedDevotees = Math.max(1, req.getDevoteeCount());

        if (availFamilies < requestedFamilies) {
            throw new EventFullException(
                    "This slot is full — no family spots available for '"
                    + schedule.getPoojaSeva().getName() + "'. Please choose another slot.");
        }
        if (availDevotees < requestedDevotees) {
            throw new EventFullException(
                    "This slot is full — no devotee spots available for '"
                    + schedule.getPoojaSeva().getName() + "'. Please choose another slot.");
        }

        // ── 5. Assign token number (before incrementing) ──
        int tokenNumber = schedule.getNextTokenSeq();

        // ── 6. Create reservation ──
        LocalDateTime expiresAt = now.plusMinutes(reservationTtlMinutes);

        PoojaSlotReservation reservation = PoojaSlotReservation.builder()
                .schedule(schedule)
                .user(user)
                .reservedFamilyCount(requestedFamilies)
                .reservedDevoteeCount(requestedDevotees)
                .status(ReservationStatus.RESERVED)
                .expiresAt(expiresAt)
                .idempotencyKey(req.getIdempotencyKey())
                .build();

        PoojaSlotReservation saved = reservationRepo.save(reservation);

        // ── 7. Increment token sequence on the schedule ──
        schedule.setNextTokenSeq(tokenNumber + 1);
        scheduleRepo.save(schedule);

        auditService.record(AuditAction.POOJA_SLOT_RESERVED, AuditModule.EVENTS,
                "PoojaSlotReservation", saved.getId().toString(),
                null, "schedule=" + scheduleId + " families=" + requestedFamilies + " devotees=" + requestedDevotees);

        return PoojaReserveResponse.builder()
                .reservationId(saved.getId())
                .scheduleId(scheduleId)
                .idempotencyKey(saved.getIdempotencyKey())
                .reservedFamilyCount(requestedFamilies)
                .reservedDevoteeCount(requestedDevotees)
                .expiresAt(expiresAt)
                .status(ReservationStatus.RESERVED.name())
                .tokenNumber(tokenNumber)
                .build();
    }

    @Override
    @Transactional
    public void confirmReservation(Long reservationId, Long registrationId) {
        PoojaSlotReservation r = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("PoojaSlotReservation", reservationId));
        r.setStatus(ReservationStatus.CONFIRMED);
        r.setRegistrationId(registrationId);
        reservationRepo.save(r);
    }

    @Override
    @Transactional
    public void releaseReservation(Long reservationId) {
        reservationRepo.findById(reservationId).ifPresent(r -> {
            r.setStatus(ReservationStatus.CANCELLED);
            reservationRepo.save(r);
            auditService.record(AuditAction.POOJA_SLOT_RESERVATION_CANCELLED, AuditModule.EVENTS,
                    "PoojaSlotReservation", reservationId.toString());
        });
    }
}
