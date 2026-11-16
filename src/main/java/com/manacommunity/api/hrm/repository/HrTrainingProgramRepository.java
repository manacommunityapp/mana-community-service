package com.manacommunity.api.hrm.repository;
import com.manacommunity.api.hrm.entity.HrTrainingProgram;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface HrTrainingProgramRepository extends JpaRepository<HrTrainingProgram, Long> {
    Page<HrTrainingProgram> findByOrganizationId(Long orgId, Pageable pageable);
    Page<HrTrainingProgram> findByOrganizationIdAndStatus(Long orgId, String status, Pageable pageable);
}
