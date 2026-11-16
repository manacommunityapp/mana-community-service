package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.BrsSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrsSessionRepository extends JpaRepository<BrsSession, Long> {

    List<BrsSession> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    java.util.Optional<BrsSession> findByCommunityIdAndStatementMonth(Long communityId, String statementMonth);
}
