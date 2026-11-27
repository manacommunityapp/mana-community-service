package com.manacommunity.api.helpdesk.service;

import com.manacommunity.api.helpdesk.entity.HelpdeskTicket;
import com.manacommunity.api.helpdesk.repository.HelpdeskTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DuplicateClusteringService {

    private final HelpdeskTicketRepository ticketRepository;

    private static final Duration CLUSTER_WINDOW = Duration.ofMinutes(15);
    private static final int CLUSTER_THRESHOLD = 3;
    private static final double SIMILARITY_THRESHOLD = 0.4;

    @Transactional
    public ClusterResult detectAndCluster(HelpdeskTicket newTicket) {
        Instant windowStart = Instant.now().minus(CLUSTER_WINDOW);
        List<HelpdeskTicket> recent = ticketRepository.findRecentSimilarTickets(
                newTicket.getCommunityId(), newTicket.getCategory(),
                newTicket.getTower(), windowStart);

        HelpdeskTicket existingParent = recent.stream()
                .filter(HelpdeskTicket::getIsParentIncident)
                .findFirst().orElse(null);

        if (existingParent != null) {
            double sim = computeSimilarity(newTicket.getTitle(), existingParent.getTitle());
            if (sim >= SIMILARITY_THRESHOLD) {
                newTicket.setParentIncidentId(existingParent.getId());
                ticketRepository.save(newTicket);

                existingParent.setChildCount(existingParent.getChildCount() + 1);
                ticketRepository.save(existingParent);

                log.info("Ticket {} clustered under parent incident {} (similarity {:.2f})",
                        newTicket.getId(), existingParent.getId(), sim);
                return new ClusterResult(true, existingParent.getId(), existingParent.getChildCount(),
                        "Merged into existing incident: " + existingParent.getTitle());
            }
        }

        List<HelpdeskTicket> similar = recent.stream()
                .filter(t -> !t.getId().equals(newTicket.getId()))
                .filter(t -> t.getParentIncidentId() == null)
                .filter(t -> computeSimilarity(newTicket.getTitle(), t.getTitle()) >= SIMILARITY_THRESHOLD)
                .collect(Collectors.toList());

        if (similar.size() + 1 >= CLUSTER_THRESHOLD) {
            HelpdeskTicket parent = similar.get(0);
            parent.setIsParentIncident(true);
            parent.setChildCount(similar.size()); // others + newTicket, minus parent itself

            for (int i = 1; i < similar.size(); i++) {
                similar.get(i).setParentIncidentId(parent.getId());
                ticketRepository.save(similar.get(i));
            }
            newTicket.setParentIncidentId(parent.getId());
            ticketRepository.save(newTicket);

            parent.setChildCount(similar.size()); // recalculate
            ticketRepository.save(parent);

            log.info("New parent incident {} created with {} child tickets for '{}'",
                    parent.getId(), similar.size(), parent.getTitle());
            return new ClusterResult(true, parent.getId(), similar.size(),
                    "New incident cluster created: " + parent.getTitle());
        }

        return new ClusterResult(false, null, 0, "No cluster match found");
    }

    double computeSimilarity(String a, String b) {
        if (a == null || b == null) return 0;

        Set<String> tokensA = tokenize(a);
        Set<String> tokensB = tokenize(b);

        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0;

        Set<String> intersection = new HashSet<>(tokensA);
        intersection.retainAll(tokensB);

        Set<String> union = new HashSet<>(tokensA);
        union.addAll(tokensB);

        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+"))
                .filter(s -> s.length() > 2)
                .collect(Collectors.toSet());
    }

    public record ClusterResult(boolean clustered, Long parentIncidentId, int clusterSize, String message) {}
}
