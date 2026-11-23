package com.manacommunity.api.social.repository;

import com.manacommunity.api.social.entity.GroupEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface GroupEventRepository extends JpaRepository<GroupEvent, Long> {
    List<GroupEvent> findByGroupIdAndEventDateAfterOrderByEventDateAsc(Long groupId, OffsetDateTime now);
}
