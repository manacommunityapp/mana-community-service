package com.manacommunity.api.cfbos.billing.repository;
import com.manacommunity.api.cfbos.billing.entity.BillingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
@Repository
public interface BillingRuleRepository extends JpaRepository<BillingRule, Long> { List<BillingRule> findByBillingScheduleIdAndIsActiveTrue(Long scheduleId); @Query("SELECT r FROM BillingRule r WHERE r.isActive = true AND r.effectiveFrom <= :date AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date) ORDER BY r.priority DESC") List<BillingRule> findActiveRulesForDate(@Param("date") LocalDate date); }
