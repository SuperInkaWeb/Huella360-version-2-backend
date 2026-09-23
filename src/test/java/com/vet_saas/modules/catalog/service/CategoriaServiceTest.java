package com.vet_saas.modules.catalog.service;

import com.vet_saas.modules.catalog.dto.MarketplaceCategoriaResponse;
import com.vet_saas.modules.catalog.model.Categoria;
import com.vet_saas.modules.catalog.model.EstadoProducto;
import com.vet_saas.modules.catalog.repository.CategoriaProductCount;
import com.vet_saas.modules.catalog.repository.CategoriaRepository;
import com.vet_saas.modules.catalog.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock private CategoriaRepository categoriaRepository;
    @Mock private ProductoRepository productoRepository;

    private CategoriaService categoriaService;

    private Categoria alimentos;
    private Categoria alimentosPerros;
    private Categoria alimentosGatos;
    private Categoria medicamentos;
    private Categoria juguetes;
    private Categoria juguetesPerros;

    @BeforeEach
    void setUp() {
        categoriaService = new CategoriaService(categoriaRepository, productoRepository);

        alimentos = categoria(1L, "Alimentos", null);
        alimentosPerros = categoria(11L, "Perros", alimentos);
        alimentosGatos = categoria(12L, "Gatos", alimentos);
        medicamentos = categoria(2L, "Medicamentos", null);
        juguetes = categoria(3L, "Juguetes", null);
        juguetesPerros = categoria(31L, "Juguetes para Perros", juguetes);

        when(categoriaRepository.findByActivoTrueOrderByOrdenAsc())
                .thenReturn(List.of(alimentos, medicamentos, juguetes, alimentosPerros, alimentosGatos, juguetesPerros));
    }

    @Test
    void getMarketplaceCategories_sinProductos_devuelveArbolVacio() {
        when(productoRepository.countPublicProductsByCategoria(EstadoProducto.ACTIVO)).thenReturn(List.of());

        assertTrue(categoriaService.getMarketplaceCategories().isEmpty());
    }

    @Test
    void getMarketplaceCategories_omiteCategoriasYSubcategoriasSinProductos() {
        when(productoRepository.countPublicProductsByCategoria(EstadoProducto.ACTIVO))
                .thenReturn(List.of(conteo(11L, 5L)));

        List<MarketplaceCategoriaResponse> result = categoriaService.getMarketplaceCategories();

        assertEquals(1, result.size());
        MarketplaceCategoriaResponse padre = result.get(0);
        assertEquals("Alimentos", padre.nombre());
        assertEquals(1, padre.subcategorias().size());
        assertEquals("Perros", padre.subcategorias().get(0).nombre());
        assertEquals(5L, padre.subcategorias().get(0).productCount());
    }

    @Test
    void getMarketplaceCategories_padreSumaProductosPropiosYDeSusSubcategorias() {
        when(productoRepository.countPublicProductsByCategoria(EstadoProducto.ACTIVO))
                .thenReturn(List.of(conteo(1L, 2L), conteo(11L, 5L), conteo(12L, 3L)));

        MarketplaceCategoriaResponse padre = categoriaService.getMarketplaceCategories().get(0);

        assertEquals(10L, padre.productCount());
        assertEquals(2, padre.subcategorias().size());
    }

    @Test
    void getMarketplaceCategories_padreSoloConProductosDirectosSeMuestraSinSubcategorias() {
        when(productoRepository.countPublicProductsByCategoria(EstadoProducto.ACTIVO))
                .thenReturn(List.of(conteo(2L, 4L)));

        List<MarketplaceCategoriaResponse> result = categoriaService.getMarketplaceCategories();

        assertEquals(1, result.size());
        assertEquals("Medicamentos", result.get(0).nombre());
        assertEquals(4L, result.get(0).productCount());
        assertTrue(result.get(0).subcategorias().isEmpty());
    }

    @Test
    void getMarketplaceCategories_conservaElOrdenDeLasCategoriasPadre() {
        when(productoRepository.countPublicProductsByCategoria(EstadoProducto.ACTIVO))
                .thenReturn(List.of(conteo(31L, 1L), conteo(2L, 1L), conteo(11L, 1L)));

        List<String> nombres = categoriaService.getMarketplaceCategories().stream()
                .map(MarketplaceCategoriaResponse::nombre)
                .toList();

        assertEquals(List.of("Alimentos", "Medicamentos", "Juguetes"), nombres);
    }

    private Categoria categoria(Long id, String nombre, Categoria padre) {
        return Categoria.builder().id(id).nombre(nombre).slug(nombre.toLowerCase()).padre(padre).build();
    }

    private CategoriaProductCount conteo(Long categoriaId, Long total) {
        return new CategoriaProductCount() {
            @Override
            public Long getCategoriaId() {
                return categoriaId;
            }

            @Override
            public Long getTotal() {
                return total;
            }
        };
    }
}
