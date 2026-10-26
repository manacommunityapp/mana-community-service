package com.manacommunity.api.insurance.repository;

import com.manacommunity.api.insurance.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, Long> {
    List<InsurancePolicy> findByUserIdOrderByExpiryDateAsc(Long userId);
    Optional<InsurancePolicy> findByIdAndUserId(Long id, Long userId);
}

