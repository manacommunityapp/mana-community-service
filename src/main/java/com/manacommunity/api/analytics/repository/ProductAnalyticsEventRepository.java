package com.manacommunity.api.analytics.repository;

import com.manacommunity.api.analytics.model.ProductAnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ProductAnalyticsEventRepository extends JpaRepository<ProductAnalyticsEvent, Long> {

    @Query("SELECT COUNT(DISTINCT p.userHash) FROM ProductAnalyticsEvent p " +
           "WHERE (:communityId IS NULL OR p.communityId = :communityId) " +
           "AND p.occurredAt BETWEEN :from AND :to AND p.userHash IS NOT NULL")
    long countDistinctActiveUsers(
            @Param("communityId") Long communityId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Query("SELECT COUNT(p) FROM ProductAnalyticsEvent p " +
           "WHERE (:communityId IS NULL OR p.communityId = :communityId) " +
           "AND p.eventName = :eventName AND p.occurredAt BETWEEN :from AND :to")
    long countByEventName(
            @Param("communityId") Long communityId,
            @Param("eventName") String eventName,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Query("SELECT COUNT(DISTINCT p.userHash) FROM ProductAnalyticsEvent p " +
           "WHERE (:communityId IS NULL OR p.communityId = :communityId) " +
           "AND p.eventName = :eventName AND p.occurredAt BETWEEN :from AND :to AND p.userHash IS NOT NULL")
    long countDistinctUsersByEventName(
            @Param("communityId") Long communityId,
            @Param("eventName") String eventName,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Query("SELECT p.eventName, COUNT(p) FROM ProductAnalyticsEvent p " +
           "WHERE (:communityId IS NULL OR p.communityId = :communityId) " +
           "AND p.occurredAt BETWEEN :from AND :to " +
           "GROUP BY p.eventName ORDER BY COUNT(p) DESC")
    List<Object[]> countGroupByEventName(
            @Param("communityId") Long communityId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}
