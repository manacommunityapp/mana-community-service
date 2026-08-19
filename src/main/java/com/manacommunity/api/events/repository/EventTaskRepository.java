package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface EventTaskRepository extends JpaRepository<EventTask, Long> {

    @Modifying
    void deleteByEventId(Long eventId);

    List<EventTask> findByEventIdOrderByDueDateAsc(Long eventId);

    List<EventTask> findByCommunityIdOrderByDueDateAsc(Long communityId);

    long countByEventIdAndDoneFalse(Long eventId);

    List<EventTask> findByEventIdOrderByCreatedAtDesc(Long eventId);
    long countByCommunityIdAndDoneFalse(Long communityId);
    long countByDoneFalse();
    List<EventTask> findByEventCommunityIdOrderByCreatedAtDesc(Long communityId);
}
