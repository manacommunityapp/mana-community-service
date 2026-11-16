package com.manacommunity.api.hrm.repository;
import com.manacommunity.api.hrm.entity.HrTrainingEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HrTrainingEnrollmentRepository extends JpaRepository<HrTrainingEnrollment, Long> {
    List<HrTrainingEnrollment> findByEmployeeId(Long employeeId);
    List<HrTrainingEnrollment> findByTrainingProgramId(Long programId);
    boolean existsByEmployeeIdAndTrainingProgramId(Long employeeId, Long programId);
}
