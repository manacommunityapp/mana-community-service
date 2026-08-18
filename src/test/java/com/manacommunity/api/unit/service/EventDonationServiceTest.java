package com.manacommunity.api.unit.service;

import com.manacommunity.api.events.dto.EventDonationRequest;
import com.manacommunity.api.events.dto.EventDonationResponse;
import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.entity.EventDonation;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.EventDonationRepository;
import com.manacommunity.api.events.service.EventDonationService;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.support.TestDataBuilder;
import com.manacommunity.api.user.model.AppUser;
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
@DisplayName("EventDonationService")
class EventDonationServiceTest {

    @Mock EventDonationRepository donationRepo;
    @Mock CommunityEventRepository eventRepo;

    @InjectMocks EventDonationService donationService;

    private Community community;
    private AppUser adminUser;
    private CommunityEvent event;
    private EventDonation donation;

    @BeforeEach
    void setUp() {
        community = TestDataBuilder.community(1L, "INVITE123");
        adminUser = TestDataBuilder.adminUser();
        event = TestDataBuilder.communityEvent(100L, community, adminUser);

        donation = EventDonation.builder()
                .id(10L)
                .event(event)
                .donorName("Jane Smith")
                .donorEmail("jane@example.com")
                .donorPhone("9876543210")
                .amount(500.0)
                .paymentMethod(EventDonation.PaymentMethod.UPI)
                .community(community)
                .recordedBy(adminUser)
                .build();
    }

    @Nested
    @DisplayName("Query Donations")
    class QueryDonations {

        @Test
        @DisplayName("getByEvent returns donations for specific event")
        void getByEvent_success() {
            when(donationRepo.findByEventIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(donation));

            List<EventDonationResponse> list = donationService.getByEvent(100L);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getDonorName()).isEqualTo("Jane Smith");
            assertThat(list.get(0).getAmount()).isEqualTo(500.0);
        }

        @Test
        @DisplayName("getByCommunity returns donations for specific community")
        void getByCommunity_success() {
            when(donationRepo.findByCommunityIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(donation));

            List<EventDonationResponse> list = donationService.getByCommunity(1L);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getId()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("Create, Update and Delete Donation")
    class ModifyDonation {

        @Test
        @DisplayName("create donation saves entity and returns response")
        void create_success() {
            EventDonationRequest req = new EventDonationRequest();
            req.setEventId(100L);
            req.setDonorName("Jane Smith");
            req.setAmount(500.0);
            req.setPaymentMethod("UPI");

            when(eventRepo.findById(100L)).thenReturn(Optional.of(event));
            when(donationRepo.save(any(EventDonation.class))).thenReturn(donation);

            EventDonationResponse res = donationService.create(req, adminUser, community);

            assertThat(res).isNotNull();
            assertThat(res.getDonorName()).isEqualTo("Jane Smith");
            verify(donationRepo).save(any(EventDonation.class));
        }

        @Test
        @DisplayName("create donation throws ResourceNotFoundException if event missing")
        void create_missingEvent() {
            EventDonationRequest req = new EventDonationRequest();
            req.setEventId(999L);
            when(eventRepo.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> donationService.create(req, adminUser, community))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("delete removes existing donation")
        void delete_success() {
            when(donationRepo.findById(10L)).thenReturn(Optional.of(donation));

            donationService.delete(10L);

            verify(donationRepo).delete(donation);
        }
    }
}
