package com.manacommunity.api.noticeboard.statutory.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notice_audience_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AudienceRule extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long audienceGroupId;

    @Enumerated(EnumType.STRING)
    private RuleField field;

    @Enumerated(EnumType.STRING)
    private RuleOperator operator;

    private String value;

    @Enumerated(EnumType.STRING)
    private RuleConjunction conjunction;

    private int ruleOrder;

    public enum RuleField {
        TOWER, FLAT_TYPE, OWNERSHIP_TYPE, VEHICLE_TYPE,
        PENDING_DUES_AMOUNT, MEMBER_SINCE_MONTHS, OCCUPANCY_STATUS,
        FLAT_AREA_SQFT, PARKING_SLOT, COMMITTEE_ROLE, PET_OWNER,
        INTERCOM_REGISTERED, AGE_GROUP
    }

    public enum RuleOperator {
        EQUALS, NOT_EQUALS, IN, NOT_IN,
        GREATER_THAN, LESS_THAN, GREATER_EQUAL, LESS_EQUAL,
        CONTAINS, STARTS_WITH, IS_TRUE, IS_FALSE
    }

    public enum RuleConjunction { AND, OR }
}
