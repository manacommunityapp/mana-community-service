package com.manacommunity.api.helpdesk.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.helpdesk.dto.HelpdeskAiDtos.*;
import com.manacommunity.api.helpdesk.dto.HelpdeskAnalyticsResponse;
import com.manacommunity.api.helpdesk.dto.TicketFeedbackRequest;
import com.manacommunity.api.helpdesk.dto.TicketRequest;
import com.manacommunity.api.helpdesk.dto.TicketResponse;
import com.manacommunity.api.helpdesk.engine.HelpdeskAiClassificationEngine;
import com.manacommunity.api.helpdesk.engine.HelpdeskAssignmentEngine;
import com.manacommunity.api.helpdesk.engine.HelpdeskSlaEngine;
import com.manacommunity.api.helpdesk.entity.Ticket;
import com.manacommunity.api.helpdesk.entity.TicketComment;
import com.manacommunity.api.helpdesk.entity.TicketSlaRule;
import com.manacommunity.api.helpdesk.repository.TicketCommentRepository;
import com.manacommunity.api.helpdesk.repository.TicketRepository;
import com.manacommunity.api.helpdesk.repository.TicketSlaRuleRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository repo;
    private final TicketCommentRepository commentRepository;
    private final TicketSlaRuleRepository slaRuleRepository;
    private final AppUserRepository userRepo;
    private final HelpdeskSlaEngine slaEngine;
    private final HelpdeskAiClassificationEngine aiClassificationEngine;
    private final HelpdeskAssignmentEngine assignmentEngine;

    @Transactional(readOnly = true)
    public List<TicketResponse> getCommunityTickets(Long communityId, String statusFilter) {
        if (statusFilter != null && !"All".equalsIgnoreCase(statusFilter)) {
            Ticket.TicketStatus s = parseEnum(Ticket.TicketStatus.class, statusFilter);
            if (s != null) {
                return repo.findByCommunityAndStatuses(communityId, List.of(s))
                        .stream().map(this::toResponse).toList();
            }
        }
        return repo.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getOpenTickets(Long communityId) {
        return repo.findByCommunityAndStatuses(communityId,
                        List.of(Ticket.TicketStatus.OPEN, Ticket.TicketStatus.IN_PROGRESS))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getMyTickets(Long userId) {
        return repo.findByRaisedByIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getById(Long id, AppUser currentUser) {
        Ticket ticket = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));

        assertSameCommunity(ticket.getCommunity(), currentUser);

        String role = currentUser.getRole() != null ? currentUser.getRole().toUpperCase() : "";
        boolean isManager = Set.of("ADMIN", "SUPER_ADMIN", "COMMUNITY_ADMIN", "SPORTS_ADMIN").contains(role);
        if (!isManager && !ticket.getRaisedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only view your own tickets.");
        }

        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse create(TicketRequest req, AppUser user, Community community) {
        // AI Triage and Safety Classification
        AiClassificationResult aiResult = aiClassificationEngine.classifyTicket(req.getSubject(), req.getDescription());

        Ticket.TicketCategory category;
        if (req.getCategory() != null && !req.getCategory().isBlank()) {
            category = parseEnumOrDefault(Ticket.TicketCategory.class, req.getCategory(), aiResult.category());
        } else {
            category = aiResult.category();
        }

        // If safety hazard detected, auto-override priority to CRITICAL
        Ticket.TicketPriority priority;
        if (aiResult.isSafetyHazard()) {
            priority = Ticket.TicketPriority.CRITICAL;
        } else if (req.getPriority() != null && !req.getPriority().isBlank()) {
            priority = parseEnumOrDefault(Ticket.TicketPriority.class, req.getPriority(), aiResult.suggestedPriority());
        } else {
            priority = aiResult.suggestedPriority();
        }

        TicketSlaRule rule = slaRuleRepository.findByCategoryAndPriorityAndCommunityIdAndActiveTrue(
                category, priority, community != null ? community.getId() : null)
                .or(() -> slaRuleRepository.findByCategoryAndPriorityAndCommunityIsNullAndActiveTrue(category, priority))
                .orElse(null);

        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime slaDueAt = slaEngine.calculateSlaDueDate(createdAt, priority, rule);

        // Automated Technician Assignment if applicable
        AppUser assignee = null;
        Ticket.TicketStatus initialStatus = Ticket.TicketStatus.OPEN;
        if (community != null) {
            assignee = assignmentEngine.recommendAssignee(category, community.getId()).orElse(null);
            if (assignee != null) {
                initialStatus = Ticket.TicketStatus.IN_PROGRESS;
            }
        }

        Ticket ticket = Ticket.builder()
                .ticketNumber(generateTicketNumber())
                .subject(HtmlSanitizer.sanitizePlainText(req.getSubject()))
                .description(HtmlSanitizer.sanitizeRichText(req.getDescription()))
                .category(category)
                .priority(priority)
                .status(initialStatus)
                .raisedBy(user)
                .assignedTo(assignee)
                .community(community)
                .slaDueAt(slaDueAt)
                .slaStatus(Ticket.SlaStatus.ON_TRACK)
                .urgencyScore(aiResult.urgencyScore())
                .aiClassificationJson(aiResult.toJson())
                .attachments(req.getAttachments())
                .escalationLevel(0)
                .escalated(false)
                .reopenCount(0)
                .residentSignoff(false)
                .build();

        return toResponse(repo.save(ticket));
    }

    @Transactional(readOnly = true)
    public AiClassificationResult classifyAi(AiClassificationRequest req) {
        return aiClassificationEngine.classifyTicket(req.subject(), req.description());
    }

    @Transactional
    public TicketResponse resolveTicket(Long id, TicketResolutionRequest req, AppUser currentUser) {
        Ticket ticket = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        assertSameCommunity(ticket.getCommunity(), currentUser);

        ticket.setStatus(Ticket.TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());
        ticket.setResolutionNotes(HtmlSanitizer.sanitizeRichText(req.resolutionNotes()));
        ticket.setResolutionProofUrl(req.resolutionProofUrl());
        ticket.setResolutionCode(req.resolutionCode());
        if (req.remarks() != null && !req.remarks().isBlank()) {
            ticket.setAdminRemarks(HtmlSanitizer.sanitizePlainText(req.remarks()));
        }

        // Add audit comment
        TicketComment resolutionComment = TicketComment.builder()
                .ticket(ticket)
                .author(currentUser)
                .message("✅ Ticket resolved: " + (req.resolutionNotes() != null ? req.resolutionNotes() : "Work completed"))
                .build();
        commentRepository.save(resolutionComment);

        return toResponse(repo.save(ticket));
    }

    @Transactional
    public TicketResponse reopenTicket(Long id, TicketReopenRequest req, AppUser currentUser) {
        Ticket ticket = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        assertSameCommunity(ticket.getCommunity(), currentUser);

        ticket.setStatus(Ticket.TicketStatus.IN_PROGRESS);
        ticket.setReopenCount(ticket.getReopenCount() + 1);
        ticket.setResidentSignoff(false);

        // Reset resolvedAt
        ticket.setResolvedAt(null);

        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .author(currentUser)
                .message("🔄 Ticket reopened (Reopen #" + ticket.getReopenCount() + "): " + req.reason())
                .build();
        commentRepository.save(comment);

        return toResponse(repo.save(ticket));
    }

    @Transactional
    public TicketResponse updateStatus(Long id, String status, String remarks, AppUser currentUser) {
        Ticket ticket = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        assertSameCommunity(ticket.getCommunity(), currentUser);
        Ticket.TicketStatus s = Ticket.TicketStatus.valueOf(status);
        ticket.setStatus(s);
        if (remarks != null) ticket.setAdminRemarks(remarks);
        if (s == Ticket.TicketStatus.RESOLVED || s == Ticket.TicketStatus.CLOSED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        return toResponse(repo.save(ticket));
    }

    @Transactional
    public TicketResponse submitResidentFeedback(Long ticketId, TicketFeedbackRequest feedback, AppUser currentUser) {
        Ticket ticket = repo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        assertSameCommunity(ticket.getCommunity(), currentUser);

        if (!ticket.getRaisedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the resident who raised the ticket can submit sign-off feedback.");
        }

        ticket.setSatisfactionRating(feedback.getSatisfactionRating());
        ticket.setFeedbackRemarks(feedback.getFeedbackRemarks());
        ticket.setResidentSignoff(feedback.isSignOffConfirmed());
        ticket.setResidentSignoffAt(LocalDateTime.now());

        if (feedback.isSignOffConfirmed()) {
            ticket.setStatus(Ticket.TicketStatus.CLOSED);
            TicketComment signoffComment = TicketComment.builder()
                    .ticket(ticket)
                    .author(currentUser)
                    .message("🎉 Resident verified and signed off. Ticket closed. Rating: " + feedback.getSatisfactionRating() + "/5")
                    .build();
            commentRepository.save(signoffComment);
        } else {
            // Reopen ticket if resident is not satisfied
            ticket.setStatus(Ticket.TicketStatus.IN_PROGRESS);
            ticket.setReopenCount(ticket.getReopenCount() + 1);
            ticket.setResolvedAt(null);
            TicketComment rejectionComment = TicketComment.builder()
                    .ticket(ticket)
                    .author(currentUser)
                    .message("⚠️ Resident rejected resolution (Reopen #" + ticket.getReopenCount() + "): " +
                            (feedback.getFeedbackRemarks() != null ? feedback.getFeedbackRemarks() : "Unsatisfactory resolution"))
                    .build();
            commentRepository.save(rejectionComment);
        }

        return toResponse(repo.save(ticket));
    }

    @Transactional
    public TicketResponse assign(Long id, Long assigneeId, AppUser currentUser) {
        Ticket ticket = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        assertSameCommunity(ticket.getCommunity(), currentUser);
        AppUser assignee = userRepo.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", assigneeId));
        ticket.setAssignedTo(assignee);
        if (ticket.getStatus() == Ticket.TicketStatus.OPEN) {
            ticket.setStatus(Ticket.TicketStatus.IN_PROGRESS);
        }
        return toResponse(repo.save(ticket));
    }

    @Transactional
    public TicketResponse addComment(Long ticketId, String message, AppUser author) {
        Ticket ticket = repo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        assertSameCommunity(ticket.getCommunity(), author);
        TicketComment comment = TicketComment.builder()
                .message(HtmlSanitizer.sanitizeRichText(message))
                .ticket(ticket)
                .author(author)
                .build();
        ticket.getComments().add(comment);
        return toResponse(repo.save(ticket));
    }

    @Transactional
    public TicketResponse checkAndEscalate(Long ticketId) {
        Ticket ticket = repo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        final Ticket.TicketCategory category = ticket.getCategory();
        final Ticket.TicketPriority priority = ticket.getPriority();
        final Long communityId = ticket.getCommunity() != null ? ticket.getCommunity().getId() : null;

        TicketSlaRule rule = slaRuleRepository.findByCategoryAndPriorityAndCommunityIdAndActiveTrue(
                category, priority, communityId)
                .or(() -> slaRuleRepository.findByCategoryAndPriorityAndCommunityIsNullAndActiveTrue(category, priority))
                .orElse(null);

        int targetLevel = slaEngine.evaluateEscalationLevel(ticket, LocalDateTime.now(), rule);
        if (targetLevel > ticket.getEscalationLevel()) {
            ticket.setEscalated(true);
            ticket.setEscalationLevel(targetLevel);
            ticket.setEscalatedAt(LocalDateTime.now());
            ticket.setLastEscalatedAt(LocalDateTime.now());
            ticket = repo.save(ticket);
        }

        return toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public HelpdeskAnalyticsResponse getAnalytics(Long communityId) {
        List<Ticket> tickets = repo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        return slaEngine.calculateAnalytics(tickets);
    }

    private String generateTicketNumber() {
        return "TKT-" + ThreadLocalRandom.current().nextInt(100000, 999999);
    }

    private void assertSameCommunity(Community ticketCommunity, AppUser user) {
        Long userCommunityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        Long ticketCommunityId = ticketCommunity != null ? ticketCommunity.getId() : null;
        if (ticketCommunityId == null || !ticketCommunityId.equals(userCommunityId)) {
            throw new AccessDeniedException("Access denied: resource belongs to a different community.");
        }
    }

    private TicketResponse toResponse(Ticket t) {
        return TicketResponse.builder()
                .id(t.getId())
                .ticketNumber(t.getTicketNumber())
                .subject(t.getSubject())
                .description(t.getDescription())
                .category(t.getCategory().name())
                .priority(t.getPriority().name())
                .status(t.getStatus().name())
                .adminRemarks(t.getAdminRemarks())
                .raisedById(t.getRaisedBy().getId())
                .raisedByName(t.getRaisedBy().getFullName())
                .assignedToId(t.getAssignedTo() != null ? t.getAssignedTo().getId() : null)
                .assignedToName(t.getAssignedTo() != null ? t.getAssignedTo().getFullName() : null)
                .communityId(t.getCommunity() != null ? t.getCommunity().getId() : null)
                .slaDueAt(formatDt(t.getSlaDueAt()))
                .slaStatus(t.getSlaStatus() != null ? t.getSlaStatus().name() : null)
                .urgencyScore(t.getUrgencyScore())
                .aiClassificationJson(t.getAiClassificationJson())
                .resolutionNotes(t.getResolutionNotes())
                .resolutionProofUrl(t.getResolutionProofUrl())
                .resolutionCode(t.getResolutionCode())
                .reopenCount(t.getReopenCount())
                .isEscalated(t.isEscalated())
                .escalatedAt(formatDt(t.getEscalatedAt()))
                .escalationLevel(t.getEscalationLevel())
                .satisfactionRating(t.getSatisfactionRating())
                .feedbackRemarks(t.getFeedbackRemarks())
                .residentSignoff(t.isResidentSignoff())
                .residentSignoffAt(formatDt(t.getResidentSignoffAt()))
                .attachments(t.getAttachments())
                .resolvedAt(formatDt(t.getResolvedAt()))
                .createdAt(formatDt(t.getCreatedAt()))
                .updatedAt(formatDt(t.getUpdatedAt()))
                .comments(t.getComments() != null
                        ? t.getComments().stream().map(c -> TicketResponse.CommentDto.builder()
                                .id(c.getId())
                                .message(c.getMessage())
                                .authorId(c.getAuthor() != null ? c.getAuthor().getId() : null)
                                .authorName(c.getAuthor() != null ? c.getAuthor().getFullName() : "System")
                                .createdAt(formatDt(c.getCreatedAt()))
                                .build()).toList()
                        : List.of())
                .build();
    }

    private String formatDt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null || value.isBlank()) return null;
        try { return Enum.valueOf(enumClass, value); }
        catch (IllegalArgumentException e) { return null; }
    }

    private <E extends Enum<E>> E parseEnumOrDefault(Class<E> enumClass, String value, E def) {
        E r = parseEnum(enumClass, value);
        return r != null ? r : def;
    }
}
