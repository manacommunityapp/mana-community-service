package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventInvoiceRepository extends JpaRepository<EventInvoice, Long> {

    List<EventInvoice> findByEventIdOrderByCreatedAtDesc(Long eventId);

    List<EventInvoice> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
}
