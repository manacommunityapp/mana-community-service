package com.manacommunity.platform.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Long-polls configured SQS queues and routes messages to registered {@link SqsMessageHandler} beans.
 * Active only when {@code app.sqs.enabled=true}.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.sqs", name = "enabled", havingValue = "true")
public class SqsQueuePoller {

    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;
    private final Map<String, SqsMessageHandler> handlersByQueue;
    private final ConcurrentHashMap<String, String> queueUrlCache = new ConcurrentHashMap<>();

    public SqsQueuePoller(
            SqsClient sqsClient,
            SqsProperties sqsProperties,
            ObjectMapper objectMapper,
            List<SqsMessageHandler> handlers) {
        this.sqsClient = sqsClient;
        this.sqsProperties = sqsProperties;
        this.objectMapper = objectMapper;
        this.handlersByQueue = handlers.stream()
                .collect(Collectors.toMap(SqsMessageHandler::queueName, h -> h, (a, b) -> a));
        log.info("Initialized SqsQueuePoller for queues: {}", handlersByQueue.keySet());
    }

    @Scheduled(fixedDelayString = "${app.sqs.poll-delay-ms:2000}")
    public void pollAllQueues() {
        for (Map.Entry<String, SqsMessageHandler> entry : handlersByQueue.entrySet()) {
            pollQueue(entry.getKey(), entry.getValue());
        }
    }

    public void pollQueue(String queueName, SqsMessageHandler handler) {
        try {
            String queueUrl = SqsConfig.resolveQueueUrl(sqsClient, queueUrlCache, queueName);
            List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(sqsProperties.getMaxMessagesPerPoll())
                    .waitTimeSeconds(sqsProperties.getWaitTimeSeconds())
                    .build()).messages();

            for (Message msg : messages) {
                try {
                    SqsEventEnvelope envelope = objectMapper.readValue(msg.body(), SqsEventEnvelope.class);
                    handler.handle(envelope);
                    sqsClient.deleteMessage(DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(msg.receiptHandle())
                            .build());
                } catch (Exception ex) {
                    // Leave message un-deleted so SQS visibility timeout retries and routes to DLQ
                    log.error("SQS handler failed for queue={} messageId={}: {}",
                            queueName, msg.messageId(), ex.getMessage(), ex);
                }
            }
        } catch (Exception ex) {
            log.warn("SQS poll skipped/failed for queue={}: {}", queueName, ex.getMessage());
        }
    }
}
