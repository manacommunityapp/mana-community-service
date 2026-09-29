package com.manacommunity.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published when a post is created in the community feed.
 * Only fires for official/announcement posts to avoid notification fatigue —
 * regular member posts do NOT push to the whole community.
 */
@Getter
public class NewPostEvent extends ApplicationEvent {

    private final Long         postId;
    private final String       authorName;
    private final String       contentPreview;
    private final String       postType;   // "post" | "announcement" | "poll" | "event"
    private final Long         communityId;
    private final Long         authorId;
    /** If non-null, push to these specific users instead of the whole community. */
    private final java.util.List<Long> targetUserIds;

    public NewPostEvent(Object source,
                        Long postId,
                        String authorName,
                        String rawContent,
                        String postType,
                        Long communityId,
                        Long authorId,
                        java.util.List<Long> targetUserIds) {
        super(source);
        this.postId         = postId;
        this.authorName     = authorName;
        this.contentPreview = rawContent != null && rawContent.length() > 80
                ? rawContent.substring(0, 77) + "…"
                : rawContent;
        this.postType       = postType;
        this.communityId    = communityId;
        this.authorId       = authorId;
        this.targetUserIds  = targetUserIds;
    }
}
