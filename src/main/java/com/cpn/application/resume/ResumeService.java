package com.cpn.application.resume;

import com.cpn.domain.resume.model.Resume;
import com.cpn.domain.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;

    @Transactional(readOnly = true)
    public List<Resume> getUserResumes(UUID userId) {
        return resumeRepository.findByUserId(userId);
    }

    @Transactional
    public Resume saveResume(Resume resume) {
        resume.setVersion(1);
        resume.setIsAiGenerated(false);
        return resumeRepository.save(resume);
    }
}
