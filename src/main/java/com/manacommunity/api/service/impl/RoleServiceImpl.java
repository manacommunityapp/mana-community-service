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
    public List<Role> getAllRoles() {
        return roleRepo.findAll().stream()
                .filter(r -> r.getName() != null && !isRestrictedRole(r.getName()))
                .toList();
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
