package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.FinanceVendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceVendorRepository extends JpaRepository<FinanceVendor, Long> {
    List<FinanceVendor> findAllByOrderByNameAsc();
}
