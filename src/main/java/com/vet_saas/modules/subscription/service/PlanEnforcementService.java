package com.vet_saas.modules.subscription.service;

import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.referral.service.ReferralService;
import com.vet_saas.modules.subscription.model.Plan;
import com.vet_saas.modules.subscription.model.Suscripcion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlanEnforcementService {

    private final SubscriptionService subscriptionService;
    private final ReferralService referralService;

    @Transactional(readOnly = true)
    public void enforceMascotaLimit(Long usuarioId, long currentCount, String role) {
        if ("CLIENTE".equals(role)) {
            enforceClientPetLimit(usuarioId, currentCount);
        }
    }

    private void enforceClientPetLimit(Long usuarioId, long currentCount) {
        Suscripcion sub = subscriptionService.getSuscripcionByUsuarioId(usuarioId);
        if (sub == null) return;
        Integer limit = sub.getPlan().getLimiteMascotas();
        if (limit == null || limit < 0) {
            return; // -1 o null = ilimitado
        }
        if (limit == 0) {
            throw new BusinessException(
                    "Tu plan " + sub.getPlan().getNombre() + " no incluye mascotas. "
                            + "Mejora tu suscripción para agregar mascotas.");
        }

        // Bonus: +1 mascota extra si tiene >= 10 referidos
        long effectiveLimit = limit;
        try {
            if (referralService.getReferralCount(usuarioId).isDesbloqueo2daMascota()) {
                effectiveLimit = limit + 1;
            }
        } catch (Exception e) {
            // Si falla la consulta de referidos, usar el límite base del plan
        }

        if (currentCount >= effectiveLimit) {
            String extraMsg = effectiveLimit > limit ? " (+1 por referidos)" : "";
            throw new BusinessException(
                    "Has alcanzado el límite de " + effectiveLimit + " mascota(s) de tu plan " +
                            sub.getPlan().getNombre() + extraMsg + ". Mejora tu suscripción para agregar más.");
        }
    }

    @Transactional(readOnly = true)
    public void enforceServiceLimit(Long empresaId, long currentCount) {
        Suscripcion sub = subscriptionService.getSuscripcionEntityByEmpresa(empresaId);
        if (sub == null) return;
        Plan plan = sub.getPlan();
        Integer limit = plan.getLimiteServicios();
        if (limit == null || limit < 0) {
            return; // -1 o null = ilimitado
        }
        if (limit == 0) {
            throw new BusinessException(
                    "Tu plan " + plan.getNombre() + " no incluye servicios. "
                            + "Actualiza tu plan para agregar servicios.");
        }
        if (currentCount >= limit) {
            throw new BusinessException(
                    "Has alcanzado el límite de " + limit + " servicio(s) de tu plan " +
                            plan.getNombre() + ". Actualiza tu plan para agregar más.");
        }
    }

    @Transactional(readOnly = true)
    public void enforceProductLimit(Long empresaId, long currentCount) {
        Suscripcion sub = subscriptionService.getSuscripcionEntityByEmpresa(empresaId);
        if (sub == null) return;
        Plan plan = sub.getPlan();
        Integer limit = plan.getLimiteProductos();
        if (limit == null || limit < 0) {
            return; // -1 o null = ilimitado
        }
        if (limit == 0) {
            throw new BusinessException(
                    "Tu plan " + plan.getNombre() + " no incluye productos. "
                            + "Actualiza tu plan para agregar productos.");
        }
        if (currentCount >= limit) {
            throw new BusinessException(
                    "Has alcanzado el límite de " + limit + " producto(s) de tu plan " +
                            plan.getNombre() + ". Actualiza tu plan para agregar más.");
        }
    }

    @Transactional(readOnly = true)
    public void enforceServiceLimitForVeterinario(Long veterinarioId, long currentCount) {
        Suscripcion sub = subscriptionService.getSuscripcionEntityByVeterinario(veterinarioId);
        if (sub == null) return;
        Plan plan = sub.getPlan();
        Integer limit = plan.getLimiteServicios();
        if (limit == null || limit < 0) {
            return; // -1 o null = ilimitado
        }
        if (limit == 0) {
            throw new BusinessException(
                    "Tu plan " + plan.getNombre() + " no incluye servicios. "
                            + "Actualiza tu plan para agregar servicios.");
        }
        if (currentCount >= limit) {
            throw new BusinessException(
                    "Has alcanzado el límite de " + limit + " servicio(s) de tu plan " +
                            plan.getNombre() + ". Actualiza tu plan para agregar más.");
        }
    }

    @Transactional(readOnly = true)
    public void enforceReminderLimit(Long usuarioId, long currentCount, String role) {
        Suscripcion sub = subscriptionService.getSuscripcionByUsuarioId(usuarioId);
        if (sub == null) return;
        Integer limit = sub.getPlan().getLimiteRecordatorios();
        if (limit == null || limit < 0) {
            return; // -1 o null = ilimitado
        }
        if (limit == 0) {
            throw new BusinessException(
                    "Tu plan " + sub.getPlan().getNombre() + " no incluye recordatorios. "
                            + "Actualiza tu plan para crear recordatorios.");
        }
        if (currentCount >= limit) {
            throw new BusinessException(
                    "Has alcanzado el límite de " + limit + " recordatorio(s) de tu plan " +
                            sub.getPlan().getNombre() + ". Actualiza tu plan para crear más.");
        }
    }

    @Transactional(readOnly = true)
    public void enforceIaLimit(Long usuarioId, long currentUsage) {
        Suscripcion sub = subscriptionService.getSuscripcionByUsuarioId(usuarioId);
        if (sub == null) return;
        Integer limit = sub.getPlan().getLimiteIaUso();
        if (limit == null || limit < 0) {
            return; // -1 o null = ilimitado
        }
        if (limit == 0) {
            throw new BusinessException(
                    "Tu plan " + sub.getPlan().getNombre() + " no incluye acceso al asistente de IA. "
                            + "Actualiza tu plan para obtener consultas IA.");
        }
        if (currentUsage >= limit) {
            throw new BusinessException(
                    "Has alcanzado el límite de " + limit + " consulta(s) de IA de tu plan " +
                            sub.getPlan().getNombre() + ". Actualiza tu plan para continuar.");
        }
    }

    @Transactional(readOnly = true)
    public Plan getUserPlan(Long usuarioId) {
        Suscripcion sub = subscriptionService.getSuscripcionByUsuarioId(usuarioId);
        return sub != null ? sub.getPlan() : null;
    }
}
