package com.manacommunity.api.notification.service;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.exception.NotificationAlreadyProcessedException;
import com.manacommunity.api.notification.config.SmsProperties;
import com.manacommunity.api.notification.dto.SendSmsRequest;
import com.manacommunity.api.notification.dto.SmsMessageResponse;
import com.manacommunity.api.notification.entity.SmsCostRecord;
import com.manacommunity.api.notification.entity.SmsMessage;
import com.manacommunity.api.notification.enums.SmsStatus;
import com.manacommunity.api.notification.provider.SmsProvider;
import com.manacommunity.api.notification.provider.SmsSendRequest;
import com.manacommunity.api.notification.provider.SmsSendResponse;
import com.manacommunity.api.notification.repository.SmsCostRecordRepository;
import com.manacommunity.api.notification.repository.SmsMessageRepository;
import com.manacommunity.api.notification.repository.UserSmsPreferenceRepository;
import com.manacommunity.api.notification.template.SmsTemplateResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsServiceImpl implements SmsService {

    private final SmsMessageRepository smsMessageRepo;
    private final SmsCostRecordRepository costRecordRepo;
    private final UserSmsPreferenceRepository preferenceRepo;
    private final SmsTemplateResolver templateResolver;
    private final PhoneNumberService phoneNumberService;
    private final SmsRateLimiter rateLimiter;
    private final SmsProvider smsProvider;
    private final SmsProperties smsProperties;

    @Override
    @Transactional
    public Long send(SendSmsRequest request) {
        if (!smsProperties.isEnabled()) {
            log.debug("SMS disabled — skipping send to {}", phoneNumberService.mask(request.getPhoneNumber()));
            return null;
        }

        String normalizedPhone = phoneNumberService.normalize(request.getPhoneNumber());

        // Respect user opt-out (if userId is known and a preference record says no)
        if (request.getUserId() != null) {
            boolean optedOut = preferenceRepo
                    .findByUserIdAndNotificationType(request.getUserId(), request.getTemplateCode())
                    .map(p -> !p.isSmsEnabled())
                    .orElse(false);
            if (optedOut) {
                log.info("User {} opted out of SMS type {}", request.getUserId(), request.getTemplateCode());
                return null;
            }
        }

        // Idempotency — if already processed, return existing id
        if (request.getIdempotencyKey() != null) {
            var existing = smsMessageRepo.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                throw new NotificationAlreadyProcessedException(request.getIdempotencyKey());
            }
        }

        // Rate limiting
        rateLimiter.checkPhone(normalizedPhone);
        if (request.getUserId() != null) {
            rateLimiter.checkUser(request.getUserId());
        }

        // Render body
        String body = templateResolver.resolve(
                request.getTemplateCode(),
                request.getLanguage(),
                request.getVariables() != null ? request.getVariables() : Map.of());

        String idempotencyKey = request.getIdempotencyKey() != null
                ? request.getIdempotencyKey()
                : UUID.randomUUID().toString();

        SmsMessage message = SmsMessage.builder()
                .userId(request.getUserId())
                .phoneNumber(normalizedPhone)
                .templateCode(request.getTemplateCode())
                .renderedBody(body)
                .messageType(request.getMessageType())
                .priority(request.getPriority())
                .language(request.getLanguage())
                .status(SmsStatus.QUEUED)
                .maxRetries(smsProperties.getRetry().getMaxAttempts())
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .idempotencyKey(idempotencyKey)
                .providerName(smsProvider.getProviderName())
                .build();

        SmsMessage saved = smsMessageRepo.save(message);
        dispatchAsync(saved.getId());
        return saved.getId();
    }

    @Async("smsAsyncExecutor")
    public void dispatchAsync(Long messageId) {
        try {
            doSend(messageId);
        } catch (Exception ex) {
            log.error("Async SMS dispatch failed for messageId={}: {}", messageId, ex.getMessage(), ex);
        }
    }

    @Transactional
    public void doSend(Long messageId) {
        SmsMessage message = smsMessageRepo.findById(messageId)
                .orElseThrow(() -> new ManaCommunityException("SmsMessage not found: " + messageId,
                        HttpStatus.NOT_FOUND, "SMS_NOT_FOUND"));

        if (message.getStatus() == SmsStatus.SENT || message.getStatus() == SmsStatus.DELIVERED
                || message.getStatus() == SmsStatus.CANCELLED) {
            return;
        }

        message.setStatus(SmsStatus.SENDING);
        smsMessageRepo.save(message);

        SmsSendResponse response = smsProvider.send(SmsSendRequest.builder()
                .to(message.getPhoneNumber())
                .body(message.getRenderedBody())
                .idempotencyKey(message.getIdempotencyKey())
                .build());

        if (response.isSuccess()) {
            message.setStatus(SmsStatus.SENT);
            message.setProviderMessageId(response.getProviderMessageId());
            message.setProviderResponse(response.getRawResponse());
            message.setSentAt(LocalDateTime.now());
            recordCost(message);
        } else {
            handleFailure(message, response.getErrorCode() + ": " + response.getErrorMessage());
        }
        smsMessageRepo.save(message);
    }

    private void handleFailure(SmsMessage message, String reason) {
        message.setRetryCount(message.getRetryCount() + 1);
        message.setFailureReason(reason);

        if (message.getRetryCount() >= message.getMaxRetries()) {
            message.setStatus(SmsStatus.DLQ);
            message.setFailedAt(LocalDateTime.now());
            log.warn("SMS messageId={} moved to DLQ after {} retries", message.getId(), message.getRetryCount());
        } else {
            message.setStatus(SmsStatus.FAILED);
            long delaySec = computeBackoffSeconds(message.getRetryCount());
            message.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySec));
            log.info("SMS messageId={} failed (attempt {}), retry at {}", message.getId(),
                    message.getRetryCount(), message.getNextRetryAt());
        }
    }

    private long computeBackoffSeconds(int attempt) {
        SmsProperties.Retry retryConf = smsProperties.getRetry();
        long delay = (long) (retryConf.getInitialDelaySeconds()
                * Math.pow(retryConf.getBackoffMultiplier(), attempt - 1));
        return Math.min(delay, retryConf.getMaxDelaySeconds());
    }

    private void recordCost(SmsMessage message) {
        try {
            costRecordRepo.save(SmsCostRecord.builder()
                    .smsMessageId(message.getId())
                    .providerName(message.getProviderName())
                    .providerMessageId(message.getProviderMessageId())
                    .toNumber(message.getPhoneNumber())
                    .segments(message.getSegments())
                    .costUsd(message.getCostUsd() != null ? message.getCostUsd() : BigDecimal.ZERO)
                    .billingDate(LocalDate.now())
                    .messageType(message.getMessageType() != null ? message.getMessageType().name() : null)
                    .build());
        } catch (Exception ex) {
            log.warn("Cost recording failed for messageId={}: {}", message.getId(), ex.getMessage());
        }
    }

    @Override
    @Transactional
    public SmsMessageResponse retry(Long smsMessageId) {
        SmsMessage message = smsMessageRepo.findById(smsMessageId)
                .orElseThrow(() -> new ManaCommunityException("SmsMessage not found: " + smsMessageId,
                        HttpStatus.NOT_FOUND, "SMS_NOT_FOUND"));
        if (message.getStatus() != SmsStatus.FAILED && message.getStatus() != SmsStatus.DLQ) {
            throw new ManaCommunityException("Cannot retry message in status: " + message.getStatus(),
                    HttpStatus.CONFLICT, "SMS_INVALID_STATE");
        }
        message.setStatus(SmsStatus.QUEUED);
        message.setNextRetryAt(null);
        smsMessageRepo.save(message);
        dispatchAsync(message.getId());
        return toResponse(message);
    }

    @Override
    @Transactional
    public void cancel(Long smsMessageId) {
        SmsMessage message = smsMessageRepo.findById(smsMessageId)
                .orElseThrow(() -> new ManaCommunityException("SmsMessage not found: " + smsMessageId,
                        HttpStatus.NOT_FOUND, "SMS_NOT_FOUND"));
        if (message.getStatus() == SmsStatus.SENT || message.getStatus() == SmsStatus.DELIVERED) {
            throw new ManaCommunityException("Cannot cancel already-sent message",
                    HttpStatus.CONFLICT, "SMS_ALREADY_SENT");
        }
        message.setStatus(SmsStatus.CANCELLED);
        smsMessageRepo.save(message);
    }

    private SmsMessageResponse toResponse(SmsMessage m) {
        return SmsMessageResponse.builder()
                .id(m.getId())
                .phoneNumber(phoneNumberService.mask(m.getPhoneNumber()))
                .templateCode(m.getTemplateCode())
                .renderedBody(m.getRenderedBody())
                .status(m.getStatus())
                .providerMessageId(m.getProviderMessageId())
                .retryCount(m.getRetryCount())
                .sentAt(m.getSentAt())
                .deliveredAt(m.getDeliveredAt())
                .createdAt(m.getCreatedAt())
                .failureReason(m.getFailureReason())
                .build();
    }
}
