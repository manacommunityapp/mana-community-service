package com.manacommunity.api.repository;

import com.manacommunity.api.model.InventorySequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventorySequenceRepository extends JpaRepository<InventorySequence, Long> {

    /** Pessimistic lock to prevent duplicate sequence numbers under concurrency. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InventorySequence s WHERE s.prefix = :prefix")
    Optional<InventorySequence> findByPrefixForUpdate(@Param("prefix") String prefix);
}
