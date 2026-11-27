package com.manacommunity.api.billing.engine;

import com.manacommunity.api.billing.engine.rules.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Set;

@Configuration
public class FeeEngineConfig {

    @Bean
    public FeeEngine feeEngine() {
        FeeEngine engine = new FeeEngine();

        engine.registerRule(new AreaBasedMaintenanceRule(new BigDecimal("5.50")));

        engine.registerRule(new FixedChargeRule("SINKING_FUND", "Sinking fund contribution",
                new BigDecimal("500.00"), null));
        engine.registerRule(new FixedChargeRule("WATER_CHARGE", "Water charges",
                new BigDecimal("300.00"), null));
        engine.registerRule(new FixedChargeRule("PARKING_CAR", "Car parking charge",
                new BigDecimal("200.00"), Set.of("1BHK", "2BHK", "3BHK", "4BHK", "VILLA")));
        engine.registerRule(new FixedChargeRule("PARKING_BIKE", "Two-wheeler parking charge",
                new BigDecimal("50.00"), null));

        engine.registerRule(new CompoundInterestPenaltyRule(new BigDecimal("0.02"), true));

        engine.registerRule(new WaiverRule());

        engine.registerRule(new GstRule(new BigDecimal("0.09"), new BigDecimal("0.09")));

        return engine;
    }
}
