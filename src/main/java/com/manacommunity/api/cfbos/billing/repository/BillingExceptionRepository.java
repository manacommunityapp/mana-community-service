package com.manacommunity.api.cfbos.billing.repository;
import com.manacommunity.api.cfbos.billing.entity.BillingException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface BillingExceptionRepository extends JpaRepository<BillingException, Long> { List<BillingException> findByPropertyIdAndIsActiveTrue(Long propertyId); }
