package com.manacommunity.api.unit.service;

import com.manacommunity.api.events.dto.EventVolunteerRequest;
import com.manacommunity.api.events.dto.EventVolunteerResponse;
import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.entity.EventVolunteer;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.EventVolunteerRepository;
import com.manacommunity.api.events.service.EventVolunteerService;
import com.manacommunity.api.exception.AlreadyRegisteredException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.support.TestDataBuilder;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventVolunteerService")
class EventVolunteerServiceTest {

    @Mock EventVolunteerRepository volunteerRepo;
    @Mock EventCommunityRepository eventRepo;
    @Mock AppUserRepository userRepo;

    @InjectMocks EventVolunteerService volunteerService;

    private Community community;
    private AppUser adminUser;
    private AppUser volunteerUser;
    private EventCommunity event;
    private EventVolunteer volunteer;

    @BeforeEach
    void setUp() {
        community = TestDataBuilder.community(1L, "INVITE123");
        adminUser = TestDataBuilder.adminUser();
        volunteerUser = TestDataBuilder.memberUser();
        event = TestDataBuilder.communityEvent(100L, community, adminUser);

        volunteer = EventVolunteer.builder()
                .id(20L)
                .event(event)
                .user(volunteerUser)
                .role("Coordinator")
                .status("CONFIRMED")
                .volunteerStatus(EventVolunteer.VolunteerStatus.ASSIGNED)
                .community(community)
                .build();
    }

    @Nested
    @DisplayName("Query Volunteers")
    class QueryVolunteers {

        @Test
        @DisplayName("getByEvent returns volunteer list for event")
        void getByEvent_success() {
            when(volunteerRepo.findByEventIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(volunteer));

            List<EventVolunteerResponse> list = volunteerService.getByEvent(100L);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getRole()).isEqualTo("Coordinator");
        }

        @Test
        @DisplayName("getByCommunity returns volunteer list for community")
        void getByCommunity_success() {
            when(volunteerRepo.findByCommunityIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(volunteer));

            List<EventVolunteerResponse> list = volunteerService.getByCommunity(1L);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getId()).isEqualTo(20L);
        }
    }

    @Nested
    @DisplayName("Create & Status Updates")
    class ModifyVolunteers {

        @Test
        @DisplayName("create volunteer happy path")
        void create_success() {
            EventVolunteerRequest req = new EventVolunteerRequest();
            req.setEventId(100L);
            req.setUserId(volunteerUser.getId());
            req.setRole("Security Lead");

            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(volunteerRepo.existsByEventIdAndUserId(100L, volunteerUser.getId())).thenReturn(false);
            when(userRepo.findById(volunteerUser.getId())).thenReturn(Optional.of(volunteerUser));
            when(volunteerRepo.save(any(EventVolunteer.class))).thenReturn(volunteer);

            EventVolunteerResponse res = volunteerService.create(req, community);

            assertThat(res).isNotNull();
            verify(volunteerRepo).save(any(EventVolunteer.class));
        }

        @Test
        @DisplayName("create volunteer throws AlreadyRegisteredException if already assigned")
        void create_alreadyRegistered() {
            EventVolunteerRequest req = new EventVolunteerRequest();
            req.setEventId(100L);
            req.setUserId(volunteerUser.getId());

            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(volunteerRepo.existsByEventIdAndUserId(100L, volunteerUser.getId())).thenReturn(true);

            assertThatThrownBy(() -> volunteerService.create(req, community))
                    .isInstanceOf(AlreadyRegisteredException.class);
        }

        @Test
        @DisplayName("checkIn marks volunteer as checked in")
        void checkIn_success() {
            when(volunteerRepo.findById(20L)).thenReturn(Optional.of(volunteer));
            when(volunteerRepo.save(any(EventVolunteer.class))).thenAnswer(inv -> inv.getArgument(0));

            EventVolunteerResponse res = volunteerService.checkIn(20L);

            assertThat(res).isNotNull();
        }
    }
}
