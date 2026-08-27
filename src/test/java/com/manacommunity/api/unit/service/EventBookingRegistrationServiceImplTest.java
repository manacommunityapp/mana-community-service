package com.manacommunity.api.unit.service;

import com.manacommunity.api.events.entity.EventBookingRegistration;
import com.manacommunity.api.events.entity.EventCulturalEvent;
import com.manacommunity.api.events.entity.EventPoojaSeva;
import com.manacommunity.api.events.entity.EventPoojaSevaDayTimeSlot;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.CompetitionRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.CulturalEventRepository;
import com.manacommunity.api.events.repository.EventPoojaUserRegistrationRepository;
import com.manacommunity.api.events.repository.EventRegistrationRepository;
import com.manacommunity.api.events.repository.EventTicketCategoryRepository;
import com.manacommunity.api.events.repository.LunchDinnerRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.events.service.impl.EventBookingRegistrationServiceImpl;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
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

        EventPoojaSeva seva = new EventPoojaSeva();
        seva.setId(10L);
        seva.setSlots(20);
        seva.setDate(LocalDate.now().plusDays(5));

        when(poojaRepo.findById(10L)).thenReturn(Optional.of(seva));
        when(regRepo.save(any(EventBookingRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        EventBookingRegistrationServiceImpl service = service(regRepo, poojaRepo);
        EventBookingRegistration saved = service.createRegistration(registration("pooja-10", "2026-08-28", "08:30", 3), null, null);

        assertThat(saved).isNotNull();
        assertThat(seva.getSlots()).isEqualTo(17);
    }

    @Test
    @DisplayName("pooja booking decrements specific timeSlotConfig slot for multi-day seva")
    void poojaBookingDecrementsSpecificTimeSlotConfig() {
        EventBookingRegistrationRepository regRepo = mock(EventBookingRegistrationRepository.class);
        PoojaSevaRepository poojaRepo = mock(PoojaSevaRepository.class);

        EventPoojaSevaDayTimeSlot slot1 = new EventPoojaSevaDayTimeSlot(null, LocalDate.of(2026, 8, 28), "08:30", 10);
        EventPoojaSevaDayTimeSlot slot2 = new EventPoojaSevaDayTimeSlot(null, LocalDate.of(2026, 8, 28), "18:30", 10);

        EventPoojaSeva seva = new EventPoojaSeva();
        seva.setId(20L);
        seva.setMultiDay(true);
        seva.setDate(LocalDate.now().plusDays(2));
        seva.setEndDate(LocalDate.now().plusDays(4));
        seva.setTimeSlotConfig(List.of(slot1, slot2));

        when(poojaRepo.findById(20L)).thenReturn(Optional.of(seva));
        when(regRepo.save(any(EventBookingRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        EventBookingRegistrationServiceImpl service = service(regRepo, poojaRepo);
        EventBookingRegistration saved = service.createRegistration(registration("pooja-20", "2026-08-28", "08:30", 3), null, null);

        assertThat(saved).isNotNull();
        assertThat(slot1.getSlotCount()).isEqualTo(7);
        assertThat(slot2.getSlotCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("createRegistration throws AlreadyRegisteredException when user already registered for activity")
    void createRegistration_throwsAlreadyRegisteredException_whenActivityAlreadyRegistered() {
        EventBookingRegistrationRepository regRepo = mock(EventBookingRegistrationRepository.class);
        PoojaSevaRepository poojaRepo = mock(PoojaSevaRepository.class);
        AppUser user = AppUser.builder().id(99L).role("MEMBER").build();

        when(regRepo.existsByUserIdAndActivityIdAndStatusNot(99L, "pooja-10", "CANCELLED")).thenReturn(true);

        EventBookingRegistrationServiceImpl service = service(regRepo, poojaRepo);
        EventBookingRegistration req = registration("pooja-10", "2026-08-28", "08:30", 1);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createRegistration(req, user, null))
                .isInstanceOf(com.manacommunity.api.exception.AlreadyRegisteredException.class);
    }

    @Test
    @DisplayName("createRegistration throws AlreadyRegisteredException when user already registered for cultural event")
    void createRegistration_throwsAlreadyRegisteredException_whenCulturalAlreadyRegistered() {
        EventBookingRegistrationRepository regRepo = mock(EventBookingRegistrationRepository.class);
        CulturalEventRepository culturalRepo = mock(CulturalEventRepository.class);
        AppUser user = AppUser.builder().id(99L).role("MEMBER").build();

        EventCulturalEvent cultural = new EventCulturalEvent();
        cultural.setId(5L);
        cultural.setName("Dance Show");
        when(culturalRepo.findById(5L)).thenReturn(Optional.of(cultural));
        when(regRepo.existsByUserIdAndActivityIdAndStatusNot(99L, "cultural-5", "CANCELLED")).thenReturn(true);

        EventBookingRegistrationServiceImpl service = new EventBookingRegistrationServiceImpl(
                regRepo,
                mock(EventPoojaUserRegistrationRepository.class),
                mock(CommunityRepository.class),
                mock(PoojaSevaRepository.class),
                mock(LunchDinnerRepository.class),
                mock(CompetitionRepository.class),
                culturalRepo,
                mock(EventCommunityRepository.class),
                mock(EventRegistrationRepository.class),
                mock(EventTicketCategoryRepository.class),
                mock(AppUserRepository.class)
        );

        EventBookingRegistration req = registration("cultural-5", "2026-08-28", "18:00", 1);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createRegistration(req, user, null))
                .isInstanceOf(com.manacommunity.api.exception.AlreadyRegisteredException.class);
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
                mock(EventPoojaUserRegistrationRepository.class),
                mock(CommunityRepository.class),
                poojaSevaRepository,
                mock(LunchDinnerRepository.class),
                mock(CompetitionRepository.class),
                mock(CulturalEventRepository.class),
                mock(EventCommunityRepository.class),
                mock(EventRegistrationRepository.class),
                mock(EventTicketCategoryRepository.class),
                mock(AppUserRepository.class)
        );
    }
}
