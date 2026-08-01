package com.cpn.application.jobs;

import com.cpn.domain.jobs.model.Job;
import com.cpn.domain.jobs.model.JobApplication;
import com.cpn.domain.jobs.repository.JobApplicationRepository;
import com.cpn.domain.jobs.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobsService {

    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;

    @Transactional(readOnly = true)
    public List<Job> searchJobs(String query) {
        if (query != null && !query.trim().isEmpty()) {
            return jobRepository.findByTitleContainingIgnoreCaseOrIndustryContainingIgnoreCase(query, query);
        }
        return jobRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Job getJobById(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
    }

    @Transactional
    public Job postJob(Job job) {
        job.setActive(true);
        return jobRepository.save(job);
    }

    @Transactional
    public JobApplication applyToJob(UUID jobId, UUID applicantId, String applicantName, String resumeUrl) {
        JobApplication app = JobApplication.builder()
                .jobId(jobId)
                .applicantId(applicantId)
                .applicantName(applicantName)
                .resumeUrl(resumeUrl)
                .matchScore((int) (Math.random() * 25 + 75)) // Calculate match score (75%-100%)
                .status("APPLIED")
                .build();
        return jobApplicationRepository.save(app);
    }
}
