package com.manacommunity.api.cfbos.billing.repository;
import com.manacommunity.api.cfbos.billing.entity.BillingRun;
import com.manacommunity.api.cfbos.billing.enums.BillingRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface BillingRunRepository extends JpaRepository<BillingRun, Long> { Optional<BillingRun> findByRunNumber(String runNumber); List<BillingRun> findByStatus(BillingRunStatus status); }
