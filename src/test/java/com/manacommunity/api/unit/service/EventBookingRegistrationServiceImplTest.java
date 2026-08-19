package com.manacommunity.api.unit.service;

import com.manacommunity.api.events.entity.EventBookingRegistration;
import com.manacommunity.api.events.entity.PoojaSeva;
import com.manacommunity.api.events.entity.PoojaSevaDayTimeSlot;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.CompetitionRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.LunchDinnerRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.events.service.impl.EventBookingRegistrationServiceImpl;
import com.manacommunity.api.repository.CommunityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("EventBookingRegistrationServiceImpl")
class EventBookingRegistrationServiceImplTest {

    @Test
    @DisplayName("pooja booking decrements top-level slots for single-day seva")
    void createRegistration_decrementsSingleDayPoojaSlots() {
        EventBookingRegistrationRepository registrationRepository = mock(EventBookingRegistrationRepository.class);
        PoojaSevaRepository poojaSevaRepository = mock(PoojaSevaRepository.class);
        when(registrationRepository.save(any(EventBookingRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        PoojaSeva seva = new PoojaSeva();
        seva.setId(7L);
        seva.setMultiDay(false);
        seva.setSlots(10);
        when(poojaSevaRepository.findById(7L)).thenReturn(Optional.of(seva));

        EventBookingRegistrationServiceImpl service = service(registrationRepository, poojaSevaRepository);

        service.createRegistration(registration("pooja-7", "2026-09-01", "08:00", 2), null, null);

        assertThat(seva.getSlots()).isEqualTo(8);
    }

    @Test
    @DisplayName("pooja booking decrements matching date/time slot for multi-day seva")
    void createRegistration_decrementsMultiDayPoojaTimeSlot() {
        EventBookingRegistrationRepository registrationRepository = mock(EventBookingRegistrationRepository.class);
        PoojaSevaRepository poojaSevaRepository = mock(PoojaSevaRepository.class);
        when(registrationRepository.save(any(EventBookingRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        PoojaSeva seva = new PoojaSeva();
        seva.setId(7L);
        seva.setMultiDay(true);
        seva.setSlots(99);
        seva.setTimeSlotConfig(List.of(
                new PoojaSevaDayTimeSlot(LocalDate.of(2026, 9, 1), "08:00", 10),
                new PoojaSevaDayTimeSlot(LocalDate.of(2026, 9, 2), "08:00", 10)
        ));
        when(poojaSevaRepository.findById(7L)).thenReturn(Optional.of(seva));

        EventBookingRegistrationServiceImpl service = service(registrationRepository, poojaSevaRepository);

        service.createRegistration(registration("pooja-7", "2026-09-02", "08:00", 3), null, null);

        assertThat(seva.getSlots()).isEqualTo(99);
        assertThat(seva.getTimeSlotConfig().get(0).getSlotCount()).isEqualTo(10);
        assertThat(seva.getTimeSlotConfig().get(1).getSlotCount()).isEqualTo(7);
    }

    private EventBookingRegistration registration(String activityId, String eventDate, String eventTime, int devoteeCount) {
        EventBookingRegistration registration = new EventBookingRegistration();
        registration.setActivityId(activityId);
        registration.setActivityTitle("Ganesh Puja");
        registration.setCategory("POOJA");
        registration.setParticipantName("Devotee");
        registration.setEventDate(eventDate);
        registration.setEventTime(eventTime);
        registration.setDevoteeCount(devoteeCount);
        registration.setBookingFee(0.0);
        return registration;
    }

    private EventBookingRegistrationServiceImpl service(
            EventBookingRegistrationRepository registrationRepository,
            PoojaSevaRepository poojaSevaRepository) {
        return new EventBookingRegistrationServiceImpl(
                registrationRepository,
                mock(CommunityRepository.class),
                poojaSevaRepository,
                mock(LunchDinnerRepository.class),
                mock(CompetitionRepository.class),
                mock(CommunityEventRepository.class)
        );
    }
}
