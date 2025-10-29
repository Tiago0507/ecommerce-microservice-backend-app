package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.ErrorCode;
import com.selimhorri.app.exception.custom.InvalidInputException;
import com.selimhorri.app.exception.custom.ResourceNotFoundException;
import com.selimhorri.app.helper.ProductMappingHelper;
import com.selimhorri.app.repository.CategoryRepository;
import com.selimhorri.app.repository.ProductRepository;
import com.selimhorri.app.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public List<ProductDto> findAll() {
        log.info("Fetching all products");
        return this.productRepository.findAllWithoutDeleted()
                .stream()
                .map(ProductMappingHelper::map)
                .distinct()
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public ProductDto findById(final Integer productId) {
        log.info("Fetching product with id: {}", productId);
        return this.productRepository.findByIdWithoutDeleted(productId)
                .map(ProductMappingHelper::map)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, productId));
    }

    @Override
    public ProductDto save(final ProductDto productDto) {
        log.info("Saving new product");

        if (productDto.getProductTitle() == null || productDto.getProductTitle().isEmpty()) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD, "Product title is required");
        }

        if (productDto.getImageUrl() == null || productDto.getImageUrl().isEmpty()) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD, "Image URL is required");
        }

        if (productDto.getSku() == null || productDto.getSku().isEmpty()) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD, "SKU is required");
        }

        if (productDto.getPriceUnit() == null) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD, "Price unit is required");
        }

        if (productDto.getQuantity() == null) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD, "Quantity is required");
        }

        if (productDto.getCategoryDto() == null || productDto.getCategoryDto().getCategoryId() == null) {
            throw new InvalidInputException(ErrorCode.MISSING_REQUIRED_FIELD, "Category is required");
        }

        Integer categoryId = productDto.getCategoryDto().getCategoryId();
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND, categoryId));

        productDto.setProductId(null);
        return ProductMappingHelper.map(this.productRepository.save(ProductMappingHelper.map(productDto)));
    }

    @Override
    public ProductDto update(final ProductDto productDto) {
        log.info("Updating product with id: {}", productDto.getProductId());

        if (productDto.getProductId() == null || !productRepository.existsById(productDto.getProductId())) {
            throw new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, productDto.getProductId());
        }

        return ProductMappingHelper.map(this.productRepository.save(ProductMappingHelper.map(productDto)));
    }

    @Override
    public ProductDto update(final Integer productId, final ProductDto productDto) {
        log.info("Updating product with id: {}", productId);

        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, productId));

        Product updatedProduct = ProductMappingHelper.map(productDto);
        updatedProduct.setProductId(existingProduct.getProductId());

        return ProductMappingHelper.map(this.productRepository.save(updatedProduct));
    }

    @Override
    public void deleteById(final Integer productId) {
        log.info("Soft deleting product with id: {}", productId);

        Product product = this.productRepository.findByIdWithoutDeleted(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, productId));

        Category deletedCategory = this.categoryRepository.findByCategoryTitle("Deleted")
                .orElseThrow(() -> new InvalidInputException(ErrorCode.INVALID_INPUT, 
                        "Category 'Deleted' not found in database"));

        product.setCategory(deletedCategory);
        this.productRepository.save(product);
    }
}