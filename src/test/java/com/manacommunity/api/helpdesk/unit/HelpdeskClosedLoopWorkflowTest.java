package com.manacommunity.api.helpdesk.unit;

import com.manacommunity.api.helpdesk.dto.HelpdeskAiDtos.AiClassificationRequest;
import com.manacommunity.api.helpdesk.dto.HelpdeskAiDtos.AiClassificationResult;
import com.manacommunity.api.helpdesk.dto.HelpdeskAiDtos.TicketReopenRequest;
import com.manacommunity.api.helpdesk.dto.HelpdeskAiDtos.TicketResolutionRequest;
import com.manacommunity.api.helpdesk.dto.TicketFeedbackRequest;
import com.manacommunity.api.helpdesk.dto.TicketRequest;
import com.manacommunity.api.helpdesk.dto.TicketResponse;
import com.manacommunity.api.helpdesk.engine.HelpdeskAiClassificationEngine;
import com.manacommunity.api.helpdesk.engine.HelpdeskAssignmentEngine;
import com.manacommunity.api.helpdesk.engine.HelpdeskSlaEngine;
import com.manacommunity.api.helpdesk.entity.Ticket;
import com.manacommunity.api.helpdesk.entity.TicketComment;
import com.manacommunity.api.helpdesk.repository.TicketCommentRepository;
import com.manacommunity.api.helpdesk.repository.TicketRepository;
import com.manacommunity.api.helpdesk.repository.TicketSlaRuleRepository;
import com.manacommunity.api.helpdesk.service.TicketService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Helpdesk Closed-Loop Workflow Unit Tests")
class HelpdeskClosedLoopWorkflowTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketCommentRepository commentRepository;

    @Mock
    private TicketSlaRuleRepository slaRuleRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private HelpdeskSlaEngine slaEngine;

    @Mock
    private HelpdeskAiClassificationEngine aiClassificationEngine;

    @Mock
    private HelpdeskAssignmentEngine assignmentEngine;

    @InjectMocks
    private TicketService ticketService;

    private AppUser resident;
    private AppUser technician;
    private Community community;

    @BeforeEach
    void setUp() {
        community = Community.builder().id(10L).name("Mana Silicon Heights").build();

        resident = AppUser.builder()
                .id(101L)
                .fullName("Alice Resident")
                .community(community)
                .role("RESIDENT")
                .build();

        technician = AppUser.builder()
                .id(202L)
                .fullName("Bob Electrician")
                .community(community)
                .role("STAFF")
                .build();
    }

    @Test
    @DisplayName("Complete Closed-Loop Lifecycle: Create -> AI Triage -> Auto Assign -> Resolve -> Reject & Reopen -> Resolve -> Resident Sign-Off")
    void testCompleteClosedLoopWorkflow() {
        // 1. Complaint & AI Triage
        TicketRequest createReq = new TicketRequest();
        createReq.setSubject("Sparking wire in corridor");
        createReq.setDescription("Electric sparks coming from switchboard outside flat 402");

        AiClassificationResult aiResult = AiClassificationResult.builder()
                .category(Ticket.TicketCategory.ELECTRICAL)
                .priority(Ticket.TicketPriority.CRITICAL)
                .urgencyScore(95)
                .emergencyHazard(true)
                .rootCauseHypothesis("Electrical arcing / short circuit detected")
                .requiredSkills(java.util.List.of("ELECTRICIAN"))
                .confidence(0.95)
                .build();

        when(aiClassificationEngine.classifyTicket(any(), any())).thenReturn(aiResult);
        when(slaRuleRepository.findByCategoryAndPriorityAndCommunityIdAndActiveTrue(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(slaRuleRepository.findByCategoryAndPriorityAndCommunityIsNullAndActiveTrue(any(), any()))
                .thenReturn(Optional.empty());
        when(slaEngine.calculateSlaDueDate(any(), any(), any()))
                .thenReturn(LocalDateTime.now().plusHours(2));
        when(assignmentEngine.recommendAssignee(Ticket.TicketCategory.ELECTRICAL, 10L))
                .thenReturn(Optional.of(technician));

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            if (t.getId() == null) {
                t.setId(1001L);
            }
            if (t.getComments() == null) {
                t.setComments(new ArrayList<>());
            }
            return t;
        });

        TicketResponse createdTicket = ticketService.create(createReq, resident, community);
        assertNotNull(createdTicket);
        assertEquals(Ticket.TicketCategory.ELECTRICAL.name(), createdTicket.getCategory());
        assertEquals(Ticket.TicketPriority.CRITICAL.name(), createdTicket.getPriority());
        assertEquals("IN_PROGRESS", createdTicket.getStatus());
        assertEquals(202L, createdTicket.getAssignedToId());
        assertEquals(95, createdTicket.getUrgencyScore());

        // 2. Staff resolves ticket with proof
        Ticket existingTicket = Ticket.builder()
                .id(1001L)
                .ticketNumber("TKT-1001")
                .subject("Sparking wire in corridor")
                .description("Electric sparks")
                .category(Ticket.TicketCategory.ELECTRICAL)
                .priority(Ticket.TicketPriority.CRITICAL)
                .status(Ticket.TicketStatus.IN_PROGRESS)
                .raisedBy(resident)
                .assignedTo(technician)
                .community(community)
                .slaDueAt(LocalDateTime.now().plusHours(2))
                .reopenCount(0)
                .comments(new ArrayList<>())
                .build();

        when(ticketRepository.findById(1001L)).thenReturn(Optional.of(existingTicket));

        TicketResolutionRequest resReq = new TicketResolutionRequest(
                "Replaced faulty MCB switch and insulated exposed wiring",
                "https://storage.mana.community/proofs/mcb-fix.jpg",
                "FIX-9901",
                "Job verified on site"
        );
        TicketResponse resolvedTicket = ticketService.resolveTicket(1001L, resReq, technician);

        assertEquals("RESOLVED", resolvedTicket.getStatus());
        assertEquals("Replaced faulty MCB switch and insulated exposed wiring", resolvedTicket.getResolutionNotes());
        assertEquals("https://storage.mana.community/proofs/mcb-fix.jpg", resolvedTicket.getResolutionProofUrl());
        assertEquals("FIX-9901", resolvedTicket.getResolutionCode());
        assertFalse(resolvedTicket.isResidentSignoff());

        // 3. Resident rejects resolution (closed-loop signoff = false)
        TicketFeedbackRequest rejectFeedback = new TicketFeedbackRequest();
        rejectFeedback.setSignOffConfirmed(false);
        rejectFeedback.setFeedbackRemarks("Cover panel is still loose and sparking sound persists");
        rejectFeedback.setSatisfactionRating(1);

        TicketResponse reopenedTicket = ticketService.submitResidentFeedback(1001L, rejectFeedback, resident);
        assertEquals("IN_PROGRESS", reopenedTicket.getStatus());
        assertEquals(1, reopenedTicket.getReopenCount());
        assertFalse(reopenedTicket.isResidentSignoff());

        // 4. Staff re-fixes and resident confirms with 5-star rating
        TicketFeedbackRequest acceptFeedback = new TicketFeedbackRequest();
        acceptFeedback.setSignOffConfirmed(true);
        acceptFeedback.setFeedbackRemarks("Fixed properly now, tested and working safely");
        acceptFeedback.setSatisfactionRating(5);

        TicketResponse closedTicket = ticketService.submitResidentFeedback(1001L, acceptFeedback, resident);
        assertEquals("CLOSED", closedTicket.getStatus());
        assertTrue(closedTicket.isResidentSignoff());
        assertEquals(5, closedTicket.getSatisfactionRating());
    }
}
