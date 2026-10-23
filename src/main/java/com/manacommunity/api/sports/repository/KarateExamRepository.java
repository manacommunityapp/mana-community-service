package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.KarateExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KarateExamRepository extends JpaRepository<KarateExam, Long> {
    List<KarateExam> findByBatchIdOrderByExamDateAsc(Long batchId);
}
