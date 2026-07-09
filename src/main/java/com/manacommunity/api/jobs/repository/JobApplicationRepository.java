package com.manacommunity.api.jobs.repository;

import com.manacommunity.api.jobs.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findByJobId(Long jobId);

    List<JobApplication> findByApplicantIdOrderByAppliedAtDesc(Long applicantId);

    boolean existsByJobIdAndApplicantId(Long jobId, Long applicantId);

    Optional<JobApplication> findByJobIdAndApplicantId(Long jobId, Long applicantId);
}
