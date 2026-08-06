package com.manacommunity.api.booking.service;

import com.manacommunity.api.booking.dto.*;
import com.manacommunity.api.booking.entity.ApprovalWorkflow;
import com.manacommunity.api.booking.entity.BusinessRule;
import com.manacommunity.api.booking.entity.PricingRule;
import com.manacommunity.api.booking.entity.ResourceCategory;
import com.manacommunity.api.booking.entity.Resource;
import com.manacommunity.api.booking.entity.enums.PricingType;
import com.manacommunity.api.booking.entity.enums.RuleType;
import com.manacommunity.api.booking.entity.enums.WorkflowStepType;
import com.manacommunity.api.booking.repository.ApprovalWorkflowRepository;
import com.manacommunity.api.booking.repository.BusinessRuleRepository;
import com.manacommunity.api.booking.repository.PricingRuleRepository;
import com.manacommunity.api.booking.repository.ResourceCategoryRepository;
import com.manacommunity.api.booking.repository.ResourceRepository;
import com.manacommunity.api.model.Community;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingRuleService {

    private final BusinessRuleRepository ruleRepo;
    private final PricingRuleRepository pricingRepo;
    private final ApprovalWorkflowRepository workflowRepo;
    private final ResourceRepository resourceRepo;
    private final ResourceCategoryRepository categoryRepo;

    // ── Business Rules ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BusinessRuleResponse> getRules(Long communityId, Long resourceId) {
        if (resourceId != null) {
            return ruleRepo.findByResourceIdAndIsActiveTrue(resourceId)
                    .stream().map(this::toRuleResponse).toList();
        }
        return ruleRepo.findByCommunityIdAndIsActiveTrueOrderByPriorityAsc(communityId)
                .stream().map(this::toRuleResponse).toList();
    }

    @Transactional
    public BusinessRuleResponse createRule(BusinessRuleRequest req, Community community) {
        BusinessRule rule = BusinessRule.builder()
                .ruleType(RuleType.valueOf(req.getRuleType()))
                .ruleKey(req.getRuleKey())
                .ruleValue(req.getRuleValue())
                .ruleOperator(req.getRuleOperator())
                .description(req.getDescription())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .priority(req.getPriority() != null ? req.getPriority() : 0)
                .community(community)
                .build();

        if (req.getResourceId() != null) {
            rule.setResource(resourceRepo.findById(req.getResourceId()).orElse(null));
        }
        if (req.getCategoryId() != null) {
            rule.setCategory(categoryRepo.findById(req.getCategoryId()).orElse(null));
        }
        if (req.getValidFrom() != null) rule.setValidFrom(LocalDate.parse(req.getValidFrom()));
        if (req.getValidTo() != null) rule.setValidTo(LocalDate.parse(req.getValidTo()));

        return toRuleResponse(ruleRepo.save(rule));
    }

    @Transactional
    public BusinessRuleResponse updateRule(Long id, BusinessRuleRequest req) {
        BusinessRule rule = ruleRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + id));

        rule.setRuleType(RuleType.valueOf(req.getRuleType()));
        rule.setRuleKey(req.getRuleKey());
        rule.setRuleValue(req.getRuleValue());
        rule.setRuleOperator(req.getRuleOperator());
        rule.setDescription(req.getDescription());
        if (req.getIsActive() != null) rule.setActive(req.getIsActive());
        if (req.getPriority() != null) rule.setPriority(req.getPriority());
        if (req.getValidFrom() != null) rule.setValidFrom(LocalDate.parse(req.getValidFrom()));
        if (req.getValidTo() != null) rule.setValidTo(LocalDate.parse(req.getValidTo()));

        return toRuleResponse(ruleRepo.save(rule));
    }

    @Transactional
    public void deleteRule(Long id) {
        BusinessRule rule = ruleRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + id));
        rule.setActive(false);
        ruleRepo.save(rule);
    }

    // ── Pricing Rules ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PricingRuleResponse> getPricingRules(Long communityId, Long resourceId) {
        if (resourceId != null) {
            return pricingRepo.findByResourceIdAndIsActiveTrueOrderByPricingTypeAsc(resourceId)
                    .stream().map(this::toPricingResponse).toList();
        }
        return pricingRepo.findByCommunityIdAndIsActiveTrueOrderByPricingTypeAsc(communityId)
                .stream().map(this::toPricingResponse).toList();
    }

    @Transactional
    public PricingRuleResponse createPricingRule(PricingRuleRequest req, Community community) {
        PricingRule rule = PricingRule.builder()
                .pricingType(PricingType.valueOf(req.getPricingType()))
                .amount(req.getAmount() != null ? BigDecimal.valueOf(req.getAmount()) : null)
                .percentage(req.getPercentage() != null ? BigDecimal.valueOf(req.getPercentage()) : null)
                .description(req.getDescription())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .dayOfWeek(req.getDayOfWeek())
                .community(community)
                .build();

        if (req.getResourceId() != null) {
            rule.setResource(resourceRepo.findById(req.getResourceId()).orElse(null));
        }
        if (req.getCategoryId() != null) {
            rule.setCategory(categoryRepo.findById(req.getCategoryId()).orElse(null));
        }
        if (req.getValidFrom() != null) rule.setValidFrom(LocalDate.parse(req.getValidFrom()));
        if (req.getValidTo() != null) rule.setValidTo(LocalDate.parse(req.getValidTo()));
        if (req.getStartTime() != null) rule.setStartTime(LocalTime.parse(req.getStartTime()));
        if (req.getEndTime() != null) rule.setEndTime(LocalTime.parse(req.getEndTime()));

        return toPricingResponse(pricingRepo.save(rule));
    }

    @Transactional
    public PricingRuleResponse updatePricingRule(Long id, PricingRuleRequest req) {
        PricingRule rule = pricingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing rule not found: " + id));

        rule.setPricingType(PricingType.valueOf(req.getPricingType()));
        if (req.getAmount() != null) rule.setAmount(BigDecimal.valueOf(req.getAmount()));
        if (req.getPercentage() != null) rule.setPercentage(BigDecimal.valueOf(req.getPercentage()));
        rule.setDescription(req.getDescription());
        if (req.getIsActive() != null) rule.setActive(req.getIsActive());
        rule.setDayOfWeek(req.getDayOfWeek());
        if (req.getValidFrom() != null) rule.setValidFrom(LocalDate.parse(req.getValidFrom()));
        if (req.getValidTo() != null) rule.setValidTo(LocalDate.parse(req.getValidTo()));
        if (req.getStartTime() != null) rule.setStartTime(LocalTime.parse(req.getStartTime()));
        if (req.getEndTime() != null) rule.setEndTime(LocalTime.parse(req.getEndTime()));

        return toPricingResponse(pricingRepo.save(rule));
    }

    @Transactional
    public void deletePricingRule(Long id) {
        PricingRule rule = pricingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing rule not found: " + id));
        rule.setActive(false);
        pricingRepo.save(rule);
    }

    // ── Approval Workflows ──────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ApprovalWorkflowResponse> getWorkflows(Long communityId, Long resourceId) {
        if (resourceId != null) {
            return workflowRepo.findByResourceIdAndIsActiveTrueOrderByStepOrderAsc(resourceId)
                    .stream().map(this::toWorkflowResponse).toList();
        }
        return List.of();
    }

    // ── Mappers ─────────────────────────────────────────────────

    private BusinessRuleResponse toRuleResponse(BusinessRule r) {
        return BusinessRuleResponse.builder()
                .id(r.getId())
                .resourceId(r.getResource() != null ? r.getResource().getId() : null)
                .categoryId(r.getCategory() != null ? r.getCategory().getId() : null)
                .ruleType(r.getRuleType())
                .ruleKey(r.getRuleKey())
                .ruleValue(r.getRuleValue())
                .ruleOperator(r.getRuleOperator())
                .description(r.getDescription())
                .isActive(r.isActive())
                .priority(r.getPriority())
                .validFrom(r.getValidFrom() != null ? r.getValidFrom().toString() : null)
                .validTo(r.getValidTo() != null ? r.getValidTo().toString() : null)
                .build();
    }

    private PricingRuleResponse toPricingResponse(PricingRule r) {
        return PricingRuleResponse.builder()
                .id(r.getId())
                .resourceId(r.getResource() != null ? r.getResource().getId() : null)
                .categoryId(r.getCategory() != null ? r.getCategory().getId() : null)
                .pricingType(r.getPricingType())
                .amount(r.getAmount())
                .percentage(r.getPercentage())
                .description(r.getDescription())
                .isActive(r.isActive())
                .validFrom(r.getValidFrom() != null ? r.getValidFrom().toString() : null)
                .validTo(r.getValidTo() != null ? r.getValidTo().toString() : null)
                .dayOfWeek(r.getDayOfWeek())
                .startTime(r.getStartTime() != null ? r.getStartTime().toString() : null)
                .endTime(r.getEndTime() != null ? r.getEndTime().toString() : null)
                .build();
    }

    private ApprovalWorkflowResponse toWorkflowResponse(ApprovalWorkflow w) {
        return ApprovalWorkflowResponse.builder()
                .id(w.getId())
                .resourceId(w.getResource() != null ? w.getResource().getId() : null)
                .categoryId(w.getCategory() != null ? w.getCategory().getId() : null)
                .workflowName(w.getWorkflowName())
                .stepOrder(w.getStepOrder())
                .stepType(w.getStepType())
                .stepName(w.getStepName())
                .isRequired(w.isRequired())
                .approverRole(w.getApproverRole())
                .timeoutHours(w.getTimeoutHours())
                .isActive(w.isActive())
                .build();
    }
}
