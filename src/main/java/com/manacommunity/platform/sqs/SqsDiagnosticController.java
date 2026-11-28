package com.manacommunity.platform.sqs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;

import java.util.Map;

/**
 * Diagnostic and testing controller for AWS SQS on local/dev environments.
 * Active only under {@code local}/{@code dev} profiles when {@code app.sqs.enabled=true}.
 */
@Slf4j
@RestController
@Profile({"local", "dev"})
@RequestMapping("/api/dev/sqs")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.sqs", name = "enabled", havingValue = "true")
public class SqsDiagnosticController {

    private final SqsProperties sqsProperties;
    private final SqsMessagePublisher publisher;
    private final SqsClient sqsClient;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        ListQueuesResponse response = sqsClient.listQueues();
        return ResponseEntity.ok(Map.of(
                "enabled", sqsProperties.isEnabled(),
                "endpoint", sqsProperties.getEndpoint(),
                "region", sqsProperties.getRegion(),
                "activeQueuesOnServer", response.queueUrls(),
                "configuredQueues", sqsProperties.getQueues()
        ));
    }

    @PostMapping("/test-publish")
    public ResponseEntity<Map<String, Object>> testPublish(
            @RequestParam(defaultValue = "mana-sports-events") String queue,
            @RequestParam(defaultValue = "TEST_EVENT") String eventType,
            @RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> body = (payload != null && !payload.isEmpty())
                ? payload
                : Map.of("test", true, "timestamp", System.currentTimeMillis());

        boolean published = publisher.publish(queue, eventType, body, () -> {
            log.info("Executed in-process fallback for test event on queue={}", queue);
        });

        return ResponseEntity.ok(Map.of(
                "publishedToSqs", published,
                "queue", queue,
                "eventType", eventType,
                "payload", body
        ));
    }
}
