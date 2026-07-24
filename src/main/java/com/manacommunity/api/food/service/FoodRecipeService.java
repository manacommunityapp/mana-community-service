package com.manacommunity.api.food.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.food.entity.*;
import com.manacommunity.api.food.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodRecipeService {

    private final FoodRecipeRepository recipeRepo;
    private final FoodRecipeIngredientRepository ingredientRepo;
    private final FoodRecipeCollectionRepository collectionRepo;
    private final FoodRecipeCollectionItemRepository collectionItemRepo;
    private final FoodRecipeCommentRepository commentRepo;
    private final FoodRecipeRatingRepository ratingRepo;

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getRecipes(Long communityId, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return recipeRepo.searchByCommunity(communityId, search, pageable).map(this::toResponse);
        }
        return recipeRepo.findByCommunityIdAndStatus(communityId, "PUBLISHED", pageable).map(this::toResponse);
    }

    @Transactional
    public Map<String, Object> getRecipeById(Long id, Long communityId) {
        FoodRecipe recipe = recipeRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", id));
        recipe.setViewCount(recipe.getViewCount() != null ? recipe.getViewCount() + 1 : 1);
        recipeRepo.save(recipe);

        Map<String, Object> response = toResponse(recipe);
        List<FoodRecipeIngredient> ingredients = ingredientRepo.findByRecipeId(id);
        response.put("ingredients", ingredients.stream().map(ing -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ing.getId());
            m.put("ingredientName", ing.getIngredientName());
            m.put("quantity", ing.getQuantity());
            m.put("unit", ing.getUnit());
            m.put("isOptional", ing.getIsOptional());
            m.put("substitute", ing.getSubstitute());
            return m;
        }).collect(Collectors.toList()));

        return response;
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getMyRecipes(Long userId, Pageable pageable) {
        return recipeRepo.findByAuthorId(userId, pageable).map(this::toResponse);
    }

    @Transactional
    public Map<String, Object> createRecipe(Map<String, Object> request, AppUser user, Community community) {
        String title = (String) request.get("title");
        String slug = title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");

        FoodRecipe recipe = FoodRecipe.builder()
                .title(title)
                .slug(slug)
                .description((String) request.get("description"))
                .cuisineType((String) request.get("cuisineType"))
                .mealType((String) request.get("mealType"))
                .courseType(request.get("courseType") != null ?
                        FoodRecipe.CourseType.valueOf((String) request.get("courseType")) : null)
                .difficulty(request.get("difficulty") != null ?
                        FoodRecipe.Difficulty.valueOf((String) request.get("difficulty")) : null)
                .prepTime(request.get("prepTime") != null ?
                        Integer.valueOf(request.get("prepTime").toString()) : null)
                .cookTime(request.get("cookTime") != null ?
                        Integer.valueOf(request.get("cookTime").toString()) : null)
                .totalTime(request.get("totalTime") != null ?
                        Integer.valueOf(request.get("totalTime").toString()) : null)
                .servings(request.get("servings") != null ?
                        Integer.valueOf(request.get("servings").toString()) : null)
                .calories(request.get("calories") != null ?
                        Integer.valueOf(request.get("calories").toString()) : null)
                .protein(request.get("protein") != null ?
                        new BigDecimal(request.get("protein").toString()) : null)
                .carbs(request.get("carbs") != null ?
                        new BigDecimal(request.get("carbs").toString()) : null)
                .fat(request.get("fat") != null ?
                        new BigDecimal(request.get("fat").toString()) : null)
                .instructions((String) request.get("instructions"))
                .videoUrl((String) request.get("videoUrl"))
                .imageUrl((String) request.get("imageUrl"))
                .isVeg(request.get("isVeg") != null ? (Boolean) request.get("isVeg") : null)
                .isVegan(request.get("isVegan") != null ? (Boolean) request.get("isVegan") : null)
                .status(FoodRecipe.RecipeStatus.PUBLISHED)
                .sourceType(FoodRecipe.RecipeSourceType.COMMUNITY)
                .author(user)
                .community(community)
                .build();

        return toResponse(recipeRepo.save(recipe));
    }

    @Transactional
    public Map<String, Object> updateRecipe(Long id, Map<String, Object> request, Long communityId) {
        FoodRecipe recipe = recipeRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", id));

        if (request.containsKey("title")) {
            recipe.setTitle((String) request.get("title"));
            recipe.setSlug(((String) request.get("title")).toLowerCase()
                    .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", ""));
        }
        if (request.containsKey("description")) recipe.setDescription((String) request.get("description"));
        if (request.containsKey("cuisineType")) recipe.setCuisineType((String) request.get("cuisineType"));
        if (request.containsKey("mealType")) recipe.setMealType((String) request.get("mealType"));
        if (request.containsKey("courseType"))
            recipe.setCourseType(FoodRecipe.CourseType.valueOf((String) request.get("courseType")));
        if (request.containsKey("difficulty"))
            recipe.setDifficulty(FoodRecipe.Difficulty.valueOf((String) request.get("difficulty")));
        if (request.containsKey("prepTime"))
            recipe.setPrepTime(Integer.valueOf(request.get("prepTime").toString()));
        if (request.containsKey("cookTime"))
            recipe.setCookTime(Integer.valueOf(request.get("cookTime").toString()));
        if (request.containsKey("totalTime"))
            recipe.setTotalTime(Integer.valueOf(request.get("totalTime").toString()));
        if (request.containsKey("servings"))
            recipe.setServings(Integer.valueOf(request.get("servings").toString()));
        if (request.containsKey("calories"))
            recipe.setCalories(Integer.valueOf(request.get("calories").toString()));
        if (request.containsKey("protein"))
            recipe.setProtein(new BigDecimal(request.get("protein").toString()));
        if (request.containsKey("carbs"))
            recipe.setCarbs(new BigDecimal(request.get("carbs").toString()));
        if (request.containsKey("fat"))
            recipe.setFat(new BigDecimal(request.get("fat").toString()));
        if (request.containsKey("instructions")) recipe.setInstructions((String) request.get("instructions"));
        if (request.containsKey("videoUrl")) recipe.setVideoUrl((String) request.get("videoUrl"));
        if (request.containsKey("imageUrl")) recipe.setImageUrl((String) request.get("imageUrl"));
        if (request.containsKey("isVeg")) recipe.setIsVeg((Boolean) request.get("isVeg"));
        if (request.containsKey("isVegan")) recipe.setIsVegan((Boolean) request.get("isVegan"));

        return toResponse(recipeRepo.save(recipe));
    }

    @Transactional
    public void deleteRecipe(Long id, Long communityId) {
        FoodRecipe recipe = recipeRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", id));
        recipeRepo.delete(recipe);
    }

    @Transactional
    public Map<String, Object> rateRecipe(Long recipeId, int rating, AppUser user) {
        FoodRecipe recipe = recipeRepo.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", recipeId));

        Optional<FoodRecipeRating> existing = ratingRepo.findByRecipeIdAndUserId(recipeId, user.getId());
        FoodRecipeRating recipeRating;
        if (existing.isPresent()) {
            recipeRating = existing.get();
            recipeRating.setRating(rating);
        } else {
            recipeRating = FoodRecipeRating.builder()
                    .recipe(recipe)
                    .user(user)
                    .rating(rating)
                    .community(recipe.getCommunity())
                    .build();
        }
        ratingRepo.save(recipeRating);

        Map<String, Object> map = new HashMap<>();
        map.put("id", recipeRating.getId());
        map.put("recipeId", recipeId);
        map.put("userId", user.getId());
        map.put("rating", recipeRating.getRating());
        map.put("createdAt", recipeRating.getCreatedAt());
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCollections(Long userId, Long communityId) {
        return collectionRepo.findByUserIdAndCommunityId(userId, communityId)
                .stream().map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("name", c.getName());
                    map.put("description", c.getDescription());
                    map.put("imageUrl", c.getImageUrl());
                    map.put("userId", c.getUser() != null ? c.getUser().getId() : null);
                    map.put("isPublic", c.getIsPublic());
                    map.put("communityId", c.getCommunity() != null ? c.getCommunity().getId() : null);
                    map.put("createdAt", c.getCreatedAt());
                    map.put("updatedAt", c.getUpdatedAt());
                    return map;
                }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createCollection(String name, String description, AppUser user, Community community) {
        FoodRecipeCollection collection = FoodRecipeCollection.builder()
                .name(name)
                .description(description)
                .user(user)
                .isPublic(true)
                .community(community)
                .build();
        collection = collectionRepo.save(collection);

        Map<String, Object> map = new HashMap<>();
        map.put("id", collection.getId());
        map.put("name", collection.getName());
        map.put("description", collection.getDescription());
        map.put("userId", collection.getUser() != null ? collection.getUser().getId() : null);
        map.put("isPublic", collection.getIsPublic());
        map.put("communityId", collection.getCommunity() != null ? collection.getCommunity().getId() : null);
        map.put("createdAt", collection.getCreatedAt());
        map.put("updatedAt", collection.getUpdatedAt());
        return map;
    }

    @Transactional
    public Map<String, Object> addToCollection(Long collectionId, Long recipeId) {
        FoodRecipeCollection collection = collectionRepo.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("RecipeCollection", collectionId));
        FoodRecipe recipe = recipeRepo.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", recipeId));

        FoodRecipeCollectionItem item = FoodRecipeCollectionItem.builder()
                .collection(collection)
                .recipe(recipe)
                .build();
        item = collectionItemRepo.save(item);

        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getId());
        map.put("collectionId", collectionId);
        map.put("recipeId", recipeId);
        map.put("createdAt", item.getCreatedAt());
        return map;
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getComments(Long recipeId, Pageable pageable) {
        return commentRepo.findByRecipeId(recipeId, pageable)
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("recipeId", c.getRecipe() != null ? c.getRecipe().getId() : null);
                    map.put("userId", c.getUser() != null ? c.getUser().getId() : null);
                    map.put("commentText", c.getCommentText());
                    map.put("parentId", c.getParent() != null ? c.getParent().getId() : null);
                    map.put("likes", c.getLikes());
                    map.put("createdAt", c.getCreatedAt());
                    map.put("updatedAt", c.getUpdatedAt());
                    return map;
                });
    }

    @Transactional
    public Map<String, Object> addComment(Long recipeId, String commentText, Long parentId, AppUser user) {
        FoodRecipe recipe = recipeRepo.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", recipeId));

        FoodRecipeComment parent = null;
        if (parentId != null) {
            parent = commentRepo.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException("RecipeComment", parentId));
        }

        FoodRecipeComment comment = FoodRecipeComment.builder()
                .recipe(recipe)
                .user(user)
                .commentText(commentText)
                .parent(parent)
                .community(recipe.getCommunity())
                .build();
        comment = commentRepo.save(comment);

        Map<String, Object> map = new HashMap<>();
        map.put("id", comment.getId());
        map.put("recipeId", recipeId);
        map.put("userId", user.getId());
        map.put("commentText", comment.getCommentText());
        map.put("parentId", parentId);
        map.put("createdAt", comment.getCreatedAt());
        return map;
    }

    private Map<String, Object> toResponse(FoodRecipe r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("title", r.getTitle());
        map.put("slug", r.getSlug());
        map.put("description", r.getDescription());
        map.put("cuisineType", r.getCuisineType());
        map.put("mealType", r.getMealType());
        map.put("courseType", r.getCourseType() != null ? r.getCourseType().name() : null);
        map.put("difficulty", r.getDifficulty() != null ? r.getDifficulty().name() : null);
        map.put("prepTime", r.getPrepTime());
        map.put("cookTime", r.getCookTime());
        map.put("totalTime", r.getTotalTime());
        map.put("servings", r.getServings());
        map.put("calories", r.getCalories());
        map.put("protein", r.getProtein());
        map.put("carbs", r.getCarbs());
        map.put("fat", r.getFat());
        map.put("instructions", r.getInstructions());
        map.put("videoUrl", r.getVideoUrl());
        map.put("imageUrl", r.getImageUrl());
        map.put("isVeg", r.getIsVeg());
        map.put("isVegan", r.getIsVegan());
        map.put("status", r.getStatus() != null ? r.getStatus().name() : null);
        map.put("viewCount", r.getViewCount());
        map.put("likeCount", r.getLikeCount());
        map.put("authorId", r.getAuthor() != null ? r.getAuthor().getId() : null);
        map.put("communityId", r.getCommunity() != null ? r.getCommunity().getId() : null);
        map.put("createdAt", r.getCreatedAt());
        map.put("updatedAt", r.getUpdatedAt());
        return map;
    }
}
