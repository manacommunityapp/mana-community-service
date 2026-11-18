package com.manacommunity.api.serviceplatform.pricing.repository;
import com.manacommunity.api.serviceplatform.pricing.entity.ServiceQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ServiceQuoteRepository extends JpaRepository<ServiceQuote, Long> {
    List<ServiceQuote> findByServiceRequestId(Long serviceRequestId);
    java.util.Optional<ServiceQuote> findByServiceRequestIdAndProviderId(Long serviceRequestId, Long providerId);
    List<ServiceQuote> findByServiceRequestIdOrderByCreatedAtDesc(Long serviceRequestId);
    List<ServiceQuote> findByProviderId(Long providerId);
}
