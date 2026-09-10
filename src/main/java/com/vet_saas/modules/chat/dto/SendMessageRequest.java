package com.vet_saas.modules.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
        @NotBlank(message = "El mensaje no puede estar vacío") String contenido) {
}
