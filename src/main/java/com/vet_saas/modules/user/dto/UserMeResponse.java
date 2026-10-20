package com.vet_saas.modules.user.dto;

import com.vet_saas.modules.user.model.Role;

/**
 * @param verificacionPendiente true solo si el backend exige el correo verificado y este no lo esta:
 *                              es lo que el frontend usa para mostrar la pantalla de verificacion.
 */
public record UserMeResponse(
        Long id,
        String correo,
        Role rol,
        boolean emailVerificado,
        boolean verificacionPendiente
) {}
