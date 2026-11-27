package com.manacommunity.api.accounting.repository;

import com.manacommunity.api.accounting.entity.TaxRuleVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxRuleVersionRepository extends JpaRepository<TaxRuleVersionEntity, UUID> {

    List<TaxRuleVersionEntity> findByRuleFamilyAndFyCodeOrderByVersionNoDesc(String ruleFamily, String fyCode);

    List<TaxRuleVersionEntity> findAllByOrderByFyCodeDescVersionNoDesc();

    @Query("""
        SELECT r FROM TaxRuleVersionEntity r
         WHERE r.ruleFamily = :ruleFamily
           AND r.fyCode = :fyCode
           AND r.effectiveFrom <= :supplyDate
           AND (r.effectiveTo IS NULL OR r.effectiveTo >= :supplyDate)
         ORDER BY r.versionNo DESC
        """)
    List<TaxRuleVersionEntity> findMatchingRules(
            @Param("ruleFamily") String ruleFamily,
            @Param("fyCode") String fyCode,
            @Param("supplyDate") LocalDate supplyDate
    );

    Optional<TaxRuleVersionEntity> findTopByRuleFamilyAndFyCodeOrderByVersionNoDesc(String ruleFamily, String fyCode);
}
