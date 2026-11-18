package com.manacommunity.api.serviceplatform.interiors.repository;

import com.manacommunity.api.serviceplatform.interiors.entity.InteriorQuoteLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InteriorQuoteLineItemRepository extends JpaRepository<InteriorQuoteLineItem, Long> {
    List<InteriorQuoteLineItem> findByQuoteIdOrderByPositionAsc(Long quoteId);
    void deleteByQuoteId(Long quoteId);
}
