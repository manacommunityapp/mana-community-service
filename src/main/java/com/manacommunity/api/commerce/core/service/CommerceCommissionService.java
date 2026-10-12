package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.dto.CommerceCommissionRuleDto;
import com.manacommunity.api.commerce.core.dto.CommerceCommissionRuleRequest;
import com.manacommunity.api.commerce.core.model.CommerceChannel;
import com.manacommunity.api.commerce.core.model.CommerceCommissionRule;
import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.repository.CommerceCommissionRuleRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommerceCommissionService {

    private static final BigDecimal DEFAULT_FEE_PERCENT = BigDecimal.valueOf(2.5);

    private final CommerceCommissionRuleRepository ruleRepository;
    private final AppUserRepository userRepository;

    /**
     * Returns the platform fee percentage that applies to a specific vendor in a community.
     * Priority: vendor-specific rule > category rule > community default rule > 2.5% fallback.
     */
    public BigDecimal getFeePercent(Long communityId, Long vendorUserId, String category) {
        List<CommerceCommissionRule> rules = vendorUserId != null
                ? ruleRepository.findApplicableRules(communityId, vendorUserId)
                : ruleRepository.findByCommunityIdAndActiveTrueOrderByPriorityAsc(communityId);

        // Vendor-specific match first
        if (vendorUserId != null) {
            for (CommerceCommissionRule r : rules) {
                if (r.getVendorUser() != null && r.getVendorUser().getId().equals(vendorUserId)) {
                    return r.getFeePercent();
                }
            }
        }

        // Category match
        if (category != null && !category.isBlank()) {
            for (CommerceCommissionRule r : rules) {
                if (r.getVendorUser() == null && category.equalsIgnoreCase(r.getCategory())) {
                    return r.getFeePercent();
                }
            }
        }

        // Default community rule (no vendor, no category)
        for (CommerceCommissionRule r : rules) {
            if (r.getVendorUser() == null && (r.getCategory() == null || r.getCategory().isBlank())) {
                return r.getFeePercent();
            }
        }

        return DEFAULT_FEE_PERCENT;
    }

    /**
     * Computes the platform fee amount for a gross amount given vendor context.
     */
    public BigDecimal computeFeeAmount(BigDecimal gross, Long communityId, Long vendorUserId, String category) {
        BigDecimal pct = getFeePercent(communityId, vendorUserId, category);
        return gross.multiply(pct.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ── Admin CRUD ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CommerceCommissionRuleDto> getRules(AppUser admin) {
        return ruleRepository.findByCommunityIdAndActiveTrueOrderByPriorityAsc(admin.getCommunity().getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommerceCommissionRuleDto> getAllRules(AppUser admin) {
        return ruleRepository.findByCommunityIdAndActiveTrueOrderByPriorityAsc(admin.getCommunity().getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public CommerceCommissionRuleDto createRule(AppUser admin, CommerceCommissionRuleRequest req) {
        Community community = admin.getCommunity();
        AppUser vendorUser = req.getVendorUserId() != null
                ? userRepository.findById(req.getVendorUserId()).orElse(null) : null;

        CommerceCommissionRule rule = CommerceCommissionRule.builder()
                .community(community)
                .ruleName(req.getRuleName() != null ? req.getRuleName() : "Rule")
                .vendorUser(vendorUser)
                .category(req.getCategory())
                .channel(parseChannel(req.getChannel()))
                .feePercent(req.getFeePercent() != null ? req.getFeePercent() : DEFAULT_FEE_PERCENT)
                .gstOnFee(Boolean.TRUE.equals(req.getGstOnFee()))
                .tdsPercent(req.getTdsPercent())
                .priority(req.getPriority() != null ? req.getPriority() : 100)
                .active(req.getActive() == null || req.getActive())
                .notes(req.getNotes())
                .build();

        return toDto(ruleRepository.save(rule));
    }

    @Transactional
    public CommerceCommissionRuleDto updateRule(AppUser admin, Long ruleId, CommerceCommissionRuleRequest req) {
        CommerceCommissionRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));
        if (!rule.getCommunity().getId().equals(admin.getCommunity().getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Rule not in your community.");
        }

        if (req.getRuleName() != null) rule.setRuleName(req.getRuleName());
        if (req.getFeePercent() != null) rule.setFeePercent(req.getFeePercent());
        if (req.getGstOnFee() != null) rule.setGstOnFee(req.getGstOnFee());
        if (req.getTdsPercent() != null) rule.setTdsPercent(req.getTdsPercent());
        if (req.getPriority() != null) rule.setPriority(req.getPriority());
        if (req.getActive() != null) rule.setActive(req.getActive());
        if (req.getCategory() != null) rule.setCategory(req.getCategory());
        if (req.getChannel() != null) rule.setChannel(parseChannel(req.getChannel()));
        if (req.getNotes() != null) rule.setNotes(req.getNotes());
        if (req.getVendorUserId() != null) {
            rule.setVendorUser(userRepository.findById(req.getVendorUserId()).orElse(null));
        }

        return toDto(ruleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(AppUser admin, Long ruleId) {
        CommerceCommissionRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));
        if (!rule.getCommunity().getId().equals(admin.getCommunity().getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Rule not in your community.");
        }
        rule.setActive(false);
        ruleRepository.save(rule);
    }

    private CommerceChannel parseChannel(String ch) {
        if (ch == null || ch.isBlank()) return null;
        try { return CommerceChannel.valueOf(ch.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private CommerceCommissionRuleDto toDto(CommerceCommissionRule r) {
        return CommerceCommissionRuleDto.builder()
                .id(r.getId())
                .ruleName(r.getRuleName())
                .vendorUserId(r.getVendorUser() != null ? r.getVendorUser().getId() : null)
                .vendorName(r.getVendorUser() != null ? r.getVendorUser().getFullName() : null)
                .category(r.getCategory())
                .channel(r.getChannel() != null ? r.getChannel().name() : null)
                .feePercent(r.getFeePercent())
                .gstOnFee(r.isGstOnFee())
                .tdsPercent(r.getTdsPercent())
                .priority(r.getPriority())
                .active(r.isActive())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)
                .build();
    }
}
