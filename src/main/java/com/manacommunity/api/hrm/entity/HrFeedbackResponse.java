package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_feedback_responses")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrFeedbackResponse {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "feedback_request_id", nullable = false)
    private HrFeedbackRequest feedbackRequest;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewer_employee_id", nullable = false)
    private HrEmployee reviewer;
    @Column(name = "overall_rating") private Integer overallRating;
    @Column(name = "communication_rating") private Integer communicationRating;
    @Column(name = "teamwork_rating") private Integer teamworkRating;
    @Column(name = "technical_rating") private Integer technicalRating;
    @Column(name = "leadership_rating") private Integer leadershipRating;
    @Column(name = "strengths", columnDefinition = "TEXT") private String strengths;
    @Column(name = "improvements", columnDefinition = "TEXT") private String improvements;
    @Column(name = "general_comments", columnDefinition = "TEXT") private String generalComments;
    @Column(name = "submitted_at") private LocalDateTime submittedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
