package com.manacommunity.api.unit.notification;

import com.manacommunity.api.notification.entity.SmsMessage;
import com.manacommunity.api.notification.enums.SmsStatus;
import com.manacommunity.api.notification.repository.SmsMessageRepository;
import com.manacommunity.api.notification.scheduler.SmsRetryScheduler;
import com.manacommunity.api.notification.service.SmsRateLimiter;
import com.manacommunity.api.notification.service.SmsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsRetrySchedulerTest {

    @Mock private SmsMessageRepository smsMessageRepo;
    @Mock private SmsServiceImpl smsService;
    @Mock private SmsRateLimiter rateLimiter;

    private SmsRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SmsRetryScheduler(smsMessageRepo, smsService, rateLimiter);
    }

    private SmsMessage failedMessage(Long id) {
        return SmsMessage.builder()
                .id(id).phoneNumber("+919876543210").renderedBody("Hello")
                .status(SmsStatus.FAILED).retryCount(1).maxRetries(3).build();
    }

    @Test
    @DisplayName("processRetries sets RETRYING status, saves, and dispatches for eligible messages")
    void processRetriesEligibleMessages() {
        SmsMessage m1 = failedMessage(1L);
        SmsMessage m2 = failedMessage(2L);
        when(smsMessageRepo.findMessagesForRetry(any())).thenReturn(List.of(m1, m2));
        when(smsMessageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.processRetries();

        ArgumentCaptor<SmsMessage> captor = ArgumentCaptor.forClass(SmsMessage.class);
        verify(smsMessageRepo, times(2)).save(captor.capture());
        captor.getAllValues().forEach(msg ->
                assertThat(msg.getStatus()).isEqualTo(SmsStatus.RETRYING));

        verify(smsService).dispatchAsync(1L);
        verify(smsService).dispatchAsync(2L);
    }

    @Test
    @DisplayName("processRetries with empty list: no saves, no dispatches")
    void processRetriesEmptyList() {
        when(smsMessageRepo.findMessagesForRetry(any())).thenReturn(List.of());

        scheduler.processRetries();

        verify(smsMessageRepo, never()).save(any());
        verify(smsService, never()).dispatchAsync(any());
    }

    @Test
    @DisplayName("evictRateLimitBuckets delegates to rateLimiter without throwing")
    void evictBucketsCallsRateLimiter() {
        scheduler.evictRateLimitBuckets();
        verify(rateLimiter).evictStale();
    }
}
