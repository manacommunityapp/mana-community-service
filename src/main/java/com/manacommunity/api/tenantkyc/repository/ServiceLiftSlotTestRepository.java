package com.manacommunity.api.tenantkyc.repository;

import com.manacommunity.api.tenantkyc.entity.ServiceLiftSlotTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceLiftSlotTestRepository extends JpaRepository<ServiceLiftSlotTest, Long> {}
