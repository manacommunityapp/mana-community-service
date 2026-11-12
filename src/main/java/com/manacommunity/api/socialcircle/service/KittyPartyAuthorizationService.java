package com.manacommunity.api.socialcircle.service;

import com.manacommunity.api.socialcircle.entity.KittyPartyMember;
import com.manacommunity.api.socialcircle.repository.KittyPartyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class KittyPartyAuthorizationService {

    private final KittyPartyMemberRepository memberRepository;

    private static final Set<String> ADMIN_ROLES = Set.of("OWNER", "ADMIN");
    private static final Set<String> FINANCE_ROLES = Set.of("OWNER", "ADMIN", "TREASURER");
    private static final Set<String> EVENT_MANAGER_ROLES = Set.of("OWNER", "ADMIN", "HOST");
    private static final Set<String> GAME_HOST_ROLES = Set.of("OWNER", "ADMIN", "HOST");

    public KittyPartyMember getActiveMember(Long circleId, Long userId) {
        KittyPartyMember member = memberRepository.findByCircleIdAndUserId(circleId, userId)
                .orElseThrow(() -> new SecurityException("User is not a member of this circle"));
        if (!"ACTIVE".equals(member.getMembershipStatus())) {
            throw new SecurityException("User membership is not active in this circle");
        }
        return member;
    }

    public void assertCircleMember(Long circleId, Long userId) {
        getActiveMember(circleId, userId);
    }

    public void assertAdminOrOwner(Long circleId, Long userId) {
        KittyPartyMember member = getActiveMember(circleId, userId);
        if (!ADMIN_ROLES.contains(member.getRoleCode())) {
            throw new SecurityException("Requires circle Admin or Owner role");
        }
    }

    public void assertFinanceRole(Long circleId, Long userId) {
        KittyPartyMember member = getActiveMember(circleId, userId);
        if (!FINANCE_ROLES.contains(member.getRoleCode())) {
            throw new SecurityException("Requires circle Owner, Admin, or Treasurer role");
        }
    }

    public void assertEventManager(Long circleId, Long userId) {
        KittyPartyMember member = getActiveMember(circleId, userId);
        if (!EVENT_MANAGER_ROLES.contains(member.getRoleCode())) {
            throw new SecurityException("Requires circle Owner, Admin, or Host role");
        }
    }

    public void assertGameHost(Long circleId, Long userId) {
        KittyPartyMember member = getActiveMember(circleId, userId);
        if (!GAME_HOST_ROLES.contains(member.getRoleCode())) {
            throw new SecurityException("Requires circle Owner, Admin, or Host role");
        }
    }

    public void assertNotExternal(Long circleId, Long userId) {
        KittyPartyMember member = getActiveMember(circleId, userId);
        if (Boolean.TRUE.equals(member.getIsExternal())) {
            throw new SecurityException("External members are not permitted to perform this action");
        }
    }

    public String getMemberRole(Long circleId, Long userId) {
        return memberRepository.findByCircleIdAndUserId(circleId, userId)
                .filter(m -> "ACTIVE".equals(m.getMembershipStatus()))
                .map(KittyPartyMember::getRoleCode)
                .orElse(null);
    }
}
