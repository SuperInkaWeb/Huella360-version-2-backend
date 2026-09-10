package com.vet_saas.modules.user.dto;

import com.vet_saas.modules.user.model.Role;

public record UserMeResponse(
        Long id,
        String correo,
        Role rol
) {}
