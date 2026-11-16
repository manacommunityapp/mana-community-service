package com.manacommunity.api.hrm.repository;
import com.manacommunity.api.hrm.entity.HrRecognition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface HrRecognitionRepository extends JpaRepository<HrRecognition, Long> {
    Page<HrRecognition> findByOrganizationIdAndIsPublicTrue(Long orgId, Pageable pageable);
    Page<HrRecognition> findByReceiverEmployeeId(Long employeeId, Pageable pageable);
    Page<HrRecognition> findByGiverEmployeeId(Long employeeId, Pageable pageable);
}
