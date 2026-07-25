package com.manacommunity.api.cfbos.tax.repository;

import com.manacommunity.api.cfbos.tax.entity.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {
    List<TaxRate> findByIsActiveTrue();
}
