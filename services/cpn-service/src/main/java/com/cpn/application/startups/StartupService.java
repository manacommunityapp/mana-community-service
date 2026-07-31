package com.cpn.application.startups;

import com.cpn.domain.startups.model.Startup;
import com.cpn.domain.startups.repository.StartupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StartupService {

    private final StartupRepository startupRepository;

    @Transactional(readOnly = true)
    public List<Startup> getAllStartups() {
        return startupRepository.findAll();
    }

    @Transactional
    public Startup registerStartup(Startup startup) {
        return startupRepository.save(startup);
    }
}
