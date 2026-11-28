package com.manacommunity.api.perfbenchmark.repository;

import com.manacommunity.api.perfbenchmark.entity.FrontendPerfTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FrontendPerfTestRepository extends JpaRepository<FrontendPerfTest, Long> {}
