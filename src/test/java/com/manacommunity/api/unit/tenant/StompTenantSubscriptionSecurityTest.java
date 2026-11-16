package com.manacommunity.api.unit.tenant;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.ConversationParticipantRepository;
import com.manacommunity.api.security.CommunityMembershipService;
import com.manacommunity.api.security.StompSubscriptionInterceptor;
import com.manacommunity.api.socialcircle.repository.KittyPartyGameRoomRepository;
import com.manacommunity.api.sports.scheduler.SportsTournamentConfig;
import com.manacommunity.api.sports.repository.SportsTournamentConfigRepository;
import com.manacommunity.api.sports.repository.SportsTournamentMatchRepository;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("STOMP Tenant Topic Subscription Security Test")
class StompTenantSubscriptionSecurityTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private ConversationParticipantRepository participantRepository;

    @Mock
    private SportsTournamentConfigRepository configRepository;

    @Mock
    private SportsTournamentMatchRepository matchRepository;

    @Mock
    private KittyPartyGameRoomRepository gameRoomRepository;

    @Mock
    private CommunityMembershipService membershipService;

    @Mock
    private MessageChannel channel;

    private StompSubscriptionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new StompSubscriptionInterceptor(
                userRepository,
                participantRepository,
                configRepository,
                matchRepository,
                gameRoomRepository,
                membershipService
        );
    }

    private Message<?> createSubscribeMessage(Long userId, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        if (userId != null) {
            accessor.setUser(new UsernamePasswordAuthenticationToken(String.valueOf(userId), null));
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("Resident subscribing to own community topic is permitted")
    void residentSubscribingToOwnCommunityTopicSucceeds() {
        when(membershipService.isUserAuthorizedForCommunity(eq(10L), eq(100L))).thenReturn(true);

        Message<?> msg = createSubscribeMessage(10L, "/topic/community/100/events");
        Message<?> result = interceptor.preSend(msg, channel);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Resident subscribing to cross-tenant community topic is dropped (rejected)")
    void residentSubscribingToCrossTenantCommunityTopicIsDropped() {
        when(membershipService.isUserAuthorizedForCommunity(eq(10L), eq(200L))).thenReturn(false);

        Message<?> msg = createSubscribeMessage(10L, "/topic/community/200/events");
        Message<?> result = interceptor.preSend(msg, channel);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Cross-tenant emergency broadcast subscription is dropped")
    void crossTenantEmergencyTopicSubscriptionIsDropped() {
        when(membershipService.isUserAuthorizedForCommunity(eq(10L), eq(999L))).thenReturn(false);

        Message<?> msg = createSubscribeMessage(10L, "/topic/emergency/999");
        Message<?> result = interceptor.preSend(msg, channel);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Auction subscription for own community is permitted")
    void auctionSubscriptionForOwnCommunitySucceeds() {
        Community comm = Community.builder().name("Palm Grove").build();
        comm.setId(100L);
        SportsTournamentConfig config = SportsTournamentConfig.builder().community(comm).build();
        config.setId(45L);

        when(configRepository.findById(45L)).thenReturn(Optional.of(config));
        when(membershipService.isUserAuthorizedForCommunity(eq(10L), eq(100L))).thenReturn(true);

        Message<?> msg = createSubscribeMessage(10L, "/topic/auction/45");
        Message<?> result = interceptor.preSend(msg, channel);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Auction subscription for other community is dropped")
    void auctionSubscriptionForOtherCommunityIsDropped() {
        Community comm = Community.builder().name("Other Estate").build();
        comm.setId(200L);
        SportsTournamentConfig config = SportsTournamentConfig.builder().community(comm).build();
        config.setId(45L);

        when(configRepository.findById(45L)).thenReturn(Optional.of(config));
        when(membershipService.isUserAuthorizedForCommunity(eq(10L), eq(200L))).thenReturn(false);

        Message<?> msg = createSubscribeMessage(10L, "/topic/auction/45");
        Message<?> result = interceptor.preSend(msg, channel);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Unauthenticated subscribe is rejected")
    void unauthenticatedSubscribeIsRejected() {
        Message<?> msg = createSubscribeMessage(null, "/topic/community/100/events");
        Message<?> result = interceptor.preSend(msg, channel);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Unmapped topic destination under /topic/ is rejected by default")
    void unmappedTopicDestinationIsRejectedByDefault() {
        Message<?> msg = createSubscribeMessage(10L, "/topic/secret-internal-backdoor");
        Message<?> result = interceptor.preSend(msg, channel);

        assertThat(result).isNull();
    }
}
