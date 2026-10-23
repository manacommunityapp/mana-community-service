package com.manacommunity.api.cfbos;

import com.manacommunity.api.cfbos.statements.dto.CashflowForecastDto;
import com.manacommunity.api.cfbos.statements.service.AiCashflowForecastingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiCashflowForecastingServiceTest {

    private final AiCashflowForecastingService service = new AiCashflowForecastingService();

    @Test
    @DisplayName("AI Forecaster: Generates daily forecast data points and proactive reserve alerts")
    void testForecastGeneration() {
        CashflowForecastDto forecast = service.forecastCashflow(1L, 60);
        assertThat(forecast).isNotNull();
        assertThat(forecast.getHorizonDays()).isEqualTo(60);
        assertThat(forecast.getDailyForecasts()).hasSize(60);
        assertThat(forecast.getProactiveAlerts()).isNotEmpty();
        assertThat(forecast.getCollectionEfficiencyTrend()).isGreaterThan(90.0);
    }
}
