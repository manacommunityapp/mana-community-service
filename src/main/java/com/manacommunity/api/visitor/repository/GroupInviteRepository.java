package com.manacommunity.api.visitor.repository;

import com.manacommunity.api.visitor.entity.GroupInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, Long> {
    List<GroupInvite> findByHostIdOrderByCreatedAtDesc(Long hostId);
}
