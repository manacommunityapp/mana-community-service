package com.manacommunity.api.cfbos.billing.repository;
import com.manacommunity.api.cfbos.billing.entity.BillingRunLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface BillingRunLineRepository extends JpaRepository<BillingRunLine, Long> { List<BillingRunLine> findByBillingRunId(Long billingRunId); List<BillingRunLine> findByPropertyId(Long propertyId); }
