package com.manacommunity.api.sportsops.service;

import com.manacommunity.api.sportsops.entity.RefereeAssignment;
import com.manacommunity.api.sportsops.repository.RefereeAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefereeService {
    private final RefereeAssignmentRepository repo;

    public List<RefereeAssignment> listByCommunity(Long communityId) {
        return repo.findByCommunityId(communityId);
    }

    public List<RefereeAssignment> listByReferee(Long communityId, Long refereeId) {
        return repo.findByCommunityIdAndRefereeId(communityId, refereeId);
    }

    @Transactional
    public RefereeAssignment assign(RefereeAssignment assignment) {
        return repo.save(assignment);
    }

    @Transactional
    public RefereeAssignment confirm(Long communityId, Long id) {
        RefereeAssignment a = repo.findById(id)
            .filter(x -> x.getCommunityId().equals(communityId))
            .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));
        a.setConfirmed(true);
        a.setConfirmedAt(Instant.now());
        return repo.save(a);
    }
}
