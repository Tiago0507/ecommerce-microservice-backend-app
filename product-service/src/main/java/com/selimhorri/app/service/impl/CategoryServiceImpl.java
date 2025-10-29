package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.exception.custom.DuplicateResourceException;
import com.selimhorri.app.exception.custom.ForbiddenOperationException;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.helper.CategoryMappingHelper;
import com.selimhorri.app.repository.CategoryRepository;
import com.selimhorri.app.repository.ProductRepository;
import com.selimhorri.app.service.CategoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    public List<CategoryDto> findAll() {
        log.info("Fetching all categories");
        return this.categoryRepository.findAllNonReserved()
                .stream()
                .map(CategoryMappingHelper::map)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public CategoryDto findById(final Integer categoryId) {
        log.info("Fetching category with id: {}", categoryId);
        return this.categoryRepository.findNonReservedById(categoryId)
                .map(CategoryMappingHelper::map)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND, categoryId));
    }

    @Override
    @Transactional
    public CategoryDto save(final CategoryDto categoryDto) {
        log.info("Saving new category");

        if (categoryDto.getCategoryTitle() == null || categoryDto.getCategoryTitle().trim().isEmpty()) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD, "Category title is required");
        }

        String normalizedTitle = categoryDto.getCategoryTitle().trim();

        boolean nameExists = this.categoryRepository.existsByCategoryTitleIgnoreCase(normalizedTitle);
        if (nameExists) {
            throw new DuplicateResourceException(ErrorCode.CATEGORY_TITLE_ALREADY_EXISTS, normalizedTitle);
        }

        categoryDto.setParentCategoryDto(null);
        categoryDto.setSubCategoriesDtos(null);
        categoryDto.setCategoryId(null);

        return CategoryMappingHelper.map(
                this.categoryRepository.save(CategoryMappingHelper.map(categoryDto)));
    }

    @Override
    @Transactional
    public CategoryDto update(final CategoryDto categoryDto) {
        log.info("Updating category with id: {}", categoryDto.getCategoryId());

        if (categoryDto.getCategoryId() == null) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD, "Category ID is required for update");
        }

        if (categoryDto.getCategoryTitle() == null || categoryDto.getCategoryTitle().trim().isEmpty()) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD, "Category title is required");
        }

        String normalizedTitle = categoryDto.getCategoryTitle().trim();

        Category existingCategory = this.categoryRepository.findById(categoryDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND, categoryDto.getCategoryId()));

        boolean nameExists = this.categoryRepository.existsByCategoryTitleIgnoreCaseAndCategoryIdNot(
                normalizedTitle, categoryDto.getCategoryId());

        if (nameExists) {
            throw new DuplicateResourceException(ErrorCode.CATEGORY_TITLE_ALREADY_EXISTS, normalizedTitle);
        }

        existingCategory.setCategoryTitle(normalizedTitle);
        existingCategory.setParentCategory(null);
        existingCategory.setSubCategories(null);

        return CategoryMappingHelper.map(this.categoryRepository.save(existingCategory));
    }

    @Override
    @Transactional
    public CategoryDto update(final Integer categoryId, final CategoryDto categoryDto) {
        log.info("Updating category with id: {}", categoryId);

        if (categoryId == null) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD, "Category ID is required");
        }

        if (categoryDto.getCategoryTitle() == null || categoryDto.getCategoryTitle().trim().isEmpty()) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD, "Category title is required");
        }

        String normalizedTitle = categoryDto.getCategoryTitle().trim();

        Category existingCategory = this.categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND, categoryId));

        boolean nameExists = this.categoryRepository.existsByCategoryTitleIgnoreCaseAndCategoryIdNot(
                normalizedTitle, categoryId);

        if (nameExists) {
            throw new DuplicateResourceException(ErrorCode.CATEGORY_TITLE_ALREADY_EXISTS, normalizedTitle);
        }

        existingCategory.setCategoryTitle(normalizedTitle);
        existingCategory.setParentCategory(null);
        existingCategory.setSubCategories(null);

        return CategoryMappingHelper.map(this.categoryRepository.save(existingCategory));
    }

    @Override
    @Transactional
    public void deleteById(final Integer categoryId) {
        log.info("Deleting category with id: {}", categoryId);

        Category category = this.categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND, categoryId));

        String categoryName = category.getCategoryTitle().toLowerCase().trim();
        if ("deleted".equals(categoryName) || "no category".equals(categoryName)) {
            throw new ForbiddenOperationException(
                    ErrorCode.RESERVED_CATEGORY_DELETE,
                    "Cannot delete reserved categories: 'Deleted' or 'No Category'");
        }

        Category noCategory = this.categoryRepository.findByCategoryTitleIgnoreCase("No Category")
                .orElseThrow(() -> new InvalidInputException(ErrorCode.INVALID_INPUT,
                        "The 'No Category' category is required but not found in database"));

        this.productRepository.updateCategoryForProducts(categoryId, noCategory);
        this.categoryRepository.delete(category);
    }
}