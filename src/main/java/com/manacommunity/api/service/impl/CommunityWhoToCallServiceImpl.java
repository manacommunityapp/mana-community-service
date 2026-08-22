package com.manacommunity.api.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.dto.CommunityWhoToCallHistoryResponse;
import com.manacommunity.api.dto.CommunityWhoToCallRequest;
import com.manacommunity.api.dto.CommunityWhoToCallResponse;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.CommunityWhoToCall;
import com.manacommunity.api.model.CommunityWhoToCallHistory;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.CommunityWhoToCallHistoryRepository;
import com.manacommunity.api.repository.CommunityWhoToCallRepository;
import com.manacommunity.api.service.CommunityWhoToCallService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityWhoToCallServiceImpl implements CommunityWhoToCallService {

    private final CommunityWhoToCallRepository whoToCallRepository;
    private final CommunityWhoToCallHistoryRepository historyRepository;
    private final CommunityRepository communityRepository;
    private final AppUserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CommunityWhoToCallResponse> getActive(Long communityId) {
        return whoToCallRepository.findByCommunityIdAndIsActiveTrueOrderByDisplayOrderAsc(communityId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommunityWhoToCallResponse> getAll(Long communityId) {
        return whoToCallRepository.findByCommunityIdOrderByDisplayOrderAsc(communityId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CommunityWhoToCallResponse getById(Long id) {
        CommunityWhoToCall item = findByIdOrThrow(id);
        return toResponse(item);
    }

    @Override
    @Transactional
    public CommunityWhoToCallResponse create(Long communityId, Long userId, String userName, CommunityWhoToCallRequest req) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));

        AppUser linkedUser = null;
        if (req.getUserId() != null && req.getUserId() > 0) {
            linkedUser = userRepository.findById(req.getUserId()).orElse(null);
        }

        CommunityWhoToCall item = CommunityWhoToCall.builder()
                .community(community)
                .department(req.getDepartment().trim())
                .contactPerson(req.getContactPerson().trim())
                .user(linkedUser)
                .phoneNumber(req.getPhoneNumber().trim())
                .secondaryPhone(blankToNull(req.getSecondaryPhone()))
                .email(blankToNull(req.getEmail()))
                .designation(blankToNull(req.getDesignation()))
                .availability(req.getAvailability() != null && !req.getAvailability().isBlank() ? req.getAvailability().trim() : "24/7 Available")
                .locationOrDesk(blankToNull(req.getLocationOrDesk()))
                .icon(req.getIcon() != null && !req.getIcon().isBlank() ? req.getIcon().trim() : "HelpCircle")
                .color(req.getColor() != null && !req.getColor().isBlank() ? req.getColor().trim() : "text-indigo-600 bg-indigo-50 border-indigo-200")
                .isEmergency(Boolean.TRUE.equals(req.getIsEmergency()))
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        CommunityWhoToCall saved = whoToCallRepository.save(item);

        recordHistory(
                saved.getId(),
                communityId,
                "CREATED",
                userId,
                userName,
                saved.getDepartment(),
                saved.getContactPerson(),
                saved.getPhoneNumber(),
                "Created new contact for " + saved.getDepartment() + " (" + saved.getContactPerson() + ")",
                saved
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public CommunityWhoToCallResponse update(Long id, Long userId, String userName, CommunityWhoToCallRequest req) {
        CommunityWhoToCall item = findByIdOrThrow(id);

        AppUser linkedUser = null;
        if (req.getUserId() != null && req.getUserId() > 0) {
            linkedUser = userRepository.findById(req.getUserId()).orElse(null);
        }

        List<String> changeList = new ArrayList<>();
        if (!item.getDepartment().equalsIgnoreCase(req.getDepartment().trim())) {
            changeList.add("Department: '" + item.getDepartment() + "' -> '" + req.getDepartment().trim() + "'");
        }
        if (!item.getContactPerson().equalsIgnoreCase(req.getContactPerson().trim())) {
            changeList.add("Contact Person: '" + item.getContactPerson() + "' -> '" + req.getContactPerson().trim() + "'");
        }
        if (!item.getPhoneNumber().equalsIgnoreCase(req.getPhoneNumber().trim())) {
            changeList.add("Phone: '" + item.getPhoneNumber() + "' -> '" + req.getPhoneNumber().trim() + "'");
        }
        if (req.getAvailability() != null && !req.getAvailability().equals(item.getAvailability())) {
            changeList.add("Availability: '" + item.getAvailability() + "' -> '" + req.getAvailability() + "'");
        }
        if (req.getIsEmergency() != null && !req.getIsEmergency().equals(item.getIsEmergency())) {
            changeList.add("Emergency status changed to " + req.getIsEmergency());
        }

        String summary = changeList.isEmpty() ? "Updated contact details" : String.join("; ", changeList);

        item.setDepartment(req.getDepartment().trim());
        item.setContactPerson(req.getContactPerson().trim());
        item.setUser(linkedUser);
        item.setPhoneNumber(req.getPhoneNumber().trim());
        item.setSecondaryPhone(blankToNull(req.getSecondaryPhone()));
        item.setEmail(blankToNull(req.getEmail()));
        item.setDesignation(blankToNull(req.getDesignation()));
        if (req.getAvailability() != null && !req.getAvailability().isBlank()) {
            item.setAvailability(req.getAvailability().trim());
        }
        item.setLocationOrDesk(blankToNull(req.getLocationOrDesk()));
        if (req.getIcon() != null && !req.getIcon().isBlank()) {
            item.setIcon(req.getIcon().trim());
        }
        if (req.getColor() != null && !req.getColor().isBlank()) {
            item.setColor(req.getColor().trim());
        }
        if (req.getIsEmergency() != null) {
            item.setIsEmergency(req.getIsEmergency());
        }
        if (req.getDisplayOrder() != null) {
            item.setDisplayOrder(req.getDisplayOrder());
        }
        if (req.getIsActive() != null) {
            item.setIsActive(req.getIsActive());
        }
        item.setUpdatedBy(userId);

        CommunityWhoToCall saved = whoToCallRepository.save(item);

        recordHistory(
                saved.getId(),
                saved.getCommunity().getId(),
                "UPDATED",
                userId,
                userName,
                saved.getDepartment(),
                saved.getContactPerson(),
                saved.getPhoneNumber(),
                summary,
                saved
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id, Long userId, String userName) {
        CommunityWhoToCall item = findByIdOrThrow(id);
        boolean newStatus = !Boolean.TRUE.equals(item.getIsActive());
        item.setIsActive(newStatus);
        item.setUpdatedBy(userId);
        whoToCallRepository.save(item);

        recordHistory(
                item.getId(),
                item.getCommunity().getId(),
                newStatus ? "RESTORED" : "DEACTIVATED",
                userId,
                userName,
                item.getDepartment(),
                item.getContactPerson(),
                item.getPhoneNumber(),
                newStatus ? "Re-activated contact" : "Deactivated contact",
                item
        );
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId, String userName) {
        CommunityWhoToCall item = findByIdOrThrow(id);
        item.setIsActive(false);
        item.setUpdatedBy(userId);
        whoToCallRepository.save(item);

        recordHistory(
                item.getId(),
                item.getCommunity().getId(),
                "DEACTIVATED",
                userId,
                userName,
                item.getDepartment(),
                item.getContactPerson(),
                item.getPhoneNumber(),
                "Removed from active directory",
                item
        );
    }

    @Override
    @Transactional
    public CommunityWhoToCallResponse restore(Long id, Long userId, String userName) {
        CommunityWhoToCall item = findByIdOrThrow(id);
        item.setIsActive(true);
        item.setUpdatedBy(userId);
        CommunityWhoToCall saved = whoToCallRepository.save(item);

        recordHistory(
                saved.getId(),
                saved.getCommunity().getId(),
                "RESTORED",
                userId,
                userName,
                saved.getDepartment(),
                saved.getContactPerson(),
                saved.getPhoneNumber(),
                "Restored to active directory",
                saved
        );

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommunityWhoToCallHistoryResponse> getHistory(Long whoToCallId) {
        return historyRepository.findByWhoToCallIdOrderByCreatedAtDesc(whoToCallId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommunityWhoToCallHistoryResponse> getAllCommunityHistory(Long communityId) {
        return historyRepository.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private void recordHistory(Long whoToCallId, Long communityId, String action, Long userId,
                               String userName, String department, String contactPerson,
                               String phoneNumber, String summary, CommunityWhoToCall entity) {
        String snapshot = null;
        try {
            if (entity != null) {
                snapshot = objectMapper.writeValueAsString(toResponse(entity));
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize WhoToCall snapshot for id {}: {}", whoToCallId, e.getMessage());
        }

        CommunityWhoToCallHistory hist = CommunityWhoToCallHistory.builder()
                .whoToCallId(whoToCallId)
                .communityId(communityId)
                .action(action)
                .changedByUserId(userId)
                .changedByName(userName != null ? userName : "Admin")
                .department(department)
                .contactPerson(contactPerson)
                .phoneNumber(phoneNumber)
                .changeSummary(summary)
                .snapshotData(snapshot)
                .build();

        historyRepository.save(hist);
    }

    private CommunityWhoToCall findByIdOrThrow(Long id) {
        return whoToCallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WhoToCall Contact", id));
    }

    private CommunityWhoToCallResponse toResponse(CommunityWhoToCall item) {
        return CommunityWhoToCallResponse.builder()
                .id(item.getId())
                .communityId(item.getCommunity() != null ? item.getCommunity().getId() : null)
                .department(item.getDepartment())
                .contactPerson(item.getContactPerson())
                .userId(item.getUser() != null ? item.getUser().getId() : null)
                .userFullName(item.getUser() != null ? item.getUser().getFullName() : null)
                .userProfilePicUrl(item.getUser() != null ? item.getUser().getProfilePicUrl() : null)
                .phoneNumber(item.getPhoneNumber())
                .secondaryPhone(item.getSecondaryPhone())
                .email(item.getEmail())
                .designation(item.getDesignation())
                .availability(item.getAvailability())
                .locationOrDesk(item.getLocationOrDesk())
                .icon(item.getIcon())
                .color(item.getColor())
                .isEmergency(item.getIsEmergency())
                .displayOrder(item.getDisplayOrder())
                .isActive(item.getIsActive())
                .createdBy(item.getCreatedBy())
                .updatedBy(item.getUpdatedBy())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private CommunityWhoToCallHistoryResponse toHistoryResponse(CommunityWhoToCallHistory h) {
        return CommunityWhoToCallHistoryResponse.builder()
                .id(h.getId())
                .whoToCallId(h.getWhoToCallId())
                .communityId(h.getCommunityId())
                .action(h.getAction())
                .changedByUserId(h.getChangedByUserId())
                .changedByName(h.getChangedByName())
                .department(h.getDepartment())
                .contactPerson(h.getContactPerson())
                .phoneNumber(h.getPhoneNumber())
                .changeSummary(h.getChangeSummary())
                .snapshotData(h.getSnapshotData())
                .createdAt(h.getCreatedAt())
                .build();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
