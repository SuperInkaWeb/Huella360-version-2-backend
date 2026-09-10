package com.vet_saas.modules.catalog.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.catalog.dto.CreateCategoriaRequest;
import com.vet_saas.modules.catalog.dto.UpdateCategoriaRequest;
import com.vet_saas.modules.catalog.dto.CategoriaResponse;
import com.vet_saas.modules.catalog.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoriaResponse>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success(
                categoriaService.getAllActiveCategories(),
                "Categorías recuperadas con éxito"));
    }

    @GetMapping("/parents")
    public ResponseEntity<ApiResponse<List<CategoriaResponse>>> getParentCategories() {
        return ResponseEntity.ok(ApiResponse.success(
                categoriaService.getParentCategories(),
                "Categorías padre recuperadas con éxito"));
    }

    @GetMapping("/{padreId}/subcategories")
    public ResponseEntity<ApiResponse<List<CategoriaResponse>>> getSubcategories(@PathVariable Long padreId) {
        return ResponseEntity.ok(ApiResponse.success(
                categoriaService.getActiveSubcategories(padreId),
                "Subcategorías recuperadas con éxito"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoriaResponse>> createCategory(
            @RequestBody @Valid CreateCategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(categoriaService.createCategory(request), "Categoría creada con éxito"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoriaResponse>> updateCategory(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCategoriaRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                categoriaService.updateCategory(id, request),
                "Categoría actualizada con éxito"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoriaService.softDeleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Categoría eliminada con éxito"));
    }
}
