package com.cpn.domain.jobs.repository;

import com.cpn.domain.jobs.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
    List<JobApplication> findByJobId(UUID jobId);
    List<JobApplication> findByApplicantId(UUID applicantId);
}
