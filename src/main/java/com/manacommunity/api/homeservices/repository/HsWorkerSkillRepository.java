package com.manacommunity.api.homeservices.repository;
import com.manacommunity.api.homeservices.entity.HsWorkerSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HsWorkerSkillRepository extends JpaRepository<HsWorkerSkill, Long> {
    List<HsWorkerSkill> findByWorkerId(Long workerId);
    List<HsWorkerSkill> findByCategory(String category);
}
