package com.manacommunity.api.events.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.events.service.EventReportService;
import com.manacommunity.api.service.BillingService;
import com.manacommunity.api.service.ExpoPushService;
import com.manacommunity.platform.sqs.SqsEventEnvelope;
import com.manacommunity.platform.sqs.SqsMessageHandler;
import com.manacommunity.platform.sqs.SqsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * SQS-10 consumer for {@code mana-report-jobs}.
 * Generates heavy CSV exports asynchronously off request threads, caches the generated
 * artifact in Redis for 1 hour (when available), and pushes a ready notification.
 */
@Slf4j
@Component
public class ReportJobSqsHandler implements SqsMessageHandler {

    private static final String REPORT_KEY_PREFIX = "report:job:";
    private static final Duration REPORT_TTL = Duration.ofHours(1);

    private final EventReportService reportService;
    private final ExpoPushService expoPushService;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private ObjectProvider<BillingService> billingServiceProvider;

    public ReportJobSqsHandler(
            EventReportService reportService,
            ExpoPushService expoPushService,
            SqsProperties sqsProperties,
            ObjectMapper objectMapper,
            @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.reportService = reportService;
        this.expoPushService = expoPushService;
        this.sqsProperties = sqsProperties;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    public record ReportJobPayload(
            String jobId,
            Long eventId,
            String category,
            Long requesterUserId
    ) {}

    public record BatchInvoicePayload(
            Long expenseId
    ) {}

    @Override
    public String queueName() {
        return sqsProperties.getQueues().getReportJobs();
    }

    @Override
    public void handle(SqsEventEnvelope envelope) throws Exception {
        switch (envelope.eventType()) {
            case "EVENT_REGISTRATIONS_CSV" -> {
                ReportJobPayload payload = objectMapper.readValue(envelope.payloadJson(), ReportJobPayload.class);
                processReportJob(payload);
            }
            case "BATCH_INVOICE_GENERATION" -> {
                BatchInvoicePayload payload = objectMapper.readValue(envelope.payloadJson(), BatchInvoicePayload.class);
                processBatchInvoiceJob(payload);
            }
            default -> log.debug("Unhandled report-job SQS event type={} eventId={}",
                    envelope.eventType(), envelope.eventId());
        }
    }

    public void processBatchInvoiceJob(BatchInvoicePayload payload) {
        if (payload == null || payload.expenseId() == null) return;
        BillingService billingService = billingServiceProvider != null ? billingServiceProvider.getIfAvailable() : null;
        if (billingService == null) {
            log.warn("BillingService unavailable for expenseId={}", payload.expenseId());
            return;
        }
        billingService.executeBatchGenerateInvoices(payload.expenseId());
    }

    public void processReportJob(ReportJobPayload payload) {
        if (payload == null || payload.eventId() == null) return;
        byte[] csvBytes = reportService.generateRegistrationsCsv(payload.eventId(), payload.category());

        if (redisTemplate != null && payload.jobId() != null && csvBytes != null) {
            try {
                redisTemplate.opsForValue().set(
                        REPORT_KEY_PREFIX + payload.jobId(),
                        new String(csvBytes, StandardCharsets.UTF_8),
                        REPORT_TTL
                );
            } catch (Exception e) {
                log.warn("Failed to cache async report jobId={}: {}", payload.jobId(), e.getMessage());
            }
        }

        if (payload.requesterUserId() != null) {
            expoPushService.sendToUser(
                    payload.requesterUserId(),
                    "Report Export Ready",
                    "Registrations CSV for event #" + payload.eventId() + " is ready to download.",
                    Map.of(
                            "type", "REPORT_EXPORT_READY",
                            "jobId", payload.jobId() != null ? payload.jobId() : "",
                            "eventId", String.valueOf(payload.eventId())
                    ),
                    "events"
            );
        }
    }

    public Optional<byte[]> getCachedReport(String jobId) {
        if (redisTemplate == null || jobId == null || jobId.isBlank()) {
            return Optional.empty();
        }
        try {
            String csv = redisTemplate.opsForValue().get(REPORT_KEY_PREFIX + jobId);
            return csv != null ? Optional.of(csv.getBytes(StandardCharsets.UTF_8)) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
