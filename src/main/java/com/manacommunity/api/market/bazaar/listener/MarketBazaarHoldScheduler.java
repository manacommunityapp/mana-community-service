package com.manacommunity.api.market.bazaar.listener;

import com.manacommunity.api.market.bazaar.service.MarketBazaarService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketBazaarHoldScheduler {
    private final MarketBazaarService marketBazaarService;

    @Scheduled(fixedDelayString = "${app.market.bazaar.hold-expiry-delay-ms:60000}")
    public void releaseExpiredHolds() {
        marketBazaarService.expireHolds();
    }
}
