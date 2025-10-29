package com.selimhorri.app.resource;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.CategoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/categories")
@Slf4j
@RequiredArgsConstructor
public class CategoryResource {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<DtoCollectionResponse<CategoryDto>> findAll() {
        log.info("Fetching all categories");
        return ResponseEntity.ok(new DtoCollectionResponse<>(this.categoryService.findAll()));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryDto> findById(@PathVariable("categoryId") final Integer categoryId) {
        log.info("Fetching category with id: {}", categoryId);
        return ResponseEntity.ok(this.categoryService.findById(categoryId));
    }

    @PostMapping
    public ResponseEntity<CategoryDto> save(
            @RequestBody @NotNull(message = "Request body cannot be null") @Valid final CategoryDto categoryDto) {
        log.info("Saving new category");
        return ResponseEntity.ok(this.categoryService.save(categoryDto));
    }

    @PutMapping
    public ResponseEntity<CategoryDto> update(
            @RequestBody @NotNull(message = "Request body cannot be null") @Valid final CategoryDto categoryDto) {
        log.info("Updating category");
        return ResponseEntity.ok(this.categoryService.update(categoryDto));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryDto> update(
            @PathVariable("categoryId") final Integer categoryId,
            @RequestBody @NotNull(message = "Request body cannot be null") @Valid final CategoryDto categoryDto) {
        log.info("Updating category with id: {}", categoryId);
        return ResponseEntity.ok(this.categoryService.update(categoryId, categoryDto));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Boolean> deleteById(@PathVariable("categoryId") final Integer categoryId) {
        log.info("Deleting category with id: {}", categoryId);
        this.categoryService.deleteById(categoryId);
        return ResponseEntity.ok(true);
    }
}