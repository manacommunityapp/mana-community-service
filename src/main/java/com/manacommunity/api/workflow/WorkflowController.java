package com.manacommunity.api.workflow;

import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.workflow.dto.AllowedTransitionDto;
import com.manacommunity.api.workflow.dto.TransitionHistoryDto;
import com.manacommunity.api.workflow.dto.WorkflowTransitionRequest;
import com.manacommunity.api.workflow.dto.WorkflowTransitionResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowEngine workflowEngine;

    /** Get allowed next transitions for the current user's role. */
    @GetMapping("/{workflowId}/{entityId}/transitions")
    public ResponseEntity<List<AllowedTransitionDto>> getTransitions(
            @PathVariable String workflowId,
            @PathVariable String entityId,
            @RequestParam String currentStatus,
            @AuthenticationPrincipal UserPrincipal principal) {
        String role = extractPrimaryRole(principal);
        return ResponseEntity.ok(workflowEngine.getAllowedTransitions(workflowId, currentStatus, role));
    }

    /** Get transition history for an entity. */
    @GetMapping("/{entityType}/{entityId}/history")
    public ResponseEntity<List<TransitionHistoryDto>> getHistory(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workflowEngine.getHistory(entityType, entityId));
    }

    /**
     * Execute a workflow transition.
     * Most module-specific transitions go through module controllers; this generic endpoint
     * is for direct workflow-driven actions (e.g. admin resolving a maintenance ticket).
     */
    @PostMapping("/{workflowId}/{entityId}/transition")
    public ResponseEntity<WorkflowTransitionResult> executeTransition(
            @PathVariable String workflowId,
            @PathVariable String entityId,
            @RequestParam String currentStatus,
            @RequestBody @Valid WorkflowTransitionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        WorkflowTransitionResult result = workflowEngine.transition(
                workflowId,
                workflowId,   // entityType defaults to workflowId for the generic endpoint
                entityId,
                principal.getCommunityId(),
                currentStatus,
                request.toStatus(),
                principal.getUserId(),
                extractPrimaryRole(principal),
                request.reason(),
                request.idempotencyKey(),
                null
        );
        return ResponseEntity.ok(result);
    }

    /**
     * Extract the user's primary application role (without the ROLE_ prefix).
     * Falls back to AppUser.role when available, otherwise takes the first GrantedAuthority.
     */
    private String extractPrimaryRole(UserPrincipal principal) {
        if (principal.getUser() != null && principal.getUser().getRole() != null) {
            return principal.getUser().getRole();
        }
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .findFirst()
                .orElse("RESIDENT");
    }
}
