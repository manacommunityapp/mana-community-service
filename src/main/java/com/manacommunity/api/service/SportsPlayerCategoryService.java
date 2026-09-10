package com.manacommunity.api.service;

import com.manacommunity.api.dto.SportsPlayerCategoryRequest;
import com.manacommunity.api.model.SportsPlayerCategory;
import com.manacommunity.api.user.model.AppUser;
import java.util.List;

public interface SportsPlayerCategoryService {
    List<SportsPlayerCategory> getCategories(AppUser user);
    SportsPlayerCategory createCategory(SportsPlayerCategoryRequest req);
    SportsPlayerCategory updateCategory(Long id, SportsPlayerCategoryRequest req);
    void deleteCategory(Long id);
}
