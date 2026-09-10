package com.vet_saas.modules.catalog.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.core.service.StorageService;
import com.vet_saas.modules.catalog.dto.CreateProductDto;
import com.vet_saas.modules.catalog.dto.ProductResponse;
import com.vet_saas.modules.catalog.dto.UpdateProductDto;
import com.vet_saas.modules.catalog.model.Categoria;
import com.vet_saas.modules.catalog.model.EstadoProducto;
import com.vet_saas.modules.catalog.model.Producto;
import com.vet_saas.modules.catalog.repository.CategoriaRepository;
import com.vet_saas.modules.catalog.repository.ProductoRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.service.EmpresaLookupService;
import com.vet_saas.modules.subscription.service.SubscriptionService;
import com.vet_saas.modules.user.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductoRepository productoRepository;
    private final EmpresaLookupService empresaLookupService;
    private final CategoriaRepository categoriaRepository;
    private final StorageService storageService;
    private final SubscriptionService subscriptionService;

    private static final int MAX_IMAGES = 5;

    @Transactional
    public ProductResponse createProduct(Usuario usuario, CreateProductDto dto, List<MultipartFile> imageFiles) {
        Empresa empresa = empresaLookupService.getEmpresaFromUsuario(usuario);

        // Validar límite de suscripción
        long currentCount = productoRepository.countByEmpresaIdAndActivoTrue(empresa.getId());
        if (!subscriptionService.canAddProduct(empresa.getId(), currentCount)) {
            throw new BusinessException(
                    "Has alcanzado el límite de productos permitido por tu plan actual. Considera subir de nivel a un plan superior.");
        }

        String sku = (dto.sku() != null && !dto.sku().isBlank()) ? dto.sku() : generateSku(empresa.getId(), currentCount);

        if (productoRepository.existsByEmpresaIdAndSku(empresa.getId(), sku)) {
            throw new BusinessException("El SKU " + sku + " ya está registrado en tu empresa.");
        }

        Categoria categoria = categoriaRepository.findByIdAndActivoTrue(dto.categoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", "id", dto.categoriaId()));

        validatePricing(dto.precio(), dto.precioOferta());

        List<String> imageUrls = uploadImages(imageFiles);

        Producto producto = Producto.builder()
                .empresa(empresa)
                .categoria(categoria)
                .nombre(dto.nombre())
                .descripcion(dto.descripcion())
                .precio(dto.precio())
                .precioOferta(dto.precioOferta())
                .ofertaInicio(dto.ofertaInicio())
                .ofertaFin(dto.ofertaFin())
                .stock(dto.stock())
                .sku(sku)
                .visible(dto.visible() != null ? dto.visible() : true)
                .imagenes(imageUrls)
                .build();

        return mapToResponse(productoRepository.save(producto));
    }

    private String generateSku(Long empresaId, long currentCount) {
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "SKU-" + uuid;
    }

    @Transactional
    public ProductResponse updateProduct(Usuario usuario, Long id, UpdateProductDto dto, List<MultipartFile> newImageFiles, boolean replaceImages) {
        Empresa empresa = empresaLookupService.getEmpresaFromUsuario(usuario);
        Producto producto = getProductoPropio(id, empresa.getId());

        if (dto.sku() != null && !dto.sku().equals(producto.getSku())) {
            if (productoRepository.existsByEmpresaIdAndSkuAndIdNot(empresa.getId(), dto.sku(), id)) {
                throw new BusinessException("El SKU " + dto.sku() + " ya está siendo utilizado por otro producto.");
            }
            producto.setSku(dto.sku());
        }

        if (dto.categoriaId() != null) {
            Categoria categoria = categoriaRepository.findByIdAndActivoTrue(dto.categoriaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria", "id", dto.categoriaId()));
            producto.setCategoria(categoria);
        }

        if (dto.nombre() != null) producto.setNombre(dto.nombre());
        if (dto.descripcion() != null) producto.setDescripcion(dto.descripcion());
        if (dto.precio() != null) producto.setPrecio(dto.precio());
        
        // Si el flag actualizarOferta es true, aplicamos los valores del DTO (incluyendo null para limpiar)
        if (Boolean.TRUE.equals(dto.actualizarOferta())) {
            validatePricing(producto.getPrecio(), dto.precioOferta());
            producto.setPrecioOferta(dto.precioOferta());
            producto.setOfertaInicio(dto.ofertaInicio());
            producto.setOfertaFin(dto.ofertaFin());
        } else {
            // Si no se pide actualizar oferta explícitamente, pero se cambió el precio base, validar contra la oferta actual
            if (dto.precio() != null) {
                validatePricing(producto.getPrecio(), producto.getPrecioOferta());
            }
        }

        if (dto.stock() != null) producto.setStock(dto.stock());
        if (dto.visible() != null) producto.setVisible(dto.visible());

        if (dto.estado() != null) {
            try {
                producto.setEstado(EstadoProducto.valueOf(dto.estado().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Estado de producto inválido. Debe ser: ACTIVO, INACTIVO o AGOTADO.");
            }
        }

        List<String> urlsA_Mantener = dto.imagenes() != null ? new ArrayList<>(dto.imagenes()) : new ArrayList<>();

        List<String> urlsExistentes = producto.getImagenes() != null ? producto.getImagenes() : new ArrayList<>();

        urlsExistentes.stream()
                .filter(url -> !urlsA_Mantener.contains(url))
                .forEach(storageService::deleteFile);

        List<String> listaFinal;
        if (replaceImages) {
            urlsA_Mantener.forEach(storageService::deleteFile);
            listaFinal = new ArrayList<>();
        } else {
            listaFinal = urlsA_Mantener;
        }

        if (newImageFiles != null && !newImageFiles.isEmpty()) {
            int spaceLeft = MAX_IMAGES - listaFinal.size();

            if (newImageFiles.size() > spaceLeft) {
                throw new BusinessException("No puedes tener más de " + MAX_IMAGES + " imágenes. Espacio restante: " + spaceLeft);
            }
            List<String> uploadedUrls = uploadImages(newImageFiles);
            listaFinal.addAll(uploadedUrls);
        }
        producto.setImagenes(listaFinal);

        return mapToResponse(productoRepository.save(producto));
    }

    @Transactional
    public void softDeleteProduct(Usuario usuario, Long id) {
        Empresa empresa = empresaLookupService.getEmpresaFromUsuario(usuario);
        Producto producto = getProductoPropio(id, empresa.getId());

        producto.setActivo(false);
        producto.setVisible(false);
        productoRepository.save(producto);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getMyProducts(Usuario usuario, Pageable pageable) {
        Empresa empresa = empresaLookupService.getEmpresaFromUsuario(usuario);
        return productoRepository.findByEmpresaIdAndActivoTrue(empresa.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getPublicProducts(String q, Long categoriaId, Pageable pageable) {
        return productoRepository.findMarketplaceProducts(q, EstadoProducto.ACTIVO, categoriaId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getPublicProductsByCompany(Long companyId, Pageable pageable) {
        return productoRepository.findByEmpresaIdAndEstadoAndVisibleTrueAndActivoTrue(companyId, EstadoProducto.ACTIVO,
                pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return productoRepository.findByIdAndEstadoAndVisibleTrueAndActivoTrue(id, EstadoProducto.ACTIVO)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Producto Marketplace", "id", id));
    }

    private Producto getProductoPropio(Long productoId, Long empresaId) {
        Producto producto = productoRepository.findByIdAndActivoTrue(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", productoId));

        if (!producto.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException("Acceso denegado: No tienes permiso para editar este producto.");
        }
        return producto;
    }

    private void validatePricing(BigDecimal precio, BigDecimal precioOferta) {
        if (precioOferta != null && precio != null && precioOferta.compareTo(precio) >= 0) {
            throw new BusinessException("El precio de oferta debe ser menor al precio base.");
        }
    }

    private List<String> uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return new ArrayList<>();

        for (MultipartFile file : files) {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new BusinessException("El archivo " + file.getOriginalFilename() + " no es una imagen válida.");
            }
        }

        if (files.size() > MAX_IMAGES) {
            throw new BusinessException("Solo se permiten un máximo de " + MAX_IMAGES + " imágenes.");
        }
        return files.stream()
                .filter(file -> !file.isEmpty())
                .map(file -> storageService.uploadFile(file, "catalogo/productos"))
                .collect(Collectors.toList());
    }

    private ProductResponse mapToResponse(Producto producto) {
        return new ProductResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getPrecioOferta(),
                producto.getPrecioActual(),
                producto.getOfertaInicio(),
                producto.getOfertaFin(),
                producto.getStock(),
                producto.getSku(),
                producto.getEstado(),
                producto.getImagenes() != null ? producto.getImagenes() : new ArrayList<>(),
                producto.getCategoria() != null ? producto.getCategoria().getId() : null,
                producto.getCategoria() != null ? producto.getCategoria().getNombre() : null,
                producto.getEmpresa().getId(),
                producto.getEmpresa().getNombreComercial(),
                producto.getEmpresa().getTipoServicio(),
                producto.getEmpresa().getMpPublicKey(),
                producto.getActivo(),
                producto.getVisible(),
                producto.getCreatedAt(),
                producto.getUpdatedAt());
    }
}
