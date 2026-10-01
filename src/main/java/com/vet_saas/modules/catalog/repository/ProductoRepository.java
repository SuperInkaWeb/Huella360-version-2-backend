package com.vet_saas.modules.catalog.repository;

import com.vet_saas.modules.catalog.model.EstadoProducto;
import com.vet_saas.modules.catalog.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.vet_saas.modules.veterinarian.model.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

        // Helper para verificar existencia de SKU por empresa
        boolean existsByEmpresaIdAndSku(Long empresaId, String sku);

        boolean existsByEmpresaIdAndSkuAndIdNot(Long empresaId, String sku, Long id);

        // Queries de empresa (dashboard)
        Page<Producto> findByEmpresaIdAndActivoTrue(Long empresaId, Pageable pageable);

        Optional<Producto> findByIdAndActivoTrue(Long id);

        Optional<Producto> findByIdAndEmpresaIdAndActivoTrue(Long id, Long empresaId);

        long countByEmpresaIdAndActivoTrue(Long empresaId);

        @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(sku FROM LENGTH(CONCAT('SKU-', :empresaId, '-')) + 1 AS UNSIGNED)), 0) FROM productos WHERE empresa_id = :empresaId AND sku LIKE CONCAT('SKU-', :empresaId, '-%')", nativeQuery = true)
        long findMaxSkuSequence(@Param("empresaId") Long empresaId);

        // Regla de visibilidad (A3): solo se publica lo de empresas VERIFICADAS.
        // Los metodos default mantienen la firma que usan los servicios y fijan el estado.

        // Endpoint individual público
        Optional<Producto> findByIdAndEstadoAndVisibleTrueAndActivoTrueAndEmpresaEstadoValidacion(
                        Long id, EstadoProducto estado, VerificationStatus estadoEmpresa);

        default Optional<Producto> findByIdAndEstadoAndVisibleTrueAndActivoTrue(Long id, EstadoProducto estado) {
                return findByIdAndEstadoAndVisibleTrueAndActivoTrueAndEmpresaEstadoValidacion(
                                id, estado, VerificationStatus.VERIFICADO);
        }

        // Queries para Marketplace (públicos)
        // Una categoría padre incluye los productos de sus subcategorías (c.padre.id).
        @Query("SELECT p FROM Producto p " +
                        "JOIN FETCH p.empresa " +
                        "LEFT JOIN FETCH p.categoria c " +
                        "WHERE p.estado = :estado AND p.visible = true AND p.activo = true " +
                        "AND p.empresa.estadoValidacion = :estadoEmpresa " +
                        "AND (CAST(:categoriaId AS long) IS NULL OR c.id = :categoriaId OR c.padre.id = :categoriaId) " +
                        "AND (CAST(:q AS string) IS NULL OR :q = '' OR " +
                        "     LOWER(p.nombre) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR " +
                        "     LOWER(p.descripcion) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))")
        Page<Producto> findMarketplaceProductsPorEstadoEmpresa(
                        @Param("q") String q,
                        @Param("estado") EstadoProducto estado,
                        @Param("categoriaId") Long categoriaId,
                        @Param("estadoEmpresa") VerificationStatus estadoEmpresa,
                        Pageable pageable);

        default Page<Producto> findMarketplaceProducts(String q, EstadoProducto estado, Long categoriaId,
                        Pageable pageable) {
                return findMarketplaceProductsPorEstadoEmpresa(q, estado, categoriaId, VerificationStatus.VERIFICADO,
                                pageable);
        }

        // Conteo de productos públicos por categoría (mismos criterios que findMarketplaceProducts)
        @Query("SELECT p.categoria.id AS categoriaId, COUNT(p) AS total FROM Producto p " +
                        "JOIN p.empresa e " +
                        "WHERE p.estado = :estado AND p.visible = true AND p.activo = true " +
                        "AND e.estadoValidacion = :estadoEmpresa " +
                        "AND p.categoria IS NOT NULL " +
                        "GROUP BY p.categoria.id")
        List<CategoriaProductCount> countPublicProductsByCategoriaPorEstadoEmpresa(
                        @Param("estado") EstadoProducto estado,
                        @Param("estadoEmpresa") VerificationStatus estadoEmpresa);

        default List<CategoriaProductCount> countPublicProductsByCategoria(EstadoProducto estado) {
                return countPublicProductsByCategoriaPorEstadoEmpresa(estado, VerificationStatus.VERIFICADO);
        }

        // Nuevo método dedicado para perfiles públicos de empresa
        Page<Producto> findByEmpresaIdAndEstadoAndVisibleTrueAndActivoTrueAndEmpresaEstadoValidacion(
                        Long empresaId,
                        EstadoProducto estado,
                        VerificationStatus estadoEmpresa,
                        Pageable pageable);

        default Page<Producto> findByEmpresaIdAndEstadoAndVisibleTrueAndActivoTrue(Long empresaId,
                        EstadoProducto estado, Pageable pageable) {
                return findByEmpresaIdAndEstadoAndVisibleTrueAndActivoTrueAndEmpresaEstadoValidacion(
                                empresaId, estado, VerificationStatus.VERIFICADO, pageable);
        }

        @Query("SELECT p FROM Producto p WHERE p.id = :id")
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        Optional<Producto> findByIdForUpdate(@Param("id") Long id);

}