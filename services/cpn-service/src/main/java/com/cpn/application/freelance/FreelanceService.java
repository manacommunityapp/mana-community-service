package com.cpn.application.freelance;

import com.cpn.domain.freelance.model.FreelanceProject;
import com.cpn.domain.freelance.repository.FreelanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FreelanceService {

    private final FreelanceRepository freelanceRepository;

    @Transactional(readOnly = true)
    public List<FreelanceProject> getOpenProjects() {
        return freelanceRepository.findByStatus("OPEN");
    }

    @Transactional
    public FreelanceProject postProject(FreelanceProject project) {
        project.setStatus("OPEN");
        return freelanceRepository.save(project);
    }
}
