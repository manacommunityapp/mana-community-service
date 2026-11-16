package com.manacommunity.api.hrm.repository;
import com.manacommunity.api.hrm.entity.HrCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
public interface HrCertificationRepository extends JpaRepository<HrCertification, Long> {
    List<HrCertification> findByEmployeeId(Long employeeId);
    List<HrCertification> findByOrganizationIdAndExpiryDateBetweenAndReminderSentFalse(
        Long orgId, LocalDate from, LocalDate to);
}
