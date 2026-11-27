package com.manacommunity.api.patrol.repository;

import com.manacommunity.api.patrol.entity.PatrolBreachIncident;
import com.manacommunity.api.patrol.entity.PatrolCheckpointNode;
import com.manacommunity.api.patrol.entity.PatrolScanAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatrolRepositories {

    interface PatrolCheckpointNodeRepository extends JpaRepository<PatrolCheckpointNode, Long> {
        List<PatrolCheckpointNode> findByCommunityIdAndRouteNameAndIsActiveTrueOrderBySequenceOrderAsc(
                Long communityId, String routeName);

        Optional<PatrolCheckpointNode> findByCommunityIdAndRouteNameAndSequenceOrder(
                Long communityId, String routeName, Integer sequenceOrder);
    }

    interface PatrolScanAuditRepository extends JpaRepository<PatrolScanAudit, Long> {
        Optional<PatrolScanAudit> findFirstByGuardUser_IdOrderByServerReceivedAtDesc(Long guardUserId);
        List<PatrolScanAudit> findByCommunityIdAndShiftIdOrderByServerReceivedAtAsc(Long communityId, Long shiftId);
    }

    interface PatrolBreachIncidentRepository extends JpaRepository<PatrolBreachIncident, Long> {
        List<PatrolBreachIncident> findByCommunityIdAndEscalationStatus(Long communityId, String escalationStatus);
    }
}
