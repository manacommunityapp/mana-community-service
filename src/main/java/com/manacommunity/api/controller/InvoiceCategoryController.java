package com.manacommunity.api.controller;

import com.manacommunity.api.dto.InvoiceCategoryDto;
import com.manacommunity.api.service.InvoiceCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/finance/categories")
@RequiredArgsConstructor
public class InvoiceCategoryController {

    private final InvoiceCategoryService service;

    @GetMapping
    public ResponseEntity<List<InvoiceCategoryDto>> list(@AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getCommunityId();
        return ResponseEntity.ok(service.listCategories(communityId));
    }

    @PostMapping
    public ResponseEntity<InvoiceCategoryDto> create(@RequestBody InvoiceCategoryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveCategory(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
