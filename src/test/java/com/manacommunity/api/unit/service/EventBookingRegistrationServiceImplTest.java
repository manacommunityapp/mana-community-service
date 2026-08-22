package com.manacommunity.api.unit.service;

import com.manacommunity.api.events.entity.EventBookingRegistration;
import com.manacommunity.api.events.entity.PoojaSeva;
import com.manacommunity.api.events.entity.PoojaSevaDayTimeSlot;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.CompetitionRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.CulturalEventRepository;
import com.manacommunity.api.events.repository.EventRegistrationRepository;
import com.manacommunity.api.events.repository.EventTicketCategoryRepository;
import com.manacommunity.api.events.repository.LunchDinnerRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.events.service.impl.EventBookingRegistrationServiceImpl;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.repository.AppUserRepository;
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
    void poojaBookingDecrementsTopLevelSlotsForSingleDaySeva() {
        EventBookingRegistrationRepository regRepo = mock(EventBookingRegistrationRepository.class);
        PoojaSevaRepository poojaRepo = mock(PoojaSevaRepository.class);

        PoojaSeva seva = new PoojaSeva();
        seva.setId(10L);
        seva.setSlots(20);
        when(poojaRepo.findById(10L)).thenReturn(Optional.of(seva));
        when(regRepo.save(any(EventBookingRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        EventBookingRegistrationServiceImpl service = service(regRepo, poojaRepo);
        EventBookingRegistration saved = service.createRegistration(registration("pooja-10", "2026-08-25", "08:30 AM", 2), null, 1L);

        assertThat(saved).isNotNull();
        assertThat(seva.getSlots()).isEqualTo(18);
    }

    @Test
    @DisplayName("pooja booking decrements matching time-slot config")
    void poojaBookingDecrementsMatchingTimeSlotConfig() {
        EventBookingRegistrationRepository regRepo = mock(EventBookingRegistrationRepository.class);
        PoojaSevaRepository poojaRepo = mock(PoojaSevaRepository.class);

        PoojaSevaDayTimeSlot slot1 = new PoojaSevaDayTimeSlot(LocalDate.of(2026, 8, 25), "08:30", 10);
        PoojaSevaDayTimeSlot slot2 = new PoojaSevaDayTimeSlot(LocalDate.of(2026, 8, 25), "10:30", 10);

        PoojaSeva seva = new PoojaSeva();
        seva.setId(11L);
        seva.setSlots(20);
        seva.setTimeSlotConfig(List.of(slot1, slot2));
        when(poojaRepo.findById(11L)).thenReturn(Optional.of(seva));
        when(regRepo.save(any(EventBookingRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        EventBookingRegistrationServiceImpl service = service(regRepo, poojaRepo);
        EventBookingRegistration saved = service.createRegistration(registration("pooja-11", "2026-08-25", "08:30 AM", 3), null, 1L);

        assertThat(saved).isNotNull();
        assertThat(slot1.getSlotCount()).isEqualTo(7);
        assertThat(slot2.getSlotCount()).isEqualTo(10);
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
                mock(CulturalEventRepository.class),
                mock(CommunityEventRepository.class),
                mock(EventRegistrationRepository.class),
                mock(EventTicketCategoryRepository.class),
                mock(AppUserRepository.class)
        );
    }
}
