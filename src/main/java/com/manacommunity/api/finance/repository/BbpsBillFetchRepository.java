package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.BbpsBillFetch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BbpsBillFetchRepository extends JpaRepository<BbpsBillFetch, Long> {

    Optional<BbpsBillFetch> findByPaymentOrderId(Long paymentOrderId);
}
