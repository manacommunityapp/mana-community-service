package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.MarketCategoryRequest;
import com.manacommunity.api.marketplace.dto.MarketCategoryResponse;
import com.manacommunity.api.marketplace.service.MarketCategoryService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/marketplace/categories", "/api/v1/marketplace/categories"})
@RequiredArgsConstructor
public class MarketCategoryController {

    private final MarketCategoryService categoryService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<MarketCategoryResponse>> getCategories() {
        return ResponseEntity.ok(categoryService.getAllActive());
    }

    @GetMapping("/{parentId}/subcategories")
    public ResponseEntity<List<MarketCategoryResponse>> getSubCategories(@PathVariable Long parentId) {
        return ResponseEntity.ok(categoryService.getSubCategories(parentId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Manage Categories')")
    public ResponseEntity<MarketCategoryResponse> createCategory(
            @Valid @RequestBody MarketCategoryRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.create(req, user.getCommunity()));
    }
}
