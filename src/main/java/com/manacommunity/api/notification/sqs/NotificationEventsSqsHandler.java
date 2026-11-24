package com.manacommunity.api.notification.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.notification.dto.SendSmsRequest;
import com.manacommunity.api.notification.service.SmsService;
import com.manacommunity.platform.sqs.SqsEventEnvelope;
import com.manacommunity.platform.sqs.SqsMessageHandler;
import com.manacommunity.platform.sqs.SqsMessagePublisher;
import com.manacommunity.platform.sqs.SqsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * SQS-2 producer/consumer bridge for {@code mana-notification-events}.
 * Decouples post-commit registration, payment, and cancellation notification
 * fan-outs via SQS, falling back directly to {@link SmsService#send(SendSmsRequest)}
 * when SQS is disabled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventsSqsHandler implements SqsMessageHandler {

    private final SmsService smsService;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private SqsMessagePublisher sqsMessagePublisher;

    @Override
    public String queueName() {
        return sqsProperties.getQueues().getNotificationEvents();
    }

    /**
     * Dispatches a {@link SendSmsRequest} through SQS (with direct fallback when SQS is off).
     */
    public void dispatch(String eventType, SendSmsRequest request) {
        if (sqsMessagePublisher != null) {
            sqsMessagePublisher.publish(queueName(), eventType, request, () -> smsService.send(request));
        } else {
            smsService.send(request);
        }
    }

    @Override
    public void handle(SqsEventEnvelope envelope) throws Exception {
        SendSmsRequest request = objectMapper.readValue(envelope.payloadJson(), SendSmsRequest.class);
        smsService.send(request);
    }
}
