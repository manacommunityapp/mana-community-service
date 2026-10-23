package com.manacommunity.api.user.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "family_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class FamilyMember extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"password", "roles", "community"})
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Community community;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 60)
    private String relation;

    private Integer age;

    @Column(length = 20)
    private String gender;

    @Column(length = 50)
    private String dob;

    @Column(length = 50)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "blood_group", length = 20)
    private String bloodGroup;

    @Column(length = 100)
    private String gothram;

    @Column(name = "emergency_contact")
    @Builder.Default
    private Boolean emergencyContact = false;

    @Column(name = "is_devotee")
    @Builder.Default
    private Boolean isDevotee = true;

    @Column(name = "avatar", columnDefinition = "TEXT")
    private String avatar;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(length = 30)
    @Builder.Default
    private String status = "ACTIVE";

    public String getGotram() {
        return gothram;
    }

    public void setGotram(String gotram) {
        this.gothram = gotram;
    }

}
