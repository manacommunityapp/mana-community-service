package com.manacommunity.api.governance.repository;

import com.manacommunity.api.governance.entity.ElectionVoterRoll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ElectionVoterRollRepository extends JpaRepository<ElectionVoterRoll, Long> {
    Optional<ElectionVoterRoll> findByElectionIdAndFlatId(Long electionId, Long flatId);
    long countByElectionId(Long electionId);
}
