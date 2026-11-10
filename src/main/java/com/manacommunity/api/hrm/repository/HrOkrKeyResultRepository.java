package com.manacommunity.api.hrm.repository;
import com.manacommunity.api.hrm.entity.HrOkrKeyResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HrOkrKeyResultRepository extends JpaRepository<HrOkrKeyResult, Long> {
    List<HrOkrKeyResult> findByOkrId(Long okrId);
}
