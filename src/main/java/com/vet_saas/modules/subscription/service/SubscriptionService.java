package com.vet_saas.modules.subscription.service;

import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.company.model.Empresa;
import com.vet_saas.modules.company.service.EmpresaLookupService;
import com.vet_saas.modules.subscription.model.EstadoSuscripcion;
import com.vet_saas.modules.subscription.model.Plan;
import com.vet_saas.modules.subscription.model.Suscripcion;
import com.vet_saas.modules.subscription.repository.PlanRepository;
import com.vet_saas.modules.subscription.repository.SuscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vet_saas.modules.subscription.dto.PlanResponseDto;
import com.vet_saas.modules.subscription.dto.SuscripcionResponseDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.vet_saas.modules.subscription.dto.SubscriptionUsageDto;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import com.vet_saas.modules.catalog.repository.ProductoRepository;
import com.vet_saas.modules.ia.repository.IaUsageRepository;
import com.vet_saas.modules.payment.dto.PaymentPreferenceResponse;
import com.vet_saas.modules.payment.gateway.MercadoPagoGateway;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
        private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SubscriptionService.class);

        private final SuscripcionRepository suscripcionRepository;
        private final PlanRepository planRepository;
        private final ProductoRepository productoRepository;
        private final EmpresaLookupService empresaLookupService;
        private final com.vet_saas.modules.veterinarian.repository.VeterinarioRepository veterinarioRepository;
        private final MascotaRepository mascotaRepository;
        private final com.vet_saas.modules.catalog.repository.ServicioRepository servicioRepository;
        private final MercadoPagoGateway mercadoPagoGateway;
        private final com.vet_saas.config.AppProperties appProperties;
        private final IaUsageRepository iaUsageRepository;

        private Empresa getEmpresaFromUsuario(com.vet_saas.modules.user.model.Usuario usuario) {
                return empresaLookupService.getEmpresaFromUsuario(usuario);
        }

        private com.vet_saas.modules.veterinarian.model.Veterinario getVeterinarioFromUsuario(
                        com.vet_saas.modules.user.model.Usuario usuario) {
                return veterinarioRepository.findByUsuarioId(usuario.getId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "No se encontró un perfil de veterinario asociado a este usuario."));
        }

        @Transactional(readOnly = true)
        public List<PlanResponseDto> getAvailablePlans() {
                return planRepository.findAllByActivoTrue().stream()
                                .map(PlanResponseDto::fromEntity)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<PlanResponseDto> getAvailablePlansByType(String tipo) {
                return planRepository.findAllByActivoTrue().stream()
                                .filter(p -> tipo.equals(p.getTipo()))
                                .map(PlanResponseDto::fromEntity)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public Suscripcion getSuscripcionByUsuarioId(Long usuarioId) {
                // Try empresa first
                var empresaOpt = empresaLookupService.getEmpresaByUsuarioId(usuarioId);
                if (empresaOpt.isPresent()) {
                        return suscripcionRepository.findByEmpresaId(empresaOpt.get().getId()).orElse(null);
                }
                // Try veterinario
                var vetOpt = veterinarioRepository.findByUsuarioId(usuarioId);
                if (vetOpt.isPresent()) {
                        return suscripcionRepository.findByVeterinarioId(vetOpt.get().getId()).orElse(null);
                }
                // Try client (B2C)
                return suscripcionRepository.findByUsuarioId(usuarioId).orElse(null);
        }

        @Transactional
        public Suscripcion assignDefaultPlanForClient(com.vet_saas.modules.user.model.Usuario usuario) {
                if (suscripcionRepository.findByUsuarioId(usuario.getId()).isPresent()) {
                        return suscripcionRepository.findByUsuarioId(usuario.getId()).get();
                }

                Plan freePlan = planRepository.findByNombreIgnoreCase("Huella Básica")
                                .or(() -> planRepository.findAllByActivoTrue().stream().findFirst())
                                .orElseThrow(() -> new com.vet_saas.core.exceptions.types.ResourceNotFoundException(
                                                "No se encontraron planes activos en el sistema."));

                Suscripcion suscripcion = Suscripcion.builder()
                                .usuario(usuario)
                                .plan(freePlan)
                                .fechaInicio(LocalDateTime.now())
                                .estado(EstadoSuscripcion.ACTIVA)
                                .build();

                return suscripcionRepository.save(suscripcion);
        }

        @Transactional
        public SuscripcionResponseDto getSuscripcionActual(com.vet_saas.modules.user.model.Usuario usuario) {
                if ("VETERINARIO".equals(usuario.getRol().name())) {
                        com.vet_saas.modules.veterinarian.model.Veterinario vet = getVeterinarioFromUsuario(usuario);
                        return SuscripcionResponseDto.fromEntity(getSuscripcionEntityByVeterinario(vet.getId()));
                } else if ("CLIENTE".equals(usuario.getRol().name())) {
                        return SuscripcionResponseDto.fromEntity(
                                        suscripcionRepository.findByUsuarioId(usuario.getId())
                                                        .orElseGet(() -> assignDefaultPlanForClient(usuario)));
                } else {
                        Empresa empresa = getEmpresaFromUsuario(usuario);
                        return SuscripcionResponseDto.fromEntity(getSuscripcionEntityByEmpresa(empresa.getId()));
                }
        }

        @Transactional
        public Suscripcion getSuscripcionEntityByEmpresa(Long empresaId) {
                return suscripcionRepository.findByEmpresaId(empresaId)
                                .orElseGet(() -> {
                                        // Lazy assignment: Si no tiene, buscar empresa y asignar Básico
                                        com.vet_saas.modules.company.model.Empresa empresa = empresaLookupService
                                                        .getEmpresaById(empresaId);
                                        return assignDefaultPlan(empresa);
                                });
        }

        @Transactional(readOnly = true)
        public Suscripcion getSuscripcionByEmpresa(Long empresaId) {
                return suscripcionRepository.findByEmpresaId(empresaId)
                                .orElseThrow(() -> new com.vet_saas.core.exceptions.types.ResourceNotFoundException(
                                                "No se encontró una suscripción para la empresa ID: " + empresaId));
        }

        @Transactional
        public Suscripcion getSuscripcionEntityByVeterinario(Long veterinarioId) {
                return suscripcionRepository.findByVeterinarioId(veterinarioId)
                                .orElseGet(() -> {
                                        com.vet_saas.modules.veterinarian.model.Veterinario vet = veterinarioRepository
                                                        .findById(veterinarioId)
                                                        .orElseThrow(() -> new com.vet_saas.core.exceptions.types.ResourceNotFoundException(
                                                                        "Veterinario no encontrado con ID: "
                                                                                        + veterinarioId));
                                        return assignDefaultPlanToVeterinario(vet);
                                });
        }

        @Transactional(readOnly = true)
        public Suscripcion getSuscripcionByVeterinario(Long veterinarioId) {
                return suscripcionRepository.findByVeterinarioId(veterinarioId)
                                .orElseThrow(() -> new com.vet_saas.core.exceptions.types.ResourceNotFoundException(
                                                "No se encontró una suscripción para el veterinario ID: "
                                                                + veterinarioId));
        }

        @Transactional
        public Suscripcion assignDefaultPlan(Empresa empresa) {
                // Evitar duplicados si ya tiene suscripción
                if (suscripcionRepository.findByEmpresaId(empresa.getId()).isPresent()) {
                        return suscripcionRepository.findByEmpresaId(empresa.getId()).get();
                }

                // Preferir planes específicos para B2B/Empresa, buscar "Huella Free B2B" o "Huella Free" primero
                Plan freePlan = planRepository.findByNombreIgnoreCase("Huella Free B2B")
                                .or(() -> planRepository.findByNombreIgnoreCase("Huella Free"))
                                .or(() -> planRepository.findByNombreIgnoreCase(appProperties.getBusiness().getDefaultPlanName()))
                                .or(() -> planRepository.findAllByActivoTrue().stream().findFirst())
                                .orElseThrow(() -> new com.vet_saas.core.exceptions.types.ResourceNotFoundException(
                                                "No se encontraron planes activos en el sistema."));

                Suscripcion suscripcion = Suscripcion.builder()
                                .empresa(empresa)
                                .plan(freePlan)
                                .fechaInicio(LocalDateTime.now())
                                .estado(EstadoSuscripcion.ACTIVA)
                                .build();

                return suscripcionRepository.save(suscripcion);
        }

        @Transactional
        public Suscripcion assignDefaultPlanToVeterinario(com.vet_saas.modules.veterinarian.model.Veterinario vet) {
                if (suscripcionRepository.findByVeterinarioId(vet.getId()).isPresent()) {
                        return suscripcionRepository.findByVeterinarioId(vet.getId()).get();
                }

                // Preferir planes específicos para B2B/Empresa, buscar "Huella Free B2B" o "Huella Free" primero
                Plan freePlan = planRepository.findByNombreIgnoreCase("Huella Free B2B")
                                .or(() -> planRepository.findByNombreIgnoreCase("Huella Free"))
                                .or(() -> planRepository.findByNombreIgnoreCase(appProperties.getBusiness().getDefaultPlanName()))
                                .or(() -> planRepository.findAllByActivoTrue().stream().findFirst())
                                .orElseThrow(() -> new com.vet_saas.core.exceptions.types.ResourceNotFoundException(
                                                "No se encontraron planes activos en el sistema."));

                Suscripcion suscripcion = Suscripcion.builder()
                                .veterinario(vet)
                                .plan(freePlan)
                                .fechaInicio(LocalDateTime.now())
                                .estado(EstadoSuscripcion.ACTIVA)
                                .build();

                return suscripcionRepository.save(suscripcion);
        }

        @Transactional
        public SuscripcionResponseDto updatePlan(Long empresaId, Long planId) {
                return updatePlanForOwner(empresaId, null, planId);
        }

        @Transactional
        public SuscripcionResponseDto updatePlanForUsuario(com.vet_saas.modules.user.model.Usuario usuario,
                        Long planId) {
                if ("VETERINARIO".equals(usuario.getRol().name())) {
                        com.vet_saas.modules.veterinarian.model.Veterinario vet = getVeterinarioFromUsuario(usuario);
                        return updatePlanForOwner(null, vet.getId(), planId);
                } else if ("CLIENTE".equals(usuario.getRol().name())) {
                        Suscripcion sub = suscripcionRepository.findByUsuarioId(usuario.getId())
                                        .orElseThrow(() -> new com.vet_saas.core.exceptions.types.ResourceNotFoundException(
                                                        "No se encontró suscripción para este usuario."));
                        Plan newPlan = planRepository.findById(planId)
                                        .orElseThrow(() -> new com.vet_saas.core.exceptions.types.ResourceNotFoundException(
                                                        "Plan no encontrado con ID: " + planId));
                        sub.setPlan(newPlan);
                        return SuscripcionResponseDto.fromEntity(suscripcionRepository.save(sub));
                } else {
                        Empresa empresa = getEmpresaFromUsuario(usuario);
                        return updatePlanForOwner(empresa.getId(), null, planId);
                }
        }

        @Transactional
        public SuscripcionResponseDto updatePlanForVeterinario(Long veterinarioId, Long planId) {
                return updatePlanForOwner(null, veterinarioId, planId);
        }

        @Transactional
        public SuscripcionResponseDto cancelSubscription(com.vet_saas.modules.user.model.Usuario usuario) {
                Suscripcion suscripcion;
                if ("VETERINARIO".equals(usuario.getRol().name())) {
                        com.vet_saas.modules.veterinarian.model.Veterinario vet = getVeterinarioFromUsuario(usuario);
                        suscripcion = getSuscripcionEntityByVeterinario(vet.getId());
                } else if ("CLIENTE".equals(usuario.getRol().name())) {
                        suscripcion = suscripcionRepository.findByUsuarioId(usuario.getId())
                                        .orElseThrow(() -> new com.vet_saas.core.exceptions.types.ResourceNotFoundException(
                                                        "No se encontró suscripción para este usuario."));
                } else {
                        Empresa empresa = getEmpresaFromUsuario(usuario);
                        suscripcion = getSuscripcionEntityByEmpresa(empresa.getId());
                }

                if (suscripcion.getEstado() == EstadoSuscripcion.CANCELADA) {
                        throw new com.vet_saas.core.exceptions.types.BusinessException("La suscripción ya está cancelada.");
                }

                suscripcion.setEstado(EstadoSuscripcion.CANCELADA);
                suscripcion.setUpdatedAt(LocalDateTime.now());

                return SuscripcionResponseDto.fromEntity(suscripcionRepository.save(suscripcion));
        }

        private SuscripcionResponseDto updatePlanForOwner(Long empresaId, Long veterinarioId, Long planId) {
                Suscripcion suscripcion;
                if (empresaId != null) {
                        suscripcion = getSuscripcionEntityByEmpresa(empresaId);
                } else {
                        suscripcion = getSuscripcionEntityByVeterinario(veterinarioId);
                }

                Plan newPlan = planRepository.findById(planId)
                                .orElseThrow(() -> new com.vet_saas.core.exceptions.types.ResourceNotFoundException(
                                                "Plan no encontrado con ID: " + planId));

                suscripcion.setPlan(newPlan);
                suscripcion.setUpdatedAt(LocalDateTime.now());

                return SuscripcionResponseDto.fromEntity(suscripcionRepository.save(suscripcion));
        }

        /**
         * Verifica si una empresa puede agregar una mascota según su plan.
         * 
         * @param empresaId   ID de la empresa.
         * @param currentCount Cantidad actual de mascotas activas de la empresa.
         * @return true si puede agregar, false si alcanzó el límite.
         */
        public boolean canAddMascota(Long empresaId, long currentCount) {
                Suscripcion sub = getSuscripcionByEmpresa(empresaId);
                if (sub == null) return true;
                Integer limit = sub.getPlan().getLimiteMascotas();
                if (limit == null || limit < 0) {
                        return true; // -1 o null = ilimitado
                }
                if (limit == 0) {
                        return false; // 0 = no incluido
                }
                return currentCount < limit;
        }

        /**
         * Verifica si una empresa puede agregar un producto según su plan.
         * 
         * @param empresaId   ID de la empresa.
         * @param currentCount Cantidad actual de productos activos de la empresa.
         * @return true si puede agregar, false si alcanzó el límite o no incluido.
         */
        public boolean canAddProduct(Long empresaId, long currentCount) {
                Suscripcion sub = getSuscripcionByEmpresa(empresaId);
                if (sub == null) return true;
                Integer limit = sub.getPlan().getLimiteProductos();
                if (limit == null || limit < 0) {
                        return true; // -1 o null = ilimitado
                }
                if (limit == 0) {
                        return false; // 0 = no incluido
                }
                return currentCount < limit;
        }

        /**
         * Verifica si un usuario CLIENTE puede hacer uso de IA según su plan.
         * Si el plan tiene limiteIaUso == -1 o null, es ilimitado.
         * Si el plan tiene limiteIaUso == 0, no tiene acceso a IA.
         *
         * @param usuarioId ID del usuario.
         * @throws com.vet_saas.core.exceptions.types.BusinessException si alcanzó el límite o no tiene acceso.
         */
        public void enforceIaUsage(Long usuarioId) {
                Suscripcion sub = suscripcionRepository.findByUsuarioId(usuarioId).orElse(null);
                if (sub == null || sub.getPlan() == null) {
                        return; // Sin suscripción, permitir
                }
                Integer limit = sub.getPlan().getLimiteIaUso();
                if (limit == null || limit < 0) {
                        return; // -1 o null = ilimitado
                }
                if (limit == 0) {
                        throw new com.vet_saas.core.exceptions.types.BusinessException(
                                        "Tu plan " + sub.getPlan().getNombre() + " no incluye acceso al asistente de IA. "
                                                        + "Actualiza tu plan para obtener consultas IA.");
                }
                java.time.LocalDateTime inicioMes = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
                long usosMes = iaUsageRepository.countByUsuarioIdSince(usuarioId, inicioMes);
                if (usosMes >= limit) {
                        throw new com.vet_saas.core.exceptions.types.BusinessException(
                                        "Has alcanzado el límite de " + limit + " consultas IA de tu plan este mes. "
                                                        + "Actualiza tu plan para obtener más consultas.");
                }
        }

        @Transactional(readOnly = true)
        public SubscriptionUsageDto getUsageMetrics(com.vet_saas.modules.user.model.Usuario usuario) {
                Suscripcion sub;
                long petCount;
                long productCount = 0;
                long serviceCount = 0;

                if ("CLIENTE".equals(usuario.getRol().name())) {
                        sub = suscripcionRepository.findByUsuarioId(usuario.getId()).orElse(null);
                        petCount = mascotaRepository.countByUsuarioIdAndActivoTrue(usuario.getId());
                } else {
                        Empresa empresa = getEmpresaFromUsuario(usuario);
                        sub = getSuscripcionByEmpresa(empresa.getId());
                        petCount = mascotaRepository.countByUsuarioIdAndActivoTrue(usuario.getId());
                        productCount = productoRepository.countByEmpresaIdAndActivoTrue(empresa.getId());
                        serviceCount = servicioRepository.countByEmpresaIdAndActivoTrue(empresa.getId());
                }

                if (sub == null) {
                        return SubscriptionUsageDto.builder()
                                        .currentPets(petCount)
                                        .maxPets(0)
                                        .petPercentage(0)
                                        .currentProducts(productCount)
                                        .maxProducts(0)
                                        .productPercentage(0)
                                        .currentServices(serviceCount)
                                        .maxServices(0)
                                        .servicePercentage(0)
                                        .build();
                }

                Plan plan = sub.getPlan();

                return SubscriptionUsageDto.builder()
                                .currentPets(petCount)
                                .maxPets(plan.getLimiteMascotas())
                                .petPercentage(calculatePercentage(petCount, plan.getLimiteMascotas()))
                                .currentProducts(productCount)
                                .maxProducts(plan.getLimiteProductos())
                                .productPercentage(calculatePercentage(productCount, plan.getLimiteProductos()))
                                .currentServices(serviceCount)
                                .maxServices(plan.getLimiteServicios())
                                .servicePercentage(calculatePercentage(serviceCount, plan.getLimiteServicios()))
                                .build();
        }

        @Transactional
        public PaymentPreferenceResponse createSubscriptionCheckout(com.vet_saas.modules.user.model.Usuario usuario,
                        Long planId) {
                Long entityId;
                String entityName;
                String prefix;

                if ("VETERINARIO".equals(usuario.getRol().name())) {
                        com.vet_saas.modules.veterinarian.model.Veterinario vet = getVeterinarioFromUsuario(usuario);
                        entityId = vet.getId();
                        entityName = vet.getNombres() + " " + vet.getApellidos();
                        prefix = "VET";
                } else if ("CLIENTE".equals(usuario.getRol().name())) {
                        entityId = usuario.getId();
                        entityName = usuario.getCorreo();
                        prefix = "CLI";
                } else {
                        Empresa empresa = getEmpresaFromUsuario(usuario);
                        entityId = empresa.getId();
                        entityName = empresa.getNombreComercial();
                        prefix = "EMP";
                }
                Plan plan = planRepository.findById(planId)
                                .orElseThrow(() -> new com.vet_saas.core.exceptions.types.ResourceNotFoundException(
                                                "Plan no encontrado con ID: " + planId));

                if (plan.getPrecioMensual().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new com.vet_saas.core.exceptions.types.BusinessException(
                                        "El plan seleccionado es gratuito. Use el flujo de cambio directo.");
                }

                // Configurar Items para Mercado Pago
                PreferenceItemRequest item = PreferenceItemRequest.builder()
                                .id(plan.getId().toString())
                                .title("Suscripción VetSaaS: Plan " + plan.getNombre())
                                .description(plan.getDescripcion())
                                .quantity(1)
                                .unitPrice(new BigDecimal(plan.getPrecioMensual().toString()))
                                .build();

                PreferencePayerRequest payer = PreferencePayerRequest.builder()
                                .email(usuario.getCorreo())
                                .name(entityName)
                                .build();

                // Metadata para procesar el pago luego
                Map<String, Object> metadata = new java.util.HashMap<>();
                metadata.put("type", "SUBSCRIPTION");
                metadata.put("plan_id", plan.getId());
                metadata.put("user_id", usuario.getId());
                if ("EMP".equals(prefix)) {
                        metadata.put("empresa_id", entityId);
                } else if ("VET".equals(prefix)) {
                        metadata.put("veterinario_id", entityId);
                } else {
                        metadata.put("usuario_id", entityId);
                }

                String portalPath = switch (prefix) {
                        case "EMP" -> "empresa";
                        case "VET" -> "veterinario";
                        case "CLI" -> "cliente";
                        default -> "empresa";
                };
                String successUrl = appProperties.getExternal().getFrontendUrl()
                                + "/portal/" + portalPath + "/pago-exitoso";
                String notificationUrl = appProperties.getExternal().getBackendUrl() + "/api/v1/payments/webhook";

                return mercadoPagoGateway.createPreference(
                                appProperties.getExternal().getMercadoPago().getAccessToken(),
                                "SUB" + prefix + "-" + entityId + "-" + System.currentTimeMillis(),
                                Collections.singletonList(item),
                                payer,
                                metadata,
                                successUrl,
                                notificationUrl);
        }

        @Transactional
        public void processSubscriptionPayment(Long empresaId, Long veterinarioId, Long planId, String mpPaymentId) {
                // Verificar si este pago ya fue procesado (Idempotencia)
                Suscripcion suscripcionExistente;

                if (empresaId != null) {
                        suscripcionExistente = getSuscripcionByEmpresa(empresaId);
                } else {
                        suscripcionExistente = getSuscripcionByVeterinario(veterinarioId);
                }

                if (suscripcionExistente != null && suscripcionExistente.getMpPreapprovalId() != null &&
                                suscripcionExistente.getMpPreapprovalId().equals(mpPaymentId)) {
                        LOGGER.info("El pago {} ya fue procesado. Omitiendo.", mpPaymentId);
                        return;
                }

                LOGGER.info("Procesando pago de suscripción al plan {}. PaymentId: {}", planId, mpPaymentId);

                Plan plan = planRepository.findById(planId)
                                .orElseThrow(() -> new com.vet_saas.core.exceptions.types.ResourceNotFoundException(
                                                "Plan no encontrado con ID: " + planId));

                Suscripcion suscripcion;
                if (empresaId != null) {
                        Empresa empresa = empresaLookupService.getEmpresaById(empresaId);
                        suscripcion = suscripcionRepository.findByEmpresaId(empresaId)
                                        .orElseGet(() -> Suscripcion.builder()
                                                        .empresa(empresa)
                                                        .fechaInicio(LocalDateTime.now())
                                                        .build());
                } else {
                        com.vet_saas.modules.veterinarian.model.Veterinario vet = veterinarioRepository
                                        .findById(veterinarioId)
                                        .orElseThrow(() -> new com.vet_saas.core.exceptions.types.ResourceNotFoundException(
                                                        "Veterinario no encontrado con ID: " + veterinarioId));
                        suscripcion = suscripcionRepository.findByVeterinarioId(veterinarioId)
                                        .orElseGet(() -> Suscripcion.builder()
                                                        .veterinario(vet)
                                                        .fechaInicio(LocalDateTime.now())
                                                        .build());
                }

                suscripcion.setPlan(plan);
                suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
                suscripcion.setFechaInicio(LocalDateTime.now());
                suscripcion.setFechaFin(LocalDateTime.now().plusMonths(1));
                suscripcion.setUpdatedAt(LocalDateTime.now());

                suscripcionRepository.save(suscripcion);
                LOGGER.info("Suscripción actualizada exitosamente.");
        }

        private double calculatePercentage(long current, Integer max) {
                if (max == null || max <= 0) {
                        return 0; // Ilimitado o no configurado
                }
                double percent = (double) current / max * 100;
                return Math.min(percent, 100.0);
        }
}
