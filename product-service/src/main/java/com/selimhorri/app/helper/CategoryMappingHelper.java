package com.selimhorri.app.helper;

import java.util.Optional;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.dto.CategoryDto;

public interface CategoryMappingHelper {
	
	public static CategoryDto map(final Category category) {

		return CategoryDto.builder()
				.categoryId(category.getCategoryId())
				.categoryTitle(category.getCategoryTitle())
				.imageUrl(category.getImageUrl())
				.build();
	}
	
	public static Category map(final CategoryDto categoryDto) {
		
		return Category.builder()
				.categoryId(categoryDto.getCategoryId())
				.categoryTitle(categoryDto.getCategoryTitle())
				.imageUrl(categoryDto.getImageUrl())

				.build();
	}
	
	
	
}









