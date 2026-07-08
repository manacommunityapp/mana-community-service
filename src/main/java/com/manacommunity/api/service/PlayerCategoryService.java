package com.manacommunity.api.service;

import com.manacommunity.api.dto.PlayerCategoryRequest;
import com.manacommunity.api.model.PlayerCategory;
import com.manacommunity.api.user.model.AppUser;
import java.util.List;

public interface PlayerCategoryService {
    List<PlayerCategory> getCategories(AppUser user);
    PlayerCategory createCategory(PlayerCategoryRequest req);
    PlayerCategory updateCategory(Long id, PlayerCategoryRequest req);
    void deleteCategory(Long id);
}
