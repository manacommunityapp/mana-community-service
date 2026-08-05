package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventTaskRepository extends JpaRepository<EventTask, Long> {
    List<EventTask> findByEventIdOrderByCreatedAtDesc(Long eventId);
    List<EventTask> findByEventCommunityIdOrderByCreatedAtDesc(Long communityId);
}
