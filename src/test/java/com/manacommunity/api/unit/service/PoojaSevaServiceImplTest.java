package com.manacommunity.api.unit.service;

import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.entity.PoojaSeva;
import com.manacommunity.api.events.entity.PoojaSevaDaySlot;
import com.manacommunity.api.events.entity.PoojaSevaDayTimeSlot;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.events.service.impl.PoojaSevaServiceImpl;
import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.support.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PoojaSevaServiceImpl")
class PoojaSevaServiceImplTest {

    private PoojaSevaRepository repository;
    private CommunityEventRepository eventRepository;
    private EventBookingRegistrationRepository bookingRepo;
    private PoojaSevaServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(PoojaSevaRepository.class);
        eventRepository = mock(CommunityEventRepository.class);
        bookingRepo = mock(EventBookingRegistrationRepository.class);
        service = new PoojaSevaServiceImpl(repository, eventRepository, bookingRepo);
    }

    // ── Create ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Create Pooja/Seva")
    class Create {

        @Test
        @DisplayName("creates single-day pooja with top-level slots")
        void createSingleDay_usesTopLevelSlots() {
            when(repository.save(any(PoojaSeva.class))).thenAnswer(inv -> inv.getArgument(0));

            PoojaSeva seva = new PoojaSeva();
            seva.setName("Ganesh Puja");
            seva.setType("Ganesh Puja");
            seva.setDate(LocalDate.of(2026, 9, 1));
            seva.setMultiDay(false);
            seva.setSlots(25);

            PoojaSeva saved = service.createPoojaSeva(10L, seva);

            assertThat(saved.getCommunityId()).isEqualTo(10L);
            assertThat(saved.getSlots()).isEqualTo(25);
        }

        @Test
        @DisplayName("creates multi-day pooja preserving provided fields")
        void createMultiDay_preservesFields() {
            when(repository.save(any(PoojaSeva.class))).thenAnswer(inv -> inv.getArgument(0));

            PoojaSeva seva = new PoojaSeva();
            seva.setName("Festival Archana");
            seva.setType("Archana");
            seva.setDate(LocalDate.of(2026, 9, 1));
            seva.setEndDate(LocalDate.of(2026, 9, 2));
            seva.setMultiDay(true);
            seva.setStartTimes(List.of("08:00", "10:00"));
            seva.setSlots(12);

            PoojaSeva saved = service.createPoojaSeva(10L, seva);

            assertThat(saved.getCommunityId()).isEqualTo(10L);
            assertThat(saved.getMultiDay()).isTrue();
            assertThat(saved.getDate()).isEqualTo(LocalDate.of(2026, 9, 1));
            assertThat(saved.getEndDate()).isEqualTo(LocalDate.of(2026, 9, 2));
            assertThat(saved.getStartTimes()).containsExactly("08:00", "10:00");
            assertThat(saved.getSlots()).isEqualTo(12);
        }

        @Test
        @DisplayName("create rejects main event from different community")
        void create_rejectsMismatchedCommunity() {
            Community otherCommunity = TestDataBuilder.community(99L, "OTHER");
            CommunityEvent parentEvent = TestDataBuilder.communityEvent(100L, otherCommunity, TestDataBuilder.adminUser());
            when(eventRepository.findById(100L)).thenReturn(Optional.of(parentEvent));

            PoojaSeva seva = new PoojaSeva();
            seva.setName("Ganesh Puja");
            seva.setType("Ganesh Puja");
            seva.setMainEventId(100L);

            assertThatThrownBy(() -> service.createPoojaSeva(10L, seva))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("different community");
            verify(repository, never()).save(any(PoojaSeva.class));
        }

        @Test
        @DisplayName("create rejects missing parent event")
        void create_rejectsMissingParentEvent() {
            PoojaSeva seva = new PoojaSeva();
            seva.setName("Ganesh Puja");
            seva.setMainEventId(999L);
            when(eventRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createPoojaSeva(10L, seva))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── Date Validation ──────────────────────────────────────────────

    @Nested
    @DisplayName("Date Validation within Parent Event")
    class DateValidation {

        private CommunityEvent parentEvent;

        @BeforeEach
        void setUpParent() {
            Community community = TestDataBuilder.community(10L, "TEST");
            parentEvent = TestDataBuilder.communityEvent(100L, community, TestDataBuilder.adminUser());
            parentEvent.setStartDate(LocalDate.of(2026, 9, 1));
            parentEvent.setEndDate(LocalDate.of(2026, 9, 5));
            when(eventRepository.findById(100L)).thenReturn(Optional.of(parentEvent));
            lenient().when(repository.save(any(PoojaSeva.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("create allows pooja date within parent range")
        void create_dateWithinRange() {
            PoojaSeva seva = pooja(LocalDate.of(2026, 9, 3), null);

            PoojaSeva saved = service.createPoojaSeva(10L, seva);

            assertThat(saved).isNotNull();
            verify(repository).save(any(PoojaSeva.class));
        }

        @Test
        @DisplayName("create allows pooja on same date as parent start")
        void create_dateOnParentStart() {
            PoojaSeva seva = pooja(LocalDate.of(2026, 9, 1), null);

            PoojaSeva saved = service.createPoojaSeva(10L, seva);

            assertThat(saved).isNotNull();
        }

        @Test
        @DisplayName("create allows pooja on same date as parent end")
        void create_dateOnParentEnd() {
            PoojaSeva seva = pooja(LocalDate.of(2026, 9, 5), null);

            PoojaSeva saved = service.createPoojaSeva(10L, seva);

            assertThat(saved).isNotNull();
        }

        @Test
        @DisplayName("create rejects pooja date before parent start")
        void create_dateBeforeParentStart() {
            PoojaSeva seva = pooja(LocalDate.of(2026, 8, 31), null);

            assertThatThrownBy(() -> service.createPoojaSeva(10L, seva))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("within the parent event range");
        }

        @Test
        @DisplayName("create rejects pooja date after parent end")
        void create_dateAfterParentEnd() {
            PoojaSeva seva = pooja(LocalDate.of(2026, 9, 6), null);

            assertThatThrownBy(() -> service.createPoojaSeva(10L, seva))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("within the parent event range");
        }

        @Test
        @DisplayName("create rejects pooja end date outside parent range")
        void create_endDateOutsideParent() {
            PoojaSeva seva = pooja(LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 10));

            assertThatThrownBy(() -> service.createPoojaSeva(10L, seva))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("end date must be within the parent event range");
        }

        @Test
        @DisplayName("create rejects daySlot date outside parent range")
        void create_daySlotOutsideParent() {
            PoojaSeva seva = pooja(LocalDate.of(2026, 9, 2), null);
            PoojaSevaDaySlot slot = new PoojaSevaDaySlot();
            slot.setSlotDate(LocalDate.of(2026, 9, 10));
            seva.setDaySlots(new ArrayList<>(List.of(slot)));

            assertThatThrownBy(() -> service.createPoojaSeva(10L, seva))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("slot date");
        }

        @Test
        @DisplayName("create rejects timeSlotConfig date outside parent range")
        void create_timeSlotConfigOutsideParent() {
            PoojaSeva seva = pooja(LocalDate.of(2026, 9, 2), null);
            seva.setTimeSlotConfig(new ArrayList<>(List.of(
                    new PoojaSevaDayTimeSlot(LocalDate.of(2026, 9, 20), "08:00", 10)
            )));

            assertThatThrownBy(() -> service.createPoojaSeva(10L, seva))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("time slot date");
        }

        @Test
        @DisplayName("parent with no endDate uses startDate as range end")
        void create_parentWithoutEndDate() {
            parentEvent.setEndDate(null);
            PoojaSeva seva = pooja(LocalDate.of(2026, 9, 1), null);

            PoojaSeva saved = service.createPoojaSeva(10L, seva);

            assertThat(saved).isNotNull();
        }

        @Test
        @DisplayName("parent with no endDate rejects pooja after startDate")
        void create_parentWithoutEndDate_rejectsLater() {
            parentEvent.setEndDate(null);
            PoojaSeva seva = pooja(LocalDate.of(2026, 9, 2), null);

            assertThatThrownBy(() -> service.createPoojaSeva(10L, seva))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("within the parent event range");
        }

        private PoojaSeva pooja(LocalDate date, LocalDate endDate) {
            PoojaSeva seva = new PoojaSeva();
            seva.setName("Test Pooja");
            seva.setType("POOJA");
            seva.setMainEventId(100L);
            seva.setDate(date);
            seva.setEndDate(endDate);
            return seva;
        }
    }

    // ── Query ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Query Pooja/Seva")
    class Query {

        @Test
        @DisplayName("getById scoped to caller's community")
        void getById_usesCommunityScopedLookup() {
            PoojaSeva seva = new PoojaSeva();
            seva.setId(5L);
            seva.setCommunityId(10L);
            when(repository.findByIdAndCommunityId(5L, 10L)).thenReturn(Optional.of(seva));

            PoojaSeva result = service.getPoojaSevaById(5L, 10L);

            assertThat(result).isSameAs(seva);
            verify(repository).findByIdAndCommunityId(5L, 10L);
        }

        @Test
        @DisplayName("getById without community uses global lookup")
        void getById_globalLookup() {
            PoojaSeva seva = new PoojaSeva();
            seva.setId(5L);
            when(repository.findById(5L)).thenReturn(Optional.of(seva));

            PoojaSeva result = service.getPoojaSevaById(5L, null);

            assertThat(result).isSameAs(seva);
            verify(repository).findById(5L);
        }

        @Test
        @DisplayName("getById throws when not found")
        void getById_notFound() {
            when(repository.findByIdAndCommunityId(5L, 10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPoojaSevaById(5L, 10L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("getAllPoojaSevas by community and main event")
        void getAllByMainEvent() {
            PoojaSeva seva = new PoojaSeva();
            when(repository.findByCommunityIdAndMainEventIdOrderByDateAscStartTimeAsc(10L, 100L))
                    .thenReturn(List.of(seva));

            List<PoojaSeva> result = service.getAllPoojaSevas(10L, 100L);

            assertThat(result).containsExactly(seva);
        }

        @Test
        @DisplayName("getAllPoojaSevas by main event only")
        void getAllByMainEventOnly() {
            PoojaSeva seva = new PoojaSeva();
            when(repository.findByMainEventIdOrderByDateAscStartTimeAsc(100L))
                    .thenReturn(List.of(seva));

            List<PoojaSeva> result = service.getAllPoojaSevas(null, 100L);

            assertThat(result).containsExactly(seva);
        }
    }

    // ── Update ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Update Pooja/Seva")
    class Update {

        @Test
        @DisplayName("update rejects slots below active bookings")
        void update_rejectsSlotsBelow() {
            PoojaSeva existing = new PoojaSeva();
            existing.setId(5L);
            existing.setCommunityId(10L);
            existing.setSlots(20);
            existing.setItems(new ArrayList<>());
            existing.setStartTimes(new ArrayList<>());
            existing.setDaySlots(new ArrayList<>());
            existing.setTimeSlotConfig(new ArrayList<>());
            when(repository.findByIdAndCommunityId(5L, 10L)).thenReturn(Optional.of(existing));
            when(bookingRepo.countByActivityIdAndStatusNot("pooja-5", "CANCELLED")).thenReturn(8L);

            PoojaSeva updated = new PoojaSeva();
            updated.setName("Updated Pooja");
            updated.setSlots(5);

            assertThatThrownBy(() -> service.updatePoojaSeva(5L, 10L, updated))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("Slots cannot be less than active bookings (8)");
        }

        @Test
        @DisplayName("update accepts slots equal to active bookings")
        void update_acceptsSlotsEqual() {
            PoojaSeva existing = new PoojaSeva();
            existing.setId(5L);
            existing.setCommunityId(10L);
            existing.setSlots(20);
            existing.setItems(new ArrayList<>());
            existing.setStartTimes(new ArrayList<>());
            existing.setDaySlots(new ArrayList<>());
            existing.setTimeSlotConfig(new ArrayList<>());
            when(repository.findByIdAndCommunityId(5L, 10L)).thenReturn(Optional.of(existing));
            when(bookingRepo.countByActivityIdAndStatusNot("pooja-5", "CANCELLED")).thenReturn(5L);
            when(repository.save(any(PoojaSeva.class))).thenAnswer(inv -> inv.getArgument(0));

            PoojaSeva updated = new PoojaSeva();
            updated.setName("Updated Pooja");
            updated.setSlots(5);

            PoojaSeva result = service.updatePoojaSeva(5L, 10L, updated);

            assertThat(result.getSlots()).isEqualTo(5);
        }

        @Test
        @DisplayName("update accepts increased slots")
        void update_acceptsIncreasedSlots() {
            PoojaSeva existing = new PoojaSeva();
            existing.setId(5L);
            existing.setCommunityId(10L);
            existing.setSlots(10);
            existing.setItems(new ArrayList<>());
            existing.setStartTimes(new ArrayList<>());
            existing.setDaySlots(new ArrayList<>());
            existing.setTimeSlotConfig(new ArrayList<>());
            when(repository.findByIdAndCommunityId(5L, 10L)).thenReturn(Optional.of(existing));
            when(bookingRepo.countByActivityIdAndStatusNot("pooja-5", "CANCELLED")).thenReturn(5L);
            when(repository.save(any(PoojaSeva.class))).thenAnswer(inv -> inv.getArgument(0));

            PoojaSeva updated = new PoojaSeva();
            updated.setName("Updated Pooja");
            updated.setSlots(50);

            PoojaSeva result = service.updatePoojaSeva(5L, 10L, updated);

            assertThat(result.getSlots()).isEqualTo(50);
        }

        @Test
        @DisplayName("update rejects main event from different community")
        void update_rejectsWrongCommunityMainEvent() {
            PoojaSeva existing = new PoojaSeva();
            existing.setId(5L);
            existing.setCommunityId(10L);
            when(repository.findByIdAndCommunityId(5L, 10L)).thenReturn(Optional.of(existing));

            Community otherCommunity = TestDataBuilder.community(99L, "OTHER");
            CommunityEvent parentEvent = TestDataBuilder.communityEvent(100L, otherCommunity, TestDataBuilder.adminUser());
            when(eventRepository.findById(100L)).thenReturn(Optional.of(parentEvent));

            PoojaSeva updated = new PoojaSeva();
            updated.setMainEventId(100L);

            assertThatThrownBy(() -> service.updatePoojaSeva(5L, 10L, updated))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("different community");
            verify(repository, never()).save(any(PoojaSeva.class));
        }

        @Test
        @DisplayName("update validates dates within parent event")
        void update_validatesDateWithinParent() {
            PoojaSeva existing = new PoojaSeva();
            existing.setId(5L);
            existing.setCommunityId(10L);
            existing.setMainEventId(100L);
            when(repository.findByIdAndCommunityId(5L, 10L)).thenReturn(Optional.of(existing));

            Community community = TestDataBuilder.community(10L, "TEST");
            CommunityEvent parent = TestDataBuilder.communityEvent(100L, community, TestDataBuilder.adminUser());
            parent.setStartDate(LocalDate.of(2026, 9, 1));
            parent.setEndDate(LocalDate.of(2026, 9, 5));
            when(eventRepository.findById(100L)).thenReturn(Optional.of(parent));

            PoojaSeva updated = new PoojaSeva();
            updated.setDate(LocalDate.of(2026, 10, 1));

            assertThatThrownBy(() -> service.updatePoojaSeva(5L, 10L, updated))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("within the parent event range");
        }
    }

    // ── Delete ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Delete Pooja/Seva")
    class Delete {

        @Test
        @DisplayName("delete rejects pooja with active bookings")
        void delete_blockedByActiveBookings() {
            PoojaSeva existing = new PoojaSeva();
            existing.setId(5L);
            existing.setCommunityId(10L);
            when(repository.findByIdAndCommunityId(5L, 10L)).thenReturn(Optional.of(existing));
            when(bookingRepo.existsByActivityIdAndStatusNot("pooja-5", "CANCELLED")).thenReturn(true);

            assertThatThrownBy(() -> service.deletePoojaSeva(5L, 10L))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("Cannot delete pooja/seva with active bookings");
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("delete succeeds when no active bookings")
        void delete_succeeds() {
            PoojaSeva existing = new PoojaSeva();
            existing.setId(5L);
            existing.setCommunityId(10L);
            when(repository.findByIdAndCommunityId(5L, 10L)).thenReturn(Optional.of(existing));
            when(bookingRepo.existsByActivityIdAndStatusNot("pooja-5", "CANCELLED")).thenReturn(false);

            service.deletePoojaSeva(5L, 10L);

            verify(repository).delete(existing);
        }

        @Test
        @DisplayName("delete allowed when only cancelled bookings exist")
        void delete_allowedWithCancelledBookings() {
            PoojaSeva existing = new PoojaSeva();
            existing.setId(5L);
            existing.setCommunityId(10L);
            when(repository.findByIdAndCommunityId(5L, 10L)).thenReturn(Optional.of(existing));
            when(bookingRepo.existsByActivityIdAndStatusNot("pooja-5", "CANCELLED")).thenReturn(false);

            service.deletePoojaSeva(5L, 10L);

            verify(repository).delete(existing);
        }

        @Test
        @DisplayName("delete throws when pooja not found")
        void delete_notFound() {
            when(repository.findByIdAndCommunityId(5L, 10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deletePoojaSeva(5L, 10L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
