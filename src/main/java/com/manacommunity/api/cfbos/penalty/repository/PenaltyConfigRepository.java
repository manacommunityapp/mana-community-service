package com.manacommunity.api.cfbos.penalty.repository;
import com.manacommunity.api.cfbos.penalty.entity.PenaltyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface PenaltyConfigRepository extends JpaRepository<PenaltyConfig, Long> { Optional<PenaltyConfig> findByIsActiveTrue(); }
