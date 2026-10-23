package com.manacommunity.api.insurance.repository;

import com.manacommunity.api.insurance.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface PolicyStatusHistoryRepository extends JpaRepository<PolicyStatusHistory, Long> {
    boolean existsByPolicyIdAndEventId(Long policyId, String eventId);
    List<PolicyStatusHistory> findByPolicyIdOrderByIdAsc(Long policyId);
}

