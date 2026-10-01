package com.manacommunity.api.sports.scheduler;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity @Table(name = "sports_tournament_group")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SportsTournamentGroup {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id")
    private SportsTournamentConfig config;

    @Column(nullable = false)
    private String groupName;     // "Group A", "Group B", ...

    @Column(nullable = false)
    private Integer groupOrder;   // 1, 2, 3 …

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<SportsGroupTeamStanding> standings;
}
