package com.manacommunity.api.privacy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataDeletionRequestRepository extends JpaRepository<DataDeletionRequest, Long> {

    List<DataDeletionRequest> findByUserIdOrderByRequestedAtDesc(Long userId);

    Optional<DataDeletionRequest> findFirstByUserIdAndStatusInOrderByRequestedAtDesc(
            Long userId, List<DataDeletionRequest.DeletionStatus> statuses);

    Optional<DataDeletionRequest> findByVerificationToken(String verificationToken);

    List<DataDeletionRequest> findByCommunityIdAndStatusOrderByRequestedAtDesc(
            Long communityId, DataDeletionRequest.DeletionStatus status);

    List<DataDeletionRequest> findByStatusOrderByRequestedAtDesc(DataDeletionRequest.DeletionStatus status);
}
