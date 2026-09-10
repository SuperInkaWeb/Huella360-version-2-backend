package com.vet_saas.modules.reminder.service;

import com.vet_saas.core.exceptions.types.ResourceNotFoundException;
import com.vet_saas.modules.notification.service.EmailService;
import com.vet_saas.modules.pet.model.Mascota;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import com.vet_saas.modules.reminder.dto.CreateRecordatorioRequest;
import com.vet_saas.modules.reminder.dto.RecordatorioResponse;
import com.vet_saas.modules.reminder.model.Recordatorio;
import com.vet_saas.modules.reminder.model.RecordatorioTipo;
import com.vet_saas.modules.reminder.repository.RecordatorioRepository;
import com.vet_saas.modules.subscription.service.PlanEnforcementService;
import com.vet_saas.modules.user.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordatorioService {

    private final RecordatorioRepository recordatorioRepository;
    private final MascotaRepository mascotaRepository;
    private final PlanEnforcementService planEnforcementService;
    private final EmailService emailService;

    @Transactional
    public RecordatorioResponse createRecordatorio(Usuario usuario, CreateRecordatorioRequest request) {
        Mascota mascota = mascotaRepository.findById(request.mascotaId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota", "id", request.mascotaId()));

        // Enforce reminder limits
        long currentCount = recordatorioRepository.countByUsuarioIdAndActivoTrue(usuario.getId());
        planEnforcementService.enforceReminderLimit(usuario.getId(), currentCount, usuario.getRol().name());

        RecordatorioTipo tipo;
        try {
            tipo = RecordatorioTipo.valueOf(request.tipo().toUpperCase());
        } catch (IllegalArgumentException e) {
            tipo = RecordatorioTipo.OTRO;
        }

        Recordatorio recordatorio = Recordatorio.builder()
                .usuario(usuario)
                .mascota(mascota)
                .tipo(tipo)
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .fechaProgramada(request.fechaProgramada())
                .enviado(false)
                .activo(true)
                .build();

        Recordatorio saved = recordatorioRepository.save(recordatorio);
        log.info("Recordatorio {} creado para usuario {}", saved.getId(), usuario.getId());

        return RecordatorioResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<RecordatorioResponse> getMyRecordatorios(Usuario usuario) {
        return recordatorioRepository.findByUsuarioIdAndActivoTrueOrderByFechaProgramadaAsc(usuario.getId())
                .stream()
                .map(RecordatorioResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteRecordatorio(Usuario usuario, Long recordatorioId) {
        Recordatorio recordatorio = recordatorioRepository.findById(recordatorioId)
                .orElseThrow(() -> new ResourceNotFoundException("Recordatorio", "id", recordatorioId));

        if (!recordatorio.getUsuario().getId().equals(usuario.getId())) {
            throw new com.vet_saas.core.exceptions.types.BusinessException("No tienes permiso para eliminar este recordatorio");
        }

        recordatorio.setActivo(false);
        recordatorioRepository.save(recordatorio);
    }

    @Scheduled(cron = "0 0 8 * * *") // Every day at 8am
    @Transactional
    public void sendPendingReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Recordatorio> pending = recordatorioRepository.findByEnviadoFalseAndActivoTrueAndFechaProgramadaLessThanEqual(now);

        for (Recordatorio recordatorio : pending) {
            try {
                String email = recordatorio.getUsuario().getCorreo();
                if (email != null && !email.isBlank()) {
                    String subject = "Recordatorio Huella360: " + recordatorio.getTitulo();
                    String htmlContent = buildReminderEmail(recordatorio);
                    emailService.sendEmail(email, subject, htmlContent);
                }

                recordatorio.setEnviado(true);
                recordatorioRepository.save(recordatorio);

                log.info("Recordatorio {} enviado a usuario {}", recordatorio.getId(), recordatorio.getUsuario().getId());
            } catch (Exception e) {
                log.error("Error enviando recordatorio {}: {}", recordatorio.getId(), e.getMessage());
            }
        }
    }

    private String buildReminderEmail(Recordatorio recordatorio) {
        return String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #1ea59c;">🐾 Recordatorio Huella360</h2>
                    <div style="background-color: #f8f9fa; padding: 15px; border-radius: 8px; margin: 15px 0;">
                        <h3 style="margin: 0; color: #333;">%s</h3>
                        <p style="color: #666; margin: 5px 0;">Mascota: <strong>%s</strong></p>
                        <p style="color: #666; margin: 5px 0;">Tipo: <strong>%s</strong></p>
                        %s
                    </div>
                    <p style="color: #999; font-size: 12px;">Este es un recordatorio automático de Huella360.</p>
                </div>
                """,
                recordatorio.getTitulo(),
                recordatorio.getMascota().getNombre(),
                recordatorio.getTipo().name(),
                recordatorio.getDescripcion() != null ? "<p style=\"color: #666;\">" + recordatorio.getDescripcion() + "</p>" : ""
        );
    }
}
