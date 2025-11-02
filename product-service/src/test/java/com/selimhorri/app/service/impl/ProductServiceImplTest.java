package com.selimhorri.app.service.impl;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.repository.CategoryRepository;
import com.selimhorri.app.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category existingCategory;
    private Product existingProduct;

    @BeforeEach
    void setUp() {
        existingCategory = Category.builder()
                .categoryId(100)
                .categoryTitle("Electronics")
                .imageUrl("cat.png")
                .build();

        existingProduct = Product.builder()
                .productId(1)
                .productTitle("Phone")
                .imageUrl("img.png")
                .sku("SKU-1")
                .priceUnit(999.0)
                .quantity(10)
                .category(existingCategory)
                .build();
    }

    @Test
    void findById_WhenProductMissing_ThrowsResourceNotFoundException() {
        // Arrange
        when(productRepository.findByIdWithoutDeleted(99)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> productService.findById(99));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void findAll_WhenProductsExist_ReturnsMappedDtos() {
        // Arrange
        when(productRepository.findAllWithoutDeleted()).thenReturn(Arrays.asList(existingProduct));

        // Act
        List<ProductDto> result = productService.findAll();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductTitle()).isEqualTo("Phone");
        assertThat(result.get(0).getCategoryDto()).isNotNull();
        assertThat(result.get(0).getCategoryDto().getCategoryTitle()).isEqualTo("Electronics");
    }

    @Test
    void save_WhenMissingMandatoryFields_ThrowsInvalidInputException() {
        // Arrange
        ProductDto dto = ProductDto.builder()
                .productTitle("") // missing
                .imageUrl(null) // missing
                .sku(null)
                .priceUnit(null)
                .quantity(null)
                .categoryDto(null)
                .build();

        // Act + Assert
        assertThrows(InvalidInputException.class, () -> productService.save(dto));
    }

    @Test
    void save_WhenCategoryNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        ProductDto dto = validProductDto();
        when(categoryRepository.findById(100)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> productService.save(dto));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
        verify(productRepository, never()).save(any());
    }

    @Test
    void save_WhenValid_PersistsAndReturnsDto() {
        // Arrange
        ProductDto dto = validProductDto();
        when(categoryRepository.findById(100)).thenReturn(Optional.of(existingCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setProductId(10);
            return p;
        });

        // Act
        ProductDto result = productService.save(dto);

        // Assert
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();

        assertThat(saved.getSku()).isEqualTo("SKU-NEW");
        assertThat(result.getProductId()).isEqualTo(10);
    }

    @Test
    void update_WhenProductIdDoesNotExist_ThrowsResourceNotFoundException() {
        // Arrange
        ProductDto dto = ProductDto.builder().productId(55).build();
        when(productRepository.existsById(55)).thenReturn(false);

        // Act + Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> productService.update(dto));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void update_WithIdParam_WhenProductMissing_ThrowsResourceNotFoundException() {
        // Arrange
        ProductDto dto = ProductDto.builder().productTitle("New").build();
        when(productRepository.findById(77)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> productService.update(77, dto));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void deleteById_WhenProductExists_SetsDeletedCategory() {
        // Arrange
        Category deleted = Category.builder().categoryId(999).categoryTitle("Deleted").build();
        Product prod = Product.builder().productId(5).category(existingCategory).build();

        when(productRepository.findByIdWithoutDeleted(5)).thenReturn(Optional.of(prod));
        when(categoryRepository.findByCategoryTitle("Deleted")).thenReturn(Optional.of(deleted));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        productService.deleteById(5);

        // Assert
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getCategory()).isNotNull();
        assertThat(saved.getCategory().getCategoryTitle()).isEqualTo("Deleted");
    }

    private ProductDto validProductDto() {
        return ProductDto.builder()
                .productTitle("Phone")
                .imageUrl("img")
                .sku("SKU-NEW")
                .priceUnit(10.0)
                .quantity(1)
                .categoryDto(CategoryDto.builder().categoryId(100).categoryTitle("Electronics").build())
                .build();
    }
}
