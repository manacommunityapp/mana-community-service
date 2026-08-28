package com.manacommunity.api.unit.service;

import com.manacommunity.api.events.entity.EventPoojaSchedule;
import com.manacommunity.api.events.entity.EventPoojaUserRegistration;
import com.manacommunity.api.events.enums.PoojaRegistrationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.EventPoojaBookingParticipantRepository;
import com.manacommunity.api.events.repository.EventPoojaScheduleRepository;
import com.manacommunity.api.events.repository.EventPoojaSlotReservationRepository;
import com.manacommunity.api.events.repository.EventPoojaUserRegistrationRepository;
import com.manacommunity.api.events.repository.EventRegistrationRepository;
import com.manacommunity.api.events.service.PoojaSlotReservationService;
import com.manacommunity.api.events.service.impl.EventPoojaUserRegistrationServiceImpl;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Pooja Registration Status Model")
class PoojaRegistrationStatusTest {

    // ── State machine unit tests ──────────────────────────────────────────────

    @Nested
    @DisplayName("PoojaRegistrationStatus transitions")
    class TransitionTests {

        @Test
        @DisplayName("CONFIRMED can move to CHECKED_IN, CANCELLED, NO_SHOW")
        void confirmedAllowedTransitions() {
            assertThat(PoojaRegistrationStatus.CONFIRMED.canTransitionTo(PoojaRegistrationStatus.CHECKED_IN)).isTrue();
            assertThat(PoojaRegistrationStatus.CONFIRMED.canTransitionTo(PoojaRegistrationStatus.CANCELLED)).isTrue();
            assertThat(PoojaRegistrationStatus.CONFIRMED.canTransitionTo(PoojaRegistrationStatus.NO_SHOW)).isTrue();
        }

        @Test
        @DisplayName("CONFIRMED cannot jump to COMPLETED or IN_PROGRESS")
        void confirmedCannotSkipSteps() {
            assertThat(PoojaRegistrationStatus.CONFIRMED.canTransitionTo(PoojaRegistrationStatus.COMPLETED)).isFalse();
            assertThat(PoojaRegistrationStatus.CONFIRMED.canTransitionTo(PoojaRegistrationStatus.IN_PROGRESS)).isFalse();
        }

        @Test
        @DisplayName("CHECKED_IN cannot transition back to CONFIRMED or RESERVED")
        void checkedInCannotGoBackward() {
            assertThat(PoojaRegistrationStatus.CHECKED_IN.canTransitionTo(PoojaRegistrationStatus.CONFIRMED)).isFalse();
            assertThat(PoojaRegistrationStatus.CHECKED_IN.canTransitionTo(PoojaRegistrationStatus.RESERVED)).isFalse();
        }

        @Test
        @DisplayName("COMPLETED, CANCELLED, EXPIRED, NO_SHOW are terminal — no allowed transitions")
        void terminalStatesHaveNoTransitions() {
            for (PoojaRegistrationStatus terminal : new PoojaRegistrationStatus[]{
                    PoojaRegistrationStatus.COMPLETED,
                    PoojaRegistrationStatus.CANCELLED,
                    PoojaRegistrationStatus.EXPIRED,
                    PoojaRegistrationStatus.NO_SHOW}) {
                assertThat(terminal.isTerminal()).as(terminal + " is terminal").isTrue();
                for (PoojaRegistrationStatus any : PoojaRegistrationStatus.values()) {
                    assertThat(terminal.canTransitionTo(any))
                            .as(terminal + " → " + any + " must be forbidden")
                            .isFalse();
                }
            }
        }

        @Test
        @DisplayName("only CONFIRMED is reschedulable")
        void onlyConfirmedIsReschedulable() {
            assertThat(PoojaRegistrationStatus.CONFIRMED.isReschedulable()).isTrue();
            assertThat(PoojaRegistrationStatus.CHECKED_IN.isReschedulable()).isFalse();
            assertThat(PoojaRegistrationStatus.IN_PROGRESS.isReschedulable()).isFalse();
            assertThat(PoojaRegistrationStatus.COMPLETED.isReschedulable()).isFalse();
            assertThat(PoojaRegistrationStatus.RESERVED.isReschedulable()).isFalse();
            assertThat(PoojaRegistrationStatus.CANCELLED.isReschedulable()).isFalse();
        }

        @Test
        @DisplayName("parse returns fallback for null, blank, or unknown values")
        void parseHandlesInvalidInput() {
            assertThat(PoojaRegistrationStatus.parse(null, PoojaRegistrationStatus.CONFIRMED))
                    .isEqualTo(PoojaRegistrationStatus.CONFIRMED);
            assertThat(PoojaRegistrationStatus.parse("", PoojaRegistrationStatus.CANCELLED))
                    .isEqualTo(PoojaRegistrationStatus.CANCELLED);
            assertThat(PoojaRegistrationStatus.parse("LEGACY_STATUS", PoojaRegistrationStatus.CONFIRMED))
                    .isEqualTo(PoojaRegistrationStatus.CONFIRMED);
        }

        @Test
        @DisplayName("parse is case-insensitive")
        void parseCaseInsensitive() {
            assertThat(PoojaRegistrationStatus.parse("checked_in", PoojaRegistrationStatus.CONFIRMED))
                    .isEqualTo(PoojaRegistrationStatus.CHECKED_IN);
            assertThat(PoojaRegistrationStatus.parse("Cancelled", PoojaRegistrationStatus.CONFIRMED))
                    .isEqualTo(PoojaRegistrationStatus.CANCELLED);
        }
    }

    // ── Service enforcement tests ─────────────────────────────────────────────

    @Nested
    @DisplayName("Reschedule guard")
    class RescheduleGuardTests {

        @Test
        @DisplayName("reschedule throws when registration is CHECKED_IN")
        void rescheduleBlockedForCheckedIn() {
            EventPoojaUserRegistrationRepository repo = mock(EventPoojaUserRegistrationRepository.class);
            EventPoojaUserRegistration reg = registration("CHECKED_IN");
            when(repo.findById(1L)).thenReturn(Optional.of(reg));

            EventPoojaUserRegistrationServiceImpl service = service(repo);

            assertThatThrownBy(() -> service.reschedule(1L, 99L, "key", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CHECKED_IN");
        }

        @Test
        @DisplayName("reschedule throws when registration is COMPLETED")
        void rescheduleBlockedForCompleted() {
            EventPoojaUserRegistrationRepository repo = mock(EventPoojaUserRegistrationRepository.class);
            EventPoojaUserRegistration reg = registration("COMPLETED");
            when(repo.findById(1L)).thenReturn(Optional.of(reg));

            EventPoojaUserRegistrationServiceImpl service = service(repo);

            assertThatThrownBy(() -> service.reschedule(1L, 99L, "key", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("COMPLETED");
        }

        @Test
        @DisplayName("reschedule throws when registration is CANCELLED")
        void rescheduleBlockedForCancelled() {
            EventPoojaUserRegistrationRepository repo = mock(EventPoojaUserRegistrationRepository.class);
            EventPoojaUserRegistration reg = registration("CANCELLED");
            when(repo.findById(1L)).thenReturn(Optional.of(reg));

            EventPoojaUserRegistrationServiceImpl service = service(repo);

            assertThatThrownBy(() -> service.reschedule(1L, 99L, "key", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CANCELLED");
        }

        @Test
        @DisplayName("reschedule proceeds when registration is CONFIRMED")
        void rescheduleAllowedForConfirmed() {
            EventPoojaUserRegistrationRepository repo = mock(EventPoojaUserRegistrationRepository.class);
            EventPoojaScheduleRepository schedRepo = mock(EventPoojaScheduleRepository.class);
            PoojaSlotReservationService reservationService = mock(PoojaSlotReservationService.class);

            EventPoojaUserRegistration reg = registration("CONFIRMED");
            when(repo.findById(1L)).thenReturn(Optional.of(reg));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            EventPoojaSchedule newSchedule = new EventPoojaSchedule();
            newSchedule.setId(99L);
            newSchedule.setScheduleDate(java.time.LocalDate.now().plusDays(7));
            newSchedule.setStartTime(java.time.LocalTime.of(8, 30));
            when(schedRepo.findById(99L)).thenReturn(Optional.of(newSchedule));

            com.manacommunity.api.events.dto.PoojaReserveResponse reserveResp =
                    com.manacommunity.api.events.dto.PoojaReserveResponse.builder()
                            .reservationId(200L).tokenNumber(5).scheduleId(99L)
                            .expiresAt(java.time.LocalDateTime.now().plusMinutes(5))
                            .status("RESERVED").build();
            when(reservationService.reserve(eq(99L), any(), any())).thenReturn(reserveResp);

            EventPoojaUserRegistrationServiceImpl service = new EventPoojaUserRegistrationServiceImpl(
                    repo, mock(CommunityRepository.class), reservationService,
                    schedRepo, mock(EventRegistrationRepository.class),
                    mock(EventBookingRegistrationRepository.class),
                    mock(EventPoojaSlotReservationRepository.class),
                    mock(EventPoojaBookingParticipantRepository.class),
                    new ObjectMapper());

            EventPoojaUserRegistration result = service.reschedule(1L, 99L, "key", null);
            assertThat(result.getScheduleId()).isEqualTo(99L);
        }
    }

    @Nested
    @DisplayName("Cancel guard")
    class CancelGuardTests {

        @Test
        @DisplayName("cancel throws for a COMPLETED registration")
        void cancelBlockedForCompleted() {
            EventPoojaUserRegistrationRepository repo = mock(EventPoojaUserRegistrationRepository.class);
            EventPoojaUserRegistration reg = registration("COMPLETED");
            when(repo.findById(1L)).thenReturn(Optional.of(reg));

            EventPoojaUserRegistrationServiceImpl service = service(repo);

            assertThatThrownBy(() -> service.cancelRegistration(1L, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel");
        }

        @Test
        @DisplayName("cancel throws for an already CANCELLED registration")
        void cancelBlockedForAlreadyCancelled() {
            EventPoojaUserRegistrationRepository repo = mock(EventPoojaUserRegistrationRepository.class);
            EventPoojaUserRegistration reg = registration("CANCELLED");
            when(repo.findById(1L)).thenReturn(Optional.of(reg));

            EventPoojaUserRegistrationServiceImpl service = service(repo);

            assertThatThrownBy(() -> service.cancelRegistration(1L, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel");
        }

        @Test
        @DisplayName("cancel succeeds for a CONFIRMED registration")
        void cancelAllowedForConfirmed() {
            EventPoojaUserRegistrationRepository repo = mock(EventPoojaUserRegistrationRepository.class);
            PoojaSlotReservationService reservationService = mock(PoojaSlotReservationService.class);
            EventPoojaUserRegistration reg = registration("CONFIRMED");
            when(repo.findById(1L)).thenReturn(Optional.of(reg));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            EventPoojaUserRegistrationServiceImpl service = new EventPoojaUserRegistrationServiceImpl(
                    repo, mock(CommunityRepository.class), reservationService,
                    mock(EventPoojaScheduleRepository.class),
                    mock(EventRegistrationRepository.class),
                    mock(EventBookingRegistrationRepository.class),
                    mock(EventPoojaSlotReservationRepository.class),
                    mock(EventPoojaBookingParticipantRepository.class),
                    new ObjectMapper());

            service.cancelRegistration(1L, null);

            assertThat(reg.getStatus()).isEqualTo("CANCELLED");
        }
    }

    @Nested
    @DisplayName("UpdateRegistration status guard")
    class UpdateStatusGuardTests {

        @Test
        @DisplayName("non-admin cannot set an invalid transition (COMPLETED → CONFIRMED)")
        void nonAdminBlockedInvalidTransition() {
            EventPoojaUserRegistrationRepository repo = mock(EventPoojaUserRegistrationRepository.class);
            EventPoojaUserRegistration reg = registration("COMPLETED");
            reg.setId(1L);
            when(repo.findById(1L)).thenReturn(Optional.of(reg));

            AppUser member = AppUser.builder().id(5L).role("MEMBER").build();
            EventPoojaUserRegistration patch = new EventPoojaUserRegistration();
            patch.setStatus("CONFIRMED");

            EventPoojaUserRegistrationServiceImpl service = service(repo);

            assertThatThrownBy(() -> service.updateRegistration(1L, patch, member))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("COMPLETED");
        }

        @Test
        @DisplayName("admin can force any status transition (data correction)")
        void adminCanForceAnyTransition() {
            EventPoojaUserRegistrationRepository repo = mock(EventPoojaUserRegistrationRepository.class);
            EventPoojaUserRegistration reg = registration("COMPLETED");
            reg.setId(1L);
            when(repo.findById(1L)).thenReturn(Optional.of(reg));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AppUser admin = AppUser.builder().id(1L).role("ROLE_ADMIN").build();
            EventPoojaUserRegistration patch = new EventPoojaUserRegistration();
            patch.setStatus("CONFIRMED");

            EventPoojaUserRegistrationServiceImpl service = service(repo);
            EventPoojaUserRegistration result = service.updateRegistration(1L, patch, admin);

            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        }
    }

    @Nested
    @DisplayName("Main Event Registration Check")
    class ParentEventRegistrationCheckTests {

        @Test
        @DisplayName("createRegistration succeeds when user is registered in event_booking_registrations")
        void succeedsWhenRegisteredInBookingRegistrations() {
            EventPoojaUserRegistrationRepository repo = mock(EventPoojaUserRegistrationRepository.class);
            EventRegistrationRepository eventRegRepo = mock(EventRegistrationRepository.class);
            EventBookingRegistrationRepository bookingRegRepo = mock(EventBookingRegistrationRepository.class);
            PoojaSlotReservationService reservationService = mock(PoojaSlotReservationService.class);

            AppUser user = AppUser.builder().id(5L).role("MEMBER").build();
            EventPoojaUserRegistration reg = new EventPoojaUserRegistration();
            reg.setEventId(100L);
            reg.setPoojaSlotName("Morning Pooja");
            reg.setParticipantName("Devotee 1");

            when(eventRegRepo.existsByEventIdAndUserId(100L, 5L)).thenReturn(false);
            when(bookingRegRepo.existsByUserIdAndActivityIdAndStatusNot(5L, "event-100", "CANCELLED")).thenReturn(true);
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            EventPoojaUserRegistrationServiceImpl service = new EventPoojaUserRegistrationServiceImpl(
                    repo, mock(CommunityRepository.class), reservationService,
                    mock(EventPoojaScheduleRepository.class),
                    eventRegRepo,
                    bookingRegRepo,
                    mock(EventPoojaSlotReservationRepository.class),
                    mock(EventPoojaBookingParticipantRepository.class),
                    new ObjectMapper());

            EventPoojaUserRegistration created = service.createRegistration(reg, user, 1L, false);
            assertThat(created).isNotNull();
        }

        @Test
        @DisplayName("createRegistration throws when user is not registered in either repository")
        void throwsWhenNotRegisteredInEither() {
            EventPoojaUserRegistrationRepository repo = mock(EventPoojaUserRegistrationRepository.class);
            EventRegistrationRepository eventRegRepo = mock(EventRegistrationRepository.class);
            EventBookingRegistrationRepository bookingRegRepo = mock(EventBookingRegistrationRepository.class);
            PoojaSlotReservationService reservationService = mock(PoojaSlotReservationService.class);

            AppUser user = AppUser.builder().id(5L).role("MEMBER").build();
            EventPoojaUserRegistration reg = new EventPoojaUserRegistration();
            reg.setEventId(100L);
            reg.setPoojaSlotName("Morning Pooja");
            reg.setParticipantName("Devotee 1");

            when(eventRegRepo.existsByEventIdAndUserId(100L, 5L)).thenReturn(false);
            when(bookingRegRepo.existsByUserIdAndActivityIdAndStatusNot(5L, "event-100", "CANCELLED")).thenReturn(false);
            when(bookingRegRepo.existsByUserIdAndActivityIdAndStatusNot(5L, "100", "CANCELLED")).thenReturn(false);

            EventPoojaUserRegistrationServiceImpl service = new EventPoojaUserRegistrationServiceImpl(
                    repo, mock(CommunityRepository.class), reservationService,
                    mock(EventPoojaScheduleRepository.class),
                    eventRegRepo,
                    bookingRegRepo,
                    mock(EventPoojaSlotReservationRepository.class),
                    mock(EventPoojaBookingParticipantRepository.class),
                    new ObjectMapper());

            assertThatThrownBy(() -> service.createRegistration(reg, user, 1L, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Registration for the main event is required");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private EventPoojaUserRegistration registration(String status) {
        EventPoojaUserRegistration reg = new EventPoojaUserRegistration();
        reg.setId(1L);
        reg.setStatus(status);
        reg.setRegCode("MNA-TEST-001");
        reg.setParticipantName("Test Devotee");
        return reg;
    }

    private EventPoojaUserRegistrationServiceImpl service(EventPoojaUserRegistrationRepository repo) {
        return new EventPoojaUserRegistrationServiceImpl(
                repo,
                mock(CommunityRepository.class),
                mock(PoojaSlotReservationService.class),
                mock(EventPoojaScheduleRepository.class),
                mock(EventRegistrationRepository.class),
                mock(EventBookingRegistrationRepository.class),
                mock(EventPoojaSlotReservationRepository.class),
                mock(EventPoojaBookingParticipantRepository.class),
                new ObjectMapper());
    }
}
