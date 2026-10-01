package com.manacommunity.api.serviceplatform.pricing.repository;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceUrgency;
import com.manacommunity.api.serviceplatform.pricing.entity.ServicePricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface ServicePricingRuleRepository extends JpaRepository<ServicePricingRule, Long> {
    Optional<ServicePricingRule> findByCategoryIdAndUrgencyAndIsActiveTrue(Long categoryId, ServiceUrgency urgency);
    Optional<ServicePricingRule> findByUrgencyAndIsActiveTrue(ServiceUrgency urgency);
}
