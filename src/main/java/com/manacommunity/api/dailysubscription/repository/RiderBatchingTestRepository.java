package com.manacommunity.api.dailysubscription.repository;

import com.manacommunity.api.dailysubscription.entity.RiderBatchingTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RiderBatchingTestRepository extends JpaRepository<RiderBatchingTest, Long> {}
