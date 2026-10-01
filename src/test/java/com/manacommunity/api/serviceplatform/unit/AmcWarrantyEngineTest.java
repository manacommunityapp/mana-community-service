package com.manacommunity.api.serviceplatform.unit;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.serviceplatform.amc.engine.AmcWarrantyEngine;
import com.manacommunity.api.serviceplatform.amc.entity.AmcPlan;
import com.manacommunity.api.serviceplatform.amc.entity.AmcSubscription;
import com.manacommunity.api.serviceplatform.amc.entity.AmcSubscriptionStatus;
import com.manacommunity.api.serviceplatform.amc.entity.ServiceWarranty;
import com.manacommunity.api.serviceplatform.amc.entity.WarrantyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AMC & Warranty Engine Unit Tests")
class AmcWarrantyEngineTest {

    private AmcWarrantyEngine engine;

    @BeforeEach
    void setUp() {
        engine = new AmcWarrantyEngine();
    }

    @Test
    @DisplayName("Should validate active warranty within date range")
    void testWarrantyValidity() {
        ServiceWarranty warranty = ServiceWarranty.builder()
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .status(WarrantyStatus.ACTIVE)
                .build();

        assertTrue(engine.isWarrantyValid(warranty, LocalDate.of(2026, 6, 15)));
        assertFalse(engine.isWarrantyValid(warranty, LocalDate.of(2027, 1, 1))); // Expired by date

        warranty.setStatus(WarrantyStatus.VOID);
        assertFalse(engine.isWarrantyValid(warranty, LocalDate.of(2026, 6, 15)));
    }

    @Test
    @DisplayName("Should validate and consume AMC visit correctly")
    void testAmcVisitConsumption() {
        AmcPlan plan = AmcPlan.builder()
                .durationMonths(12)
                .visitsIncluded(4)
                .price(new BigDecimal("2400.00"))
                .build();

        AppUser user = AppUser.builder().id(1L).fullName("John Doe").build();
        Community community = Community.builder().id(1L).name("Mana Residency").build();

        AmcSubscription sub = engine.createSubscription(plan, user, community, LocalDate.of(2026, 1, 1));

        assertEquals(4, sub.getVisitsRemaining());
        assertEquals(0, sub.getVisitsUsed());
        assertEquals(AmcSubscriptionStatus.ACTIVE, sub.getStatus());
        assertTrue(engine.canUseAmcVisit(sub, LocalDate.of(2026, 5, 1)));

        // Consume 1 visit
        engine.consumeAmcVisit(sub);
        assertEquals(3, sub.getVisitsRemaining());
        assertEquals(1, sub.getVisitsUsed());
        assertEquals(AmcSubscriptionStatus.ACTIVE, sub.getStatus());

        // Consume remaining 3 visits
        engine.consumeAmcVisit(sub);
        engine.consumeAmcVisit(sub);
        engine.consumeAmcVisit(sub);
        assertEquals(0, sub.getVisitsRemaining());
        assertEquals(4, sub.getVisitsUsed());
        assertEquals(AmcSubscriptionStatus.EXPIRED, sub.getStatus());
        assertFalse(engine.canUseAmcVisit(sub, LocalDate.of(2026, 5, 1)));
    }
}
