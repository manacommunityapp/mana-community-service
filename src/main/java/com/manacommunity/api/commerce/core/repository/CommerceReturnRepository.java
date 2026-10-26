package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommerceReturnRepository extends JpaRepository<CommerceReturn, Long> {
    Optional<CommerceReturn> findByReturnCode(String returnCode);
    List<CommerceReturn> findByCustomerUserIdOrderByCreatedAtDesc(Long customerId);
    List<CommerceReturn> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<CommerceReturn> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, String status);
}
