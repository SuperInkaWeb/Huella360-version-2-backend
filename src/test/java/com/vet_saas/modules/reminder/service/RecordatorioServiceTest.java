package com.vet_saas.modules.reminder.service;

import com.vet_saas.modules.notification.service.EmailService;
import com.vet_saas.modules.pet.repository.MascotaRepository;
import com.vet_saas.modules.reminder.repository.RecordatorioRepository;
import com.vet_saas.modules.subscription.service.PlanEnforcementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordatorioServiceTest {

    @Mock private RecordatorioRepository recordatorioRepository;
    @Mock private MascotaRepository mascotaRepository;
    @Mock private PlanEnforcementService planEnforcementService;
    @Mock private EmailService emailService;

    @InjectMocks private RecordatorioService recordatorioService;

    /**
     * H360: el job corre a las 8:00 y comparaba contra "ahora"; una vacuna programada hoy a las 10:30
     * quedaba fuera y se avisaba al dia siguiente, despues de la cita. Debe incluir todo el dia de hoy.
     */
    @Test
    void sendPendingReminders_incluyeLosRecordatoriosDeTodoElDiaDeHoy() {
        ArgumentCaptor<LocalDateTime> limite = ArgumentCaptor.forClass(LocalDateTime.class);
        when(recordatorioRepository.findByEnviadoFalseAndActivoTrueAndFechaProgramadaLessThanEqual(limite.capture()))
                .thenReturn(List.of());

        recordatorioService.sendPendingReminders();

        verify(recordatorioRepository).findByEnviadoFalseAndActivoTrueAndFechaProgramadaLessThanEqual(limite.getValue());
        assertEquals(LocalDate.now(), limite.getValue().toLocalDate());
        assertEquals(LocalTime.MAX, limite.getValue().toLocalTime());
    }
}
