package com.manacommunity.api.noticeboard.repository;

import com.manacommunity.api.noticeboard.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("SELECT n FROM Notice n WHERE n.community.id = :communityId " +
           "AND (n.expiresOn IS NULL OR n.expiresOn >= :today) " +
           "ORDER BY n.pinned DESC, n.createdAt DESC")
    List<Notice> findActiveByCommunity(
            @Param("communityId") Long communityId,
            @Param("today") LocalDate today);

    @Query("SELECT n FROM Notice n WHERE n.community.id = :communityId " +
           "AND n.category = :category " +
           "AND (n.expiresOn IS NULL OR n.expiresOn >= :today) " +
           "ORDER BY n.pinned DESC, n.createdAt DESC")
    List<Notice> findActiveByCommunityAndCategory(
            @Param("communityId") Long communityId,
            @Param("category") Notice.NoticeCategory category,
            @Param("today") LocalDate today);

    List<Notice> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    List<Notice> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    long countByCommunityId(Long communityId);
}
