package com.cpn.web;

import com.cpn.application.startups.StartupService;
import com.cpn.domain.startups.model.Startup;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cpn/startups")
@RequiredArgsConstructor
public class StartupsController {

    private final StartupService startupService;

    @GetMapping
    public ResponseEntity<List<Startup>> getAllStartups() {
        return ResponseEntity.ok(startupService.getAllStartups());
    }

    @PostMapping
    public ResponseEntity<Startup> registerStartup(@RequestBody Startup startup) {
        return ResponseEntity.ok(startupService.registerStartup(startup));
    }
}
