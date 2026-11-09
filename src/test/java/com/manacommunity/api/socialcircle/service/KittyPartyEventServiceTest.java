package com.manacommunity.api.socialcircle.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.socialcircle.dto.KittyPartyEventDtos.*;
import com.manacommunity.api.socialcircle.entity.KittyPartyCircle;
import com.manacommunity.api.socialcircle.entity.KittyPartyEvent;
import com.manacommunity.api.socialcircle.entity.KittyPartyEventAttendance;
import com.manacommunity.api.socialcircle.entity.KittyPartyEventRsvp;
import com.manacommunity.api.socialcircle.repository.KittyPartyCircleRepository;
import com.manacommunity.api.socialcircle.repository.KittyPartyEventAttendanceRepository;
import com.manacommunity.api.socialcircle.repository.KittyPartyEventRepository;
import com.manacommunity.api.socialcircle.repository.KittyPartyEventRsvpRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kitty Party Event Service Test Suite")
class KittyPartyEventServiceTest {

    @Mock
    private KittyPartyEventRepository eventRepository;
    @Mock
    private KittyPartyEventRsvpRepository rsvpRepository;
    @Mock
    private KittyPartyEventAttendanceRepository attendanceRepository;
    @Mock
    private KittyPartyCircleRepository circleRepository;
    @Mock
    private AppUserRepository userRepository;

    @InjectMocks
    private KittyPartyEventService eventService;

    private AppUser hostUser;
    private AppUser attendeeUser;
    private KittyPartyCircle circle;
    private KittyPartyEvent event;

    @BeforeEach
    void setUp() {
        Community community = new Community();
        community.setId(1L);
        community.setName("Green Valley");

        hostUser = new AppUser();
        hostUser.setId(10L);
        hostUser.setFullName("Priya Sharma");
        hostUser.setCommunity(community);

        attendeeUser = new AppUser();
        attendeeUser.setId(20L);
        attendeeUser.setFullName("Anjali Verma");
        attendeeUser.setCommunity(community);

        circle = KittyPartyCircle.builder()
                .id(100L)
                .circleName("Mana Kitty Queens")
                .community(community)
                .status("ACTIVE")
                .build();

        event = KittyPartyEvent.builder()
                .id(500L)
                .circle(circle)
                .eventName("November Gold Kitty Meetup")
                .description("Monthly dinner and Tambola evening")
                .eventType("KITTY_MONTHLY")
                .startTime(LocalDateTime.now().plusDays(5))
                .endTime(LocalDateTime.now().plusDays(5).plusHours(3))
                .venueType("HOST_HOME")
                .venueAddress("Villa #42, Green Valley")
                .hostUser(hostUser)
                .maxAttendees(15)
                .recurrenceRule("MONTHLY")
                .status("SCHEDULED")
                .createdBy(hostUser)
                .build();
    }

    @Test
    @DisplayName("Should successfully schedule a kitty party event and auto-RSVP the host")
    void testCreateEventSuccess() {
        when(circleRepository.findById(100L)).thenReturn(Optional.of(circle));
        when(userRepository.findById(10L)).thenReturn(Optional.of(hostUser));
        when(eventRepository.save(any(KittyPartyEvent.class))).thenAnswer(inv -> {
            KittyPartyEvent e = inv.getArgument(0);
            e.setId(500L);
            return e;
        });

        CreateEventRequest req = CreateEventRequest.builder()
                .eventName("November Gold Kitty Meetup")
                .description("Monthly dinner and Tambola")
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(2))
                .hostUserId(10L)
                .venueType("HOST_HOME")
                .maxAttendees(20)
                .build();

        EventResponse resp = eventService.createEvent(hostUser, 100L, req);

        assertNotNull(resp);
        assertEquals(500L, resp.getId());
        assertEquals("November Gold Kitty Meetup", resp.getEventName());
        verify(rsvpRepository).save(any(KittyPartyEventRsvp.class));
    }

    @Test
    @DisplayName("Should throw exception if end time is before start time")
    void testCreateEventInvalidTimeThrowsException() {
        when(circleRepository.findById(100L)).thenReturn(Optional.of(circle));

        CreateEventRequest req = CreateEventRequest.builder()
                .eventName("Invalid Time Meetup")
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(1)) // Before start time
                .build();

        assertThrows(IllegalArgumentException.class, () -> eventService.createEvent(hostUser, 100L, req));
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should list events for a circle")
    void testGetEventsByCircle() {
        when(eventRepository.findByCircleIdOrderByStartTimeAsc(100L)).thenReturn(List.of(event));

        List<EventResponse> events = eventService.getEventsByCircle(100L, 10L);

        assertEquals(1, events.size());
        assertEquals("November Gold Kitty Meetup", events.get(0).getEventName());
    }

    @Test
    @DisplayName("Should submit GOING RSVP when within capacity")
    void testSubmitRsvpGoing() {
        when(eventRepository.findById(500L)).thenReturn(Optional.of(event));
        when(rsvpRepository.countByEventIdAndResponse(500L, "GOING")).thenReturn(5L);
        when(rsvpRepository.findByEventIdAndUserId(500L, 20L)).thenReturn(Optional.empty());
        when(rsvpRepository.save(any(KittyPartyEventRsvp.class))).thenAnswer(inv -> {
            KittyPartyEventRsvp r = inv.getArgument(0);
            r.setId(800L);
            return r;
        });

        RsvpRequest req = RsvpRequest.builder()
                .response("GOING")
                .guestCount(1)
                .notes("Bringing dessert")
                .build();

        RsvpResponse resp = eventService.submitRsvp(attendeeUser, 500L, req);

        assertNotNull(resp);
        assertEquals("GOING", resp.getResponse());
        assertEquals(1, resp.getGuestCount());
        verify(rsvpRepository).save(any(KittyPartyEventRsvp.class));
    }

    @Test
    @DisplayName("Should automatically set RSVP to WAITLISTED if attendee limit is exceeded")
    void testSubmitRsvpCapacityExceededWaitlisted() {
        event.setMaxAttendees(5);
        when(eventRepository.findById(500L)).thenReturn(Optional.of(event));
        // Event capacity is 5, currently 5 going already
        when(rsvpRepository.countByEventIdAndResponse(500L, "GOING")).thenReturn(5L);
        when(rsvpRepository.findByEventIdAndUserId(500L, 20L)).thenReturn(Optional.empty());
        when(rsvpRepository.save(any(KittyPartyEventRsvp.class))).thenAnswer(inv -> {
            KittyPartyEventRsvp r = inv.getArgument(0);
            r.setId(801L);
            return r;
        });

        RsvpRequest req = RsvpRequest.builder()
                .response("GOING")
                .guestCount(0)
                .build();

        RsvpResponse resp = eventService.submitRsvp(attendeeUser, 500L, req);

        assertNotNull(resp);
        assertEquals("WAITLISTED", resp.getResponse());
    }

    @Test
    @DisplayName("Should update existing event details")
    void testUpdateEvent() {
        when(eventRepository.findById(500L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(KittyPartyEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateEventRequest req = UpdateEventRequest.builder()
                .eventName("Updated Kitty Gala")
                .venueAddress("New Banquet Hall")
                .build();

        EventResponse resp = eventService.updateEvent(hostUser, 500L, req);

        assertEquals("Updated Kitty Gala", resp.getEventName());
        assertEquals("New Banquet Hall", resp.getVenueAddress());
    }

    @Test
    @DisplayName("Should mark member attendance as PRESENT")
    void testMarkAttendance() {
        when(eventRepository.findById(500L)).thenReturn(Optional.of(event));
        when(userRepository.findById(20L)).thenReturn(Optional.of(attendeeUser));
        when(attendanceRepository.findByEventIdAndUserId(500L, 20L)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(KittyPartyEventAttendance.class))).thenAnswer(inv -> {
            KittyPartyEventAttendance a = inv.getArgument(0);
            a.setId(900L);
            return a;
        });

        MarkAttendanceRequest req = MarkAttendanceRequest.builder()
                .userId(20L)
                .attendanceStatus("PRESENT")
                .build();

        AttendanceResponse resp = eventService.markAttendance(hostUser, 500L, req);

        assertNotNull(resp);
        assertEquals("PRESENT", resp.getAttendanceStatus());
        assertEquals("Anjali Verma", resp.getUserName());
    }

    @Test
    @DisplayName("Should return event summary with counts")
    void testGetEventSummary() {
        when(eventRepository.findByCircleIdOrderByStartTimeAsc(100L)).thenReturn(List.of(event));

        EventSummaryDto summary = eventService.getEventSummary(100L, 10L);

        assertNotNull(summary);
        assertEquals(1, summary.getTotalEvents());
        assertEquals(1, summary.getUpcomingEvents());
        assertEquals(0, summary.getPastEvents());
    }
}
