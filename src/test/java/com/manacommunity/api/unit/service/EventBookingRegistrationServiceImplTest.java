package com.manacommunity.api.unit.service;

import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.entity.Competition;
import com.manacommunity.api.events.entity.EventBookingRegistration;
import com.manacommunity.api.events.entity.LunchDinner;
import com.manacommunity.api.events.entity.PoojaSeva;
import com.manacommunity.api.events.entity.PoojaSevaDayTimeSlot;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.CompetitionRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.LunchDinnerRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.events.service.impl.EventBookingRegistrationServiceImpl;
import com.manacommunity.api.exception.EventFullException;
import com.manacommunity.api.exception.RegistrationClosedException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.support.TestDataBuilder;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventBookingRegistrationServiceImpl")
class EventBookingRegistrationServiceImplTest {

    private EventBookingRegistrationRepository registrationRepository;
    private CommunityRepository communityRepository;
    private PoojaSevaRepository poojaSevaRepository;
    private LunchDinnerRepository lunchDinnerRepository;
    private CompetitionRepository competitionRepository;
    private CommunityEventRepository communityEventRepository;
    private AppUserRepository appUserRepository;
    private EventBookingRegistrationServiceImpl service;

    @BeforeEach
    void setUp() {
        registrationRepository = mock(EventBookingRegistrationRepository.class);
        communityRepository = mock(CommunityRepository.class);
        poojaSevaRepository = mock(PoojaSevaRepository.class);
        lunchDinnerRepository = mock(LunchDinnerRepository.class);
        competitionRepository = mock(CompetitionRepository.class);
        communityEventRepository = mock(CommunityEventRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        service = new EventBookingRegistrationServiceImpl(
                registrationRepository, communityRepository, poojaSevaRepository,
                lunchDinnerRepository, competitionRepository, communityEventRepository,
                appUserRepository);
        lenient().when(registrationRepository.save(any(EventBookingRegistration.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ── Slot Decrement ───────────────────────────────────────────────

    @Nested
    @DisplayName("Slot Decrement on Registration")
    class SlotDecrement {

        @Test
        @DisplayName("pooja booking decrements top-level slots for single-day seva")
        void decrementsSingleDayPoojaSlots() {
            PoojaSeva seva = new PoojaSeva();
            seva.setId(7L);
            seva.setMultiDay(false);
            seva.setSlots(10);
            when(poojaSevaRepository.findById(7L)).thenReturn(Optional.of(seva));

            service.createRegistration(registration("pooja-7", "2026-09-01", "08:00", 2), null, null);

            assertThat(seva.getSlots()).isEqualTo(8);
            verify(poojaSevaRepository).save(seva);
        }

        @Test
        @DisplayName("pooja booking decrements matching date/time slot for multi-day seva")
        void decrementsMultiDayPoojaTimeSlot() {
            PoojaSeva seva = new PoojaSeva();
            seva.setId(7L);
            seva.setMultiDay(true);
            seva.setSlots(99);
            seva.setTimeSlotConfig(List.of(
                    new PoojaSevaDayTimeSlot(LocalDate.of(2026, 9, 1), "08:00", 10),
                    new PoojaSevaDayTimeSlot(LocalDate.of(2026, 9, 2), "08:00", 10)
            ));
            when(poojaSevaRepository.findById(7L)).thenReturn(Optional.of(seva));

            service.createRegistration(registration("pooja-7", "2026-09-02", "08:00", 3), null, null);

            assertThat(seva.getSlots()).isEqualTo(99);
            assertThat(seva.getTimeSlotConfig().get(0).getSlotCount()).isEqualTo(10);
            assertThat(seva.getTimeSlotConfig().get(1).getSlotCount()).isEqualTo(7);
        }

        @Test
        @DisplayName("event booking decrements capacity and maxAttendees")
        void decrementsEventCapacity() {
            CommunityEvent event = TestDataBuilder.communityEvent(50L, null, TestDataBuilder.adminUser());
            event.setCapacity(100);
            event.setMaxAttendees(100);
            when(communityEventRepository.findById(50L)).thenReturn(Optional.of(event));

            service.createRegistration(registration("event-50", "2026-09-01", "08:00", 3), null, null);

            assertThat(event.getCapacity()).isEqualTo(97);
            assertThat(event.getMaxAttendees()).isEqualTo(97);
            verify(communityEventRepository).save(event);
        }

        @Test
        @DisplayName("food booking decrements target plates")
        void decrementsFoodPlates() {
            LunchDinner meal = new LunchDinner();
            meal.setId(20L);
            meal.setName("Prasadam");
            meal.setTargetPlates(500);
            when(lunchDinnerRepository.findById(20L)).thenReturn(Optional.of(meal));

            service.createRegistration(registration("food-20", "2026-09-01", "12:00", 4), null, null);

            assertThat(meal.getTargetPlates()).isEqualTo(496);
            verify(lunchDinnerRepository).save(meal);
        }

        @Test
        @DisplayName("competition booking decrements maxParticipants")
        void decrementsCompetitionSlots() {
            Competition comp = new Competition();
            comp.setId(30L);
            comp.setName("Quiz");
            comp.setMaxParticipants(50);
            when(competitionRepository.findById(30L)).thenReturn(Optional.of(comp));

            service.createRegistration(registration("comp-30", "2026-09-01", "14:00", 2), null, null);

            assertThat(comp.getMaxParticipants()).isEqualTo(48);
            verify(competitionRepository).save(comp);
        }

        @Test
        @DisplayName("slot decrement floors at zero")
        void decrementFloorsAtZero() {
            PoojaSeva seva = new PoojaSeva();
            seva.setId(7L);
            seva.setMultiDay(false);
            seva.setSlots(1);
            when(poojaSevaRepository.findById(7L)).thenReturn(Optional.of(seva));

            service.createRegistration(registration("pooja-7", "2026-09-01", "08:00", 5), null, null);

            assertThat(seva.getSlots()).isEqualTo(0);
        }

        @Test
        @DisplayName("plain numeric activityId decrements event capacity")
        void decrementsPlainNumericActivityId() {
            CommunityEvent event = TestDataBuilder.communityEvent(42L, null, TestDataBuilder.adminUser());
            event.setCapacity(10);
            when(communityEventRepository.findById(42L)).thenReturn(Optional.of(event));

            service.createRegistration(registration("42", "2026-09-01", "08:00", 1), null, null);

            assertThat(event.getCapacity()).isEqualTo(9);
        }
    }

    // ── Capacity & Deadline Validation ───────────────────────────────

    @Nested
    @DisplayName("Capacity and Deadline Validation")
    class CapacityDeadlineValidation {

        @Test
        @DisplayName("non-admin rejected when pooja slots are zero")
        void rejectsWhenPoojaSlotsZero() {
            PoojaSeva seva = new PoojaSeva();
            seva.setId(7L);
            seva.setName("Full Pooja");
            seva.setMultiDay(false);
            seva.setSlots(0);
            when(poojaSevaRepository.findById(7L)).thenReturn(Optional.of(seva));

            AppUser regularUser = TestDataBuilder.memberUser();

            assertThatThrownBy(() ->
                    service.createRegistration(registration("pooja-7", "2026-09-01", "08:00", 1), regularUser, null))
                    .isInstanceOf(EventFullException.class);
        }

        @Test
        @DisplayName("non-admin rejected when event capacity is zero")
        void rejectsWhenEventCapacityZero() {
            CommunityEvent event = TestDataBuilder.communityEvent(50L, null, TestDataBuilder.adminUser());
            event.setCapacity(0);
            when(communityEventRepository.findById(50L)).thenReturn(Optional.of(event));

            AppUser regularUser = TestDataBuilder.memberUser();

            assertThatThrownBy(() ->
                    service.createRegistration(registration("event-50", "2026-09-01", "08:00", 1), regularUser, null))
                    .isInstanceOf(EventFullException.class);
        }

        @Test
        @DisplayName("non-admin rejected when event registration deadline has passed")
        void rejectsWhenDeadlinePassed() {
            CommunityEvent event = TestDataBuilder.communityEvent(50L, null, TestDataBuilder.adminUser());
            event.setCapacity(100);
            event.setRegistrationDeadline(LocalDate.now().minusDays(1));
            when(communityEventRepository.findById(50L)).thenReturn(Optional.of(event));

            AppUser regularUser = TestDataBuilder.memberUser();

            assertThatThrownBy(() ->
                    service.createRegistration(registration("event-50", "2026-09-01", "08:00", 1), regularUser, null))
                    .isInstanceOf(RegistrationClosedException.class);
        }

        @Test
        @DisplayName("non-admin rejected when food plates are zero")
        void rejectsWhenFoodPlatesZero() {
            LunchDinner meal = new LunchDinner();
            meal.setId(20L);
            meal.setName("Full Prasadam");
            meal.setTargetPlates(0);
            when(lunchDinnerRepository.findById(20L)).thenReturn(Optional.of(meal));

            AppUser regularUser = TestDataBuilder.memberUser();

            assertThatThrownBy(() ->
                    service.createRegistration(registration("food-20", "2026-09-01", "12:00", 1), regularUser, null))
                    .isInstanceOf(EventFullException.class);
        }

        @Test
        @DisplayName("non-admin rejected when competition maxParticipants is zero")
        void rejectsWhenCompetitionFull() {
            Competition comp = new Competition();
            comp.setId(30L);
            comp.setName("Full Quiz");
            comp.setMaxParticipants(0);
            when(competitionRepository.findById(30L)).thenReturn(Optional.of(comp));

            AppUser regularUser = TestDataBuilder.memberUser();

            assertThatThrownBy(() ->
                    service.createRegistration(registration("comp-30", "2026-09-01", "14:00", 1), regularUser, null))
                    .isInstanceOf(EventFullException.class);
        }
    }

    // ── Admin Override ───────────────────────────────────────────────

    @Nested
    @DisplayName("Admin Override and On-Behalf Registration")
    class AdminOverride {

        @Test
        @DisplayName("adminOverride=true bypasses capacity validation")
        void adminOverride_bypassesCapacity() {
            PoojaSeva seva = new PoojaSeva();
            seva.setId(7L);
            seva.setName("Full Pooja");
            seva.setMultiDay(false);
            seva.setSlots(0);
            when(poojaSevaRepository.findById(7L)).thenReturn(Optional.of(seva));

            AppUser regularUser = TestDataBuilder.memberUser();

            EventBookingRegistration result = service.createRegistration(
                    registration("pooja-7", "2026-09-01", "08:00", 1), regularUser, null, true);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        }

        @Test
        @DisplayName("admin role bypasses capacity validation without adminOverride flag")
        void adminRole_bypassesCapacity() {
            CommunityEvent event = TestDataBuilder.communityEvent(50L, null, TestDataBuilder.adminUser());
            event.setCapacity(0);
            when(communityEventRepository.findById(50L)).thenReturn(Optional.of(event));

            AppUser adminUser = TestDataBuilder.appUser(1L, "admin@test.com", "ADMIN");

            EventBookingRegistration result = service.createRegistration(
                    registration("event-50", "2026-09-01", "08:00", 1), adminUser, null);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("admin registers on behalf of another user")
        void adminOnBehalf() {
            AppUser adminUser = TestDataBuilder.appUser(1L, "admin@test.com", "ADMIN");
            AppUser targetUser = TestDataBuilder.appUser(99L, "target@test.com", "MEMBER");
            when(appUserRepository.findById(99L)).thenReturn(Optional.of(targetUser));

            EventBookingRegistration reg = registration("event-50", "2026-09-01", "08:00", 1);
            reg.setUser(targetUser);

            EventBookingRegistration result = service.createRegistration(reg, adminUser, null);

            assertThat(result.getUser()).isEqualTo(targetUser);
        }

        @Test
        @DisplayName("admin on-behalf falls back to admin when target user not found")
        void adminOnBehalf_fallback() {
            AppUser adminUser = TestDataBuilder.appUser(1L, "admin@test.com", "ADMIN");
            AppUser targetRef = TestDataBuilder.appUser(999L, "missing@test.com", "MEMBER");
            when(appUserRepository.findById(999L)).thenReturn(Optional.empty());

            EventBookingRegistration reg = registration("event-50", "2026-09-01", "08:00", 1);
            reg.setUser(targetRef);

            EventBookingRegistration result = service.createRegistration(reg, adminUser, null);

            assertThat(result.getUser()).isEqualTo(adminUser);
        }

        @Test
        @DisplayName("non-admin cannot register on behalf of another user")
        void nonAdmin_cannotRegisterOnBehalf() {
            AppUser regularUser = TestDataBuilder.memberUser();
            AppUser otherUser = TestDataBuilder.appUser(99L, "other@test.com", "MEMBER");

            EventBookingRegistration reg = registration("event-50", "2026-09-01", "08:00", 1);
            reg.setUser(otherUser);

            EventBookingRegistration result = service.createRegistration(reg, regularUser, null);

            assertThat(result.getUser()).isEqualTo(regularUser);
            verify(appUserRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("SUPER_ADMIN role bypasses validation")
        void superAdminBypassesValidation() {
            PoojaSeva seva = new PoojaSeva();
            seva.setId(7L);
            seva.setName("Full Pooja");
            seva.setMultiDay(false);
            seva.setSlots(0);
            when(poojaSevaRepository.findById(7L)).thenReturn(Optional.of(seva));

            AppUser superAdmin = TestDataBuilder.appUser(3L, "super@test.com", "SUPER_ADMIN");

            EventBookingRegistration result = service.createRegistration(
                    registration("pooja-7", "2026-09-01", "08:00", 1), superAdmin, null);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("EVENT_ADMIN role bypasses validation")
        void eventAdminBypassesValidation() {
            CommunityEvent event = TestDataBuilder.communityEvent(50L, null, TestDataBuilder.adminUser());
            event.setCapacity(0);
            when(communityEventRepository.findById(50L)).thenReturn(Optional.of(event));

            AppUser eventAdmin = TestDataBuilder.appUser(4L, "evtadmin@test.com", "EVENT_ADMIN");

            EventBookingRegistration result = service.createRegistration(
                    registration("event-50", "2026-09-01", "08:00", 1), eventAdmin, null);

            assertThat(result).isNotNull();
        }
    }

    // ── Registration Basics ──────────────────────────────────────────

    @Nested
    @DisplayName("Registration Basics")
    class RegistrationBasics {

        @Test
        @DisplayName("generates regCode when not provided")
        void generatesRegCode() {
            EventBookingRegistration reg = registration("pooja-7", "2026-09-01", "08:00", 1);
            reg.setRegCode(null);

            EventBookingRegistration result = service.createRegistration(reg, null, null);

            assertThat(result.getRegCode()).startsWith("MNA-2026-");
        }

        @Test
        @DisplayName("generates QR code URL from regCode")
        void generatesQrCode() {
            EventBookingRegistration reg = registration("pooja-7", "2026-09-01", "08:00", 1);
            reg.setRegCode(null);
            reg.setQrCodeUrl(null);

            EventBookingRegistration result = service.createRegistration(reg, null, null);

            assertThat(result.getQrCodeUrl()).contains("qrserver.com")
                    .contains(result.getRegCode());
        }

        @Test
        @DisplayName("defaults status to CONFIRMED")
        void defaultsStatusToConfirmed() {
            EventBookingRegistration reg = registration("pooja-7", "2026-09-01", "08:00", 1);
            reg.setStatus(null);

            EventBookingRegistration result = service.createRegistration(reg, null, null);

            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        }

        @Test
        @DisplayName("defaults paymentStatus to FREE when bookingFee is zero")
        void defaultsPaymentFree() {
            EventBookingRegistration reg = registration("pooja-7", "2026-09-01", "08:00", 1);
            reg.setPaymentStatus(null);
            reg.setBookingFee(0.0);

            EventBookingRegistration result = service.createRegistration(reg, null, null);

            assertThat(result.getPaymentStatus()).isEqualTo("FREE");
        }

        @Test
        @DisplayName("sets paymentStatus to PAID when bookingFee is positive")
        void setsPaymentPaid() {
            EventBookingRegistration reg = registration("pooja-7", "2026-09-01", "08:00", 1);
            reg.setPaymentStatus(null);
            reg.setBookingFee(100.0);

            EventBookingRegistration result = service.createRegistration(reg, null, null);

            assertThat(result.getPaymentStatus()).isEqualTo("PAID");
        }

        @Test
        @DisplayName("sets community from user")
        void setsCommunityFromUser() {
            Community community = TestDataBuilder.community(10L, "TEST");
            AppUser user = TestDataBuilder.memberUser();
            user.setCommunity(community);

            EventBookingRegistration result = service.createRegistration(
                    registration("pooja-7", "2026-09-01", "08:00", 1), user, null);

            assertThat(result.getCommunity()).isEqualTo(community);
        }

        @Test
        @DisplayName("falls back to communityId when user has no community")
        void fallsToCommunityId() {
            Community community = TestDataBuilder.community(10L, "TEST");
            AppUser user = TestDataBuilder.appUser(5L, "test@test.com", "MEMBER");
            user.setCommunity(null);
            when(communityRepository.findById(10L)).thenReturn(Optional.of(community));

            EventBookingRegistration result = service.createRegistration(
                    registration("pooja-7", "2026-09-01", "08:00", 1), user, 10L);

            assertThat(result.getCommunity()).isEqualTo(community);
        }
    }

    // ── Cancel & Update ──────────────────────────────────────────────

    @Nested
    @DisplayName("Cancel and Update Registration")
    class CancelUpdate {

        @Test
        @DisplayName("cancelRegistration sets status to CANCELLED")
        void cancelRegistration() {
            EventBookingRegistration reg = registration("pooja-7", "2026-09-01", "08:00", 1);
            reg.setId(1L);
            reg.setStatus("CONFIRMED");
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));

            service.cancelRegistration(1L, null);

            assertThat(reg.getStatus()).isEqualTo("CANCELLED");
            verify(registrationRepository).save(reg);
        }

        @Test
        @DisplayName("updateRegistration patches mutable fields")
        void updateRegistration() {
            EventBookingRegistration existing = registration("pooja-7", "2026-09-01", "08:00", 1);
            existing.setId(1L);
            existing.setStatus("CONFIRMED");
            existing.setParticipantName("Original");
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(registrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            EventBookingRegistration patch = new EventBookingRegistration();
            patch.setParticipantName("Updated Name");

            EventBookingRegistration result = service.updateRegistration(1L, patch, null);

            assertThat(result.getParticipantName()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("updateRegistration rejects cancelled registration")
        void updateCancelledRejected() {
            EventBookingRegistration existing = registration("pooja-7", "2026-09-01", "08:00", 1);
            existing.setId(1L);
            existing.setStatus("CANCELLED");
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(existing));

            EventBookingRegistration patch = new EventBookingRegistration();
            patch.setParticipantName("Updated Name");

            assertThatThrownBy(() -> service.updateRegistration(1L, patch, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot update a cancelled registration");
        }
    }

    // ── My Registrations ─────────────────────────────────────────────

    @Nested
    @DisplayName("My Registrations")
    class MyRegistrations {

        @Test
        @DisplayName("getMyRegistrations returns user's registrations")
        void getMyRegistrations() {
            AppUser user = TestDataBuilder.memberUser();
            EventBookingRegistration reg = registration("pooja-7", "2026-09-01", "08:00", 1);
            when(registrationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                    .thenReturn(List.of(reg));

            List<EventBookingRegistration> result = service.getMyRegistrations(user, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("getMyRegistrations returns empty for null user")
        void getMyRegistrations_nullUser() {
            List<EventBookingRegistration> result = service.getMyRegistrations(null, null);

            assertThat(result).isEmpty();
        }
    }

    private EventBookingRegistration registration(String activityId, String eventDate, String eventTime, int devoteeCount) {
        EventBookingRegistration registration = new EventBookingRegistration();
        registration.setActivityId(activityId);
        registration.setActivityTitle("Test Activity");
        registration.setCategory("POOJA");
        registration.setParticipantName("Devotee");
        registration.setEventDate(eventDate);
        registration.setEventTime(eventTime);
        registration.setDevoteeCount(devoteeCount);
        registration.setBookingFee(0.0);
        return registration;
    }
}
