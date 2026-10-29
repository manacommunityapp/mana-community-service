package com.manacommunity.api.community;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/community/directory")
@RequiredArgsConstructor
public class DirectoryController {

    private final AppUserRepository appUserRepository;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String block,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser me = loggedInUserService.resolve(principal);
        Long communityId = me.getCommunity().getId();
        List<AppUser> users = appUserRepository.findByCommunityId(communityId);
        return ResponseEntity.ok(users.stream()
                .filter(u -> block == null || block.equalsIgnoreCase(u.getBlock()))
                .filter(u -> search == null ||
                        (u.getFullName() != null && u.getFullName().toLowerCase().contains(search.toLowerCase())) ||
                        (u.getFlatNo() != null && u.getFlatNo().toLowerCase().contains(search.toLowerCase())))
                .map(this::toDirectoryEntry)
                .collect(Collectors.toList()));
    }

    @GetMapping("/flat/{flatNumber}")
    public ResponseEntity<List<Map<String, Object>>> getByFlat(
            @PathVariable String flatNumber,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser me = loggedInUserService.resolve(principal);
        List<AppUser> users = appUserRepository.findByCommunityId(me.getCommunity().getId())
                .stream()
                .filter(u -> flatNumber.equalsIgnoreCase(u.getFlatNo()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users.stream().map(this::toDirectoryEntry).collect(Collectors.toList()));
    }

    @GetMapping("/blocks")
    public ResponseEntity<List<String>> getBlocks(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser me = loggedInUserService.resolve(principal);
        List<String> blocks = appUserRepository.findByCommunityId(me.getCommunity().getId())
                .stream()
                .map(AppUser::getBlock)
                .filter(b -> b != null && !b.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return ResponseEntity.ok(blocks);
    }

    private Map<String, Object> toDirectoryEntry(AppUser u) {
        return Map.of(
                "id", u.getId(),
                "fullName", u.getFullName() != null ? u.getFullName() : "",
                "flatNo", u.getFlatNo() != null ? u.getFlatNo() : "",
                "block", u.getBlock() != null ? u.getBlock() : "",
                "phone", u.getPhone() != null ? u.getPhone() : "",
                "profilePicUrl", u.getProfilePicUrl() != null ? u.getProfilePicUrl() : ""
        );
    }
}
