package com.manacommunity.api.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "community_meeting")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityMeeting extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 20)
    private String type; // AGM, EGM, COMMITTEE

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "meeting_date", nullable = false)
    private LocalDate meetingDate;

    @Column(name = "meeting_time")
    private LocalTime meetingTime;

    @Column
    private String location;

    @Column(name = "meeting_link")
    private String meetingLink;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "UPCOMING"; // UPCOMING, LIVE_NOW, CONCLUDED

    @Column(name = "quorum_required")
    @Builder.Default
    private Integer quorumRequired = 50;

    @Column(name = "annual_report_url")
    private String annualReportUrl;

    @Column(name = "minutes_url")
    private String minutesUrl;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<MeetingAgendaItem> agendaItems = new ArrayList<>();

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<MeetingRsvp> rsvps = new ArrayList<>();
}
