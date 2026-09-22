package com.vet_saas.modules.catalog.repository;

import com.vet_saas.AbstractIntegrationTest;
import com.vet_saas.modules.catalog.model.Categoria;
import com.vet_saas.modules.catalog.model.EstadoProducto;
import com.vet_saas.modules.catalog.model.Producto;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.user.model.Role;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.user.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regresion H360-MKT-001: cubre con BD real (Testcontainers, mismo esquema que produccion)
 * las consultas JPQL de findMarketplaceProducts() y countPublicProductsByCategoria(), que en
 * CategoriaServiceTest solo se ejercitan con ProductoRepository mockeado.
 */
class ProductoRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Empresa empresa;
    private Categoria padre;
    private Categoria hija;

    @BeforeEach
    void setUp() {
        limpiarDatos();

        Usuario propietario = usuarioRepository.save(Usuario.builder()
                .correo("empresa-test@test.com")
                .password("encoded-password")
                .rol(Role.EMPRESA)
                .estado(true)
                .emailVerificado(true)
                .build());

        empresa = empresaRepository.save(Empresa.builder()
                .usuarioPropietario(propietario)
                .nombreComercial("Empresa de Prueba")
                .build());

        padre = categoriaRepository.save(Categoria.builder()
                .nombre("Alimentos")
                .slug("alimentos")
                .activo(true)
                .orden(0)
                .build());

        hija = categoriaRepository.save(Categoria.builder()
                .nombre("Perros")
                .slug("alimentos-perros")
                .padre(padre)
                .activo(true)
                .orden(0)
                .build());
    }

    private void limpiarDatos() {
        productoRepository.deleteAll();
        categoriaRepository.findAll().stream()
                .filter(c -> c.getPadre() != null)
                .forEach(categoriaRepository::delete);
        categoriaRepository.findAll().stream()
                .filter(c -> c.getPadre() == null)
                .forEach(categoriaRepository::delete);
        empresaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private Producto crearProducto(String nombre, Categoria categoria, boolean visible, boolean activo, EstadoProducto estado) {
        return productoRepository.save(Producto.builder()
                .empresa(empresa)
                .categoria(categoria)
                .nombre(nombre)
                .precio(BigDecimal.TEN)
                .stock(10)
                .estado(estado)
                .activo(activo)
                .visible(visible)
                .build());
    }

    @Test
    void findMarketplaceProducts_incluyeProductosDirectosDelPadre() {
        Producto directoDelPadre = crearProducto("Alimento premium", padre, true, true, EstadoProducto.ACTIVO);

        Page<Producto> resultado = productoRepository.findMarketplaceProducts(
                null, EstadoProducto.ACTIVO, padre.getId(), PageRequest.of(0, 20));

        assertEquals(1, resultado.getTotalElements());
        assertEquals(directoDelPadre.getId(), resultado.getContent().get(0).getId());
    }

    @Test
    void findMarketplaceProducts_incluyeProductosDeSubcategoria_alFiltrarPorElPadre() {
        Producto deLaSubcategoria = crearProducto("Croquetas para perro", hija, true, true, EstadoProducto.ACTIVO);

        Page<Producto> resultado = productoRepository.findMarketplaceProducts(
                null, EstadoProducto.ACTIVO, padre.getId(), PageRequest.of(0, 20));

        assertEquals(1, resultado.getTotalElements());
        assertEquals(deLaSubcategoria.getId(), resultado.getContent().get(0).getId());
    }

    @Test
    void findMarketplaceProducts_filtraDirectamentePorSubcategoria() {
        Producto deLaSubcategoria = crearProducto("Croquetas para perro", hija, true, true, EstadoProducto.ACTIVO);
        crearProducto("Alimento generico del padre", padre, true, true, EstadoProducto.ACTIVO);

        Page<Producto> resultado = productoRepository.findMarketplaceProducts(
                null, EstadoProducto.ACTIVO, hija.getId(), PageRequest.of(0, 20));

        assertEquals(1, resultado.getTotalElements());
        assertEquals(deLaSubcategoria.getId(), resultado.getContent().get(0).getId());
    }

    @Test
    void findMarketplaceProducts_excluyeNoVisiblesInactivosOConEstadoDistintoDeActivo() {
        Producto valido = crearProducto("Producto valido", hija, true, true, EstadoProducto.ACTIVO);
        crearProducto("Producto no visible", hija, false, true, EstadoProducto.ACTIVO);
        crearProducto("Producto inactivo", hija, true, false, EstadoProducto.ACTIVO);
        crearProducto("Producto agotado", hija, true, true, EstadoProducto.AGOTADO);
        crearProducto("Producto marcado inactivo (estado)", hija, true, true, EstadoProducto.INACTIVO);

        Page<Producto> resultado = productoRepository.findMarketplaceProducts(
                null, EstadoProducto.ACTIVO, padre.getId(), PageRequest.of(0, 20));

        assertEquals(1, resultado.getTotalElements());
        assertEquals(valido.getId(), resultado.getContent().get(0).getId());
    }

    @Test
    void countPublicProductsByCategoria_soloCuentaProductosActivosVisiblesYPublicos() {
        crearProducto("Alimento generico del padre", padre, true, true, EstadoProducto.ACTIVO);
        crearProducto("Croquetas para perro", hija, true, true, EstadoProducto.ACTIVO);
        crearProducto("Otra croqueta valida", hija, true, true, EstadoProducto.ACTIVO);
        crearProducto("Producto no visible", hija, false, true, EstadoProducto.ACTIVO);
        crearProducto("Producto inactivo", hija, true, false, EstadoProducto.ACTIVO);
        crearProducto("Producto agotado", hija, true, true, EstadoProducto.AGOTADO);

        List<CategoriaProductCount> conteos = productoRepository.countPublicProductsByCategoria(EstadoProducto.ACTIVO);
        Map<Long, Long> porCategoria = conteos.stream()
                .collect(Collectors.toMap(CategoriaProductCount::getCategoriaId, CategoriaProductCount::getTotal));

        assertEquals(1L, porCategoria.get(padre.getId()));
        assertEquals(2L, porCategoria.get(hija.getId()));
    }

    @Test
    void countPublicProductsByCategoria_noIncluyeCategoriasSinProductosPublicos() {
        crearProducto("Producto no visible", hija, false, true, EstadoProducto.ACTIVO);

        List<CategoriaProductCount> conteos = productoRepository.countPublicProductsByCategoria(EstadoProducto.ACTIVO);
        boolean apareceHija = conteos.stream().anyMatch(c -> c.getCategoriaId().equals(hija.getId()));
        boolean apareceElPadre = conteos.stream().anyMatch(c -> c.getCategoriaId().equals(padre.getId()));

        assertFalse(apareceHija, "La categoria no deberia aparecer si su unico producto no es publico");
        assertFalse(apareceElPadre, "El padre no deberia aparecer si no tiene productos propios publicos");
        assertTrue(conteos.isEmpty());
    }
}
