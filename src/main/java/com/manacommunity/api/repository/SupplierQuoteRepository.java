package com.manacommunity.api.repository;

import com.manacommunity.api.model.SupplierQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierQuoteRepository extends JpaRepository<SupplierQuote, Long> {

    List<SupplierQuote> findByRfqIdOrderByGrandTotalAsc(Long rfqId);

    Optional<SupplierQuote> findByIdAndRfqId(Long id, Long rfqId);

    long countByRfqId(Long rfqId);
}
