package com.manacommunity.api.megatownship.repository;

import com.manacommunity.api.megatownship.entity.ConsolidatedBillingTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsolidatedBillingTestRepository extends JpaRepository<ConsolidatedBillingTest, Long> {}
