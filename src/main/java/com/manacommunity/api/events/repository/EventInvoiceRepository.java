package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventInvoiceRepository extends JpaRepository<EventInvoice, Long> {

    @Modifying
    void deleteByEventId(Long eventId);

    List<EventInvoice> findByEventIdOrderByCreatedAtDesc(Long eventId);

    List<EventInvoice> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
}
