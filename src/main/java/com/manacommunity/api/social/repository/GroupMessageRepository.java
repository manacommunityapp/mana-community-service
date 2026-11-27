package com.manacommunity.api.social.repository;

import com.manacommunity.api.social.entity.GroupMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {
    Page<GroupMessage> findByGroupIdAndDeletedAtIsNullOrderBySentAtDesc(Long groupId, Pageable pageable);
}
