package com.manacommunity.api.utilitymeter.repository;

import com.manacommunity.api.utilitymeter.entity.TankerPatrolTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TankerPatrolTestRepository extends JpaRepository<TankerPatrolTest, Long> {}
