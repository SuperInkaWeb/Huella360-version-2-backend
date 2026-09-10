package com.vet_saas.modules.auth.dto;

import com.vet_saas.modules.user.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SyncAuth0Request(
        @NotBlank @Email String correo,
        String nombre,
        String auth0Sub,
        Role rol
) {}
