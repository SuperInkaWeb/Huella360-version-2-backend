package com.vet_saas.modules.catalog.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.service.StorageService;
import com.vet_saas.modules.catalog.dto.CreateProductDto;
import com.vet_saas.modules.catalog.repository.CategoriaRepository;
import com.vet_saas.modules.catalog.repository.ProductoRepository;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.service.EmpresaLookupService;
import com.vet_saas.modules.subscription.service.PlanEnforcementService;
import com.vet_saas.modules.user.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * H360-EMP-001: crear un producto valida el límite del plan con PlanEnforcementService
 * (antes usaba un chequeo propio con mensaje genérico y 404 si la empresa no tenía suscripción).
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductoRepository productoRepository;
    @Mock private EmpresaLookupService empresaLookupService;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private StorageService storageService;
    @Mock private PlanEnforcementService planEnforcementService;
    @InjectMocks private ProductService productService;

    @Test
    void createProduct_alcanzadoElLimiteDelPlan_noGuardaNada() {
        Usuario usuario = new Usuario();
        when(empresaLookupService.getEmpresaFromUsuario(usuario)).thenReturn(Empresa.builder().id(7L).build());
        when(productoRepository.countByEmpresaIdAndActivoTrue(7L)).thenReturn(4L);
        doThrow(new BusinessException("Has alcanzado el límite de 4 producto(s) de tu plan Huella Free B2B. Actualiza tu plan para agregar más."))
                .when(planEnforcementService).enforceProductLimit(7L, 4L);

        CreateProductDto dto = new CreateProductDto("Pelota", null, new BigDecimal("10"), null, null, null, 5, null, 26L, true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> productService.createProduct(usuario, dto, List.of()));
        assertTrue(ex.getMessage().contains("4 producto(s) de tu plan Huella Free B2B"));
        verify(productoRepository, never()).save(any());
        verifyNoInteractions(categoriaRepository, storageService);
    }
}
