package com.vet_saas.modules.catalog.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.catalog.dto.CreateCategoriaRequest;
import com.vet_saas.modules.catalog.dto.UpdateCategoriaRequest;
import com.vet_saas.modules.catalog.dto.CategoriaResponse;
import com.vet_saas.modules.catalog.model.Categoria;
import com.vet_saas.modules.catalog.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Cacheable(value = "categorias", key = "'allActive'")
    @Transactional(readOnly = true)
    public List<CategoriaResponse> getAllActiveCategories() {
        return categoriaRepository.findByActivoTrueOrderByOrdenAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "categorias", key = "'parents'")
    @Transactional(readOnly = true)
    public List<CategoriaResponse> getParentCategories() {
        return categoriaRepository.findByPadreIsNullAndActivoTrueOrderByOrdenAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "categorias", key = "'sub_' + #padreId")
    @Transactional(readOnly = true)
    public List<CategoriaResponse> getActiveSubcategories(Long padreId) {
        return categoriaRepository.findByPadreIdAndActivoTrueOrderByOrdenAsc(padreId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "categorias", allEntries = true)
    @Transactional
    public CategoriaResponse createCategory(CreateCategoriaRequest request) {
        String slug = generateSlug(request.nombre());
        if (categoriaRepository.existsBySlug(slug)) {
            throw new BusinessException("Ya existe una categoría con un nombre similar, intente con otro nombre.");
        }

        Categoria padre = null;
        if (request.padreId() != null) {
            padre = categoriaRepository.findByIdAndActivoTrue(request.padreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria", "id", request.padreId()));
        }

        Categoria categoria = Categoria.builder()
                .nombre(request.nombre())
                .slug(slug)
                .padre(padre)
                .iconoUrl(request.iconoUrl())
                .activo(request.activo())
                .orden(request.orden())
                .build();

        return mapToResponse(categoriaRepository.save(categoria));
    }

    @CacheEvict(value = "categorias", allEntries = true)
    @Transactional
    public CategoriaResponse updateCategory(Long id, UpdateCategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", "id", id));

        if (request.nombre() != null) {
            String newSlug = generateSlug(request.nombre());
            if (!categoria.getSlug().equals(newSlug)) {
                if (categoriaRepository.existsBySlugAndIdNot(newSlug, id)) {
                    throw new BusinessException("El nombre genera un identificador (slug) que ya está en uso.");
                }
                categoria.setSlug(newSlug);
            }
            categoria.setNombre(request.nombre());
        }

        if (request.padreId() != null) {
            if (request.padreId() == -1) { // Convención para desvincular el padre
                categoria.setPadre(null);
            } else {
                Categoria padre = categoriaRepository.findByIdAndActivoTrue(request.padreId())
                        .orElseThrow(() -> new ResourceNotFoundException("Categoria Padre", "id", request.padreId()));
                validateNoCycle(id, padre);
                categoria.setPadre(padre);
            }
        }

        if (request.iconoUrl() != null) {
            categoria.setIconoUrl(request.iconoUrl());
        }

        if (request.activo() != null) {
            categoria.setActivo(request.activo());
        }

        if (request.orden() != null) {
            categoria.setOrden(request.orden());
        }

        return mapToResponse(categoriaRepository.save(categoria));
    }

    @CacheEvict(value = "categorias", allEntries = true)
    @Transactional
    public void softDeleteCategory(Long id) {
        Categoria categoria = categoriaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", "id", id));

        List<Categoria> subcategorias = categoriaRepository.findByPadreIdAndActivoTrueOrderByOrdenAsc(id);
        if (!subcategorias.isEmpty()) {
            throw new BusinessException("No se puede desactivar una categoría que tiene subcategorías activas.");
        }

        categoria.setActivo(false);
        categoriaRepository.save(categoria);
    }

    private String generateSlug(String input) {
        if (input == null)
            return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }

    private void validateNoCycle(Long categoryId, Categoria nuevoPadre) {
        if (categoryId == null || nuevoPadre == null) {
            return;
        }

        if (categoryId.equals(nuevoPadre.getId())) {
            throw new BusinessException("Una categoría no puede ser su propio padre directamente.");
        }

        Categoria ancestroActual = nuevoPadre.getPadre();
        int maxDepth = 100;
        int currentDepth = 0;

        while (ancestroActual != null && currentDepth < maxDepth) {
            if (categoryId.equals(ancestroActual.getId())) {
                throw new BusinessException(
                        "Ciclo detectado en la jerarquía. La categoría padre seleccionada ya es descendiente (o subcategoría) de la categoría actual.");
            }
            ancestroActual = ancestroActual.getPadre();
            currentDepth++;
        }

        if (currentDepth >= maxDepth) {
            throw new BusinessException(
                    "Se ha detectado una profundidad máxima superada o una posible corrupción previa en la jerarquía de categorías.");
        }
    }

    private CategoriaResponse mapToResponse(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getSlug(),
                categoria.getPadre() != null ? categoria.getPadre().getId() : null,
                categoria.getPadre() != null ? categoria.getPadre().getNombre() : null,
                categoria.getIconoUrl(),
                categoria.getActivo(),
                categoria.getOrden());
    }
}
