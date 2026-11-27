package com.manacommunity.api.repository;

import com.manacommunity.api.model.BillingRunLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingRunLineItemRepository extends JpaRepository<BillingRunLineItem, Long> {

    List<BillingRunLineItem> findByBillingRunId(Long billingRunId);

    Optional<BillingRunLineItem> findByBillingRunIdAndResidentId(Long billingRunId, Long residentId);

    long countByBillingRunIdAndStatus(Long billingRunId, String status);

    List<BillingRunLineItem> findByBillingRunIdAndStatus(Long billingRunId, String status);
}
