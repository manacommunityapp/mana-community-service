package com.manacommunity.api.cfbos.billing.repository;
import com.manacommunity.api.cfbos.billing.entity.BillingCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface BillingCategoryRepository extends JpaRepository<BillingCategory, Long> { Optional<BillingCategory> findByCode(String code); }
