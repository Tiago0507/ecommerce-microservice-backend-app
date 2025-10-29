package com.selimhorri.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    @Query("SELECT p FROM Product p WHERE p.category.categoryTitle <> 'Deleted'")
    List<Product> findAllWithoutDeleted();

    @Query("SELECT p FROM Product p WHERE p.id = :productId AND p.category.categoryTitle <> 'Deleted'")
    Optional<Product> findByIdWithoutDeleted(Integer productId);

    @Modifying
    @Query("UPDATE Product p SET p.category = :newCategory WHERE p.category.categoryId = :oldCategoryId")
    void updateCategoryForProducts(@Param("oldCategoryId") Integer oldCategoryId,
            @Param("newCategory") Category newCategory);

    @Modifying
    @Query("UPDATE Product p SET p.category.categoryId = :newCategoryId WHERE p.category.categoryId = :oldCategoryId")
    void updateCategoryIdForProducts(@Param("oldCategoryId") Integer oldCategoryId,
            @Param("newCategoryId") Integer newCategoryId);
}