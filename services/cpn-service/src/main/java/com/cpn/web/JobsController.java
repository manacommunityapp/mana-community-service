package com.cpn.web;

import com.cpn.application.jobs.JobsService;
import com.cpn.domain.jobs.model.Job;
import com.cpn.domain.jobs.model.JobApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cpn/jobs")
@RequiredArgsConstructor
public class JobsController {

    private final JobsService jobsService;

    @GetMapping
    public ResponseEntity<List<Job>> searchJobs(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(jobsService.searchJobs(q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(jobsService.getJobById(id));
    }

    @PostMapping
    public ResponseEntity<Job> postJob(@RequestBody Job job) {
        return ResponseEntity.ok(jobsService.postJob(job));
    }

    @PostMapping("/{jobId}/apply")
    public ResponseEntity<JobApplication> apply(
            @PathVariable UUID jobId,
            @RequestParam UUID applicantId,
            @RequestParam String applicantName,
            @RequestParam(required = false) String resumeUrl) {
        return ResponseEntity.ok(jobsService.applyToJob(jobId, applicantId, applicantName, resumeUrl));
    }
}
