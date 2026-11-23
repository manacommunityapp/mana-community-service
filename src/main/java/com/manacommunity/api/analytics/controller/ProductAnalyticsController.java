package com.manacommunity.api.analytics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.analytics.dto.AnalyticsBatchRequest;
import com.manacommunity.api.analytics.model.ProductAnalyticsEvent;
import com.manacommunity.api.analytics.repository.ProductAnalyticsEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class ProductAnalyticsController {

    private final ProductAnalyticsEventRepository eventRepo;
    private final ObjectMapper objectMapper;

    @PostMapping({"/api/v1/analytics/events", "/api/analytics/events"})
    public ResponseEntity<Void> ingestEvents(@RequestBody(required = false) AnalyticsBatchRequest request) {
        if (request == null || request.getEvents() == null || request.getEvents().isEmpty()) {
            return ResponseEntity.ok().build();
        }

        try {
            List<ProductAnalyticsEvent> entities = new ArrayList<>();
            for (AnalyticsBatchRequest.AnalyticsEventItemDto item : request.getEvents()) {
                if (item.getEvent() == null || item.getEvent().isBlank()) continue;

                OffsetDateTime occurredAt = OffsetDateTime.now();
                if (item.getTs() != null && !item.getTs().isBlank()) {
                    try {
                        occurredAt = OffsetDateTime.parse(item.getTs());
                    } catch (Exception ignored) {}
                }

                String propsJson = null;
                if (item.getProps() != null && !item.getProps().isEmpty()) {
                    try {
                        propsJson = objectMapper.writeValueAsString(item.getProps());
                    } catch (Exception ignored) {}
                }

                entities.add(ProductAnalyticsEvent.builder()
                        .eventName(item.getEvent())
                        .communityId(item.getCommunityId())
                        .userHash(item.getUserHash())
                        .cohort(item.getCohort())
                        .sessionId(item.getSessionId())
                        .propsJson(propsJson)
                        .occurredAt(occurredAt)
                        .build());
            }

            if (!entities.isEmpty()) {
                eventRepo.saveAll(entities);
            }
        } catch (Exception ex) {
            log.warn("Error ingesting analytics events: {}", ex.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
