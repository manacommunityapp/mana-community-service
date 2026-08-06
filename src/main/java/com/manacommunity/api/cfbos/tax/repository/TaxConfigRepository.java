package com.manacommunity.api.cfbos.tax.repository;

import com.manacommunity.api.cfbos.tax.entity.TaxConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxConfigRepository extends JpaRepository<TaxConfig, Long> {
}
