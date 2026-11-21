package com.manacommunity.api.document.service;

import com.manacommunity.api.document.entity.DocAccessLevel;
import com.manacommunity.api.document.entity.Document;
import com.manacommunity.api.document.repository.DocumentAccessGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentAccessService {

    private final DocumentAccessGrantRepository grantRepo;

    public boolean canAccess(Document document, Long userId, String userRole, String permission) {
        if (userId != null && userId.equals(document.getOwnerUserId())) return true;
        if ("SUPER_ADMIN".equalsIgnoreCase(userRole)) return true;
        if ("COMMUNITY_ADMIN".equalsIgnoreCase(userRole) &&
                !DocAccessLevel.OWNER_ONLY.equals(document.getAccessLevel())) return true;
        if (DocAccessLevel.PUBLIC.equals(document.getAccessLevel())) return true;
        if (DocAccessLevel.COMMUNITY.equals(document.getAccessLevel())) return true;
        if (userId != null && grantRepo.existsByDocumentIdAndGranteeTypeAndGranteeIdAndPermission(
                document.getId(), "USER", String.valueOf(userId), permission)) return true;
        if (userRole != null && grantRepo.existsByDocumentIdAndGranteeTypeAndGranteeIdAndPermission(
                document.getId(), "ROLE", userRole, permission)) return true;
        return false;
    }
}
