package com.manacommunity.api.noticeboard.statutory.repository;

import com.manacommunity.api.noticeboard.statutory.entity.StatutoryNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatutoryNoticeRepository extends JpaRepository<StatutoryNotice, Long> {
    List<StatutoryNotice> findByCommunityIdAndStatusOrderByPublishedAtDesc(String communityId, StatutoryNotice.NoticeStatus status);

    @Query("SELECT n FROM StatutoryNotice n WHERE n.statutory = true AND n.readReceiptRequired = true AND n.status = 'PUBLISHED' ORDER BY n.publishedAt DESC")
    List<StatutoryNotice> findStatutoryWithReadReceipt();

    @Query("SELECT n FROM StatutoryNotice n WHERE n.pinned = true AND n.status = 'PUBLISHED' ORDER BY n.displayOrder")
    List<StatutoryNotice> findPinnedNotices();

    @Query("SELECT n.category, COUNT(n) FROM StatutoryNotice n WHERE n.status = 'PUBLISHED' GROUP BY n.category")
    List<Object[]> countByCategory();
}
