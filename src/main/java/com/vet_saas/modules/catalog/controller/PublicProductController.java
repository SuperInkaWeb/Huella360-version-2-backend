package com.vet_saas.modules.catalog.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.catalog.dto.ProductResponse;
import com.vet_saas.modules.catalog.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/products")
@RequiredArgsConstructor
public class PublicProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort) {

        String field = "createdAt";
        Sort.Direction direction = Sort.Direction.DESC;

        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            if (parts.length == 2) {
                field = parts[0].trim();
                direction = parts[1].trim().equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            } else if (sort.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }
        }

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, field));
        Page<ProductResponse> result = productService.getPublicProducts(q, category, pageable);
        return ResponseEntity.ok(ApiResponse.success(result, "Resultados de la búsqueda"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Producto encontrado"));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProductsByCompany(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort) {

        Sort.Direction direction = sort.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, "createdAt"));

        Page<ProductResponse> result = productService.getPublicProductsByCompany(companyId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result, "Productos de la empresa recuperados"));
    }
}