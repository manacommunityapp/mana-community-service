package com.manacommunity.api.service;

import com.manacommunity.api.dto.CommunityLeaderRequest;
import com.manacommunity.api.dto.CommunityLeaderResponse;
import com.manacommunity.api.exception.DuplicateResourceException;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.CommunityLeader;
import com.manacommunity.api.repository.CommunityLeaderRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityDirectoryService {

    private final CommunityLeaderRepository leaderRepo;
    private final CommunityRepository communityRepo;
    private final AppUserRepository userRepo;

    @Transactional(readOnly = true)
    public List<CommunityLeaderResponse> getActiveLeaders(Long communityId) {
        return leaderRepo
                .findByCommunityIdAndIsActiveTrueOrderByDisplayOrderAsc(communityId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommunityLeaderResponse> getAllLeaders(Long communityId) {
        return leaderRepo
                .findByCommunityIdOrderByDisplayOrderAsc(communityId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CommunityLeaderResponse getLeaderById(Long id) {
        CommunityLeader leader = leaderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommunityLeader", "id", id.toString()));
        return toResponse(leader);
    }

    @Transactional(readOnly = true)
    public Map<String, List<CommunityLeaderResponse>> getGroupedByCommittee(Long communityId) {
        return leaderRepo
                .findByCommunityIdAndIsActiveTrueOrderByDisplayOrderAsc(communityId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.groupingBy(
                        l -> l.committee() != null ? l.committee() : "Executive"
                ));
    }

    @Transactional(readOnly = true)
    public DirectoryStats getStats(Long communityId) {
        List<CommunityLeader> leaders = leaderRepo
                .findByCommunityIdAndIsActiveTrueOrderByDisplayOrderAsc(communityId);

        long executiveCount = leaders.stream()
                .filter(l -> l.getCommittee() == null || l.getCommittee().isBlank())
                .count();

        long committeeCount = leaders.stream()
                .filter(l -> l.getCommittee() != null && !l.getCommittee().isBlank())
                .map(CommunityLeader::getCommittee)
                .distinct()
                .count();

        long memberCount = leaders.size();

        return new DirectoryStats(memberCount, executiveCount, committeeCount);
    }

    @Transactional
    public CommunityLeaderResponse addLeader(Long communityId, CommunityLeaderRequest req) {
        if (leaderRepo.existsByCommunityIdAndUserIdAndDesignation(
                communityId, req.userId(), req.designation())) {
            throw new DuplicateResourceException(
                    "CommunityLeader", "designation", req.designation());
        }

        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", "id", communityId.toString()));

        AppUser targetUser = userRepo.findById(req.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", req.userId().toString()));

        if (targetUser.getCommunity() == null ||
                !targetUser.getCommunity().getId().equals(communityId)) {
            throw new InvalidInputException("User does not belong to this community.");
        }

        CommunityLeader leader = CommunityLeader.builder()
                .community(community)
                .user(targetUser)
                .designation(req.designation())
                .committee(req.committee())
                .contactPhone(req.contactPhone() != null ? req.contactPhone() : targetUser.getPhone())
                .contactEmail(req.contactEmail() != null ? req.contactEmail() : targetUser.getEmail())
                .displayOrder(req.displayOrder() != null ? req.displayOrder() : 0)
                .build();

        leader = leaderRepo.save(leader);
        log.info("Added community leader: {} as {} (community={})",
                targetUser.getFullName(), req.designation(), communityId);
        return toResponse(leader);
    }

    @Transactional
    public CommunityLeaderResponse updateLeader(Long id, CommunityLeaderRequest req) {
        CommunityLeader leader = leaderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommunityLeader", "id", id.toString()));

        if (!leader.getDesignation().equals(req.designation()) &&
                leaderRepo.existsByCommunityIdAndUserIdAndDesignation(
                        leader.getCommunity().getId(), leader.getUser().getId(), req.designation())) {
            throw new DuplicateResourceException(
                    "CommunityLeader", "designation", req.designation());
        }

        leader.setDesignation(req.designation());
        leader.setCommittee(req.committee());
        leader.setDisplayOrder(req.displayOrder() != null ? req.displayOrder() : leader.getDisplayOrder());
        if (req.contactPhone() != null) leader.setContactPhone(req.contactPhone());
        if (req.contactEmail() != null) leader.setContactEmail(req.contactEmail());

        leader = leaderRepo.save(leader);
        log.info("Updated community leader id={}: designation={}", id, req.designation());
        return toResponse(leader);
    }

    @Transactional
    public void removeLeader(Long id) {
        CommunityLeader leader = leaderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommunityLeader", "id", id.toString()));
        leader.setIsActive(false);
        leaderRepo.save(leader);
        log.info("Soft-deleted community leader id={} ({})", id, leader.getDesignation());
    }

    @Transactional
    public CommunityLeaderResponse restoreLeader(Long id) {
        CommunityLeader leader = leaderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CommunityLeader", "id", id.toString()));
        leader.setIsActive(true);
        leader = leaderRepo.save(leader);
        log.info("Restored community leader id={} ({})", id, leader.getDesignation());
        return toResponse(leader);
    }

    @Transactional
    public void reorder(Long communityId, List<Long> orderedIds) {
        List<CommunityLeader> leaders = leaderRepo
                .findByCommunityIdAndIsActiveTrueOrderByDisplayOrderAsc(communityId);

        Map<Long, CommunityLeader> byId = leaders.stream()
                .collect(Collectors.toMap(CommunityLeader::getId, l -> l));

        int order = 1;
        for (Long lid : orderedIds) {
            CommunityLeader l = byId.get(lid);
            if (l != null) {
                l.setDisplayOrder(order++);
            }
        }
        leaderRepo.saveAll(leaders);
        log.info("Reordered {} leaders for community {}", orderedIds.size(), communityId);
    }

    private CommunityLeaderResponse toResponse(CommunityLeader l) {
        AppUser u = l.getUser();
        return new CommunityLeaderResponse(
                l.getId(),
                u.getId(),
                u.getFullName(),
                u.getProfilePicUrl(),
                l.getDesignation(),
                l.getCommittee(),
                l.getContactPhone(),
                l.getContactEmail(),
                u.getFlatNo(),
                u.getBlock(),
                l.getDisplayOrder()
        );
    }

    public record DirectoryStats(long totalMembers, long executiveCount, long committeeCount) {}
}
