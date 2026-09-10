package com.vet_saas.modules.points.service;

import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.points.dto.PointsConfigDto;
import com.vet_saas.modules.points.model.ConfiguracionPuntos;
import com.vet_saas.modules.points.repository.ConfiguracionPuntosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointsConfigService {

    private final ConfiguracionPuntosRepository configRepository;

    @Transactional(readOnly = true)
    public List<PointsConfigDto> getAllConfigs() {
        return configRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PointsConfigDto updateConfig(Long id, Integer puntosOtorgados, Boolean activo) {
        ConfiguracionPuntos config = configRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ConfiguracionPuntos", "id", id));

        config.setPuntosOtorgados(puntosOtorgados);
        if (activo != null) {
            config.setActivo(activo);
        }

        config = configRepository.save(config);
        return mapToDto(config);
    }

    /**
     * Helper to get points for an action, returns 0 if disabled or not found
     */
    @Transactional(readOnly = true)
    public int getPointsForAction(String accion) {
        return configRepository.findByAccion(accion)
                .filter(ConfiguracionPuntos::getActivo)
                .map(ConfiguracionPuntos::getPuntosOtorgados)
                .orElse(0); 
    }

    private PointsConfigDto mapToDto(ConfiguracionPuntos entity) {
        return PointsConfigDto.builder()
                .id(entity.getId())
                .accion(entity.getAccion())
                .puntosOtorgados(entity.getPuntosOtorgados())
                .activo(entity.getActivo())
                .descripcion(entity.getDescripcion())
                .build();
    }
}
