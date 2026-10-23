package com.manacommunity.api.safety.service.impl;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.safety.dto.CreateIncidentRequest;
import com.manacommunity.api.safety.dto.SecurityIncidentResponse;
import com.manacommunity.api.safety.model.SecurityIncident;
import com.manacommunity.api.safety.repository.SecurityIncidentRepository;
import com.manacommunity.api.safety.service.SecurityIncidentService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityIncidentServiceImpl implements SecurityIncidentService {

    private final SecurityIncidentRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<SecurityIncidentResponse> getIncidents(Long communityId, String status) {
        List<SecurityIncident> list;
        if (status != null && !status.isBlank()) {
            list = repository.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, status);
        } else {
            list = repository.findByCommunityIdOrderByCreatedAtDesc(communityId);
        }
        return list.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public SecurityIncidentResponse createIncident(Long communityId, CreateIncidentRequest request, AppUser reporter) {
        SecurityIncident incident = SecurityIncident.builder()
                .communityId(communityId)
                .type(request.getType())
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .location(request.getLocation())
                .imageUrl(request.getImageUrl())
                .reportedById(reporter.getId())
                .reportedByName(reporter.getFullName())
                .build();
        return toResponse(repository.save(incident));
    }

    @Override
    @Transactional
    public SecurityIncidentResponse updateIncidentStatus(Long id, String status, String resolutionNotes) {
        SecurityIncident incident = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SecurityIncident", id));
        incident.setStatus(status);
        if ("RESOLVED".equals(status) || "CLOSED".equals(status)) {
            incident.setResolvedAt(LocalDateTime.now());
            incident.setResolutionNotes(resolutionNotes);
        }
        return toResponse(repository.save(incident));
    }

    private SecurityIncidentResponse toResponse(SecurityIncident i) {
        return SecurityIncidentResponse.builder()
                .id(i.getId())
                .type(i.getType())
                .title(i.getTitle())
                .description(i.getDescription())
                .status(i.getStatus())
                .priority(i.getPriority())
                .location(i.getLocation())
                .reportedById(i.getReportedById())
                .reportedByName(i.getReportedByName())
                .assignedToId(i.getAssignedToId())
                .assignedToName(i.getAssignedToName())
                .imageUrl(i.getImageUrl())
                .resolutionNotes(i.getResolutionNotes())
                .resolvedAt(i.getResolvedAt() != null ? i.getResolvedAt().toString() : null)
                .createdAt(i.getCreatedAt() != null ? i.getCreatedAt().toString() : null)
                .updatedAt(i.getUpdatedAt() != null ? i.getUpdatedAt().toString() : null)
                .build();
    }
}
