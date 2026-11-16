package com.manacommunity.api.market.bazaar.listener;

import com.manacommunity.api.market.bazaar.service.MarketBazaarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MarketBazaarPaymentListener {
    private final MarketBazaarService marketBazaarService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentSucceeded(CashfreePaymentSucceededEvent event) {
        if (!"MARKET_STALL_BOOKING".equals(event.referenceType()) || event.referenceId() == null) return;
        marketBazaarService.confirmPaidBooking(event.referenceId(), event.userId(), event.amount());
    }

    public record CashfreePaymentSucceededEvent(String referenceType, Long referenceId, Long userId, java.math.BigDecimal amount) {}
}
