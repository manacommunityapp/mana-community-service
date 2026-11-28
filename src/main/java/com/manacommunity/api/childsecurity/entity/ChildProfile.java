package com.manacommunity.api.childsecurity.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "child_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChildProfile extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String childName;
    private LocalDate dateOfBirth;
    private int age;

    private String flatNumber;
    private String towerName;
    private String photoUrl;

    private String schoolName;
    private String schoolGrade;

    private Long busRouteId;
    private String busRouteCode;

    private boolean exitLockEnabled;
    private boolean busTrackingEnabled;

    @Enumerated(EnumType.STRING)
    private ChildStatus status;

    public enum ChildStatus { ACTIVE, INACTIVE, GRADUATED }
}
