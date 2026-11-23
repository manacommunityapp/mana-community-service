package com.manacommunity.api.governance.repository;

import com.manacommunity.api.governance.entity.ElectionVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ElectionVoteRepository extends JpaRepository<ElectionVote, Long> {
    Optional<ElectionVote> findByElectionIdAndVoterId(Long electionId, Long voterId);
    long countByElectionId(Long electionId);

    @Query("SELECT v.nominationId, COUNT(v) FROM ElectionVote v WHERE v.electionId = :electionId GROUP BY v.nominationId")
    List<Object[]> countVotesByNomination(Long electionId);
}
