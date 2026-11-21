package com.manacommunity.api.grouporder.repository;

import com.manacommunity.api.grouporder.entity.GroupOrderContribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupOrderContributionRepository extends JpaRepository<GroupOrderContribution, Long> {

    Optional<GroupOrderContribution> findByGroupOrderIdAndResidentId(Long groupOrderId, Long residentId);

    List<GroupOrderContribution> findByGroupOrderId(Long groupOrderId);

    List<GroupOrderContribution> findByResidentId(Long residentId);
}
