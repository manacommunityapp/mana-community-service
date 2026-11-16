package com.manacommunity.api.staff.repository;

import com.manacommunity.api.staff.entity.StaffAccessRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffAccessRuleRepository extends JpaRepository<StaffAccessRule, Long> {
    List<StaffAccessRule> findByStaffIdAndIsActiveTrueOrderByDayOfWeek(Long staffId);
    Optional<StaffAccessRule> findByStaffIdAndDayOfWeek(Long staffId, String dayOfWeek);
}
