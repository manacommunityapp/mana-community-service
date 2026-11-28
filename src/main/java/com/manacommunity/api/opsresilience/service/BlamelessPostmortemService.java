package com.manacommunity.api.opsresilience.service;

import com.manacommunity.api.opsresilience.entity.IncidentPostmortem;
import com.manacommunity.api.opsresilience.entity.PostmortemGuardrail;
import com.manacommunity.api.opsresilience.repository.IncidentPostmortemRepository;
import com.manacommunity.api.opsresilience.repository.PostmortemGuardrailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Blameless Post-Mortem & Systemic Guardrail Governance Service:
 * Focuses root cause on architectural systems (not individuals) and blocks postmortem closure
 * until at least one permanent guardrail is attached and VERIFIED.
 */
@Service
@RequiredArgsConstructor
public class BlamelessPostmortemService {

    private static final Set<String> ALLOWED_GUARDRAILS = Set.of(
            "BULKHEAD_POOL", "RUSH_HOUR_FREEZE", "AUTO_ROLLBACK", "CHAOS_GAMEDAY"
    );

    private final IncidentPostmortemRepository postmortemRepo;
    private final PostmortemGuardrailRepository guardrailRepo;

    @Transactional
    public IncidentPostmortem openPostmortem(
            String incidentCode,
            String title,
            String severity,
            double blastRadiusPct,
            String rootCauseSystem,
            String mitigationSummary,
            Instant startedAt
    ) {
        return postmortemRepo.save(IncidentPostmortem.builder()
                .incidentCode(incidentCode.trim().toUpperCase(Locale.ROOT))
                .title(title)
                .severity(severity.trim().toUpperCase(Locale.ROOT))
                .blastRadiusPct(blastRadiusPct)
                .rootCauseSystem(rootCauseSystem)
                .mitigationSummary(mitigationSummary)
                .status("OPEN")
                .startedAt(startedAt != null ? startedAt : Instant.now())
                .build());
    }

    @Transactional
    public PostmortemGuardrail addGuardrail(
            Long postmortemId,
            String guardrailType,
            String description,
            String ownerTeam,
            Instant dueAt
    ) {
        postmortemRepo.findById(postmortemId)
                .orElseThrow(() -> new IllegalArgumentException("Postmortem not found: " + postmortemId));
        String type = guardrailType.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_GUARDRAILS.contains(type)) {
            throw new IllegalArgumentException("Unsupported systemic guardrail type: " + guardrailType);
        }
        return guardrailRepo.save(PostmortemGuardrail.builder()
                .postmortemId(postmortemId)
                .guardrailType(type)
                .description(description)
                .ownerTeam(ownerTeam)
                .status("PENDING")
                .dueAt(dueAt)
                .build());
    }

    @Transactional
    public PostmortemGuardrail verifyGuardrail(Long guardrailId) {
        PostmortemGuardrail g = guardrailRepo.findById(guardrailId)
                .orElseThrow(() -> new IllegalArgumentException("Guardrail not found: " + guardrailId));
        g.setStatus("VERIFIED");
        g.setVerifiedAt(Instant.now());
        return guardrailRepo.save(g);
    }

    @Transactional
    public IncidentPostmortem closePostmortem(Long postmortemId) {
        IncidentPostmortem pm = postmortemRepo.findById(postmortemId)
                .orElseThrow(() -> new IllegalArgumentException("Postmortem not found: " + postmortemId));
        List<PostmortemGuardrail> guardrails = guardrailRepo.findByPostmortemIdOrderByCreatedAtAsc(postmortemId);
        if (guardrails.isEmpty()) {
            throw new IllegalStateException("Cannot close blameless postmortem without at least one systemic guardrail");
        }
        boolean allVerified = guardrails.stream().allMatch(g -> "VERIFIED".equals(g.getStatus()));
        if (!allVerified) {
            throw new IllegalStateException("Cannot close postmortem while unverified systemic guardrails remain");
        }
        pm.setStatus("CLOSED");
        pm.setResolvedAt(Instant.now());
        return postmortemRepo.save(pm);
    }
}
