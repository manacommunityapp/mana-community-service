package com.manacommunity.api.guardpayroll.repository;

import com.manacommunity.api.guardpayroll.entity.ShiftHandoverTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftHandoverTestRepository extends JpaRepository<ShiftHandoverTest, Long> {}
