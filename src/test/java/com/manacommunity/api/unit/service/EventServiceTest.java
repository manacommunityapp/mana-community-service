package com.manacommunity.api.unit.service;

import com.manacommunity.api.events.dto.*;
import com.manacommunity.api.events.entity.*;
import com.manacommunity.api.events.repository.*;
import com.manacommunity.api.events.service.EventService;
import com.manacommunity.api.exception.AlreadyRegisteredException;
import com.manacommunity.api.exception.EventFullException;
import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.media.entity.MediaModule;
import com.manacommunity.api.media.entity.MediaObject;
import com.manacommunity.api.media.entity.MediaStatus;
import com.manacommunity.api.media.entity.MediaType;
import com.manacommunity.api.media.repository.MediaRepository;
import com.manacommunity.api.media.service.MediaUrlService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Notification;
import com.manacommunity.api.repository.NotificationRepository;
import com.manacommunity.api.support.TestDataBuilder;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.repository.AuctionPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService")
class EventServiceTest {

    @Mock CommunityEventRepository eventRepo;
    @Mock EventRegistrationRepository regRepo;
    @Mock EventVolunteerRepository volunteerRepo;
    @Mock EventDonationRepository donationRepo;
    @Mock EventExpenseRepository expenseRepo;
    @Mock EventSponsorRepository sponsorRepo;
    @Mock EventTaskRepository taskRepo;
    @Mock MealRegistrationRepository mealRegRepo;
    @Mock EventAuctionItemRepository auctionItemRepo;
    @Mock AuctionPlayerRepository auctionPlayerRepo;
    @Mock ActivityRegistrationRepository activityRegRepo;
    @Mock EventProgramRepository programRepo;
    @Mock EventGalleryItemRepository galleryRepo;
    @Mock EventInvoiceRepository invoiceRepo;
    @Mock MediaRepository mediaRepo;
    @Mock MediaUrlService mediaUrlService;
    @Mock EventBookingRegistrationRepository bookingRegRepo;
    @Mock NotificationRepository notificationRepo;

    @InjectMocks EventService eventService;

    private Community community;
    private AppUser adminUser;
    private AppUser memberUser;
    private CommunityEvent event;

    @BeforeEach
    void setUp() {
        community = TestDataBuilder.community(1L, "INVITE123");
        adminUser = TestDataBuilder.adminUser();
        memberUser = TestDataBuilder.memberUser();
        event = TestDataBuilder.communityEvent(100L, community, adminUser);
    }

    // ── Query Events ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Query Events")
    class QueryEvents {

        @Test
        @DisplayName("getUpcomingEvents returns published events for community")
        void getUpcomingEvents_success() {
            when(eventRepo.findUpcomingByCommunity(1L)).thenReturn(List.of(event));

            List<EventResponse> result = eventService.getUpcomingEvents(1L, null, adminUser.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo(event.getTitle());
        }

        @Test
        @DisplayName("getUpcomingEvents with type filter filters correctly")
        void getUpcomingEvents_withTypeFilter() {
            when(eventRepo.findUpcomingByCommunityAndType(eq(1L), any(CommunityEvent.EventType.class)))
                    .thenReturn(List.of(event));

            List<EventResponse> result = eventService.getUpcomingEvents(1L, "SPORTS", adminUser.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo("SPORTS");
        }

        @Test
        @DisplayName("getUpcomingEvents falls back to unfiltered on invalid type")
        void getUpcomingEvents_invalidTypeFilter() {
            when(eventRepo.findUpcomingByCommunity(1L)).thenReturn(List.of(event));

            List<EventResponse> result = eventService.getUpcomingEvents(1L, "INVALID_TYPE", adminUser.getId());

            assertThat(result).hasSize(1);
            verify(eventRepo).findUpcomingByCommunity(1L);
        }

        @Test
        @DisplayName("getAllEvents returns all community events ordered by start date desc")
        void getAllEvents_success() {
            when(eventRepo.findByCommunityIdOrderByStartDateDesc(1L)).thenReturn(List.of(event));

            List<EventResponse> result = eventService.getAllEvents(1L, adminUser.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("getById returns event when found")
        void getById_found() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            EventResponse response = eventService.getById(100L, adminUser.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(100L);
            assertThat(response.getTitle()).isEqualTo("Annual Sports Meet");
        }

        @Test
        @DisplayName("getById throws ResourceNotFoundException when not found")
        void getById_notFound() {
            when(eventRepo.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.getById(999L, adminUser.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("getMyEvents returns events user is registered to")
        void getMyEvents_success() {
            EventRegistration reg = TestDataBuilder.eventRegistration(1L, event, memberUser);
            when(regRepo.findByUserIdOrderByRegisteredAtDesc(memberUser.getId())).thenReturn(List.of(reg));
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            List<EventResponse> result = eventService.getMyEvents(memberUser.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(100L);
        }
    }

    // ── Create Event ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Create Event")
    class CreateEvent {

        @Test
        @DisplayName("create event saves and returns EventResponse")
        void create_success() {
            EventRequest req = TestDataBuilder.eventRequest("Diwali Mela");
            when(eventRepo.save(any(CommunityEvent.class))).thenAnswer(inv -> {
                CommunityEvent e = inv.getArgument(0);
                e.setId(200L);
                return e;
            });

            EventResponse response = eventService.create(req, adminUser, community);

            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("Diwali Mela");
            verify(eventRepo).save(any(CommunityEvent.class));
        }

        @Test
        @DisplayName("create event links uploaded cover and scanner media")
        void create_withMediaReferences() {
            EventRequest req = TestDataBuilder.eventRequest("Diwali Mela");
            MediaObject cover = mediaObject(UUID.randomUUID(), MediaType.IMAGE);
            MediaObject scanner = mediaObject(UUID.randomUUID(), MediaType.QR_CODE);
            req.setImageMediaId(cover.getExternalId().toString());
            req.setScannerMediaId(scanner.getExternalId().toString());
            req.setImageUrl("https://temporary-upload-url.example/cover");
            req.setScannerUrl("https://temporary-upload-url.example/scanner");

            when(mediaRepo.findByExternalIdAndDeletedFalse(cover.getExternalId())).thenReturn(Optional.of(cover));
            when(mediaRepo.findByExternalIdAndDeletedFalse(scanner.getExternalId())).thenReturn(Optional.of(scanner));
            when(mediaUrlService.generateUrl(cover)).thenReturn("https://cdn.example/events/200/cover.jpg");
            when(mediaUrlService.generateUrl(scanner)).thenReturn("https://cdn.example/events/200/scanner.png");
            when(eventRepo.save(any(CommunityEvent.class))).thenAnswer(inv -> {
                CommunityEvent e = inv.getArgument(0);
                e.setId(200L);
                return e;
            });

            EventResponse response = eventService.create(req, adminUser, community);

            assertThat(response.getImageMediaId()).isEqualTo(cover.getExternalId().toString());
            assertThat(response.getScannerMediaId()).isEqualTo(scanner.getExternalId().toString());
        }

        @Test
        @DisplayName("create rejects null startDate")
        void create_missingStartDate() {
            EventRequest req = TestDataBuilder.eventRequest("Diwali Mela");
            req.setStartDate(null);

            assertThatThrownBy(() -> eventService.create(req, adminUser, community))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("start date is required");
            verify(eventRepo, never()).save(any(CommunityEvent.class));
        }

        @Test
        @DisplayName("create rejects unparseable startDate")
        void create_invalidStartDate() {
            EventRequest req = TestDataBuilder.eventRequest("Diwali Mela");
            req.setStartDate("not-a-date");

            assertThatThrownBy(() -> eventService.create(req, adminUser, community))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("Invalid event start date");
            verify(eventRepo, never()).save(any(CommunityEvent.class));
        }

        @Test
        @DisplayName("create rejects endDate before startDate")
        void create_endDateBeforeStartDate() {
            EventRequest req = TestDataBuilder.eventRequest("Diwali Mela");
            req.setStartDate(LocalDate.now().plusDays(10).toString());
            req.setEndDate(LocalDate.now().plusDays(5).toString());

            assertThatThrownBy(() -> eventService.create(req, adminUser, community))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("End date must not be before start date");
        }

        @Test
        @DisplayName("create rejects endTime before startTime on same day")
        void create_endTimeBeforeStartTime() {
            EventRequest req = TestDataBuilder.eventRequest("Diwali Mela");
            req.setStartTime("18:00");
            req.setEndTime("10:00");
            req.setEndDate(null);

            assertThatThrownBy(() -> eventService.create(req, adminUser, community))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("End time must not be before start time");
        }

        @Test
        @DisplayName("create allows endTime before startTime when endDate differs")
        void create_endTimeBeforeStartTime_differentDay() {
            EventRequest req = TestDataBuilder.eventRequest("Diwali Mela");
            req.setStartDate(LocalDate.now().plusDays(5).toString());
            req.setEndDate(LocalDate.now().plusDays(6).toString());
            req.setStartTime("18:00");
            req.setEndTime("10:00");
            when(eventRepo.save(any(CommunityEvent.class))).thenAnswer(inv -> {
                CommunityEvent e = inv.getArgument(0);
                e.setId(200L);
                return e;
            });

            EventResponse response = eventService.create(req, adminUser, community);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("create rejects PAID event without price")
        void create_paidEventWithoutPrice() {
            EventRequest req = TestDataBuilder.eventRequest("Diwali Mela");
            req.setPriceType("PAID");
            req.setPrice(null);

            assertThatThrownBy(() -> eventService.create(req, adminUser, community))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("Price is required");
        }

        @Test
        @DisplayName("create rejects PAID event with zero price")
        void create_paidEventWithZeroPrice() {
            EventRequest req = TestDataBuilder.eventRequest("Diwali Mela");
            req.setPriceType("PAID");
            req.setPrice(0.0);

            assertThatThrownBy(() -> eventService.create(req, adminUser, community))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("Price is required");
        }

        @Test
        @DisplayName("create parses registrationDeadline into event")
        void create_withRegistrationDeadline() {
            EventRequest req = TestDataBuilder.eventRequest("Diwali Mela");
            req.setRegistrationDeadline(LocalDate.now().plusDays(3).toString());
            when(eventRepo.save(any(CommunityEvent.class))).thenAnswer(inv -> {
                CommunityEvent e = inv.getArgument(0);
                e.setId(200L);
                return e;
            });

            EventResponse response = eventService.create(req, adminUser, community);

            assertThat(response.getRegistrationDeadline())
                    .isEqualTo(LocalDate.now().plusDays(3).toString());
        }
    }

    // ── Update Event ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Update Event")
    class UpdateEvent {

        @Test
        @DisplayName("update modifies title and saves")
        void update_success() {
            EventRequest req = TestDataBuilder.eventRequest("Updated Sports Meet");
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(eventRepo.save(any(CommunityEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            EventResponse response = eventService.update(100L, req, adminUser.getId());

            assertThat(response.getTitle()).isEqualTo("Updated Sports Meet");
            verify(eventRepo).save(any(CommunityEvent.class));
        }

        @Test
        @DisplayName("update throws ResourceNotFoundException when event missing")
        void update_notFound() {
            EventRequest req = TestDataBuilder.eventRequest("Hacked Title");
            when(eventRepo.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.update(999L, req, adminUser.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("update rejects changes to cancelled event")
        void update_cancelledEventFrozen() {
            event.setStatus(CommunityEvent.EventStatus.CANCELLED);
            EventRequest req = TestDataBuilder.eventRequest("Change Title");
            req.setStatus(null);
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.update(100L, req, adminUser.getId()))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("Cannot modify a cancelled event");
        }

        @Test
        @DisplayName("update rejects endDate before startDate")
        void update_endDateBeforeStartDate() {
            EventRequest req = new EventRequest();
            req.setEndDate(LocalDate.now().minusDays(30).toString());
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.update(100L, req, adminUser.getId()))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("End date must not be before start date");
        }

        @Test
        @DisplayName("update rejects capacity below current registrations")
        void update_capacityBelowRegistrations() {
            EventRequest req = new EventRequest();
            req.setCapacity(2);
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(regRepo.countByEventId(100L)).thenReturn(5L);

            assertThatThrownBy(() -> eventService.update(100L, req, adminUser.getId()))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("Capacity cannot be less than current registrations");
        }

        @Test
        @DisplayName("update accepts capacity equal to current registrations")
        void update_capacityEqualToRegistrations() {
            EventRequest req = new EventRequest();
            req.setCapacity(5);
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(regRepo.countByEventId(100L)).thenReturn(5L);
            when(eventRepo.save(any(CommunityEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            EventResponse response = eventService.update(100L, req, adminUser.getId());

            assertThat(response.getCapacity()).isEqualTo(5);
        }

        @Test
        @DisplayName("update rejects maxAttendees below current registrations")
        void update_maxAttendeesBelowRegistrations() {
            EventRequest req = new EventRequest();
            req.setMaxAttendees(1);
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(regRepo.countByEventId(100L)).thenReturn(3L);

            assertThatThrownBy(() -> eventService.update(100L, req, adminUser.getId()))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("Max attendees cannot be less than current registrations");
        }

        @Test
        @DisplayName("update rejects negative price")
        void update_negativePrice() {
            EventRequest req = new EventRequest();
            req.setPrice(-50.0);
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.update(100L, req, adminUser.getId()))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("Price must not be negative");
        }

        @Test
        @DisplayName("update sends notification to registered users")
        void update_notifiesRegisteredUsers() {
            EventRegistration reg = TestDataBuilder.eventRegistration(1L, event, memberUser);
            event.setRegistrations(new ArrayList<>(List.of(reg)));
            EventRequest req = TestDataBuilder.eventRequest("Updated Title");
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(eventRepo.save(any(CommunityEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            eventService.update(100L, req, adminUser.getId());

            verify(notificationRepo).save(argThat(n ->
                    n.getTitle().contains("Event Updated")));
        }

        @Test
        @DisplayName("cancellation sends notification to registered users")
        void update_cancellationNotifiesUsers() {
            EventRegistration reg = TestDataBuilder.eventRegistration(1L, event, memberUser);
            event.setRegistrations(new ArrayList<>(List.of(reg)));
            EventRequest req = new EventRequest();
            req.setStatus("CANCELLED");
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(eventRepo.save(any(CommunityEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            eventService.update(100L, req, adminUser.getId());

            verify(notificationRepo).save(argThat(n ->
                    n.getTitle().contains("Event Cancelled")));
        }

        @Test
        @DisplayName("cancellation returns immediately without second save")
        void update_cancellationReturnsEarly() {
            EventRequest req = new EventRequest();
            req.setStatus("CANCELLED");
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(eventRepo.save(any(CommunityEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            EventResponse response = eventService.update(100L, req, adminUser.getId());

            assertThat(response.getStatus()).isEqualTo("CANCELLED");
            verify(eventRepo, times(1)).save(any(CommunityEvent.class));
        }

        @Test
        @DisplayName("update sets registrationDeadline")
        void update_registrationDeadline() {
            String deadline = LocalDate.now().plusDays(3).toString();
            EventRequest req = new EventRequest();
            req.setRegistrationDeadline(deadline);
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(eventRepo.save(any(CommunityEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            EventResponse response = eventService.update(100L, req, adminUser.getId());

            assertThat(response.getRegistrationDeadline()).isEqualTo(deadline);
        }

        @Test
        @DisplayName("update replaces legacy image URL with media-backed URL")
        void update_withMediaReference() {
            EventRequest req = TestDataBuilder.eventRequest("Updated Sports Meet");
            MediaObject cover = mediaObject(UUID.randomUUID(), MediaType.IMAGE);
            req.setImageMediaId(cover.getExternalId().toString());
            req.setImageUrl("https://temporary-upload-url.example/cover");

            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(mediaRepo.findByExternalIdAndDeletedFalse(cover.getExternalId())).thenReturn(Optional.of(cover));
            when(mediaUrlService.generateUrl(cover)).thenReturn("https://cdn.example/events/100/cover.jpg");
            when(eventRepo.save(any(CommunityEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            EventResponse response = eventService.update(100L, req, adminUser.getId());

            assertThat(response.getImageMediaId()).isEqualTo(cover.getExternalId().toString());
            assertThat(response.getImageUrl()).isEqualTo("https://cdn.example/events/100/cover.jpg");
        }
    }

    // ── Delete Event ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Delete Event")
    class DeleteEvent {

        @Test
        @DisplayName("delete cascades cleanup and deletes event when no registrations")
        void delete_success() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            eventService.delete(100L, adminUser.getId());

            verify(activityRegRepo).deleteByProgramEventId(100L);
            verify(auctionItemRepo).deleteAuctionBidsByEventId(100L);
            verify(regRepo).deleteByEventId(100L);
            verify(volunteerRepo).deleteByEventId(100L);
            verify(donationRepo).deleteByEventId(100L);
            verify(expenseRepo).deleteByEventId(100L);
            verify(sponsorRepo).deleteByEventId(100L);
            verify(taskRepo).deleteByEventId(100L);
            verify(mealRegRepo).deleteByEventId(100L);
            verify(galleryRepo).deleteByEventId(100L);
            verify(invoiceRepo).deleteByEventId(100L);
            verify(auctionItemRepo).deleteByEventId(100L);
            verify(programRepo).deleteByEventId(100L);
            verify(eventRepo).delete(event);
        }

        @Test
        @DisplayName("delete throws UnauthorizedActionException if caller is unauthorized")
        void delete_unauthorized() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.delete(100L, 999L))
                    .isInstanceOf(UnauthorizedActionException.class);
        }

        @Test
        @DisplayName("admin userId=-1 can delete any event")
        void delete_adminBypass() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            eventService.delete(100L, -1L);

            verify(eventRepo).delete(event);
        }

        @Test
        @DisplayName("delete rejects when event has registrations")
        void delete_blockedByEventRegistrations() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(regRepo.countByEventId(100L)).thenReturn(3L);

            assertThatThrownBy(() -> eventService.delete(100L, adminUser.getId()))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("Cannot delete event with 3 registration(s)");
            verify(eventRepo, never()).delete(any());
        }

        @Test
        @DisplayName("delete rejects when event has booking registrations")
        void delete_blockedByBookingRegistrations() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(regRepo.countByEventId(100L)).thenReturn(0L);
            when(bookingRegRepo.countByActivityIdAndStatusNot("event-100", "CANCELLED")).thenReturn(2L);

            assertThatThrownBy(() -> eventService.delete(100L, adminUser.getId()))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("Cannot delete event with 2 registration(s)");
            verify(eventRepo, never()).delete(any());
        }

        @Test
        @DisplayName("delete counts both registration types together")
        void delete_combinesBothRegistrationCounts() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(regRepo.countByEventId(100L)).thenReturn(1L);
            when(bookingRegRepo.countByActivityIdAndStatusNot("event-100", "CANCELLED")).thenReturn(2L);

            assertThatThrownBy(() -> eventService.delete(100L, adminUser.getId()))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("Cannot delete event with 3 registration(s)");
        }

        @Test
        @DisplayName("delete allowed when only cancelled booking registrations exist")
        void delete_allowedWithOnlyCancelledBookings() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(regRepo.countByEventId(100L)).thenReturn(0L);
            when(bookingRegRepo.countByActivityIdAndStatusNot("event-100", "CANCELLED")).thenReturn(0L);

            eventService.delete(100L, adminUser.getId());

            verify(eventRepo).delete(event);
        }
    }

    // ── Event Registration ───────────────────────────────────────────

    @Nested
    @DisplayName("Event Registration")
    class Registration {

        @Test
        @DisplayName("register happy path creates registration")
        void register_success() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(regRepo.existsByEventIdAndUserId(100L, memberUser.getId())).thenReturn(false);
            when(regRepo.countByEventId(100L)).thenReturn(0L);
            when(regRepo.save(any(EventRegistration.class))).thenAnswer(inv -> {
                EventRegistration r = inv.getArgument(0);
                r.setId(10L);
                return r;
            });

            EventResponse response = eventService.register(100L, memberUser);

            assertThat(response).isNotNull();
            verify(regRepo).save(any(EventRegistration.class));
        }

        @Test
        @DisplayName("register rejects cancelled event")
        void register_cancelledEvent() {
            event.setStatus(CommunityEvent.EventStatus.CANCELLED);
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.register(100L, memberUser))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("cancelled event");
        }

        @Test
        @DisplayName("register rejects draft event")
        void register_draftEvent() {
            event.setStatus(CommunityEvent.EventStatus.DRAFT);
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.register(100L, memberUser))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("draft event");
        }

        @Test
        @DisplayName("register rejects ended event (endDate in past)")
        void register_endedEvent() {
            event.setEndDate(LocalDate.now().minusDays(1));
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.register(100L, memberUser))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("already ended");
        }

        @Test
        @DisplayName("register rejects passed single-day event (startDate in past, no endDate)")
        void register_passedSingleDayEvent() {
            event.setStartDate(LocalDate.now().minusDays(1));
            event.setEndDate(null);
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.register(100L, memberUser))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("already passed");
        }

        @Test
        @DisplayName("register rejects when deadline has passed for non-admin")
        void register_deadlinePassed() {
            event.setRegistrationDeadline(LocalDate.now().minusDays(1));
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.register(100L, memberUser))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("Registration deadline has passed");
        }

        @Test
        @DisplayName("admin (userId=-1) can register past deadline")
        void register_adminBypassesDeadline() {
            event.setRegistrationDeadline(LocalDate.now().minusDays(1));
            AppUser admin = TestDataBuilder.appUser(-1L, "admin@sys.com", "ADMIN");
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(regRepo.existsByEventIdAndUserId(100L, -1L)).thenReturn(false);
            when(regRepo.countByEventId(100L)).thenReturn(0L);
            when(regRepo.save(any(EventRegistration.class))).thenAnswer(inv -> {
                EventRegistration r = inv.getArgument(0);
                r.setId(10L);
                return r;
            });

            EventResponse response = eventService.register(100L, admin);

            assertThat(response).isNotNull();
            verify(regRepo).save(any(EventRegistration.class));
        }

        @Test
        @DisplayName("register throws AlreadyRegisteredException if already registered")
        void register_alreadyRegistered() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(regRepo.existsByEventIdAndUserId(100L, memberUser.getId())).thenReturn(true);

            assertThatThrownBy(() -> eventService.register(100L, memberUser))
                    .isInstanceOf(AlreadyRegisteredException.class);
        }

        @Test
        @DisplayName("register throws EventFullException when DB count reaches capacity")
        void register_capacityFull() {
            event.setCapacity(5);
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(regRepo.existsByEventIdAndUserId(100L, memberUser.getId())).thenReturn(false);
            when(regRepo.countByEventId(100L)).thenReturn(5L);

            assertThatThrownBy(() -> eventService.register(100L, memberUser))
                    .isInstanceOf(EventFullException.class);
        }

        @Test
        @DisplayName("register succeeds when count is below capacity")
        void register_belowCapacity() {
            event.setCapacity(10);
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(regRepo.existsByEventIdAndUserId(100L, memberUser.getId())).thenReturn(false);
            when(regRepo.countByEventId(100L)).thenReturn(9L);
            when(regRepo.save(any(EventRegistration.class))).thenAnswer(inv -> {
                EventRegistration r = inv.getArgument(0);
                r.setId(10L);
                return r;
            });

            EventResponse response = eventService.register(100L, memberUser);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("register skips capacity check when capacity is null")
        void register_nullCapacityAllowed() {
            event.setCapacity(null);
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(regRepo.existsByEventIdAndUserId(100L, memberUser.getId())).thenReturn(false);
            when(regRepo.save(any(EventRegistration.class))).thenAnswer(inv -> {
                EventRegistration r = inv.getArgument(0);
                r.setId(10L);
                return r;
            });

            EventResponse response = eventService.register(100L, memberUser);

            assertThat(response).isNotNull();
            verify(regRepo, never()).countByEventId(anyLong());
        }

        @Test
        @DisplayName("unregister removes existing registration")
        void unregister_success() {
            EventRegistration registration = TestDataBuilder.eventRegistration(10L, event, memberUser);
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(regRepo.findByEventIdAndUserId(100L, memberUser.getId())).thenReturn(Optional.of(registration));

            EventResponse response = eventService.unregister(100L, memberUser.getId());

            assertThat(response).isNotNull();
            verify(regRepo).delete(registration);
        }

        @Test
        @DisplayName("confirmRegistration updates status to CONFIRMED")
        void confirmRegistration_success() {
            EventRegistration reg = TestDataBuilder.eventRegistration(10L, event, memberUser);
            reg.setStatus(EventRegistration.RegistrationStatus.PENDING);
            when(regRepo.findById(10L)).thenReturn(Optional.of(reg));
            when(regRepo.save(any(EventRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

            RegistrationResponse response = eventService.confirmRegistration(10L);

            assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        }

        @Test
        @DisplayName("rejectRegistration updates status to REJECTED")
        void rejectRegistration_success() {
            EventRegistration reg = TestDataBuilder.eventRegistration(10L, event, memberUser);
            reg.setStatus(EventRegistration.RegistrationStatus.PENDING);
            when(regRepo.findById(10L)).thenReturn(Optional.of(reg));
            when(regRepo.save(any(EventRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

            RegistrationResponse response = eventService.rejectRegistration(10L);

            assertThat(response.getStatus()).isEqualTo("REJECTED");
        }
    }

    // ── Check-in ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Check-in")
    class CheckIn {

        @Test
        @DisplayName("toggleCheckIn marks checked in with timestamp")
        void toggleCheckIn_in() {
            EventRegistration reg = TestDataBuilder.eventRegistration(10L, event, memberUser);
            reg.setCheckedIn(false);
            when(regRepo.findById(10L)).thenReturn(Optional.of(reg));
            when(regRepo.save(any(EventRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

            RegistrationResponse response = eventService.toggleCheckIn(10L, true);

            assertThat(response.isCheckedIn()).isTrue();
            assertThat(response.getCheckedInAt()).isNotNull();
        }

        @Test
        @DisplayName("toggleCheckIn clears check-in state")
        void toggleCheckIn_out() {
            EventRegistration reg = TestDataBuilder.eventRegistration(10L, event, memberUser);
            reg.setCheckedIn(true);
            reg.setCheckedInAt(LocalDateTime.now());
            when(regRepo.findById(10L)).thenReturn(Optional.of(reg));
            when(regRepo.save(any(EventRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

            RegistrationResponse response = eventService.toggleCheckIn(10L, false);

            assertThat(response.isCheckedIn()).isFalse();
            assertThat(response.getCheckedInAt()).isNull();
        }
    }

    // ── Dashboard Stats ──────────────────────────────────────────────

    @Nested
    @DisplayName("Dashboard Stats")
    class DashboardStats {

        @Test
        @DisplayName("getDashboardStats aggregates community metrics")
        void getDashboardStats_success() {
            when(eventRepo.countByCommunityId(1L)).thenReturn(5L);
            when(regRepo.countByEventCommunityId(1L)).thenReturn(50L);
            when(volunteerRepo.countByCommunityId(1L)).thenReturn(10L);
            when(donationRepo.sumAmountByCommunity(1L)).thenReturn(15000.0);
            when(expenseRepo.sumAmountByCommunity(1L)).thenReturn(5000.0);
            when(sponsorRepo.sumAmountReceivedByCommunity(1L)).thenReturn(20000.0);
            when(sponsorRepo.findByEventCommunityIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());
            when(auctionItemRepo.sumCurrentBidsByCommunity(1L)).thenReturn(2500.0);
            when(auctionItemRepo.countByCommunityIdAndBidCountGreaterThan(1L, 0)).thenReturn(2L);

            DashboardStatsResponse stats = eventService.getDashboardStats(1L);

            assertThat(stats).isNotNull();
            assertThat(stats.getTotalEvents()).isEqualTo(5);
            assertThat(stats.getTotalRegistrations()).isEqualTo(50);
            assertThat(stats.getTotalVolunteers()).isEqualTo(10);
            assertThat(stats.getTotalRevenue()).isGreaterThan(0.0);
            assertThat(stats.getTotalExpenses()).isEqualTo(5000.0);
        }
    }

    // ── Event Registrations List ─────────────────────────────────────

    @Nested
    @DisplayName("Event Registrations List")
    class EventRegistrationsList {

        @Test
        @DisplayName("getEventRegistrations returns mapped registration responses")
        void getEventRegistrations_success() {
            EventRegistration reg = TestDataBuilder.eventRegistration(10L, event, memberUser);
            when(regRepo.findByEventId(100L)).thenReturn(List.of(reg));

            List<RegistrationResponse> result = eventService.getEventRegistrations(100L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(memberUser.getId());
            assertThat(result.get(0).getEventTitle()).isEqualTo(event.getTitle());
        }

        @Test
        @DisplayName("getEventRegistrations includes checkedIn state")
        void getEventRegistrations_includesCheckIn() {
            EventRegistration reg = TestDataBuilder.eventRegistration(10L, event, memberUser);
            reg.setCheckedIn(true);
            reg.setCheckedInAt(LocalDateTime.of(2026, 9, 1, 10, 30));
            when(regRepo.findByEventId(100L)).thenReturn(List.of(reg));

            List<RegistrationResponse> result = eventService.getEventRegistrations(100L);

            assertThat(result.get(0).isCheckedIn()).isTrue();
            assertThat(result.get(0).getCheckedInAt()).contains("2026-09-01");
        }
    }

    private MediaObject mediaObject(UUID externalId, MediaType mediaType) {
        return MediaObject.builder()
                .externalId(externalId)
                .module(MediaModule.EVENT)
                .moduleId("pending")
                .communityId(community.getId())
                .subContext("pending")
                .originalFileName("event-image.png")
                .storedFileName(externalId + ".png")
                .mimeType(mediaType == MediaType.QR_CODE ? "image/png" : "image/jpeg")
                .extension(mediaType == MediaType.QR_CODE ? "png" : "jpg")
                .fileSize(1024L)
                .mediaType(mediaType)
                .bucketName("test-bucket")
                .s3Key("events/pending/" + externalId)
                .uploadedBy(adminUser.getId())
                .status(MediaStatus.ACTIVE)
                .build();
    }
}
