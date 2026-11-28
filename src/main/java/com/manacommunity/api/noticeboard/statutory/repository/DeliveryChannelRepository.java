package com.manacommunity.api.noticeboard.statutory.repository;

import com.manacommunity.api.noticeboard.statutory.entity.DeliveryChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryChannelRepository extends JpaRepository<DeliveryChannel, Long> {
    List<DeliveryChannel> findByNoticeId(Long noticeId);

    @Query("SELECT dc.channelType, SUM(dc.totalSent), SUM(dc.totalDelivered), SUM(dc.totalOpened) FROM DeliveryChannel dc GROUP BY dc.channelType")
    List<Object[]> aggregateByChannel();
}
