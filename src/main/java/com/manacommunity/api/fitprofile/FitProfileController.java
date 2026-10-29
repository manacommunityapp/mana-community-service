package com.manacommunity.api.fitprofile;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/jewellery")
@RequiredArgsConstructor
public class FitProfileController {

    public record FitProfileRequest(
            @NotNull @Pattern(regexp = "round|oval|square|heart|oblong|unknown") String faceShape,
            @DecimalMin("0.0") @DecimalMax("5.0") BigDecimal widthRatio,
            @DecimalMin("0.0") @DecimalMax("5.0") BigDecimal neckRatio,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal confidence) {}

    private final FitProfileService profiles;
    private final RecommendationService recommendations;
    private final LoggedInUserService loggedInUserService;

    @PutMapping("/fit-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> saveProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FitProfileRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        profiles.save(user.getId(), new FitProfileService.Profile(
                req.faceShape(), req.widthRatio(), req.neckRatio(), req.confidence()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/fit-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FitProfileService.Profile> getProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return profiles.getProfile(user.getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/fit-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        profiles.delete(user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recommendations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RecommendationService.Recommendation>> getRecommendations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "EARRINGS") String category,
            @RequestParam(defaultValue = "12") int limit) {
        AppUser user = loggedInUserService.resolve(principal);
        int capped = Math.min(Math.max(limit, 1), 30);
        return recommendations.forUser(user.getId(), category, capped)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
