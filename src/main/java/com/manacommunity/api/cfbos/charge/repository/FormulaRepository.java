package com.manacommunity.api.cfbos.charge.repository;

import com.manacommunity.api.cfbos.charge.entity.Formula;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FormulaRepository extends JpaRepository<Formula, Long> {
    List<Formula> findByIsActiveTrue();
}
