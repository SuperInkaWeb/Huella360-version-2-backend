package com.vet_saas.modules.reminder.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record CreateRecordatorioRequest(
    @NotNull Long mascotaId,
    @NotNull String tipo,
    @NotBlank String titulo,
    String descripcion,
    @NotNull LocalDateTime fechaProgramada
) {}
