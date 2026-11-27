package com.manacommunity.api.noticeboard.statutory.service;

import com.manacommunity.api.noticeboard.statutory.entity.*;
import com.manacommunity.api.noticeboard.statutory.entity.AudienceGroup.GroupStatus;
import com.manacommunity.api.noticeboard.statutory.entity.AudienceMember.OwnershipType;
import com.manacommunity.api.noticeboard.statutory.entity.AudienceRule.*;
import com.manacommunity.api.noticeboard.statutory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudienceSegmentationEngine {

    private final AudienceGroupRepository groupRepo;
    private final AudienceRuleRepository ruleRepo;
    private final AudienceMemberRepository memberRepo;

    @Transactional
    public AudienceGroup createDynamicGroup(String communityId, String name, String description,
                                              List<RuleSpec> rules) {
        AudienceGroup group = AudienceGroup.builder()
                .communityId(communityId).name(name).description(description)
                .dynamic(true).systemDefined(false)
                .groupStatus(GroupStatus.ACTIVE).build();
        group = groupRepo.save(group);

        for (int i = 0; i < rules.size(); i++) {
            RuleSpec spec = rules.get(i);
            ruleRepo.save(AudienceRule.builder()
                    .audienceGroupId(group.getId())
                    .field(spec.field()).operator(spec.operator())
                    .value(spec.value())
                    .conjunction(i < rules.size() - 1 ? spec.conjunction() : null)
                    .ruleOrder(i + 1).build());
        }

        return group;
    }

    @Transactional
    public EvaluationResult evaluateGroup(Long groupId, List<ResidentRecord> allResidents) {
        AudienceGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        group.setGroupStatus(GroupStatus.EVALUATING);
        groupRepo.save(group);

        List<AudienceRule> rules = ruleRepo.findByAudienceGroupIdOrderByRuleOrder(groupId);

        List<ResidentRecord> matched = allResidents.stream()
                .filter(r -> evaluateRules(r, rules))
                .toList();

        // Clear old members and populate new
        List<AudienceMember> existing = memberRepo.findByAudienceGroupId(groupId);
        memberRepo.deleteAll(existing);

        List<AudienceMember> newMembers = matched.stream().map(r -> AudienceMember.builder()
                .audienceGroupId(groupId).communityId(group.getCommunityId())
                .flatNumber(r.flatNumber()).towerName(r.towerName())
                .residentName(r.name()).email(r.email()).phone(r.phone())
                .userId(r.userId()).ownershipType(r.ownershipType())
                .matchedByRule(true).build()
        ).toList();

        memberRepo.saveAll(newMembers);

        group.setCachedMemberCount(newMembers.size());
        group.setLastEvaluatedAt(LocalDateTime.now());
        group.setGroupStatus(GroupStatus.ACTIVE);
        groupRepo.save(group);

        Map<String, Long> towerBreakdown = newMembers.stream()
                .collect(Collectors.groupingBy(AudienceMember::getTowerName, Collectors.counting()));

        return new EvaluationResult(groupId, group.getName(), allResidents.size(),
                newMembers.size(), towerBreakdown, rules.size());
    }

    private boolean evaluateRules(ResidentRecord resident, List<AudienceRule> rules) {
        if (rules.isEmpty()) return true;

        Boolean result = null;

        for (AudienceRule rule : rules) {
            boolean match = evaluateRule(resident, rule);

            if (result == null) {
                result = match;
            } else {
                RuleConjunction prevConj = rules.get(rules.indexOf(rule) - 1).getConjunction();
                if (prevConj == RuleConjunction.AND) {
                    result = result && match;
                } else {
                    result = result || match;
                }
            }
        }

        return result != null && result;
    }

    private boolean evaluateRule(ResidentRecord resident, AudienceRule rule) {
        String fieldValue = getFieldValue(resident, rule.getField());
        if (fieldValue == null) return false;

        return switch (rule.getOperator()) {
            case EQUALS -> fieldValue.equalsIgnoreCase(rule.getValue());
            case NOT_EQUALS -> !fieldValue.equalsIgnoreCase(rule.getValue());
            case IN -> Arrays.asList(rule.getValue().split(",")).stream()
                    .map(String::trim).anyMatch(v -> v.equalsIgnoreCase(fieldValue));
            case NOT_IN -> Arrays.asList(rule.getValue().split(",")).stream()
                    .map(String::trim).noneMatch(v -> v.equalsIgnoreCase(fieldValue));
            case GREATER_THAN -> parseDouble(fieldValue) > parseDouble(rule.getValue());
            case LESS_THAN -> parseDouble(fieldValue) < parseDouble(rule.getValue());
            case GREATER_EQUAL -> parseDouble(fieldValue) >= parseDouble(rule.getValue());
            case LESS_EQUAL -> parseDouble(fieldValue) <= parseDouble(rule.getValue());
            case CONTAINS -> fieldValue.toLowerCase().contains(rule.getValue().toLowerCase());
            case STARTS_WITH -> fieldValue.toLowerCase().startsWith(rule.getValue().toLowerCase());
            case IS_TRUE -> "true".equalsIgnoreCase(fieldValue);
            case IS_FALSE -> "false".equalsIgnoreCase(fieldValue);
        };
    }

    private String getFieldValue(ResidentRecord r, RuleField field) {
        return switch (field) {
            case TOWER -> r.towerName();
            case FLAT_TYPE -> r.flatType();
            case OWNERSHIP_TYPE -> r.ownershipType() != null ? r.ownershipType().name() : null;
            case VEHICLE_TYPE -> r.vehicleType();
            case PENDING_DUES_AMOUNT -> String.valueOf(r.pendingDues());
            case MEMBER_SINCE_MONTHS -> String.valueOf(r.memberSinceMonths());
            case OCCUPANCY_STATUS -> r.occupancyStatus();
            case FLAT_AREA_SQFT -> String.valueOf(r.flatAreaSqft());
            case PARKING_SLOT -> r.parkingSlot();
            case COMMITTEE_ROLE -> r.committeeRole();
            case PET_OWNER -> String.valueOf(r.petOwner());
            case INTERCOM_REGISTERED -> String.valueOf(r.intercomRegistered());
            case AGE_GROUP -> r.ageGroup();
        };
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    public AudienceGroupDashboard getDashboard() {
        List<AudienceGroup> groups = groupRepo.findAll();
        List<AudienceRule> allRules = ruleRepo.findAll();
        List<AudienceMember> allMembers = memberRepo.findAll();

        long dynamicGroups = groups.stream().filter(AudienceGroup::isDynamic).count();
        long staticGroups = groups.size() - dynamicGroups;
        int totalMembers = allMembers.size();

        Map<String, Long> membersByGroup = allMembers.stream()
                .collect(Collectors.groupingBy(m -> groups.stream()
                        .filter(g -> g.getId().equals(m.getAudienceGroupId()))
                        .findFirst().map(AudienceGroup::getName).orElse("Unknown"), Collectors.counting()));

        return new AudienceGroupDashboard(groups.size(), dynamicGroups, staticGroups,
                totalMembers, membersByGroup, groups, allRules);
    }

    public record RuleSpec(RuleField field, RuleOperator operator, String value, RuleConjunction conjunction) {}

    public record ResidentRecord(String flatNumber, String towerName, String name, String email,
                                   String phone, Long userId, OwnershipType ownershipType,
                                   String flatType, String vehicleType, double pendingDues,
                                   int memberSinceMonths, String occupancyStatus, int flatAreaSqft,
                                   String parkingSlot, String committeeRole, boolean petOwner,
                                   boolean intercomRegistered, String ageGroup) {}

    public record EvaluationResult(Long groupId, String groupName, int totalEvaluated,
                                     int matched, Map<String, Long> towerBreakdown, int rulesApplied) {}

    public record AudienceGroupDashboard(int totalGroups, long dynamicGroups, long staticGroups,
                                           int totalMembers, Map<String, Long> membersByGroup,
                                           List<AudienceGroup> groups, List<AudienceRule> rules) {}
}
