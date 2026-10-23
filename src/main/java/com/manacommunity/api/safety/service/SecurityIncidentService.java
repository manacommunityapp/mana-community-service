package com.manacommunity.api.safety.service;

import com.manacommunity.api.safety.dto.CreateIncidentRequest;
import com.manacommunity.api.safety.dto.SecurityIncidentResponse;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface SecurityIncidentService {

    List<SecurityIncidentResponse> getIncidents(Long communityId, String status);

    SecurityIncidentResponse createIncident(Long communityId, CreateIncidentRequest request, AppUser reporter);

    SecurityIncidentResponse updateIncidentStatus(Long id, String status, String resolutionNotes);
}
