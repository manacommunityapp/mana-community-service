package com.manacommunity.api.megatownship.repository;

import com.manacommunity.api.megatownship.entity.FederatedGateTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FederatedGateTestRepository extends JpaRepository<FederatedGateTest, Long> {}
