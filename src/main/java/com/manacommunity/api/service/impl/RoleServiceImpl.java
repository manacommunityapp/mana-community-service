package com.manacommunity.api.service.impl;

import com.manacommunity.api.exception.DuplicateResourceException;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.repository.RoleRepository;
import com.manacommunity.api.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepo;

    private boolean isRestrictedRole(String roleName) {
        if (roleName == null) return false;
        String upper = roleName.trim().toUpperCase();
        return upper.equals("SUPER_ADMIN") || upper.equals("SUPERADMIN") || upper.equals("SUPER_ADMINISTRATOR")
                || upper.equals("COMMUNITY_ADMIN") || upper.equals("COMMUNITYADMIN") || upper.equals("COMMUNITY_ADMINISTRATOR") || upper.equals("COMMUNITY ADMIN");
    }

    @Override
    public List<Role> getAllRoles(Long communityId) {
        List<Role> allRoles;
        if (communityId != null) {
            allRoles = roleRepo.findByCommunityIdOrCommunityIdIsNull(communityId);
        } else {
            allRoles = roleRepo.findAll();
        }

        // Deduplicate by upper-cased trimmed role name.
        // If both community-scoped and global roles exist for the same name, prefer community-scoped.
        java.util.Map<String, Role> uniqueRoles = new java.util.LinkedHashMap<>();
        for (Role role : allRoles) {
            if (role == null || role.getName() == null || isRestrictedRole(role.getName())) {
                continue;
            }
            String key = role.getName().trim().toUpperCase();
            Role existing = uniqueRoles.get(key);
            if (existing == null) {
                uniqueRoles.put(key, role);
            } else if (existing.getCommunityId() == null && role.getCommunityId() != null) {
                // Replace global fallback with community-scoped role
                uniqueRoles.put(key, role);
            }
        }

        return new java.util.ArrayList<>(uniqueRoles.values());
    }

    @Override
    public List<Role> getAllRoles() {
        return getAllRoles(null);
    }

    @Override
    @Transactional
    public Role createRole(String name) {
        return createRole(name, null);
    }

    @Override
    @Transactional
    public Role createRole(String name, Long communityId) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Role name is required");
        }
        
        String normalizedName = name.trim().toUpperCase();
        if (isRestrictedRole(normalizedName)) {
            throw new InvalidInputException("Creation of system administrative roles is restricted.");
        }
        boolean exists = (communityId != null)
                ? roleRepo.existsByNameIgnoreCaseAndCommunityId(normalizedName, communityId)
                : roleRepo.existsByNameIgnoreCaseAndCommunityIdIsNull(normalizedName);
                
        if (exists) {
            throw new DuplicateResourceException("Role", "name", name);
        }
        
        Role newRole = Role.builder()
                .name(normalizedName)
                .communityId(communityId)
                .permissions(new java.util.HashSet<>())
                .build();
                
        return roleRepo.save(newRole);
    }

    @Override
    @Transactional
    public Role findOrCreateRole(String name) {
        return findOrCreateRole(name, null);
    }

    @Override
    @Transactional
    public Role findOrCreateRole(String name, Long communityId) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Role name is required");
        }
        
        String normalizedName = name.trim().toUpperCase();
        if (communityId != null) {
            return roleRepo.findByNameIgnoreCaseAndCommunityId(normalizedName, communityId)
                    .orElseGet(() -> roleRepo.save(Role.builder()
                            .name(normalizedName)
                            .communityId(communityId)
                            .permissions(new java.util.HashSet<>())
                            .build()));
        } else {
            return roleRepo.findByNameIgnoreCaseAndCommunityIdIsNull(normalizedName)
                    .orElseGet(() -> roleRepo.save(Role.builder()
                            .name(normalizedName)
                            .permissions(new java.util.HashSet<>())
                            .build()));
        }
    }
}
