package com.manacommunity.api.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.dto.CommunityWhoToCallHistoryResponse;
import com.manacommunity.api.dto.CommunityWhoToCallRequest;
import com.manacommunity.api.dto.CommunityWhoToCallResponse;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.CommunityWhoToCall;
import com.manacommunity.api.model.CommunityWhoToCallHistory;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.CommunityWhoToCallHistoryRepository;
import com.manacommunity.api.repository.CommunityWhoToCallRepository;
import com.manacommunity.api.service.impl.CommunityWhoToCallServiceImpl;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityWhoToCallServiceImpl - Unit Tests")
class CommunityWhoToCallServiceTest {

    @Mock private CommunityWhoToCallRepository whoToCallRepository;
    @Mock private CommunityWhoToCallHistoryRepository historyRepository;
    @Mock private CommunityRepository communityRepository;
    @Mock private AppUserRepository userRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CommunityWhoToCallServiceImpl service;

    private Community community;
    private AppUser leaderUser;

    @BeforeEach
    void setUp() {
        community = Community.builder()
                .id(1L)
                .name("Green Valley Society")
                .inviteCode("APT-TOWER-A-2024")
                .build();

        leaderUser = AppUser.builder()
                .id(10L)
                .fullName("Priya Sharma")
                .email("priya@example.com")
                .phone("+91 98765 43210")
                .build();
    }

    @Test
    @DisplayName("create: saves contact, sets defaults, and writes history log entry")
    void create_success() {
        CommunityWhoToCallRequest req = CommunityWhoToCallRequest.builder()
                .department("Lift Emergency")
                .contactPerson("Priya Sharma")
                .userId(10L)
                .phoneNumber("+91 98765 43210")
                .isEmergency(true)
                .availability("24/7 Available")
                .locationOrDesk("Main Gate Security")
                .build();

        when(communityRepository.findById(1L)).thenReturn(Optional.of(community));
        when(userRepository.findById(10L)).thenReturn(Optional.of(leaderUser));
        when(whoToCallRepository.save(any(CommunityWhoToCall.class))).thenAnswer(inv -> {
            CommunityWhoToCall c = inv.getArgument(0);
            c.setId(101L);
            return c;
        });

        CommunityWhoToCallResponse resp = service.create(1L, 1L, "Admin User", req);

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(101L);
        assertThat(resp.getDepartment()).isEqualTo("Lift Emergency");
        assertThat(resp.getContactPerson()).isEqualTo("Priya Sharma");
        assertThat(resp.getIsEmergency()).isTrue();
        assertThat(resp.getUserFullName()).isEqualTo("Priya Sharma");

        verify(historyRepository).save(argThat(h ->
                h.getWhoToCallId().equals(101L) &&
                h.getAction().equals("CREATED") &&
                h.getChangedByName().equals("Admin User")
        ));
    }

    @Test
    @DisplayName("update: modifies fields and records UPDATED history with change summary")
    void update_success() {
        CommunityWhoToCall existing = CommunityWhoToCall.builder()
                .id(101L)
                .community(community)
                .department("Maintenance & Repairs")
                .contactPerson("Rahul Verma")
                .phoneNumber("9876543210")
                .availability("9 AM - 6 PM")
                .isActive(true)
                .build();

        CommunityWhoToCallRequest req = CommunityWhoToCallRequest.builder()
                .department("Maintenance & Repairs")
                .contactPerson("Rahul Verma")
                .phoneNumber("9876599999") // Changed phone
                .availability("24/7 Available") // Changed availability
                .build();

        when(whoToCallRepository.findById(101L)).thenReturn(Optional.of(existing));
        when(whoToCallRepository.save(any(CommunityWhoToCall.class))).thenAnswer(inv -> inv.getArgument(0));

        CommunityWhoToCallResponse resp = service.update(101L, 1L, "Admin User", req);

        assertThat(resp.getPhoneNumber()).isEqualTo("9876599999");
        assertThat(resp.getAvailability()).isEqualTo("24/7 Available");

        verify(historyRepository).save(argThat(h ->
                h.getWhoToCallId().equals(101L) &&
                h.getAction().equals("UPDATED") &&
                h.getChangeSummary().contains("Phone:")
        ));
    }

    @Test
    @DisplayName("toggleStatus / delete: deactivates and records DEACTIVATED history")
    void delete_success() {
        CommunityWhoToCall existing = CommunityWhoToCall.builder()
                .id(101L)
                .community(community)
                .department("Security")
                .contactPerson("Security Officer")
                .phoneNumber("9876543210")
                .isActive(true)
                .build();

        when(whoToCallRepository.findById(101L)).thenReturn(Optional.of(existing));

        service.delete(101L, 1L, "Admin User");

        assertThat(existing.getIsActive()).isFalse();
        verify(historyRepository).save(argThat(h -> h.getAction().equals("DEACTIVATED")));
    }

    @Test
    @DisplayName("getHistory: retrieves history list for contact in reverse chronological order")
    void getHistory_success() {
        CommunityWhoToCallHistory h1 = CommunityWhoToCallHistory.builder()
                .id(1L)
                .whoToCallId(101L)
                .communityId(1L)
                .action("CREATED")
                .department("Plumbing")
                .contactPerson("Plumber")
                .phoneNumber("9876543210")
                .build();

        when(historyRepository.findByWhoToCallIdOrderByCreatedAtDesc(101L)).thenReturn(List.of(h1));

        List<CommunityWhoToCallHistoryResponse> history = service.getHistory(101L);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getAction()).isEqualTo("CREATED");
    }
}
