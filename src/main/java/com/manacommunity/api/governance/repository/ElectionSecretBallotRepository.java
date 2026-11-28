package com.manacommunity.api.governance.repository;

import com.manacommunity.api.governance.entity.ElectionSecretBallot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ElectionSecretBallotRepository extends JpaRepository<ElectionSecretBallot, Long> {
    Optional<ElectionSecretBallot> findByBallotNullifier(String ballotNullifier);
    Optional<ElectionSecretBallot> findByElectionIdAndReceiptCode(Long electionId, String receiptCode);
    List<ElectionSecretBallot> findByElectionIdOrderByIdAsc(Long electionId);
    long countByElectionId(Long electionId);

    @Query("SELECT b.nominationId, COUNT(b) FROM ElectionSecretBallot b WHERE b.electionId = :electionId GROUP BY b.nominationId")
    List<Object[]> countSecretVotesByNomination(@Param("electionId") Long electionId);
}
