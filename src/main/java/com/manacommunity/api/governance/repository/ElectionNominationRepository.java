package com.manacommunity.api.governance.repository;

import com.manacommunity.api.governance.entity.ElectionNomination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ElectionNominationRepository extends JpaRepository<ElectionNomination, Long> {
    List<ElectionNomination> findByElectionId(Long electionId);
    Optional<ElectionNomination> findByElectionIdAndNomineeId(Long electionId, Long nomineeId);
    long countByElectionId(Long electionId);
}
