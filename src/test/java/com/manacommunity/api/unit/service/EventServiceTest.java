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
import com.manacommunity.api.support.TestDataBuilder;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.email.EmailService;
import com.manacommunity.api.repository.AuctionPlayerRepository;
import com.manacommunity.api.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService")
class EventServiceTest {

    @Mock EventCommunityRepository eventRepo;
    @Mock EventVolunteerRepository volunteerRepo;
    @Mock EventDonationRepository donationRepo;
    @Mock EventExpenseRepository expenseRepo;
    @Mock EventSponsorRepository sponsorRepo;
    @Mock EventTaskRepository taskRepo;
    @Mock EventMealRegistrationRepository mealRegRepo;
    @Mock EventAuctionItemRepository auctionItemRepo;
    @Mock AuctionPlayerRepository auctionPlayerRepo;
    @Mock EventActivityRegistrationRepository activityRegRepo;
    @Mock EventInvoiceRepository invoiceRepo;
    @Mock EventGalleryItemRepository galleryRepo;
    @Mock EventProgramRepository programRepo;
    @Mock PoojaSevaRepository poojaSevaRepo;
    @Mock CulturalEventRepository culturalEventRepo;
    @Mock LunchDinnerRepository lunchDinnerRepo;
    @Mock CompetitionRepository competitionRepo;
    @Mock EventBookingRegistrationRepository bookingRegRepo;
    @Mock EventFamilyMemberRepository familyMemberRepo;
    @Mock MediaRepository mediaRepo;
    @Mock MediaUrlService mediaUrlService;
    @Mock EventTicketCategoryRepository ticketCategoryRepo;
    @Mock EventContactRepository eventContactRepo;
    @Mock NotificationRepository notificationRepo;
    @Mock AppUserRepository appUserRepo;
    @Mock com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private EventService eventService;

    private Community community;
    private AppUser adminUser;
    private AppUser memberUser;
    private EventCommunity event;

    @BeforeEach
    void setUp() {
        eventService = new EventService(
                eventRepo, volunteerRepo, donationRepo, expenseRepo,
                sponsorRepo, taskRepo, mealRegRepo, auctionItemRepo, auctionPlayerRepo,
                activityRegRepo, programRepo, galleryRepo, invoiceRepo, mediaRepo,
                mediaUrlService, bookingRegRepo, notificationRepo, poojaSevaRepo,
                culturalEventRepo, competitionRepo, lunchDinnerRepo, familyMemberRepo,
                ticketCategoryRepo, eventContactRepo, appUserRepo, objectMapper
        );
        community = TestDataBuilder.community(1L, "INVITE123");
        adminUser = TestDataBuilder.adminUser();
        memberUser = TestDataBuilder.memberUser();
        event = TestDataBuilder.communityEvent(100L, community, adminUser);
    }

    // ── getUpcomingEvents & getAllEvents ──────────────────────────────

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
            when(eventRepo.findUpcomingByCommunityAndType(eq(1L), any(EventCommunity.EventType.class)))
                    .thenReturn(List.of(event));

            List<EventResponse> result = eventService.getUpcomingEvents(1L, "SPORTS", adminUser.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo("SPORTS");
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
    }

    // ── Create & Update ───────────────────────────────────────────────

    @Nested
    @DisplayName("Create and Update Events")
    class CreateAndUpdate {

        @Test
        @DisplayName("create event saves and returns EventResponse")
        void create_success() {
            EventRequest req = TestDataBuilder.eventRequest("Diwali Mela");
            when(eventRepo.save(any(EventCommunity.class))).thenAnswer(inv -> {
                EventCommunity e = inv.getArgument(0);
                e.setId(200L);
                return e;
            });

            EventResponse response = eventService.create(req, adminUser, community);

            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("Diwali Mela");
            verify(eventRepo).save(any(EventCommunity.class));
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
            when(eventRepo.save(any(EventCommunity.class))).thenAnswer(inv -> {
                EventCommunity e = inv.getArgument(0);
                e.setId(200L);
                return e;
            });

            EventResponse response = eventService.create(req, adminUser, community);

            assertThat(response.getImageMediaId()).isEqualTo(cover.getExternalId().toString());
            assertThat(response.getScannerMediaId()).isEqualTo(scanner.getExternalId().toString());
            assertThat(response.getImageUrl()).isEqualTo("https://cdn.example/events/200/cover.jpg");
            assertThat(response.getScannerUrl()).isEqualTo("https://cdn.example/events/200/scanner.png");
            assertThat(cover.getModuleId()).isEqualTo("200");
            assertThat(cover.getSubContext()).isEqualTo("cover");
            assertThat(cover.isFeatured()).isTrue();
            assertThat(scanner.getModuleId()).isEqualTo("200");
            assertThat(scanner.getSubContext()).isEqualTo("payment-scanner");
            verify(mediaRepo).save(cover);
            verify(mediaRepo).save(scanner);
        }

        @Test
        @DisplayName("create event rejects media from a different community")
        void create_rejectsDifferentCommunityMedia() {
            EventRequest req = TestDataBuilder.eventRequest("Diwali Mela");
            MediaObject cover = mediaObject(UUID.randomUUID(), MediaType.IMAGE);
            cover.setCommunityId(999L);
            req.setImageMediaId(cover.getExternalId().toString());
            when(mediaRepo.findByExternalIdAndDeletedFalse(cover.getExternalId())).thenReturn(Optional.of(cover));

            assertThatThrownBy(() -> eventService.create(req, adminUser, community))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("same community");
            verify(eventRepo, never()).save(any(EventCommunity.class));
        }

        @Test
        @DisplayName("create event rejects missing startDate")
        void create_missingStartDate() {
            EventRequest req = TestDataBuilder.eventRequest("Diwali Mela");
            req.setStartDate(null);

            assertThatThrownBy(() -> eventService.create(req, adminUser, community))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("start date is required");
            verify(eventRepo, never()).save(any(EventCommunity.class));
        }

        @Test
        @DisplayName("create event rejects invalid startDate")
        void create_invalidStartDate() {
            EventRequest req = TestDataBuilder.eventRequest("Diwali Mela");
            req.setStartDate("not-a-date");

            assertThatThrownBy(() -> eventService.create(req, adminUser, community))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("Invalid event start date");
            verify(eventRepo, never()).save(any(EventCommunity.class));
        }

        @Test
        @DisplayName("update event modifies details when user is authorized")
        void update_success() {
            EventRequest req = TestDataBuilder.eventRequest("Updated Sports Meet");
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(eventRepo.save(any(EventCommunity.class))).thenAnswer(inv -> inv.getArgument(0));

            EventResponse response = eventService.update(100L, req, adminUser.getId());

            assertThat(response.getTitle()).isEqualTo("Updated Sports Meet");
            verify(eventRepo).save(any(EventCommunity.class));
        }

        @Test
        @DisplayName("update event throws ResourceNotFoundException when event missing")
        void update_notFound() {
            EventRequest req = TestDataBuilder.eventRequest("Hacked Title");
            when(eventRepo.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.update(999L, req, adminUser.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── Delete & Cascades ─────────────────────────────────────────────

    @Nested
    @DisplayName("Delete Event")
    class DeleteEvent {

        @BeforeEach
        void setupDelete() {
            lenient().when(bookingRegRepo.findByActivityId(anyString())).thenReturn(Collections.emptyList());
        }

        @Test
        @DisplayName("delete cascades cleanup for sub-events, registrations, volunteers, auction items, and deletes event")
        void delete_success() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            EventPoojaSeva pooja = new EventPoojaSeva();
            pooja.setId(10L);
            pooja.setMainEventId(100L);
            when(poojaSevaRepo.findByMainEventIdOrderByDateAscStartTimeAsc(100L)).thenReturn(List.of(pooja));

            EventCulturalEvent cultural = new EventCulturalEvent();
            cultural.setId(20L);
            cultural.setMainEventId(100L);
            when(culturalEventRepo.findByMainEventIdOrderByDateAscStartTimeAsc(100L)).thenReturn(List.of(cultural));

            EventCompetition comp = new EventCompetition();
            comp.setId(30L);
            comp.setMainEventId(100L);
            when(competitionRepo.findByMainEventIdOrderByDateAscStartTimeAsc(100L)).thenReturn(List.of(comp));

            EventLunchDinner ld = new EventLunchDinner();
            ld.setId(40L);
            ld.setMainEventId(100L);
            when(lunchDinnerRepo.findByMainEventIdOrderByDateAscStartTimeAsc(100L)).thenReturn(List.of(ld));

            EventFamilyMember familyMember = new EventFamilyMember();
            familyMember.setId(50L);
            familyMember.setEvent(event);
            when(familyMemberRepo.findByEventIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(familyMember));
            lenient().when(bookingRegRepo.countByActivityIdAndStatusNot(anyString(), anyString())).thenReturn(0L);

            eventService.delete(100L, adminUser.getId());

            verify(poojaSevaRepo).deleteAll(List.of(pooja));
            verify(culturalEventRepo).deleteAll(List.of(cultural));
            verify(competitionRepo).deleteAll(List.of(comp));
            verify(lunchDinnerRepo).deleteAll(List.of(ld));
            verify(familyMemberRepo).deleteAll(List.of(familyMember));
            verify(activityRegRepo).deleteByProgramEventId(100L);
            verify(auctionItemRepo).deleteAuctionBidsByEventId(100L);
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
        @DisplayName("delete cancels event and registrations when main event has registrations")
        void delete_cancelsEvent_whenMainEventHasRegistrations() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            EventBookingRegistration booking = new EventBookingRegistration();
            booking.setActivityId("event-100");
            when(bookingRegRepo.findByActivityId("event-100")).thenReturn(List.of(booking));

            eventService.delete(100L, adminUser.getId());

            assertThat(event.getStatus()).isEqualTo(EventCommunity.EventStatus.CANCELLED);
            verify(eventRepo).save(event);
            verify(eventRepo, never()).delete(any());
            verify(poojaSevaRepo, never()).deleteAll(any());
        }

        @Test
        @DisplayName("delete cancels event and registrations when a pooja sub-event has active bookings")
        void delete_cancelsEvent_whenPoojaHasBookings() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            EventPoojaSeva pooja = new EventPoojaSeva();
            pooja.setId(10L);
            pooja.setMainEventId(100L);
            when(poojaSevaRepo.findByMainEventIdOrderByDateAscStartTimeAsc(100L)).thenReturn(List.of(pooja));
            EventBookingRegistration booking = new EventBookingRegistration();
            booking.setActivityId("pooja-10");
            when(bookingRegRepo.findByActivityId("pooja-10")).thenReturn(List.of(booking));

            eventService.delete(100L, adminUser.getId());

            assertThat(event.getStatus()).isEqualTo(EventCommunity.EventStatus.CANCELLED);
            verify(eventRepo).save(event);
            verify(eventRepo, never()).delete(any());
            verify(poojaSevaRepo, never()).deleteAll(any());
        }

        @Test
        @DisplayName("delete cancels event and registrations when a cultural sub-event has active bookings")
        void delete_cancelsEvent_whenCulturalHasBookings() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            EventCulturalEvent cultural = new EventCulturalEvent();
            cultural.setId(20L);
            cultural.setMainEventId(100L);
            when(culturalEventRepo.findByMainEventIdOrderByDateAscStartTimeAsc(100L)).thenReturn(List.of(cultural));
            EventBookingRegistration booking = new EventBookingRegistration();
            booking.setActivityId("cultural-20");
            when(bookingRegRepo.findByActivityId("cultural-20")).thenReturn(List.of(booking));

            eventService.delete(100L, adminUser.getId());

            assertThat(event.getStatus()).isEqualTo(EventCommunity.EventStatus.CANCELLED);
            verify(eventRepo).save(event);
            verify(eventRepo, never()).delete(any());
            verify(culturalEventRepo, never()).deleteAll(any());
        }

        @Test
        @DisplayName("delete cancels event and registrations when a competition sub-event has active bookings")
        void delete_cancelsEvent_whenCompetitionHasBookings() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            EventCompetition comp = new EventCompetition();
            comp.setId(30L);
            comp.setMainEventId(100L);
            when(competitionRepo.findByMainEventIdOrderByDateAscStartTimeAsc(100L)).thenReturn(List.of(comp));
            EventBookingRegistration booking = new EventBookingRegistration();
            booking.setActivityId("comp-30");
            when(bookingRegRepo.findByActivityId("comp-30")).thenReturn(List.of(booking));

            eventService.delete(100L, adminUser.getId());

            assertThat(event.getStatus()).isEqualTo(EventCommunity.EventStatus.CANCELLED);
            verify(eventRepo).save(event);
            verify(eventRepo, never()).delete(any());
            verify(competitionRepo, never()).deleteAll(any());
        }

        @Test
        @DisplayName("delete cancels event and registrations when a lunch/dinner sub-event has active bookings")
        void delete_cancelsEvent_whenLunchDinnerHasBookings() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            EventLunchDinner ld = new EventLunchDinner();
            ld.setId(40L);
            ld.setMainEventId(100L);
            when(lunchDinnerRepo.findByMainEventIdOrderByDateAscStartTimeAsc(100L)).thenReturn(List.of(ld));
            EventBookingRegistration booking = new EventBookingRegistration();
            booking.setActivityId("food-40");
            when(bookingRegRepo.findByActivityId("food-40")).thenReturn(List.of(booking));

            eventService.delete(100L, adminUser.getId());

            assertThat(event.getStatus()).isEqualTo(EventCommunity.EventStatus.CANCELLED);
            verify(eventRepo).save(event);
            verify(eventRepo, never()).delete(any());
            verify(lunchDinnerRepo, never()).deleteAll(any());
        }

        @Test
        @DisplayName("delete throws UnauthorizedActionException if caller is unauthorized")
        void delete_unauthorized() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.delete(100L, 999L))
                    .isInstanceOf(UnauthorizedActionException.class);
        }
    }

    // ── Registrations ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Event Registration")
    class Registration {

        @Test
        @DisplayName("register happy path creates registration")
        void register_success() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(bookingRegRepo.existsByUserIdAndActivityIdAndStatusNot(memberUser.getId(), "event-100", "CANCELLED")).thenReturn(false);
            when(bookingRegRepo.save(any(EventBookingRegistration.class))).thenAnswer(inv -> {
                EventBookingRegistration r = inv.getArgument(0);
                r.setId(10L);
                return r;
            });

            EventResponse response = eventService.register(100L, memberUser);

            assertThat(response).isNotNull();
            verify(bookingRegRepo).save(any(EventBookingRegistration.class));
        }

        @Test
        @DisplayName("register throws AlreadyRegisteredException if already registered")
        void register_alreadyRegistered() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(bookingRegRepo.existsByUserIdAndActivityIdAndStatusNot(memberUser.getId(), "event-100", "CANCELLED")).thenReturn(true);

            assertThatThrownBy(() -> eventService.register(100L, memberUser))
                    .isInstanceOf(AlreadyRegisteredException.class);
        }

        @Test
        @DisplayName("register throws EventFullException when capacity is reached")
        void register_capacityFull() {
            event.setCapacity(1);

            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(bookingRegRepo.existsByUserIdAndActivityIdAndStatusNot(memberUser.getId(), "event-100", "CANCELLED")).thenReturn(false);
            EventBookingRegistration existing = new EventBookingRegistration();
            existing.setStatus("CONFIRMED");
            existing.setDevoteeCount(1);
            when(bookingRegRepo.findByActivityId("event-100")).thenReturn(List.of(existing));

            assertThatThrownBy(() -> eventService.register(100L, memberUser))
                    .isInstanceOf(EventFullException.class);
        }

        @Test
        @DisplayName("unregister removes existing registration")
        void unregister_success() {
            EventBookingRegistration registration = new EventBookingRegistration();
            registration.setId(10L);
            registration.setUser(memberUser);
            registration.setStatus("CONFIRMED");
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(bookingRegRepo.findByActivityId("event-100")).thenReturn(List.of(registration));

            EventResponse response = eventService.unregister(100L, memberUser.getId());

            assertThat(response).isNotNull();
            assertThat(registration.getStatus()).isEqualTo("CANCELLED");
            verify(bookingRegRepo).save(registration);
        }

        @Test
        @DisplayName("confirmRegistration updates status to CONFIRMED")
        void confirmRegistration_success() {
            EventBookingRegistration reg = new EventBookingRegistration();
            reg.setId(10L);
            reg.setStatus("PENDING");
            when(bookingRegRepo.findById(10L)).thenReturn(Optional.of(reg));
            when(bookingRegRepo.save(any(EventBookingRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

            RegistrationResponse response = eventService.confirmRegistration(10L);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        }

        @Test
        @DisplayName("rejectRegistration updates status to CANCELLED")
        void rejectRegistration_success() {
            EventBookingRegistration reg = new EventBookingRegistration();
            reg.setId(10L);
            reg.setStatus("PENDING");
            when(bookingRegRepo.findById(10L)).thenReturn(Optional.of(reg));
            when(bookingRegRepo.save(any(EventBookingRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

            RegistrationResponse response = eventService.rejectRegistration(10L);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("CANCELLED");
        }
    }

    // ── Dashboard Stats & Analytics ───────────────────────────────────

    @Nested
    @DisplayName("Dashboard Stats")
    class DashboardStats {

        @Test
        @DisplayName("getDashboardStats aggregates community metrics")
        void getDashboardStats_success() {
            when(eventRepo.countByCommunityId(1L)).thenReturn(5L);
            when(volunteerRepo.countByCommunityId(1L)).thenReturn(10L);
            when(donationRepo.sumAmountByCommunity(1L)).thenReturn(15000.0);
            when(expenseRepo.sumAmountByCommunity(1L)).thenReturn(5000.0);
            when(sponsorRepo.sumAmountReceivedByCommunity(1L)).thenReturn(20000.0);
            when(sponsorRepo.findByEventCommunityIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());
            when(auctionItemRepo.sumCurrentBidsByCommunity(1L)).thenReturn(2500.0);
            when(eventRepo.findByCommunityIdOrderByStartDateDesc(1L)).thenReturn(List.of(event));

            EventBookingRegistration b = new EventBookingRegistration();
            b.setStatus("CONFIRMED");
            b.setDevoteeCount(50);
            when(bookingRegRepo.findByCommunityIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(b));

            DashboardStatsResponse stats = eventService.getDashboardStats(1L);

            assertThat(stats).isNotNull();
            assertThat(stats.getTotalEvents()).isEqualTo(5);
            assertThat(stats.getTotalRegistrations()).isEqualTo(50);
            assertThat(stats.getTotalVolunteers()).isEqualTo(10);
            assertThat(stats.getTotalRevenue()).isGreaterThan(0.0);
            assertThat(stats.getTotalExpenses()).isEqualTo(5000.0);
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
