package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meeting_agenda_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingAgendaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CommunityMeeting meeting;

    @Column(name = "item_order")
    @Builder.Default
    private Integer itemOrder = 1;

    @Column(nullable = false)
    private String title;

    @Column
    private String presenter;

    @Column
    private String duration;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, IN_PROGRESS, DISCUSSED, VOTING_OPEN, APPROVED

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "attachment_name")
    private String attachmentName;
}
