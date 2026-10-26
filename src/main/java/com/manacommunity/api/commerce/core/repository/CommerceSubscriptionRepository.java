package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommerceSubscriptionRepository extends JpaRepository<CommerceSubscription, Long> {

    Optional<CommerceSubscription> findBySubscriptionCode(String subscriptionCode);

    List<CommerceSubscription> findByCustomerUserIdOrderByCreatedAtDesc(Long customerId);

    List<CommerceSubscription> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<CommerceSubscription> findByStatusAndNextRunDateLessThanEqual(String status, LocalDate date);
}
