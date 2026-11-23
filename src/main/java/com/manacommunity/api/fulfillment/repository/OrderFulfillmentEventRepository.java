package com.manacommunity.api.fulfillment.repository;

import com.manacommunity.api.fulfillment.entity.OrderFulfillmentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderFulfillmentEventRepository extends JpaRepository<OrderFulfillmentEvent, Long> {

    List<OrderFulfillmentEvent> findByOrderIdOrderByOccurredAtAsc(String orderId);

    @Query("SELECT e FROM OrderFulfillmentEvent e WHERE e.orderId = :orderId ORDER BY e.occurredAt DESC LIMIT 1")
    Optional<OrderFulfillmentEvent> findLatestByOrderId(String orderId);

    List<OrderFulfillmentEvent> findByCommunityIdOrderByOccurredAtDesc(Long communityId);
}
