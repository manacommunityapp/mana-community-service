package com.manacommunity.api.profiling.repository;

import com.manacommunity.api.profiling.entity.ProfilingRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfilingRecommendationRepository extends JpaRepository<ProfilingRecommendation, Long> {
    List<ProfilingRecommendation> findByServiceIdOrderBySeverityAsc(String serviceId);
    List<ProfilingRecommendation> findByDomainOrderBySeverityAsc(ProfilingRecommendation.RuntimeDomain domain);
    List<ProfilingRecommendation> findByStatusOrderBySeverityAsc(ProfilingRecommendation.RecommendationStatus status);
}
