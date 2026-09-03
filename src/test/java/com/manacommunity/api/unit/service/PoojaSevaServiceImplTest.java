package com.manacommunity.api.unit.service;

import com.manacommunity.api.events.entity.EventPoojaSeva;
import com.manacommunity.api.events.entity.EventPoojaSevaDayTimeSlot;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.EventPoojaSevaTimeSlotRepository;
import com.manacommunity.api.events.repository.EventPoojaUserRegistrationRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.events.repository.PoojaTypeRepository;
import com.manacommunity.api.events.service.impl.PoojaSevaServiceImpl;
import com.manacommunity.api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PoojaSevaServiceImpl")
class PoojaSevaServiceImplTest {

    @Test
    @DisplayName("create keeps single-day availability on top-level slots")
    void createSingleDay_usesTopLevelSlots() {
        PoojaSevaRepository repository = mock(PoojaSevaRepository.class);
        EventCommunityRepository eventRepository = mock(EventCommunityRepository.class);
        EventBookingRegistrationRepository bookingRepo = mock(EventBookingRegistrationRepository.class);
        PoojaTypeRepository poojaTypeRepository = mock(PoojaTypeRepository.class);
        EventPoojaSevaTimeSlotRepository timeSlotRepository = mock(EventPoojaSevaTimeSlotRepository.class);
        EventPoojaUserRegistrationRepository poojaRegRepo = mock(EventPoojaUserRegistrationRepository.class);
        when(repository.save(any(EventPoojaSeva.class))).thenAnswer(inv -> inv.getArgument(0));
        PoojaSevaServiceImpl service = new PoojaSevaServiceImpl(repository, eventRepository, bookingRepo, poojaTypeRepository, timeSlotRepository, poojaRegRepo);

        EventPoojaSeva seva = new EventPoojaSeva();
        seva.setName("Ganesh Puja");
        seva.setType("Ganesh Puja");
        seva.setDate(LocalDate.of(2026, 9, 1));
        seva.setMultiDay(false);
        seva.setSlots(25);

        EventPoojaSeva saved = service.createPoojaSeva(10L, seva);

        assertThat(saved.getCommunityId()).isEqualTo(10L);
        assertThat(saved.getSlots()).isEqualTo(25);
        assertThat(saved.getTimeSlotConfig()).isEmpty();
    }

    @Test
    @DisplayName("create expands multi-day availability into every date and time slot")
    void createMultiDay_expandsTimeSlotAvailability() {
        PoojaSevaRepository repository = mock(PoojaSevaRepository.class);
        EventCommunityRepository eventRepository = mock(EventCommunityRepository.class);
        EventBookingRegistrationRepository bookingRepo = mock(EventBookingRegistrationRepository.class);
        PoojaTypeRepository poojaTypeRepository = mock(PoojaTypeRepository.class);
        EventPoojaSevaTimeSlotRepository timeSlotRepository = mock(EventPoojaSevaTimeSlotRepository.class);
        EventPoojaUserRegistrationRepository poojaRegRepo = mock(EventPoojaUserRegistrationRepository.class);
        when(repository.save(any(EventPoojaSeva.class))).thenAnswer(inv -> inv.getArgument(0));

        // Capture what saveAll receives so the subsequent findBy query can return the same slots
        @SuppressWarnings("unchecked")
        List<EventPoojaSevaDayTimeSlot>[] slotHolder = new List[]{new ArrayList<>()};
        when(timeSlotRepository.saveAll(any())).thenAnswer(inv -> {
            Iterable<EventPoojaSevaDayTimeSlot> arg = inv.getArgument(0);
            List<EventPoojaSevaDayTimeSlot> list = new ArrayList<>();
            arg.forEach(list::add);
            slotHolder[0] = list;
            return list;
        });
        when(timeSlotRepository.findByPoojaSevaIdOrderBySlotDateAscStartTimeAsc(any()))
                .thenAnswer(inv -> slotHolder[0]);

        PoojaSevaServiceImpl service = new PoojaSevaServiceImpl(repository, eventRepository, bookingRepo, poojaTypeRepository, timeSlotRepository, poojaRegRepo);

        EventPoojaSeva seva = new EventPoojaSeva();
        seva.setName("Festival Archana");
        seva.setType("Archana");
        seva.setDate(LocalDate.of(2026, 9, 1));
        seva.setEndDate(LocalDate.of(2026, 9, 2));
        seva.setMultiDay(true);
        seva.setStartTimes(List.of("08:00", "10:00"));
        seva.setSlots(12);

        EventPoojaSeva saved = service.createPoojaSeva(10L, seva);

        assertThat(saved.getTimeSlotConfig()).hasSize(4);
        assertThat(saved.getTimeSlotConfig())
                .allSatisfy(slot -> assertThat(slot.getSlotCount()).isEqualTo(12));
        assertThat(saved.getTimeSlotConfig())
                .extracting(slot -> slot.getSlotDate() + " " + slot.getStartTime())
                .containsExactly("2026-09-01 08:00", "2026-09-01 10:00",
                        "2026-09-02 08:00", "2026-09-02 10:00");
    }

    @Test
    @DisplayName("getById is scoped to the caller's community")
    void getById_usesCommunityScopedLookup() {
        PoojaSevaRepository repository = mock(PoojaSevaRepository.class);
        EventCommunityRepository eventRepository = mock(EventCommunityRepository.class);
        EventBookingRegistrationRepository bookingRepo = mock(EventBookingRegistrationRepository.class);
        PoojaTypeRepository poojaTypeRepository = mock(PoojaTypeRepository.class);
        EventPoojaSevaTimeSlotRepository timeSlotRepository = mock(EventPoojaSevaTimeSlotRepository.class);
        EventPoojaUserRegistrationRepository poojaRegRepo = mock(EventPoojaUserRegistrationRepository.class);
        PoojaSevaServiceImpl service = new PoojaSevaServiceImpl(repository, eventRepository, bookingRepo, poojaTypeRepository, timeSlotRepository, poojaRegRepo);

        EventPoojaSeva seva = new EventPoojaSeva();
        seva.setId(5L);
        seva.setCommunityId(10L);
        when(repository.findByIdAndCommunityId(5L, 10L)).thenReturn(Optional.of(seva));

        EventPoojaSeva result = service.getPoojaSevaById(5L, 10L);

        assertThat(result).isSameAs(seva);
        verify(repository).findByIdAndCommunityId(5L, 10L);
    }

    @Test
    @DisplayName("listing by main event validates parent event and remains community scoped")
    void getAllByMainEvent_usesCommunityScopedLookup() {
        PoojaSevaRepository repository = mock(PoojaSevaRepository.class);
        EventCommunityRepository eventRepository = mock(EventCommunityRepository.class);
        EventBookingRegistrationRepository bookingRepo = mock(EventBookingRegistrationRepository.class);
        PoojaTypeRepository poojaTypeRepository = mock(PoojaTypeRepository.class);
        EventPoojaSevaTimeSlotRepository timeSlotRepository = mock(EventPoojaSevaTimeSlotRepository.class);
        EventPoojaUserRegistrationRepository poojaRegRepo = mock(EventPoojaUserRegistrationRepository.class);
        PoojaSevaServiceImpl service = new PoojaSevaServiceImpl(repository, eventRepository, bookingRepo, poojaTypeRepository, timeSlotRepository, poojaRegRepo);

        EventPoojaSeva seva = new EventPoojaSeva();
        when(repository.findByCommunityIdAndMainEventIdOrderByDateAscStartTimeAsc(10L, 100L))
                .thenReturn(List.of(seva));

        List<EventPoojaSeva> result = service.getAllPoojaSevas(10L, 100L);

        assertThat(result).containsExactly(seva);
        verify(repository).findByCommunityIdAndMainEventIdOrderByDateAscStartTimeAsc(10L, 100L);
    }

    @Test
    @DisplayName("create rejects a main event outside the caller's community")
    void create_rejectsMissingOrWrongCommunityMainEvent() {
        PoojaSevaRepository repository = mock(PoojaSevaRepository.class);
        EventCommunityRepository eventRepository = mock(EventCommunityRepository.class);
        EventBookingRegistrationRepository bookingRepo = mock(EventBookingRegistrationRepository.class);
        PoojaTypeRepository poojaTypeRepository = mock(PoojaTypeRepository.class);
        EventPoojaSevaTimeSlotRepository timeSlotRepository = mock(EventPoojaSevaTimeSlotRepository.class);
        EventPoojaUserRegistrationRepository poojaRegRepo = mock(EventPoojaUserRegistrationRepository.class);
        PoojaSevaServiceImpl service = new PoojaSevaServiceImpl(repository, eventRepository, bookingRepo, poojaTypeRepository, timeSlotRepository, poojaRegRepo);

        EventPoojaSeva seva = new EventPoojaSeva();
        seva.setName("Ganesh Puja");
        seva.setType("Ganesh Puja");
        seva.setMainEventId(100L);
        when(eventRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPoojaSeva(10L, seva))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any(EventPoojaSeva.class));
    }

    @Test
    @DisplayName("update rejects moving a pooja to a main event outside the caller's community")
    void update_rejectsWrongCommunityMainEvent() {
        PoojaSevaRepository repository = mock(PoojaSevaRepository.class);
        EventCommunityRepository eventRepository = mock(EventCommunityRepository.class);
        EventBookingRegistrationRepository bookingRepo = mock(EventBookingRegistrationRepository.class);
        PoojaTypeRepository poojaTypeRepository = mock(PoojaTypeRepository.class);
        EventPoojaSevaTimeSlotRepository timeSlotRepository = mock(EventPoojaSevaTimeSlotRepository.class);
        EventPoojaUserRegistrationRepository poojaRegRepo = mock(EventPoojaUserRegistrationRepository.class);
        PoojaSevaServiceImpl service = new PoojaSevaServiceImpl(repository, eventRepository, bookingRepo, poojaTypeRepository, timeSlotRepository, poojaRegRepo);

        EventPoojaSeva existing = new EventPoojaSeva();
        existing.setId(5L);
        existing.setCommunityId(10L);
        EventPoojaSeva updated = new EventPoojaSeva();
        updated.setMainEventId(100L);
        when(repository.findByIdAndCommunityId(5L, 10L)).thenReturn(Optional.of(existing));
        when(eventRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePoojaSeva(5L, 10L, updated))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any(EventPoojaSeva.class));
    }

    @Test
    @DisplayName("create with explicit timeSlotConfig preserves slot startTime and endTime")
    void create_preservesExplicitSlotStartAndEndTimes() {
        PoojaSevaRepository repository = mock(PoojaSevaRepository.class);
        EventCommunityRepository eventRepository = mock(EventCommunityRepository.class);
        EventBookingRegistrationRepository bookingRepo = mock(EventBookingRegistrationRepository.class);
        PoojaTypeRepository poojaTypeRepository = mock(PoojaTypeRepository.class);
        EventPoojaSevaTimeSlotRepository timeSlotRepository = mock(EventPoojaSevaTimeSlotRepository.class);
        EventPoojaUserRegistrationRepository poojaRegRepo = mock(EventPoojaUserRegistrationRepository.class);
        when(repository.save(any(EventPoojaSeva.class))).thenAnswer(inv -> {
            EventPoojaSeva p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });

        @SuppressWarnings("unchecked")
        List<EventPoojaSevaDayTimeSlot>[] slotHolder = new List[]{new ArrayList<>()};
        when(timeSlotRepository.saveAll(any())).thenAnswer(inv -> {
            Iterable<EventPoojaSevaDayTimeSlot> arg = inv.getArgument(0);
            List<EventPoojaSevaDayTimeSlot> list = new ArrayList<>();
            arg.forEach(list::add);
            slotHolder[0] = list;
            return list;
        });
        when(timeSlotRepository.findByPoojaSevaIdOrderBySlotDateAscStartTimeAsc(99L))
                .thenAnswer(inv -> slotHolder[0]);

        PoojaSevaServiceImpl service = new PoojaSevaServiceImpl(repository, eventRepository, bookingRepo, poojaTypeRepository, timeSlotRepository, poojaRegRepo);

        EventPoojaSeva seva = new EventPoojaSeva();
        seva.setName("Ganesh Puja");
        seva.setType("Ganesh Puja");
        seva.setDate(LocalDate.of(2026, 8, 28));
        seva.setEndDate(LocalDate.of(2026, 8, 29));
        seva.setMultiDay(true);
        seva.setStartTime(java.time.LocalTime.of(9, 0));
        seva.setEndTime(java.time.LocalTime.of(10, 30));

        EventPoojaSevaDayTimeSlot slot1 = new EventPoojaSevaDayTimeSlot();
        slot1.setSlotDate(LocalDate.of(2026, 8, 28));
        slot1.setStartTime("09:00");
        slot1.setEndTime("10:30");
        slot1.setTitle("Pooja 1");
        slot1.setSlotCount(40);

        EventPoojaSevaDayTimeSlot slot2 = new EventPoojaSevaDayTimeSlot();
        slot2.setSlotDate(LocalDate.of(2026, 8, 29));
        slot2.setStartTime("09:00");
        slot2.setEndTime("10:30");
        slot2.setTitle("Pooja 1");
        slot2.setSlotCount(40);

        seva.setTimeSlotConfig(List.of(slot1, slot2));

        EventPoojaSeva saved = service.createPoojaSeva(10L, seva);

        assertThat(saved.getTimeSlotConfig()).hasSize(2);
        assertThat(saved.getTimeSlotConfig().get(0).getEndTime()).isEqualTo("10:30");
        assertThat(saved.getTimeSlotConfig().get(0).getId()).isNotNull();
        assertThat(saved.getTimeSlotConfig().get(1).getEndTime()).isEqualTo("10:30");
        assertThat(saved.getTimeSlotConfig().get(1).getId()).isNotNull();
    }

    @Test
    @DisplayName("create falls back to parent endTime when slot endTime is omitted")
    void create_fallsBackToParentEndTimeWhenSlotEndTimeOmitted() {
        PoojaSevaRepository repository = mock(PoojaSevaRepository.class);
        EventCommunityRepository eventRepository = mock(EventCommunityRepository.class);
        EventBookingRegistrationRepository bookingRepo = mock(EventBookingRegistrationRepository.class);
        PoojaTypeRepository poojaTypeRepository = mock(PoojaTypeRepository.class);
        EventPoojaSevaTimeSlotRepository timeSlotRepository = mock(EventPoojaSevaTimeSlotRepository.class);
        EventPoojaUserRegistrationRepository poojaRegRepo = mock(EventPoojaUserRegistrationRepository.class);
        when(repository.save(any(EventPoojaSeva.class))).thenAnswer(inv -> {
            EventPoojaSeva p = inv.getArgument(0);
            p.setId(101L);
            return p;
        });

        @SuppressWarnings("unchecked")
        List<EventPoojaSevaDayTimeSlot>[] slotHolder = new List[]{new ArrayList<>()};
        when(timeSlotRepository.saveAll(any())).thenAnswer(inv -> {
            Iterable<EventPoojaSevaDayTimeSlot> arg = inv.getArgument(0);
            List<EventPoojaSevaDayTimeSlot> list = new ArrayList<>();
            arg.forEach(list::add);
            slotHolder[0] = list;
            return list;
        });
        when(timeSlotRepository.findByPoojaSevaIdOrderBySlotDateAscStartTimeAsc(101L))
                .thenAnswer(inv -> slotHolder[0]);

        PoojaSevaServiceImpl service = new PoojaSevaServiceImpl(repository, eventRepository, bookingRepo, poojaTypeRepository, timeSlotRepository, poojaRegRepo);

        EventPoojaSeva seva = new EventPoojaSeva();
        seva.setName("Ganesh Puja");
        seva.setType("Ganesh Puja");
        seva.setDate(LocalDate.of(2026, 8, 28));
        seva.setStartTime(java.time.LocalTime.of(9, 0));
        seva.setEndTime(java.time.LocalTime.of(10, 30));

        EventPoojaSevaDayTimeSlot slot = new EventPoojaSevaDayTimeSlot();
        slot.setSlotDate(LocalDate.of(2026, 8, 28));
        slot.setStartTime("09:00");
        slot.setTitle("Morning Pooja");
        slot.setSlotCount(25);
        seva.setTimeSlotConfig(List.of(slot));

        EventPoojaSeva saved = service.createPoojaSeva(10L, seva);

        assertThat(saved.getTimeSlotConfig()).hasSize(1);
        assertThat(saved.getTimeSlotConfig().get(0).getEndTime()).isEqualTo("10:30");
        assertThat(saved.getTimeSlotConfig().get(0).getId()).isNotNull();
    }
}
