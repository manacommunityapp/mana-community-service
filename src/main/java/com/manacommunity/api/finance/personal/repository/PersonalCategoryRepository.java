package com.manacommunity.api.finance.personal.repository;

import com.manacommunity.api.finance.personal.entity.PersonalCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonalCategoryRepository extends JpaRepository<PersonalCategory, String> {
    @Query("SELECT c FROM PersonalCategory c WHERE (c.user.id = :userId OR c.user IS NULL) AND c.parentId IS NULL ORDER BY c.name ASC")
    List<PersonalCategory> findTopLevelCategoriesForUser(@Param("userId") Long userId);

    @Query("SELECT c FROM PersonalCategory c WHERE (c.user.id = :userId OR c.user IS NULL) AND c.parentId = :parentId ORDER BY c.name ASC")
    List<PersonalCategory> findSubcategoriesForUser(@Param("userId") Long userId, @Param("parentId") String parentId);

    @Query("SELECT c FROM PersonalCategory c WHERE (c.user.id = :userId OR c.user IS NULL) ORDER BY c.name ASC")
    List<PersonalCategory> findAllForUser(@Param("userId") Long userId);
}
