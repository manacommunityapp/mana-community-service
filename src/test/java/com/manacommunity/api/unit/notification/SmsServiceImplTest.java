package com.manacommunity.api.unit.notification;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.exception.NotificationAlreadyProcessedException;
import com.manacommunity.api.exception.SmsRateLimitException;
import com.manacommunity.api.notification.config.SmsProperties;
import com.manacommunity.api.notification.dto.SendSmsRequest;
import com.manacommunity.api.notification.dto.SmsMessageResponse;
import com.manacommunity.api.notification.entity.SmsMessage;
import com.manacommunity.api.notification.entity.UserSmsPreference;
import com.manacommunity.api.notification.enums.*;
import com.manacommunity.api.notification.provider.SmsSendResponse;
import com.manacommunity.api.notification.provider.SmsProvider;
import com.manacommunity.api.notification.repository.SmsCostRecordRepository;
import com.manacommunity.api.notification.repository.SmsMessageRepository;
import com.manacommunity.api.notification.repository.UserSmsPreferenceRepository;
import com.manacommunity.api.notification.service.PhoneNumberService;
import com.manacommunity.api.notification.service.SmsRateLimiter;
import com.manacommunity.api.notification.service.SmsServiceImpl;
import com.manacommunity.api.notification.template.SmsTemplateResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsServiceImplTest {

    @Mock private SmsMessageRepository smsMessageRepo;
    @Mock private SmsCostRecordRepository costRecordRepo;
    @Mock private UserSmsPreferenceRepository preferenceRepo;
    @Mock private SmsTemplateResolver templateResolver;
    @Mock private SmsProvider smsProvider;

    private SmsProperties smsProperties;
    private PhoneNumberService phoneNumberService;
    private SmsRateLimiter rateLimiter;
    private SmsServiceImpl smsService;

    @BeforeEach
    void setUp() {
        smsProperties = new SmsProperties();
        smsProperties.setEnabled(true);
        smsProperties.setDefaultCountryCode("+91");

        phoneNumberService = new PhoneNumberService(smsProperties);
        rateLimiter = new SmsRateLimiter(smsProperties);

        smsService = new SmsServiceImpl(
                smsMessageRepo, costRecordRepo, preferenceRepo,
                templateResolver, phoneNumberService, rateLimiter,
                smsProvider, smsProperties);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private SmsMessage queuedMessage(Long id) {
        return SmsMessage.builder()
                .id(id)
                .phoneNumber("+919876543210")
                .renderedBody("Hello")
                .status(SmsStatus.QUEUED)
                .retryCount(0)
                .maxRetries(3)
                .build();
    }

    private SendSmsRequest basicRequest() {
        return SendSmsRequest.builder()
                .phoneNumber("9876543210")
                .templateCode("REGISTRATION_CONFIRMED")
                .messageType(MessageType.TRANSACTIONAL)
                .language(SmsLanguage.EN)
                .priority(SmsPriority.NORMAL)
                .variables(Map.of("name", "Arun"))
                .build();
    }

    // ── send() ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("send()")
    class SendTests {

        @Test
        @DisplayName("returns null immediately when SMS is disabled")
        void disabledReturnsNull() {
            smsProperties.setEnabled(false);
            Long result = smsService.send(basicRequest());
            assertThat(result).isNull();
            verifyNoInteractions(smsMessageRepo, templateResolver, smsProvider);
        }

        @Test
        @DisplayName("persists QUEUED message and triggers async dispatch")
        void persistsAndDispatches() {
            when(templateResolver.resolve(anyString(), any(), any())).thenReturn("Hello Arun");
            when(smsProvider.getProviderName()).thenReturn("MOCK");

            SmsMessage saved = queuedMessage(1L);
            when(smsMessageRepo.save(any())).thenReturn(saved);
            when(smsMessageRepo.findById(1L)).thenReturn(Optional.of(saved));
            when(smsProvider.send(any())).thenReturn(SmsSendResponse.builder()
                    .success(true).providerMessageId("MOCK-001").rawResponse("ok").build());

            Long id = smsService.send(basicRequest());

            ArgumentCaptor<SmsMessage> captor = ArgumentCaptor.forClass(SmsMessage.class);
            verify(smsMessageRepo, atLeastOnce()).save(captor.capture());
            SmsMessage firstSave = captor.getAllValues().get(0);
            assertThat(firstSave.getStatus()).isEqualTo(SmsStatus.QUEUED);
            assertThat(firstSave.getPhoneNumber()).isEqualTo("+919876543210");
        }

        @Test
        @DisplayName("normalises 10-digit phone to E.164 before persisting")
        void normalisesPhone() {
            when(templateResolver.resolve(anyString(), any(), any())).thenReturn("Hello");
            when(smsProvider.getProviderName()).thenReturn("MOCK");

            SmsMessage saved = queuedMessage(2L);
            when(smsMessageRepo.save(any())).thenReturn(saved);
            when(smsMessageRepo.findById(2L)).thenReturn(Optional.of(saved));
            when(smsProvider.send(any())).thenReturn(SmsSendResponse.builder()
                    .success(true).providerMessageId("M1").rawResponse("ok").build());

            smsService.send(SendSmsRequest.builder()
                    .phoneNumber("9876543210")
                    .templateCode("T1")
                    .messageType(MessageType.TRANSACTIONAL)
                    .language(SmsLanguage.EN)
                    .priority(SmsPriority.NORMAL)
                    .build());

            ArgumentCaptor<SmsMessage> captor = ArgumentCaptor.forClass(SmsMessage.class);
            verify(smsMessageRepo, atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues().get(0).getPhoneNumber()).isEqualTo("+919876543210");
        }

        @Test
        @DisplayName("returns null when user has opted out of this notification type")
        void userOptOutReturnsNull() {
            UserSmsPreference pref = UserSmsPreference.builder()
                    .userId(99L).notificationType("REGISTRATION_CONFIRMED").smsEnabled(false).build();
            when(preferenceRepo.findByUserIdAndNotificationType(99L, "REGISTRATION_CONFIRMED"))
                    .thenReturn(Optional.of(pref));

            Long result = smsService.send(SendSmsRequest.builder()
                    .phoneNumber("9876543210")
                    .templateCode("REGISTRATION_CONFIRMED")
                    .userId(99L)
                    .messageType(MessageType.TRANSACTIONAL)
                    .language(SmsLanguage.EN)
                    .priority(SmsPriority.NORMAL)
                    .build());

            assertThat(result).isNull();
            verify(smsMessageRepo, never()).save(any());
        }

        @Test
        @DisplayName("throws NotificationAlreadyProcessedException on duplicate idempotency key")
        void duplicateIdempotencyKeyThrows() {
            when(smsMessageRepo.findByIdempotencyKey("key-123"))
                    .thenReturn(Optional.of(new SmsMessage()));

            assertThatThrownBy(() -> smsService.send(SendSmsRequest.builder()
                    .phoneNumber("9876543210")
                    .templateCode("TEST")
                    .idempotencyKey("key-123")
                    .messageType(MessageType.TRANSACTIONAL)
                    .language(SmsLanguage.EN)
                    .priority(SmsPriority.NORMAL)
                    .build()))
                    .isInstanceOf(NotificationAlreadyProcessedException.class);
        }

        @Test
        @DisplayName("throws SmsRateLimitException when per-phone limit exceeded")
        void phoneLimitExceeded() {
            when(templateResolver.resolve(anyString(), any(), any())).thenReturn("Body");
            when(smsProvider.getProviderName()).thenReturn("MOCK");
            SmsMessage saved = queuedMessage(10L);
            when(smsMessageRepo.save(any())).thenReturn(saved);
            when(smsMessageRepo.findById(10L)).thenReturn(Optional.of(saved));
            when(smsProvider.send(any())).thenReturn(SmsSendResponse.builder()
                    .success(true).providerMessageId("M").rawResponse("ok").build());

            // Saturate the per-phone limit (default 2/min)
            smsService.send(basicRequest());
            smsService.send(basicRequest());

            assertThatThrownBy(() -> smsService.send(basicRequest()))
                    .isInstanceOf(SmsRateLimitException.class);
        }
    }

    // ── doSend() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("doSend()")
    class DoSendTests {

        @Test
        @DisplayName("success path: sets SENT, records providerMessageId and cost")
        void successSetsStatusAndRecordsCost() {
            SmsMessage msg = queuedMessage(5L);
            when(smsMessageRepo.findById(5L)).thenReturn(Optional.of(msg));
            when(smsMessageRepo.save(any())).thenReturn(msg);
            when(smsProvider.send(any())).thenReturn(SmsSendResponse.builder()
                    .success(true).providerMessageId("SID-001").rawResponse("{}").build());

            smsService.doSend(5L);

            assertThat(msg.getStatus()).isEqualTo(SmsStatus.SENT);
            assertThat(msg.getProviderMessageId()).isEqualTo("SID-001");
            assertThat(msg.getSentAt()).isNotNull();
            verify(costRecordRepo).save(any());
        }

        @Test
        @DisplayName("first failure: status=FAILED, retryCount incremented, nextRetryAt set")
        void firstFailureSetsRetrying() {
            SmsMessage msg = queuedMessage(6L);
            when(smsMessageRepo.findById(6L)).thenReturn(Optional.of(msg));
            when(smsMessageRepo.save(any())).thenReturn(msg);
            when(smsProvider.send(any())).thenReturn(SmsSendResponse.builder()
                    .success(false).errorCode("30008").errorMessage("Unknown").build());

            smsService.doSend(6L);

            assertThat(msg.getStatus()).isEqualTo(SmsStatus.FAILED);
            assertThat(msg.getRetryCount()).isEqualTo(1);
            assertThat(msg.getNextRetryAt()).isNotNull();
            verify(costRecordRepo, never()).save(any());
        }

        @Test
        @DisplayName("failure at maxRetries: status=DLQ, failedAt set")
        void failureAtMaxRetriesGoesToDlq() {
            SmsMessage msg = SmsMessage.builder()
                    .id(7L).phoneNumber("+919876543210").renderedBody("Hello")
                    .status(SmsStatus.QUEUED).retryCount(2).maxRetries(3).build();
            when(smsMessageRepo.findById(7L)).thenReturn(Optional.of(msg));
            when(smsMessageRepo.save(any())).thenReturn(msg);
            when(smsProvider.send(any())).thenReturn(SmsSendResponse.builder()
                    .success(false).errorCode("E").errorMessage("timeout").build());

            smsService.doSend(7L);

            assertThat(msg.getStatus()).isEqualTo(SmsStatus.DLQ);
            assertThat(msg.getRetryCount()).isEqualTo(3);
            assertThat(msg.getFailedAt()).isNotNull();
        }

        @Test
        @DisplayName("skips message already in SENT state")
        void skipsSentMessage() {
            SmsMessage msg = SmsMessage.builder()
                    .id(8L).phoneNumber("+919876543210")
                    .status(SmsStatus.SENT).build();
            when(smsMessageRepo.findById(8L)).thenReturn(Optional.of(msg));

            smsService.doSend(8L);

            verify(smsProvider, never()).send(any());
            // only one save would be for status transition to SENDING, which is skipped
            verify(smsMessageRepo, never()).save(any());
        }

        @Test
        @DisplayName("skips message already in CANCELLED state")
        void skipsCancelledMessage() {
            SmsMessage msg = SmsMessage.builder()
                    .id(9L).phoneNumber("+919876543210")
                    .status(SmsStatus.CANCELLED).build();
            when(smsMessageRepo.findById(9L)).thenReturn(Optional.of(msg));

            smsService.doSend(9L);

            verify(smsProvider, never()).send(any());
        }

        @Test
        @DisplayName("throws ManaCommunityException when message not found")
        void throwsWhenNotFound() {
            when(smsMessageRepo.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> smsService.doSend(999L))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("backoff doubles with multiplier: attempt 2 → 120s, capped at maxDelay")
        void backoffComputedCorrectly() {
            // Retry config: initial=30s, multiplier=4.0, max=600s
            // attempt 1: 30 * 4^0 = 30s
            // attempt 2: 30 * 4^1 = 120s
            // attempt 3+: capped at 600s
            SmsMessage msg1 = SmsMessage.builder()
                    .id(11L).phoneNumber("+919876543210").renderedBody("Hi")
                    .status(SmsStatus.QUEUED).retryCount(0).maxRetries(10).build();
            when(smsMessageRepo.findById(11L)).thenReturn(Optional.of(msg1));
            when(smsMessageRepo.save(any())).thenReturn(msg1);
            when(smsProvider.send(any())).thenReturn(SmsSendResponse.builder()
                    .success(false).errorCode("E").errorMessage("err").build());

            smsService.doSend(11L);
            assertThat(msg1.getNextRetryAt()).isAfterOrEqualTo(
                    java.time.LocalDateTime.now().plusSeconds(28));

            SmsMessage msg2 = SmsMessage.builder()
                    .id(12L).phoneNumber("+919876543210").renderedBody("Hi")
                    .status(SmsStatus.QUEUED).retryCount(1).maxRetries(10).build();
            when(smsMessageRepo.findById(12L)).thenReturn(Optional.of(msg2));

            smsService.doSend(12L);
            assertThat(msg2.getNextRetryAt()).isAfterOrEqualTo(
                    java.time.LocalDateTime.now().plusSeconds(118));
        }
    }

    // ── retry() ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("retry()")
    class RetryTests {

        @Test
        @DisplayName("FAILED → QUEUED, dispatches async")
        void failedToQueued() {
            SmsMessage msg = SmsMessage.builder()
                    .id(20L).phoneNumber("+919876543210").renderedBody("Hi")
                    .status(SmsStatus.FAILED).retryCount(1).maxRetries(3).build();
            when(smsMessageRepo.findById(20L)).thenReturn(Optional.of(msg));
            when(smsMessageRepo.save(any())).thenReturn(msg);
            when(smsMessageRepo.findById(20L)).thenReturn(Optional.of(msg));
            when(smsProvider.send(any())).thenReturn(SmsSendResponse.builder()
                    .success(true).providerMessageId("M").rawResponse("ok").build());

            SmsMessageResponse resp = smsService.retry(20L);

            verify(smsMessageRepo, atLeastOnce()).save(any());
            assertThat(msg.getNextRetryAt()).isNull();
        }

        @Test
        @DisplayName("DLQ → QUEUED, dispatches")
        void dlqToQueued() {
            SmsMessage msg = SmsMessage.builder()
                    .id(21L).phoneNumber("+919876543210").renderedBody("Hi")
                    .status(SmsStatus.DLQ).retryCount(3).maxRetries(3).build();
            when(smsMessageRepo.findById(21L)).thenReturn(Optional.of(msg));
            when(smsMessageRepo.save(any())).thenReturn(msg);
            when(smsProvider.send(any())).thenReturn(SmsSendResponse.builder()
                    .success(true).providerMessageId("M").rawResponse("ok").build());

            smsService.retry(21L);

            assertThat(msg.getStatus()).isNotEqualTo(SmsStatus.DLQ);
        }

        @Test
        @DisplayName("throws ManaCommunityException when status is SENT (not retryable)")
        void sentNotRetryable() {
            SmsMessage msg = SmsMessage.builder()
                    .id(22L).phoneNumber("+919876543210").status(SmsStatus.SENT).build();
            when(smsMessageRepo.findById(22L)).thenReturn(Optional.of(msg));

            assertThatThrownBy(() -> smsService.retry(22L))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("Cannot retry");
        }

        @Test
        @DisplayName("throws ManaCommunityException when message not found")
        void notFoundThrows() {
            when(smsMessageRepo.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> smsService.retry(99L))
                    .isInstanceOf(ManaCommunityException.class);
        }
    }

    // ── cancel() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancel()")
    class CancelTests {

        @Test
        @DisplayName("QUEUED → CANCELLED")
        void queuedToCancelled() {
            SmsMessage msg = SmsMessage.builder()
                    .id(30L).phoneNumber("+919876543210").status(SmsStatus.QUEUED).build();
            when(smsMessageRepo.findById(30L)).thenReturn(Optional.of(msg));
            when(smsMessageRepo.save(any())).thenReturn(msg);

            smsService.cancel(30L);

            assertThat(msg.getStatus()).isEqualTo(SmsStatus.CANCELLED);
        }

        @Test
        @DisplayName("FAILED → CANCELLED")
        void failedToCancelled() {
            SmsMessage msg = SmsMessage.builder()
                    .id(31L).phoneNumber("+919876543210").status(SmsStatus.FAILED).build();
            when(smsMessageRepo.findById(31L)).thenReturn(Optional.of(msg));
            when(smsMessageRepo.save(any())).thenReturn(msg);

            smsService.cancel(31L);

            assertThat(msg.getStatus()).isEqualTo(SmsStatus.CANCELLED);
        }

        @Test
        @DisplayName("throws ManaCommunityException when message is already SENT")
        void sentCannotBeCancelled() {
            SmsMessage msg = SmsMessage.builder()
                    .id(32L).phoneNumber("+919876543210").status(SmsStatus.SENT).build();
            when(smsMessageRepo.findById(32L)).thenReturn(Optional.of(msg));

            assertThatThrownBy(() -> smsService.cancel(32L))
                    .isInstanceOf(ManaCommunityException.class)
                    .hasMessageContaining("Cannot cancel");
        }

        @Test
        @DisplayName("throws ManaCommunityException when message is DELIVERED")
        void deliveredCannotBeCancelled() {
            SmsMessage msg = SmsMessage.builder()
                    .id(33L).phoneNumber("+919876543210").status(SmsStatus.DELIVERED).build();
            when(smsMessageRepo.findById(33L)).thenReturn(Optional.of(msg));

            assertThatThrownBy(() -> smsService.cancel(33L))
                    .isInstanceOf(ManaCommunityException.class);
        }
    }
}
