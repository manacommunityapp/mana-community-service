package com.manacommunity.api.observability.repository;

import com.manacommunity.api.observability.entity.ApiLatencyTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiLatencyTestRepository extends JpaRepository<ApiLatencyTest, Long> {}
