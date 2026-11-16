package com.manacommunity.api.hrm.repository;
import com.manacommunity.api.hrm.entity.HrGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HrGoalRepository extends JpaRepository<HrGoal, Long> {
    List<HrGoal> findByEmployeeId(Long employeeId);
    List<HrGoal> findByEmployeeIdAndAppraisalCycleId(Long employeeId, Long cycleId);
}
