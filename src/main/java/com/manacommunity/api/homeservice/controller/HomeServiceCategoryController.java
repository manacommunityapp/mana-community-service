package com.manacommunity.api.homeservice.controller;

import com.manacommunity.api.homeservice.model.entity.HomeServiceCategoryEntity;
import com.manacommunity.api.homeservice.repository.HomeServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController("homeServiceCategoryController")
@RequestMapping("/api/v1/home-services/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HomeServiceCategoryController {
    private final HomeServiceCategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<List<HomeServiceCategoryEntity>> getCategories() {
        return ResponseEntity.ok(categoryRepository.findByActiveTrueOrderByDisplayOrderAsc());
    }
}
