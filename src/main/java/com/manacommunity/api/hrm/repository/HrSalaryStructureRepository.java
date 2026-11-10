package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrSalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HrSalaryStructureRepository extends JpaRepository<HrSalaryStructure, Long> {
    List<HrSalaryStructure> findByEmployeeIdOrderByEffectiveFromDesc(Long employeeId);
    Optional<HrSalaryStructure> findFirstByEmployeeIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(Long employeeId, LocalDate date);
}
