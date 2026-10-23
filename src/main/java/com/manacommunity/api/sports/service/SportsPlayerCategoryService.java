package com.manacommunity.api.sports.service;

import com.manacommunity.api.sports.dto.SportsPlayerCategoryRequest;
import com.manacommunity.api.sports.model.SportsPlayerCategory;
import com.manacommunity.api.user.model.AppUser;
import java.util.List;

public interface SportsPlayerCategoryService {
    List<SportsPlayerCategory> getCategories(AppUser user);
    SportsPlayerCategory createCategory(SportsPlayerCategoryRequest req);
    SportsPlayerCategory updateCategory(Long id, SportsPlayerCategoryRequest req);
    void deleteCategory(Long id);
}
