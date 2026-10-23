package com.manacommunity.api.serviceplatform.review.repository;

import com.manacommunity.api.serviceplatform.review.entity.ServiceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceReviewRepository extends JpaRepository<ServiceReview, Long> {
    List<ServiceReview> findByProviderId(Long providerId);
    Optional<ServiceReview> findByWorkOrderId(Long workOrderId);

    @Query("SELECT AVG(r.rating) FROM ServiceReview r WHERE r.provider.id = :providerId")
    Double calculateAverageRating(@Param("providerId") Long providerId);

    @Query("SELECT COUNT(r) FROM ServiceReview r WHERE r.provider.id = :providerId")
    Long countReviewsByProvider(@Param("providerId") Long providerId);
}
