package com.manacommunity.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published by AdminServiceImpl after an announcement is created.
 * Triggers a community-wide push notification.
 *
 * The admin panel service already creates these — just add
 * events.publishEvent(new AnnouncementCreatedEvent(...)) to AdminServiceImpl
 * after announcementRepo.save(ann).
 */
@Getter
public class AnnouncementCreatedEvent extends ApplicationEvent {

    private final Long    announcementId;
    private final String  title;
    private final String  content;
    /** "NORMAL" or "URGENT" */
    private final String  priority;
    private final Long    communityId;
    private final Long    authorId;

    public AnnouncementCreatedEvent(Object source,
                                    Long announcementId,
                                    String title,
                                    String content,
                                    String priority,
                                    Long communityId,
                                    Long authorId) {
        super(source);
        this.announcementId = announcementId;
        this.title          = title;
        this.content        = content;
        this.priority       = priority;
        this.communityId    = communityId;
        this.authorId       = authorId;
    }
}
