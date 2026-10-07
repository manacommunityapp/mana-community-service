package com.manacommunity.api.helpdesk.dto;

import com.manacommunity.api.helpdesk.entity.Ticket.TicketCategory;
import com.manacommunity.api.helpdesk.entity.Ticket.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.List;

public final class HelpdeskAiDtos {

    private HelpdeskAiDtos() {
    }

    public record AiClassificationRequest(
            @NotBlank(message = "Subject is required")
            String subject,
            @NotBlank(message = "Description is required")
            String description,
            Long communityId
    ) {}

    @Builder
    public record AiClassificationResult(
            TicketCategory category,
            TicketPriority priority,
            int urgencyScore,
            List<String> requiredSkills,
            String rootCauseHypothesis,
            double confidence,
            boolean emergencyHazard
    ) {
        public boolean isSafetyHazard() {
            return emergencyHazard;
        }

        public TicketPriority suggestedPriority() {
            return priority;
        }

        public String toJson() {
            return String.format(
                    "{\"category\":\"%s\",\"priority\":\"%s\",\"urgencyScore\":%d,\"emergencyHazard\":%b,\"confidence\":%.2f}",
                    category != null ? category.name() : "GENERAL",
                    priority != null ? priority.name() : "MEDIUM",
                    urgencyScore,
                    emergencyHazard,
                    confidence
            );
        }
    }

    public record TicketResolutionRequest(
            @NotBlank(message = "Resolution notes are required")
            String notes,
            String proofAttachmentUrl,
            String resolutionCode,
            String remarks
    ) {
        public String resolutionNotes() {
            return notes;
        }

        public String resolutionProofUrl() {
            return proofAttachmentUrl;
        }
    }

    public record TicketReopenRequest(
            @NotBlank(message = "Reason for reopening is required")
            String reason,
            String remarks
    ) {}
}
