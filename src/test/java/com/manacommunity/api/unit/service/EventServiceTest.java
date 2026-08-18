package com.manacommunity.api.unit.service;

import com.manacommunity.api.events.dto.*;
import com.manacommunity.api.events.entity.*;
import com.manacommunity.api.events.repository.*;
import com.manacommunity.api.events.service.EventService;
import com.manacommunity.api.exception.AlreadyRegisteredException;
import com.manacommunity.api.exception.EventFullException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.support.TestDataBuilder;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.email.EmailService;
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
    @Mock AppUserRepository userRepo;
    @Mock EmailService emailService;
    @Mock EventVenueRepository venueRepo;
    @Mock EventInvoiceRepository invoiceRepo;
    @Mock EventGalleryItemRepository galleryRepo;
    @Mock EventProgramRepository programRepo;
    @Mock PoojaSevaRepository poojaSevaRepo;
    @Mock CulturalEventRepository culturalEventRepo;
    @Mock LunchDinnerRepository lunchDinnerRepo;
    @Mock CompetitionRepository competitionRepo;
    @Mock EventBookingRegistrationRepository eventBookingRegRepo;
    @Mock EventFamilyMemberRepository familyMemberRepo;

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
            when(eventRepo.findUpcomingByCommunityAndType(eq(1L), any(CommunityEvent.EventType.class)))
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
        @DisplayName("update event modifies details when user is authorized")
        void update_success() {
            EventRequest req = TestDataBuilder.eventRequest("Updated Sports Meet");
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(eventRepo.save(any(CommunityEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            EventResponse response = eventService.update(100L, req, adminUser.getId());

            assertThat(response.getTitle()).isEqualTo("Updated Sports Meet");
            verify(eventRepo).save(any(CommunityEvent.class));
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

        @Test
        @DisplayName("delete cascades cleanup for registrations, volunteers, auction items, and deletes event")
        void delete_success() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(regRepo.findByEventId(100L)).thenReturn(Collections.emptyList());
            when(volunteerRepo.findByEventIdOrderByCreatedAtDesc(100L)).thenReturn(Collections.emptyList());
            when(donationRepo.findByEventIdOrderByCreatedAtDesc(100L)).thenReturn(Collections.emptyList());
            when(expenseRepo.findByEventIdOrderByCreatedAtDesc(100L)).thenReturn(Collections.emptyList());
            when(sponsorRepo.findByEventIdOrderByCreatedAtDesc(100L)).thenReturn(Collections.emptyList());
            when(taskRepo.findByEventIdOrderByCreatedAtDesc(100L)).thenReturn(Collections.emptyList());
            when(mealRegRepo.findByEventIdOrdered(100L)).thenReturn(Collections.emptyList());
            when(auctionItemRepo.findByEventId(100L)).thenReturn(Collections.emptyList());

            eventService.delete(100L, adminUser.getId());

            verify(eventRepo).delete(event);
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
            when(regRepo.existsByEventIdAndUserId(100L, memberUser.getId())).thenReturn(false);
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
        @DisplayName("register throws AlreadyRegisteredException if already registered")
        void register_alreadyRegistered() {
            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(regRepo.existsByEventIdAndUserId(100L, memberUser.getId())).thenReturn(true);

            assertThatThrownBy(() -> eventService.register(100L, memberUser))
                    .isInstanceOf(AlreadyRegisteredException.class);
        }

        @Test
        @DisplayName("register throws EventFullException when capacity is reached")
        void register_capacityFull() {
            event.setCapacity(1);
            event.getRegistrations().add(TestDataBuilder.eventRegistration(1L, event, adminUser));

            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(regRepo.existsByEventIdAndUserId(100L, memberUser.getId())).thenReturn(false);

            assertThatThrownBy(() -> eventService.register(100L, memberUser))
                    .isInstanceOf(EventFullException.class);
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

            assertThat(response).isNotNull();
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

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("REJECTED");
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
            when(regRepo.countByEventCommunityId(1L)).thenReturn(50L);
            when(volunteerRepo.countByCommunityId(1L)).thenReturn(10L);
            when(donationRepo.sumAmountByCommunity(1L)).thenReturn(15000.0);
            when(expenseRepo.sumAmountByCommunity(1L)).thenReturn(5000.0);
            when(sponsorRepo.sumAmountReceivedByCommunity(1L)).thenReturn(20000.0);
            when(sponsorRepo.findByEventCommunityIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());
            when(auctionItemRepo.sumCurrentBidsByCommunity(1L)).thenReturn(2500.0);
            when(auctionItemRepo.countByCommunityIdAndBidCountGreaterThan(1L, 0)).thenReturn(2L);
            when(eventRepo.findByCommunityIdOrderByStartDateDesc(1L)).thenReturn(List.of(event));

            DashboardStatsResponse stats = eventService.getDashboardStats(1L);

            assertThat(stats).isNotNull();
            assertThat(stats.getTotalEvents()).isEqualTo(5);
            assertThat(stats.getTotalRegistrations()).isEqualTo(50);
            assertThat(stats.getTotalVolunteers()).isEqualTo(10);
            assertThat(stats.getTotalRevenue()).isGreaterThan(0.0);
            assertThat(stats.getTotalExpenses()).isEqualTo(5000.0);
        }
    }
}
