package com.manacommunity.api.repository.karate;

import com.manacommunity.api.model.karate.SportsKarateExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsKarateExamResultRepository extends JpaRepository<SportsKarateExamResult, Long> {

    List<SportsKarateExamResult> findByExamId(Long examId);

    List<SportsKarateExamResult> findByEnrollmentId(Long enrollmentId);

    Optional<SportsKarateExamResult> findByExamIdAndEnrollmentId(Long examId, Long enrollmentId);

    boolean existsByExamIdAndEnrollmentId(Long examId, Long enrollmentId);

    List<SportsKarateExamResult> findByExamIdAndPassedTrue(Long examId);
}
