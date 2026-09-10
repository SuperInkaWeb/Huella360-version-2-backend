package com.vet_saas.modules.auth.dto;

import com.vet_saas.modules.user.model.Role;

public record SyncAuth0Response(
        Long id,
        String correo,
        Role rol
) {}
