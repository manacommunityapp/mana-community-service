package com.cpn.web;

import com.cpn.application.freelance.FreelanceService;
import com.cpn.domain.freelance.model.FreelanceProject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cpn/freelance")
@RequiredArgsConstructor
public class FreelanceController {

    private final FreelanceService freelanceService;

    @GetMapping("/projects")
    public ResponseEntity<List<FreelanceProject>> getProjects() {
        return ResponseEntity.ok(freelanceService.getOpenProjects());
    }

    @PostMapping("/projects")
    public ResponseEntity<FreelanceProject> postProject(@RequestBody FreelanceProject project) {
        return ResponseEntity.ok(freelanceService.postProject(project));
    }
}
