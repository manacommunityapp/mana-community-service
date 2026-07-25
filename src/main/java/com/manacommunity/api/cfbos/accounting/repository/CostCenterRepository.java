package com.manacommunity.api.cfbos.accounting.repository;

import com.manacommunity.api.cfbos.accounting.entity.CostCenter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostCenterRepository extends JpaRepository<CostCenter, Long> {
}
