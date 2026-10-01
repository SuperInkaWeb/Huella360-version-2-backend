package com.vet_saas.modules.points.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.points.model.ConfiguracionPuntos;
import com.vet_saas.modules.points.repository.ConfiguracionPuntosRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointsConfigServiceTest {

    @Mock
    private ConfiguracionPuntosRepository configRepository;

    @InjectMocks
    private PointsConfigService service;

    private ConfiguracionPuntos registro() {
        ConfiguracionPuntos c = new ConfiguracionPuntos();
        c.setId(1L);
        c.setAccion("REGISTRO");
        c.setPuntosOtorgados(50);
        c.setActivo(true);
        return c;
    }

    @Test
    void updateConfig_guardaValorValido() {
        ConfiguracionPuntos c = registro();
        when(configRepository.findById(1L)).thenReturn(Optional.of(c));
        when(configRepository.save(c)).thenReturn(c);

        var dto = service.updateConfig(1L, 55, false);

        assertEquals(55, dto.getPuntosOtorgados());
        assertFalse(dto.getActivo());
    }

    @Test
    void updateConfig_aceptaCero() {
        ConfiguracionPuntos c = registro();
        when(configRepository.findById(1L)).thenReturn(Optional.of(c));
        when(configRepository.save(c)).thenReturn(c);

        assertEquals(0, service.updateConfig(1L, 0, null).getPuntosOtorgados());
    }

    @Test
    void updateConfig_rechazaNegativos() {
        assertThrows(BusinessException.class, () -> service.updateConfig(1L, -1000, null));
        verify(configRepository, never()).save(any());
    }

    @Test
    void updateConfig_rechazaValoresSobreElTope() {
        assertThrows(BusinessException.class,
                () -> service.updateConfig(1L, PointsConfigService.MAX_PUNTOS_POR_ACCION + 1, null));
        verify(configRepository, never()).save(any());
    }

    @Test
    void updateConfig_rechazaNulo() {
        assertThrows(BusinessException.class, () -> service.updateConfig(1L, null, true));
    }

    @Test
    void getAllConfigs_devuelveOrdenFijoPorId() {
        when(configRepository.findAll(Sort.by("id"))).thenReturn(List.of(registro()));

        assertEquals(1, service.getAllConfigs().size());
        verify(configRepository).findAll(Sort.by("id"));
    }
}
