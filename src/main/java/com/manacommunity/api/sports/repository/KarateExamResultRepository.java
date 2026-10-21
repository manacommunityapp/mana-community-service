package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.KarateExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KarateExamResultRepository extends JpaRepository<KarateExamResult, Long> {
    List<KarateExamResult> findByExamIdOrderByRecordedAtAsc(Long examId);
    Optional<KarateExamResult> findByExamIdAndUserId(Long examId, Long userId);
    boolean existsByExamIdAndUserId(Long examId, Long userId);
}
