package com.manacommunity.api.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.analytics.controller.ProductAnalyticsController;
import com.manacommunity.api.analytics.dto.AnalyticsBatchRequest;
import com.manacommunity.api.analytics.model.ProductAnalyticsEvent;
import com.manacommunity.api.analytics.repository.ProductAnalyticsEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductAnalyticsController")
class ProductAnalyticsControllerTest {

    @Mock
    private ProductAnalyticsEventRepository eventRepo;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ProductAnalyticsController controller;

    @Test
    @DisplayName("ingestEvents persists valid events batch")
    void ingestEventsSuccess() {
        AnalyticsBatchRequest.AnalyticsEventItemDto item = AnalyticsBatchRequest.AnalyticsEventItemDto.builder()
                .event("booking_created")
                .communityId(5L)
                .userHash("abc123hash")
                .cohort("pilot-1")
                .sessionId("sess-xyz")
                .props(Map.of("module", "sports", "paid", true))
                .build();

        AnalyticsBatchRequest request = AnalyticsBatchRequest.builder()
                .events(List.of(item))
                .build();

        ResponseEntity<Void> response = controller.ingestEvents(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        ArgumentCaptor<List<ProductAnalyticsEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventRepo).saveAll(captor.capture());

        List<ProductAnalyticsEvent> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getEventName()).isEqualTo("booking_created");
        assertThat(saved.get(0).getCommunityId()).isEqualTo(5L);
        assertThat(saved.get(0).getUserHash()).isEqualTo("abc123hash");
        assertThat(saved.get(0).getPropsJson()).contains("\"module\":\"sports\"");
    }

    @Test
    @DisplayName("ingestEvents handles empty/null gracefully")
    void ingestEventsNull() {
        ResponseEntity<Void> response = controller.ingestEvents(null);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
