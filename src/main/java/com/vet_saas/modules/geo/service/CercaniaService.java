package com.vet_saas.modules.geo.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.geo.dto.LugarCercanoResponse;
import com.vet_saas.modules.veterinarian.model.VerificationStatus;
import com.vet_saas.modules.veterinarian.model.Veterinario;
import com.vet_saas.modules.veterinarian.repository.VeterinarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Búsqueda por cercanía: empresas y veterinarios independientes verificados, con ubicación,
 * ordenados por distancia a un punto y dentro de un radio.
 */
@Service
@RequiredArgsConstructor
public class CercaniaService {

    public static final String TIPO_EMPRESA = "EMPRESA";
    public static final String TIPO_VETERINARIO = "VETERINARIO";

    static final double RADIO_MAXIMO_KM = 50;
    static final int LIMITE_MAXIMO = 100;
    private static final double RADIO_TIERRA_KM = 6371;
    private static final double KM_POR_GRADO_LATITUD = 111.32;

    private final EmpresaRepository empresaRepository;
    private final VeterinarioRepository veterinarioRepository;

    /**
     * @param tipo         EMPRESA, VETERINARIO o null para ambos
     * @param tipoServicio filtra empresas por tipo de servicio (por ejemplo VETERINARIA para clínicas); null para todas
     */
    @Transactional(readOnly = true)
    public List<LugarCercanoResponse> buscar(double lat, double lng, double radioKm,
                                             String tipo, String tipoServicio, int limite) {
        validar(lat, lng, radioKm, limite);
        String tipoNormalizado = normalizarTipo(tipo);
        // tipoServicio solo aplica a empresas: con él se excluyen los veterinarios.
        boolean incluirEmpresas = !TIPO_VETERINARIO.equals(tipoNormalizado);
        boolean incluirVeterinarios = !TIPO_EMPRESA.equals(tipoNormalizado) && esVacio(tipoServicio);

        // Recuadro alrededor del punto para que la base devuelva solo candidatos cercanos.
        double deltaLat = radioKm / KM_POR_GRADO_LATITUD;
        double deltaLng = radioKm / (KM_POR_GRADO_LATITUD * Math.max(Math.cos(Math.toRadians(lat)), 0.01));
        BigDecimal latMin = BigDecimal.valueOf(lat - deltaLat);
        BigDecimal latMax = BigDecimal.valueOf(lat + deltaLat);
        BigDecimal lngMin = BigDecimal.valueOf(lng - deltaLng);
        BigDecimal lngMax = BigDecimal.valueOf(lng + deltaLng);

        List<LugarCercanoResponse> resultados = new ArrayList<>();
        if (incluirEmpresas) {
            for (Empresa e : empresaRepository.buscarEnRecuadro(VerificationStatus.VERIFICADO, latMin, latMax, lngMin, lngMax)) {
                if (!esVacio(tipoServicio) && !tipoServicio.trim().equalsIgnoreCase(e.getTipoServicio())) {
                    continue;
                }
                resultados.add(new LugarCercanoResponse(TIPO_EMPRESA, e.getId(), e.getNombreComercial(),
                        e.getTipoServicio(), e.getDireccion(), e.getCiudad(), e.getLogoUrl(),
                        e.getUbicacionLat(), e.getUbicacionLng(),
                        distanciaKm(lat, lng, e.getUbicacionLat(), e.getUbicacionLng())));
            }
        }
        if (incluirVeterinarios) {
            for (Veterinario v : veterinarioRepository.buscarEnRecuadro(VerificationStatus.VERIFICADO, latMin, latMax, lngMin, lngMax)) {
                resultados.add(new LugarCercanoResponse(TIPO_VETERINARIO, v.getId(),
                        (v.getNombres() + " " + v.getApellidos()).trim(),
                        v.getEspecialidad(), v.getDireccion(), v.getCiudad(), v.getFotoPerfilUrl(),
                        v.getUbicacionLat(), v.getUbicacionLng(),
                        distanciaKm(lat, lng, v.getUbicacionLat(), v.getUbicacionLng())));
            }
        }

        // El recuadro incluye sus esquinas: el radio real se aplica aquí.
        return resultados.stream()
                .filter(r -> r.distanciaKm() <= radioKm)
                .sorted(Comparator.comparingDouble(LugarCercanoResponse::distanciaKm))
                .limit(limite)
                .toList();
    }

    private void validar(double lat, double lng, double radioKm, int limite) {
        if (lat < -90 || lat > 90) {
            throw new BusinessException("La latitud debe estar entre -90 y 90");
        }
        if (lng < -180 || lng > 180) {
            throw new BusinessException("La longitud debe estar entre -180 y 180");
        }
        if (radioKm <= 0 || radioKm > RADIO_MAXIMO_KM) {
            throw new BusinessException("El radio debe ser mayor que 0 y de hasta " + (int) RADIO_MAXIMO_KM + " km");
        }
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new BusinessException("El límite debe estar entre 1 y " + LIMITE_MAXIMO);
        }
    }

    private String normalizarTipo(String tipo) {
        if (esVacio(tipo)) {
            return null;
        }
        String valor = tipo.trim().toUpperCase(Locale.ROOT);
        if (!TIPO_EMPRESA.equals(valor) && !TIPO_VETERINARIO.equals(valor)) {
            throw new BusinessException("El tipo debe ser EMPRESA o VETERINARIO");
        }
        return valor;
    }

    private static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    /** Distancia Haversine en km, redondeada a 2 decimales. */
    static double distanciaKm(double lat, double lng, BigDecimal lat2, BigDecimal lng2) {
        double dLat = Math.toRadians(lat2.doubleValue() - lat);
        double dLng = Math.toRadians(lng2.doubleValue() - lng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double km = RADIO_TIERRA_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
