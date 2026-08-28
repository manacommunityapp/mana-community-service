package com.manacommunity.api.unit.service;

import com.manacommunity.api.events.dto.PoojaScheduleDto;
import com.manacommunity.api.events.dto.PoojaScheduleRequest;
import com.manacommunity.api.events.entity.EventPoojaSchedule;
import com.manacommunity.api.events.entity.EventPoojaSeva;
import com.manacommunity.api.events.entity.EventPoojaSevaDayTimeSlot;
import com.manacommunity.api.events.enums.PoojaScheduleStatus;
import com.manacommunity.api.events.repository.EventPoojaScheduleRepository;
import com.manacommunity.api.events.repository.EventPoojaSevaTimeSlotRepository;
import com.manacommunity.api.events.repository.EventPoojaSlotReservationRepository;
import com.manacommunity.api.events.repository.EventPoojaUserRegistrationRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.events.service.impl.PoojaScheduleServiceImpl;
import com.manacommunity.api.security.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PoojaScheduleServiceImpl Unit Tests")
class PoojaScheduleServiceImplTest {

    private EventPoojaScheduleRepository scheduleRepo;
    private PoojaSevaRepository poojaSevaRepo;
    private EventPoojaSevaTimeSlotRepository timeSlotRepo;
    private EventPoojaSlotReservationRepository reservationRepo;
    private EventPoojaUserRegistrationRepository registrationRepo;
    private AuditService auditService;
    private PoojaScheduleServiceImpl service;

    @BeforeEach
    void setUp() {
        scheduleRepo = mock(EventPoojaScheduleRepository.class);
        poojaSevaRepo = mock(PoojaSevaRepository.class);
        timeSlotRepo = mock(EventPoojaSevaTimeSlotRepository.class);
        reservationRepo = mock(EventPoojaSlotReservationRepository.class);
        registrationRepo = mock(EventPoojaUserRegistrationRepository.class);
        auditService = mock(AuditService.class);

        service = new PoojaScheduleServiceImpl(
                scheduleRepo,
                poojaSevaRepo,
                timeSlotRepo,
                reservationRepo,
                registrationRepo,
                auditService
        );
    }

    @Test
    @DisplayName("ensureSchedulesExist creates schedules directly from event_pooja_seva_time_slots with correct slot_count, times and timeSlotConfigId")
    void ensureSchedulesExist_createsFromTimeSlots() {
        Long poojaId = 10L;
        EventPoojaSeva seva = new EventPoojaSeva();
        seva.setId(poojaId);
        seva.setName("Ganesh Pooja");
        seva.setCommunityId(1L);
        seva.setDate(LocalDate.now().plusDays(5));

        EventPoojaSevaDayTimeSlot slot1 = new EventPoojaSevaDayTimeSlot(
                poojaId, LocalDate.now().plusDays(5), "09:00", "10:30", "Morning Slot", 15
        );
        slot1.setId(101L);

        EventPoojaSevaDayTimeSlot slot2 = new EventPoojaSevaDayTimeSlot(
                poojaId, LocalDate.now().plusDays(5), "18:00", "19:30", "Evening Slot", 25
        );
        slot2.setId(102L);

        when(poojaSevaRepo.findById(poojaId)).thenReturn(Optional.of(seva));
        when(timeSlotRepo.findByPoojaSevaIdOrderBySlotDateAscStartTimeAsc(poojaId)).thenReturn(List.of(slot1, slot2));
        when(scheduleRepo.findByPoojaSeva_IdOrderByScheduleDateAscStartTimeAsc(poojaId)).thenReturn(new ArrayList<>());
        when(scheduleRepo.findByPoojaSeva_IdAndScheduleDateAndStartTime(any(), any(), any())).thenReturn(Optional.empty());

        List<EventPoojaSchedule> savedList = new ArrayList<>();
        when(scheduleRepo.saveAll(any())).thenAnswer(inv -> {
            Iterable<EventPoojaSchedule> items = inv.getArgument(0);
            items.forEach(savedList::add);
            return savedList;
        });

        service.getByPooja(poojaId);

        assertThat(savedList).hasSize(2);

        EventPoojaSchedule s1 = savedList.get(0);
        assertThat(s1.getScheduleDate()).isEqualTo(LocalDate.now().plusDays(5));
        assertThat(s1.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(s1.getEndTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(s1.getFamilyCapacity()).isEqualTo(15);
        assertThat(s1.getDevoteeCapacity()).isEqualTo(15);
        assertThat(s1.getTimeSlotConfigId()).isEqualTo(101L);

        EventPoojaSchedule s2 = savedList.get(1);
        assertThat(s2.getScheduleDate()).isEqualTo(LocalDate.now().plusDays(5));
        assertThat(s2.getStartTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(s2.getEndTime()).isEqualTo(LocalTime.of(19, 30));
        assertThat(s2.getFamilyCapacity()).isEqualTo(25);
        assertThat(s2.getDevoteeCapacity()).isEqualTo(25);
        assertThat(s2.getTimeSlotConfigId()).isEqualTo(102L);
    }

    @Test
    @DisplayName("ensureSchedulesExist syncs existing schedules with slot_count and fills timeSlotConfigId")
    void ensureSchedulesExist_syncsExistingWithSlotCount() {
        Long poojaId = 10L;
        EventPoojaSeva seva = new EventPoojaSeva();
        seva.setId(poojaId);
        seva.setName("Ganesh Pooja");
        seva.setCommunityId(1L);
        seva.setDate(LocalDate.now().plusDays(5));

        EventPoojaSevaDayTimeSlot slot1 = new EventPoojaSevaDayTimeSlot(
                poojaId, LocalDate.now().plusDays(5), "09:00", "10:30", "Morning Slot", 12
        );
        slot1.setId(101L);

        EventPoojaSchedule existingSchedule = EventPoojaSchedule.builder()
                .id(501L)
                .poojaSeva(seva)
                .communityId(1L)
                .scheduleDate(LocalDate.now().plusDays(5))
                .startTime(LocalTime.of(9, 0))
                .familyCapacity(10) // outdated capacity
                .devoteeCapacity(30) // outdated default capacity
                .status(PoojaScheduleStatus.OPEN)
                .build();

        List<EventPoojaSchedule> existingList = new ArrayList<>(List.of(existingSchedule));

        when(poojaSevaRepo.findById(poojaId)).thenReturn(Optional.of(seva));
        when(timeSlotRepo.findByPoojaSevaIdOrderBySlotDateAscStartTimeAsc(poojaId)).thenReturn(List.of(slot1));
        when(scheduleRepo.findByPoojaSeva_IdOrderByScheduleDateAscStartTimeAsc(poojaId)).thenReturn(existingList);

        service.getByPooja(poojaId);

        verify(scheduleRepo).saveAll(any());
        assertThat(existingSchedule.getFamilyCapacity()).isEqualTo(12);
        assertThat(existingSchedule.getDevoteeCapacity()).isEqualTo(12);
        assertThat(existingSchedule.getEndTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(existingSchedule.getTimeSlotConfigId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("createSchedule maps slotCount from timeSlotConfigId if capacity is not supplied in request")
    void createSchedule_mapsSlotCountFromTimeSlotConfig() {
        Long poojaId = 10L;
        Long slotConfigId = 101L;
        EventPoojaSeva seva = new EventPoojaSeva();
        seva.setId(poojaId);
        seva.setCommunityId(1L);

        EventPoojaSevaDayTimeSlot slot = new EventPoojaSevaDayTimeSlot(
                poojaId, LocalDate.now().plusDays(5), "09:00", "10:30", "Morning Slot", 18
        );
        slot.setId(slotConfigId);

        PoojaScheduleRequest req = new PoojaScheduleRequest();
        req.setPoojaId(poojaId);
        req.setScheduleDate(LocalDate.now().plusDays(5));
        req.setStartTime(LocalTime.of(9, 0));
        req.setTimeSlotConfigId(slotConfigId);

        when(poojaSevaRepo.findById(poojaId)).thenReturn(Optional.of(seva));
        when(timeSlotRepo.findById(slotConfigId)).thenReturn(Optional.of(slot));
        when(scheduleRepo.save(any())).thenAnswer(inv -> {
            EventPoojaSchedule s = inv.getArgument(0);
            s.setId(999L);
            return s;
        });

        PoojaScheduleDto dto = service.createSchedule(req);

        assertThat(dto.getFamilyCapacity()).isEqualTo(18);
        assertThat(dto.getDevoteeCapacity()).isEqualTo(18);
        assertThat(dto.getEndTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(dto.getTimeSlotConfigId()).isEqualTo(slotConfigId);
    }

    @Test
    @DisplayName("ensureSchedulesExist cleans up stale/duplicate schedules without bookings when 4 time slots are present")
    void ensureSchedulesExist_cleansUpDuplicateAndStaleSchedules() {
        Long poojaId = 10L;
        LocalDate d1 = LocalDate.now().plusDays(5);
        LocalDate d2 = LocalDate.now().plusDays(6);

        EventPoojaSeva seva = new EventPoojaSeva();
        seva.setId(poojaId);
        seva.setName("Ganesh Pooja");
        seva.setCommunityId(1L);

        // 4 actual time slots in event_pooja_seva_time_slots
        EventPoojaSevaDayTimeSlot slot1 = new EventPoojaSevaDayTimeSlot(poojaId, d1, "09:00", "10:30", "D1 Morning", 10);
        slot1.setId(101L);
        EventPoojaSevaDayTimeSlot slot2 = new EventPoojaSevaDayTimeSlot(poojaId, d1, "18:00", "19:30", "D1 Evening", 20);
        slot2.setId(102L);
        EventPoojaSevaDayTimeSlot slot3 = new EventPoojaSevaDayTimeSlot(poojaId, d2, "09:00", "10:30", "D2 Morning", 15);
        slot3.setId(103L);
        EventPoojaSevaDayTimeSlot slot4 = new EventPoojaSevaDayTimeSlot(poojaId, d2, "18:00", "19:30", "D2 Evening", 25);
        slot4.setId(104L);

        // 4 matching schedules with outdated capacity
        List<EventPoojaSchedule> existingList = new ArrayList<>();
        existingList.add(EventPoojaSchedule.builder().id(501L).poojaSeva(seva).communityId(1L).scheduleDate(d1).startTime(LocalTime.of(9, 0)).familyCapacity(10).devoteeCapacity(30).status(PoojaScheduleStatus.OPEN).build());
        existingList.add(EventPoojaSchedule.builder().id(502L).poojaSeva(seva).communityId(1L).scheduleDate(d1).startTime(LocalTime.of(18, 0)).familyCapacity(10).devoteeCapacity(30).status(PoojaScheduleStatus.OPEN).build());
        existingList.add(EventPoojaSchedule.builder().id(503L).poojaSeva(seva).communityId(1L).scheduleDate(d2).startTime(LocalTime.of(9, 0)).familyCapacity(10).devoteeCapacity(30).status(PoojaScheduleStatus.OPEN).build());
        existingList.add(EventPoojaSchedule.builder().id(504L).poojaSeva(seva).communityId(1L).scheduleDate(d2).startTime(LocalTime.of(18, 0)).familyCapacity(10).devoteeCapacity(30).status(PoojaScheduleStatus.OPEN).build());
        // 4 duplicate / orphan schedules without bookings
        existingList.add(EventPoojaSchedule.builder().id(505L).poojaSeva(seva).communityId(1L).scheduleDate(d1).startTime(LocalTime.of(9, 0)).familyCapacity(10).devoteeCapacity(30).status(PoojaScheduleStatus.OPEN).build());
        existingList.add(EventPoojaSchedule.builder().id(506L).poojaSeva(seva).communityId(1L).scheduleDate(d1).startTime(LocalTime.of(18, 0)).familyCapacity(10).devoteeCapacity(30).status(PoojaScheduleStatus.OPEN).build());
        existingList.add(EventPoojaSchedule.builder().id(507L).poojaSeva(seva).communityId(1L).scheduleDate(d2).startTime(LocalTime.of(9, 0)).familyCapacity(10).devoteeCapacity(30).status(PoojaScheduleStatus.OPEN).build());
        existingList.add(EventPoojaSchedule.builder().id(508L).poojaSeva(seva).communityId(1L).scheduleDate(d2).startTime(LocalTime.of(18, 0)).familyCapacity(10).devoteeCapacity(30).status(PoojaScheduleStatus.OPEN).build());

        when(poojaSevaRepo.findById(poojaId)).thenReturn(Optional.of(seva));
        when(timeSlotRepo.findByPoojaSevaIdOrderBySlotDateAscStartTimeAsc(poojaId))
                .thenReturn(List.of(slot1, slot2, slot3, slot4));
        when(scheduleRepo.findByPoojaSeva_IdOrderByScheduleDateAscStartTimeAsc(poojaId)).thenReturn(existingList);
        when(registrationRepo.countConfirmedByScheduleId(any())).thenReturn(0L);
        when(reservationRepo.sumConfirmedFamilies(any())).thenReturn(0);

        List<PoojaScheduleDto> dtos = service.getByPooja(poojaId);

        // Verify that stale duplicate rows were deleted
        verify(scheduleRepo).deleteAll(any());
        // Verify remaining schedules exactly equal 4
        assertThat(existingList).hasSize(4);
        assertThat(dtos).hasSize(4);
    }

    @Test
    @DisplayName("ensureSchedulesExist migrates bookings from duplicate schedule to canonical schedule and fixes wrong data")
    void ensureSchedulesExist_migratesBookingsFromDuplicateToCanonicalSchedule() {
        Long poojaId = 10L;
        LocalDate d1 = LocalDate.now().plusDays(5);

        EventPoojaSeva seva = new EventPoojaSeva();
        seva.setId(poojaId);
        seva.setName("Ganesh Pooja");
        seva.setCommunityId(1L);

        // 1 actual time slot with slot_count = 20
        EventPoojaSevaDayTimeSlot slot1 = new EventPoojaSevaDayTimeSlot(poojaId, d1, "09:00", "10:30", "D1 Morning", 20);
        slot1.setId(101L);

        // 2 schedule rows for same date & time (e.g. created by old bugs): 501 and duplicate 502
        EventPoojaSchedule canonical = EventPoojaSchedule.builder()
                .id(501L).poojaSeva(seva).communityId(1L).scheduleDate(d1).startTime(LocalTime.of(9, 0))
                .familyCapacity(10).devoteeCapacity(30).status(PoojaScheduleStatus.OPEN).build();
        EventPoojaSchedule duplicate = EventPoojaSchedule.builder()
                .id(502L).poojaSeva(seva).communityId(1L).scheduleDate(d1).startTime(LocalTime.of(9, 0))
                .familyCapacity(10).devoteeCapacity(30).status(PoojaScheduleStatus.OPEN).build();

        List<EventPoojaSchedule> existingList = new ArrayList<>(List.of(canonical, duplicate));

        when(poojaSevaRepo.findById(poojaId)).thenReturn(Optional.of(seva));
        when(timeSlotRepo.findByPoojaSevaIdOrderBySlotDateAscStartTimeAsc(poojaId)).thenReturn(List.of(slot1));
        when(scheduleRepo.findByPoojaSeva_IdOrderByScheduleDateAscStartTimeAsc(poojaId)).thenReturn(existingList);
        when(registrationRepo.countConfirmedByScheduleId(502L)).thenReturn(0L);
        when(reservationRepo.sumConfirmedFamilies(502L)).thenReturn(0);

        service.getByPooja(poojaId);

        // Verify registrations & reservations on duplicate 502 are migrated to canonical 501
        verify(registrationRepo).migrateScheduleId(502L, 501L);
        verify(reservationRepo).migrateScheduleId(502L, 501L);

        // Verify capacity is self-healed to 20
        assertThat(canonical.getFamilyCapacity()).isEqualTo(20);
        assertThat(canonical.getDevoteeCapacity()).isEqualTo(20);
        assertThat(canonical.getTimeSlotConfigId()).isEqualTo(101L);

        // Verify duplicate 502 is deleted
        verify(scheduleRepo).deleteAll(any());
    }
}
