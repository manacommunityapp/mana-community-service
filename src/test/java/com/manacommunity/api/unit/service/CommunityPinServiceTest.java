package com.manacommunity.api.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.pins.dto.CommunityPinDto;
import com.manacommunity.api.pins.dto.CreatePinRequest;
import com.manacommunity.api.pins.entity.CommunityPin;
import com.manacommunity.api.pins.repository.CommunityPinRepository;
import com.manacommunity.api.pins.service.CommunityPinService;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Community Pin Service Tests")
class CommunityPinServiceTest {

    @Mock
    private CommunityPinRepository pinRepository;

    @Mock
    private CommunityRepository communityRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private CommunityPinService pinService;

    private Community testCommunity;
    private AppUser testUser;
    private CommunityPin testPin;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pinService, "objectMapper", new ObjectMapper());

        testCommunity = new Community();
        testCommunity.setId(101L);
        testCommunity.setName("Green Meadows");

        testUser = AppUser.builder()
                .id(202L)
                .fullName("Ravi Kumar")
                .flatNo("A-402")
                .community(testCommunity)
                .role("USER")
                .build();

        testPin = CommunityPin.builder()
                .id(1L)
                .community(testCommunity)
                .author(testUser)
                .authorName("Ravi Kumar")
                .flatNumber("A-402")
                .category("ALERTS")
                .content("Water supply maintenance from 2 PM to 5 PM today.")
                .expiresAt(Instant.now().plus(Duration.ofHours(6)))
                .isClosed(false)
                .likesCount(3)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("getActivePins - falls back to DB when cache is empty")
    void testGetActivePins_DbFallback() {
        when(pinRepository.findByCommunityIdAndExpiresAtAfterOrderByCreatedAtDesc(eq(101L), any(Instant.class)))
                .thenReturn(List.of(testPin));

        List<CommunityPinDto> result = pinService.getActivePins(101L, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).contains("Water supply maintenance");
        assertThat(result.get(0).getCategory()).isEqualTo("ALERTS");
        assertThat(result.get(0).getAuthorName()).isEqualTo("Ravi Kumar");
        assertThat(result.get(0).getIsExpired()).isFalse();
    }

    @Test
    @DisplayName("createPin - computes correct expiration and persists pin")
    void testCreatePin_Success() {
        when(communityRepository.findById(101L)).thenReturn(Optional.of(testCommunity));
        when(pinRepository.save(any(CommunityPin.class))).thenAnswer(invocation -> {
            CommunityPin toSave = invocation.getArgument(0);
            toSave.setId(2L);
            return toSave;
        });

        CreatePinRequest req = CreatePinRequest.builder()
                .category("SPORTS")
                .content("Need 2 players for badminton Court 2 tonight at 8 PM!")
                .durationHours(12)
                .build();

        CommunityPinDto created = pinService.createPin(101L, testUser, req);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(2L);
        assertThat(created.getCategory()).isEqualTo("SPORTS");
        assertThat(created.getContent()).contains("badminton Court 2");
        assertThat(created.getExpiresAt()).isAfter(Instant.now().plus(Duration.ofHours(11)));
        verify(pinRepository, times(1)).save(any(CommunityPin.class));
    }

    @Test
    @DisplayName("closePin - marks pin as closed when author requests")
    void testClosePin_ByAuthor() {
        when(pinRepository.findById(1L)).thenReturn(Optional.of(testPin));
        when(pinRepository.save(any(CommunityPin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommunityPinDto closed = pinService.closePin(1L, 101L, testUser);

        assertThat(closed.getIsClosed()).isTrue();
        assertThat(closed.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("toggleLike - increments likes counter atomically")
    void testToggleLike() {
        when(pinRepository.findById(1L)).thenReturn(Optional.of(testPin));

        pinService.toggleLike(1L, 101L);

        verify(pinRepository, times(1)).incrementLikes(1L);
    }
}
