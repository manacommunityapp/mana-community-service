package com.manacommunity.api.calendar.adapter;

import com.manacommunity.api.calendar.CalendarType;
import com.manacommunity.api.calendar.EventSource;
import com.manacommunity.api.calendar.dto.CalendarEventDto;
import com.manacommunity.api.payments.entity.PaymentOrder;
import com.manacommunity.api.payments.model.PaymentOrderStatus;
import com.manacommunity.api.payments.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PaymentDueCalendarAdapter implements CalendarAdapter {

    private final PaymentOrderRepository paymentOrderRepository;

    @Override public EventSource getSource() { return EventSource.PAYMENT_DUE; }
    @Override public CalendarType getCalendarType() { return CalendarType.PAYMENT_DUES; }

    @Override
    public List<CalendarEventDto> fetchEvents(Long communityId, Long userId, Instant from, Instant to) {
        return paymentOrderRepository
                .findByCommunityIdAndStatus(communityId,
                        PaymentOrderStatus.INITIATED,
                        PageRequest.of(0, 100))
                .stream()
                .filter(p -> p.getPayerUserId() != null && p.getPayerUserId().equals(userId))
                .filter(p -> {
                    if (p.getCreatedAt() == null) return false;
                    Instant t = p.getCreatedAt().toInstant();
                    return !t.isBefore(from) && t.isBefore(to);
                })
                .map(p -> {
                    Instant t = p.getCreatedAt().toInstant();
                    return new CalendarEventDto(
                            "PAYMENT_DUE:" + p.getId(),
                            "Payment Due: " + (p.getReferenceType() != null ? p.getReferenceType().name() : "Payment"),
                            null, null, t, t.plusSeconds(86400), true, "UTC",
                            CalendarType.PAYMENT_DUES, EventSource.PAYMENT_DUE,
                            String.valueOf(p.getId()), "#DC2626", false, false,
                            "PAYMENT_ORDER", String.valueOf(p.getId()), null
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
