package com.manacommunity.api.grouporder.repository;

import com.manacommunity.api.grouporder.entity.GroupOrderUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupOrderUpdateRepository extends JpaRepository<GroupOrderUpdate, Long> {

    List<GroupOrderUpdate> findByGroupOrderIdOrderByPostedAtAsc(Long groupOrderId);
}
