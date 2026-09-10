package com.vet_saas.modules.company.service;

import com.vet_saas.config.AppProperties;
import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.company.dto.CompanyResponse;
import com.vet_saas.modules.company.dto.CreateCompanyDto;
import com.vet_saas.modules.company.dto.UpdateCompanyDto;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.core.utils.CryptoUtil;
import com.vet_saas.modules.company.repository.EmpresaRepository;
import com.vet_saas.modules.subscription.service.SubscriptionService;
import com.vet_saas.modules.user.model.Usuario;
import com.vet_saas.modules.veterinarian.model.VerificationStatus;
import com.vet_saas.modules.pet.dto.PetResponse;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final EmpresaRepository empresaRepository;
    private final CryptoUtil cryptoUtil;
    private final AppProperties appProperties;
    private final SubscriptionService subscriptionService;
    private final MascotaRepository mascotaRepository;

    @Transactional
    public CompanyResponse createProfile(Usuario usuario, CreateCompanyDto dto, String logoUrl, String bannerUrl) {

        if (empresaRepository.existsByUsuarioPropietarioId(usuario.getId())) {
            throw new BusinessException("Este usuario ya tiene una empresa registrada");
        }

        if (dto.ruc() != null && empresaRepository.existsByRuc(dto.ruc())) {
            throw new BusinessException("El RUC ya está registrado");
        }

        Empresa empresa = Empresa.builder()
                .usuarioPropietario(usuario)
                .nombreComercial(dto.nombreComercial())
                .razonSocial(dto.razonSocial())
                .ruc(dto.ruc())
                .descripcion(dto.descripcion())
                .tipoServicio(dto.tipoServicio())
                .telefonoContacto(dto.telefono())
                .emailContacto(dto.emailContacto())
                .direccion(dto.direccion())
                .ciudad(dto.ciudad())
                .pais("Perú")
                .ubicacionLat(dto.latitud())
                .ubicacionLng(dto.longitud())
                .logoUrl(logoUrl)
                .bannerUrl(bannerUrl)
                .estadoValidacion(VerificationStatus.PENDIENTE)
                .build();

        Empresa saved = empresaRepository.save(empresa);

        // Asignar plan gratuito por defecto
        subscriptionService.assignDefaultPlan(saved);

        return mapToResponse(saved);
    }

    @Transactional
    public CompanyResponse updateProfile(Usuario usuario, UpdateCompanyDto dto, String logoUrl, String bannerUrl) {
        Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("No se encontró el perfil de empresa para este usuario"));

        if (dto.nombreComercial() != null)
            empresa.setNombreComercial(dto.nombreComercial());
        if (dto.descripcion() != null)
            empresa.setDescripcion(dto.descripcion());
        if (dto.tipoServicio() != null) {
            empresa.setTipoServicio(dto.tipoServicio());
        }
        if (dto.telefono() != null)
            empresa.setTelefonoContacto(dto.telefono());
        if (dto.emailContacto() != null)
            empresa.setEmailContacto(dto.emailContacto());
        if (dto.direccion() != null)
            empresa.setDireccion(dto.direccion());
        if (dto.ciudad() != null)
            empresa.setCiudad(dto.ciudad());

        if (dto.latitud() != null)
            empresa.setUbicacionLat(dto.latitud());
        if (dto.longitud() != null)
            empresa.setUbicacionLng(dto.longitud());

        if (logoUrl != null && !logoUrl.isEmpty()) {
            empresa.setLogoUrl(logoUrl);
        }
        if (bannerUrl != null && !bannerUrl.isEmpty()) {
            empresa.setBannerUrl(bannerUrl);
        }

        Empresa updated = empresaRepository.save(empresa);
        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getProfile(Usuario usuario) {
        Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "usuario", usuario.getId()));
        return mapToResponse(empresa);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getPublicProfile(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", id));

        return mapToResponse(empresa);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CompanyResponse> getAllPublicCompanies(
            org.springframework.data.domain.Pageable pageable) {
        return empresaRepository.findByEstadoValidacion(VerificationStatus.VERIFICADO, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public void updateMercadoPagoCredentials(Usuario usuario, String mpAccessToken, String mpPublicKey) {
        Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("No se encontró el perfil de empresa para este usuario"));

        String encryptedToken = cryptoUtil.encrypt(mpAccessToken);

        empresa.setMpAccessToken(encryptedToken);
        empresa.setMpPublicKey(mpPublicKey);

        empresaRepository.save(empresa);
    }

    @Transactional
    public void connectMercadoPago(Usuario usuario, String code, String redirectUri) {
        Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("No se encontró el perfil de empresa para este usuario"));

        String clientId = appProperties.getExternal().getMercadoPago().getClientId();
        String clientSecret = appProperties.getExternal().getMercadoPago().getClientSecret();

        if (clientId == null || clientSecret == null) {
            throw new BusinessException(
                    "El servidor no tiene configuradas las credenciales del sistema (Client ID/Secret).");
        }

        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

            org.springframework.util.MultiValueMap<String, String> map = new org.springframework.util.LinkedMultiValueMap<>();
            map.add("client_id", clientId);
            map.add("client_secret", clientSecret);
            map.add("grant_type", "authorization_code");
            map.add("code", code);
            map.add("redirect_uri", redirectUri);

            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> request = new org.springframework.http.HttpEntity<>(
                    map, headers);

            com.vet_saas.modules.company.dto.MercadoPagoOAuthResponse response = restTemplate.postForObject(
                    "https://api.mercadopago.com/oauth/token",
                    request,
                    com.vet_saas.modules.company.dto.MercadoPagoOAuthResponse.class);

            if (response != null && response.accessToken() != null) {
                String encryptedToken = cryptoUtil.encrypt(response.accessToken());
                empresa.setMpAccessToken(encryptedToken);
                empresa.setMpPublicKey(response.publicKey());
                empresaRepository.save(empresa);
            } else {
                throw new BusinessException("No se recibió una respuesta válida de Mercado Pago.");
            }
        } catch (Exception e) {
            throw new BusinessException("Error al conectar con Mercado Pago: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<PetResponse> getPatientsByCompany(Usuario usuario) {
        Empresa empresa = empresaRepository.findByUsuarioPropietarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("No se encontró el perfil de empresa para este usuario"));

        return mascotaRepository.findPacientesByEmpresa(empresa.getId()).stream()
                .map(this::mapToPetResponse)
                .toList();
    }

    private PetResponse mapToPetResponse(com.vet_saas.modules.pet.model.Mascota mascota) {
        return new PetResponse(
                mascota.getId(),
                mascota.getNombre(),
                mascota.getEspecie(),
                mascota.getRaza(),
                mascota.getSexo() != null ? mascota.getSexo() : null,
                mascota.getFechaNacimiento(),
                mascota.getPesoKg(),
                mascota.getFotoUrl(),
                mascota.getEsterilizado(),
                mascota.getObservacionesMedicas(),
                mascota.getCreatedAt());
    }

    private CompanyResponse mapToResponse(Empresa empresa) {
        return new CompanyResponse(
                empresa.getId(),
                empresa.getUsuarioPropietario().getId(),
                empresa.getNombreComercial(),
                empresa.getRazonSocial(),
                empresa.getRuc(),
                empresa.getDescripcion(),
                empresa.getTipoServicio(),
                empresa.getTelefonoContacto(),
                empresa.getEmailContacto(),
                empresa.getDireccion(),
                empresa.getCiudad(),
                empresa.getPais(),
                empresa.getUbicacionLat(),
                empresa.getUbicacionLng(),
                empresa.getLogoUrl(),
                empresa.getBannerUrl(),
                empresa.getMpPublicKey(),
                empresa.getEstadoValidacion().name());
    }
}