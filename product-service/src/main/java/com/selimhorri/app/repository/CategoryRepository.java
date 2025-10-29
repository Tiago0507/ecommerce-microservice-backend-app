package com.selimhorri.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.selimhorri.app.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    Optional<Category> findByCategoryTitle(String string);

    boolean existsByCategoryTitleIgnoreCaseAndCategoryIdNot(String categoryTitle, Integer categoryId);

    boolean existsByCategoryTitleIgnoreCase(String categoryTitle);

    Optional<Category> findByCategoryTitleIgnoreCase(String categoryTitle);

    @Query("SELECT c FROM Category c WHERE LOWER(c.categoryTitle) NOT IN ('deleted', 'no category')")
    List<Category> findAllNonReserved();

    @Query("SELECT c FROM Category c WHERE c.categoryId = :id AND LOWER(c.categoryTitle) NOT IN ('deleted', 'no category')")
    Optional<Category> findNonReservedById(@Param("id") Integer id);

}