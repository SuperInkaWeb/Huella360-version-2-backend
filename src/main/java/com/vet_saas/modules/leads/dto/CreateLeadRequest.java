package com.vet_saas.modules.leads.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record CreateLeadRequest(
    @NotNull Long empresaId,
    @NotBlank String clienteNombre,
    String clienteEmail,
    String clienteTelefono,
    String servicioSolicitado,
    String mensaje
) {}
