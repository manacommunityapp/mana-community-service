package com.manacommunity.api.helpdesk.service;

import com.manacommunity.api.helpdesk.dto.TicketRequest;
import com.manacommunity.api.helpdesk.dto.TicketResponse;
import com.manacommunity.api.helpdesk.entity.Ticket;
import com.manacommunity.api.helpdesk.entity.TicketComment;
import com.manacommunity.api.helpdesk.repository.TicketRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository repo;
    private final AppUserRepository userRepo;

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
    public TicketResponse getById(Long id) {
        return toResponse(repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + id)));
    }

    @Transactional
    public TicketResponse create(TicketRequest req, AppUser user, Community community) {
        Ticket ticket = Ticket.builder()
                .ticketNumber(generateTicketNumber())
                .subject(req.getSubject())
                .description(req.getDescription())
                .category(parseEnumOrDefault(Ticket.TicketCategory.class, req.getCategory(), Ticket.TicketCategory.GENERAL))
                .priority(parseEnumOrDefault(Ticket.TicketPriority.class, req.getPriority(), Ticket.TicketPriority.MEDIUM))
                .raisedBy(user)
                .community(community)
                .build();

        return toResponse(repo.save(ticket));
    }

    @Transactional
    public TicketResponse updateStatus(Long id, String status, String remarks) {
        Ticket ticket = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + id));
        Ticket.TicketStatus s = Ticket.TicketStatus.valueOf(status);
        ticket.setStatus(s);
        if (remarks != null) ticket.setAdminRemarks(remarks);
        if (s == Ticket.TicketStatus.RESOLVED || s == Ticket.TicketStatus.CLOSED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        return toResponse(repo.save(ticket));
    }

    @Transactional
    public TicketResponse assign(Long id, Long assigneeId) {
        Ticket ticket = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + id));
        AppUser assignee = userRepo.findById(assigneeId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + assigneeId));
        ticket.setAssignedTo(assignee);
        if (ticket.getStatus() == Ticket.TicketStatus.OPEN) {
            ticket.setStatus(Ticket.TicketStatus.IN_PROGRESS);
        }
        return toResponse(repo.save(ticket));
    }

    @Transactional
    public TicketResponse addComment(Long ticketId, String message, AppUser author) {
        Ticket ticket = repo.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
        TicketComment comment = TicketComment.builder()
                .message(message)
                .ticket(ticket)
                .author(author)
                .build();
        ticket.getComments().add(comment);
        return toResponse(repo.save(ticket));
    }

    private String generateTicketNumber() {
        return "TKT-" + ThreadLocalRandom.current().nextInt(100000, 999999);
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
                .resolvedAt(formatDt(t.getResolvedAt()))
                .createdAt(formatDt(t.getCreatedAt()))
                .updatedAt(formatDt(t.getUpdatedAt()))
                .comments(t.getComments() != null
                        ? t.getComments().stream().map(c -> TicketResponse.CommentDto.builder()
                                .id(c.getId())
                                .message(c.getMessage())
                                .authorId(c.getAuthor().getId())
                                .authorName(c.getAuthor().getFullName())
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
