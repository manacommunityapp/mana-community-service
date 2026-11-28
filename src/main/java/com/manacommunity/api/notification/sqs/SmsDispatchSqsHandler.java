package com.manacommunity.api.notification.sqs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.notification.service.SmsServiceImpl;
import com.manacommunity.platform.sqs.SqsEventEnvelope;
import com.manacommunity.platform.sqs.SqsMessageHandler;
import com.manacommunity.platform.sqs.SqsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SQS-1 consumer for {@code mana-sms-dispatch}.
 * Decouples SMS provider HTTP calls from transactional API endpoints and leverages
 * native SQS retries + DLQ on transient gateway failures.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsDispatchSqsHandler implements SqsMessageHandler {

    private final SmsServiceImpl smsServiceImpl;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;

    @Override
    public String queueName() {
        return sqsProperties.getQueues().getSmsDispatch();
    }

    @Override
    public void handle(SqsEventEnvelope envelope) throws Exception {
        JsonNode node = objectMapper.readTree(envelope.payloadJson());
        if (!node.has("smsMessageId")) {
            log.warn("Discarding malformed SMS dispatch SQS message eventId={}", envelope.eventId());
            return;
        }
        long messageId = node.get("smsMessageId").asLong();
        smsServiceImpl.doSend(messageId);
    }
}
