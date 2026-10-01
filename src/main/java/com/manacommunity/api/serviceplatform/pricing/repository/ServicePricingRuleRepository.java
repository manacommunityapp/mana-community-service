package com.manacommunity.api.serviceplatform.pricing.repository;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceUrgency;
import com.manacommunity.api.serviceplatform.pricing.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface ServicePricingRuleRepository extends JpaRepository<PricingRule, Long> {
    Optional<PricingRule> findByCategoryIdAndUrgencyAndIsActiveTrue(Long categoryId, ServiceUrgency urgency);
    Optional<PricingRule> findByUrgencyAndIsActiveTrue(ServiceUrgency urgency);
}
