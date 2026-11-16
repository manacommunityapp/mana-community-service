package com.manacommunity.api.repository;

import com.manacommunity.api.model.ProposalSupport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProposalSupportRepository extends JpaRepository<ProposalSupport, Long> {
    Optional<ProposalSupport> findByProposalIdAndUserId(Long proposalId, Long userId);
    long countByProposalId(Long proposalId);
}
