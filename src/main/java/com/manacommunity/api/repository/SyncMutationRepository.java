package com.manacommunity.api.repository;

import com.manacommunity.api.model.SyncMutation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyncMutationRepository extends JpaRepository<SyncMutation, Long> {

    Optional<SyncMutation> findByMutationId(String mutationId);

    boolean existsByMutationId(String mutationId);
}
