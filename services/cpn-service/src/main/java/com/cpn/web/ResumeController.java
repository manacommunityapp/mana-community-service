package com.cpn.web;

import com.cpn.application.resume.ResumeService;
import com.cpn.domain.resume.model.Resume;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cpn/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Resume>> getResumes(@PathVariable UUID userId) {
        return ResponseEntity.ok(resumeService.getUserResumes(userId));
    }

    @PostMapping
    public ResponseEntity<Resume> saveResume(@RequestBody Resume resume) {
        return ResponseEntity.ok(resumeService.saveResume(resume));
    }
}
