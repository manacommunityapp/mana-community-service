package com.manacommunity.api.repository;

import com.manacommunity.api.model.StaffScheduleRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffScheduleRuleRepository extends JpaRepository<StaffScheduleRule, Long> {

    List<StaffScheduleRule> findBySocietyId(Long societyId);

    Optional<StaffScheduleRule> findByStaffId(Long staffId);
}
