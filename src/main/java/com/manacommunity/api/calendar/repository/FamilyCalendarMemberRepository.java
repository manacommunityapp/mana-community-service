package com.manacommunity.api.calendar.repository;

import com.manacommunity.api.calendar.entity.FamilyCalendarMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FamilyCalendarMemberRepository extends JpaRepository<FamilyCalendarMember, Long> {

    List<FamilyCalendarMember> findByUserId(Long userId);

    List<FamilyCalendarMember> findByFamilyId(String familyId);
}
