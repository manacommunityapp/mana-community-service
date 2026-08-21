package com.manacommunity.api.notification.scheduler;

import com.manacommunity.api.notification.entity.SmsMessage;
import com.manacommunity.api.notification.enums.SmsStatus;
import com.manacommunity.api.notification.repository.SmsMessageRepository;
import com.manacommunity.api.notification.service.SmsServiceImpl;
import com.manacommunity.api.notification.service.SmsRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmsRetryScheduler {

    private final SmsMessageRepository smsMessageRepo;
    private final SmsServiceImpl smsServiceImpl;
    private final SmsRateLimiter rateLimiter;

    /** Every 2 minutes: pick up FAILED messages whose retry window has elapsed. */
    @Scheduled(fixedDelayString = "${app.sms.retry-scheduler-delay-ms:120000}")
    public void processRetries() {
        List<SmsMessage> messages = smsMessageRepo.findMessagesForRetry(LocalDateTime.now());
        if (!messages.isEmpty()) {
            log.info("SMS retry scheduler: {} messages eligible for retry", messages.size());
        }
        for (SmsMessage msg : messages) {
            try {
                msg.setStatus(SmsStatus.RETRYING);
                smsMessageRepo.save(msg);
                smsServiceImpl.dispatchAsync(msg.getId());
            } catch (Exception ex) {
                log.error("Retry dispatch failed for messageId={}: {}", msg.getId(), ex.getMessage());
            }
        }
    }

    /** Every hour: clean up stale in-memory rate-limit buckets. */
    @Scheduled(fixedDelayString = "3600000")
    public void evictRateLimitBuckets() {
        rateLimiter.evictStale();
    }
}
