package com.manacommunity.api.noticeboard.repository;

import com.manacommunity.api.noticeboard.entity.NoticeReadReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoticeReadReceiptRepository extends JpaRepository<NoticeReadReceipt, Long> {
    Optional<NoticeReadReceipt> findByNoticeIdAndUserId(Long noticeId, Long userId);

    @Query("SELECT COUNT(r) FROM NoticeReadReceipt r WHERE r.notice.id = :noticeId")
    long countReadReceipts(@Param("noticeId") Long noticeId);

    @Query("SELECT COUNT(r) FROM NoticeReadReceipt r WHERE r.notice.id = :noticeId AND r.acknowledged = true")
    long countAcknowledgements(@Param("noticeId") Long noticeId);
}
