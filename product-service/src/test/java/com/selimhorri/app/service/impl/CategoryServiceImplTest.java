package com.selimhorri.app.service.impl;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.exception.custom.DuplicateResourceException;
import com.selimhorri.app.exception.custom.ForbiddenOperationException;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.repository.CategoryRepository;
import com.selimhorri.app.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category existing;

    @BeforeEach
    void setUp() {
        existing = Category.builder()
                .categoryId(10)
                .categoryTitle("Electronics")
                .imageUrl("img.png")
                .build();
    }

    @Test
    void save_WhenTitleMissing_ThrowsInvalidInputException() {
        // Arrange
        CategoryDto dto = CategoryDto.builder().categoryTitle(" ") .build();

        // Act + Assert
        InvalidInputException ex = assertThrows(InvalidInputException.class,
                () -> categoryService.save(dto));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MISSING_REQUIRED_FIELD);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void save_WhenTitleDuplicate_ThrowsDuplicateResourceException() {
        // Arrange
        CategoryDto dto = CategoryDto.builder().categoryTitle("Electronics").build();
        when(categoryRepository.existsByCategoryTitleIgnoreCase("Electronics"))
                .thenReturn(true);

        // Act + Assert
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> categoryService.save(dto));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_TITLE_ALREADY_EXISTS);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_WhenIdMissing_ThrowsInvalidInputException() {
        // Arrange
        CategoryDto dto = CategoryDto.builder().categoryTitle("Phones").build();

        // Act + Assert
        InvalidInputException ex = assertThrows(InvalidInputException.class,
                () -> categoryService.update(dto));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MISSING_REQUIRED_FIELD);
    }

    @Test
    void update_WhenTitleDuplicate_ThrowsDuplicateResourceException() {
        // Arrange
        CategoryDto dto = CategoryDto.builder().categoryId(10).categoryTitle("Phones").build();
        when(categoryRepository.findById(10)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByCategoryTitleIgnoreCaseAndCategoryIdNot("Phones", 10))
                .thenReturn(true);

        // Act + Assert
        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> categoryService.update(dto));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_TITLE_ALREADY_EXISTS);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteById_WhenReservedCategory_ThrowsForbiddenOperationException() {
        // Arrange
        Category reserved = Category.builder().categoryId(1).categoryTitle("Deleted").build();
        when(categoryRepository.findById(1)).thenReturn(Optional.of(reserved));

        // Act + Assert
        ForbiddenOperationException ex = assertThrows(ForbiddenOperationException.class,
                () -> categoryService.deleteById(1));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESERVED_CATEGORY_DELETE);
        verify(productRepository, never()).updateCategoryForProducts(anyInt(), any());
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteById_WhenValid_ReassignsProductsToNoCategoryAndDeletes() {
        // Arrange
        Category toDelete = Category.builder().categoryId(22).categoryTitle("Accessories").build();
        Category noCategory = Category.builder().categoryId(2).categoryTitle("No Category").build();
        when(categoryRepository.findById(22)).thenReturn(Optional.of(toDelete));
        when(categoryRepository.findByCategoryTitleIgnoreCase("No Category")).thenReturn(Optional.of(noCategory));

        // Act
        categoryService.deleteById(22);

        // Assert
        verify(productRepository).updateCategoryForProducts(22, noCategory);
        verify(categoryRepository).delete(toDelete);
    }
}
