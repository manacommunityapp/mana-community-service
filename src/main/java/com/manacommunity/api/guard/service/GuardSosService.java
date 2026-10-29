package com.manacommunity.api.guard.service;

import com.manacommunity.api.guard.entity.GuardSos;
import com.manacommunity.api.guard.repository.GuardSosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GuardSosService {

    private final GuardSosRepository repo;

    @Transactional
    public GuardSos triggerSos(Long communityId, Long guardUserId, Double lat, Double lng, String message) {
        return repo.save(GuardSos.builder()
                .communityId(communityId)
                .guardUserId(guardUserId)
                .latitude(lat)
                .longitude(lng)
                .message(message)
                .build());
    }

    @Transactional
    public GuardSos acknowledgeSos(Long id, Long adminUserId) {
        GuardSos sos = repo.findById(id)
                .orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("GuardSos", id));
        sos.setStatus("ACKNOWLEDGED");
        sos.setAcknowledgedBy(adminUserId);
        sos.setAcknowledgedAt(LocalDateTime.now());
        return repo.save(sos);
    }

    @Transactional(readOnly = true)
    public List<GuardSos> getActiveSos(Long communityId) {
        return repo.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, "ACTIVE");
    }
}
