package com.manacommunity.api.tenantkyc.repository;

import com.manacommunity.api.tenantkyc.entity.MoveOutRevocationTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MoveOutRevocationTestRepository extends JpaRepository<MoveOutRevocationTest, Long> {}
