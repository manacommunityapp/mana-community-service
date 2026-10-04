package com.manacommunity.api.homeservices.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "staff_job_post", indexes = {
        @Index(name = "idx_staff_job_post_community", columnList = "community_id"),
        @Index(name = "idx_staff_job_post_status", columnList = "status"),
        @Index(name = "idx_staff_job_post_role", columnList = "role")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffJobPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posted_by", nullable = false)
    private AppUser postedBy;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DomesticStaff.StaffRole role;

    @Column(length = 1000)
    private String description;

    @Column(name = "salary_range", length = 50)
    private String salaryRange;

    @Column(name = "shift_preference", length = 50)
    private String shiftPreference;

    @ElementCollection
    @CollectionTable(name = "staff_job_post_requirements", joinColumns = @JoinColumn(name = "job_post_id"))
    @Column(name = "requirement")
    @Builder.Default
    private List<String> requirements = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private JobPostStatus status = JobPostStatus.OPEN;

    @Builder.Default
    @Column(name = "applicant_count", nullable = false)
    private Integer applicantCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum JobPostStatus {
        OPEN, FILLED, CLOSED
    }
}
