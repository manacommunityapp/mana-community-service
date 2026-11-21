package com.manacommunity.api.healthcare.service;

import com.manacommunity.api.healthcare.entity.DiagnosticsSla;
import com.manacommunity.api.healthcare.repository.DiagnosticsSlaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosticsSlaService {

    private final DiagnosticsSlaRepository repo;

    @Transactional
    public DiagnosticsSla create(DiagnosticsSla sla) {
        return repo.save(sla);
    }

    @Transactional
    public DiagnosticsSla markReportDelivered(Long id) {
        DiagnosticsSla sla = repo.findById(id).orElseThrow();
        sla.setReportDeliveredAt(Instant.now());
        return repo.save(sla);
    }

    @Transactional
    public DiagnosticsSla escalate(Long id, String notes) {
        DiagnosticsSla sla = repo.findById(id).orElseThrow();
        sla.setEscalated(true);
        sla.setEscalatedAt(Instant.now());
        if (notes != null) sla.setNotes(notes);
        return repo.save(sla);
    }

    public List<DiagnosticsSla> getByCommunity(Long communityId) {
        return repo.findByCommunityIdOrderByReportDueAtAsc(communityId);
    }

    public List<DiagnosticsSla> getBreaches(Long communityId) {
        return repo.findByCommunityIdAndBreach(communityId, true);
    }

    /** Runs every 15 minutes — detects and logs SLA breaches. */
    @Scheduled(fixedDelay = 900_000)
    @Transactional
    public void detectBreaches() {
        List<DiagnosticsSla> unmarked = repo.findUnmarkedBreaches(Instant.now());
        for (DiagnosticsSla sla : unmarked) {
            sla.setBreach(true);
            repo.save(sla);
            log.warn("Diagnostics SLA breach detected: id={} partner={} due={}", sla.getId(), sla.getPartnerName(), sla.getReportDueAt());
        }
        List<DiagnosticsSla> unnotified = repo.findUnnotifiedBreaches();
        for (DiagnosticsSla sla : unnotified) {
            log.warn("Sending SLA breach notification: id={} patient={}", sla.getId(), sla.getPatientUserId());
            sla.setBreachNotified(true);
            repo.save(sla);
        }
    }
}
