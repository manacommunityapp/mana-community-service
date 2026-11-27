package com.manacommunity.api.serviceplatform.interiors.repository;

import com.manacommunity.api.serviceplatform.interiors.entity.InteriorPaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InteriorPaymentEventRepository extends JpaRepository<InteriorPaymentEvent,Long> {
    List<InteriorPaymentEvent> findByMilestoneIdOrderByCreatedAtAsc(Long milestoneId);
}
