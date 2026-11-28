package com.manacommunity.api.guardpayroll.repository;

import com.manacommunity.api.guardpayroll.entity.AttendancePayrollTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendancePayrollTestRepository extends JpaRepository<AttendancePayrollTest, Long> {}
