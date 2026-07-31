package com.cpn.domain.feed.model;

import com.cpn.domain.common.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post extends TenantAwareEntity {

    @Column(nullable = false)
    private UUID authorId;

    private String authorName;

    private String authorRole;

    private String authorFlat;

    private String postType; // ACHIEVEMENT, JOB, ARTICLE, EVENT, GENERAL

    @Column(length = 4000)
    private String content;

    private String jobTag;

    @Builder.Default
    private int likeCount = 0;

    @Builder.Default
    private int celebrateCount = 0;

    @Builder.Default
    private int insightfulCount = 0;

    @Builder.Default
    private int commentCount = 0;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();
}
