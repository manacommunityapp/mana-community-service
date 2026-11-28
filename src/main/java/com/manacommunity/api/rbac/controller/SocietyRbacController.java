package com.manacommunity.api.rbac.controller;

import com.manacommunity.api.rbac.service.SocietyPersonaRbacService;
import com.manacommunity.api.rbac.service.SocietyPersonaRbacService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/rbac")
@RequiredArgsConstructor
public class SocietyRbacController {

    private final SocietyPersonaRbacService rbacService;

    @PostMapping("/evaluate")
    public ResponseEntity<Map<String, Object>> evaluate(@RequestBody Map<String, Object> body) {
        ActorContext actor = ActorContext.builder()
                .userId(Long.parseLong(body.get("userId").toString()))
                .rawRole(body.get("rawRole").toString())
                .communityId(body.get("actorCommunityId") != null ? Long.parseLong(body.get("actorCommunityId").toString()) : null)
                .flatId(body.get("actorFlatId") != null ? Long.parseLong(body.get("actorFlatId").toString()) : null)
                .build();

        Long targetCommunityId = body.get("targetCommunityId") != null
                ? Long.parseLong(body.get("targetCommunityId").toString()) : null;
        Long targetFlatId = body.get("targetFlatId") != null
                ? Long.parseLong(body.get("targetFlatId").toString()) : null;
        PersonaCapability cap = PersonaCapability.valueOf(body.get("capability").toString());

        boolean allowed = rbacService.isAuthorized(actor, targetCommunityId, targetFlatId, cap);
        return ResponseEntity.ok(Map.of(
                "allowed", allowed,
                "persona", SocietyPersonaRbacService.resolvePersona(actor.rawRole()).name(),
                "capability", cap.name()
        ));
    }
}
