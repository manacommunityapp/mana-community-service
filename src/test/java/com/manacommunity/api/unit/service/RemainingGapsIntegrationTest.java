package com.manacommunity.api.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.events.dto.EventAuctionItemResponse;
import com.manacommunity.api.events.service.EventAuctionCacheService;
import com.manacommunity.api.events.service.EventReportService;
import com.manacommunity.api.events.sqs.ReportJobSqsHandler;
import com.manacommunity.api.service.ExpoPushService;
import com.manacommunity.api.sports.sqs.AuctionNotificationSqsHandler;
import com.manacommunity.api.sports.sqs.AuctionNotificationSqsHandler.AuctionPushPayload;
import com.manacommunity.platform.sqs.SqsEventEnvelope;
import com.manacommunity.platform.sqs.SqsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Remaining Redis & SQS Gap Fill Tests")
class RemainingGapsIntegrationTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private EventReportService eventReportService;
    @Mock private ExpoPushService expoPushService;

    private ObjectMapper objectMapper;
    private SqsProperties sqsProperties;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sqsProperties = new SqsProperties();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("EventAuctionCacheService caches item bid snapshot with 30m TTL and invalidates cleanly")
    void eventAuctionCacheHitMissInvalidate() throws Exception {
        EventAuctionCacheService cacheService = new EventAuctionCacheService(redisTemplate, objectMapper);
        EventAuctionItemResponse item = EventAuctionItemResponse.builder()
                .id(11L).name("Silver Lamp").currentBid(new BigDecimal("2500")).bidCount(3).leaderName("Ravi").build();

        String key = "event:auction:comm:5:item:11:latest-bid";
        when(valueOperations.get(key)).thenReturn(null);

        EventAuctionItemResponse loaded = cacheService.getItem(5L, 11L, () -> item);
        assertThat(loaded.getName()).isEqualTo("Silver Lamp");
        verify(valueOperations).set(eq(key), anyString(), eq(Duration.ofMinutes(30)));

        when(valueOperations.get(key)).thenReturn(objectMapper.writeValueAsString(item));
        EventAuctionItemResponse hit = cacheService.getItem(5L, 11L, () -> {
            throw new AssertionError("Should not invoke DB loader on hit");
        });
        assertThat(hit.getLeaderName()).isEqualTo("Ravi");

        cacheService.invalidate(5L, 11L);
        verify(redisTemplate).delete(key);
    }

    @Test
    @DisplayName("AuctionNotificationSqsHandler sends ITEM_WON push for closed event auction items")
    void auctionNotificationItemWon() throws Exception {
        AuctionNotificationSqsHandler handler = new AuctionNotificationSqsHandler(expoPushService, sqsProperties, objectMapper);
        AuctionPushPayload payload = new AuctionPushPayload("ITEM_WON", 11L, "Silver Lamp", "Ravi", 2500L, List.of(42L));

        handler.handle(new SqsEventEnvelope("e-won", "ITEM_WON", handler.queueName(),
                Instant.now(), objectMapper.writeValueAsString(payload)));

        verify(expoPushService).sendToUsers(eq(List.of(42L)), contains("Auction Won"),
                contains("Silver Lamp"), anyMap(), eq("auctions"));
    }

    @Test
    @DisplayName("ReportJobSqsHandler generates CSV, stores in Redis with 1h TTL, and notifies user")
    void reportJobGeneratesCachesAndNotifies() throws Exception {
        byte[] csv = "name,status\nArun,CONFIRMED\n".getBytes(StandardCharsets.UTF_8);
        when(eventReportService.generateRegistrationsCsv(9L, "all")).thenReturn(csv);

        ReportJobSqsHandler handler = new ReportJobSqsHandler(
                eventReportService, expoPushService, sqsProperties, objectMapper, redisTemplate);

        String payloadJson = objectMapper.writeValueAsString(
                new ReportJobSqsHandler.ReportJobPayload("job-123", 9L, "all", 77L));
        handler.handle(new SqsEventEnvelope("e-rep", "EVENT_REGISTRATIONS_CSV", handler.queueName(),
                Instant.now(), payloadJson));

        verify(valueOperations).set(eq("report:job:job-123"), contains("Arun,CONFIRMED"), eq(Duration.ofHours(1)));
        verify(expoPushService).sendToUser(eq(77L), contains("Report Export Ready"), anyString(), anyMap(), eq("events"));

        when(valueOperations.get("report:job:job-123")).thenReturn("name,status\nArun,CONFIRMED\n");
        Optional<byte[]> cached = handler.getCachedReport("job-123");
        assertThat(cached).isPresent();
        assertThat(new String(cached.get(), StandardCharsets.UTF_8)).contains("Arun,CONFIRMED");
    }
}
