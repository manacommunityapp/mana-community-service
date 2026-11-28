package com.manacommunity.api.rbac.service;

import lombok.Builder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Canonical Persona RBAC & Multi-Tenant Isolation Policy Service across Mygate roles:
 * FLAT_OWNER, TENANT, FAMILY_MEMBER, GUARD, ESTATE_MANAGER, MC_ADMIN, and PLATFORM_SUPER_ADMIN.
 * Guarantees strict society_id isolation (zero cross-society data leaks) and flat-level isolation for resident personas.
 */
@Service
public class SocietyPersonaRbacService {

    public enum SocietyPersona {
        FLAT_OWNER,
        TENANT,
        FAMILY_MEMBER,
        GUARD,
        ESTATE_MANAGER,
        MC_ADMIN,
        PLATFORM_SUPER_ADMIN
    }

    public enum PersonaCapability {
        APPROVE_VISITOR,
        GATE_CHECKIN_SCAN,
        BOOK_AMENITY,
        RAISE_COMPLAINT,
        VIEW_OWN_DUES,
        PAY_MAINTENANCE_DUES,
        VOTE_OWNER_AGM,
        MANAGE_FLAT_OCCUPANCY,
        MANAGE_HELPDESK_SLA,
        MANAGE_SOCIETY_GOVERNANCE,
        CROSS_SOCIETY_PLATFORM_ACCESS
    }

    @Builder
    public record ActorContext(
            Long userId,
            String rawRole,
            Long communityId,
            Long flatId
    ) {}

    private static final Map<SocietyPersona, Set<PersonaCapability>> MATRIX = new EnumMap<>(SocietyPersona.class);

    static {
        MATRIX.put(SocietyPersona.FLAT_OWNER, EnumSet.of(
                PersonaCapability.APPROVE_VISITOR,
                PersonaCapability.BOOK_AMENITY,
                PersonaCapability.RAISE_COMPLAINT,
                PersonaCapability.VIEW_OWN_DUES,
                PersonaCapability.PAY_MAINTENANCE_DUES,
                PersonaCapability.VOTE_OWNER_AGM,
                PersonaCapability.MANAGE_FLAT_OCCUPANCY
        ));

        MATRIX.put(SocietyPersona.TENANT, EnumSet.of(
                PersonaCapability.APPROVE_VISITOR,
                PersonaCapability.BOOK_AMENITY,
                PersonaCapability.RAISE_COMPLAINT,
                PersonaCapability.VIEW_OWN_DUES,
                PersonaCapability.PAY_MAINTENANCE_DUES
                // Tenants cannot vote in owner-only AGM governance or alter owner lease deed
        ));

        MATRIX.put(SocietyPersona.FAMILY_MEMBER, EnumSet.of(
                PersonaCapability.APPROVE_VISITOR,
                PersonaCapability.BOOK_AMENITY,
                PersonaCapability.RAISE_COMPLAINT
                // Family members cannot view/pay financial ledgers or vote in AGM
        ));

        MATRIX.put(SocietyPersona.GUARD, EnumSet.of(
                PersonaCapability.GATE_CHECKIN_SCAN
                // Guards cannot view financial invoices, book resident amenities, or manage bylaws
        ));

        MATRIX.put(SocietyPersona.ESTATE_MANAGER, EnumSet.of(
                PersonaCapability.GATE_CHECKIN_SCAN,
                PersonaCapability.BOOK_AMENITY,
                PersonaCapability.RAISE_COMPLAINT,
                PersonaCapability.MANAGE_HELPDESK_SLA
                // Estate managers handle society ops/SLAs but not MC bylaws or cross-society data
        ));

        MATRIX.put(SocietyPersona.MC_ADMIN, EnumSet.of(
                PersonaCapability.APPROVE_VISITOR,
                PersonaCapability.GATE_CHECKIN_SCAN,
                PersonaCapability.BOOK_AMENITY,
                PersonaCapability.RAISE_COMPLAINT,
                PersonaCapability.VIEW_OWN_DUES,
                PersonaCapability.PAY_MAINTENANCE_DUES,
                PersonaCapability.VOTE_OWNER_AGM,
                PersonaCapability.MANAGE_FLAT_OCCUPANCY,
                PersonaCapability.MANAGE_HELPDESK_SLA,
                PersonaCapability.MANAGE_SOCIETY_GOVERNANCE
        ));

        MATRIX.put(SocietyPersona.PLATFORM_SUPER_ADMIN, EnumSet.allOf(PersonaCapability.class));
    }

    private static final Set<SocietyPersona> FLAT_SCOPED_PERSONAS = EnumSet.of(
            SocietyPersona.FLAT_OWNER,
            SocietyPersona.TENANT,
            SocietyPersona.FAMILY_MEMBER
    );

    private static final Set<PersonaCapability> FLAT_SCOPED_CAPABILITIES = EnumSet.of(
            PersonaCapability.APPROVE_VISITOR,
            PersonaCapability.BOOK_AMENITY,
            PersonaCapability.RAISE_COMPLAINT,
            PersonaCapability.VIEW_OWN_DUES,
            PersonaCapability.PAY_MAINTENANCE_DUES,
            PersonaCapability.VOTE_OWNER_AGM,
            PersonaCapability.MANAGE_FLAT_OCCUPANCY
    );

    public static SocietyPersona resolvePersona(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            throw new AccessDeniedException("Missing role");
        }
        String norm = rawRole.trim().toUpperCase(Locale.ROOT);
        return switch (norm) {
            case "OWNER", "FLAT_OWNER" -> SocietyPersona.FLAT_OWNER;
            case "TENANT", "RESIDENT", "MEMBER", "USER" -> SocietyPersona.TENANT;
            case "FAMILY", "FAMILY_MEMBER", "DEPENDENT" -> SocietyPersona.FAMILY_MEMBER;
            case "GUARD", "SECURITY_GUARD" -> SocietyPersona.GUARD;
            case "ESTATE_MANAGER", "COMMUNITY_MGMT_ADMIN", "HELPDESK_ADMIN", "BOOKINGS_ADMIN", "STAFF" ->
                    SocietyPersona.ESTATE_MANAGER;
            case "MC_ADMIN", "COMMUNITY_ADMIN", "ADMIN" -> SocietyPersona.MC_ADMIN;
            case "SUPER_ADMIN", "PLATFORM_SUPER_ADMIN" -> SocietyPersona.PLATFORM_SUPER_ADMIN;
            default -> throw new AccessDeniedException("Unrecognized community role: " + rawRole);
        };
    }

    public boolean isAuthorized(
            ActorContext actor,
            Long targetCommunityId,
            Long targetFlatId,
            PersonaCapability capability
    ) {
        if (actor == null || capability == null) {
            return false;
        }
        SocietyPersona persona;
        try {
            persona = resolvePersona(actor.rawRole());
        } catch (AccessDeniedException ex) {
            return false;
        }

        if (!MATRIX.getOrDefault(persona, Set.of()).contains(capability)) {
            return false;
        }

        // Strict cross-society isolation barrier (even MC_ADMIN cannot cross societies)
        if (persona != SocietyPersona.PLATFORM_SUPER_ADMIN) {
            if (actor.communityId() == null || targetCommunityId == null
                    || !actor.communityId().equals(targetCommunityId)) {
                return false;
            }
        }

        // Strict flat isolation for resident personas inside the same society
        if (FLAT_SCOPED_PERSONAS.contains(persona) && FLAT_SCOPED_CAPABILITIES.contains(capability)) {
            if (targetFlatId != null && (actor.flatId() == null || !actor.flatId().equals(targetFlatId))) {
                return false;
            }
        }

        return true;
    }

    public void assertAuthorized(
            ActorContext actor,
            Long targetCommunityId,
            Long targetFlatId,
            PersonaCapability capability
    ) {
        if (!isAuthorized(actor, targetCommunityId, targetFlatId, capability)) {
            throw new AccessDeniedException(
                    "Access denied for role=" + (actor != null ? actor.rawRole() : "null")
                            + " capability=" + capability
                            + " targetCommunityId=" + targetCommunityId
                            + " targetFlatId=" + targetFlatId);
        }
    }
}
