package com.manacommunity.api.commerce.customer.repository;

import com.manacommunity.api.commerce.customer.model.CommerceCustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CommerceCustomerProfileRepository extends JpaRepository<CommerceCustomerProfile, Long> {

    Optional<CommerceCustomerProfile> findByUserIdAndCommunityId(Long userId, Long communityId);

    boolean existsByUserIdAndCommunityId(Long userId, Long communityId);

    @Query("SELECT p FROM CommerceCustomerProfile p WHERE p.community.id = :communityId ORDER BY p.createdAt DESC")
    java.util.List<CommerceCustomerProfile> findByCommunityId(Long communityId,
            org.springframework.data.domain.Pageable pageable);
}
