package com.manacommunity.api.insurance.repository;

import com.manacommunity.api.insurance.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface PartnerWebhookEventRepository extends JpaRepository<PartnerWebhookEvent, Long> {
    boolean existsByPartnerCodeAndEventIdAndOutcome(String partnerCode, String eventId, String outcome);
    List<PartnerWebhookEvent> findTop100ByOutcomeNotOrderByReceivedAtDesc(String outcome);
}

