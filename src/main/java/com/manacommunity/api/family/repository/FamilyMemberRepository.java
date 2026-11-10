package com.manacommunity.api.family.repository;

import com.manacommunity.api.user.model.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {
    List<FamilyMember> findByUserIdAndStatusNot(Long userId, String status);
}
