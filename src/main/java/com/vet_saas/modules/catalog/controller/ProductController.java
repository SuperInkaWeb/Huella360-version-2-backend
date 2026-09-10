package com.vet_saas.modules.catalog.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.catalog.dto.CreateProductDto;
import com.vet_saas.modules.catalog.dto.ProductResponse;
import com.vet_saas.modules.catalog.dto.UpdateProductDto;
import com.vet_saas.modules.catalog.service.ProductService;
import com.vet_saas.modules.user.model.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal Usuario usuario,
            @RequestPart("data") @Valid CreateProductDto dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        ProductResponse response = productService.createProduct(usuario, dto, images);
        return ResponseEntity.ok(ApiResponse.success(response, "Producto creado exitosamente"));
    }

    @GetMapping("/my-products")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getMyProducts(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort) {

        Sort.Direction direction = sort.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, "createdAt"));

        Page<ProductResponse> result = productService.getMyProducts(usuario, pageable);
        return ResponseEntity.ok(ApiResponse.success(result, "Productos de la empresa"));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long id,
            @RequestPart("data") @Valid UpdateProductDto dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(defaultValue = "false") boolean replaceImages) {

        ProductResponse response = productService.updateProduct(usuario, id, dto, images, replaceImages);
        return ResponseEntity.ok(ApiResponse.success(response, "Producto actualizado exitosamente"));
    }

    /**
     * PATCH /api/v1/products/{id} (JSON variant sin imágenes)
     * Actualizar metadatos de un producto sin cambiar imágenes.
     * Requiere rol EMPRESA.
     */
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductJson(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long id,
            @RequestBody @Valid UpdateProductDto dto) {

        ProductResponse response = productService.updateProduct(usuario, id, dto, null, false);
        return ResponseEntity.ok(ApiResponse.success(response, "Producto actualizado exitosamente"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPRESA')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long id) {
        productService.softDeleteProduct(usuario, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Producto eliminado exitosamente"));
    }
}