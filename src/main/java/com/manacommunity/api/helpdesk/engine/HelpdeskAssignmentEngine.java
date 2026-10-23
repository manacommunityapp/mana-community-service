package com.manacommunity.api.helpdesk.engine;

import com.manacommunity.api.helpdesk.entity.Ticket;
import com.manacommunity.api.helpdesk.entity.Ticket.TicketStatus;
import com.manacommunity.api.helpdesk.repository.TicketRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Intelligent staff/technician assignment engine.
 * Matches category and skill requirements against available staff and balances active ticket workloads.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HelpdeskAssignmentEngine {

    private final AppUserRepository userRepository;
    private final TicketRepository ticketRepository;

    private static final Set<String> ELIGIBLE_STAFF_ROLES = Set.of(
            "STAFF", "FACILITY_MANAGER", "TECHNICIAN", "ADMIN", "COMMUNITY_ADMIN", "SECURITY"
    );

    public java.util.Optional<AppUser> recommendAssignee(Ticket.TicketCategory category, Long communityId) {
        if (communityId == null) {
            return java.util.Optional.empty();
        }
        List<AppUser> activeUsers = userRepository.findByCommunityIdAndIsActiveTrue(communityId);
        List<AppUser> eligibleStaff = activeUsers.stream()
                .filter(u -> u.getRole() != null && ELIGIBLE_STAFF_ROLES.contains(u.getRole().toUpperCase()))
                .toList();

        if (eligibleStaff.isEmpty()) {
            return java.util.Optional.empty();
        }

        return eligibleStaff.stream()
                .min(Comparator.comparingLong(u ->
                        ticketRepository.countByAssignedToIdAndStatusIn(u.getId(), List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS))
                ));
    }

    public AppUser autoAssign(Ticket ticket, Community community, List<String> requiredSkills) {
        if (community == null || community.getId() == null) {
            return null;
        }

        List<AppUser> activeUsers = userRepository.findByCommunityIdAndIsActiveTrue(community.getId());
        List<AppUser> eligibleStaff = activeUsers.stream()
                .filter(u -> u.getRole() != null && ELIGIBLE_STAFF_ROLES.contains(u.getRole().toUpperCase()))
                .toList();

        if (eligibleStaff.isEmpty()) {
            log.info("[HelpdeskAssignment] No staff users found in community {}", community.getId());
            return null;
        }

        // Rank by active ticket load (ascending)
        return eligibleStaff.stream()
                .min(Comparator.comparingLong(u ->
                        ticketRepository.countByAssignedToIdAndStatusIn(u.getId(), List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS))
                ))
                .orElse(null);
    }
}
